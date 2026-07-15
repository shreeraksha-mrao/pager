import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sessionsApi } from '../api/sessions';

const REFRESH_MS = 30_000;

export function useSessions(filters) {
  return useQuery({
    queryKey: ['sessions', filters],
    queryFn: () => sessionsApi.list(filters),
    refetchInterval: REFRESH_MS,
  });
}

export function useSessionActions() {
  const qc = useQueryClient();
  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['sessions'] });
    qc.invalidateQueries({ queryKey: ['analytics'] });
  };
  const complete = useMutation({ mutationFn: sessionsApi.complete, onSuccess: invalidate });
  const extend = useMutation({
    mutationFn: ({ id, minutes }) => sessionsApi.extend(id, minutes),
    onSuccess: invalidate,
  });
  const abandon = useMutation({ mutationFn: sessionsApi.abandon, onSuccess: invalidate });
  const logManual = useMutation({ mutationFn: sessionsApi.logManual, onSuccess: invalidate });
  return { complete, extend, abandon, logManual };
}
