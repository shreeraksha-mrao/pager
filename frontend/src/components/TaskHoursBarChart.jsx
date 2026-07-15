import { BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';

/** rows: [{ taskName, minutes, color }]. Values are converted to hours for display. */
export default function TaskHoursBarChart({ rows }) {
  if (!rows?.length) {
    return <p className="text-sm text-slate-500">No data yet.</p>;
  }
  const data = rows.map((row) => ({ ...row, hours: Math.round((row.minutes / 60) * 100) / 100 }));
  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
        <XAxis dataKey="taskName" stroke="#64748b" fontSize={12} />
        <YAxis stroke="#64748b" fontSize={12} label={{ value: 'hours', angle: -90, position: 'insideLeft', fill: '#64748b', fontSize: 12 }} />
        <Tooltip
          formatter={(value) => [`${value} hr`, 'Time']}
          contentStyle={{ background: '#0f172a', border: '1px solid #334155' }}
        />
        <Bar dataKey="hours" radius={[4, 4, 0, 0]}>
          {data.map((row) => (
            <Cell key={row.taskName} fill={row.color || '#3b82f6'} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

