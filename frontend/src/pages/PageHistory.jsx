import { useState } from 'react';
import { usePages, useRespondToPage } from '../hooks/usePages';
import { useTasks } from '../hooks/useTasks';
import PageHistoryTable from '../components/PageHistoryTable';

const STATUSES = ['PENDING', 'ACCEPTED', 'DECLINED', 'MISSED', 'EXPIRED', 'CANCELLED'];

export default function PageHistory() {
  const [status, setStatus] = useState('');
  const [taskId, setTaskId] = useState('');
  const [page, setPage] = useState(0);
  const [declineTarget, setDeclineTarget] = useState(null);
  const [declineReason, setDeclineReason] = useState('');

  const { data: tasks } = useTasks();
  const { data, isLoading, error } = usePages({
    status: status || undefined,
    taskId: taskId || undefined,
    page,
    size: 20,
  });
  const respond = useRespondToPage();

  const submitDecline = () => {
    if (!declineTarget) return;
    respond.mutate(
      { id: declineTarget, status: 'DECLINED', reason: declineReason || 'No reason given' },
      { onSuccess: () => { setDeclineTarget(null); setDeclineReason(''); } }
    );
  };

  return (
    <div className="grid gap-6">
      <h1 className="text-xl font-semibold text-slate-100">Page history</h1>

      <div className="flex flex-wrap gap-3">
        <select
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0); }}
          className="rounded bg-slate-800 px-2 py-1 text-sm text-slate-200"
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        <select
          value={taskId}
          onChange={(e) => { setTaskId(e.target.value); setPage(0); }}
          className="rounded bg-slate-800 px-2 py-1 text-sm text-slate-200"
        >
          <option value="">All tasks</option>
          {tasks?.map((t) => (
            <option key={t.id} value={t.id}>{t.name}</option>
          ))}
        </select>
      </div>

      {isLoading && <p className="text-slate-400">Loading…</p>}
      {error && <p className="text-red-400">Failed to load pages: {error.message}</p>}

      {data && (
        <>
          <PageHistoryTable
            pages={data.content}
            onAccept={(id) => respond.mutate({ id, status: 'ACCEPTED' })}
            onDecline={(id) => setDeclineTarget(id)}
          />
          <div className="flex items-center justify-between text-sm text-slate-400">
            <button
              disabled={data.first}
              onClick={() => setPage((p) => Math.max(p - 1, 0))}
              className="rounded bg-slate-700 px-3 py-1 disabled:opacity-40"
            >
              Previous
            </button>
            <span>Page {data.number + 1} of {Math.max(data.totalPages, 1)}</span>
            <button
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
              className="rounded bg-slate-700 px-3 py-1 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </>
      )}

      {declineTarget && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/60">
          <div className="w-80 rounded-xl border border-slate-700 bg-slate-900 p-4">
            <h3 className="mb-2 text-sm font-semibold text-slate-100">Decline reason</h3>
            <input
              autoFocus
              className="w-full rounded bg-slate-800 px-2 py-1 text-sm text-slate-100"
              placeholder="e.g. Too busy"
              value={declineReason}
              onChange={(e) => setDeclineReason(e.target.value)}
            />
            <div className="mt-3 flex justify-end gap-2">
              <button
                onClick={() => { setDeclineTarget(null); setDeclineReason(''); }}
                className="rounded bg-slate-700 px-3 py-1 text-sm text-slate-200"
              >
                Cancel
              </button>
              <button
                onClick={submitDecline}
                className="rounded bg-red-600 px-3 py-1 text-sm font-medium text-white"
              >
                Decline
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
