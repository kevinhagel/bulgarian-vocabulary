import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export function useRemoveFromList() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, { listId: number; lemmaId: number }>({
    mutationFn: async ({ listId, lemmaId }) => {
      await api.delete(`/lists/${listId}/members/${lemmaId}`);
    },
    onSuccess: (_data, { listId }) => {
      queryClient.invalidateQueries({ queryKey: ['lists', listId] });
      queryClient.invalidateQueries({ queryKey: ['lists'] });
    },
  });
}
