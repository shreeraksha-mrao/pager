import { apiClient } from './client';

export const pagesApi = {
  list: ({ status, taskId, page = 0, size = 20 } = {}) =>
    apiClient
      .get('/pages', { params: { status, taskId, page, size } })
      .then((r) => r.data),
  get: (id) => apiClient.get(`/pages/${id}`).then((r) => r.data),
  respond: (id, status, reason) =>
    apiClient.post(`/pages/${id}/respond`, { status, reason }).then((r) => r.data),
};
