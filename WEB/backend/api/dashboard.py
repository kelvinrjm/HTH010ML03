"""Executive dashboard router."""

from fastapi import APIRouter, Depends
from backend.auth.dependencies import CurrentUser, get_current_user
from backend.schemas.dashboard import DashboardOverviewResponse
from backend.services.dashboard_service import get_dashboard_overview

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])

@router.get("/overview", response_model=DashboardOverviewResponse)
def get_overview(user: CurrentUser = Depends(get_current_user)):
    """Fetch complete executive dashboard intelligence."""
    return get_dashboard_overview()
