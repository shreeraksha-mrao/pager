import { apiClient } from './client';

export const settingsApi = {
  get: () => apiClient.get('/settings').then((r) => r.data),
  update: (payload) => apiClient.put('/settings', payload).then((r) => r.data),
};
