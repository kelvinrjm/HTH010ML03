"""Pydantic schemas for demand forecasting and ML model registry."""

from typing import List, Optional
from pydantic import BaseModel

class ForecastSeriesPoint(BaseModel):
    date: str
    actual: Optional[float] = None
    predicted: Optional[float] = None
    lower_bound: Optional[float] = None
    upper_bound: Optional[float] = None

ForecastPoint = ForecastSeriesPoint

class ForecastResponse(BaseModel):
    store_id: int
    store_name: str
    product_id: int
    product_name: str
    sku: str
    horizon_days: int
    current_stock: int
    total_predicted_demand: float
    average_daily_demand: float
    stockout_risk: str
    recommended_reorder: int
    model_version: str
    model_type: str
    confidence_level: float
    series: List[ForecastSeriesPoint]

class ModelRegistryItem(BaseModel):
    model_id: int
    model_name: str
    model_version: str
    model_type: str
    target: str
    training_mae: float
    training_rmse: float
    training_r2: float
    trained_at: str
    is_active: bool

class ModelTrainingCenterResponse(BaseModel):
    active_model: Optional[ModelRegistryItem] = None
    all_models: List[ModelRegistryItem] = []
    total_training_records: int
    last_trained_at: Optional[str] = None
    features_used: List[str] = []

class RetrainResponse(BaseModel):
    status: str
    message: str
    active_model: ModelRegistryItem
    candidate_results: List[dict]
    training_duration_seconds: float
