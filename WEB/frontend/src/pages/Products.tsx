import React, { useEffect, useState } from 'react';
import {
  Package,
  Search,
  Plus,
  Percent,
  CheckCircle2,
  XCircle,
  X,
  Layers,
} from 'lucide-react';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { api } from '../services/api';
import { Product } from '../types';

export const Products: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');

  // New Product Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [modalSuccess, setModalSuccess] = useState<string | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [newProduct, setNewProduct] = useState({
    sku: '',
    name: '',
    description: '',
    category_id: 1,
    unit_of_measure: 'unit',
    unit_cost: 50,
    unit_price: 75,
  });

  const loadProducts = async () => {
    try {
      setLoading(true);
      const data = await api.getProducts({
        category_id: selectedCategory ? Number(selectedCategory) : undefined,
        search: search || undefined,
      });
      setProducts(data);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, [selectedCategory]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadProducts();
  };

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProduct.sku || !newProduct.name) {
      setModalError('SKU and Product Name are required.');
      return;
    }
    if (newProduct.unit_price <= newProduct.unit_cost) {
      setModalError('Selling price must exceed unit cost for positive margin.');
      return;
    }

    try {
      setSubmitting(true);
      setModalError(null);
      await api.createProduct(newProduct);
      setModalSuccess(`Product ${newProduct.sku} created successfully.`);
      setTimeout(() => {
        setShowAddModal(false);
        setModalSuccess(null);
        setNewProduct({
          sku: '',
          name: '',
          description: '',
          category_id: 1,
          unit_of_measure: 'unit',
          unit_cost: 50,
          unit_price: 75,
        });
        loadProducts();
      }, 1200);
    } catch (err: any) {
      setModalError(err.message || 'Failed to create product');
    } finally {
      setSubmitting(false);
    }
  };

  // Metrics
  const totalProducts = products.length;
  const avgMargin =
    products.length > 0
      ? (
          products.reduce((acc, p) => acc + (p.profit_margin || 0), 0) / products.length
        ).toFixed(1)
      : '0';
  const totalAggregatedUnits = products.reduce((acc, p) => acc + (p.total_stock_on_hand || 0), 0);
  const totalValuation = products.reduce((acc, p) => acc + (p.total_inventory_value || 0), 0);

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <Package className="w-6 h-6 text-blue-400" />
            Product Master Catalog
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Global SKU management, cost prices, retail pricing, gross margins, and cross-store stock.
          </p>
        </div>

        <button
          onClick={() => {
            setModalError(null);
            setModalSuccess(null);
            setShowAddModal(true);
          }}
          className="flex items-center space-x-2 px-3.5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-sm transition self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add New Product</span>
        </button>
      </div>

      {/* Snapshot Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Catalog Depth</span>
          <p className="text-xl font-bold text-white mt-1">{totalProducts} SKUs</p>
          <span className="text-[11px] text-slate-500">Active catalog items</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Average Gross Margin</span>
          <p className="text-xl font-bold text-emerald-400 mt-1">{avgMargin}%</p>
          <span className="text-[11px] text-slate-500">Weighted markup margin</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Total Enterprise Stock</span>
          <p className="text-xl font-bold text-blue-400 mt-1">
            {totalAggregatedUnits.toLocaleString('en-IN')} units
          </p>
          <span className="text-[11px] text-slate-500">Across all network stores</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Enterprise Stock Valuation</span>
          <p className="text-xl font-bold text-purple-400 mt-1">
            ₹{totalValuation.toLocaleString('en-IN')}
          </p>
          <span className="text-[11px] text-slate-500">At current retail value</span>
        </div>
      </div>

      {/* Search & Filter Bar */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-3 shadow-sm">
        <form onSubmit={handleSearchSubmit} className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search by SKU code or product title..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-4 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
          />
        </form>

        <div className="flex items-center gap-3">
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Categories</option>
            <option value="1">Grains, Flours & Staples</option>
            <option value="2">Edible Oils & Ghee</option>
            <option value="3">Dairy & Breakfast Essentials</option>
            <option value="4">Snacks & Packaged Foods</option>
            <option value="5">Personal Care & Hygiene</option>
            <option value="6">Household & Cleaning</option>
          </select>

          <button
            onClick={() => {
              setSelectedCategory('');
              setSearch('');
            }}
            className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition"
          >
            Reset
          </button>
        </div>
      </div>

      {/* Products Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-6">
            <LoadingSkeleton rows={8} />
          </div>
        ) : products.length === 0 ? (
          <div className="p-12 text-center">
            <Package className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <p className="text-slate-300 font-semibold text-sm">No products found</p>
            <p className="text-slate-500 text-xs mt-1">Try refining your search keyword or category filter.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 uppercase tracking-wider text-[10px]">
                <tr>
                  <th className="py-3 px-4">SKU Code</th>
                  <th className="py-3 px-4">Product Name</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4 text-center">UoM</th>
                  <th className="py-3 px-4 text-right">Cost Price</th>
                  <th className="py-3 px-4 text-right">Selling Price</th>
                  <th className="py-3 px-4 text-right">Gross Margin</th>
                  <th className="py-3 px-4 text-right">Total Network Stock</th>
                  <th className="py-3 px-4 text-right">Retail Value</th>
                  <th className="py-3 px-4 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {products.map((p) => (
                  <tr key={p.product_id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 font-mono text-[11px] font-semibold text-blue-400">
                      {p.sku}
                    </td>
                    <td className="py-3 px-4">
                      <div className="font-semibold text-white max-w-[220px] truncate">{p.name}</div>
                      {p.description && (
                        <p className="text-[10px] text-slate-500 truncate max-w-[220px]">
                          {p.description}
                        </p>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-300">{p.category_name}</td>
                    <td className="py-3 px-4 text-center text-slate-400 uppercase font-mono text-[10px]">
                      {p.unit_of_measure}
                    </td>
                    <td className="py-3 px-4 text-right text-slate-400">
                      ₹{p.unit_cost.toLocaleString('en-IN')}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-white">
                      ₹{p.unit_price.toLocaleString('en-IN')}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <span className="font-bold text-emerald-400 flex items-center justify-end gap-0.5">
                        <Percent className="w-3 h-3" />
                        {p.profit_margin}%
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-semibold text-slate-200">
                      {p.total_stock_on_hand !== undefined ? p.total_stock_on_hand : '-'}
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-slate-300">
                      ₹{(p.total_inventory_value || 0).toLocaleString('en-IN')}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span
                        className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                          p.is_active
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : 'bg-slate-800 text-slate-400'
                        }`}
                      >
                        {p.is_active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Product Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl relative">
            <button
              onClick={() => setShowAddModal(false)}
              className="absolute right-4 top-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-lg font-bold text-white flex items-center gap-2 mb-1">
              <Package className="w-5 h-5 text-blue-400" />
              Register New SKU
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Add a new commercial product to the global enterprise catalog.
            </p>

            {modalSuccess && (
              <div className="mb-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center gap-2 text-emerald-400 text-xs">
                <CheckCircle2 className="w-4 h-4 shrink-0" />
                <span>{modalSuccess}</span>
              </div>
            )}

            {modalError && (
              <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 flex items-center gap-2 text-rose-400 text-xs">
                <XCircle className="w-4 h-4 shrink-0" />
                <span>{modalError}</span>
              </div>
            )}

            <form onSubmit={handleAddSubmit} className="space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">SKU Code</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g., GR-RICE-BAS-05"
                    value={newProduct.sku}
                    onChange={(e) =>
                      setNewProduct({ ...newProduct, sku: e.target.value.toUpperCase() })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Category</label>
                  <select
                    value={newProduct.category_id}
                    onChange={(e) =>
                      setNewProduct({ ...newProduct, category_id: Number(e.target.value) })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    <option value="1">Grains, Flours & Staples</option>
                    <option value="2">Edible Oils & Ghee</option>
                    <option value="3">Dairy & Breakfast Essentials</option>
                    <option value="4">Snacks & Packaged Foods</option>
                    <option value="5">Personal Care & Hygiene</option>
                    <option value="6">Household & Cleaning</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Product Name / Title
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Premium Royal Basmati Rice 5kg"
                  value={newProduct.name}
                  onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Description / Specification
                </label>
                <input
                  type="text"
                  placeholder="e.g., Aged long-grain aromatic rice, vacuum packed"
                  value={newProduct.description}
                  onChange={(e) => setNewProduct({ ...newProduct, description: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Unit of Measure</label>
                  <select
                    value={newProduct.unit_of_measure}
                    onChange={(e) =>
                      setNewProduct({ ...newProduct, unit_of_measure: e.target.value })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    <option value="unit">Unit / Piece</option>
                    <option value="kg">Kilogram (kg)</option>
                    <option value="liter">Liter (L)</option>
                    <option value="pack">Pack / Bundle</option>
                    <option value="box">Carton / Box</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Cost Price (₹)
                  </label>
                  <input
                    type="number"
                    min="1"
                    step="0.01"
                    value={newProduct.unit_cost}
                    onChange={(e) =>
                      setNewProduct({ ...newProduct, unit_cost: Number(e.target.value) })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Selling Price (₹)
                  </label>
                  <input
                    type="number"
                    min="1"
                    step="0.01"
                    value={newProduct.unit_price}
                    onChange={(e) =>
                      setNewProduct({ ...newProduct, unit_price: Number(e.target.value) })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow transition disabled:opacity-50"
                >
                  {submitting ? 'Registering...' : 'Save Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
