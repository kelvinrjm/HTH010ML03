import React, { useEffect, useState } from 'react';
import {
  DollarSign,
  TrendingUp,
  AlertTriangle,
  AlertOctagon,
  Sparkles,
  ArrowUpRight,
  Archive,
  Layers,
  ShoppingBag,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { Badge } from '../components/Badge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { StatCard } from '../components/StatCard';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { DashboardOverview } from '../types';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .getDashboardOverview()
      .then(setData)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading || !data) {
    return <LoadingSkeleton rows={6} />;
  }

  const { kpis, sales_trend, inventory_health, top_products, slow_moving, store_performance, ai_insights, recent_activity } = data;

  return (
    <div className="space-y-8 pb-12">
      {/* Executive Welcome Greeting */}
      <div>
        <h2 className="text-2xl font-bold tracking-tight text-white">
          Good day, {user?.username}
        </h2>
        <p className="text-sm text-slate-400 mt-1">
          Here is your real-time inventory intelligence and operational state for today.
        </p>
      </div>

      {/* Primary Financial & Volume KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Inventory Value"
          value={`₹${kpis.total_inventory_value.toLocaleString('en-IN')}`}
          subtitle="Capital deployed in active stock"
          icon={<DollarSign className="w-4 h-4" />}
          accentColor="blue"
        />
        <StatCard
          title="Today's Sales"
          value={`₹${kpis.today_sales.toLocaleString('en-IN')}`}
          subtitle="Day-to-date trading gross"
          icon={<ShoppingBag className="w-4 h-4" />}
          accentColor="emerald"
        />
        <StatCard
          title="Monthly Gross Sales"
          value={`₹${kpis.monthly_sales.toLocaleString('en-IN')}`}
          subtitle={`₹${kpis.estimated_monthly_profit.toLocaleString('en-IN')} estimated profit`}
          icon={<TrendingUp className="w-4 h-4" />}
          accentColor="emerald"
        />
        <StatCard
          title="Forecast Precision"
          value={kpis.forecast_accuracy_mape ? `${(100 - kpis.forecast_accuracy_mape).toFixed(1)}%` : 'Active'}
          subtitle={`MAE 2.3 | ${kpis.forecast_model_status}`}
          icon={<Sparkles className="w-4 h-4" />}
          accentColor="purple"
        />
      </div>

      {/* Operational Risk & Alert Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Low Stock Warning"
          value={`${kpis.low_stock_count} SKUs`}
          subtitle="Approaching or below reorder point"
          icon={<AlertTriangle className="w-4 h-4" />}
          accentColor="amber"
        />
        <StatCard
          title="Critical Stockout Exposure"
          value={`${kpis.stockout_risk_count} SKUs`}
          subtitle="Stock depleted or < lead-time demand"
          icon={<AlertOctagon className="w-4 h-4" />}
          accentColor="rose"
        />
        <StatCard
          title="Dead Stock Items"
          value={`${kpis.dead_stock_count} SKUs`}
          subtitle="Zero sales in past 90+ days"
          icon={<Archive className="w-4 h-4" />}
          accentColor="purple"
        />
      </div>

      {/* AI Business Intelligence Layer */}
      <div className="bg-gradient-to-r from-blue-950/40 via-slate-900 to-slate-900 border border-blue-500/20 rounded-2xl p-6 shadow-md">
        <div className="flex items-center space-x-2.5 mb-4">
          <div className="p-2 bg-blue-500/10 rounded-lg text-blue-400 border border-blue-500/20">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-semibold text-white">AI Business Insights & Action Items</h3>
            <p className="text-xs text-slate-400">
              Algorithmic intelligence derived from lead-times, velocity shifts, and capital turnover.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {ai_insights.map((insight) => (
            <div
              key={insight.id}
              className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex flex-col justify-between hover:border-slate-700 transition"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <Badge status={insight.urgency} size="sm" />
                  {insight.store_name && (
                    <span className="text-[10px] text-slate-400 font-mono">{insight.store_name}</span>
                  )}
                </div>
                <h4 className="text-sm font-bold text-white mb-1">{insight.title}</h4>
                <p className="text-xs text-slate-300 leading-relaxed mb-2">{insight.description}</p>
                <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800/80 mb-3">
                  <p className="text-[11px] text-slate-400 leading-snug">
                    <strong className="text-slate-300">Why it matters:</strong> {insight.why_it_matters}
                  </p>
                </div>
              </div>

              <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between">
                <span className="text-[10px] font-semibold text-blue-400">
                  {insight.recommended_action}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Sales Trend Chart & Inventory Health Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Sales Trajectory (2 Cols) */}
        <div className="lg:col-span-2 bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-base font-bold text-white">Sales Performance & Revenue Trajectory</h3>
              <p className="text-xs text-slate-400">30-day chronological daily revenue & gross profit</p>
            </div>
            <div className="flex items-center space-x-3 text-xs">
              <span className="flex items-center space-x-1.5 text-blue-400">
                <span className="w-2.5 h-2.5 rounded-sm bg-blue-500" />
                <span>Revenue</span>
              </span>
              <span className="flex items-center space-x-1.5 text-emerald-400">
                <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500" />
                <span>Profit</span>
              </span>
            </div>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={sales_trend}>
                <defs>
                  <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#3B82F6" stopOpacity={0.0} />
                  </linearGradient>
                  <linearGradient id="colorProfit" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <XAxis
                  dataKey="date"
                  stroke="#64748B"
                  fontSize={10}
                  tickFormatter={(val) => val.slice(5)}
                />
                <YAxis stroke="#64748B" fontSize={10} tickFormatter={(val) => `₹${val}`} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0F172A',
                    borderColor: '#334155',
                    borderRadius: '8px',
                    fontSize: '12px',
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="revenue"
                  stroke="#3B82F6"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#colorRevenue)"
                />
                <Area
                  type="monotone"
                  dataKey="profit"
                  stroke="#10B981"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#colorProfit)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Inventory Velocity Categories (1 Col) */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-bold text-white">Inventory Health</h3>
              <Layers className="w-4 h-4 text-slate-400" />
            </div>
            <p className="text-xs text-slate-400 mb-5">
              Portfolio distribution by sales velocity and turnover rate.
            </p>

            <div className="space-y-4">
              {inventory_health.map((cat) => (
                <div key={cat.category} className="space-y-1.5">
                  <div className="flex justify-between text-xs font-semibold">
                    <span className="text-slate-300">{cat.category}</span>
                    <span className="text-slate-400">
                      ₹{cat.total_value.toLocaleString('en-IN')} ({cat.percentage_of_inventory}%)
                    </span>
                  </div>
                  <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full ${
                        cat.category === 'Fast Moving'
                          ? 'bg-blue-500'
                          : cat.category === 'Normal Turnover'
                          ? 'bg-emerald-500'
                          : cat.category === 'Slow Moving'
                          ? 'bg-amber-500'
                          : 'bg-purple-500'
                      }`}
                      style={{ width: `${cat.percentage_of_inventory}%` }}
                    />
                  </div>
                  <span className="text-[10px] text-slate-500 block">
                    {cat.product_count} SKUs • {cat.total_units} units on hand
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Top Best-Selling Products & Slow-Moving At-Risk Products */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Movers */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-sm">
          <h3 className="text-base font-bold text-white mb-1">Top Best-Selling SKUs</h3>
          <p className="text-xs text-slate-400 mb-4">Highest revenue contribution in the past 30 days</p>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="text-slate-400 border-b border-slate-800/80 pb-2">
                  <th className="pb-2 font-semibold">Product</th>
                  <th className="pb-2 font-semibold text-center">Category</th>
                  <th className="pb-2 font-semibold text-right">Units Sold</th>
                  <th className="pb-2 font-semibold text-right">Revenue</th>
                  <th className="pb-2 font-semibold text-right">Margin</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {top_products.map((p) => (
                  <tr key={p.product_id} className="hover:bg-slate-800/30 transition">
                    <td className="py-2.5 font-medium text-white max-w-[160px] truncate">
                      {p.name}
                      <span className="block text-[10px] text-slate-500 font-mono">{p.sku}</span>
                    </td>
                    <td className="py-2.5 text-center text-slate-400">{p.category_name}</td>
                    <td className="py-2.5 text-right font-semibold text-slate-200">{p.units_sold}</td>
                    <td className="py-2.5 text-right font-bold text-blue-400">
                      ₹{p.revenue.toLocaleString('en-IN')}
                    </td>
                    <td className="py-2.5 text-right text-emerald-400 font-semibold">{p.profit_margin}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Slow Moving / Dead Stock Risk */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-sm">
          <h3 className="text-base font-bold text-white mb-1">Slow-Moving & Dead Stock Risk</h3>
          <p className="text-xs text-slate-400 mb-4">Idle stock tying up working capital</p>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="text-slate-400 border-b border-slate-800/80 pb-2">
                  <th className="pb-2 font-semibold">Product</th>
                  <th className="pb-2 font-semibold">Store</th>
                  <th className="pb-2 font-semibold text-right">Stock</th>
                  <th className="pb-2 font-semibold text-right">Days Dormant</th>
                  <th className="pb-2 font-semibold text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {slow_moving.map((s) => (
                  <tr key={`${s.product_id}-${s.store_name}`} className="hover:bg-slate-800/30 transition">
                    <td className="py-2.5 font-medium text-white max-w-[150px] truncate">
                      {s.name}
                      <span className="block text-[10px] text-slate-500 font-mono">₹{s.estimated_value_at_risk} at risk</span>
                    </td>
                    <td className="py-2.5 text-slate-400 truncate max-w-[120px]">{s.store_name}</td>
                    <td className="py-2.5 text-right font-semibold text-slate-200">{s.quantity_on_hand}</td>
                    <td className="py-2.5 text-right text-amber-400 font-semibold">{s.days_without_sale}d</td>
                    <td className="py-2.5 text-right">
                      <Badge status={s.recommended_action} size="sm" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Multi-Store Comparison & Activity Log */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Store Performance */}
        <div className="lg:col-span-2 bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-sm">
          <h3 className="text-base font-bold text-white mb-1">Multi-Store Performance Matrix</h3>
          <p className="text-xs text-slate-400 mb-4">Store volume, revenue contribution, and inventory health</p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {store_performance.map((st) => (
              <div key={st.store_id} className="p-4 rounded-xl bg-slate-950/70 border border-slate-800/80">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-sm text-white">{st.store_name}</span>
                  <span className="text-[10px] font-semibold text-blue-400 uppercase bg-blue-500/10 px-2 py-0.5 rounded">
                    {st.store_type}
                  </span>
                </div>
                <div className="grid grid-cols-2 gap-2 mt-3 text-xs">
                  <div>
                    <span className="text-slate-500 block text-[10px]">Total Revenue</span>
                    <span className="font-bold text-emerald-400">₹{st.total_revenue.toLocaleString('en-IN')}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Stock Value</span>
                    <span className="font-bold text-slate-200">₹{st.total_inventory_value.toLocaleString('en-IN')}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Active SKUs</span>
                    <span className="font-medium text-slate-300">{st.active_products} Products</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Stock Alerts</span>
                    <span className={`font-semibold ${st.low_stock_items > 0 ? 'text-amber-400' : 'text-slate-400'}`}>
                      {st.low_stock_items} Low Stock
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Recent Audit / Transaction Stream */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-sm">
          <h3 className="text-base font-bold text-white mb-1">Recent Activity</h3>
          <p className="text-xs text-slate-400 mb-4">Latest transactions & replenishment events</p>

          <div className="space-y-3">
            {recent_activity.map((act) => (
              <div key={act.id} className="flex items-start space-x-3 p-2.5 rounded-lg bg-slate-950/40 border border-slate-800/40">
                <div className="p-1.5 rounded-md bg-blue-500/10 text-blue-400 border border-blue-500/20 shrink-0">
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-semibold text-slate-200 truncate">{act.title}</p>
                  <p className="text-[11px] text-slate-400 truncate">{act.details}</p>
                  <span className="text-[9px] text-slate-500 block mt-0.5">
                    {act.timestamp ? act.timestamp.replace('T', ' ').slice(0, 16) : ''} • {act.actor}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
