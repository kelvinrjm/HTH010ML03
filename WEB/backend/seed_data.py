"""STOCKSENSE High-Fidelity Dataset Generator.

Populates a realistic, production-scale retail dataset:
- 105 realistic SKUs across 6 diverse retail/wholesale categories
- 4 stores with distinct customer demographics and scale
- 8 suppliers with accurate lead times (3 to 30 days) and minimum order quantities
- 24 months of historical daily sales with genuine retail dynamics:
    * Summer beverage surges
    * Monsoon rain gear spikes
    * Holiday & festive peak shopping weeks
    * Weekend shopping lifts
    * Fast-moving staples vs. slow-moving luxury vs. dead-stock items
- Multi-store inventory records with realistic stock, reserved amounts, and reorder levels
- Purchase orders across status lifecycles (received, submitted, draft)
- Seed users: admin (Admin@123) and staff (Staff@123)
"""

import json
import math
import random
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Tuple

from backend.auth.security import hash_password
from backend.database import get_db, init_db
from smartstock.db.operations import fetch_one, insert_record

CATEGORIES = [
    {"name": "Pantry & Groceries", "parent": None},
    {"name": "Dairy & Fresh Foods", "parent": None},
    {"name": "Beverages & Refreshments", "parent": None},
    {"name": "Personal Care & Hygiene", "parent": None},
    {"name": "Household & Cleaning", "parent": None},
    {"name": "Electronics & Accessories", "parent": None},
]

STORES = [
    {"name": "Downtown Central Flagship", "location": "Metropolis Mall, City Center", "store_type": "retail"},
    {"name": "Metro Suburban Hypermarket", "location": "West End Commercial Avenue", "store_type": "retail"},
    {"name": "Regional Distribution Hub", "location": "North Logistics Park, Industrial Zone", "store_type": "wholesale"},
    {"name": "Express Transit Hub", "location": "Airport Terminal 2 Concourse", "store_type": "retail"},
]

SUPPLIERS = [
    {"name": "AgroGlobal Staples Ltd", "contact": "orders@agroglobal.com", "lead_time": 7},
    {"name": "PureDairy Farms Co-op", "contact": "supply@puredairy.com", "lead_time": 3},
    {"name": "HydraBeverage Distribution", "contact": "logistics@hydrabev.com", "lead_time": 5},
    {"name": "NaturaLife Personal Care", "contact": "wholesale@naturalife.com", "lead_time": 10},
    {"name": "CleanPro Industrial Solutions", "contact": "contact@cleanpro.com", "lead_time": 8},
    {"name": "NextGen Electronics Tech", "contact": "b2b@nextgentech.com", "lead_time": 18},
    {"name": "Sunrise Organic Imports", "contact": "imports@sunriseorganic.com", "lead_time": 21},
    {"name": "Premier Packaged Goods Corp", "contact": "orders@premierpkg.com", "lead_time": 6},
]

# 105 Realistic Product Specifications:
# (sku, name, category_idx, unit, unit_cost, unit_price, supplier_idx, demand_type)
# demand_type: 'staple_fast', 'staple_normal', 'summer_seasonal', 'rain_seasonal', 'festive_seasonal', 'slow_moving', 'dead_stock_candidate'
PRODUCT_DEFS: List[Tuple[str, str, int, str, float, float, int, str]] = [
    # Category 0: Pantry & Groceries (22 products)
    ("PAN-001", "Basmati Rice Royal Harvest 5kg", 0, "bag", 14.50, 22.99, 0, "staple_fast"),
    ("PAN-002", "Whole Wheat Flour Stoneground 10kg", 0, "bag", 9.20, 15.49, 0, "staple_fast"),
    ("PAN-003", "Refined Sunflower Cooking Oil 5L", 0, "bottle", 11.00, 17.50, 0, "staple_fast"),
    ("PAN-004", "Extra Virgin Olive Oil 1L", 0, "bottle", 8.80, 14.99, 6, "staple_normal"),
    ("PAN-005", "Organic Brown Cane Sugar 1kg", 0, "pack", 1.80, 3.49, 0, "staple_normal"),
    ("PAN-006", "Iodized Table Salt 1kg", 0, "pack", 0.40, 0.99, 0, "staple_fast"),
    ("PAN-007", "Red Lentils Masoor Dal 1kg", 0, "pack", 1.90, 3.75, 0, "staple_fast"),
    ("PAN-008", "Chickpeas Garbanzo 1kg", 0, "pack", 2.10, 4.20, 0, "staple_normal"),
    ("PAN-009", "Premium Pasta Penne Rigate 500g", 0, "box", 1.20, 2.79, 7, "staple_normal"),
    ("PAN-010", "Italian Pasta Spaghetti No.5 500g", 0, "pack", 1.15, 2.65, 7, "staple_normal"),
    ("PAN-011", "Organic Rolled Oats 1kg", 0, "box", 2.40, 4.99, 6, "staple_normal"),
    ("PAN-012", "Roasted Peanut Butter Crunchy 500g", 0, "jar", 2.60, 5.25, 7, "staple_normal"),
    ("PAN-013", "Pure Wildflower Honey 500g", 0, "jar", 3.80, 7.99, 6, "staple_normal"),
    ("PAN-014", "Ground Arabica Coffee Dark Roast 250g", 0, "pack", 4.20, 8.50, 6, "staple_normal"),
    ("PAN-015", "English Breakfast Black Tea 100 Bags", 0, "box", 3.10, 6.49, 7, "staple_normal"),
    ("PAN-016", "Tomato Paste Puree Cans 400g", 0, "can", 0.70, 1.49, 7, "staple_fast"),
    ("PAN-017", "Sweet Corn Kernels 340g", 0, "can", 0.85, 1.80, 7, "staple_normal"),
    ("PAN-018", "Organic Quinoa Grain 500g", 0, "pack", 3.50, 7.20, 6, "slow_moving"),
    ("PAN-019", "Himalayan Pink Rock Salt Grinder 200g", 0, "bottle", 2.80, 5.99, 6, "slow_moving"),
    ("PAN-020", "Artisan Truffle Mustard Sauce 180g", 0, "jar", 6.50, 14.99, 6, "dead_stock_candidate"),
    ("PAN-021", "Spicy Sriracha Chili Sauce 450ml", 0, "bottle", 2.10, 4.50, 7, "staple_normal"),
    ("PAN-022", "Organic Chia Seeds Superfood 300g", 0, "pack", 2.90, 6.30, 6, "slow_moving"),

    # Category 1: Dairy & Fresh Foods (18 products)
    ("DAI-001", "Farm Fresh Pasteurized Whole Milk 1L", 1, "carton", 1.10, 1.89, 1, "staple_fast"),
    ("DAI-002", "Low Fat Skimmed Milk 1L", 1, "carton", 1.05, 1.85, 1, "staple_fast"),
    ("DAI-003", "Greek Style Plain Yogurt 500g", 1, "tub", 1.60, 3.29, 1, "staple_normal"),
    ("DAI-004", "Strawberry Flavored Bio Yogurt 400g", 1, "tub", 1.80, 3.49, 1, "staple_normal"),
    ("DAI-005", "Unsalted Pure Creamery Butter 250g", 1, "block", 1.95, 3.89, 1, "staple_fast"),
    ("DAI-006", "Salted Butter Spread 250g", 1, "tub", 2.05, 3.99, 1, "staple_normal"),
    ("DAI-007", "Mild Cheddar Cheese Block 400g", 1, "block", 3.40, 6.75, 1, "staple_normal"),
    ("DAI-008", "Shredded Mozzarella Cheese 250g", 1, "pack", 2.20, 4.50, 1, "staple_fast"),
    ("DAI-009", "Fresh Cottage Cheese Paneer 200g", 1, "pack", 1.70, 3.25, 1, "staple_fast"),
    ("DAI-010", "Whipped Heavy Dairy Cream 250ml", 1, "carton", 1.40, 2.90, 1, "staple_normal"),
    ("DAI-011", "Free Range Grade A Eggs 12pk", 1, "carton", 2.20, 4.19, 1, "staple_fast"),
    ("DAI-012", "Organic Omega-3 Enriched Eggs 6pk", 1, "carton", 1.80, 3.40, 1, "staple_normal"),
    ("DAI-013", "Swiss Emmental Cheese Slices 150g", 1, "pack", 2.90, 5.80, 1, "slow_moving"),
    ("DAI-014", "French Brie Soft Artisan Cheese 125g", 1, "wheel", 4.20, 8.99, 1, "slow_moving"),
    ("DAI-015", "Gourmet Blue Stilton Cheese 100g", 1, "wedge", 5.00, 11.50, 1, "dead_stock_candidate"),
    ("DAI-016", "Vanilla Custard Cream Dessert 500g", 1, "tub", 1.50, 3.10, 1, "staple_normal"),
    ("DAI-017", "Plant-Based Almond Milk Unsweetened 1L", 1, "carton", 1.90, 3.75, 6, "staple_normal"),
    ("DAI-018", "Oat Barista Milk Blend 1L", 1, "carton", 2.10, 4.10, 6, "staple_normal"),

    # Category 2: Beverages & Refreshments (18 products)
    ("BEV-001", "Natural Spring Mineral Water 1.5L", 2, "bottle", 0.35, 0.99, 2, "summer_seasonal"),
    ("BEV-002", "Spring Water Multi-pack 6x500ml", 2, "pack", 1.40, 3.29, 2, "summer_seasonal"),
    ("BEV-003", "Sparkling Lemon Water 750ml", 2, "bottle", 0.90, 2.19, 2, "summer_seasonal"),
    ("BEV-004", "Cold Brew Unsweetened Black Coffee 330ml", 2, "can", 1.40, 3.49, 2, "summer_seasonal"),
    ("BEV-005", "Classic Cola 2L Bottle", 2, "bottle", 1.10, 2.49, 2, "staple_fast"),
    ("BEV-006", "Diet Zero Sugar Cola 12x330ml", 2, "case", 4.80, 9.99, 2, "summer_seasonal"),
    ("BEV-007", "Pure Orange Juice No Pulp 1L", 2, "carton", 1.80, 3.75, 2, "staple_fast"),
    ("BEV-008", "Fresh Pressed Apple Cider Juice 1L", 2, "bottle", 2.20, 4.50, 2, "staple_normal"),
    ("BEV-009", "Isotonic Energy Sports Drink Berry 500ml", 2, "bottle", 1.10, 2.49, 2, "summer_seasonal"),
    ("BEV-010", "Electrolyte Hydration Drink Orange 500ml", 2, "bottle", 1.15, 2.59, 2, "summer_seasonal"),
    ("BEV-011", "Organic Green Tea Citrus Bottle 400ml", 2, "bottle", 1.25, 2.89, 2, "staple_normal"),
    ("BEV-012", "Ginger Ale Craft Soda 4x330ml", 2, "pack", 2.50, 5.49, 2, "staple_normal"),
    ("BEV-013", "Coconut Water 100% Pure 330ml", 2, "tetra", 1.10, 2.60, 2, "summer_seasonal"),
    ("BEV-014", "Holiday Spiced Cranberry Punch 1L", 2, "bottle", 2.80, 6.50, 2, "festive_seasonal"),
    ("BEV-015", "Sparkling Apple Celebration Cider 750ml", 2, "bottle", 3.20, 7.99, 2, "festive_seasonal"),
    ("BEV-016", "Artisan Kombucha Passionfruit 300ml", 2, "bottle", 2.40, 5.20, 6, "slow_moving"),
    ("BEV-017", "Elderflower Rose Cordial 500ml", 2, "bottle", 4.50, 10.99, 6, "dead_stock_candidate"),
    ("BEV-018", "Tonic Water Indian Quinine 1L", 2, "bottle", 0.95, 2.20, 2, "staple_normal"),

    # Category 3: Personal Care & Hygiene (16 products)
    ("PER-001", "Hydrating Shampoo Argan Oil 400ml", 3, "bottle", 2.90, 6.49, 3, "staple_normal"),
    ("PER-002", "Deep Conditioner Keratin Repair 350ml", 3, "bottle", 3.10, 6.89, 3, "staple_normal"),
    ("PER-003", "Antibacterial Body Wash Citrus 500ml", 3, "bottle", 2.40, 5.29, 3, "staple_fast"),
    ("PER-004", "Moisturizing Beauty Bar Soap 4x100g", 3, "pack", 1.60, 3.49, 3, "staple_fast"),
    ("PER-005", "Toothpaste Total Dental Protection 150g", 3, "tube", 1.40, 3.20, 3, "staple_fast"),
    ("PER-006", "Bamboo Soft Bristle Toothbrush 4pk", 3, "pack", 2.10, 4.99, 3, "staple_normal"),
    ("PER-007", "Antiperspirant Deodorant Roll-on 50ml", 3, "bottle", 1.80, 3.99, 3, "summer_seasonal"),
    ("PER-008", "Mineral Sunscreen Lotion SPF 50+ 150ml", 3, "tube", 5.20, 12.99, 3, "summer_seasonal"),
    ("PER-009", "After-Sun Cooling Aloe Gel 200ml", 3, "bottle", 3.10, 7.49, 3, "summer_seasonal"),
    ("PER-010", "Triple Blade Disposable Razors 8pk", 3, "pack", 3.40, 7.50, 3, "staple_normal"),
    ("PER-011", "Hand Sanitizer Gel Pump 500ml", 3, "bottle", 1.80, 3.99, 3, "staple_normal"),
    ("PER-012", "Gentle Cleansing Facial Foam 150ml", 3, "pump", 3.80, 8.50, 3, "staple_normal"),
    ("PER-013", "Intense Moisture Body Butter 250g", 3, "tub", 4.20, 9.75, 3, "staple_normal"),
    ("PER-014", "Holiday Luxury Bath & Spa Gift Set", 3, "box", 12.50, 29.99, 3, "festive_seasonal"),
    ("PER-015", "Exfoliating Volcanic Mud Foot Mask", 3, "pack", 3.80, 8.90, 3, "dead_stock_candidate"),
    ("PER-016", "Sensitive Skin Baby Wipes 80ct", 3, "pack", 1.40, 2.99, 3, "staple_fast"),

    # Category 4: Household & Cleaning (16 products)
    ("HOU-001", "Ultra Liquid Laundry Detergent 3L", 4, "bottle", 6.80, 13.99, 4, "staple_fast"),
    ("HOU-002", "Concentrated Fabric Softener Spring 2L", 4, "bottle", 3.20, 6.99, 4, "staple_normal"),
    ("HOU-003", "Antibacterial Dishwashing Liquid 1L", 4, "bottle", 1.40, 2.99, 4, "staple_fast"),
    ("HOU-004", "Heavy Duty Sponge Scrubbers 6pk", 4, "pack", 1.10, 2.49, 4, "staple_fast"),
    ("HOU-005", "Multi-surface Floor Cleaner Pine 1.5L", 4, "bottle", 2.20, 4.75, 4, "staple_normal"),
    ("HOU-006", "Bathroom Bleach Power Gel 750ml", 4, "bottle", 1.60, 3.49, 4, "staple_fast"),
    ("HOU-007", "Glass Cleaner Shine Trigger Spray 500ml", 4, "spray", 1.30, 2.89, 4, "staple_normal"),
    ("HOU-008", "Kitchen Paper Towels 2-Ply 4 Rolls", 4, "pack", 2.40, 4.99, 4, "staple_fast"),
    ("HOU-009", "Ultra Soft Toilet Paper 3-Ply 12 Rolls", 4, "pack", 4.50, 9.49, 4, "staple_fast"),
    ("HOU-010", "Heavy Duty Garbage Trash Bags 50L 30ct", 4, "roll", 2.10, 4.49, 4, "staple_fast"),
    ("HOU-011", "Mosquito Vaporizer Refill Twin Pack", 4, "pack", 2.60, 5.80, 4, "rain_seasonal"),
    ("HOU-012", "Compact Auto Open Windproof Umbrella", 4, "unit", 4.80, 12.99, 4, "rain_seasonal"),
    ("HOU-013", "Disposable Raincoat Emergency Poncho 2pk", 4, "pack", 1.20, 3.50, 4, "rain_seasonal"),
    ("HOU-014", "Holiday Pine Scented Decorative Candle", 4, "unit", 3.40, 8.50, 4, "festive_seasonal"),
    ("HOU-015", "Specialty Silverware Polishing Cream 250ml", 4, "jar", 5.10, 12.00, 4, "dead_stock_candidate"),
    ("HOU-016", "Microfiber Dusting Cloths Pack of 4", 4, "pack", 1.90, 4.25, 4, "staple_normal"),

    # Category 5: Electronics & Accessories (15 products)
    ("ELE-001", "Fast Charging USB-C to USB-C Cable 2m", 5, "unit", 3.20, 9.99, 5, "staple_fast"),
    ("ELE-002", "Braided Lightning iPhone Cable 1.5m", 5, "unit", 3.50, 11.49, 5, "staple_fast"),
    ("ELE-003", "Dual USB-C 30W Fast Wall Charger", 5, "unit", 6.80, 18.99, 5, "staple_normal"),
    ("ELE-004", "Magnetic Wireless Power Bank 10000mAh", 5, "unit", 14.50, 34.99, 5, "staple_normal"),
    ("ELE-005", "Wireless Bluetooth In-Ear Earbuds", 5, "pair", 12.00, 29.99, 5, "staple_normal"),
    ("ELE-006", "Noise Cancelling Over-Ear Headphones", 5, "unit", 35.00, 79.99, 5, "festive_seasonal"),
    ("ELE-007", "Portable Waterproof Bluetooth Speaker", 5, "unit", 16.00, 39.99, 5, "summer_seasonal"),
    ("ELE-008", "AAA Alkaline Long Life Batteries 8pk", 5, "pack", 2.20, 5.49, 5, "staple_fast"),
    ("ELE-009", "AA Heavy Duty Batteries 8pk", 5, "pack", 2.40, 5.89, 5, "staple_fast"),
    ("ELE-010", "Universal Travel Plug Adapter with USB", 5, "unit", 5.50, 14.99, 5, "summer_seasonal"),
    ("ELE-011", "Ergonomic Silent Wireless Computer Mouse", 5, "unit", 7.20, 19.99, 5, "staple_normal"),
    ("ELE-012", "LED Ring Light with Phone Tripod Stand", 5, "unit", 11.00, 26.50, 5, "slow_moving"),
    ("ELE-013", "Holiday LED Copper Wire Fairy Lights 10m", 5, "unit", 2.90, 8.99, 5, "festive_seasonal"),
    ("ELE-014", "Vintage Mechanical Alarm Clock Retro", 5, "unit", 9.80, 24.00, 5, "dead_stock_candidate"),
    ("ELE-015", "Smart Home WiFi Plug Energy Monitor", 5, "unit", 8.40, 21.99, 5, "staple_normal"),
]


def seed_database():
    """Execute complete dataset seeding."""
    print("Initiating STOCKSENSE comprehensive dataset seeding...")
    init_db()

    with get_db() as conn:
        # Check if already seeded (e.g. products count >= 100)
        row = fetch_one(conn, "SELECT COUNT(*) as cnt FROM products")
        if row and row["cnt"] >= 100:
            print(f"Database already populated with {row['cnt']} products. Seeding skipped.")
            return

        now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

        # 1. Seed Users
        admin_pass = hash_password("Admin@123")
        staff_pass = hash_password("Staff@123")
        conn.execute(
            "INSERT OR IGNORE INTO users (username, password_hash, role, is_active, created_at) VALUES (?, ?, ?, 1, ?)",
            ("admin", admin_pass, "admin", now_utc),
        )
        conn.execute(
            "INSERT OR IGNORE INTO users (username, password_hash, role, is_active, created_at) VALUES (?, ?, ?, 1, ?)",
            ("staff", staff_pass, "staff", now_utc),
        )

        # 2. Seed Categories
        category_ids: List[int] = []
        for cat in CATEGORIES:
            cur = conn.execute(
                "INSERT INTO categories (name, parent_category_id, created_at) VALUES (?, NULL, ?)",
                (cat["name"], now_utc),
            )
            category_ids.append(cur.lastrowid)

        # 3. Seed Stores
        store_ids: List[int] = []
        for st in STORES:
            cur = conn.execute(
                "INSERT INTO stores (name, location, store_type, is_active, created_at) VALUES (?, ?, ?, 1, ?)",
                (st["name"], st["location"], st["store_type"], now_utc),
            )
            store_ids.append(cur.lastrowid)

        # 4. Seed Suppliers
        supplier_ids: List[int] = []
        for sup in SUPPLIERS:
            cur = conn.execute(
                "INSERT INTO suppliers (name, contact_info, default_lead_time_days, is_active, created_at) VALUES (?, ?, ?, 1, ?)",
                (sup["name"], sup["contact"], sup["lead_time"], now_utc),
            )
            supplier_ids.append(cur.lastrowid)

        # 5. Seed Products & Supplier Links
        product_meta: List[dict] = []
        for p in PRODUCT_DEFS:
            sku, name, cat_idx, uom, cost, price, sup_idx, demand_type = p
            cat_id = category_ids[cat_idx]
            sup_id = supplier_ids[sup_idx]

            cur = conn.execute(
                """
                INSERT INTO products (sku, name, description, category_id, unit_of_measure, unit_cost, unit_price, is_active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)
                """,
                (sku, name, f"Standard commercial grade {name}", cat_id, uom, cost, price, now_utc),
            )
            prod_id = cur.lastrowid

            lead_days = SUPPLIERS[sup_idx]["lead_time"]
            moq = 10 if cost < 5.0 else 5
            conn.execute(
                """
                INSERT INTO product_suppliers (product_id, supplier_id, supplier_sku, lead_time_days, minimum_order_quantity, is_preferred, created_at)
                VALUES (?, ?, ?, ?, ?, 1, ?)
                """,
                (prod_id, sup_id, f"SUP-{sku}", lead_days, moq, now_utc),
            )

            product_meta.append({
                "product_id": prod_id,
                "sku": sku,
                "name": name,
                "cost": cost,
                "price": price,
                "lead_days": lead_days,
                "moq": moq,
                "demand_type": demand_type,
            })

        print(f"Created {len(product_meta)} products across {len(category_ids)} categories.")

        # 6. Seed Multi-Store Inventory
        for prod in product_meta:
            dtype = prod["demand_type"]
            for s_idx, s_id in enumerate(store_ids):
                # Wholesale store holds 3-4x volume, airport express holds less
                scale = 4.0 if s_idx == 2 else (0.6 if s_idx == 3 else 1.5)
                
                if dtype == "staple_fast":
                    on_hand = int(random.randint(60, 180) * scale)
                    reserved = int(random.randint(2, 10))
                    reorder_pt = int(40 * scale)
                    reorder_qty = int(80 * scale)
                elif dtype == "staple_normal":
                    on_hand = int(random.randint(30, 90) * scale)
                    reserved = int(random.randint(0, 5))
                    reorder_pt = int(25 * scale)
                    reorder_qty = int(50 * scale)
                elif dtype in ("summer_seasonal", "rain_seasonal", "festive_seasonal"):
                    on_hand = int(random.randint(20, 60) * scale)
                    reserved = int(random.randint(0, 3))
                    reorder_pt = int(20 * scale)
                    reorder_qty = int(40 * scale)
                elif dtype == "slow_moving":
                    on_hand = int(random.randint(15, 45) * scale)
                    reserved = 0
                    reorder_pt = int(10 * scale)
                    reorder_qty = int(20 * scale)
                else:  # dead_stock_candidate
                    on_hand = int(random.randint(25, 70) * scale)
                    reserved = 0
                    reorder_pt = 5
                    reorder_qty = 10

                conn.execute(
                    """
                    INSERT INTO inventory (store_id, product_id, quantity_on_hand, quantity_reserved, quantity_on_order, reorder_point, reorder_quantity, last_updated)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (s_id, prod["product_id"], on_hand, reserved, 0, reorder_pt, reorder_qty, now_utc),
                )

        # 7. Seed Customers
        customer_names = [
            ("Apex Food Stores", "procurement@apexfoods.com", "wholesale"),
            ("Metro Mart Central", "inventory@metromart.com", "wholesale"),
            ("Sara Jenkins", "sara.j@gmail.com", "retail"),
            ("David Kim", "david.kim@outlook.com", "retail"),
            ("Priya Patel", "priya.p@yahoo.com", "retail"),
            ("Michael Brown", "mbrown@corporate.net", "retail"),
            ("Zenith Cafe & Lounge", "supplies@zenithcafe.com", "wholesale"),
            ("Sunrise Bakery Chain", "purchasing@sunrisebakery.com", "wholesale"),
        ]
        customer_ids = []
        for name, email, ctype in customer_names:
            cur = conn.execute(
                "INSERT INTO customers (name, email, phone, address, created_at) VALUES (?, ?, '555-0192', 'Commercial Hub', ?)",
                (name, email, now_utc),
            )
            customer_ids.append(cur.lastrowid)

        # 8. Seed 24 Months of Realistic Historical Sales
        # Generating 730 days of data ending today
        end_date = datetime.now(timezone.utc).date()
        start_date = end_date - timedelta(days=730)
        
        print("Generating 24 months of daily transaction patterns (this establishes real time-series history)...")
        
        # We sample transactions per day to keep DB size fast yet rich
        # Sample days: every day for last 90 days, and every 2-3 days prior
        current_d = start_date
        total_sales_count = 0
        total_items_count = 0

        while current_d <= end_date:
            days_from_now = (end_date - current_d).days
            # Frequency: daily for recent 180 days, every other day for older to optimize speed
            skip = (days_from_now > 180 and current_d.day % 2 != 0)
            if not skip:
                month = current_d.month
                weekday = current_d.weekday() # 5, 6 = weekend
                is_weekend = weekday in (5, 6)
                
                # Seasonal multipliers
                is_summer = month in (5, 6, 7, 8)
                is_monsoon = month in (7, 8, 9)
                is_festive = month in (10, 11, 12)

                # Generate 4 to 8 transactions across stores for this date
                num_tx = random.randint(5, 10) if is_weekend else random.randint(3, 7)
                for _ in range(num_tx):
                    st_id = random.choice(store_ids)
                    cust_id = random.choice(customer_ids) if random.random() < 0.4 else None
                    channel = "wholesale" if st_id == store_ids[2] else ("online" if random.random() < 0.25 else "in-store")
                    
                    tx_date_str = current_d.strftime("%Y-%m-%d")
                    tx_time_str = f"{tx_date_str}T{random.randint(9, 20):02d}:{random.randint(0, 59):02d}:00Z"
                    
                    # Pick 1 to 4 products for this cart
                    cart_size = random.randint(1, 4)
                    selected_prods = random.sample(product_meta, cart_size)
                    
                    sale_total = 0.0
                    cart_items = []
                    
                    for prod in selected_prods:
                        dtype = prod["demand_type"]
                        base_qty = 1
                        
                        if dtype == "staple_fast":
                            base_qty = random.randint(1, 4)
                        elif dtype == "summer_seasonal":
                            base_qty = random.randint(2, 6) if is_summer else (random.randint(0, 1))
                        elif dtype == "rain_seasonal":
                            base_qty = random.randint(1, 3) if is_monsoon else (1 if random.random() < 0.08 else 0)
                        elif dtype == "festive_seasonal":
                            base_qty = random.randint(2, 5) if is_festive else (1 if random.random() < 0.05 else 0)
                        elif dtype == "slow_moving":
                            base_qty = 1 if random.random() < 0.2 else 0
                        elif dtype == "dead_stock_candidate":
                            # No sales in the last 120 days!
                            base_qty = 1 if days_from_now > 120 and random.random() < 0.15 else 0

                        if base_qty > 0:
                            line_subtotal = round(base_qty * prod["price"], 2)
                            sale_total += line_subtotal
                            cart_items.append((prod["product_id"], base_qty, prod["price"], line_subtotal))

                    if cart_items:
                        cur_sale = conn.execute(
                            "INSERT INTO sales (store_id, customer_id, sale_date, total_amount, channel, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                            (st_id, cust_id, tx_date_str, round(sale_total, 2), channel, tx_time_str),
                        )
                        s_id = cur_sale.lastrowid
                        total_sales_count += 1
                        
                        for pid, qty, uprice, subt in cart_items:
                            conn.execute(
                                "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                                (s_id, pid, qty, uprice, subt, tx_time_str),
                            )
                            total_items_count += 1

            current_d += timedelta(days=1)

        print(f"Generated {total_sales_count} sales transactions with {total_items_count} line items across 24 months.")

        # 9. Seed Purchases & Dead Stock Flags
        # Create some historical received POs and active draft/submitted POs
        po_dates = [
            ("2026-01-10", "2026-01-18", "received"),
            ("2026-02-01", "2026-02-10", "received"),
            ("2026-03-01", "2026-03-08", "received"),
            ("2026-09-15", "2026-09-25", "submitted"),
            ("2026-09-22", "2026-10-02", "draft"),
        ]
        for o_date, d_date, stat in po_dates:
            for s_id in store_ids[:2]:
                sup_id = random.choice(supplier_ids)
                p_cur = conn.execute(
                    "INSERT INTO purchases (store_id, supplier_id, order_date, expected_delivery_date, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    (s_id, sup_id, o_date, d_date, stat, f"{o_date}T08:00:00Z"),
                )
                p_id = p_cur.lastrowid
                
                # Add 3 lines
                for sample_prod in random.sample(product_meta, 3):
                    q_ord = sample_prod["moq"] * random.randint(2, 5)
                    q_rec = q_ord if stat == "received" else 0
                    conn.execute(
                        "INSERT INTO purchase_items (purchase_id, product_id, quantity_ordered, quantity_received, unit_cost, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        (p_id, sample_prod["product_id"], q_ord, q_rec, sample_prod["cost"], f"{o_date}T08:00:00Z"),
                    )

        # 10. Seed Initial Dead Stock Flags
        for p in product_meta:
            if p["demand_type"] == "dead_stock_candidate":
                for s_id in store_ids[:2]:
                    conn.execute(
                        """
                        INSERT INTO dead_stock_flags (store_id, product_id, flagged_at, days_without_sale, quantity_at_risk, estimated_value_at_risk, recommendation, resolution_notes)
                        VALUES (?, ?, ?, 125, 45, ?, 'discount', '')
                        """,
                        (s_id, p["product_id"], now_utc, round(45 * p["cost"], 2)),
                    )

        # 11. Initial Model Registry entry
        conn.execute(
            """
            INSERT INTO ml_model_registry (model_name, model_version, model_type, target, artefact_path, training_mae, training_rmse, training_r2, trained_at, is_active)
            VALUES ('XGBoost Demand Forecaster', 'v1.0.0', 'XGBRegressor', 'demand', 'models/xgboost_demand_v1.0.0.joblib', 2.34, 3.85, 0.88, ?, 1)
            """,
            (now_utc,),
        )

        conn.commit()
        print("STOCKSENSE database successfully seeded with full commercial dataset.")


if __name__ == "__main__":
    seed_database()
