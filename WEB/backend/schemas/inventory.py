"""Pydantic schemas for inventory operations and stock tracking."""

from typing import Optional
from pydantic import BaseModel, Field

class InventoryItemResponse(BaseModel):
    inventory_id: int
    store_id: int
    store_name: str
    product_id: int
    product_name: str
    sku: str
    category_name: str
    unit_cost: float
    unit_price: float
    quantity_on_hand: int
    quantity_reserved: int
    quantity_available: int
    quantity_on_order: int
    reorder_point: int
    reorder_quantity: int
    days_of_stock: Optional[float] = None
    stock_health_status: str  # critical, low, healthy, overstock, dead_stock
    inventory_value: float
    last_updated: str

class StockTransferRequest(BaseModel):
    from_store_id: int
    to_store_id: int
    product_id: int
    quantity: int = Field(..., gt=0)
    notes: Optional[str] = ""

class StockAdjustmentRequest(BaseModel):
    store_id: int
    product_id: int
    new_quantity_on_hand: int = Field(..., ge=0)
    reason: str = Field(..., min_length=3, max_length=150)
    notes: Optional[str] = ""
