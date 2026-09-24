import React, { useEffect, useState } from 'react';
import { Store as StoreIcon, Activity, Sparkles } from 'lucide-react';
import { api } from '../services/api';
import { Store } from '../types';

interface NavbarProps {
  selectedStoreId: number | null;
  onStoreChange: (id: number | null) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ selectedStoreId, onStoreChange }) => {
  const [stores, setStores] = useState<Store[]>([]);

  useEffect(() => {
    api.getStores().then(setStores).catch(console.error);
  }, []);

  return (
    <header className="h-16 bg-slate-900/80 backdrop-blur-md border-b border-slate-800 sticky top-0 z-20 px-8 flex items-center justify-between">
      <div className="flex items-center space-x-4">
        {/* Store Selector Filter */}
        <div className="flex items-center space-x-2 bg-slate-800/80 border border-slate-700/60 rounded-lg px-3 py-1.5 shadow-sm">
          <StoreIcon className="w-4 h-4 text-blue-400" />
          <span className="text-xs font-medium text-slate-400">Store:</span>
          <select
            value={selectedStoreId || ''}
            onChange={(e) => onStoreChange(e.target.value ? Number(e.target.value) : null)}
            className="bg-transparent text-xs font-semibold text-slate-100 outline-none cursor-pointer pr-2"
          >
            <option value="" className="bg-slate-900 text-slate-200">
              All Stores (Consolidated)
            </option>
            {stores.map((s) => (
              <option key={s.store_id} value={s.store_id} className="bg-slate-900 text-slate-200">
                {s.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        {/* System Intelligence Active Badge */}
        <div className="hidden sm:flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-medium">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>AI Inference Active</span>
        </div>

        <div className="text-xs text-slate-400 hidden md:block">
          {new Date().toLocaleDateString('en-US', {
            weekday: 'short',
            month: 'short',
            day: 'numeric',
            year: 'numeric',
          })}
        </div>
      </div>
    </header>
  );
};
