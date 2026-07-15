import { NavLink, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import DebtDashboard from './pages/DebtDashboard';
import Tasks from './pages/Tasks';
import PageHistory from './pages/PageHistory';
import Settings from './pages/Settings';

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard' },
  { to: '/debt', label: 'Debt' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/history', label: 'Page history' },
  { to: '/settings', label: 'Settings' },
];

export default function App() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/60">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <h1 className="text-lg font-bold">📟 Productivity Pager</h1>
          <nav className="flex gap-4 text-sm">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `rounded px-2 py-1 ${isActive ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white'}`
                }
                end={item.to === '/'}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/debt" element={<DebtDashboard />} />
          <Route path="/tasks" element={<Tasks />} />
          <Route path="/history" element={<PageHistory />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </main>
    </div>
  );
}
