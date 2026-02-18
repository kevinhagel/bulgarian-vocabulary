import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { WordListDetailDTO } from '@/features/lists/types';

export function useListDetail(listId: number | null) {
  return useQuery<WordListDetailDTO>({
    queryKey: ['lists', listId],
    queryFn: async () => (await api.get<WordListDetailDTO>(`/lists/${listId}`)).data,
    enabled: listId !== null,
    staleTime: 30_000,
  });
}
