import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export function useDeleteList() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, { listId: number }>({
    mutationFn: async ({ listId }) => { await api.delete(`/lists/${listId}`); },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['lists'] }),
  });
}
