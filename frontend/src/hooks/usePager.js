import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { pagerApi } from '../api/pager';

const KEY = ['pager-status'];

export function usePagerStatus() {
  return useQuery({ queryKey: KEY, queryFn: pagerApi.status, refetchInterval: 30_000 });
}

export function usePagerActions() {
  const qc = useQueryClient();
  const invalidate = () => qc.invalidateQueries({ queryKey: KEY });
  return {
    pause: useMutation({ mutationFn: pagerApi.pause, onSuccess: invalidate }),
    resume: useMutation({ mutationFn: pagerApi.resume, onSuccess: invalidate }),
  };
}
