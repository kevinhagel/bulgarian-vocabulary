import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { LemmaDetailDTO } from '@/types/api';

/**
 * TanStack Query hook for fetching full vocabulary detail including inflections.
 * GET /api/vocabulary/{id}.
 * Used by edit modal to pre-populate the form.
 *
 * @param id - Lemma ID to fetch, or null to disable the query
 */
export function useVocabularyDetail(id: number | null) {
  return useQuery({
    queryKey: ['vocabulary', id],
    queryFn: async (): Promise<LemmaDetailDTO> => {
      const response = await api.get<LemmaDetailDTO>(`/vocabulary/${id}`);
      return response.data;
    },
    enabled: id !== null,
  });
}
