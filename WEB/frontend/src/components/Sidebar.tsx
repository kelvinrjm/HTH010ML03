import React from 'react';
import {
  LayoutDashboard,
  Boxes,
  Package,
  ShoppingCart,
  Truck,
  TrendingUp,
  BrainCircuit,
  FileSpreadsheet,
  ShieldCheck,
  LogOut,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export type NavTab =
  | 'dashboard'
  | 'inventory'
  | 'products'
  | 'sales'
  | 'purchases'
  | 'forecasting'
  | 'smart_decisions'
  | 'reports'
  | 'admin';

interface SidebarProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onTabChange }) => {
  const { user, logout, isAdmin } = useAuth();

  const navItems: { id: NavTab; label: string; icon: React.ReactNode; adminOnly?: boolean }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
    { id: 'inventory', label: 'Inventory', icon: <Boxes className="w-5 h-5" /> },
    { id: 'products', label: 'Products Catalog', icon: <Package className="w-5 h-5" /> },
    { id: 'sales', label: 'Sales & Checkout', icon: <ShoppingCart className="w-5 h-5" /> },
    { id: 'purchases', label: 'Purchases & Orders', icon: <Truck className="w-5 h-5" /> },
    { id: 'forecasting', label: 'Demand Forecasting', icon: <TrendingUp className="w-5 h-5" /> },
    { id: 'smart_decisions', label: 'Smart Decisions', icon: <BrainCircuit className="w-5 h-5" /> },
    { id: 'reports', label: 'Reports & Export', icon: <FileSpreadsheet className="w-5 h-5" /> },
    { id: 'admin', label: 'Administration', icon: <ShieldCheck className="w-5 h-5" />, adminOnly: true },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col h-screen fixed left-0 top-0 select-none z-30">
      {/* Brand Header with Official Circular Logo */}
      <div className="p-5 border-b border-slate-800/80 flex items-center space-x-3.5">
        <div className="w-10 h-10 rounded-full border border-blue-500/40 p-0.5 bg-slate-950 shrink-0 shadow-sm flex items-center justify-center">
          <img
            src="/logo.png"
            alt="STOCKSENSE Logo"
            className="w-full h-full object-cover rounded-full"
          />
        </div>
        <div className="flex flex-col">
          <span className="text-xl font-bold tracking-tight text-white font-mono flex items-center gap-1.5">
            STOCKSENSE
          </span>
          <span className="text-[10px] text-emerald-400 font-medium tracking-wide uppercase">
            AI Inventory Platform
          </span>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-1.5 overflow-y-auto">
        {navItems.map((item) => {
          if (item.adminOnly && !isAdmin) return null;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className={`w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                isActive
                  ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <span className={isActive ? 'text-blue-400' : 'text-slate-400'}>{item.icon}</span>
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* User Session Footer */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-900/60">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2.5 overflow-hidden">
            <div className="w-8 h-8 rounded-full bg-blue-600/30 text-blue-400 flex items-center justify-center font-bold text-xs border border-blue-500/30">
              {user?.username.slice(0, 2).toUpperCase()}
            </div>
            <div className="truncate">
              <p className="text-xs font-semibold text-slate-200 truncate">{user?.username}</p>
              <p className="text-[10px] text-slate-400 capitalize">{user?.role} Account</p>
            </div>
          </div>
          <button
            onClick={logout}
            title="Log Out"
            className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
