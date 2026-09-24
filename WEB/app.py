"""STOCKSENSE — AI-Powered Inventory Intelligence & Demand Forecasting Platform.

Predict Demand. Prevent Stockouts. Make Smarter Inventory Decisions.
"""

import os
import streamlit as st

from smartstock.config import DEFAULT_CONFIG
from smartstock.db.connection import get_connection
from smartstock.db.operations import list_tables, fetch_all, fetch_one
from smartstock.db.schema import get_current_version, init_database

st.set_page_config(
    page_title="STOCKSENSE — AI-Powered Inventory Intelligence",
    page_icon="LOGO.png" if os.path.exists("LOGO.png") else None,
    layout="wide",
    initial_sidebar_state="expanded",
)

# Header with Official STOCKSENSE Branding
logo_col, title_col = st.columns([1, 8])
with logo_col:
    if os.path.exists("LOGO.png"):
        st.image("LOGO.png", width=90)
with title_col:
    st.markdown(
        """
        <h1 style="color: #F8FAFC; margin: 0; font-size: 2.2rem; font-weight: 800; font-family: sans-serif;">
            STOCKSENSE
        </h1>
        <p style="color: #38BDF8; margin: 0.2rem 0 0 0; font-size: 1.05rem; font-weight: 500;">
            Predict Demand. Prevent Stockouts. Make Smarter Inventory Decisions.
        </p>
        """,
        unsafe_allow_html=True,
    )

st.markdown("---")

# Executive Notice Banner
st.info(
    "🚀 **STOCKSENSE Full-Stack Platform Active**: The modern React + TypeScript + FastAPI frontend "
    "is available on **http://localhost:5173** (API: **http://localhost:8000**)."
)

# Initialize database verification
try:
    init_database(DEFAULT_CONFIG)
    with get_connection(DEFAULT_CONFIG) as conn:
        prod_count = fetch_one(conn, "SELECT COUNT(*) as cnt FROM products")["cnt"]
        sales_count = fetch_one(conn, "SELECT COUNT(*) as cnt FROM sales")["cnt"]
        stores_count = fetch_one(conn, "SELECT COUNT(*) as cnt FROM stores")["cnt"]
        inv_val = fetch_one(conn, "SELECT SUM(i.quantity_on_hand * p.unit_cost) as val FROM inventory i JOIN products p ON i.product_id = p.product_id")["val"] or 0.0

    st.subheader("📊 Business Intelligence Overview")
    kpi1, kpi2, kpi3, kpi4 = st.columns(4)
    with kpi1:
        st.metric("Total Inventory Value", f"₹{int(inv_val):,}")
    with kpi2:
        st.metric("Catalog SKUs", f"{prod_count} Products")
    with kpi3:
        st.metric("Historical Sales Recorded", f"{sales_count:,} Orders")
    with kpi4:
        st.metric("Network Stores", f"{stores_count} Locations")

    # Developer / Internal Diagnostics moved into collapsible admin section
    with st.expander("🔒 Admin & System Health Diagnostics (Admin Only)", expanded=False):
        with get_connection(DEFAULT_CONFIG) as conn:
            schema_ver = get_current_version(conn)
            tables = list_tables(conn)
            fk_check = conn.execute("PRAGMA foreign_keys").fetchone()[0]

        dcol1, dcol2, dcol3 = st.columns(3)
        with dcol1:
            st.metric("Schema Version", f"v{schema_ver}")
        with dcol2:
            st.metric("Registered Tables", f"{len(tables)} Tables")
        with dcol3:
            st.metric("Foreign Key Constraints", "Enforced" if fk_check else "Disabled")

        st.caption("Internal diagnostic data has been removed from standard user views and isolated in Admin System Health.")

except Exception as err:
    st.error(f"Error loading STOCKSENSE operational data: {err}")
