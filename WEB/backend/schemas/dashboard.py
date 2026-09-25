"""Pydantic schemas for the executive business dashboard."""

from typing import List, Optional
from pydantic import BaseModel

class DashboardKPIs(BaseModel):
    total_inventory_value: float
    today_sales: float
    monthly_sales: float
    estimated_monthly_profit: float
    low_stock_count: int
    stockout_risk_count: int
    dead_stock_count: int
    forecast_accuracy_mape: Optional[float] = None
    forecast_model_status: str

class SalesTrendPoint(BaseModel):
    date: str
    revenue: float
    units_sold: int
    profit: float

class InventoryHealthCategory(BaseModel):
    category: str  # Fast Moving, Normal, Slow Moving, Dead Stock
    product_count: int
    total_units: int
    total_value: float
    percentage_of_inventory: float

class TopProductItem(BaseModel):
    product_id: int
    name: str
    sku: str
    category_name: str
    units_sold: int
    revenue: float
    profit_margin: float

class SlowMovingItem(BaseModel):
    product_id: int
    name: str
    sku: str
    store_name: str
    quantity_on_hand: int
    days_without_sale: int
    estimated_value_at_risk: float
    recommended_action: str  # Markdown, Discount, Write-off

class StorePerformanceItem(BaseModel):
    store_id: int
    store_name: str
    store_type: str
    total_revenue: float
    total_inventory_value: float
    active_products: int
    low_stock_items: int

class AIInsightItem(BaseModel):
    id: str
    type: str  # critical_stockout, demand_spike, capital_locked, store_imbalance
    title: str
    product_name: Optional[str] = None
    store_name: Optional[str] = None
    description: str
    why_it_matters: str
    supporting_metrics: dict
    recommended_action: str
    urgency: str  # critical, high, medium, info

class RecentActivityItem(BaseModel):
    id: str
    activity_type: str  # sale, purchase, transfer, adjustment
    title: str
    details: str
    timestamp: str
    actor: str

class DashboardOverviewResponse(BaseModel):
    kpis: DashboardKPIs
    sales_trend: List[SalesTrendPoint]
    inventory_health: List[InventoryHealthCategory]
    top_products: List[TopProductItem]
    slow_moving: List[SlowMovingItem]
    store_performance: List[StorePerformanceItem]
    ai_insights: List[AIInsightItem]
    recent_activity: List[RecentActivityItem]
