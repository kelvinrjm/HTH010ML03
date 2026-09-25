"""Purchase orders and supplier replenishment service."""

from datetime import datetime, timezone
from typing import List, Optional
from fastapi import HTTPException
from backend.database import get_db
from backend.schemas.purchases import PurchaseCreate, PurchaseItemResponse, PurchaseResponse
from smartstock.db.operations import fetch_all, fetch_one, insert_record

def list_purchases(
    store_id: Optional[int] = None,
    status: Optional[str] = None,
) -> List[PurchaseResponse]:
    """Retrieve purchase orders with line items and status."""
    with get_db() as conn:
        query = """
            SELECT 
                p.purchase_id, p.store_id, st.name as store_name,
                p.supplier_id, sup.name as supplier_name,
                p.order_date, p.expected_delivery_date, p.status, p.created_at,
                COALESCE(SUM(pi.quantity_ordered * pi.unit_cost), 0.0) as total_amount
            FROM purchases p
            JOIN stores st ON p.store_id = st.store_id
            JOIN suppliers sup ON p.supplier_id = sup.supplier_id
            LEFT JOIN purchase_items pi ON p.purchase_id = pi.purchase_id
            WHERE 1=1
        """
        params = []
        if store_id is not None:
            query += " AND p.store_id = ?"
            params.append(store_id)
        if status:
            query += " AND p.status = ?"
            params.append(status)

        query += " GROUP BY p.purchase_id ORDER BY p.created_at DESC"
        rows = fetch_all(conn, query, params)

        results = []
        for r in rows:
            line_rows = fetch_all(
                conn,
                """
                SELECT pi.purchase_item_id, pi.product_id, prod.name as product_name,
                       prod.sku, pi.quantity_ordered, pi.quantity_received, pi.unit_cost,
                       (pi.quantity_ordered * pi.unit_cost) as subtotal
                FROM purchase_items pi
                JOIN products prod ON pi.product_id = prod.product_id
                WHERE pi.purchase_id = ?
                """,
                (r["purchase_id"],),
            )
            items = [
                PurchaseItemResponse(
                    purchase_item_id=it["purchase_item_id"],
                    product_id=it["product_id"],
                    product_name=it["product_name"],
                    sku=it["sku"],
                    quantity_ordered=it["quantity_ordered"],
                    quantity_received=it["quantity_received"],
                    unit_cost=it["unit_cost"],
                    subtotal=round(it["subtotal"], 2),
                )
                for it in line_rows
            ]

            results.append(
                PurchaseResponse(
                    purchase_id=r["purchase_id"],
                    store_id=r["store_id"],
                    store_name=r["store_name"],
                    supplier_id=r["supplier_id"],
                    supplier_name=r["supplier_name"],
                    order_date=r["order_date"],
                    expected_delivery_date=r["expected_delivery_date"],
                    status=r["status"],
                    total_amount=round(r["total_amount"], 2),
                    created_at=r["created_at"],
                    items=items,
                )
            )
        return results

def create_purchase_order(po: PurchaseCreate, actor: str) -> PurchaseResponse:
    """Create a new purchase order and lines in draft status."""
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    with get_db() as conn:
        p_id = insert_record(
            conn,
            "purchases",
            {
                "store_id": po.store_id,
                "supplier_id": po.supplier_id,
                "order_date": po.order_date,
                "expected_delivery_date": po.expected_delivery_date,
                "status": "draft",
                "created_at": now_utc,
            },
        )

        total_amount = 0.0
        response_items = []
        for it in po.items:
            cur = conn.execute(
                "INSERT INTO purchase_items (purchase_id, product_id, quantity_ordered, quantity_received, unit_cost, created_at) VALUES (?, ?, ?, 0, ?, ?)",
                (p_id, it.product_id, it.quantity_ordered, it.unit_cost, now_utc),
            )
            item_id = cur.lastrowid
            subt = round(it.quantity_ordered * it.unit_cost, 2)
            total_amount += subt

            p_info = fetch_one(conn, "SELECT name, sku FROM products WHERE product_id = ?", (it.product_id,))
            response_items.append(
                PurchaseItemResponse(
                    purchase_item_id=item_id,
                    product_id=it.product_id,
                    product_name=p_info["name"],
                    sku=p_info["sku"],
                    quantity_ordered=it.quantity_ordered,
                    quantity_received=0,
                    unit_cost=it.unit_cost,
                    subtotal=subt,
                )
            )

        insert_record(
            conn,
            "audit_logs",
            {
                "event_type": "purchase_order_created",
                "entity_type": "purchases",
                "entity_id": str(p_id),
                "actor": actor,
                "occurred_at": now_utc,
                "before_state": None,
                "after_state": f'{{"purchase_id": {p_id}, "total": {round(total_amount, 2)}}}',
                "notes": "Draft purchase order created",
            },
        )

        st_info = fetch_one(conn, "SELECT name FROM stores WHERE store_id = ?", (po.store_id,))
        sup_info = fetch_one(conn, "SELECT name FROM suppliers WHERE supplier_id = ?", (po.supplier_id,))

        return PurchaseResponse(
            purchase_id=p_id,
            store_id=po.store_id,
            store_name=st_info["name"],
            supplier_id=po.supplier_id,
            supplier_name=sup_info["name"],
            order_date=po.order_date,
            expected_delivery_date=po.expected_delivery_date,
            status="draft",
            total_amount=round(total_amount, 2),
            created_at=now_utc,
            items=response_items,
        )

def receive_purchase_order(purchase_id: int, actor: str) -> dict:
    """Mark a purchase order as received and automatically replenish on-hand inventory."""
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    with get_db() as conn:
        po = fetch_one(conn, "SELECT * FROM purchases WHERE purchase_id = ?", (purchase_id,))
        if not po:
            raise HTTPException(status_code=404, detail="Purchase order not found")
        if po["status"] == "received":
            raise HTTPException(status_code=400, detail="Purchase order already received")

        # Get lines
        lines = fetch_all(conn, "SELECT * FROM purchase_items WHERE purchase_id = ?", (purchase_id,))
        for line in lines:
            qty = line["quantity_ordered"]
            # 1. Update line received qty
            conn.execute(
                "UPDATE purchase_items SET quantity_received = ? WHERE purchase_item_id = ?",
                (qty, line["purchase_item_id"]),
            )
            # 2. Increment store inventory
            conn.execute(
                """
                UPDATE inventory 
                SET quantity_on_hand = quantity_on_hand + ?, last_updated = ?
                WHERE store_id = ? AND product_id = ?
                """,
                (qty, now_utc, po["store_id"], line["product_id"]),
            )

        # 3. Update PO status
        conn.execute("UPDATE purchases SET status = 'received' WHERE purchase_id = ?", (purchase_id,))

        # 4. Audit
        insert_record(
            conn,
            "audit_logs",
            {
                "event_type": "purchase_received",
                "entity_type": "purchases",
                "entity_id": str(purchase_id),
                "actor": actor,
                "occurred_at": now_utc,
                "before_state": f'{{"status": "{po["status"]}"}}',
                "after_state": '{"status": "received"}',
                "notes": f"Replenished inventory for {len(lines)} line items.",
            },
        )
        return {"status": "success", "message": f"Purchase order #{purchase_id} received. Inventory updated."}
