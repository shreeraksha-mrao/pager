import { useState } from 'react';

const DEFAULTS = { taskId: '', durationMinutes: 30, notes: '' };

/** Modal form for manually logging completed work not tied to any page (POST /api/sessions/manual). */
export default function LogWorkModal({ tasks, onSubmit, onCancel, isSubmitting }) {
  const [form, setForm] = useState({ ...DEFAULTS, taskId: tasks?.[0]?.id ?? '' });
  const [error, setError] = useState(null);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = (e) => {
    e.preventDefault();
    if (!form.taskId) {
      setError('Please select a task.');
      return;
    }
    const minutes = Number(form.durationMinutes);
    if (!minutes || minutes <= 0) {
      setError('Duration must be a positive number of minutes.');
      return;
    }
    setError(null);
    onSubmit({
      taskId: Number(form.taskId),
      durationMinutes: minutes,
      notes: form.notes || undefined,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <form
        onSubmit={submit}
        className="grid w-full max-w-md gap-3 rounded-xl border border-slate-800 bg-slate-900 p-5 shadow-xl"
      >
        <h2 className="text-lg font-semibold text-slate-100">Log completed work</h2>
        <p className="text-xs text-slate-400">
          Use this if you finished a task on your own — e.g. after declining a page — so debt reflects the
          work you actually did.
        </p>

        <label className="text-sm text-slate-300">
          Task
          <select
            className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
            value={form.taskId}
            onChange={set('taskId')}
          >
            {(tasks || []).map((t) => (
              <option key={t.id} value={t.id}>
                {t.icon} {t.name}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm text-slate-300">
          Duration (minutes)
          <input
            type="number"
            min="1"
            className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
            value={form.durationMinutes}
            onChange={set('durationMinutes')}
          />
        </label>

        <label className="text-sm text-slate-300">
          Notes (optional)
          <textarea
            rows={2}
            className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
            value={form.notes}
            onChange={set('notes')}
          />
        </label>

        {error && <p className="text-sm text-red-400">{error}</p>}

        <div className="mt-1 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="rounded bg-slate-700 px-3 py-1 text-sm text-slate-200 hover:bg-slate-600"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded bg-emerald-600 px-3 py-1 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
          >
            {isSubmitting ? 'Logging…' : 'Log work'}
          </button>
        </div>
      </form>
    </div>
  );
}
