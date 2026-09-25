"""Pydantic schemas for product management."""

from typing import Optional
from pydantic import BaseModel, Field

class ProductBase(BaseModel):
    sku: str = Field(..., min_length=2, max_length=50)
    name: str = Field(..., min_length=2, max_length=150)
    description: Optional[str] = ""
    category_id: int
    unit_of_measure: str = "unit"
    unit_cost: float = Field(..., ge=0)
    unit_price: float = Field(..., ge=0)
    is_active: int = 1

class ProductCreate(ProductBase):
    pass

class ProductUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    category_id: Optional[int] = None
    unit_of_measure: Optional[str] = None
    unit_cost: Optional[float] = None
    unit_price: Optional[float] = None
    is_active: Optional[int] = None

class ProductResponse(ProductBase):
    product_id: int
    category_name: Optional[str] = None
    created_at: str
    
    # Computed metrics
    total_stock_on_hand: Optional[int] = 0
    total_inventory_value: Optional[float] = 0.0
    profit_margin: Optional[float] = 0.0
    stock_status: Optional[str] = "Normal"
