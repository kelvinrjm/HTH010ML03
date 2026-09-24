"""Stores and suppliers management routers."""

from fastapi import APIRouter, Depends
from backend.auth.dependencies import CurrentUser, get_current_user
from backend.database import get_db
from smartstock.db.operations import fetch_all

stores_router = APIRouter(prefix="/stores", tags=["Stores"])
suppliers_router = APIRouter(prefix="/suppliers", tags=["Suppliers"])

@stores_router.get("")
def get_stores(user: CurrentUser = Depends(get_current_user)):
    """List all retail and wholesale store locations."""
    with get_db() as conn:
        return fetch_all(conn, "SELECT * FROM stores WHERE is_active = 1 ORDER BY name ASC")

@suppliers_router.get("")
def get_suppliers(user: CurrentUser = Depends(get_current_user)):
    """List all registered vendors and lead times."""
    with get_db() as conn:
        return fetch_all(conn, "SELECT * FROM suppliers WHERE is_active = 1 ORDER BY name ASC")
