"""Reports and data export router."""

import io
import csv
from fastapi import APIRouter, Depends, Response
from backend.auth.dependencies import CurrentUser, get_current_user
from backend.services.inventory_service import list_inventory
from backend.services.sales_service import list_sales

router = APIRouter(prefix="/reports", tags=["Reports"])

@router.get("/inventory/csv")
def export_inventory_csv(user: CurrentUser = Depends(get_current_user)):
    """Export current multi-store inventory snapshot as CSV."""
    records = list_inventory()
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "Store", "SKU", "Product Name", "Category", "Unit Cost", "Unit Price",
        "On Hand", "Reserved", "Available", "Days of Stock", "Health Status", "Total Value"
    ])
    for r in records:
        writer.writerow([
            r.store_name, r.sku, r.product_name, r.category_name, r.unit_cost, r.unit_price,
            r.quantity_on_hand, r.quantity_reserved, r.quantity_available,
            r.days_of_stock or "N/A", r.stock_health_status.upper(), r.inventory_value
        ])

    return Response(
        content=output.getvalue(),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=stocksense_inventory_report.csv"},
    )

@router.get("/sales/csv")
def export_sales_csv(user: CurrentUser = Depends(get_current_user)):
    """Export recent sales transactions as CSV."""
    records = list_sales(limit=1000)
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["Order ID", "Store", "Date", "Customer", "Channel", "Total Amount"])
    for r in records:
        writer.writerow([
            r.sale_id, r.store_name, r.sale_date, r.customer_name or "Walk-in", r.channel, r.total_amount
        ])

    return Response(
        content=output.getvalue(),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=stocksense_sales_report.csv"},
    )
