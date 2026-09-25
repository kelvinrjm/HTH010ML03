import React, { useEffect, useState } from 'react';
import {
  BrainCircuit,
  AlertTriangle,
  RotateCcw,
  Sparkles,
  Sliders,
  Send,
  Building2,
  CheckCircle2,
  Info,
  DollarSign,
  ArrowRight,
} from 'lucide-react';
import { Badge } from '../components/Badge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { api } from '../services/api';
import {
  Product,
  ReorderRecommendation,
  StockoutRisk,
  Store,
} from '../types';

export const SmartDecisions: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'reorder' | 'stockout' | 'allocation' | 'simulator'>(
    'reorder'
  );

  const [reorders, setReorders] = useState<ReorderRecommendation[]>([]);
  const [stockouts, setStockouts] = useState<StockoutRisk[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [loading, setLoading] = useState(true);

  // Multi-Store Allocation State
  const [allocProductId, setAllocProductId] = useState<number>(1);
  const [allocQuantity, setAllocQuantity] = useState<number>(100);
  const [allocRule, setAllocRule] = useState<string>('proportional_to_shortage');
  const [allocationResult, setAllocationResult] = useState<any | null>(null);
  const [allocating, setAllocating] = useState(false);

  // What-If Simulator State
  const [simStoreId, setSimStoreId] = useState<number>(1);
  const [simProductId, setSimProductId] = useState<number>(1);
  const [simDemandGrowth, setSimDemandGrowth] = useState<number>(20);
  const [simLeadTimeChange, setSimLeadTimeChange] = useState<number>(3);
  const [simPriceChange, setSimPriceChange] = useState<number>(0);
  const [simResult, setSimResult] = useState<any | null>(null);
  const [simulating, setSimulating] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.getReorderRecommendations(),
      api.getStockoutRisks(),
      api.getProducts(),
      api.getStores(),
    ])
      .then(([reordersData, stockoutsData, productsData, storesData]) => {
        setReorders(reordersData);
        setStockouts(stockoutsData);
        setProducts(productsData);
        setStores(storesData);
        if (productsData.length > 0) {
          setAllocProductId(productsData[0].product_id);
          setSimProductId(productsData[0].product_id);
        }
        if (storesData.length > 0) {
          setSimStoreId(storesData[0].store_id);
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const handleRunAllocation = async () => {
    try {
      setAllocating(true);
      const res = await api.calculateAllocation({
        product_id: allocProductId,
        total_quantity_available: allocQuantity,
        allocation_rule: allocRule,
      });
      setAllocationResult(res);
    } catch (err: any) {
      alert(err.message || 'Allocation failed');
    } finally {
      setAllocating(false);
    }
  };

  const handleRunSimulation = async () => {
    try {
      setSimulating(true);
      const res = await api.simulateWhatIf({
        store_id: simStoreId,
        product_id: simProductId,
        demand_growth_pct: simDemandGrowth,
        lead_time_change_days: simLeadTimeChange,
        price_change_pct: simPriceChange,
      });
      setSimResult(res);
    } catch (err: any) {
      alert(err.message || 'Simulation failed');
    } finally {
      setSimulating(false);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
          <BrainCircuit className="w-6 h-6 text-blue-400" />
          Smart Decision Intelligence
        </h2>
        <p className="text-sm text-slate-400 mt-1">
          Automated Economic Order Quantity (EOQ), stockout radar, multi-store inventory allocation,
          and scenario simulation sandbox.
        </p>
      </div>

      {/* Navigation Sub-Tabs */}
      <div className="flex border-b border-slate-800 space-x-2">
        <button
          onClick={() => setActiveTab('reorder')}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition ${
            activeTab === 'reorder'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Reorder Engine ({reorders.length})
        </button>
        <button
          onClick={() => setActiveTab('stockout')}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition ${
            activeTab === 'stockout'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Stockout Risk Radar ({stockouts.length})
        </button>
        <button
          onClick={() => {
            setActiveTab('allocation');
            if (!allocationResult) handleRunAllocation();
          }}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition ${
            activeTab === 'allocation'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Multi-Store Allocation
        </button>
        <button
          onClick={() => {
            setActiveTab('simulator');
            if (!simResult) handleRunSimulation();
          }}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition ${
            activeTab === 'simulator'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          What-If Simulator Sandbox
        </button>
      </div>

      {loading ? (
        <LoadingSkeleton rows={8} />
      ) : (
        <>
          {/* TAB 1: REORDER RECOMMENDATIONS */}
          {activeTab === 'reorder' && (
            <div className="space-y-4">
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-sm">
                <div>
                  <h3 className="text-sm font-bold text-white">Algorithmic Reorder Engine</h3>
                  <p className="text-xs text-slate-400">
                    Calculated using: [Lead Time Demand] + [Safety Stock] - [Current Stock] with
                    EOQ and MOQ constraints.
                  </p>
                </div>
                <div className="text-xs font-semibold text-slate-300">
                  Total Replenishment Capital:{' '}
                  <span className="text-emerald-400 font-bold">
                    ₹
                    {reorders
                      .reduce((acc, r) => acc + r.estimated_cost, 0)
                      .toLocaleString('en-IN')}
                  </span>
                </div>
              </div>

              <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                      <tr>
                        <th className="py-3 px-4">SKU / Product</th>
                        <th className="py-3 px-4">Store</th>
                        <th className="py-3 px-4 text-right">Current Stock</th>
                        <th className="py-3 px-4 text-right">Lead-Time Demand</th>
                        <th className="py-3 px-4 text-right">Safety Buffer</th>
                        <th className="py-3 px-4 text-right">Reorder Pt</th>
                        <th className="py-3 px-4 text-right">Suggested Qty</th>
                        <th className="py-3 px-4 text-right">Cost (₹)</th>
                        <th className="py-3 px-4 text-center">Urgency</th>
                        <th className="py-3 px-4">Algorithmic Rationale</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60">
                      {reorders.map((r) => (
                        <tr
                          key={`${r.product_id}-${r.store_id}`}
                          className="hover:bg-slate-800/40 transition"
                        >
                          <td className="py-3 px-4 font-semibold text-white max-w-[180px] truncate">
                            {r.product_name}
                            <span className="block text-[10px] text-slate-500 font-mono">{r.sku}</span>
                          </td>
                          <td className="py-3 px-4 text-slate-300">{r.store_name}</td>
                          <td className="py-3 px-4 text-right font-bold text-white">
                            {r.current_stock}
                          </td>
                          <td className="py-3 px-4 text-right text-slate-400">
                            {r.lead_time_demand}
                          </td>
                          <td className="py-3 px-4 text-right text-slate-400">{r.safety_stock}</td>
                          <td className="py-3 px-4 text-right text-slate-400">{r.reorder_point}</td>
                          <td className="py-3 px-4 text-right font-bold text-blue-400">
                            {r.recommended_reorder_qty}
                          </td>
                          <td className="py-3 px-4 text-right font-medium text-emerald-400">
                            ₹{r.estimated_cost.toLocaleString('en-IN')}
                          </td>
                          <td className="py-3 px-4 text-center">
                            <Badge status={r.urgency} size="sm" />
                          </td>
                          <td className="py-3 px-4 text-slate-400 text-[11px] max-w-[220px] truncate">
                            {r.reason}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: STOCKOUT RISK RADAR */}
          {activeTab === 'stockout' && (
            <div className="space-y-4">
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-sm">
                <div>
                  <h3 className="text-sm font-bold text-white">Stockout Exposure Radar</h3>
                  <p className="text-xs text-slate-400">
                    Products projected to deplete before standard supplier replenishment lead-time.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {stockouts.map((s) => (
                  <div
                    key={`${s.product_id}-${s.store_id}`}
                    className="p-5 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col justify-between hover:border-slate-700 transition shadow-sm"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <Badge status={s.risk_level} size="sm" />
                        <span className="text-[10px] text-slate-500 font-mono">{s.store_name}</span>
                      </div>
                      <h4 className="text-sm font-bold text-white mb-1">{s.product_name}</h4>
                      <span className="text-[10px] text-slate-500 font-mono block mb-3">{s.sku}</span>

                      <div className="grid grid-cols-3 gap-2 bg-slate-950 p-2.5 rounded-lg border border-slate-800 mb-3 text-center text-xs">
                        <div>
                          <span className="text-[10px] text-slate-500 block">Stock</span>
                          <span className="font-bold text-white">{s.current_stock}</span>
                        </div>
                        <div>
                          <span className="text-[10px] text-slate-500 block">Lead Time</span>
                          <span className="font-medium text-slate-300">{s.lead_time_days}d</span>
                        </div>
                        <div>
                          <span className="text-[10px] text-slate-500 block">Cover Remaining</span>
                          <span
                            className={`font-bold ${
                              (s.days_of_stock_remaining || 0) <= 3
                                ? 'text-rose-400'
                                : 'text-amber-400'
                            }`}
                          >
                            {s.days_of_stock_remaining !== null
                              ? `${s.days_of_stock_remaining}d`
                              : '-'}
                          </span>
                        </div>
                      </div>

                      <p className="text-xs text-slate-300 leading-relaxed">{s.explanation}</p>
                    </div>

                    <div className="pt-3 border-t border-slate-800/80 mt-4 flex items-center justify-between">
                      <span className="text-[11px] font-semibold text-blue-400">
                        Urgent Replenishment Required
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 3: MULTI-STORE ALLOCATION */}
          {activeTab === 'allocation' && (
            <div className="space-y-6">
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm">
                <h3 className="text-base font-bold text-white mb-1">
                  Constrained Inventory Allocation Engine
                </h3>
                <p className="text-xs text-slate-400 mb-4">
                  When central supply is limited, mathematically allocate units across network
                  locations based on projected demand velocity and current shortage gaps.
                </p>

                <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end">
                  <div className="sm:col-span-2">
                    <label className="block text-xs font-semibold text-slate-300 mb-1">
                      Target SKU to Distribute
                    </label>
                    <select
                      value={allocProductId}
                      onChange={(e) => setAllocProductId(Number(e.target.value))}
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
                      Available Supply Batch
                    </label>
                    <input
                      type="number"
                      min="1"
                      value={allocQuantity}
                      onChange={(e) => setAllocQuantity(Math.max(1, Number(e.target.value)))}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                    />
                  </div>

                  <div>
                    <button
                      onClick={handleRunAllocation}
                      disabled={allocating}
                      className="w-full py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow transition disabled:opacity-50"
                    >
                      {allocating ? 'Calculating...' : 'Compute Allocation'}
                    </button>
                  </div>
                </div>
              </div>

              {allocationResult && (
                <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
                  <div className="p-4 bg-slate-950/60 border-b border-slate-800 flex justify-between items-center">
                    <div>
                      <span className="text-xs font-bold uppercase text-slate-300">
                        Suggested Allocation Plan: {allocationResult.product_name}
                      </span>
                      <p className="text-[11px] text-slate-500">
                        Allocating {allocationResult.total_quantity_allocated} of{' '}
                        {allocationResult.total_quantity_available} units
                      </p>
                    </div>
                    <span className="text-xs text-emerald-400 font-semibold">
                      Max Shortage Covered
                    </span>
                  </div>

                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-950/40 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                      <tr>
                        <th className="py-3 px-4">Store Location</th>
                        <th className="py-3 px-4 text-right">Current Stock</th>
                        <th className="py-3 px-4 text-right">14-Day Demand</th>
                        <th className="py-3 px-4 text-right">Shortage Deficit</th>
                        <th className="py-3 px-4 text-right">Allocated Units</th>
                        <th className="py-3 px-4 text-right">Post-Allocation Stock</th>
                        <th className="py-3 px-4">Optimization Rationale</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60">
                      {allocationResult.allocations.map((a: any) => (
                        <tr key={a.store_id} className="hover:bg-slate-800/40 transition">
                          <td className="py-3 px-4 font-bold text-white">{a.store_name}</td>
                          <td className="py-3 px-4 text-right text-slate-300">{a.current_stock}</td>
                          <td className="py-3 px-4 text-right text-slate-400">
                            {a.forecast_demand}
                          </td>
                          <td className="py-3 px-4 text-right font-medium text-amber-400">
                            {a.shortage}
                          </td>
                          <td className="py-3 px-4 text-right font-bold text-blue-400 text-sm">
                            +{a.allocated_quantity}
                          </td>
                          <td className="py-3 px-4 text-right font-bold text-emerald-400">
                            {a.current_stock + a.allocated_quantity}
                          </td>
                          <td className="py-3 px-4 text-slate-400 text-[11px]">{a.reason}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* TAB 4: WHAT-IF SIMULATOR SANDBOX */}
          {activeTab === 'simulator' && (
            <div className="space-y-6">
              {/* Notice Banner */}
              <div className="p-3.5 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-300 text-xs flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <Sliders className="w-4 h-4 shrink-0 text-purple-400" />
                  <span className="font-semibold uppercase tracking-wider text-[11px]">
                    SIMULATION MODE: Mathematical Sandbox
                  </span>
                  <span className="text-slate-400 hidden md:inline">
                    — Adjust variables to explore stress tests. Database records will NOT be
                    modified.
                  </span>
                </div>
                <button
                  onClick={handleRunSimulation}
                  disabled={simulating}
                  className="px-3 py-1 rounded bg-purple-600 hover:bg-purple-500 text-white text-[11px] font-bold transition disabled:opacity-50"
                >
                  {simulating ? 'Calculating...' : 'Recalculate'}
                </button>
              </div>

              {/* Slider Controls */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-4 shadow-sm">
                  <h3 className="text-sm font-bold text-white mb-2">Simulation Parameters</h3>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1">
                      Store
                    </label>
                    <select
                      value={simStoreId}
                      onChange={(e) => setSimStoreId(Number(e.target.value))}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    >
                      {stores.map((s) => (
                        <option key={s.store_id} value={s.store_id}>
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1">
                      Product SKU
                    </label>
                    <select
                      value={simProductId}
                      onChange={(e) => setSimProductId(Number(e.target.value))}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    >
                      {products.map((p) => (
                        <option key={p.product_id} value={p.product_id}>
                          {p.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Demand Growth Slider */}
                  <div className="pt-2">
                    <div className="flex justify-between text-xs font-semibold mb-1">
                      <span className="text-slate-300">Demand Surge / Drop</span>
                      <span className="text-blue-400 font-bold">{simDemandGrowth}%</span>
                    </div>
                    <input
                      type="range"
                      min="-50"
                      max="100"
                      step="5"
                      value={simDemandGrowth}
                      onChange={(e) => setSimDemandGrowth(Number(e.target.value))}
                      className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-500"
                    />
                    <div className="flex justify-between text-[10px] text-slate-500 mt-1">
                      <span>-50% Recession</span>
                      <span>0%</span>
                      <span>+100% Festival Peak</span>
                    </div>
                  </div>

                  {/* Lead Time Slider */}
                  <div className="pt-2">
                    <div className="flex justify-between text-xs font-semibold mb-1">
                      <span className="text-slate-300">Supplier Delay / Speedup</span>
                      <span className="text-amber-400 font-bold">
                        {simLeadTimeChange >= 0 ? `+${simLeadTimeChange}` : simLeadTimeChange} days
                      </span>
                    </div>
                    <input
                      type="range"
                      min="-10"
                      max="20"
                      step="1"
                      value={simLeadTimeChange}
                      onChange={(e) => setSimLeadTimeChange(Number(e.target.value))}
                      className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
                    />
                    <div className="flex justify-between text-[10px] text-slate-500 mt-1">
                      <span>-10d Fast track</span>
                      <span>0d</span>
                      <span>+20d Logistics disruption</span>
                    </div>
                  </div>

                  {/* Price Change Slider */}
                  <div className="pt-2">
                    <div className="flex justify-between text-xs font-semibold mb-1">
                      <span className="text-slate-300">Retail Price Adjustment</span>
                      <span className="text-emerald-400 font-bold">
                        {simPriceChange >= 0 ? `+${simPriceChange}` : simPriceChange}%
                      </span>
                    </div>
                    <input
                      type="range"
                      min="-30"
                      max="30"
                      step="5"
                      value={simPriceChange}
                      onChange={(e) => setSimPriceChange(Number(e.target.value))}
                      className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-emerald-500"
                    />
                  </div>
                </div>

                {/* Simulation Output Cards (2 Cols) */}
                <div className="lg:col-span-2 space-y-4">
                  {simResult ? (
                    <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-sm space-y-6">
                      <div>
                        <span className="text-xs text-slate-400 uppercase tracking-wider font-semibold">
                          Simulated Impact Summary
                        </span>
                        <h4 className="text-lg font-bold text-white mt-1">
                          Scenario Assessment for {simResult.product_name}
                        </h4>
                      </div>

                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                        <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                          <span className="text-[11px] text-slate-400 block">Baseline Demand</span>
                          <span className="text-lg font-bold text-slate-200">
                            {simResult.baseline_demand}
                          </span>
                        </div>
                        <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                          <span className="text-[11px] text-slate-400 block">Simulated Demand</span>
                          <span className="text-lg font-bold text-blue-400">
                            {simResult.simulated_demand}
                          </span>
                        </div>
                        <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                          <span className="text-[11px] text-slate-400 block">Stockout Risk</span>
                          <span className="text-lg font-bold capitalize text-amber-400">
                            {simResult.simulated_stockout_risk}
                          </span>
                        </div>
                        <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                          <span className="text-[11px] text-slate-400 block">Required Order</span>
                          <span className="text-lg font-bold text-emerald-400">
                            {simResult.simulated_reorder_qty} units
                          </span>
                        </div>
                      </div>

                      <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 space-y-2 text-xs">
                        <div className="flex justify-between">
                          <span className="text-slate-400">Current On Hand Stock:</span>
                          <span className="font-semibold text-white">
                            {simResult.current_stock} units
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-400">Simulated Lead Time:</span>
                          <span className="font-semibold text-white">
                            {simResult.simulated_lead_time_days} days
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-400">Projected Unit Shortage Deficit:</span>
                          <span className="font-bold text-rose-400">
                            {simResult.simulated_shortage} units
                          </span>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="p-12 text-center text-slate-500 text-xs">
                      Adjust sliders to compute instant what-if inventory scenarios.
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
