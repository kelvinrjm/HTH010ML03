"""Pydantic schemas for purchase orders and supplier replenishment."""

from typing import List, Optional
from pydantic import BaseModel, Field

class PurchaseItemCreate(BaseModel):
    product_id: int
    quantity_ordered: int = Field(..., gt=0)
    unit_cost: float = Field(..., ge=0)

class PurchaseCreate(BaseModel):
    store_id: int
    supplier_id: int
    order_date: str
    expected_delivery_date: str
    items: List[PurchaseItemCreate] = Field(..., min_length=1)

class PurchaseItemResponse(BaseModel):
    purchase_item_id: int
    product_id: int
    product_name: str
    sku: str
    quantity_ordered: int
    quantity_received: int
    unit_cost: float
    subtotal: float

class PurchaseResponse(BaseModel):
    purchase_id: int
    store_id: int
    store_name: str
    supplier_id: int
    supplier_name: str
    order_date: str
    expected_delivery_date: str
    status: str
    total_amount: float
    created_at: str
    items: Optional[List[PurchaseItemResponse]] = []
