import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { WordListSummaryDTO } from '@/features/lists/types';

export function useCreateList() {
  const queryClient = useQueryClient();
  return useMutation<WordListSummaryDTO, Error, { name: string }>({
    mutationFn: async ({ name }) =>
      (await api.post<WordListSummaryDTO>('/lists', { name })).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['lists'] }),
  });
}
