import { apiClient } from './client';

export const sessionsApi = {
  list: ({ taskId, page = 0, size = 20 } = {}) =>
    apiClient.get('/sessions', { params: { taskId, page, size } }).then((r) => r.data),
  get: (id) => apiClient.get(`/sessions/${id}`).then((r) => r.data),
  complete: (id) => apiClient.post(`/sessions/${id}/complete`).then((r) => r.data),
  extend: (id, minutes = 30) =>
    apiClient.post(`/sessions/${id}/extend`, null, { params: { minutes } }).then((r) => r.data),
  abandon: (id) => apiClient.post(`/sessions/${id}/abandon`).then((r) => r.data),
  logManual: ({ taskId, durationMinutes, notes, sessionDate }) =>
    apiClient
      .post('/sessions/manual', { taskId, durationMinutes, notes, sessionDate })
      .then((r) => r.data),
};
