import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { LemmaDetailDTO } from '@/features/vocabulary/types';

/**
 * Mutation to flag a vocabulary entry as needing correction.
 * Sets reviewStatus to NEEDS_CORRECTION and excludes the word from study sessions.
 */
export function useFlagVocabulary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number): Promise<LemmaDetailDTO> => {
      const response = await api.post<LemmaDetailDTO>(`/vocabulary/${id}/flag`);
      return response.data;
    },
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['vocabulary', id] });
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
      queryClient.invalidateQueries({ queryKey: ['review-queue'] });
      queryClient.invalidateQueries({ queryKey: ['study', 'due-count'] });
    },
  });
}
