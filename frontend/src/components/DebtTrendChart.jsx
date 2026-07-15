import { LineChart, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from 'recharts';

const COLORS = ['#3b82f6', '#10b981', '#8b5cf6', '#f59e0b', '#ef4444', '#ec4899'];

/** tasks: [{ taskName, trend: [{ date, cumulativeDebtMinutes }] }] */
export default function DebtTrendChart({ tasks }) {
  const withTrend = (tasks || []).filter((t) => t.trend?.length);
  if (!withTrend.length) {
    return <p className="text-sm text-slate-500">No debt history yet — check back after the first nightly rollover.</p>;
  }
  const dateSet = new Set();
  withTrend.forEach((t) => t.trend.forEach((p) => dateSet.add(p.date)));
  const dates = Array.from(dateSet).sort();
  const data = dates.map((date) => {
    const row = { date: date.slice(5) };
    withTrend.forEach((t) => {
      const point = t.trend.find((p) => p.date === date);
      row[t.taskName] = point ? point.cumulativeDebtMinutes : null;
    });
    return row;
  });

  return (
    <ResponsiveContainer width="100%" height={260}>
      <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
        <XAxis dataKey="date" stroke="#64748b" fontSize={12} />
        <YAxis stroke="#64748b" fontSize={12} />
        <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid #334155' }} />
        <Legend wrapperStyle={{ fontSize: 12 }} />
        {withTrend.map((t, i) => (
          <Line
            key={t.taskName}
            type="monotone"
            dataKey={t.taskName}
            stroke={t.color || COLORS[i % COLORS.length]}
            strokeWidth={2}
            dot={false}
            connectNulls
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );
}
