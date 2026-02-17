import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { DueCountDTO } from '@/features/study/types';

export function useDueCount() {
  return useQuery<DueCountDTO>({
    queryKey: ['study', 'due-count'],
    queryFn: async () => {
      const response = await api.get<DueCountDTO>('/study/due-count');
      return response.data;
    },
    staleTime: 60_000,
    retry: false,
  });
}
