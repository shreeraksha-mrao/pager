export default function TaskList({ tasks, onEdit, onToggleActive, onDelete, onMove }) {
  if (!tasks?.length) {
    return <p className="text-sm text-slate-500">No tasks configured yet.</p>;
  }
  return (
    <div className="grid gap-2">
      {tasks.map((t, idx) => (
        <div
          key={t.id}
          className="flex items-center justify-between rounded-xl border border-slate-800 bg-slate-900/60 p-3"
        >
          <div className="flex items-center gap-3">
            <span
              className="inline-block h-8 w-8 rounded-full text-center leading-8"
              style={{ backgroundColor: `${t.color}33` }}
            >
              {t.icon}
            </span>
            <div>
              <p className="font-medium text-slate-100">
                {t.name} {!t.active && <span className="ml-1 text-xs text-slate-500">(inactive)</span>}
              </p>
              <p className="text-xs text-slate-500">
                {t.dailyTargetMinutes} min/day · weight {t.priorityWeight}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {onMove && (
              <>
                <button
                  onClick={() => onMove(idx, -1)}
                  className="rounded bg-slate-700 px-2 py-1 text-xs text-slate-200 hover:bg-slate-600"
                >
                  ↑
                </button>
                <button
                  onClick={() => onMove(idx, 1)}
                  className="rounded bg-slate-700 px-2 py-1 text-xs text-slate-200 hover:bg-slate-600"
                >
                  ↓
                </button>
              </>
            )}
            <button
              onClick={() => onToggleActive(t)}
              className="rounded bg-slate-700 px-2 py-1 text-xs text-slate-200 hover:bg-slate-600"
            >
              {t.active ? 'Disable' : 'Enable'}
            </button>
            <button
              onClick={() => onEdit(t)}
              className="rounded bg-blue-600 px-2 py-1 text-xs font-medium text-white hover:bg-blue-500"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(t)}
              className="rounded bg-red-600 px-2 py-1 text-xs font-medium text-white hover:bg-red-500"
            >
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
