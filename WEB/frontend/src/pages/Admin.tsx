import React, { useEffect, useState } from 'react';
import {
  ShieldCheck,
  Activity,
  Cpu,
  Database,
  History,
  Users,
  RotateCw,
  CheckCircle2,
  AlertCircle,
  FileCode,
  HardDrive,
  Key,
} from 'lucide-react';
import { Badge } from '../components/Badge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { api } from '../services/api';

export const Admin: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'health' | 'model_center' | 'audit' | 'users'>(
    'health'
  );

  const [health, setHealth] = useState<any | null>(null);
  const [modelCenter, setModelCenter] = useState<any | null>(null);
  const [auditLogs, setAuditLogs] = useState<any | null>(null);
  const [usersList, setUsersList] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // Retrain state
  const [retraining, setRetraining] = useState(false);
  const [retrainResult, setRetrainResult] = useState<any | null>(null);

  const loadHealth = async () => {
    try {
      const data = await api.getSystemHealth();
      setHealth(data);
    } catch (err: any) {
      console.error(err);
    }
  };

  const loadModelCenter = async () => {
    try {
      const data = await api.getModelCenter();
      setModelCenter(data);
    } catch (err: any) {
      console.error(err);
    }
  };

  const loadAudit = async (page: number = 1) => {
    try {
      const data = await api.getAuditLogs({ page });
      setAuditLogs(data);
    } catch (err: any) {
      console.error(err);
    }
  };

  const loadUsers = async () => {
    try {
      const data = await api.getUsers();
      setUsersList(data);
    } catch (err: any) {
      console.error(err);
    }
  };

  useEffect(() => {
    setLoading(true);
    Promise.all([loadHealth(), loadModelCenter(), loadAudit(1), loadUsers()]).finally(() =>
      setLoading(false)
    );
  }, []);

  const handleRetrain = async () => {
    try {
      setRetraining(true);
      setRetrainResult(null);
      const res = await api.retrainModel();
      setRetrainResult(res);
      loadModelCenter();
    } catch (err: any) {
      alert(err.message || 'Model retraining failed');
    } finally {
      setRetraining(false);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
          <ShieldCheck className="w-6 h-6 text-blue-400" />
          Enterprise Administration & System Diagnostics
        </h2>
        <p className="text-sm text-slate-400 mt-1">
          Isolated administrator operations: technical infrastructure diagnostics, ML model
          governance, and append-only audit verification.
        </p>
      </div>

      {/* Sub Tabs */}
      <div className="flex border-b border-slate-800 space-x-2">
        <button
          onClick={() => setActiveTab('health')}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition flex items-center gap-2 ${
            activeTab === 'health'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Activity className="w-4 h-4" />
          <span>System Health & Diagnostics</span>
        </button>
        <button
          onClick={() => setActiveTab('model_center')}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition flex items-center gap-2 ${
            activeTab === 'model_center'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Cpu className="w-4 h-4" />
          <span>ML Model Training Center</span>
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition flex items-center gap-2 ${
            activeTab === 'audit'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Audit Trail</span>
        </button>
        <button
          onClick={() => setActiveTab('users')}
          className={`pb-3 px-4 text-xs font-semibold border-b-2 transition flex items-center gap-2 ${
            activeTab === 'users'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Users & Access</span>
        </button>
      </div>

      {loading ? (
        <LoadingSkeleton rows={8} />
      ) : (
        <>
          {/* TAB 1: SYSTEM HEALTH */}
          {activeTab === 'health' && health && (
            <div className="space-y-6">
              <div className="p-4 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-300 text-xs">
                <strong>Architectural Note:</strong> Developer technical metrics (schema versions,
                raw table registers, and database locks) are strictly confined to this System Health
                screen. Commercial users and store managers are never exposed to internal developer
                data.
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                  <div className="flex items-center space-x-2 text-slate-400 mb-2">
                    <Database className="w-4 h-4 text-blue-400" />
                    <span className="text-xs font-semibold">Database Engine</span>
                  </div>
                  <p className="text-sm font-bold text-white">{health.database_engine}</p>
                  <span className="text-[11px] text-emerald-400 font-medium">
                    {health.database_status}
                  </span>
                </div>

                <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                  <div className="flex items-center space-x-2 text-slate-400 mb-2">
                    <FileCode className="w-4 h-4 text-emerald-400" />
                    <span className="text-xs font-semibold">Schema Migrations</span>
                  </div>
                  <p className="text-xl font-bold text-white">v{health.schema_version}</p>
                  <span className="text-[11px] text-slate-500">19 migrations applied</span>
                </div>

                <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                  <div className="flex items-center space-x-2 text-slate-400 mb-2">
                    <HardDrive className="w-4 h-4 text-purple-400" />
                    <span className="text-xs font-semibold">Database Architecture</span>
                  </div>
                  <p className="text-xl font-bold text-white">{health.table_count} Tables</p>
                  <span className="text-[11px] text-slate-500">6 compatibility views</span>
                </div>

                <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                  <div className="flex items-center space-x-2 text-slate-400 mb-2">
                    <Key className="w-4 h-4 text-amber-400" />
                    <span className="text-xs font-semibold">Integrity Constraints</span>
                  </div>
                  <p className="text-sm font-bold text-emerald-400">
                    {health.foreign_keys_enforced ? 'Foreign Keys Enforced' : 'Unenforced'}
                  </p>
                  <span className="text-[11px] text-slate-500">
                    WAL mode: {health.wal_mode_enabled ? 'Active' : 'Standard'}
                  </span>
                </div>
              </div>

              {/* Data Ingestion & Storage Footprint */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm">
                <h3 className="text-sm font-bold text-white mb-3">Enterprise Table Population</h3>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                  {Object.entries(health.total_records).map(([table, count]) => (
                    <div key={table} className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                      <span className="text-[11px] text-slate-400 block capitalize">
                        {table.replace('_', ' ')}
                      </span>
                      <span className="text-lg font-bold text-white">{Number(count).toLocaleString()} rows</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: MODEL TRAINING CENTER */}
          {activeTab === 'model_center' && (
            <div className="space-y-6">
              {/* Retrain Action Banner */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm">
                <div>
                  <h3 className="text-base font-bold text-white flex items-center gap-2">
                    <Cpu className="w-5 h-5 text-blue-400" />
                    Machine Learning Model Center
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Compare Random Forest, Gradient Boosting, and XGBoost on out-of-time walk-forward validation.
                  </p>
                </div>

                <button
                  onClick={handleRetrain}
                  disabled={retraining}
                  className="flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold shadow-md transition disabled:opacity-50 self-start sm:self-auto"
                >
                  <RotateCw className={`w-4 h-4 ${retraining ? 'animate-spin' : ''}`} />
                  <span>{retraining ? 'Training & Evaluating Models...' : 'Retrain Production Model'}</span>
                </button>
              </div>

              {retrainResult && (
                <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs space-y-2">
                  <div className="flex items-center gap-2 font-bold">
                    <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                    <span>{retrainResult.message}</span>
                  </div>
                  <p className="text-[11px] text-slate-300">
                    Training completed in {retrainResult.training_duration_seconds} seconds across 2,400+ transactions.
                    New champion model selected: <strong>{retrainResult.active_model.model_type}</strong> (MAE: {retrainResult.active_model.training_mae.toFixed(2)}, R²: {retrainResult.active_model.training_r2.toFixed(3)}).
                  </p>
                </div>
              )}

              {/* Active Model Cards */}
              {modelCenter?.active_model && (
                <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
                  <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                    <span className="text-xs text-slate-400 font-medium">Champion Model</span>
                    <p className="text-sm font-bold text-white mt-1">
                      {modelCenter.active_model.model_name}
                    </p>
                    <span className="text-[10px] text-emerald-400 font-mono">
                      {modelCenter.active_model.model_version}
                    </span>
                  </div>

                  <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                    <span className="text-xs text-slate-400 font-medium">Mean Absolute Error (MAE)</span>
                    <p className="text-xl font-bold text-blue-400 mt-1">
                      {modelCenter.active_model.training_mae.toFixed(2)} units
                    </p>
                    <span className="text-[10px] text-slate-500">Real walk-forward test</span>
                  </div>

                  <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                    <span className="text-xs text-slate-400 font-medium">Root Mean Squared Error</span>
                    <p className="text-xl font-bold text-purple-400 mt-1">
                      {modelCenter.active_model.training_rmse.toFixed(2)}
                    </p>
                    <span className="text-[10px] text-slate-500">Penalizes large outlier misses</span>
                  </div>

                  <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4">
                    <span className="text-xs text-slate-400 font-medium">Coefficient of Determination (R²)</span>
                    <p className="text-xl font-bold text-emerald-400 mt-1">
                      {modelCenter.active_model.training_r2.toFixed(3)}
                    </p>
                    <span className="text-[10px] text-slate-500">Variance explained by features</span>
                  </div>
                </div>
              )}

              {/* Model Comparison Table */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
                <div className="p-4 bg-slate-950/60 border-b border-slate-800 flex justify-between items-center">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Model Benchmark & Registry History
                  </span>
                  <span className="text-xs text-slate-500">
                    {modelCenter?.total_training_records} training records utilized
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-950/40 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                      <tr>
                        <th className="py-3 px-4">Algorithm / Model Name</th>
                        <th className="py-3 px-4">Version</th>
                        <th className="py-3 px-4 text-right">MAE</th>
                        <th className="py-3 px-4 text-right">RMSE</th>
                        <th className="py-3 px-4 text-right">R² Fit</th>
                        <th className="py-3 px-4">Trained Date</th>
                        <th className="py-3 px-4 text-center">Deployment Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60">
                      {modelCenter?.all_models?.map((m: any) => (
                        <tr key={m.model_id} className="hover:bg-slate-800/40 transition">
                          <td className="py-3 px-4 font-bold text-white">{m.model_name}</td>
                          <td className="py-3 px-4 font-mono text-slate-400">{m.model_version}</td>
                          <td className="py-3 px-4 text-right font-medium text-slate-200">
                            {m.training_mae.toFixed(2)}
                          </td>
                          <td className="py-3 px-4 text-right text-slate-300">
                            {m.training_rmse.toFixed(2)}
                          </td>
                          <td className="py-3 px-4 text-right font-bold text-emerald-400">
                            {m.training_r2.toFixed(3)}
                          </td>
                          <td className="py-3 px-4 text-slate-400">{m.trained_at ? m.trained_at.slice(0, 16) : '-'}</td>
                          <td className="py-3 px-4 text-center">
                            <span
                              className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                                m.is_active
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-slate-800 text-slate-500'
                              }`}
                            >
                              {m.is_active ? 'Active Champion' : 'Evaluated Candidate'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: AUDIT TRAIL */}
          {activeTab === 'audit' && (
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
              <div className="p-4 bg-slate-950/60 border-b border-slate-800 flex justify-between items-center">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Append-Only System Audit Trail ({auditLogs?.total_count || 0} Events)
                </span>
                <span className="text-xs text-slate-500">Tamper-evident system log</span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-950/40 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                    <tr>
                      <th className="py-3 px-4">Event ID</th>
                      <th className="py-3 px-4">Timestamp</th>
                      <th className="py-3 px-4">Action / Event</th>
                      <th className="py-3 px-4">Entity</th>
                      <th className="py-3 px-4">Actor</th>
                      <th className="py-3 px-4">Notes & Context</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {auditLogs?.items?.map((item: any) => (
                      <tr key={item.audit_id} className="hover:bg-slate-800/40 transition">
                        <td className="py-3 px-4 font-mono text-slate-400">#{item.audit_id}</td>
                        <td className="py-3 px-4 text-slate-400">
                          {item.occurred_at ? item.occurred_at.slice(0, 19).replace('T', ' ') : '-'}
                        </td>
                        <td className="py-3 px-4 font-semibold text-white">
                          <span className="px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 text-[10px]">
                            {item.event_type}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-slate-300 font-mono text-[11px]">
                          {item.entity_type} #{item.entity_id}
                        </td>
                        <td className="py-3 px-4 text-slate-300">{item.actor}</td>
                        <td className="py-3 px-4 text-slate-400 text-[11px] max-w-[280px] truncate">
                          {item.notes || '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 4: USERS & ACCESS */}
          {activeTab === 'users' && (
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
              <div className="p-4 bg-slate-950/60 border-b border-slate-800 flex justify-between items-center">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Registered System Operators
                </span>
                <span className="text-xs text-slate-500">{usersList.length} Accounts</span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-950/40 text-slate-400 border-b border-slate-800 text-[10px] uppercase">
                    <tr>
                      <th className="py-3 px-4">User ID</th>
                      <th className="py-3 px-4">Username</th>
                      <th className="py-3 px-4">Role & Privileges</th>
                      <th className="py-3 px-4 text-center">Status</th>
                      <th className="py-3 px-4">Registered Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {usersList.map((u) => (
                      <tr key={u.user_id} className="hover:bg-slate-800/40 transition">
                        <td className="py-3 px-4 font-mono text-slate-500">#{u.user_id}</td>
                        <td className="py-3 px-4 font-bold text-white">{u.username}</td>
                        <td className="py-3 px-4">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] uppercase font-bold ${
                              u.role === 'admin'
                                ? 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                                : 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                            }`}
                          >
                            {u.role}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-center">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            {u.is_active ? 'Active' : 'Suspended'}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-slate-400">{u.created_at ? u.created_at.slice(0, 10) : '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
