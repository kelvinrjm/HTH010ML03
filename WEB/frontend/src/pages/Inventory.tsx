import React, { useEffect, useState } from 'react';
import {
  Boxes,
  Search,
  ArrowRightLeft,
  SlidersHorizontal,
  CheckCircle2,
  XCircle,
  X,
} from 'lucide-react';
import { Badge } from '../components/Badge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { api } from '../services/api';
import { InventoryItem, Store, Product } from '../types';

export const Inventory: React.FC = () => {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [selectedStore, setSelectedStore] = useState<string>('');
  const [selectedHealth, setSelectedHealth] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [modalSuccess, setModalSuccess] = useState<string | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Transfer form
  const [transferForm, setTransferForm] = useState({
    from_store_id: 1,
    to_store_id: 2,
    product_id: 1,
    quantity: 10,
    notes: '',
  });

  // Adjust form
  const [adjustForm, setAdjustForm] = useState({
    store_id: 1,
    product_id: 1,
    new_quantity_on_hand: 50,
    reason: 'Inventory count adjustment',
    notes: '',
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const [inventoryData, storesData, productsData] = await Promise.all([
        api.getInventory({
          store_id: selectedStore ? Number(selectedStore) : undefined,
          health_status: selectedHealth || undefined,
          search: searchQuery || undefined,
        }),
        api.getStores(),
        api.getProducts(),
      ]);
      setItems(inventoryData);
      setStores(storesData);
      setProducts(productsData);
      if (productsData.length > 0 && storesData.length > 1) {
        setTransferForm((prev) => ({
          ...prev,
          product_id: productsData[0].product_id,
          from_store_id: storesData[0].store_id,
          to_store_id: storesData[1].store_id,
        }));
        setAdjustForm((prev) => ({
          ...prev,
          product_id: productsData[0].product_id,
          store_id: storesData[0].store_id,
        }));
      }
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedStore, selectedHealth]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadData();
  };

  const handleTransferSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (transferForm.from_store_id === transferForm.to_store_id) {
      setModalError('Source and destination stores cannot be identical.');
      return;
    }
    try {
      setSubmitting(true);
      setModalError(null);
      const res = await api.transferStock(transferForm);
      setModalSuccess(res.message);
      setTimeout(() => {
        setShowTransferModal(false);
        setModalSuccess(null);
        loadData();
      }, 1200);
    } catch (err: any) {
      setModalError(err.message || 'Transfer failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAdjustSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      setModalError(null);
      const res = await api.adjustStock(adjustForm);
      setModalSuccess(res.message);
      setTimeout(() => {
        setShowAdjustModal(false);
        setModalSuccess(null);
        loadData();
      }, 1200);
    } catch (err: any) {
      setModalError(err.message || 'Adjustment failed');
    } finally {
      setSubmitting(false);
    }
  };

  // Aggregate metrics
  const totalValuation = items.reduce((acc, i) => acc + i.inventory_value, 0);
  const totalUnits = items.reduce((acc, i) => acc + i.quantity_on_hand, 0);
  const criticalItems = items.filter(
    (i) => i.stock_health_status === 'critical' || i.stock_health_status === 'low'
  ).length;

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <Boxes className="w-6 h-6 text-blue-400" />
            Multi-Store Inventory Overview
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Real-time multi-location stock levels, reorder points, days of cover, and velocity health.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center space-x-3">
          <button
            onClick={() => {
              setModalError(null);
              setModalSuccess(null);
              setShowTransferModal(true);
            }}
            className="flex items-center space-x-2 px-3.5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-sm transition"
          >
            <ArrowRightLeft className="w-4 h-4" />
            <span>Transfer Stock</span>
          </button>
          <button
            onClick={() => {
              setModalError(null);
              setModalSuccess(null);
              setShowAdjustModal(true);
            }}
            className="flex items-center space-x-2 px-3.5 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-semibold shadow-sm transition"
          >
            <SlidersHorizontal className="w-4 h-4 text-slate-300" />
            <span>Adjust Stock</span>
          </button>
        </div>
      </div>

      {/* Snapshot Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Stock Valuation</span>
          <p className="text-xl font-bold text-white mt-1">₹{totalValuation.toLocaleString('en-IN')}</p>
          <span className="text-[11px] text-slate-500">Across {items.length} store items</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Total Units on Hand</span>
          <p className="text-xl font-bold text-blue-400 mt-1">{totalUnits.toLocaleString('en-IN')}</p>
          <span className="text-[11px] text-slate-500">Physical stock count</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Reorder / Low Stock Alerts</span>
          <p className="text-xl font-bold text-amber-400 mt-1">{criticalItems} SKUs</p>
          <span className="text-[11px] text-slate-500">Below minimum buffer</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Stores Monitored</span>
          <p className="text-xl font-bold text-emerald-400 mt-1">{stores.length} Locations</p>
          <span className="text-[11px] text-slate-500">Synchronized daily</span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-3 shadow-sm">
        <form onSubmit={handleSearchSubmit} className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search by SKU, product title, or category..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-4 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
          />
        </form>

        <div className="flex flex-wrap items-center gap-3">
          {/* Store Filter */}
          <select
            value={selectedStore}
            onChange={(e) => setSelectedStore(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Stores</option>
            {stores.map((s) => (
              <option key={s.store_id} value={s.store_id}>
                {s.name}
              </option>
            ))}
          </select>

          {/* Health Filter */}
          <select
            value={selectedHealth}
            onChange={(e) => setSelectedHealth(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Health States</option>
            <option value="healthy">Healthy</option>
            <option value="low">Low Stock</option>
            <option value="critical">Critical / Stockout</option>
            <option value="overstock">Overstock</option>
            <option value="dead_stock">Dead Stock</option>
          </select>

          <button
            onClick={() => {
              setSelectedStore('');
              setSelectedHealth('');
              setSearchQuery('');
            }}
            className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition"
          >
            Reset
          </button>
        </div>
      </div>

      {/* Main Inventory Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-6">
            <LoadingSkeleton rows={8} />
          </div>
        ) : items.length === 0 ? (
          <div className="p-12 text-center">
            <Boxes className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <p className="text-slate-300 font-semibold text-sm">No inventory records found</p>
            <p className="text-slate-500 text-xs mt-1">Try adjusting your store or health filter.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 uppercase tracking-wider text-[10px]">
                <tr>
                  <th className="py-3 px-4">SKU / Product</th>
                  <th className="py-3 px-4">Store</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4 text-right">Available</th>
                  <th className="py-3 px-4 text-right">On Hand</th>
                  <th className="py-3 px-4 text-right">Reorder Pt</th>
                  <th className="py-3 px-4 text-right">Days Cover</th>
                  <th className="py-3 px-4 text-right">Valuation</th>
                  <th className="py-3 px-4 text-center">Health Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {items.map((item) => (
                  <tr key={item.inventory_id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4">
                      <div className="font-semibold text-white max-w-[200px] truncate">
                        {item.product_name}
                      </div>
                      <span className="text-[10px] text-slate-500 font-mono">{item.sku}</span>
                    </td>
                    <td className="py-3 px-4 text-slate-300 font-medium">{item.store_name}</td>
                    <td className="py-3 px-4 text-slate-400">{item.category_name}</td>
                    <td className="py-3 px-4 text-right font-bold text-white">
                      {item.quantity_available}
                    </td>
                    <td className="py-3 px-4 text-right text-slate-300">{item.quantity_on_hand}</td>
                    <td className="py-3 px-4 text-right text-slate-400">{item.reorder_point}</td>
                    <td className="py-3 px-4 text-right">
                      {item.days_of_stock !== null ? (
                        <span
                          className={`font-semibold ${
                            item.days_of_stock <= 5
                              ? 'text-rose-400'
                              : item.days_of_stock <= 14
                              ? 'text-amber-400'
                              : 'text-slate-300'
                          }`}
                        >
                          {item.days_of_stock}d
                        </span>
                      ) : (
                        <span className="text-slate-500">-</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-slate-200">
                      ₹{item.inventory_value.toLocaleString('en-IN')}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <Badge status={item.stock_health_status} size="sm" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Transfer Stock Modal */}
      {showTransferModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl relative">
            <button
              onClick={() => setShowTransferModal(false)}
              className="absolute right-4 top-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-lg font-bold text-white flex items-center gap-2 mb-1">
              <ArrowRightLeft className="w-5 h-5 text-blue-400" />
              Inter-Store Stock Transfer
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Transfer physical stock between retail locations with automatic double-entry ledger audit.
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

            <form onSubmit={handleTransferSubmit} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Select Product</label>
                <select
                  value={transferForm.product_id}
                  onChange={(e) =>
                    setTransferForm({ ...transferForm, product_id: Number(e.target.value) })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  {products.map((p) => (
                    <option key={p.product_id} value={p.product_id}>
                      {p.name} ({p.sku})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">From Store</label>
                  <select
                    value={transferForm.from_store_id}
                    onChange={(e) =>
                      setTransferForm({ ...transferForm, from_store_id: Number(e.target.value) })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    {stores.map((s) => (
                      <option key={s.store_id} value={s.store_id}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">To Store</label>
                  <select
                    value={transferForm.to_store_id}
                    onChange={(e) =>
                      setTransferForm({ ...transferForm, to_store_id: Number(e.target.value) })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    {stores.map((s) => (
                      <option key={s.store_id} value={s.store_id}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Quantity to Transfer
                </label>
                <input
                  type="number"
                  min="1"
                  value={transferForm.quantity}
                  onChange={(e) =>
                    setTransferForm({ ...transferForm, quantity: Number(e.target.value) })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Notes / Dispatch Reference
                </label>
                <input
                  type="text"
                  placeholder="e.g., Weekly stock balancing dispatch"
                  value={transferForm.notes}
                  onChange={(e) => setTransferForm({ ...transferForm, notes: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowTransferModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow transition disabled:opacity-50"
                >
                  {submitting ? 'Transferring...' : 'Execute Transfer'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Adjust Stock Modal */}
      {showAdjustModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl relative">
            <button
              onClick={() => setShowAdjustModal(false)}
              className="absolute right-4 top-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-lg font-bold text-white flex items-center gap-2 mb-1">
              <SlidersHorizontal className="w-5 h-5 text-amber-400" />
              Adjust Stock Level
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Audit reconciliation, shrinkage write-offs, or damaged goods correction.
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

            <form onSubmit={handleAdjustSubmit} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Select Store</label>
                <select
                  value={adjustForm.store_id}
                  onChange={(e) =>
                    setAdjustForm({ ...adjustForm, store_id: Number(e.target.value) })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  {stores.map((s) => (
                    <option key={s.store_id} value={s.store_id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Select Product</label>
                <select
                  value={adjustForm.product_id}
                  onChange={(e) =>
                    setAdjustForm({ ...adjustForm, product_id: Number(e.target.value) })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  {products.map((p) => (
                    <option key={p.product_id} value={p.product_id}>
                      {p.name} ({p.sku})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  New Quantity on Hand
                </label>
                <input
                  type="number"
                  min="0"
                  value={adjustForm.new_quantity_on_hand}
                  onChange={(e) =>
                    setAdjustForm({
                      ...adjustForm,
                      new_quantity_on_hand: Math.max(0, Number(e.target.value)),
                    })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Adjustment Reason
                </label>
                <select
                  value={adjustForm.reason}
                  onChange={(e) => setAdjustForm({ ...adjustForm, reason: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  <option value="Physical cycle count reconciliation">
                    Physical cycle count reconciliation
                  </option>
                  <option value="Damaged / broken merchandise write-off">
                    Damaged / broken merchandise write-off
                  </option>
                  <option value="Expired goods removal">Expired goods removal</option>
                  <option value="Supplier discrepancy correction">
                    Supplier discrepancy correction
                  </option>
                  <option value="Customer return restock">Customer return restock</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Audit Notes</label>
                <input
                  type="text"
                  placeholder="Optional details for audit log"
                  value={adjustForm.notes}
                  onChange={(e) => setAdjustForm({ ...adjustForm, notes: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowAdjustModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold shadow transition disabled:opacity-50"
                >
                  {submitting ? 'Applying...' : 'Apply Adjustment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
