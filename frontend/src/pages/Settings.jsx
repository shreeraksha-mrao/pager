import { useState, useEffect } from 'react';
import { useSettings, useUpdateSettings } from '../hooks/useSettings';

const FIELDS = [
  ['workingHoursStart', 'Working hours start', 'time'],
  ['workingHoursEnd', 'Working hours end', 'time'],
  ['nightLowWindowStart', 'Night-low window start', 'time'],
  ['nightLowWindowEnd', 'Night-low window end', 'time'],
  ['nightLowProbability', 'Night-low probability (0-1)', 'number'],
  ['nightMediumWindowStart', 'Night-medium window start', 'time'],
  ['nightMediumWindowEnd', 'Night-medium window end', 'time'],
  ['nightMediumProbability', 'Night-medium probability (0-1)', 'number'],
  ['baseTickProbability', 'Base tick probability (0-1)', 'number'],
  ['maxTickProbability', 'Max tick probability (0-1)', 'number'],
  ['minGapBetweenPagesMinutes', 'Min gap between pages (min)', 'number'],
  ['defaultPageDurationMinutes', 'Default page duration (min)', 'number'],
  ['missedPageTimeoutMinutes', 'Missed-page timeout (min)', 'number'],
  ['declineCooldownMinMinutes', 'Decline cooldown min (min)', 'number'],
  ['declineCooldownMaxMinutes', 'Decline cooldown max (min)', 'number'],
  ['confirmationReminderIntervalMinutes', 'Confirmation reminder interval (min)', 'number'],
  ['confirmationReminderMaxCount', 'Max confirmation reminders', 'number'],
  ['dailyMinimumTargetMinutes', 'Daily minimum target (min)', 'number'],
];

export default function Settings() {
  const { data, isLoading, error } = useSettings();
  const updateSettings = useUpdateSettings();
  const [form, setForm] = useState(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (data) {
      const normalized = { ...data };
      FIELDS.forEach(([key, , type]) => {
        if (type === 'time' && normalized[key]?.length >= 5) normalized[key] = normalized[key].slice(0, 5);
      });
      setForm(normalized);
    }
  }, [data]);

  if (isLoading || !form) return <p className="text-slate-400">Loading settings…</p>;
  if (error) return <p className="text-red-400">Failed to load settings: {error.message}</p>;

  const set = (key) => (e) => {
    const raw = e.target.value;
    setForm((f) => ({ ...f, [key]: raw }));
  };

  const submit = (e) => {
    e.preventDefault();
    const payload = { ...form };
    FIELDS.forEach(([key, , type]) => {
      if (type === 'number') payload[key] = Number(payload[key]);
      if (type === 'time' && payload[key] && payload[key].length === 5) payload[key] = `${payload[key]}:00`;
    });
    updateSettings.mutate(payload, { onSuccess: () => { setSaved(true); setTimeout(() => setSaved(false), 2000); } });
  };

  return (
    <div className="grid gap-6">
      <h1 className="text-xl font-semibold text-slate-100">Settings</h1>

      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 text-sm text-slate-300">
        Telegram registration:{' '}
        {form.telegramRegistered ? (
          <span className="text-emerald-400">✅ Registered</span>
        ) : (
          <span className="text-amber-400">Not registered — send /start to your bot</span>
        )}
      </div>

      <form onSubmit={submit} className="grid gap-3 rounded-xl border border-slate-800 bg-slate-900/60 p-4 sm:grid-cols-2">
        {FIELDS.map(([key, label, type]) => (
          <label key={key} className="text-sm text-slate-300">
            {label}
            <input
              type={type}
              step={type === 'number' ? 'any' : undefined}
              className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-slate-100"
              value={form[key] ?? ''}
              onChange={set(key)}
            />
          </label>
        ))}
        <div className="col-span-full flex items-center gap-3 pt-2">
          <button type="submit" className="rounded bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-500">
            Save settings
          </button>
          {saved && <span className="text-sm text-emerald-400">Saved!</span>}
        </div>
      </form>
      <p className="text-xs text-slate-500">
        Note: settings changes currently apply in-memory to the running backend and reset to the
        defaults in application.yml on restart.
      </p>
    </div>
  );
}
