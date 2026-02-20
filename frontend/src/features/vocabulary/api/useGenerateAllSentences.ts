import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

/**
 * Queues sentence generation for all vocabulary entries that don't have sentences yet.
 * POST /api/vocabulary/sentences/generate-all
 * Returns { queued: number }.
 */
export function useGenerateAllSentences() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (): Promise<{ queued: number }> => {
      const response = await api.post<{ queued: number }>('/vocabulary/sentences/generate-all');
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
    },
  });
}
