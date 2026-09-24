"""Product catalog and details service."""

from typing import List, Optional
from backend.database import get_db
from backend.schemas.products import ProductCreate, ProductResponse, ProductUpdate
from smartstock.db.operations import fetch_all, fetch_one, insert_record, update_records

def list_products(
    category_id: Optional[int] = None,
    search: Optional[str] = None,
    is_active: Optional[int] = None,
) -> List[ProductResponse]:
    """Retrieve all products with category names and computed stock metrics."""
    with get_db() as conn:
        query = """
            SELECT 
                p.product_id, p.sku, p.name, p.description, p.category_id,
                c.name as category_name, p.unit_of_measure, p.unit_cost, p.unit_price,
                p.is_active, p.created_at,
                COALESCE(SUM(i.quantity_on_hand), 0) as total_stock_on_hand,
                COALESCE(SUM(i.quantity_on_hand * p.unit_cost), 0.0) as total_inventory_value
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.category_id
            LEFT JOIN inventory i ON p.product_id = i.product_id
            WHERE 1=1
        """
        params = []
        if category_id is not None:
            query += " AND p.category_id = ?"
            params.append(category_id)
        if is_active is not None:
            query += " AND p.is_active = ?"
            params.append(is_active)
        if search:
            query += " AND (p.name LIKE ? OR p.sku LIKE ?)"
            params.extend([f"%{search}%", f"%{search}%"])

        query += " GROUP BY p.product_id ORDER BY p.name ASC"

        rows = fetch_all(conn, query, params)
        results = []
        for r in rows:
            margin = round(((r["unit_price"] - r["unit_cost"]) / r["unit_price"] * 100), 1) if r["unit_price"] > 0 else 0.0
            stock = r["total_stock_on_hand"]
            status = "Out of Stock" if stock == 0 else ("Low Stock" if stock < 30 else "Healthy")

            results.append(
                ProductResponse(
                    product_id=r["product_id"],
                    sku=r["sku"],
                    name=r["name"],
                    description=r["description"],
                    category_id=r["category_id"],
                    category_name=r["category_name"] or "General",
                    unit_of_measure=r["unit_of_measure"],
                    unit_cost=r["unit_cost"],
                    unit_price=r["unit_price"],
                    is_active=r["is_active"],
                    created_at=r["created_at"],
                    total_stock_on_hand=stock,
                    total_inventory_value=round(r["total_inventory_value"], 2),
                    profit_margin=margin,
                    stock_status=status,
                )
            )
        return results

def get_product(product_id: int) -> Optional[ProductResponse]:
    """Retrieve details for a single product."""
    products = list_products()
    for p in products:
        if p.product_id == product_id:
            return p
    return None

def create_product(prod: ProductCreate) -> int:
    """Create a new product record and initialize inventory across all stores."""
    from datetime import datetime, timezone
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    with get_db() as conn:
        pid = insert_record(
            conn,
            "products",
            {
                "sku": prod.sku,
                "name": prod.name,
                "description": prod.description or "",
                "category_id": prod.category_id,
                "unit_of_measure": prod.unit_of_measure,
                "unit_cost": prod.unit_cost,
                "unit_price": prod.unit_price,
                "is_active": prod.is_active,
                "created_at": now_utc,
            },
        )
        # Initialize 0 inventory in all active stores
        stores = fetch_all(conn, "SELECT store_id FROM stores WHERE is_active = 1")
        for st in stores:
            insert_record(
                conn,
                "inventory",
                {
                    "store_id": st["store_id"],
                    "product_id": pid,
                    "quantity_on_hand": 0,
                    "quantity_reserved": 0,
                    "quantity_on_order": 0,
                    "reorder_point": 20,
                    "reorder_quantity": 50,
                    "last_updated": now_utc,
                },
            )
        return pid
