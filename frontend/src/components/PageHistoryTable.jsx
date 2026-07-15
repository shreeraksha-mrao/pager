import StatusBadge from './StatusBadge';

function fmt(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function PageHistoryTable({ pages, onAccept, onDecline }) {
  if (!pages?.length) {
    return <p className="text-sm text-slate-500">No pages yet.</p>;
  }
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-800">
      <table className="w-full text-left text-sm">
        <thead className="bg-slate-900/80 text-xs uppercase tracking-wide text-slate-400">
          <tr>
            <th className="px-3 py-2">Task</th>
            <th className="px-3 py-2">Sent</th>
            <th className="px-3 py-2">Duration</th>
            <th className="px-3 py-2">Status</th>
            <th className="px-3 py-2">Decline reason</th>
            {(onAccept || onDecline) && <th className="px-3 py-2">Actions</th>}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800">
          {pages.map((p) => (
            <tr key={p.id} className="text-slate-300">
              <td className="px-3 py-2">
                <span className="mr-1">{p.taskIcon}</span>
                {p.taskName}
              </td>
              <td className="px-3 py-2">{fmt(p.sentAt)}</td>
              <td className="px-3 py-2">{p.durationMinutes}m</td>
              <td className="px-3 py-2"><StatusBadge status={p.status} /></td>
              <td className="px-3 py-2 text-slate-400">{p.declineReason || '—'}</td>
              {(onAccept || onDecline) && (
                <td className="px-3 py-2">
                  {p.status === 'PENDING' && (
                    <div className="flex gap-2">
                      {onAccept && (
                        <button
                          onClick={() => onAccept(p.id)}
                          className="rounded bg-emerald-600 px-2 py-1 text-xs font-medium text-white hover:bg-emerald-500"
                        >
                          Accept
                        </button>
                      )}
                      {onDecline && (
                        <button
                          onClick={() => onDecline(p.id)}
                          className="rounded bg-red-600 px-2 py-1 text-xs font-medium text-white hover:bg-red-500"
                        >
                          Decline
                        </button>
                      )}
                    </div>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
