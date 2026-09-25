import React from 'react';

export const LoadingSkeleton: React.FC<{ rows?: number }> = ({ rows = 4 }) => {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="h-8 bg-slate-800/80 rounded-lg w-1/4" />
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-28 bg-slate-900 border border-slate-800 rounded-xl" />
        ))}
      </div>
      <div className="space-y-2 mt-6">
        {[...Array(rows)].map((_, i) => (
          <div key={i} className="h-12 bg-slate-900 border border-slate-800/60 rounded-lg" />
        ))}
      </div>
    </div>
  );
};
