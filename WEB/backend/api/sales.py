"""Sales processing router."""

from typing import List, Optional
from fastapi import APIRouter, Depends
from backend.auth.dependencies import CurrentUser, get_current_user
from backend.schemas.sales import SaleCreate, SaleResponse
from backend.services.sales_service import list_sales, process_sale

router = APIRouter(prefix="/sales", tags=["Sales"])

@router.get("", response_model=List[SaleResponse])
def get_sales(
    store_id: Optional[int] = None,
    limit: int = 100,
    user: CurrentUser = Depends(get_current_user),
):
    """Retrieve sales order history."""
    return list_sales(store_id=store_id, limit=limit)

@router.post("", response_model=SaleResponse)
def create_sale(
    sale: SaleCreate,
    user: CurrentUser = Depends(get_current_user),
):
    """Process a sales order, validate inventory, and decrement stock."""
    return process_sale(sale, actor=user.username)
