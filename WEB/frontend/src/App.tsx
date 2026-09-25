import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Sidebar, NavTab } from './components/Sidebar';
import { Navbar } from './components/Navbar';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Inventory } from './pages/Inventory';
import { Products } from './pages/Products';
import { Sales } from './pages/Sales';
import { Purchases } from './pages/Purchases';
import { Forecasting } from './pages/Forecasting';
import { SmartDecisions } from './pages/SmartDecisions';
import { Reports } from './pages/Reports';
import { Admin } from './pages/Admin';

const AuthenticatedApp: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');
  const [selectedStoreId, setSelectedStoreId] = useState<number | null>(null);

  if (isLoading) {
    return (
      <div className="h-screen w-screen bg-slate-950 flex flex-col items-center justify-center space-y-4">
        <div className="w-16 h-16 rounded-full border-2 border-blue-500/40 p-1 bg-slate-900 shadow-xl flex items-center justify-center animate-pulse">
          <img src="/logo.png" alt="STOCKSENSE" className="w-full h-full object-cover rounded-full" />
        </div>
        <div className="flex items-center space-x-2 text-slate-400 text-xs">
          <span className="w-2 h-2 rounded-full bg-blue-500 animate-ping" />
          <span>Initializing STOCKSENSE Intelligence Engine...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Login />;
  }

  const renderActiveView = () => {
    switch (activeTab) {
      case 'dashboard':
        return <Dashboard />;
      case 'inventory':
        return <Inventory />;
      case 'products':
        return <Products />;
      case 'sales':
        return <Sales />;
      case 'purchases':
        return <Purchases />;
      case 'forecasting':
        return <Forecasting />;
      case 'smart_decisions':
        return <SmartDecisions />;
      case 'reports':
        return <Reports />;
      case 'admin':
        return <Admin />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex">
      {/* Official STOCKSENSE Navigation Sidebar */}
      <Sidebar activeTab={activeTab} onTabChange={setActiveTab} />

      {/* Main Content Area */}
      <div className="flex-1 ml-64 flex flex-col min-h-screen min-w-0">
        <Navbar selectedStoreId={selectedStoreId} onStoreChange={setSelectedStoreId} />
        <main className="flex-1 p-8 max-w-7xl w-full mx-auto">{renderActiveView()}</main>
      </div>
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <AuthenticatedApp />
    </AuthProvider>
  );
}
