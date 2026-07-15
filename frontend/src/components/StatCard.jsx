export default function StatCard({ label, value, sub, accent = 'blue' }) {
  const accents = {
    blue: 'border-blue-500/40 text-blue-400',
    green: 'border-emerald-500/40 text-emerald-400',
    red: 'border-red-500/40 text-red-400',
    amber: 'border-amber-500/40 text-amber-400',
    purple: 'border-violet-500/40 text-violet-400',
  };
  return (
    <div className={`rounded-xl border bg-slate-900/60 p-4 shadow-sm ${accents[accent] ?? accents.blue}`}>
      <p className="text-xs uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-slate-100">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </div>
  );
}
