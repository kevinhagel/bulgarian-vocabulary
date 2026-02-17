import { useMutation } from '@tanstack/react-query';
import api from '@/lib/api';
import type { StartSessionResponseDTO } from '@/features/study/types';

export function useStartSession() {
  return useMutation<StartSessionResponseDTO, Error, { maxCards?: number }>({
    mutationFn: async ({ maxCards = 20 }) => {
      const response = await api.post<StartSessionResponseDTO>(
        `/study/sessions?maxCards=${maxCards}`
      );
      return response.data;
    },
  });
}
