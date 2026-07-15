import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAnalyticsSummary } from '../hooks/useAnalytics';
import { useTasks } from '../hooks/useTasks';
import { useSessionActions } from '../hooks/useSessions';
import StatCard from '../components/StatCard';
import RateGauge from '../components/RateGauge';
import TaskHoursBarChart from '../components/TaskHoursBarChart';
import DeclineReasonsChart from '../components/DeclineReasonsChart';
import LogWorkModal from '../components/LogWorkModal';
import PagerToggle from '../components/PagerToggle';

export default function Dashboard() {
  const [range, setRange] = useState('today');
  const { data, isLoading, error } = useAnalyticsSummary(range);
  const { data: tasks } = useTasks();
  const { logManual } = useSessionActions();
  const [logging, setLogging] = useState(false);

  if (isLoading) return <p className="text-slate-400">Loading dashboard…</p>;
  if (error) return <p className="text-red-400">Failed to load analytics: {error.message}</p>;

  const rangeLabel = range === 'today' ? 'Today (since 12:00 AM)' : 'Lifetime (all time)';

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-slate-100">Dashboard</h1>
        <div className="flex items-center gap-4">
          <PagerToggle />
          <button
            onClick={() => setLogging(true)}
            className="rounded bg-emerald-600 px-3 py-1 text-sm font-medium text-white hover:bg-emerald-500"
          >
            Log Work
          </button>
          <Link to="/debt" className="text-sm text-blue-400 hover:underline">
            View debt dashboard →
          </Link>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <span className="text-sm text-slate-400">Showing:</span>
        <div className="inline-flex rounded-lg border border-slate-800 bg-slate-900/60 p-1">
          {['today', 'lifetime'].map((r) => (
            <button
              key={r}
              onClick={() => setRange(r)}
              className={`rounded-md px-3 py-1 text-sm font-medium transition ${
                range === r ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {r === 'today' ? 'Today' : 'Lifetime'}
            </button>
          ))}
        </div>
        <span className="text-xs text-slate-500">{rangeLabel}</span>
      </div>

      {logging && (
        <LogWorkModal
          tasks={tasks}
          isSubmitting={logManual.isPending}
          onCancel={() => setLogging(false)}
          onSubmit={(payload) => logManual.mutate(payload, { onSuccess: () => setLogging(false) })}
        />
      )}

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard label="Pages received" value={data.pagesReceived} accent="blue" />
        <StatCard label="Accepted" value={data.pagesAccepted} accent="green" />
        <StatCard label="Declined" value={data.pagesDeclined} accent="red" />
        <StatCard label="Missed" value={data.pagesMissed} accent="amber" />
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-2">
        <StatCard
          label="Accepted & completed"
          value={data.acceptedCompletedPages}
          sub="Accepted pages whose session was confirmed complete"
          accent="green"
        />
        <StatCard
          label="Accepted & abandoned"
          value={data.acceptedAbandonedPages}
          sub="Accepted pages whose session was later abandoned"
          accent="red"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <RateGauge label="Acceptance rate" ratio={data.acceptanceRate} />
        <RateGauge label="Completion rate" ratio={data.completionRate} />
        <StatCard
          label="Productivity debt"
          value={`${data.productivityDebtMinutes} min`}
          sub={range === 'today' ? "Today's shortfall only" : 'Across all active tasks'}
          accent="purple"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label="From accepted pages" value={`${data.minutesFromPages} min`} accent="blue" />
        <StatCard label="From manually logged work" value={`${data.minutesFromManual} min`} accent="green" />
        <StatCard
          label="Total productive minutes"
          value={`${data.totalProductiveMinutes} min`}
          sub={range === 'today' ? 'Today, all sources combined' : 'Lifetime, all sources combined'}
          accent="purple"
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <section className="rounded-xl border border-slate-800 bg-slate-900/40 p-4">
          <h2 className="mb-2 text-sm font-semibold text-slate-200">Today's hours by task</h2>
          <TaskHoursBarChart rows={data.todayHoursByTask} />
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/40 p-4">
          <h2 className="mb-2 text-sm font-semibold text-slate-200">Weekly hours by task</h2>
          <TaskHoursBarChart rows={data.weekHoursByTask} />
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/40 p-4">
          <h2 className="mb-2 text-sm font-semibold text-slate-200">Lifetime hours by task</h2>
          <TaskHoursBarChart rows={data.lifetimeHoursByTask} />
        </section>
      </div>

      <section className="rounded-xl border border-slate-800 bg-slate-900/40 p-4">
        <h2 className="mb-2 text-sm font-semibold text-slate-200">Most common decline reasons</h2>
        <DeclineReasonsChart rows={data.topDeclineReasons} />
      </section>
    </div>
  );
}
