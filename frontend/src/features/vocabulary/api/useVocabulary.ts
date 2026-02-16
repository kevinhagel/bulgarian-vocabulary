import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PaginatedResponse, LemmaResponseDTO, Source, PartOfSpeech, DifficultyLevel } from '@/types';

interface UseVocabularyParams {
  page?: number;
  size?: number;
  source?: Source | null;
  partOfSpeech?: PartOfSpeech | null;
  difficultyLevel?: DifficultyLevel | null;
}

/**
 * TanStack Query hook for fetching paginated vocabulary list with optional filters.
 *
 * @param params - Query parameters for filtering and pagination
 * @returns Query result with paginated vocabulary data
 */
export function useVocabulary(params: UseVocabularyParams = {}) {
  const { page = 0, size = 20, source, partOfSpeech, difficultyLevel } = params;

  return useQuery<PaginatedResponse<LemmaResponseDTO>>({
    queryKey: ['vocabulary', { page, size, source, partOfSpeech, difficultyLevel }],
    queryFn: async () => {
      // Build query parameters, only including non-null filters
      const queryParams = new URLSearchParams();
      queryParams.set('page', page.toString());
      queryParams.set('size', size.toString());

      if (source) {
        queryParams.set('source', source);
      }
      if (partOfSpeech) {
        queryParams.set('partOfSpeech', partOfSpeech);
      }
      if (difficultyLevel) {
        queryParams.set('difficultyLevel', difficultyLevel);
      }

      const response = await api.get<PaginatedResponse<LemmaResponseDTO>>(
        `/vocabulary?${queryParams.toString()}`
      );
      return response.data;
    },
    // staleTime is inherited from the QueryClient (5 minutes)
  });
}
