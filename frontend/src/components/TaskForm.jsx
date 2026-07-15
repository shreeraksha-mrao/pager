import { useState } from 'react';

const DEFAULTS = {
  name: '',
  description: '',
  dailyTargetMinutes: 60,
  priorityWeight: 1.0,
  color: '#3B82F6',
  icon: '🎯',
  active: true,
  sortOrder: 0,
};

export default function TaskForm({ initial, onSubmit, onCancel }) {
  const [form, setForm] = useState({ ...DEFAULTS, ...initial });

  const set = (key) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setForm((f) => ({ ...f, [key]: value }));
  };

  const submit = (e) => {
    e.preventDefault();
    onSubmit({
      ...form,
      dailyTargetMinutes: Number(form.dailyTargetMinutes),
      priorityWeight: Number(form.priorityWeight),
      sortOrder: Number(form.sortOrder),
    });
  };

  return (
    <form onSubmit={submit} className="grid gap-3 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
      <div className="grid grid-cols-2 gap-3">
        <label className="text-sm text-slate-300">
          Name
          <input
            className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
            value={form.name}
            onChange={set('name')}
            required
          />
        </label>
        <label className="text-sm text-slate-300">
          Icon
          <input
            className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
            value={form.icon}
            onChange={set('icon')}
          />
        </label>
      </div>
      <label className="text-sm text-slate-300">
        Description
        <input
          className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
          value={form.description || ''}
          onChange={set('description')}
        />
      </label>
      <div className="grid grid-cols-3 gap-3">
        <label className="text-sm text-slate-300">
          Daily target (min)
          <input
            type="number"
            min="1"
            className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
            value={form.dailyTargetMinutes}
            onChange={set('dailyTargetMinutes')}
          />
        </label>
        <label className="text-sm text-slate-300">
          Priority weight
          <input
            type="number"
            step="0.1"
            className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
            value={form.priorityWeight}
            onChange={set('priorityWeight')}
          />
        </label>
        <label className="text-sm text-slate-300">
          Color
          <input
            type="color"
            className="mt-1 h-8 w-full rounded bg-slate-800"
            value={form.color}
            onChange={set('color')}
          />
        </label>
      </div>
      <div className="flex items-center justify-between">
        <label className="flex items-center gap-2 text-sm text-slate-300">
          <input type="checkbox" checked={form.active} onChange={set('active')} />
          Active
        </label>
        <div className="flex gap-2">
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="rounded bg-slate-700 px-3 py-1 text-sm text-slate-200 hover:bg-slate-600"
            >
              Cancel
            </button>
          )}
          <button type="submit" className="rounded bg-blue-600 px-3 py-1 text-sm font-medium text-white hover:bg-blue-500">
            Save
          </button>
        </div>
      </div>
    </form>
  );
}
