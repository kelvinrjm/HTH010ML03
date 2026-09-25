import React, { useEffect, useState } from 'react';
import {
  Truck,
  Plus,
  PackageCheck,
  CheckCircle2,
  XCircle,
  X,
  Trash2,
  Clock,
  Building,
} from 'lucide-react';
import { Badge } from '../components/Badge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { api } from '../services/api';
import { Product, Purchase, Store, Supplier } from '../types';

export const Purchases: React.FC = () => {
  const [purchases, setPurchases] = useState<Purchase[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [selectedStore, setSelectedStore] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string>('');

  // Create PO Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [modalSuccess, setModalSuccess] = useState<string | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);

  // Receiving state
  const [receivingId, setReceivingId] = useState<number | null>(null);
  const [receiveFeedback, setReceiveFeedback] = useState<string | null>(null);

  // View details modal
  const [selectedPurchase, setSelectedPurchase] = useState<Purchase | null>(null);

  // Form state
  const [newPO, setNewPO] = useState({
    store_id: 1,
    supplier_id: 1,
    order_date: new Date().toISOString().split('T')[0],
    expected_delivery_date: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000)
      .toISOString()
      .split('T')[0],
    items: [
      { product_id: 1, quantity_ordered: 50, unit_cost: 45 },
    ],
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const [purchasesData, storesData, suppliersData, productsData] = await Promise.all([
        api.getPurchases(selectedStore ? Number(selectedStore) : undefined),
        api.getStores(),
        api.getSuppliers(),
        api.getProducts(),
      ]);
      setPurchases(purchasesData);
      setStores(storesData);
      setSuppliers(suppliersData);
      setProducts(productsData);
      if (storesData.length > 0 && suppliersData.length > 0 && productsData.length > 0) {
        setNewPO((prev) => ({
          ...prev,
          store_id: storesData[0].store_id,
          supplier_id: suppliersData[0].supplier_id,
          items: [{ product_id: productsData[0].product_id, quantity_ordered: 50, unit_cost: productsData[0].unit_cost }],
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
  }, [selectedStore]);

  const handleReceive = async (purchaseId: number) => {
    try {
      setReceivingId(purchaseId);
      const res = await api.receivePurchase(purchaseId);
      setReceiveFeedback(res.message);
      loadData();
      setTimeout(() => setReceiveFeedback(null), 3500);
    } catch (err: any) {
      alert(err.message || 'Failed to receive purchase order');
    } finally {
      setReceivingId(null);
    }
  };

  const handleAddItem = () => {
    if (products.length === 0) return;
    setNewPO({
      ...newPO,
      items: [
        ...newPO.items,
        { product_id: products[0].product_id, quantity_ordered: 20, unit_cost: products[0].unit_cost },
      ],
    });
  };

  const handleRemoveItem = (index: number) => {
    setNewPO({
      ...newPO,
      items: newPO.items.filter((_, i) => i !== index),
    });
  };

  const handleItemChange = (index: number, field: string, value: any) => {
    const updated = [...newPO.items];
    if (field === 'product_id') {
      const p = products.find((prod) => prod.product_id === Number(value));
      updated[index] = {
        ...updated[index],
        product_id: Number(value),
        unit_cost: p ? p.unit_cost : updated[index].unit_cost,
      };
    } else {
      updated[index] = {
        ...updated[index],
        [field]: Number(value),
      };
    }
    setNewPO({ ...newPO, items: updated });
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPO.items.length === 0) {
      setModalError('Please add at least one line item.');
      return;
    }

    try {
      setSubmitting(true);
      setModalError(null);
      await api.createPurchase(newPO);
      setModalSuccess('Purchase order created successfully.');
      setTimeout(() => {
        setShowCreateModal(false);
        setModalSuccess(null);
        loadData();
      }, 1200);
    } catch (err: any) {
      setModalError(err.message || 'Failed to create PO');
    } finally {
      setSubmitting(false);
    }
  };

  const filteredPurchases = purchases.filter((po) => {
    if (selectedStatus && po.status !== selectedStatus) return false;
    return true;
  });

  const totalProcurementSpend = purchases.reduce((acc, p) => acc + p.total_amount, 0);
  const pendingReceiptCount = purchases.filter((p) => p.status === 'submitted').length;

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <Truck className="w-6 h-6 text-blue-400" />
            Purchases & Replenishment Orders
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Supplier procurement orders, lead-time tracking, and automated stock receipt reconciliation.
          </p>
        </div>

        <button
          onClick={() => {
            setModalError(null);
            setModalSuccess(null);
            setShowCreateModal(true);
          }}
          className="flex items-center space-x-2 px-3.5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-sm transition self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>New Purchase Order</span>
        </button>
      </div>

      {/* Snapshot Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Total Orders Placed</span>
          <p className="text-xl font-bold text-white mt-1">{purchases.length} POs</p>
          <span className="text-[11px] text-slate-500">Across enterprise suppliers</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Pending Delivery / Transit</span>
          <p className="text-xl font-bold text-amber-400 mt-1">{pendingReceiptCount} Orders</p>
          <span className="text-[11px] text-slate-500">Awaiting store check-in</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Procurement Expenditure</span>
          <p className="text-xl font-bold text-emerald-400 mt-1">
            ₹{totalProcurementSpend.toLocaleString('en-IN')}
          </p>
          <span className="text-[11px] text-slate-500">Total replenishment value</span>
        </div>
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
          <span className="text-xs text-slate-400 font-medium">Active Suppliers</span>
          <p className="text-xl font-bold text-purple-400 mt-1">{suppliers.length} Vendors</p>
          <span className="text-[11px] text-slate-500">Contracted lead-times verified</span>
        </div>
      </div>

      {receiveFeedback && (
        <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 shrink-0" />
          <span className="font-semibold">{receiveFeedback}</span>
        </div>
      )}

      {/* Filter Bar */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-wrap items-center justify-between gap-3 shadow-sm">
        <div className="flex flex-wrap items-center gap-3">
          <select
            value={selectedStore}
            onChange={(e) => setSelectedStore(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Receiving Stores</option>
            {stores.map((s) => (
              <option key={s.store_id} value={s.store_id}>
                {s.name}
              </option>
            ))}
          </select>

          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All PO Statuses</option>
            <option value="submitted">Submitted (In-Transit)</option>
            <option value="received">Received (Restocked)</option>
            <option value="draft">Draft</option>
            <option value="cancelled">Cancelled</option>
          </select>

          <button
            onClick={() => {
              setSelectedStore('');
              setSelectedStatus('');
            }}
            className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition"
          >
            Reset
          </button>
        </div>
      </div>

      {/* Purchases Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-6">
            <LoadingSkeleton rows={8} />
          </div>
        ) : filteredPurchases.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            <Truck className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <p className="text-slate-300 font-semibold text-sm">No purchase orders found</p>
            <p className="text-slate-500 text-xs mt-1">Create a purchase order to replenish store stock.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 uppercase tracking-wider text-[10px]">
                <tr>
                  <th className="py-3 px-4">PO Number</th>
                  <th className="py-3 px-4">Supplier</th>
                  <th className="py-3 px-4">Fulfilling Store</th>
                  <th className="py-3 px-4">Order Date</th>
                  <th className="py-3 px-4">Expected Delivery</th>
                  <th className="py-3 px-4 text-right">Items</th>
                  <th className="py-3 px-4 text-right">Total Cost</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-4 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredPurchases.map((po) => (
                  <tr key={po.purchase_id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 font-mono font-semibold text-blue-400">
                      #PO-{po.purchase_id.toString().padStart(5, '0')}
                    </td>
                    <td className="py-3 px-4 font-medium text-white">{po.supplier_name}</td>
                    <td className="py-3 px-4 text-slate-300">{po.store_name}</td>
                    <td className="py-3 px-4 text-slate-400">{po.order_date}</td>
                    <td className="py-3 px-4 text-slate-400">{po.expected_delivery_date}</td>
                    <td className="py-3 px-4 text-right text-slate-300 font-medium">
                      {po.items?.length || 1}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-white">
                      ₹{po.total_amount.toLocaleString('en-IN')}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <Badge status={po.status} size="sm" />
                    </td>
                    <td className="py-3 px-4 text-center">
                      <div className="flex items-center justify-center space-x-2">
                        <button
                          onClick={() => setSelectedPurchase(po)}
                          className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] font-medium transition"
                        >
                          View
                        </button>
                        {po.status !== 'received' && po.status !== 'cancelled' && (
                          <button
                            onClick={() => handleReceive(po.purchase_id)}
                            disabled={receivingId === po.purchase_id}
                            className="flex items-center space-x-1 px-2.5 py-1 rounded bg-emerald-600/20 hover:bg-emerald-600 text-emerald-400 hover:text-white border border-emerald-500/30 text-[11px] font-medium transition disabled:opacity-50"
                          >
                            <PackageCheck className="w-3.5 h-3.5" />
                            <span>
                              {receivingId === po.purchase_id ? 'Receiving...' : 'Receive Stock'}
                            </span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Purchase Order Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl p-6 shadow-2xl relative max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setShowCreateModal(false)}
              className="absolute right-4 top-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-lg font-bold text-white flex items-center gap-2 mb-1">
              <Truck className="w-5 h-5 text-blue-400" />
              Create Purchase Replenishment Order
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Issue a restock order to vendor. Receiving goods will increment store inventory.
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

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Supplier</label>
                  <select
                    value={newPO.supplier_id}
                    onChange={(e) => setNewPO({ ...newPO, supplier_id: Number(e.target.value) })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    {suppliers.map((sup) => (
                      <option key={sup.supplier_id} value={sup.supplier_id}>
                        {sup.name} ({sup.default_lead_time_days}d lead time)
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Destination Store
                  </label>
                  <select
                    value={newPO.store_id}
                    onChange={(e) => setNewPO({ ...newPO, store_id: Number(e.target.value) })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    {stores.map((s) => (
                      <option key={s.store_id} value={s.store_id}>
                        {s.name} ({s.location})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Order Date</label>
                  <input
                    type="date"
                    value={newPO.order_date}
                    onChange={(e) => setNewPO({ ...newPO, order_date: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Expected Delivery Date
                  </label>
                  <input
                    type="date"
                    value={newPO.expected_delivery_date}
                    onChange={(e) =>
                      setNewPO({ ...newPO, expected_delivery_date: e.target.value })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Line Items */}
              <div className="border-t border-slate-800 pt-3">
                <div className="flex justify-between items-center mb-2">
                  <label className="text-xs font-bold uppercase tracking-wider text-slate-300">
                    Order Line Items
                  </label>
                  <button
                    type="button"
                    onClick={handleAddItem}
                    className="text-xs font-semibold text-blue-400 hover:text-blue-300 flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Item</span>
                  </button>
                </div>

                <div className="space-y-2">
                  {newPO.items.map((item, index) => (
                    <div
                      key={index}
                      className="p-3 rounded-lg bg-slate-950/70 border border-slate-800 grid grid-cols-12 gap-3 items-center"
                    >
                      <div className="col-span-6">
                        <select
                          value={item.product_id}
                          onChange={(e) => handleItemChange(index, 'product_id', e.target.value)}
                          className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none"
                        >
                          {products.map((p) => (
                            <option key={p.product_id} value={p.product_id}>
                              {p.name}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="col-span-3">
                        <input
                          type="number"
                          min="1"
                          placeholder="Qty"
                          value={item.quantity_ordered}
                          onChange={(e) =>
                            handleItemChange(index, 'quantity_ordered', e.target.value)
                          }
                          className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none"
                        />
                      </div>

                      <div className="col-span-2">
                        <input
                          type="number"
                          min="1"
                          placeholder="Cost"
                          value={item.unit_cost}
                          onChange={(e) => handleItemChange(index, 'unit_cost', e.target.value)}
                          className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none"
                        />
                      </div>

                      <div className="col-span-1 text-center">
                        {newPO.items.length > 1 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveItem(index)}
                            className="text-slate-500 hover:text-rose-400 transition"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="pt-3 border-t border-slate-800 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow transition disabled:opacity-50"
                >
                  {submitting ? 'Creating PO...' : 'Issue Purchase Order'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* PO View Detail Modal */}
      {selectedPurchase && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl relative">
            <button
              onClick={() => setSelectedPurchase(null)}
              className="absolute right-4 top-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-base font-bold text-white mb-1 flex items-center gap-2">
              <Truck className="w-5 h-5 text-blue-400" />
              PO #PO-{selectedPurchase.purchase_id.toString().padStart(5, '0')}
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Vendor: {selectedPurchase.supplier_name} • Store: {selectedPurchase.store_name}
            </p>

            <table className="w-full text-left text-xs mb-4">
              <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                <tr>
                  <th className="py-2 px-3">Item</th>
                  <th className="py-2 px-3 text-center">Ordered</th>
                  <th className="py-2 px-3 text-center">Received</th>
                  <th className="py-2 px-3 text-right">Unit Cost</th>
                  <th className="py-2 px-3 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {selectedPurchase.items.map((item) => (
                  <tr key={item.purchase_item_id}>
                    <td className="py-2.5 px-3">
                      <span className="font-semibold text-white">{item.product_name}</span>
                      <span className="block text-[10px] text-slate-500 font-mono">{item.sku}</span>
                    </td>
                    <td className="py-2.5 px-3 text-center text-slate-300">
                      {item.quantity_ordered}
                    </td>
                    <td className="py-2.5 px-3 text-center font-bold text-emerald-400">
                      {item.quantity_received}
                    </td>
                    <td className="py-2.5 px-3 text-right text-slate-400">
                      ₹{item.unit_cost.toLocaleString('en-IN')}
                    </td>
                    <td className="py-2.5 px-3 text-right font-bold text-blue-400">
                      ₹{item.subtotal.toLocaleString('en-IN')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="flex justify-between items-center border-t border-slate-800 pt-3">
              <span className="text-xs font-semibold text-slate-400">Total Purchase Cost:</span>
              <span className="text-base font-bold text-white">
                ₹{selectedPurchase.total_amount.toLocaleString('en-IN')}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
