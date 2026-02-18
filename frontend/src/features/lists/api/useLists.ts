import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { WordListSummaryDTO } from '@/features/lists/types';

export function useLists() {
  return useQuery<WordListSummaryDTO[]>({
    queryKey: ['lists'],
    queryFn: async () => (await api.get<WordListSummaryDTO[]>('/lists')).data,
    staleTime: 30_000,
  });
}
