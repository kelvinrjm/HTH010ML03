"""Products catalog router."""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException
from backend.auth.dependencies import CurrentUser, get_current_user, require_admin
from backend.schemas.products import ProductCreate, ProductResponse
from backend.services.product_service import create_product, get_product, list_products

router = APIRouter(prefix="/products", tags=["Products"])

@router.get("", response_model=List[ProductResponse])
def get_all_products(
    category_id: Optional[int] = None,
    search: Optional[str] = None,
    user: CurrentUser = Depends(get_current_user),
):
    """List all products with stock and margin metrics."""
    return list_products(category_id=category_id, search=search)

@router.get("/{product_id}", response_model=ProductResponse)
def get_single_product(product_id: int, user: CurrentUser = Depends(get_current_user)):
    """Get single product details."""
    prod = get_product(product_id)
    if not prod:
        raise HTTPException(status_code=404, detail="Product not found")
    return prod

@router.post("", response_model=dict)
def add_product(prod: ProductCreate, admin: CurrentUser = Depends(require_admin)):
    """Admin-only: Create a new product."""
    pid = create_product(prod)
    return {"status": "success", "product_id": pid}
