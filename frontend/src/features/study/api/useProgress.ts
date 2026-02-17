import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { ProgressDashboardDTO } from '@/features/study/types';

export function useProgress() {
  return useQuery<ProgressDashboardDTO>({
    queryKey: ['study', 'progress'],
    queryFn: async () => (await api.get<ProgressDashboardDTO>('/study/progress')).data,
    staleTime: 30_000,
    retry: false,
  });
}
