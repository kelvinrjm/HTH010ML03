"""Inventory operations router."""

from typing import List, Optional
from fastapi import APIRouter, Depends
from backend.auth.dependencies import CurrentUser, get_current_user, require_admin
from backend.schemas.inventory import InventoryItemResponse, StockAdjustmentRequest, StockTransferRequest
from backend.services.inventory_service import adjust_stock, list_inventory, transfer_stock

router = APIRouter(prefix="/inventory", tags=["Inventory"])

@router.get("", response_model=List[InventoryItemResponse])
def get_inventory_records(
    store_id: Optional[int] = None,
    category_id: Optional[int] = None,
    health_status: Optional[str] = None,
    search: Optional[str] = None,
    user: CurrentUser = Depends(get_current_user),
):
    """List inventory across stores with stock health indicators."""
    return list_inventory(
        store_id=store_id,
        category_id=category_id,
        health_status=health_status,
        search=search,
    )

@router.post("/transfer", response_model=dict)
def execute_transfer(
    req: StockTransferRequest,
    user: CurrentUser = Depends(get_current_user),
):
    """Transfer inventory between stores with audit logging."""
    return transfer_stock(req, actor=user.username)

@router.post("/adjust", response_model=dict)
def execute_adjustment(
    req: StockAdjustmentRequest,
    admin: CurrentUser = Depends(require_admin),
):
    """Admin-only: Correct stock counts with reason and audit trail."""
    return adjust_stock(req, actor=admin.username)
