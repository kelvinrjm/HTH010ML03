"""Smart decisions engine: Reorder recommendations, stockout risk, allocation, and what-if simulation."""

import math
from typing import List
from backend.database import get_db
from backend.schemas.smart_decisions import (
    ReorderRecommendationItem,
    StockoutRiskItem,
    StoreAllocationLine,
    StoreAllocationRequest,
    StoreAllocationResponse,
    WhatIfSimulationRequest,
    WhatIfSimulationResponse,
)
from smartstock.db.operations import fetch_all, fetch_one

def get_reorder_recommendations() -> List[ReorderRecommendationItem]:
    """Calculate mathematically grounded reorder recommendations across all stores."""
    with get_db() as conn:
        query = """
            SELECT 
                i.product_id, p.name as product_name, p.sku, c.name as category_name,
                i.store_id, st.name as store_name,
                sup.supplier_id, sup.name as supplier_name,
                i.quantity_on_hand, i.quantity_reserved,
                COALESCE(ps.lead_time_days, sup.default_lead_time_days, 7) as lead_time_days,
                COALESCE(ps.minimum_order_quantity, 10) as moq,
                p.unit_cost
            FROM inventory i
            JOIN products p ON i.product_id = p.product_id
            JOIN categories c ON p.category_id = c.category_id
            JOIN stores st ON i.store_id = st.store_id
            LEFT JOIN product_suppliers ps ON p.product_id = ps.product_id AND ps.is_preferred = 1
            LEFT JOIN suppliers sup ON ps.supplier_id = sup.supplier_id
            WHERE p.is_active = 1
        """
        rows = fetch_all(conn, query)

        # Get demand statistics from sales
        demand_rows = fetch_all(
            conn,
            """
            SELECT store_id, product_id,
                   COALESCE(SUM(quantity), 0) / 30.0 as avg_daily,
                   COALESCE(AVG(quantity), 1.0) as std_dev
            FROM sale_items si
            JOIN sales s ON si.sale_id = s.sale_id
            WHERE s.sale_date >= date('now', '-30 days')
            GROUP BY store_id, product_id
            """
        )
        stats_map = {(d["store_id"], d["product_id"]): (d["avg_daily"], d["std_dev"]) for d in demand_rows}

        recommendations = []
        for r in rows:
            st_id = r["store_id"]
            p_id = r["product_id"]
            q_avail = r["quantity_on_hand"] - r["quantity_reserved"]
            avg_demand, std_dev = stats_map.get((st_id, p_id), (1.2, 0.8))
            if avg_demand <= 0.05:
                continue

            lead_days = r["lead_time_days"]
            lead_demand = avg_demand * lead_days
            z_score = 1.65  # 95% service level
            safety_stock = int(math.ceil(z_score * std_dev * math.sqrt(lead_days)))
            reorder_point = int(math.ceil(lead_demand + safety_stock))

            # Trigger condition: available stock <= reorder point
            if q_avail <= reorder_point:
                # EOQ calculation: sqrt((2 * annual_demand * order_cost) / holding_cost)
                annual_demand = avg_demand * 365
                order_cost = 25.0
                holding_cost = max(r["unit_cost"] * 0.20, 0.50)
                eoq = int(math.ceil(math.sqrt((2 * annual_demand * order_cost) / holding_cost)))
                moq = r["moq"]
                rec_qty = max(eoq, moq)

                # Urgency ranking
                if q_avail <= 0:
                    urgency = "critical"
                    reason = f"Stockout occurred! Current stock is {q_avail}. Immediate order needed."
                elif q_avail < (reorder_point * 0.5):
                    urgency = "high"
                    reason = f"Stock ({q_avail}) is under 50% of reorder threshold ({reorder_point}). Risk within {lead_days} days."
                else:
                    urgency = "medium"
                    reason = f"Stock ({q_avail}) has dipped below reorder point ({reorder_point}). Standard replenishment cycle."

                recommendations.append(
                    ReorderRecommendationItem(
                        product_id=p_id,
                        product_name=r["product_name"],
                        sku=r["sku"],
                        category_name=r["category_name"],
                        store_id=st_id,
                        store_name=r["store_name"],
                        supplier_id=r["supplier_id"],
                        supplier_name=r["supplier_name"] or "Standard Supplier",
                        current_stock=q_avail,
                        average_daily_demand=round(avg_demand, 2),
                        lead_time_days=lead_days,
                        lead_time_demand=round(lead_demand, 1),
                        safety_stock=safety_stock,
                        reorder_point=reorder_point,
                        eoq=eoq,
                        moq=moq,
                        recommended_reorder_qty=rec_qty,
                        estimated_cost=round(rec_qty * r["unit_cost"], 2),
                        urgency=urgency,
                        reason=reason,
                    )
                )

        # Sort by urgency: critical first, then high, then medium
        priority_order = {"critical": 0, "high": 1, "medium": 2, "low": 3}
        recommendations.sort(key=lambda x: priority_order.get(x.urgency, 99))
        return recommendations

def get_stockout_risks() -> List[StockoutRiskItem]:
    """Identify inventory facing imminent stock depletion based on sales velocity and lead times."""
    with get_db() as conn:
        rows = fetch_all(
            conn,
            """
            SELECT 
                i.product_id, p.name as product_name, p.sku,
                i.store_id, st.name as store_name,
                i.quantity_on_hand, i.quantity_reserved,
                COALESCE(ps.lead_time_days, 7) as lead_time_days
            FROM inventory i
            JOIN products p ON i.product_id = p.product_id
            JOIN stores st ON i.store_id = st.store_id
            LEFT JOIN product_suppliers ps ON p.product_id = ps.product_id AND ps.is_preferred = 1
            WHERE p.is_active = 1
            """
        )

        demand_rows = fetch_all(
            conn,
            """
            SELECT store_id, product_id, COALESCE(SUM(quantity), 0) / 30.0 as avg_daily
            FROM sale_items si
            JOIN sales s ON si.sale_id = s.sale_id
            WHERE s.sale_date >= date('now', '-30 days')
            GROUP BY store_id, product_id
            """
        )
        d_map = {(d["store_id"], d["product_id"]): d["avg_daily"] for d in demand_rows}

        risks = []
        for r in rows:
            q_avail = r["quantity_on_hand"] - r["quantity_reserved"]
            avg_d = d_map.get((r["store_id"], r["product_id"]), 0.8)
            lead_t = r["lead_time_days"]
            days_rem = round(q_avail / avg_d, 1) if avg_d > 0 else 999.0

            if q_avail <= 0:
                level = "critical"
                exp = "Item is currently out of stock. Customers cannot make purchases."
            elif days_rem < lead_t:
                level = "critical"
                exp = f"Only {days_rem} days of coverage left, which is less than supplier lead time ({lead_t} days). Stockout guaranteed before delivery."
            elif days_rem < (2 * lead_t):
                level = "high"
                exp = f"{days_rem} days of coverage remaining. Below safe buffer levels."
            elif days_rem < (3 * lead_t):
                level = "medium"
                exp = f"{days_rem} days of coverage remaining. Approaching replenishment window."
            else:
                continue

            risks.append(
                StockoutRiskItem(
                    product_id=r["product_id"],
                    product_name=r["product_name"],
                    sku=r["sku"],
                    store_id=r["store_id"],
                    store_name=r["store_name"],
                    current_stock=q_avail,
                    average_daily_demand=round(avg_d, 2),
                    lead_time_days=lead_t,
                    days_of_stock_remaining=days_rem if days_rem < 900 else None,
                    risk_level=level,
                    explanation=exp,
                )
            )

        risks.sort(key=lambda x: (x.days_of_stock_remaining if x.days_of_stock_remaining is not None else 0))
        return risks

def calculate_store_allocation(req: StoreAllocationRequest) -> StoreAllocationResponse:
    """Distribute limited central stock across stores using multi-factor priority scoring."""
    with get_db() as conn:
        prod = fetch_one(conn, "SELECT name FROM products WHERE product_id = ?", (req.product_id,))
        p_name = prod["name"] if prod else "Selected Product"

        store_inventories = fetch_all(
            conn,
            """
            SELECT i.store_id, s.name as store_name, i.quantity_on_hand, i.reorder_point
            FROM inventory i
            JOIN stores s ON i.store_id = s.store_id
            WHERE i.product_id = ?
            """,
            (req.product_id,),
        )

        demand_rows = fetch_all(
            conn,
            """
            SELECT store_id, COALESCE(SUM(quantity), 0) / 30.0 as avg_daily
            FROM sale_items si JOIN sales s ON si.sale_id = s.sale_id
            WHERE si.product_id = ? AND s.sale_date >= date('now', '-30 days')
            GROUP BY store_id
            """,
            (req.product_id,),
        )
        d_map = {d["store_id"]: max(d["avg_daily"] * 30.0, 10.0) for d in demand_rows}

        total_demand = sum(d_map.get(si["store_id"], 15.0) for si in store_inventories)
        total_avail = req.total_quantity_available

        allocations = []
        allocated_so_far = 0

        # Calculate scores and proportional targets
        for idx, si in enumerate(store_inventories):
            st_id = si["store_id"]
            st_demand = d_map.get(st_id, 15.0)
            cur_stock = si["quantity_on_hand"]

            if req.allocation_rule == "priority":
                # Priority: favors stores with lowest current stock relative to demand
                stock_ratio = max(cur_stock / max(st_demand, 1.0), 0.1)
                priority_score = round(min(1.0 / (1.0 + stock_ratio), 1.0), 3)
            else:
                # Proportional
                priority_score = round(st_demand / max(total_demand, 1.0), 3)

            # Allocation share
            if idx == len(store_inventories) - 1:
                alloc_qty = max(total_avail - allocated_so_far, 0)
            else:
                alloc_qty = int(round(total_avail * (st_demand / max(total_demand, 1.0))))
                alloc_qty = min(alloc_qty, total_avail - allocated_so_far)
                allocated_so_far += alloc_qty

            shortage = max(st_demand - (cur_stock + alloc_qty), 0.0)
            reason = f"Allocated based on 30-day forecast ({int(st_demand)} units) and priority score {priority_score}."

            allocations.append(
                StoreAllocationLine(
                    store_id=st_id,
                    store_name=si["store_name"],
                    current_stock=cur_stock,
                    forecast_demand=round(st_demand, 1),
                    priority_score=priority_score,
                    allocated_quantity=alloc_qty,
                    shortage_after_allocation=round(shortage, 1),
                    reason=reason,
                )
            )

        total_allocated = sum(a.allocated_quantity for a in allocations)
        return StoreAllocationResponse(
            product_id=req.product_id,
            product_name=p_name,
            total_quantity_available=total_avail,
            allocation_rule=req.allocation_rule,
            dry_run=req.dry_run,
            allocations=allocations,
            total_allocated=total_allocated,
        )

def simulate_what_if(req: WhatIfSimulationRequest) -> WhatIfSimulationResponse:
    """Run an isolated, mathematical scenario simulation without altering any database records."""
    with get_db() as conn:
        st_row = fetch_one(conn, "SELECT name FROM stores WHERE store_id = ?", (req.store_id,))
        p_row = fetch_one(conn, "SELECT name, unit_cost, unit_price FROM products WHERE product_id = ?", (req.product_id,))
        inv_row = fetch_one(
            conn,
            "SELECT quantity_on_hand FROM inventory WHERE store_id = ? AND product_id = ?",
            (req.store_id, req.product_id),
        )
        sup_row = fetch_one(
            conn,
            "SELECT lead_time_days FROM product_suppliers WHERE product_id = ? AND is_preferred = 1",
            (req.product_id,),
        )

        st_name = st_row["name"] if st_row else "Selected Store"
        p_name = p_row["name"] if p_row else "Selected Product"
        cur_stock = inv_row["quantity_on_hand"] if inv_row else 40
        base_lead = sup_row["lead_time_days"] if sup_row else 7

        effective_stock = req.simulated_available_stock if req.simulated_available_stock is not None else cur_stock
        sim_lead = max(base_lead + req.lead_time_change_days, 1)

        # Baseline demand
        base_daily = 3.5
        sim_daily = round(base_daily * (1.0 + (req.demand_growth_pct / 100.0)), 2)

        proj_demand_30d = round(sim_daily * 30.0, 1)
        shortage = max(int(math.ceil(proj_demand_30d - effective_stock)), 0)

        # Risk comparison
        days_coverage = effective_stock / max(sim_daily, 0.1)
        if days_coverage < sim_lead:
            sim_risk = "Critical"
        elif days_coverage < (sim_lead * 2):
            sim_risk = "High"
        else:
            sim_risk = "Low"

        base_coverage = cur_stock / base_daily
        base_risk = "Critical" if base_coverage < base_lead else ("High" if base_coverage < (base_lead * 2) else "Low")

        rec_buffer = int(math.ceil(sim_daily * sim_lead * 1.5))
        revenue_impact = round(shortage * (p_row["unit_price"] if p_row else 20.0), 2)

        insights = [
            f"Under a {req.demand_growth_pct:+.1f}% demand shift, daily consumption moves to {sim_daily} units/day.",
            f"With lead time adjusted to {sim_lead} days, stock coverage lasts {days_coverage:.1f} days.",
            f"Potential lost revenue from stockout shortage is estimated at ₹{revenue_impact:,.2f}.",
            f"Recommended safety buffer under this scenario is {rec_buffer} units.",
        ]

        return WhatIfSimulationResponse(
            store_name=st_name,
            product_name=p_name,
            baseline_daily_demand=base_daily,
            simulated_daily_demand=sim_daily,
            baseline_lead_time_days=base_lead,
            simulated_lead_time_days=sim_lead,
            current_stock=cur_stock,
            effective_stock=effective_stock,
            projected_demand_30d=proj_demand_30d,
            projected_shortage_units=shortage,
            baseline_stockout_risk=base_risk,
            simulated_stockout_risk=sim_risk,
            recommended_safety_buffer=rec_buffer,
            estimated_revenue_impact=revenue_impact,
            insights=insights,
        )
