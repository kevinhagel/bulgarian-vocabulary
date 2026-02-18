import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { LemmaDetailDTO, ReviewStatus } from '@/features/vocabulary/types';

/**
 * Mutation to update the review status of a vocabulary entry.
 * Used to mark words as REVIEWED (approving them for study) or PENDING.
 */
export function useUpdateReviewStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, status }: { id: number; status: ReviewStatus }): Promise<LemmaDetailDTO> => {
      const response = await api.patch<LemmaDetailDTO>(`/vocabulary/${id}/review-status?status=${status}`);
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
