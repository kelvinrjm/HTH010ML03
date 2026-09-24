import React from 'react';

interface BadgeProps {
  status: string;
  size?: 'sm' | 'md';
}

export const Badge: React.FC<BadgeProps> = ({ status, size = 'md' }) => {
  const s = status.toLowerCase();

  let styles = 'bg-slate-800 text-slate-300 border-slate-700';

  if (s === 'critical' || s === 'out of stock' || s === 'write-off') {
    styles = 'bg-rose-500/15 text-rose-400 border-rose-500/30';
  } else if (s === 'low' || s === 'low stock' || s === 'discount' || s === 'submitted') {
    styles = 'bg-amber-500/15 text-amber-400 border-amber-500/30';
  } else if (s === 'healthy' || s === 'normal' || s === 'received' || s === 'completed') {
    styles = 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30';
  } else if (s === 'overstock' || s === 'markdown') {
    styles = 'bg-blue-500/15 text-blue-400 border-blue-500/30';
  } else if (s === 'dead_stock' || s === 'dead stock') {
    styles = 'bg-purple-500/15 text-purple-400 border-purple-500/30';
  } else if (s === 'draft') {
    styles = 'bg-slate-500/15 text-slate-400 border-slate-500/30';
  }

  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-xs';

  return (
    <span
      className={`inline-flex items-center font-semibold rounded-full border capitalize tracking-wide ${sizeClasses} ${styles}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5 opacity-80" />
      {status.replace(/_/g, ' ')}
    </span>
  );
};
