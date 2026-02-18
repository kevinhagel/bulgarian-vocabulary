import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { WordListDetailDTO } from '@/features/lists/types';

export function useListDetail(listId: number | null) {
  return useQuery<WordListDetailDTO>({
    queryKey: ['lists', listId],
    queryFn: async () => (await api.get<WordListDetailDTO>(`/lists/${listId}`)).data,
    enabled: listId !== null,
    staleTime: 30_000,
    refetchInterval: (query) => {
      const data = query.state.data;
      const hasProcessing = data?.lemmas.some((l) => !l.translation);
      return hasProcessing ? 5_000 : false;
    },
  });
}
