import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PaginatedResponse, LemmaResponseDTO } from '@/features/vocabulary/types';

/**
 * Query hook for the review queue: user-entered words that are PENDING or NEEDS_CORRECTION.
 */
export function useReviewQueue(page = 0) {
  return useQuery<PaginatedResponse<LemmaResponseDTO>>({
    queryKey: ['review-queue', page],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<LemmaResponseDTO>>('/vocabulary/review-queue', {
        params: { page, size: 20 },
      });
      return response.data;
    },
    staleTime: 30_000,
  });
}
