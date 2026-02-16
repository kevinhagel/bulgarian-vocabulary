import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

/**
 * TanStack Query mutation for deleting vocabulary entries.
 * DELETE /api/vocabulary/{id}.
 * Invalidates vocabulary list queries on success.
 */
export function useDeleteVocabulary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      await api.delete(`/vocabulary/${id}`);
    },
    onSuccess: () => {
      // Invalidate vocabulary list queries
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
    },
  });
}
