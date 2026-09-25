"""Purchase orders router."""

from typing import List, Optional
from fastapi import APIRouter, Depends
from backend.auth.dependencies import CurrentUser, get_current_user
from backend.schemas.purchases import PurchaseCreate, PurchaseResponse
from backend.services.purchases_service import create_purchase_order, list_purchases, receive_purchase_order

router = APIRouter(prefix="/purchases", tags=["Purchases"])

@router.get("", response_model=List[PurchaseResponse])
def get_purchase_orders(
    store_id: Optional[int] = None,
    status: Optional[str] = None,
    user: CurrentUser = Depends(get_current_user),
):
    """List purchase orders with line items."""
    return list_purchases(store_id=store_id, status=status)

@router.post("", response_model=PurchaseResponse)
def create_order(
    po: PurchaseCreate,
    user: CurrentUser = Depends(get_current_user),
):
    """Create a new purchase order draft."""
    return create_purchase_order(po, actor=user.username)

@router.post("/{purchase_id}/receive", response_model=dict)
def mark_received(
    purchase_id: int,
    user: CurrentUser = Depends(get_current_user),
):
    """Receive purchase order and replenish on-hand store inventory."""
    return receive_purchase_order(purchase_id, actor=user.username)
