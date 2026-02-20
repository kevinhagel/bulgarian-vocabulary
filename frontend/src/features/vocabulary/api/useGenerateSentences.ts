import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { LemmaDetailDTO } from '@/features/vocabulary/types';

/**
 * Triggers on-demand sentence generation for a vocabulary entry.
 * POST /api/vocabulary/{id}/sentences/generate
 * Returns 202 Accepted with sentenceStatus=QUEUED.
 * Poll GET /api/vocabulary/{id} for completion.
 */
export function useGenerateSentences() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (lemmaId: number): Promise<LemmaDetailDTO> => {
      const response = await api.post<LemmaDetailDTO>(`/vocabulary/${lemmaId}/sentences/generate`);
      return response.data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(['vocabulary', data.id], data);
    },
  });
}
