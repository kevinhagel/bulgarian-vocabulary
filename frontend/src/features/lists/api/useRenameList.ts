import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { WordListSummaryDTO } from '@/features/lists/types';

export function useRenameList() {
  const queryClient = useQueryClient();
  return useMutation<WordListSummaryDTO, Error, { listId: number; name: string }>({
    mutationFn: async ({ listId, name }) =>
      (await api.put<WordListSummaryDTO>(`/lists/${listId}`, { name })).data,
    onSuccess: (_data, { listId }) => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
      queryClient.invalidateQueries({ queryKey: ['lists', listId] });
    },
  });
}
