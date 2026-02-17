import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { LemmaStatsDTO } from '@/features/study/types';

export function useLemmaStats(lemmaId: number | null) {
  return useQuery<LemmaStatsDTO>({
    queryKey: ['study', 'stats', lemmaId],
    queryFn: async () => (await api.get<LemmaStatsDTO>(`/study/stats/${lemmaId}`)).data,
    enabled: lemmaId !== null,
    staleTime: 30_000,
    retry: false,
  });
}
