import React, { useEffect, useState } from 'react';
import {
  ShoppingCart,
  Plus,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Receipt,
  Store as StoreIcon,
  CreditCard,
  History,
  X,
} from 'lucide-react';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { api } from '../services/api';
import { InventoryItem, Product, Sale, Store } from '../types';

export const Sales: React.FC = () => {
  const [activeView, setActiveView] = useState<'checkout' | 'history'>('checkout');
  const [stores, setStores] = useState<Store[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [storeInventory, setStoreInventory] = useState<InventoryItem[]>([]);
  const [salesHistory, setSalesHistory] = useState<Sale[]>([]);
  const [loading, setLoading] = useState(true);

  // Checkout form state
  const [selectedStoreId, setSelectedStoreId] = useState<number>(1);
  const [channel, setChannel] = useState<'in_store' | 'online' | 'wholesale'>('in_store');
  const [cartItems, setCartItems] = useState<
    { product_id: number; product_name: string; sku: string; quantity: number; unit_price: number; max_available: number }[]
  >([]);

  // Item selector in cart
  const [selectedProductId, setSelectedProductId] = useState<number>(1);
  const [itemQuantity, setItemQuantity] = useState<number>(1);

  // Transaction feedback
  const [submitting, setSubmitting] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState<Sale | null>(null);
  const [orderError, setOrderError] = useState<string | null>(null);

  // Selected history item for modal
  const [viewSaleDetails, setViewSaleDetails] = useState<Sale | null>(null);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const [storesData, productsData] = await Promise.all([
        api.getStores(),
        api.getProducts(),
      ]);
      setStores(storesData);
      setProducts(productsData);
      if (storesData.length > 0) {
        setSelectedStoreId(storesData[0].store_id);
      }
      if (productsData.length > 0) {
        setSelectedProductId(productsData[0].product_id);
      }
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadStoreInventory = async (storeId: number) => {
    try {
      const inv = await api.getInventory({ store_id: storeId });
      setStoreInventory(inv);
    } catch (err: any) {
      console.error(err);
    }
  };

  const loadHistory = async (storeId?: number) => {
    try {
      const sales = await api.getSales(storeId);
      setSalesHistory(sales);
    } catch (err: any) {
      console.error(err);
    }
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedStoreId) {
      loadStoreInventory(selectedStoreId);
      if (activeView === 'history') {
        loadHistory(selectedStoreId);
      }
    }
  }, [selectedStoreId, activeView]);

  // Find stock for selected product in current store
  const currentInvItem = storeInventory.find((i) => i.product_id === selectedProductId);
  const availableStock = currentInvItem ? currentInvItem.quantity_available : 0;
  const currentProduct = products.find((p) => p.product_id === selectedProductId);

  const handleAddToCart = () => {
    if (!currentProduct) return;
    if (itemQuantity <= 0) {
      setOrderError('Quantity must be greater than zero.');
      return;
    }

    // Check against available stock
    const existingInCart = cartItems.find((item) => item.product_id === currentProduct.product_id);
    const totalRequested = (existingInCart ? existingInCart.quantity : 0) + itemQuantity;

    if (totalRequested > availableStock) {
      setOrderError(
        `Insufficient inventory at this store. Only ${availableStock} units available for ${currentProduct.name}.`
      );
      return;
    }

    setOrderError(null);

    if (existingInCart) {
      setCartItems(
        cartItems.map((item) =>
          item.product_id === currentProduct.product_id
            ? { ...item, quantity: item.quantity + itemQuantity }
            : item
        )
      );
    } else {
      setCartItems([
        ...cartItems,
        {
          product_id: currentProduct.product_id,
          product_name: currentProduct.name,
          sku: currentProduct.sku,
          quantity: itemQuantity,
          unit_price: currentProduct.unit_price,
          max_available: availableStock,
        },
      ]);
    }

    setItemQuantity(1);
  };

  const handleRemoveFromCart = (productId: number) => {
    setCartItems(cartItems.filter((i) => i.product_id !== productId));
  };

  const handleCheckout = async () => {
    if (cartItems.length === 0) {
      setOrderError('Please add at least one product to the checkout cart.');
      return;
    }

    try {
      setSubmitting(true);
      setOrderError(null);
      const saleResult = await api.createSale({
        store_id: selectedStoreId,
        channel: channel,
        items: cartItems.map((item) => ({
          product_id: item.product_id,
          quantity: item.quantity,
          unit_price: item.unit_price,
        })),
      });

      setOrderSuccess(saleResult);
      setCartItems([]);
      // Reload inventory for the store
      loadStoreInventory(selectedStoreId);
    } catch (err: any) {
      setOrderError(err.message || 'Checkout failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const subtotal = cartItems.reduce((acc, item) => acc + item.quantity * item.unit_price, 0);
  const taxAmount = Math.round(subtotal * 0.05); // 5% GST/VAT
  const grandTotal = subtotal + taxAmount;

  return (
    <div className="space-y-6 pb-12">
      {/* Top Header & View Toggle */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <ShoppingCart className="w-6 h-6 text-blue-400" />
            Point of Sale & Transactions
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Real-time order checkout with strict inventory validation and automated stock decrement.
          </p>
        </div>

        <div className="flex items-center bg-slate-900 border border-slate-800 p-1 rounded-xl self-start sm:self-auto">
          <button
            onClick={() => setActiveView('checkout')}
            className={`flex items-center space-x-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeView === 'checkout'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <CreditCard className="w-4 h-4" />
            <span>POS Checkout</span>
          </button>
          <button
            onClick={() => setActiveView('history')}
            className={`flex items-center space-x-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeView === 'history'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <History className="w-4 h-4" />
            <span>Sales History</span>
          </button>
        </div>
      </div>

      {activeView === 'checkout' ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left 2 Cols: Order Builder */}
          <div className="lg:col-span-2 space-y-5">
            {/* Store & Channel Configuration */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1 flex items-center gap-1.5">
                  <StoreIcon className="w-3.5 h-3.5 text-blue-400" />
                  Select Fulfilling Store
                </label>
                <select
                  value={selectedStoreId}
                  onChange={(e) => {
                    setSelectedStoreId(Number(e.target.value));
                    setCartItems([]);
                  }}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  {stores.map((s) => (
                    <option key={s.store_id} value={s.store_id}>
                      {s.name} ({s.location})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Sales Channel
                </label>
                <select
                  value={channel}
                  onChange={(e: any) => setChannel(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  <option value="in_store">In-Store Counter / Walk-in</option>
                  <option value="online">Online Direct Delivery</option>
                  <option value="wholesale">B2B Wholesaler Distribution</option>
                </select>
              </div>
            </div>

            {/* Product Selection Bar */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
                Add Items to Transaction
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end">
                <div className="sm:col-span-7">
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    Select Product
                  </label>
                  <select
                    value={selectedProductId}
                    onChange={(e) => setSelectedProductId(Number(e.target.value))}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    {products.map((p) => (
                      <option key={p.product_id} value={p.product_id}>
                        {p.name} — ₹{p.unit_price}
                      </option>
                    ))}
                  </select>
                  <div className="mt-1 flex items-center justify-between text-[11px]">
                    <span className="text-slate-500">
                      SKU: <span className="font-mono text-slate-400">{currentProduct?.sku}</span>
                    </span>
                    <span
                      className={`font-semibold ${
                        availableStock <= 5 ? 'text-rose-400' : 'text-emerald-400'
                      }`}
                    >
                      {availableStock} units available at this store
                    </span>
                  </div>
                </div>

                <div className="sm:col-span-3">
                  <label className="block text-xs font-medium text-slate-400 mb-1">Quantity</label>
                  <input
                    type="number"
                    min="1"
                    max={availableStock}
                    value={itemQuantity}
                    onChange={(e) => setItemQuantity(Math.max(1, Number(e.target.value)))}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <button
                    type="button"
                    onClick={handleAddToCart}
                    disabled={availableStock <= 0}
                    className="w-full h-9 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold flex items-center justify-center space-x-1.5 shadow-sm transition disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Add</span>
                  </button>
                </div>
              </div>

              {orderError && (
                <div className="mt-3 p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{orderError}</span>
                </div>
              )}
            </div>

            {/* Cart Line Items Table */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
              <div className="p-3 bg-slate-950/60 border-b border-slate-800 flex justify-between items-center">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Cart Items ({cartItems.length})
                </span>
                {cartItems.length > 0 && (
                  <button
                    onClick={() => setCartItems([])}
                    className="text-[11px] text-rose-400 hover:underline"
                  >
                    Clear Cart
                  </button>
                )}
              </div>

              {cartItems.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-xs">
                  Cart is empty. Select products above to start an order.
                </div>
              ) : (
                <table className="w-full text-left text-xs">
                  <thead className="text-slate-400 border-b border-slate-800/80 text-[10px] uppercase">
                    <tr>
                      <th className="py-2.5 px-4">Item</th>
                      <th className="py-2.5 px-4 text-center">Qty</th>
                      <th className="py-2.5 px-4 text-right">Unit Price</th>
                      <th className="py-2.5 px-4 text-right">Total</th>
                      <th className="py-2.5 px-4 text-center">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/50">
                    {cartItems.map((item) => (
                      <tr key={item.product_id}>
                        <td className="py-2.5 px-4">
                          <span className="font-semibold text-white">{item.product_name}</span>
                          <span className="block text-[10px] text-slate-500 font-mono">
                            {item.sku}
                          </span>
                        </td>
                        <td className="py-2.5 px-4 text-center font-bold text-slate-200">
                          {item.quantity}
                        </td>
                        <td className="py-2.5 px-4 text-right text-slate-400">
                          ₹{item.unit_price.toLocaleString('en-IN')}
                        </td>
                        <td className="py-2.5 px-4 text-right font-bold text-blue-400">
                          ₹{(item.quantity * item.unit_price).toLocaleString('en-IN')}
                        </td>
                        <td className="py-2.5 px-4 text-center">
                          <button
                            onClick={() => handleRemoveFromCart(item.product_id)}
                            className="p-1 text-slate-500 hover:text-rose-400 transition"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>

          {/* Right 1 Col: Summary & Checkout */}
          <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 flex flex-col justify-between h-fit shadow-sm">
            <div>
              <div className="flex items-center space-x-2 border-b border-slate-800 pb-3 mb-4">
                <Receipt className="w-5 h-5 text-blue-400" />
                <h3 className="text-base font-bold text-white">Order Summary</h3>
              </div>

              <div className="space-y-3 text-xs mb-6">
                <div className="flex justify-between text-slate-400">
                  <span>Subtotal</span>
                  <span className="font-medium text-slate-200">
                    ₹{subtotal.toLocaleString('en-IN')}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Estimated Tax (5% GST)</span>
                  <span className="font-medium text-slate-200">
                    ₹{taxAmount.toLocaleString('en-IN')}
                  </span>
                </div>
                <div className="border-t border-slate-800/80 pt-3 flex justify-between text-sm">
                  <span className="font-bold text-white">Grand Total</span>
                  <span className="font-bold text-emerald-400 text-base">
                    ₹{grandTotal.toLocaleString('en-IN')}
                  </span>
                </div>
              </div>

              <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-lg text-[11px] text-slate-400 space-y-1 mb-6">
                <div className="flex justify-between">
                  <span>Store:</span>
                  <span className="text-slate-200 font-medium">
                    {stores.find((s) => s.store_id === selectedStoreId)?.name}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Channel:</span>
                  <span className="text-slate-200 font-medium capitalize">
                    {channel.replace('_', ' ')}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Line Items:</span>
                  <span className="text-slate-200 font-medium">{cartItems.length}</span>
                </div>
              </div>
            </div>

            <button
              onClick={handleCheckout}
              disabled={submitting || cartItems.length === 0}
              className="w-full py-3 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-lg transition flex items-center justify-center space-x-2 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <CreditCard className="w-4 h-4" />
              <span>{submitting ? 'Validating Stock...' : `Complete Sale • ₹${grandTotal.toLocaleString('en-IN')}`}</span>
            </button>
          </div>
        </div>
      ) : (
        /* Sales History View */
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
          <div className="p-4 bg-slate-950/60 border-b border-slate-800 flex justify-between items-center">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Historical Transactions
            </span>
            <span className="text-xs text-slate-500">{salesHistory.length} orders recorded</span>
          </div>

          {salesHistory.length === 0 ? (
            <div className="p-12 text-center text-slate-500 text-xs">
              No historical sales recorded for this store.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-950/40 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                  <tr>
                    <th className="py-3 px-4">Receipt / Order ID</th>
                    <th className="py-3 px-4">Timestamp</th>
                    <th className="py-3 px-4">Store Location</th>
                    <th className="py-3 px-4 text-center">Channel</th>
                    <th className="py-3 px-4 text-right">Items</th>
                    <th className="py-3 px-4 text-right">Total Revenue</th>
                    <th className="py-3 px-4 text-center">Receipt Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {salesHistory.map((s) => (
                    <tr key={s.sale_id} className="hover:bg-slate-800/40 transition">
                      <td className="py-3 px-4 font-mono font-semibold text-blue-400">
                        #ORD-{s.sale_id.toString().padStart(5, '0')}
                      </td>
                      <td className="py-3 px-4 text-slate-300">
                        {s.created_at ? s.created_at.replace('T', ' ').slice(0, 16) : s.sale_date}
                      </td>
                      <td className="py-3 px-4 text-slate-300">{s.store_name}</td>
                      <td className="py-3 px-4 text-center">
                        <span className="px-2 py-0.5 rounded text-[10px] uppercase font-semibold bg-slate-800 text-slate-300">
                          {s.channel}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right font-medium text-slate-300">
                        {s.items?.length || 1}
                      </td>
                      <td className="py-3 px-4 text-right font-bold text-emerald-400">
                        ₹{s.total_amount.toLocaleString('en-IN')}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <button
                          onClick={() => setViewSaleDetails(s)}
                          className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-blue-400 text-[11px] font-medium transition"
                        >
                          View Items
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Sale Confirmation Modal */}
      {orderSuccess && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl relative text-center">
            <div className="w-12 h-12 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto mb-3 border border-emerald-500/30">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white mb-1">Transaction Completed</h3>
            <p className="text-xs text-slate-400 mb-4">
              Inventory successfully updated and verified across store database.
            </p>

            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-left text-xs space-y-2 mb-5">
              <div className="flex justify-between">
                <span className="text-slate-400">Order ID:</span>
                <span className="font-mono font-bold text-blue-400">
                  #ORD-{orderSuccess.sale_id.toString().padStart(5, '0')}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Store:</span>
                <span className="text-slate-200 font-medium">{orderSuccess.store_name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Items Sold:</span>
                <span className="text-slate-200 font-medium">{orderSuccess.items.length} items</span>
              </div>
              <div className="flex justify-between border-t border-slate-800 pt-2 font-bold">
                <span className="text-white">Amount Paid:</span>
                <span className="text-emerald-400 font-bold">
                  ₹{orderSuccess.total_amount.toLocaleString('en-IN')}
                </span>
              </div>
            </div>

            <button
              onClick={() => setOrderSuccess(null)}
              className="w-full py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow transition"
            >
              Done & Ready for Next Customer
            </button>
          </div>
        </div>
      )}

      {/* Sale Details Modal */}
      {viewSaleDetails && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl relative">
            <button
              onClick={() => setViewSaleDetails(null)}
              className="absolute right-4 top-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-base font-bold text-white mb-1 flex items-center gap-2">
              <Receipt className="w-5 h-5 text-blue-400" />
              Transaction Receipt #ORD-{viewSaleDetails.sale_id.toString().padStart(5, '0')}
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Recorded at {viewSaleDetails.store_name} • Channel: {viewSaleDetails.channel}
            </p>

            <table className="w-full text-left text-xs mb-4">
              <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                <tr>
                  <th className="py-2 px-3">Product</th>
                  <th className="py-2 px-3 text-center">Qty</th>
                  <th className="py-2 px-3 text-right">Price</th>
                  <th className="py-2 px-3 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {viewSaleDetails.items.map((item) => (
                  <tr key={item.sale_item_id}>
                    <td className="py-2.5 px-3">
                      <span className="font-semibold text-white">{item.product_name}</span>
                      <span className="block text-[10px] text-slate-500 font-mono">{item.sku}</span>
                    </td>
                    <td className="py-2.5 px-3 text-center font-bold text-slate-300">
                      {item.quantity}
                    </td>
                    <td className="py-2.5 px-3 text-right text-slate-400">
                      ₹{item.unit_price.toLocaleString('en-IN')}
                    </td>
                    <td className="py-2.5 px-3 text-right font-bold text-emerald-400">
                      ₹{item.subtotal.toLocaleString('en-IN')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="flex justify-between items-center border-t border-slate-800 pt-3">
              <span className="text-xs font-semibold text-slate-400">Grand Total:</span>
              <span className="text-base font-bold text-emerald-400">
                ₹{viewSaleDetails.total_amount.toLocaleString('en-IN')}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
