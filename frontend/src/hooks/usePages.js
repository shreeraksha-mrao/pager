import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { pagesApi } from '../api/pages';

const REFRESH_MS = 30_000;

export function usePages(filters) {
  return useQuery({
    queryKey: ['pages', filters],
    queryFn: () => pagesApi.list(filters),
    refetchInterval: REFRESH_MS,
  });
}

export function useRespondToPage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status, reason }) => pagesApi.respond(id, status, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pages'] });
      qc.invalidateQueries({ queryKey: ['analytics'] });
    },
  });
}
