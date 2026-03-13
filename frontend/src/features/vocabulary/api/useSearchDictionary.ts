import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { DictionarySearchResultDTO } from '../types';

/**
 * TanStack Query hook for searching the Kaikki/Wiktionary dictionary.
 * Calls GET /api/dictionary/search?q=<query>.
 * Only executes when query has 2+ characters.
 */
export function useSearchDictionary(query: string) {
  return useQuery<DictionarySearchResultDTO[]>({
    queryKey: ['dictionary', 'search', query],
    queryFn: async () => {
      const response = await api.get<DictionarySearchResultDTO[]>('/dictionary/search', {
        params: { q: query },
      });
      return response.data;
    },
    enabled: query.length >= 2,
  });
}
