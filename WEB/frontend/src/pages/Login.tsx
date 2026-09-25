import React, { useState } from 'react';
import { ArrowRight, Lock, User as UserIcon, ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('Admin@123');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(username, password);
    } catch (err: any) {
      setError(err.message || 'Failed to authenticate');
    } finally {
      setLoading(false);
    }
  };

  const fillCredentials = (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center items-center p-6 relative overflow-hidden">
      {/* Background glow accents */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/3 w-80 h-80 bg-emerald-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md z-10">
        {/* Brand identity with official circular logo */}
        <div className="text-center mb-8">
          <div className="inline-flex p-1.5 rounded-full bg-slate-900/90 border-2 border-blue-500/40 shadow-2xl shadow-blue-500/20 mb-4 hover:border-blue-400 transition">
            <img
              src="/logo.png"
              alt="STOCKSENSE Official Logo"
              className="w-20 h-20 object-cover rounded-full shadow-inner"
            />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-white font-mono">
            STOCKSENSE
          </h1>
          <p className="mt-2 text-sm text-slate-400 font-medium">
            Predict Demand. Prevent Stockouts. Make Smarter Decisions.
          </p>
        </div>

        {/* Login Card */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-8 shadow-2xl backdrop-blur-xl">
          {error && (
            <div className="mb-5 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs flex items-center space-x-2">
              <ShieldAlert className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Username
              </label>
              <div className="relative">
                <UserIcon className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Enter username"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-3 px-4 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-lg text-sm transition-all duration-150 flex items-center justify-center space-x-2 shadow-lg shadow-blue-600/20 disabled:opacity-50"
            >
              <span>{loading ? 'Authenticating...' : 'Sign In to Workspace'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Quick Demo Access Bar */}
          <div className="mt-6 pt-6 border-t border-slate-800/80">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block text-center mb-3">
              One-Click Demo Credentials
            </span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => fillCredentials('admin', 'Admin@123')}
                className="py-1.5 px-2 bg-slate-800/60 hover:bg-slate-800 border border-slate-700/50 rounded text-xs text-slate-300 text-center transition"
              >
                Admin (Full Access)
              </button>
              <button
                type="button"
                onClick={() => fillCredentials('staff', 'Staff@123')}
                className="py-1.5 px-2 bg-slate-800/60 hover:bg-slate-800 border border-slate-700/50 rounded text-xs text-slate-300 text-center transition"
              >
                Staff (Store Operations)
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
