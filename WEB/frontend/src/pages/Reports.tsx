import React from 'react';
import {
  FileSpreadsheet,
  Download,
  Boxes,
  ShoppingCart,
  Truck,
  Archive,
  ShieldCheck,
  TrendingUp,
} from 'lucide-react';
import { api } from '../services/api';

export const Reports: React.FC = () => {
  const downloadReport = (endpoint: string, filename: string) => {
    const token = localStorage.getItem('stocksense_token');
    fetch(endpoint, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
      .then((res) => {
        if (!res.ok) throw new Error('Download failed');
        return res.blob();
      })
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
      })
      .catch((err) => alert(err.message || 'Export error'));
  };

  const reportCards = [
    {
      id: 'inventory',
      title: 'Inventory Valuation & Health Audit',
      description:
        'Complete stock on hand, reservations, valuation at cost and retail, safety thresholds, and health classifications across all retail locations.',
      icon: <Boxes className="w-6 h-6 text-blue-400" />,
      endpoint: api.getInventoryCsvUrl(),
      filename: `stocksense_inventory_report_${new Date().toISOString().slice(0, 10)}.csv`,
      frequency: 'Live Synchronized',
      format: 'CSV Export',
    },
    {
      id: 'sales',
      title: 'Sales & Revenue Transaction Ledger',
      description:
        'Chronological order records, customer channels, revenue totals, gross margin performance, and itemized transaction receipts.',
      icon: <ShoppingCart className="w-6 h-6 text-emerald-400" />,
      endpoint: api.getSalesCsvUrl(),
      filename: `stocksense_sales_ledger_${new Date().toISOString().slice(0, 10)}.csv`,
      frequency: 'Live Synchronized',
      format: 'CSV Export',
    },
    {
      id: 'purchases',
      title: 'Procurement & Purchase Orders Report',
      description:
        'Detailed supplier purchase orders, lead times, order dates, receipt dates, fulfillment status, and procurement expenditures.',
      icon: <Truck className="w-6 h-6 text-purple-400" />,
      endpoint: '/api/reports/purchases/csv',
      filename: `stocksense_purchases_report_${new Date().toISOString().slice(0, 10)}.csv`,
      frequency: 'Live Synchronized',
      format: 'CSV Export',
    },
    {
      id: 'dead_stock',
      title: 'Dead Stock & Slow-Moving Capital Risk',
      description:
        'Comprehensive breakdown of dormant inventory with 90+ days without sales, tied-up working capital, and automated clearance strategies.',
      icon: <Archive className="w-6 h-6 text-amber-400" />,
      endpoint: '/api/reports/dead-stock/csv',
      filename: `stocksense_dead_stock_${new Date().toISOString().slice(0, 10)}.csv`,
      frequency: 'Live Synchronized',
      format: 'CSV Export',
    },
    {
      id: 'forecast',
      title: 'Demand Forecast & Variance Projections',
      description:
        'Predictive demand matrices across stores and SKUs, 95% confidence bounds, and projected stockout dates for upcoming periods.',
      icon: <TrendingUp className="w-6 h-6 text-cyan-400" />,
      endpoint: '/api/reports/forecast/csv',
      filename: `stocksense_demand_forecast_${new Date().toISOString().slice(0, 10)}.csv`,
      frequency: 'Daily Batch',
      format: 'CSV Export',
    },
    {
      id: 'audit',
      title: 'Enterprise Compliance Audit Log',
      description:
        'Immutable double-entry log of all inventory transfers, manual count corrections, price changes, and system adjustments.',
      icon: <ShieldCheck className="w-6 h-6 text-rose-400" />,
      endpoint: '/api/reports/audit/csv',
      filename: `stocksense_audit_trail_${new Date().toISOString().slice(0, 10)}.csv`,
      frequency: 'Real-Time Append',
      format: 'CSV Export',
    },
  ];

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
          <FileSpreadsheet className="w-6 h-6 text-blue-400" />
          Enterprise Reports & Data Export Center
        </h2>
        <p className="text-sm text-slate-400 mt-1">
          Export audit-ready CSV datasets for business intelligence, accounting reconciliations, and
          compliance reporting.
        </p>
      </div>

      {/* Grid of Report Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {reportCards.map((report) => (
          <div
            key={report.id}
            className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 flex flex-col justify-between hover:border-slate-700 transition shadow-sm"
          >
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800">
                  {report.icon}
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20">
                    {report.format}
                  </span>
                </div>
              </div>

              <h3 className="text-base font-bold text-white mb-2">{report.title}</h3>
              <p className="text-xs text-slate-400 leading-relaxed mb-4">{report.description}</p>
            </div>

            <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between">
              <span className="text-[10px] text-slate-500 font-medium">
                Update Cadence: <strong className="text-slate-400">{report.frequency}</strong>
              </span>

              <button
                onClick={() => downloadReport(report.endpoint, report.filename)}
                className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-sm transition"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Export CSV</span>
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
