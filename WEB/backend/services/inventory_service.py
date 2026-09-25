"""Inventory tracking and stock operations service."""

from datetime import datetime, timezone
from typing import List, Optional
from fastapi import HTTPException
from backend.database import get_db
from backend.schemas.inventory import InventoryItemResponse, StockAdjustmentRequest, StockTransferRequest
from smartstock.db.operations import fetch_all, fetch_one, insert_record, update_records

def list_inventory(
    store_id: Optional[int] = None,
    category_id: Optional[int] = None,
    health_status: Optional[str] = None,
    search: Optional[str] = None,
) -> List[InventoryItemResponse]:
    """Retrieve detailed store inventory rows with real health calculations."""
    with get_db() as conn:
        query = """
            SELECT 
                i.inventory_id, i.store_id, s.name as store_name,
                i.product_id, p.name as product_name, p.sku,
                c.name as category_name, p.unit_cost, p.unit_price,
                i.quantity_on_hand, i.quantity_reserved, i.quantity_on_order,
                i.reorder_point, i.reorder_quantity, i.last_updated,
                ps.lead_time_days
            FROM inventory i
            JOIN stores s ON i.store_id = s.store_id
            JOIN products p ON i.product_id = p.product_id
            JOIN categories c ON p.category_id = c.category_id
            LEFT JOIN product_suppliers ps ON p.product_id = ps.product_id AND ps.is_preferred = 1
            WHERE 1=1
        """
        params = []
        if store_id is not None:
            query += " AND i.store_id = ?"
            params.append(store_id)
        if category_id is not None:
            query += " AND p.category_id = ?"
            params.append(category_id)
        if search:
            query += " AND (p.name LIKE ? OR p.sku LIKE ?)"
            params.extend([f"%{search}%", f"%{search}%"])

        query += " ORDER BY s.name ASC, p.name ASC"
        rows = fetch_all(conn, query, params)

        # Get recent 30-day daily demand per (store, product)
        demand_rows = fetch_all(
            conn,
            """
            SELECT store_id, product_id, COALESCE(SUM(quantity), 0) / 30.0 as avg_daily_demand
            FROM sale_items si
            JOIN sales s ON si.sale_id = s.sale_id
            WHERE s.sale_date >= date('now', '-30 days')
            GROUP BY store_id, product_id
            """
        )
        demand_map = {(d["store_id"], d["product_id"]): d["avg_daily_demand"] for d in demand_rows}

        results = []
        for r in rows:
            st_id = r["store_id"]
            p_id = r["product_id"]
            q_avail = r["quantity_on_hand"] - r["quantity_reserved"]
            avg_demand = demand_map.get((st_id, p_id), 0.5)
            lead_time = r["lead_time_days"] or 7
            
            days_stock = round(q_avail / avg_demand, 1) if avg_demand > 0 else (999.0 if q_avail > 0 else 0.0)

            # Health classification
            if q_avail <= 0 or days_stock < lead_time:
                status = "critical"
            elif days_stock < (2 * lead_time):
                status = "low"
            elif days_stock <= (6 * lead_time):
                status = "healthy"
            elif avg_demand == 0.0 and days_stock > 90:
                status = "dead_stock"
            else:
                status = "overstock"

            if health_status and status != health_status.lower():
                continue

            results.append(
                InventoryItemResponse(
                    inventory_id=r["inventory_id"],
                    store_id=st_id,
                    store_name=r["store_name"],
                    product_id=p_id,
                    product_name=r["product_name"],
                    sku=r["sku"],
                    category_name=r["category_name"],
                    unit_cost=r["unit_cost"],
                    unit_price=r["unit_price"],
                    quantity_on_hand=r["quantity_on_hand"],
                    quantity_reserved=r["quantity_reserved"],
                    quantity_available=q_avail,
                    quantity_on_order=r["quantity_on_order"],
                    reorder_point=r["reorder_point"],
                    reorder_quantity=r["reorder_quantity"],
                    days_of_stock=days_stock if days_stock < 900 else None,
                    stock_health_status=status,
                    inventory_value=round(r["quantity_on_hand"] * r["unit_cost"], 2),
                    last_updated=r["last_updated"],
                )
            )
        return results

def transfer_stock(req: StockTransferRequest, actor: str) -> dict:
    """Safely transfer stock between stores with audit logging."""
    if req.from_store_id == req.to_store_id:
        raise HTTPException(status_code=400, detail="Cannot transfer inventory to the same store")

    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    with get_db() as conn:
        src = fetch_one(
            conn,
            "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
            (req.from_store_id, req.product_id),
        )
        if not src or src["quantity_on_hand"] < req.quantity:
            avail = src["quantity_on_hand"] if src else 0
            raise HTTPException(
                status_code=400,
                detail=f"Insufficient source stock. Available: {avail}, Requested: {req.quantity}",
            )

        # 1. Decrement source
        conn.execute(
            "UPDATE inventory SET quantity_on_hand = quantity_on_hand - ?, last_updated = ? WHERE store_id = ? AND product_id = ?",
            (req.quantity, now_utc, req.from_store_id, req.product_id),
        )
        # 2. Increment destination
        conn.execute(
            "UPDATE inventory SET quantity_on_hand = quantity_on_hand + ?, last_updated = ? WHERE store_id = ? AND product_id = ?",
            (req.quantity, now_utc, req.to_store_id, req.product_id),
        )
        # 3. Record transfer
        insert_record(
            conn,
            "stock_transfers",
            {
                "from_store_id": req.from_store_id,
                "to_store_id": req.to_store_id,
                "product_id": req.product_id,
                "quantity": req.quantity,
                "transfer_date": now_utc[:10],
                "status": "completed",
                "actor": actor,
                "notes": req.notes or "",
                "created_at": now_utc,
            },
        )
        # 4. Audit Log
        insert_record(
            conn,
            "audit_logs",
            {
                "event_type": "stock_transfer",
                "entity_type": "inventory",
                "entity_id": str(req.product_id),
                "actor": actor,
                "occurred_at": now_utc,
                "before_state": f'{{"source_store": {req.from_store_id}, "qty": {src["quantity_on_hand"]}}}',
                "after_state": f'{{"transferred_qty": {req.quantity}, "dest_store": {req.to_store_id}}}',
                "notes": req.notes or "Stock transfer",
            },
        )
        return {"status": "success", "message": f"Successfully moved {req.quantity} units."}

def adjust_stock(req: StockAdjustmentRequest, actor: str) -> dict:
    """Manual inventory correction with mandatory audit trail."""
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    with get_db() as conn:
        current = fetch_one(
            conn,
            "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
            (req.store_id, req.product_id),
        )
        old_qty = current["quantity_on_hand"] if current else 0

        update_records(
            conn,
            "inventory",
            {"quantity_on_hand": req.new_quantity_on_hand, "last_updated": now_utc},
            where="store_id = ? AND product_id = ?",
            where_params=(req.store_id, req.product_id),
        )

        insert_record(
            conn,
            "audit_logs",
            {
                "event_type": "stock_adjustment",
                "entity_type": "inventory",
                "entity_id": f"{req.store_id}:{req.product_id}",
                "actor": actor,
                "occurred_at": now_utc,
                "before_state": f'{{"quantity_on_hand": {old_qty}}}',
                "after_state": f'{{"quantity_on_hand": {req.new_quantity_on_hand}}}',
                "notes": f"Reason: {req.reason}. {req.notes or ''}".strip(),
            },
        )
        return {"status": "success", "message": f"Stock updated from {old_qty} to {req.new_quantity_on_hand}."}
