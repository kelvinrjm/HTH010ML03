"""Sales order processing and transaction management service."""

from datetime import datetime, timezone
from typing import List, Optional
from fastapi import HTTPException
from backend.database import get_db
from backend.schemas.sales import SaleCreate, SaleItemResponse, SaleResponse
from smartstock.db.operations import fetch_all, fetch_one, insert_record

def list_sales(
    store_id: Optional[int] = None,
    limit: int = 100,
) -> List[SaleResponse]:
    """Retrieve recent sales orders with store and customer details."""
    with get_db() as conn:
        query = """
            SELECT 
                s.sale_id, s.store_id, st.name as store_name,
                s.customer_id, c.name as customer_name,
                s.sale_date, s.total_amount, s.channel, s.created_at
            FROM sales s
            JOIN stores st ON s.store_id = st.store_id
            LEFT JOIN customers c ON s.customer_id = c.customer_id
            WHERE 1=1
        """
        params = []
        if store_id is not None:
            query += " AND s.store_id = ?"
            params.append(store_id)

        query += " ORDER BY s.created_at DESC LIMIT ?"
        params.append(limit)

        rows = fetch_all(conn, query, params)
        results = []
        for r in rows:
            # Fetch line items for this sale
            item_rows = fetch_all(
                conn,
                """
                SELECT si.sale_item_id, si.product_id, p.name as product_name, p.sku,
                       si.quantity, si.unit_price, si.subtotal
                FROM sale_items si
                JOIN products p ON si.product_id = p.product_id
                WHERE si.sale_id = ?
                """,
                (r["sale_id"],),
            )
            items = [
                SaleItemResponse(
                    sale_item_id=it["sale_item_id"],
                    product_id=it["product_id"],
                    product_name=it["product_name"],
                    sku=it["sku"],
                    quantity=it["quantity"],
                    unit_price=it["unit_price"],
                    subtotal=it["subtotal"],
                )
                for it in item_rows
            ]

            results.append(
                SaleResponse(
                    sale_id=r["sale_id"],
                    store_id=r["store_id"],
                    store_name=r["store_name"],
                    customer_id=r["customer_id"],
                    customer_name=r["customer_name"],
                    sale_date=r["sale_date"],
                    total_amount=r["total_amount"],
                    channel=r["channel"],
                    created_at=r["created_at"],
                    items=items,
                )
            )
        return results

def process_sale(sale: SaleCreate, actor: str) -> SaleResponse:
    """Validate stock, decrement inventory atomically, record sale and audit trail."""
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    sale_date = now_utc[:10]

    with get_db() as conn:
        # 1. Validate stock availability for all items
        total_amount = 0.0
        line_items_data = []

        for item in sale.items:
            inv = fetch_one(
                conn,
                """
                SELECT i.quantity_on_hand, i.quantity_reserved, p.name, p.unit_price
                FROM inventory i
                JOIN products p ON i.product_id = p.product_id
                WHERE i.store_id = ? AND i.product_id = ?
                """,
                (sale.store_id, item.product_id),
            )
            if not inv:
                raise HTTPException(status_code=400, detail=f"Product {item.product_id} not available in selected store")

            available = inv["quantity_on_hand"] - inv["quantity_reserved"]
            if available < item.quantity:
                raise HTTPException(
                    status_code=400,
                    detail=f"Insufficient stock for '{inv['name']}'. Requested: {item.quantity}, Available: {available}",
                )

            subtotal = round(item.quantity * item.unit_price, 2)
            total_amount += subtotal
            line_items_data.append((item.product_id, item.quantity, item.unit_price, subtotal))

        # 2. Insert sale header
        norm_channel = "in-store" if sale.channel in ("in_store", "in-store") else sale.channel
        sale_id = insert_record(
            conn,
            "sales",
            {
                "store_id": sale.store_id,
                "customer_id": sale.customer_id,
                "sale_date": sale_date,
                "total_amount": round(total_amount, 2),
                "channel": norm_channel,
                "created_at": now_utc,
            },
        )

        # 3. Insert sale items & decrement inventory
        response_items = []
        for pid, qty, price, subt in line_items_data:
            cur = conn.execute(
                "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                (sale_id, pid, qty, price, subt, now_utc),
            )
            item_id = cur.lastrowid
            
            # Atomic inventory decrement
            conn.execute(
                "UPDATE inventory SET quantity_on_hand = quantity_on_hand - ?, last_updated = ? WHERE store_id = ? AND product_id = ?",
                (qty, now_utc, sale.store_id, pid),
            )
            
            p_info = fetch_one(conn, "SELECT name, sku FROM products WHERE product_id = ?", (pid,))
            response_items.append(
                SaleItemResponse(
                    sale_item_id=item_id,
                    product_id=pid,
                    product_name=p_info["name"],
                    sku=p_info["sku"],
                    quantity=qty,
                    unit_price=price,
                    subtotal=subt,
                )
            )

        # 4. Audit Log
        insert_record(
            conn,
            "audit_logs",
            {
                "event_type": "sale_completed",
                "entity_type": "sales",
                "entity_id": str(sale_id),
                "actor": actor,
                "occurred_at": now_utc,
                "before_state": None,
                "after_state": f'{{"sale_id": {sale_id}, "total": {round(total_amount, 2)}, "items_count": {len(line_items_data)}}}',
                "notes": f"Sale via {sale.channel}",
            },
        )

        st_info = fetch_one(conn, "SELECT name FROM stores WHERE store_id = ?", (sale.store_id,))
        cust_name = None
        if sale.customer_id:
            c_info = fetch_one(conn, "SELECT name FROM customers WHERE customer_id = ?", (sale.customer_id,))
            cust_name = c_info["name"] if c_info else None

        return SaleResponse(
            sale_id=sale_id,
            store_id=sale.store_id,
            store_name=st_info["name"],
            customer_id=sale.customer_id,
            customer_name=cust_name,
            sale_date=sale_date,
            total_amount=round(total_amount, 2),
            channel=sale.channel,
            created_at=now_utc,
            items=response_items,
        )
