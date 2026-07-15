import { useState } from 'react';
import { usePagerStatus, usePagerActions } from '../hooks/usePager';

/** Global "disable pages" toggle — for days the user can't take pages at all.
 * While paused, the scheduler sends nothing and debt stops accruing (see backend
 * PagerPauseService / DebtCalculatorService.shortfallForDate). */
export default function PagerToggle() {
  const { data, isLoading } = usePagerStatus();
  const { pause, resume } = usePagerActions();
  const [reason, setReason] = useState('');
  const [asking, setAsking] = useState(false);

  if (isLoading || !data) return null;

  if (data.paused) {
    return (
      <div className="flex items-center gap-3 rounded-lg border border-amber-700/50 bg-amber-900/20 px-3 py-1.5 text-sm">
        <span className="text-amber-300">
          ⏸ Paging disabled{data.reason ? ` — ${data.reason}` : ''} (debt frozen)
        </span>
        <button
          onClick={() => resume.mutate()}
          disabled={resume.isPending}
          className="rounded bg-emerald-600 px-3 py-1 text-xs font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
        >
          Resume paging
        </button>
      </div>
    );
  }

  if (asking) {
    return (
      <form
        className="flex items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          pause.mutate(reason || undefined, { onSuccess: () => { setAsking(false); setReason(''); } });
        }}
      >
        <input
          autoFocus
          placeholder="Reason (optional)"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          className="rounded bg-slate-800 px-2 py-1 text-sm text-slate-100"
        />
        <button type="submit" disabled={pause.isPending} className="rounded bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-500 disabled:opacity-50">
          Confirm disable
        </button>
        <button type="button" onClick={() => setAsking(false)} className="text-xs text-slate-400 hover:underline">
          Cancel
        </button>
      </form>
    );
  }

  return (
    <button
      onClick={() => setAsking(true)}
      className="rounded bg-slate-800 px-3 py-1 text-sm font-medium text-slate-300 hover:bg-slate-700"
    >
      Disable pages
    </button>
  );
}
