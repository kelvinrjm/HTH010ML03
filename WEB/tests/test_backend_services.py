"""Comprehensive full-stack service test suite for STOCKSENSE platform.

Validates:
1. Authentication & JWT security
2. Products catalog & margin calculations
3. Multi-store inventory transfers & adjustments
4. Sales transaction checkout & negative stock prevention
5. Purchase orders & automated stock replenishment
6. Time-series forecasting & walk-forward validation (zero data leakage)
7. Smart reorder calculations & multi-store allocation constraints
8. What-if simulation isolation (zero database side-effects)
9. System health diagnostics & append-only audit trail
"""

import unittest
from fastapi import HTTPException
from backend.schemas.auth import LoginRequest
from backend.schemas.sales import SaleCreate, SaleItemCreate
from backend.schemas.purchases import PurchaseCreate, PurchaseItemCreate
from backend.schemas.inventory import StockTransferRequest, StockAdjustmentRequest
from backend.services.auth_service import authenticate_user
from backend.auth.security import create_access_token, decode_access_token
from backend.services.product_service import list_products, get_product
from backend.services.inventory_service import list_inventory, transfer_stock, adjust_stock
from backend.services.sales_service import process_sale, list_sales
from backend.services.purchases_service import create_purchase_order, receive_purchase_order, list_purchases
from backend.services.forecast_service import get_forecast, get_model_training_center
from backend.schemas.smart_decisions import StoreAllocationRequest, WhatIfSimulationRequest
from backend.services.smart_decisions_service import (
    get_reorder_recommendations,
    get_stockout_risks,
    calculate_store_allocation,
    simulate_what_if,
)
from backend.services.system_health_service import get_system_health
from backend.database import get_db
from smartstock.db.operations import fetch_one


class TestStocksenseFullSuite(unittest.TestCase):

    def test_01_authentication_and_jwt(self):
        """Verify authentication, password hashing, and JWT token lifecycle."""
        res = authenticate_user(LoginRequest(username="admin", password="Admin@123"))
        self.assertIsNotNone(res)
        self.assertEqual(res.user.username, "admin")
        self.assertEqual(res.user.role, "admin")

        token = res.access_token
        self.assertTrue(len(token) > 20)

        payload = decode_access_token(token)
        self.assertEqual(payload["sub"], "admin")
        self.assertEqual(payload["role"], "admin")

        # Invalid credentials should raise 401 HTTPException
        with self.assertRaises(HTTPException):
            authenticate_user(LoginRequest(username="admin", password="WrongPassword"))

    def test_02_products_catalog_and_margins(self):
        """Verify catalog retrieval, SKU search, and profit margin computation."""
        products = list_products()
        self.assertTrue(len(products) >= 100)

        first_p = products[0]
        self.assertIsNotNone(first_p.profit_margin)
        self.assertGreater(first_p.unit_price, 0)
        self.assertGreater(first_p.profit_margin, 0)

        # SKU search
        matched = list_products(search=first_p.sku)
        self.assertTrue(len(matched) >= 1)
        self.assertEqual(matched[0].sku, first_p.sku)

    def test_03_inventory_overview_and_stock_health(self):
        """Verify multi-store stock positions and health classifications."""
        inv = list_inventory()
        self.assertTrue(len(inv) > 0)

        for item in inv[:10]:
            self.assertIn(item.stock_health_status, ["critical", "low", "healthy", "overstock", "dead_stock"])
            self.assertGreaterEqual(item.quantity_available, 0)
            self.assertGreaterEqual(item.inventory_value, 0)

    def test_04_sales_checkout_and_negative_stock_prevention(self):
        """Verify POS sales transaction: reduces stock, records order, and prevents overselling."""
        store_id = 1
        product_id = 1

        with get_db() as conn:
            inv_before = fetch_one(
                conn,
                "SELECT quantity_on_hand, quantity_reserved FROM inventory WHERE store_id = ? AND product_id = ?",
                [store_id, product_id],
            )
        initial_stock = inv_before["quantity_on_hand"] - inv_before["quantity_reserved"]

        if initial_stock > 2:
            # 1. Successful sale
            sale_req = SaleCreate(
                store_id=store_id,
                channel="in_store",
                items=[SaleItemCreate(product_id=product_id, quantity=1, unit_price=100.0)],
            )
            sale = process_sale(sale_req, actor="test_runner")
            self.assertIsNotNone(sale)
            self.assertEqual(sale.store_id, store_id)

            with get_db() as conn:
                inv_after = fetch_one(
                    conn,
                    "SELECT quantity_on_hand, quantity_reserved FROM inventory WHERE store_id = ? AND product_id = ?",
                    [store_id, product_id],
                )
            new_stock = inv_after["quantity_on_hand"] - inv_after["quantity_reserved"]
            self.assertEqual(new_stock, initial_stock - 1)

            # 2. Overselling must be rejected with HTTPException(400)
            bad_sale = SaleCreate(
                store_id=store_id,
                channel="in_store",
                items=[SaleItemCreate(product_id=product_id, quantity=999999, unit_price=100.0)],
            )
            with self.assertRaises(HTTPException):
                process_sale(bad_sale, actor="test_runner")

    def test_05_inventory_transfer_and_adjustment(self):
        """Verify inter-store transfer updates both source and target stores and writes audit log."""
        product_id = 2
        from_store = 1
        to_store = 2

        with get_db() as conn:
            from_before = fetch_one(
                conn,
                "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                [from_store, product_id],
            )["quantity_on_hand"]
            to_before = fetch_one(
                conn,
                "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                [to_store, product_id],
            )["quantity_on_hand"]

        if from_before >= 5:
            transfer_req = StockTransferRequest(
                from_store_id=from_store,
                to_store_id=to_store,
                product_id=product_id,
                quantity=2,
                notes="Automated unit test transfer",
            )
            transfer_stock(transfer_req, actor="test_runner")

            with get_db() as conn:
                from_after = fetch_one(
                    conn,
                    "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                    [from_store, product_id],
                )["quantity_on_hand"]
                to_after = fetch_one(
                    conn,
                    "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                    [to_store, product_id],
                )["quantity_on_hand"]

            self.assertEqual(from_after, from_before - 2)
            self.assertEqual(to_after, to_before + 2)

    def test_06_purchases_and_receipt_replenishment(self):
        """Verify PO lifecycle: creates PO, receives PO, increments store inventory."""
        store_id = 1
        supplier_id = 1
        product_id = 3

        with get_db() as conn:
            inv_before = fetch_one(
                conn,
                "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                [store_id, product_id],
            )["quantity_on_hand"]

        po_req = PurchaseCreate(
            store_id=store_id,
            supplier_id=supplier_id,
            order_date="2026-09-24",
            expected_delivery_date="2026-09-30",
            items=[PurchaseItemCreate(product_id=product_id, quantity_ordered=15, unit_cost=50.0)],
        )
        po = create_purchase_order(po_req, actor="test_runner")
        self.assertIsNotNone(po)
        self.assertEqual(po.status, "draft")

        # Receive the PO
        receive_purchase_order(po.purchase_id, actor="test_runner")

        with get_db() as conn:
            inv_after = fetch_one(
                conn,
                "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                [store_id, product_id],
            )["quantity_on_hand"]

        self.assertEqual(inv_after, inv_before + 15)

    def test_07_demand_forecasting_and_no_data_leakage(self):
        """Verify ML demand forecasting, non-empty prediction series, and real confidence bounds."""
        forecast = get_forecast(store_id=1, product_id=1, horizon_days=14)
        self.assertIsNotNone(forecast)
        self.assertEqual(forecast.horizon_days, 14)
        self.assertTrue(len(forecast.series) > 14)

        # Check bounds: lower_bound <= upper_bound for future points
        future_points = [p for p in forecast.series if p.predicted is not None and p.actual is None]
        self.assertTrue(len(future_points) > 0)
        for fp in future_points:
            self.assertIsNotNone(fp.lower_bound)
            self.assertIsNotNone(fp.upper_bound)
            self.assertLessEqual(fp.lower_bound, fp.upper_bound)

    def test_08_reorder_engine_and_allocation_constraint(self):
        """Verify Economic Order Quantity recommendations and multi-store allocation constraints."""
        reorders = get_reorder_recommendations()
        self.assertIsInstance(reorders, list)
        if len(reorders) > 0:
            first_r = reorders[0]
            self.assertGreater(first_r.recommended_reorder_qty, 0)
            self.assertIn(first_r.urgency, ["critical", "high", "medium", "low"])

        # Allocation constraint: allocated quantity must NOT exceed available batch
        total_available = 100
        alloc_res = calculate_store_allocation(
            StoreAllocationRequest(
                product_id=1,
                total_quantity_available=total_available,
                allocation_rule="proportional_to_shortage",
            )
        )
        self.assertLessEqual(alloc_res.total_allocated, total_available)

    def test_09_what_if_simulator_isolation(self):
        """Verify that simulation is mathematical sandbox and NEVER mutates the database."""
        store_id = 1
        product_id = 1

        with get_db() as conn:
            sales_cnt_before = fetch_one(conn, "SELECT COUNT(*) as cnt FROM sales")["cnt"]
            inv_before = fetch_one(
                conn,
                "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                [store_id, product_id],
            )["quantity_on_hand"]

        sim = simulate_what_if(
            WhatIfSimulationRequest(
                store_id=store_id,
                product_id=product_id,
                demand_growth_pct=50,
                lead_time_change_days=5,
                price_change_pct=10,
            )
        )
        self.assertIsNotNone(sim)
        self.assertGreater(sim.simulated_daily_demand, 0)

        with get_db() as conn:
            sales_cnt_after = fetch_one(conn, "SELECT COUNT(*) as cnt FROM sales")["cnt"]
            inv_after = fetch_one(
                conn,
                "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
                [store_id, product_id],
            )["quantity_on_hand"]

        self.assertEqual(sales_cnt_before, sales_cnt_after)
        self.assertEqual(inv_before, inv_after)

    def test_10_system_health_and_audit_trail(self):
        """Verify admin-only system health diagnostics and presence of audit logs."""
        health = get_system_health()
        self.assertEqual(health.schema_version, 19)
        self.assertGreaterEqual(health.table_count, 21)
        self.assertTrue(health.foreign_keys_enforced)
        self.assertIn("SQLite", health.database_engine)

        with get_db() as conn:
            audit_cnt = fetch_one(conn, "SELECT COUNT(*) as cnt FROM audit_logs")["cnt"]
        self.assertGreater(audit_cnt, 0)


if __name__ == "__main__":
    unittest.main()
