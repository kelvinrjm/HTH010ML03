# STOCKSENSE — AI-Powered Inventory Intelligence & Demand Forecasting Platform

<p align="center">
  <img src="frontend/public/logo.png" alt="STOCKSENSE Logo" width="120" style="border-radius: 50%; border: 3px solid rgba(59, 130, 246, 0.4); box-shadow: 0 10px 25px -5px rgba(59, 130, 246, 0.4);" />
</p>

<p align="center">
  <strong>Predict Demand. Prevent Stockouts. Make Smarter Inventory Decisions.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-2.0.0-blue.svg" alt="Version" />
  <img src="https://img.shields.io/badge/Python-3.11%20%7C%203.12%20%7C%203.13-brightgreen.svg" alt="Python" />
  <img src="https://img.shields.io/badge/FastAPI-0.115-009688.svg" alt="FastAPI" />
  <img src="https://img.shields.io/badge/React-19.0-61DAFB.svg" alt="React" />
  <img src="https://img.shields.io/badge/TypeScript-5.7-3178C6.svg" alt="TypeScript" />
  <img src="https://img.shields.io/badge/TailwindCSS-v4.0-38B2AC.svg" alt="TailwindCSS" />
  <img src="https://img.shields.io/badge/Tests-29%2F29%20Passing-success.svg" alt="Tests" />
</p>

---

## 🎯 Executive Product Overview

**STOCKSENSE** is a commercial-grade, multi-store inventory intelligence and demand forecasting SaaS platform designed for retailers, supermarket chains, wholesalers, and multi-location enterprises. 

Unlike traditional static ERPs or college CRUD apps, STOCKSENSE combines:
1. **Mathematical Inventory Optimization**: Economic Order Quantity (EOQ), dynamic safety stock buffers, and lead-time demand calculations.
2. **Real Machine Learning**: Walk-forward time-series regression comparing Random Forest, Gradient Boosting, and XGBoost with empirical prediction intervals (strictly avoiding fabricated accuracy scores).
3. **Enterprise Transaction Integrity**: Zero negative stock enforcement, atomic inventory decrements, and tamper-evident append-only double-entry audit logging.
4. **Constrained Multi-Store Allocation**: Proportional and priority-based inventory distribution when central supply is limited.
5. **Interactive What-If Simulation Sandbox**: Safe scenario stress-testing with guaranteed zero database mutation side effects.

---

## 🏛 Architecture

```
STOCKSENSE
│
├── frontend/                     # React + TypeScript + Vite + Tailwind CSS (v4)
│   ├── src/
│   │   ├── components/           # Sidebar, Navbar, StatCard, Badge, LoadingSkeleton
│   │   ├── context/              # AuthContext (Role-based JWT access guard)
│   │   ├── pages/                # Dashboard, Inventory, Products, Sales, Purchases,
│   │   │                         # Forecasting, SmartDecisions, Reports, Admin, Login
│   │   ├── services/             # API client with token interceptor
│   │   └── types/                # Domain TypeScript models
│   ├── public/                   # Official STOCKSENSE logo & assets
│   └── package.json
│
├── backend/                      # FastAPI Python Application
│   ├── api/                      # REST endpoints (auth, dashboard, inventory, products,
│   │                             # sales, purchases, forecast, smart_decisions, reports, admin)
│   ├── auth/                     # Bcrypt password hashing & PyJWT token utilities
│   ├── database.py               # Context-managed connection pool bridging SQLite / PostgreSQL
│   ├── schemas/                  # Pydantic v2 validation models
│   ├── services/                 # Domain logic engines (sales, inventory, smart_decisions,
│   │                             # forecast_service, dashboard, system_health)
│   ├── seed_data.py              # Realistic 24-month multi-store historical sales generator
│   └── main.py                   # FastAPI server entrypoint
│
├── smartstock/db/                # Database Foundation
│   ├── migrations/               # 19 versioned migration SQL scripts (0001 - 0019)
│   ├── connection.py             # WAL mode, foreign keys enforcement, safe pool
│   ├── operations.py             # Reusable fetch, insert, update abstractions
│   └── schema.py                 # Version tracking and migration runner
│
├── tests/                        # 29 Automated Tests
│   ├── db/                       # Schema, foreign keys, and constraint unit tests
│   └── test_backend_services.py  # Full-stack service verification suite
│
├── data/                         # SQLite storage directory (smartstock.db)
├── LOGO.png                      # Official 3D STOCKSENSE brand icon
├── requirements.txt              # Python production dependencies
└── .env.example                  # Environment configuration template
```

---

## 🚀 Quickstart & Local Setup

### 1. Prerequisites
- **Python 3.11+** installed
- **Node.js 18+** & `npm` installed

### 2. Backend Setup
```bash
# Clone the repository
git clone https://github.com/vabis/STOCKSENSE.git
cd STOCKSENSE

# Create and activate virtual environment (optional)
python -m venv venv
venv\Scripts\activate   # On Windows
# source venv/bin/activate # On Linux/macOS

# Install dependencies
pip install -r requirements.txt

# Start the FastAPI Backend Server
python -m uvicorn backend.main:app --host 127.0.0.1 --port 8000 --reload
```
API Documentation will be live at: **http://127.0.0.1:8000/docs**

### 3. Frontend Setup
```bash
# In a separate terminal, enter the frontend directory:
cd frontend

# Install Node dependencies
npm install

# Start the Vite development server
npm run dev
```
Open your browser at: **http://127.0.0.1:5173**

---

## 🔑 Demo Access Credentials

The demo environment comes pre-seeded with a comprehensive 24-month historical sales dataset across 4 locations and 105 commercial SKUs.

| Role | Username | Password | Privileges |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `Admin@123` | Full access: All modules, System Health, Model Retraining, Audit Logs |
| **Staff Cashier** | `staff` | `Staff@123` | Operational access: POS Sales, Purchases, Inventory, Forecasts |

---

## 💡 Core Modules & Business Capabilities

### 1. Executive Intelligence Dashboard
- Answers the core owner question: *"How is my business doing today?"*
- High-level KPIs: Total Inventory Value, Today's Sales, Monthly Sales, Estimated Profit, Low Stock, Stockout Risk, Dead Stock.
- Recharts Revenue vs Profit interactive area graph.
- Algorithmic AI Business Insights with supporting numbers and actionable recommendations.
- Top best-sellers and slow-moving capital risk tables.

### 2. Multi-Store Inventory Management
- Real-time stock levels, reservations, reorder points, and days of coverage.
- Filter by store location, health status (*Healthy*, *Low Stock*, *Critical*, *Overstock*, *Dead Stock*), and search.
- **Inter-Store Transfers**: Move stock between stores with automatic dual-entry ledger audit logging.
- **Stock Adjustments**: Cycle count corrections, damaged goods write-offs, with mandatory reasons.

### 3. Products Catalog & Gross Margin Analysis
- Complete 105-product catalog with SKU codes, categories, units of measure, cost prices, and retail prices.
- Automatic gross margin calculations (`((Price - Cost) / Price) * 100`).
- Cross-store aggregate stock positions and enterprise valuation.

### 4. Point of Sale & Checkout (Negative Stock Prevention)
- Direct store-specific counter checkout.
- Multi-channel support: *In-Store Counter*, *Online Delivery*, *B2B Wholesale*.
- **Integrity Guarantee**: Real-time stock availability check prevents negative inventory; invalid orders are rejected at both API and database levels.
- Itemized receipt generation and instantaneous inventory decrement.

### 5. Purchases & Automated Replenishment
- Create supplier purchase orders with contracted vendor lead times.
- One-click **Receive Stock** action automatically increments store on-hand inventory and closes the PO.

### 6. AI Demand Forecasting Studio
- Real ML time-series regression pipeline using lag features (`lag_1`, `lag_7`, `lag_14`, `lag_28`), rolling statistical means/stds, and calendar indicators.
- Chronological walk-forward validation (zero forward data leakage).
- 7-day, 14-day, and 30-day forecast horizons.
- Empirical 95% confidence intervals derived from out-of-time residual variance.
- Expandable Model Specifications section for data transparency.

### 7. Smart Decisions Engine
- **Reorder Engine**: Calculates Economic Order Quantity (EOQ), Minimum Order Quantity (MOQ), and safety buffers.
- **Stockout Radar**: Flags products with coverage below supplier lead times.
- **Constrained Multi-Store Allocation**: Proportional distribution of central supply batches to prevent store-level stockouts.
- **What-If Scenario Simulator**: Stress-test demand surges (+/-100%), supply chain delays (+/-20 days), and price adjustments without altering the database.

### 8. Enterprise CSV Reports Center
- One-click downloadable CSV reports for Inventory Valuation, Sales Transactions, Supplier Purchases, Dead Stock, and Audit Trails.

### 9. Isolated Admin System Health & ML Model Center
- **Diagnostics Isolation**: Schema version, table registers (21 tables), and database internals are removed from normal users and housed strictly in Admin System Health.
- **Model Training Center**: Compares Random Forest, Gradient Boosting, and XGBoost; one-click "Retrain Production Model" button runs chronological walk-forward evaluation.
- **Audit Logs**: Append-only log of all inventory transfers, adjustments, and transactions.

---

## 🧪 Testing & Quality Assurance

STOCKSENSE includes a 29-test verification suite covering database integrity and full-stack services:

```bash
# Run database schema, foreign key, and constraint tests:
pytest tests/db/

# Run full-stack service tests (auth, POS, transfers, reorders, ML forecasting, simulator):
python -m unittest tests.test_backend_services
```

All 29 tests pass with 100% success rate.

---

## 🔒 Security & Production Best Practices

- **Zero Hardcoded Secrets**: Credentials, database paths, and signing keys are managed via environment variables (`.env`).
- **Cryptographic Hashing**: Passwords stored using industry-standard `bcrypt` with automatic salting.
- **Stateless Authorization**: Signed JWT bearer tokens with configurable expiration and role validation (`admin`, `staff`).
- **PostgreSQL Compatibility**: SQLAlchemy-ready schema design allowing seamless migration from SQLite to AWS RDS / Google Cloud SQL / Supabase PostgreSQL.

---

## 📄 License

Proprietary enterprise SaaS codebase developed for commercial inventory intelligence. All rights reserved.
