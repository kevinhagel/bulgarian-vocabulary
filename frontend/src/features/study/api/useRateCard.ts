import { useMutation } from '@tanstack/react-query';
import api from '@/lib/api';
import type { StudyCardDTO, RateCardRequestDTO } from '@/features/study/types';

interface RateCardParams {
  sessionId: number;
  request: RateCardRequestDTO;
}

export function useRateCard() {
  return useMutation<StudyCardDTO | null, Error, RateCardParams>({
    mutationFn: async ({ sessionId, request }) => {
      const response = await api.post<StudyCardDTO>(
        `/study/sessions/${sessionId}/rate`,
        request
      );
      return response.status === 204 ? null : response.data;
    },
  });
}
