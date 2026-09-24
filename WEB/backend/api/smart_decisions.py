"""Smart decisions router: Reorders, stockout risk, allocation, and what-if simulation."""

from typing import List
from fastapi import APIRouter, Depends
from backend.auth.dependencies import CurrentUser, get_current_user
from backend.schemas.smart_decisions import (
    ReorderRecommendationItem,
    StockoutRiskItem,
    StoreAllocationRequest,
    StoreAllocationResponse,
    WhatIfSimulationRequest,
    WhatIfSimulationResponse,
)
from backend.services.smart_decisions_service import (
    calculate_store_allocation,
    get_reorder_recommendations,
    get_stockout_risks,
    simulate_what_if,
)

router = APIRouter(prefix="/smart-decisions", tags=["Smart Decisions"])

@router.get("/reorder", response_model=List[ReorderRecommendationItem])
def get_reorder_items(user: CurrentUser = Depends(get_current_user)):
    """Fetch algorithmic reorder recommendations with EOQ and MOQ constraints."""
    return get_reorder_recommendations()

@router.get("/stockout-risk", response_model=List[StockoutRiskItem])
def get_stockout_radar(user: CurrentUser = Depends(get_current_user)):
    """Evaluate products facing imminent stockout based on sales velocity and lead times."""
    return get_stockout_risks()

@router.post("/allocation", response_model=StoreAllocationResponse)
def compute_store_allocation(
    req: StoreAllocationRequest,
    user: CurrentUser = Depends(get_current_user),
):
    """Compute proportional or priority-based stock allocation across stores."""
    return calculate_store_allocation(req)

@router.post("/simulator", response_model=WhatIfSimulationResponse)
def run_simulation(
    req: WhatIfSimulationRequest,
    user: CurrentUser = Depends(get_current_user),
):
    """Run an isolated scenario simulation (strictly read-only; does not modify DB)."""
    return simulate_what_if(req)
