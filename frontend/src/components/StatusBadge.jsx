const STATUS_STYLES = {
  PENDING: 'bg-slate-700 text-slate-200',
  ACCEPTED: 'bg-blue-600/30 text-blue-300',
  DECLINED: 'bg-red-600/30 text-red-300',
  MISSED: 'bg-amber-600/30 text-amber-300',
  EXPIRED: 'bg-slate-600/30 text-slate-300',
  CANCELLED: 'bg-slate-600/30 text-slate-400',
  IN_PROGRESS: 'bg-blue-600/30 text-blue-300',
  AWAITING_CONFIRMATION: 'bg-amber-600/30 text-amber-300',
  COMPLETED: 'bg-emerald-600/30 text-emerald-300',
  ABANDONED: 'bg-red-600/30 text-red-300',
};

export default function StatusBadge({ status }) {
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
        STATUS_STYLES[status] || 'bg-slate-700 text-slate-200'
      }`}
    >
      {status}
    </span>
  );
}
