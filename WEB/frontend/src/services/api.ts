import {
  AuthResponse,
  DashboardOverview,
  ForecastData,
  InventoryItem,
  Product,
  Purchase,
  ReorderRecommendation,
  Sale,
  StockoutRisk,
  Store,
  Supplier,
  User,
} from '../types';

const API_BASE = '/api';

export function getAuthToken(): string | null {
  return localStorage.getItem('stocksense_token');
}

export function setAuthToken(token: string): void {
  localStorage.setItem('stocksense_token', token);
}

export function clearAuthToken(): void {
  localStorage.removeItem('stocksense_token');
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    clearAuthToken();
    window.dispatchEvent(new Event('auth:unauthorized'));
    throw new Error('Session expired. Please log in again.');
  }

  if (!response.ok) {
    let errorDetail = 'Network request failed';
    try {
      const errorJson = await response.json();
      errorDetail = errorJson.detail || errorJson.message || errorDetail;
    } catch {
      errorDetail = await response.text();
    }
    throw new Error(errorDetail);
  }

  return response.json() as Promise<T>;
}

export const api = {
  // Auth
  login: (credentials: { username: string; password: string }) =>
    request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    }),
  getMe: () => request<User>('/auth/me'),

  // Dashboard
  getDashboardOverview: () => request<DashboardOverview>('/dashboard/overview'),

  // Products
  getProducts: (params?: { category_id?: number; search?: string }) => {
    const q = new URLSearchParams();
    if (params?.category_id) q.set('category_id', params.category_id.toString());
    if (params?.search) q.set('search', params.search);
    return request<Product[]>(`/products?${q.toString()}`);
  },
  getProduct: (id: number) => request<Product>(`/products/${id}`),
  createProduct: (data: Partial<Product>) =>
    request<{ status: string; product_id: number }>('/products', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Inventory
  getInventory: (params?: { store_id?: number; health_status?: string; search?: string }) => {
    const q = new URLSearchParams();
    if (params?.store_id) q.set('store_id', params.store_id.toString());
    if (params?.health_status) q.set('health_status', params.health_status);
    if (params?.search) q.set('search', params.search);
    return request<InventoryItem[]>(`/inventory?${q.toString()}`);
  },
  transferStock: (data: { from_store_id: number; to_store_id: number; product_id: number; quantity: number; notes?: string }) =>
    request<{ status: string; message: string }>('/inventory/transfer', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  adjustStock: (data: { store_id: number; product_id: number; new_quantity_on_hand: number; reason: string; notes?: string }) =>
    request<{ status: string; message: string }>('/inventory/adjust', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Sales
  getSales: (store_id?: number) => {
    const q = store_id ? `?store_id=${store_id}` : '';
    return request<Sale[]>(`/sales${q}`);
  },
  createSale: (data: { store_id: number; customer_id?: number; channel: string; items: { product_id: number; quantity: number; unit_price: number }[] }) =>
    request<Sale>('/sales', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Purchases
  getPurchases: (store_id?: number) => {
    const q = store_id ? `?store_id=${store_id}` : '';
    return request<Purchase[]>(`/purchases${q}`);
  },
  createPurchase: (data: { store_id: number; supplier_id: number; order_date: string; expected_delivery_date: string; items: { product_id: number; quantity_ordered: number; unit_cost: number }[] }) =>
    request<Purchase>('/purchases', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  receivePurchase: (id: number) =>
    request<{ status: string; message: string }>(`/purchases/${id}/receive`, {
      method: 'POST',
    }),

  // Stores & Suppliers
  getStores: () => request<Store[]>('/stores'),
  getSuppliers: () => request<Supplier[]>('/suppliers'),

  // Forecasting
  getForecast: (store_id: number, product_id: number, horizon_days: number = 30) =>
    request<ForecastData>(`/forecast?store_id=${store_id}&product_id=${product_id}&horizon_days=${horizon_days}`),
  getModelCenter: () => request<any>('/forecast/model-center'),
  retrainModel: () => request<any>('/forecast/retrain', { method: 'POST' }),

  // Smart Decisions
  getReorderRecommendations: () => request<ReorderRecommendation[]>('/smart-decisions/reorder'),
  getStockoutRisks: () => request<StockoutRisk[]>('/smart-decisions/stockout-risk'),
  calculateAllocation: (data: { product_id: number; total_quantity_available: number; allocation_rule: string }) =>
    request<any>('/smart-decisions/allocation', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  simulateWhatIf: (data: { store_id: number; product_id: number; demand_growth_pct: number; lead_time_change_days: number; price_change_pct: number }) =>
    request<any>('/smart-decisions/simulator', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Admin
  getSystemHealth: () => request<any>('/admin/system-health'),
  getAuditLogs: (params?: { page?: number; entity_type?: string }) => {
    const q = new URLSearchParams();
    if (params?.page) q.set('page', params.page.toString());
    if (params?.entity_type) q.set('entity_type', params.entity_type);
    return request<any>(`/admin/audit-logs?${q.toString()}`);
  },
  getUsers: () => request<any[]>('/admin/users'),

  // Reports CSV URLs
  getInventoryCsvUrl: () => `${API_BASE}/reports/inventory/csv`,
  getSalesCsvUrl: () => `${API_BASE}/reports/sales/csv`,
};
