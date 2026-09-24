"""STOCKSENSE Backend API Server.

Production FastAPI application providing comprehensive inventory intelligence,
demand forecasting, and decision support services.
"""

from contextlib import asynccontextmanager
import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from backend.api.admin import router as admin_router
from backend.api.auth import router as auth_router
from backend.api.dashboard import router as dashboard_router
from backend.api.forecast import router as forecast_router
from backend.api.inventory import router as inventory_router
from backend.api.products import router as products_router
from backend.api.purchases import router as purchases_router
from backend.api.reports import router as reports_router
from backend.api.sales import router as sales_router
from backend.api.smart_decisions import router as smart_decisions_router
from backend.api.stores import stores_router, suppliers_router
from backend.config import settings
from backend.database import init_db
from backend.seed_data import seed_database


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown lifecycle management."""
    print("Initializing STOCKSENSE backend services...")
    init_db()
    seed_database()
    print("STOCKSENSE services initialized successfully.")
    yield
    print("Shutting down STOCKSENSE services.")


app = FastAPI(
    title=settings.PRODUCT_TITLE,
    description=settings.TAGLINE,
    version=settings.VERSION,
    lifespan=lifespan,
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount Static Assets (including official LOGO.png)
assets_dir = os.path.dirname(os.path.dirname(__file__))
if os.path.exists(os.path.join(assets_dir, "LOGO.png")):
    app.mount("/static", StaticFiles(directory=assets_dir), name="static")

# Mount Routers
app.include_router(auth_router, prefix="/api")
app.include_router(dashboard_router, prefix="/api")
app.include_router(products_router, prefix="/api")
app.include_router(inventory_router, prefix="/api")
app.include_router(sales_router, prefix="/api")
app.include_router(purchases_router, prefix="/api")
app.include_router(stores_router, prefix="/api")
app.include_router(suppliers_router, prefix="/api")
app.include_router(forecast_router, prefix="/api")
app.include_router(smart_decisions_router, prefix="/api")
app.include_router(reports_router, prefix="/api")
app.include_router(admin_router, prefix="/api")


@app.get("/api/health")
def health_check():
    """Service liveness probe."""
    return {
        "status": "online",
        "product": settings.PROJECT_NAME,
        "title": settings.PRODUCT_TITLE,
        "tagline": settings.TAGLINE,
        "version": settings.VERSION,
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("backend.main:app", host="127.0.0.1", port=8000, reload=True)
