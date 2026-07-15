import { apiClient } from './client';

export const tasksApi = {
  list: () => apiClient.get('/tasks').then((r) => r.data),
  get: (id) => apiClient.get(`/tasks/${id}`).then((r) => r.data),
  create: (payload) => apiClient.post('/tasks', payload).then((r) => r.data),
  update: (id, payload) => apiClient.put(`/tasks/${id}`, payload).then((r) => r.data),
  setActive: (id, active) =>
    apiClient.patch(`/tasks/${id}/status`, { active }).then((r) => r.data),
  remove: (id) => apiClient.delete(`/tasks/${id}`).then((r) => r.data),
  reorder: (items) => apiClient.put('/tasks/reorder', { items }).then((r) => r.data),
};
