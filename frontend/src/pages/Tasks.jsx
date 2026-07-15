import { useState } from 'react';
import {
  useTasks,
  useCreateTask,
  useUpdateTask,
  useSetTaskActive,
  useDeleteTask,
  useReorderTasks,
} from '../hooks/useTasks';
import { useSessionActions } from '../hooks/useSessions';
import TaskForm from '../components/TaskForm';
import TaskList from '../components/TaskList';
import LogWorkModal from '../components/LogWorkModal';

export default function Tasks() {
  const { data: tasks, isLoading, error } = useTasks();
  const createTask = useCreateTask();
  const updateTask = useUpdateTask();
  const setActive = useSetTaskActive();
  const deleteTask = useDeleteTask();
  const reorderTasks = useReorderTasks();
  const { logManual } = useSessionActions();

  const [editing, setEditing] = useState(null); // task being edited, or 'new'
  const [logging, setLogging] = useState(false);

  if (isLoading) return <p className="text-slate-400">Loading tasks…</p>;
  if (error) return <p className="text-red-400">Failed to load tasks: {error.message}</p>;

  const handleSubmit = (payload) => {
    if (editing && editing.id) {
      updateTask.mutate({ id: editing.id, payload }, { onSuccess: () => setEditing(null) });
    } else {
      createTask.mutate(payload, { onSuccess: () => setEditing(null) });
    }
  };

  const handleMove = (idx, dir) => {
    const next = [...tasks];
    const swapIdx = idx + dir;
    if (swapIdx < 0 || swapIdx >= next.length) return;
    [next[idx], next[swapIdx]] = [next[swapIdx], next[idx]];
    reorderTasks.mutate(
      next.map((t, i) => ({ id: t.id, sortOrder: i, priorityWeight: t.priorityWeight }))
    );
  };

  return (
    <div className="grid gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-100">Tasks</h1>
        <div className="flex gap-2">
          <button
            onClick={() => setLogging(true)}
            className="rounded bg-emerald-600 px-3 py-1 text-sm font-medium text-white hover:bg-emerald-500"
          >
            Log Work
          </button>
          {editing === null && (
            <button
              onClick={() => setEditing({})}
              className="rounded bg-blue-600 px-3 py-1 text-sm font-medium text-white hover:bg-blue-500"
            >
              + Add task
            </button>
          )}
        </div>
      </div>

      {logging && (
        <LogWorkModal
          tasks={tasks}
          isSubmitting={logManual.isPending}
          onCancel={() => setLogging(false)}
          onSubmit={(payload) => logManual.mutate(payload, { onSuccess: () => setLogging(false) })}
        />
      )}

      {editing !== null && (
        <TaskForm
          initial={editing}
          onSubmit={handleSubmit}
          onCancel={() => setEditing(null)}
        />
      )}

      <TaskList
        tasks={tasks}
        onEdit={setEditing}
        onToggleActive={(t) => setActive.mutate({ id: t.id, active: !t.active })}
        onDelete={(t) => {
          if (window.confirm(`Delete task "${t.name}"? This cannot be undone.`)) {
            deleteTask.mutate(t.id);
          }
        }}
        onMove={handleMove}
      />
    </div>
  );
}
