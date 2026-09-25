"""Pydantic schemas for Smart Decisions (Reorder, Stockout Risk, Allocation, Simulator)."""

from typing import List, Optional
from pydantic import BaseModel, Field

class ReorderRecommendationItem(BaseModel):
    product_id: int
    product_name: str
    sku: str
    category_name: str
    store_id: int
    store_name: str
    supplier_id: Optional[int] = None
    supplier_name: Optional[str] = None
    current_stock: int
    average_daily_demand: float
    lead_time_days: int
    lead_time_demand: float
    safety_stock: int
    reorder_point: int
    eoq: int
    moq: int
    recommended_reorder_qty: int
    estimated_cost: float
    urgency: str  # critical, high, medium, low
    reason: str

class CreatePOFromReordersRequest(BaseModel):
    items: List[dict]  # list of {store_id, supplier_id, product_id, quantity, unit_cost}

class StockoutRiskItem(BaseModel):
    product_id: int
    product_name: str
    sku: str
    store_id: int
    store_name: str
    current_stock: int
    average_daily_demand: float
    lead_time_days: int
    days_of_stock_remaining: Optional[float] = None
    risk_level: str  # critical, high, medium, low
    explanation: str

class StoreAllocationLine(BaseModel):
    store_id: int
    store_name: str
    current_stock: int
    forecast_demand: float
    priority_score: float
    allocated_quantity: int
    shortage_after_allocation: float
    reason: str

class StoreAllocationRequest(BaseModel):
    product_id: int
    total_quantity_available: int = Field(..., gt=0)
    allocation_rule: str = Field(default="priority")  # proportional, priority
    dry_run: bool = True

class StoreAllocationResponse(BaseModel):
    product_id: int
    product_name: str
    total_quantity_available: int
    allocation_rule: str
    dry_run: bool
    allocations: List[StoreAllocationLine]
    total_allocated: int

class WhatIfSimulationRequest(BaseModel):
    store_id: int
    product_id: int
    demand_growth_pct: float = Field(default=0.0)  # e.g. 20.0 for +20%
    lead_time_change_days: int = Field(default=0)  # e.g. +5 days delay
    price_change_pct: float = Field(default=0.0)
    simulated_available_stock: Optional[int] = None

class WhatIfSimulationResponse(BaseModel):
    store_name: str
    product_name: str
    baseline_daily_demand: float
    simulated_daily_demand: float
    baseline_lead_time_days: int
    simulated_lead_time_days: int
    current_stock: int
    effective_stock: int
    projected_demand_30d: float
    projected_shortage_units: int
    baseline_stockout_risk: str
    simulated_stockout_risk: str
    recommended_safety_buffer: int
    estimated_revenue_impact: float
    insights: List[str]
