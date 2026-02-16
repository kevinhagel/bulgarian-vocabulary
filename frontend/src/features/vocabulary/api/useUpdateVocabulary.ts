import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { UpdateLemmaRequest, LemmaDetailDTO } from '@/types/api';

interface UpdateVocabularyVariables {
  id: number;
  data: UpdateLemmaRequest;
}

/**
 * TanStack Query mutation for updating existing vocabulary entries.
 * PUT /api/vocabulary/{id} with UpdateLemmaRequest body.
 * Returns updated LemmaDetailDTO on success.
 * Invalidates vocabulary list and detail queries.
 */
export function useUpdateVocabulary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: UpdateVocabularyVariables): Promise<LemmaDetailDTO> => {
      const response = await api.put<LemmaDetailDTO>(`/vocabulary/${id}`, data);
      return response.data;
    },
    onSuccess: (_data, variables) => {
      // Invalidate vocabulary list queries
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
      // Invalidate this specific vocabulary detail query
      queryClient.invalidateQueries({ queryKey: ['vocabulary', variables.id] });
    },
  });
}
