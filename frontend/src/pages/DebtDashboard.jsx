import { useDebtDashboard } from '../hooks/useAnalytics';
import StatCard from '../components/StatCard';

export default function DebtDashboard() {
  const { data, isLoading, error } = useDebtDashboard();

  if (isLoading) return <p className="text-slate-400">Loading debt dashboard…</p>;
  if (error) return <p className="text-red-400">Failed to load debt data: {error.message}</p>;

  return (
    <div className="grid gap-6">
      <h1 className="text-xl font-semibold text-slate-100">Debt dashboard</h1>

      <StatCard
        label="Total productivity debt"
        value={`${data.totalDebtMinutes} min`}
        sub="Sum of persistent debt across all active tasks"
        accent="purple"
      />

      <div className="grid gap-4 sm:grid-cols-3">
        {data.tasks.map((t) => (
          <div key={t.taskId} className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
            <div className="flex items-center gap-2">
              <span>{t.icon}</span>
              <p className="font-medium text-slate-100">{t.taskName}</p>
            </div>
            <p className="mt-2 text-2xl font-semibold" style={{ color: t.color }}>
              {t.effectiveDebtMinutes} min
            </p>
            <p className="text-xs text-slate-500">{t.daysBehindTarget} day(s) behind target</p>
          </div>
        ))}
      </div>
    </div>
  );
}
