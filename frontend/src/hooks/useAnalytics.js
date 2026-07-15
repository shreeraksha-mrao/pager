import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '../api/analytics';

const REFRESH_MS = 30_000;

export function useAnalyticsSummary(range = 'today') {
  return useQuery({
    queryKey: ['analytics', 'summary', range],
    queryFn: () => analyticsApi.summary(range),
    refetchInterval: REFRESH_MS,
  });
}

export function useDebtDashboard() {
  return useQuery({
    queryKey: ['analytics', 'debt'],
    queryFn: analyticsApi.debt,
    refetchInterval: REFRESH_MS,
  });
}

