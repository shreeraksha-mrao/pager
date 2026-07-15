import { apiClient } from './client';

export const pagerApi = {
  status: () => apiClient.get('/pager/status').then((r) => r.data),
  pause: (reason) => apiClient.post('/pager/pause', { reason }).then((r) => r.data),
  resume: () => apiClient.post('/pager/resume').then((r) => r.data),
};
