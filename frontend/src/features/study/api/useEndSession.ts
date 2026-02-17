import { useMutation } from '@tanstack/react-query';
import api from '@/lib/api';
import type { SessionSummaryDTO } from '@/features/study/types';

export function useEndSession() {
  return useMutation<SessionSummaryDTO, Error, { sessionId: number }>({
    mutationFn: async ({ sessionId }) => {
      const response = await api.post<SessionSummaryDTO>(
        `/study/sessions/${sessionId}/end`
      );
      return response.data;
    },
  });
}
