import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { LemmaDetailDTO } from '@/features/vocabulary/types';

/**
 * Mutation to trigger LLM reprocessing of a vocabulary entry.
 * Clears existing inflections, resets status to QUEUED, and re-runs the LLM pipeline.
 * An optional hint helps the LLM disambiguate homographs.
 */
export function useReprocessVocabulary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, hint }: { id: number; hint?: string }): Promise<LemmaDetailDTO> => {
      const response = await api.post<LemmaDetailDTO>(`/vocabulary/${id}/reprocess`, { hint });
      return response.data;
    },
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['vocabulary', id] });
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
      queryClient.invalidateQueries({ queryKey: ['review-queue'] });
      queryClient.invalidateQueries({ queryKey: ['study', 'due-count'] });
    },
  });
}
