import { apiClient } from './client';

export const analyticsApi = {
  summary: (range = 'today') =>
    apiClient.get('/analytics/summary', { params: { range } }).then((r) => r.data),
  debt: () => apiClient.get('/analytics/debt').then((r) => r.data),
  hours: (range = 'today') =>
    apiClient.get('/analytics/hours', { params: { range } }).then((r) => r.data),
  pagesStats: () => apiClient.get('/analytics/pages-stats').then((r) => r.data),
  declineReasons: () => apiClient.get('/analytics/decline-reasons').then((r) => r.data),
};

