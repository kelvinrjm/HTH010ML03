"""Demand forecasting and model training center router."""

from fastapi import APIRouter, Depends, Query
from backend.auth.dependencies import CurrentUser, get_current_user, require_admin
from backend.schemas.forecasting import ForecastResponse, ModelTrainingCenterResponse, RetrainResponse
from backend.services.forecast_service import (
    get_forecast,
    get_model_training_center,
    train_and_evaluate_models,
)

router = APIRouter(prefix="/forecast", tags=["Forecasting"])

@router.get("", response_model=ForecastResponse)
def get_sku_forecast(
    store_id: int = Query(...),
    product_id: int = Query(...),
    horizon_days: int = Query(default=30, ge=7, le=90),
    user: CurrentUser = Depends(get_current_user),
):
    """Retrieve actual demand vs predicted demand with confidence intervals."""
    return get_forecast(store_id=store_id, product_id=product_id, horizon_days=horizon_days)

@router.get("/model-center", response_model=ModelTrainingCenterResponse)
def get_model_center(user: CurrentUser = Depends(get_current_user)):
    """Examine ML models, active algorithm, validation metrics, and features."""
    return get_model_training_center()

@router.post("/retrain", response_model=RetrainResponse)
def retrain_models(admin: CurrentUser = Depends(require_admin)):
    """Admin-only: Trigger model training run comparing Random Forest, Gradient Boosting & XGBoost."""
    return train_and_evaluate_models()
