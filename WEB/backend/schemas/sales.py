"""Pydantic schemas for sales processing and transaction history."""

from typing import List, Optional
from pydantic import BaseModel, Field

class SaleItemCreate(BaseModel):
    product_id: int
    quantity: int = Field(..., gt=0)
    unit_price: float = Field(..., ge=0)

class SaleCreate(BaseModel):
    store_id: int
    customer_id: Optional[int] = None
    channel: str = Field(default="in-store")
    items: List[SaleItemCreate] = Field(..., min_length=1)

class SaleItemResponse(BaseModel):
    sale_item_id: int
    product_id: int
    product_name: str
    sku: str
    quantity: int
    unit_price: float
    subtotal: float

class SaleResponse(BaseModel):
    sale_id: int
    store_id: int
    store_name: str
    customer_id: Optional[int] = None
    customer_name: Optional[str] = None
    sale_date: str
    total_amount: float
    channel: str
    created_at: str
    items: Optional[List[SaleItemResponse]] = []
