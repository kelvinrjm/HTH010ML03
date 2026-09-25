export interface User {
  user_id: number;
  username: string;
  role: 'admin' | 'staff' | 'manager';
  is_active: number;
  created_at?: string;
}

export interface AuthResponse {
  access_token: string;
  token_type: string;
  user: User;
}

export interface DashboardKPIs {
  total_inventory_value: number;
  today_sales: number;
  monthly_sales: number;
  estimated_monthly_profit: number;
  low_stock_count: number;
  stockout_risk_count: number;
  dead_stock_count: number;
  forecast_accuracy_mape: number | null;
  forecast_model_status: string;
}

export interface SalesTrendPoint {
  date: string;
  revenue: number;
  units_sold: number;
  profit: number;
}

export interface InventoryHealthCategory {
  category: string;
  product_count: number;
  total_units: number;
  total_value: number;
  percentage_of_inventory: number;
}

export interface TopProductItem {
  product_id: number;
  name: string;
  sku: string;
  category_name: string;
  units_sold: number;
  revenue: number;
  profit_margin: number;
}

export interface SlowMovingItem {
  product_id: number;
  name: string;
  sku: string;
  store_name: string;
  quantity_on_hand: number;
  days_without_sale: number;
  estimated_value_at_risk: number;
  recommended_action: string;
}

export interface StorePerformanceItem {
  store_id: number;
  store_name: string;
  store_type: string;
  total_revenue: number;
  total_inventory_value: number;
  active_products: number;
  low_stock_items: number;
}

export interface AIInsightItem {
  id: string;
  type: string;
  title: string;
  product_name?: string;
  store_name?: string;
  description: string;
  why_it_matters: string;
  supporting_metrics: Record<string, any>;
  recommended_action: string;
  urgency: 'critical' | 'high' | 'medium' | 'info';
}

export interface RecentActivityItem {
  id: string;
  activity_type: string;
  title: string;
  details: string;
  timestamp: string;
  actor: string;
}

export interface DashboardOverview {
  kpis: DashboardKPIs;
  sales_trend: SalesTrendPoint[];
  inventory_health: InventoryHealthCategory[];
  top_products: TopProductItem[];
  slow_moving: SlowMovingItem[];
  store_performance: StorePerformanceItem[];
  ai_insights: AIInsightItem[];
  recent_activity: RecentActivityItem[];
}

export interface InventoryItem {
  inventory_id: number;
  store_id: number;
  store_name: string;
  product_id: number;
  product_name: string;
  sku: string;
  category_name: string;
  unit_cost: number;
  unit_price: number;
  quantity_on_hand: number;
  quantity_reserved: number;
  quantity_available: number;
  quantity_on_order: number;
  reorder_point: number;
  reorder_quantity: number;
  days_of_stock: number | null;
  stock_health_status: 'critical' | 'low' | 'healthy' | 'overstock' | 'dead_stock';
  inventory_value: number;
  last_updated: string;
}

export interface Product {
  product_id: number;
  sku: string;
  name: string;
  description?: string;
  category_id: number;
  category_name?: string;
  unit_of_measure: string;
  unit_cost: number;
  unit_price: number;
  is_active: number;
  total_stock_on_hand?: number;
  total_inventory_value?: number;
  profit_margin?: number;
  stock_status?: string;
}

export interface SaleItem {
  sale_item_id: number;
  product_id: number;
  product_name: string;
  sku: string;
  quantity: number;
  unit_price: number;
  subtotal: number;
}

export interface Sale {
  sale_id: number;
  store_id: number;
  store_name: string;
  customer_id?: number;
  customer_name?: string;
  sale_date: string;
  total_amount: number;
  channel: string;
  created_at: string;
  items: SaleItem[];
}

export interface PurchaseItem {
  purchase_item_id: number;
  product_id: number;
  product_name: string;
  sku: string;
  quantity_ordered: number;
  quantity_received: number;
  unit_cost: number;
  subtotal: number;
}

export interface Purchase {
  purchase_id: number;
  store_id: number;
  store_name: string;
  supplier_id: number;
  supplier_name: string;
  order_date: string;
  expected_delivery_date: string;
  status: 'draft' | 'submitted' | 'received' | 'cancelled';
  total_amount: number;
  created_at: string;
  items: PurchaseItem[];
}

export interface ReorderRecommendation {
  product_id: number;
  product_name: string;
  sku: string;
  category_name: string;
  store_id: number;
  store_name: string;
  supplier_id?: number;
  supplier_name?: string;
  current_stock: number;
  average_daily_demand: number;
  lead_time_days: number;
  lead_time_demand: number;
  safety_stock: number;
  reorder_point: number;
  eoq: number;
  moq: number;
  recommended_reorder_qty: number;
  estimated_cost: number;
  urgency: 'critical' | 'high' | 'medium' | 'low';
  reason: string;
}

export interface StockoutRisk {
  product_id: number;
  product_name: string;
  sku: string;
  store_id: number;
  store_name: string;
  current_stock: number;
  average_daily_demand: number;
  lead_time_days: number;
  days_of_stock_remaining: number | null;
  risk_level: 'critical' | 'high' | 'medium' | 'low';
  explanation: string;
}

export interface ForecastPoint {
  date: string;
  actual: number | null;
  predicted: number | null;
  lower_bound: number | null;
  upper_bound: number | null;
}

export interface ForecastData {
  store_id: number;
  store_name: string;
  product_id: number;
  product_name: string;
  sku: string;
  horizon_days: number;
  current_stock: number;
  total_predicted_demand: number;
  average_daily_demand: number;
  stockout_risk: string;
  recommended_reorder: number;
  model_version: string;
  model_type: string;
  confidence_level: number;
  series: ForecastPoint[];
}

export interface Store {
  store_id: number;
  name: string;
  location: string;
  store_type: string;
}

export interface Supplier {
  supplier_id: number;
  name: string;
  contact_info: string;
  default_lead_time_days: number;
}
