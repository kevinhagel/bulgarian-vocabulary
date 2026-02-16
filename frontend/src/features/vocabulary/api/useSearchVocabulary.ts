import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { LemmaResponseDTO } from '@/types';

/**
 * TanStack Query hook for searching vocabulary entries.
 * Only executes search when query has 2 or more characters.
 *
 * @param query - The search text
 * @returns Query result with array of matching vocabulary entries
 */
export function useSearchVocabulary(query: string) {
  return useQuery<LemmaResponseDTO[]>({
    queryKey: ['vocabulary', 'search', query],
    queryFn: async () => {
      const response = await api.get<LemmaResponseDTO[]>('/vocabulary/search', {
        params: { q: query },
      });
      return response.data;
    },
    enabled: query.length >= 2, // Only search with 2+ characters
    // staleTime is inherited from the QueryClient (5 minutes)
  });
}
