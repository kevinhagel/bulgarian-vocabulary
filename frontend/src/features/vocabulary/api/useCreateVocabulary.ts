import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { CreateLemmaRequest, LemmaDetailDTO } from '@/types/api';

/**
 * TanStack Query mutation for creating new vocabulary entries.
 * POST /api/vocabulary with CreateLemmaRequest body.
 * Returns LemmaDetailDTO on success.
 * Invalidates all vocabulary queries to refresh the list.
 */
export function useCreateVocabulary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateLemmaRequest): Promise<LemmaDetailDTO> => {
      const response = await api.post<LemmaDetailDTO>('/vocabulary', data);
      return response.data;
    },
    onSuccess: () => {
      // Invalidate all queries with keys starting with ['vocabulary']
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
    },
  });
}
