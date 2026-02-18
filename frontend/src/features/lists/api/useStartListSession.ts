import { useMutation } from '@tanstack/react-query';
import api from '@/lib/api';
import type { StartSessionResponseDTO } from '@/features/study/types';

interface StartListSessionParams {
  listId: number;
  mode: 'DUE' | 'ALL';
  maxCards?: number;
}

export function useStartListSession() {
  return useMutation<StartSessionResponseDTO, Error, StartListSessionParams>({
    mutationFn: async ({ listId, mode, maxCards = 20 }) => {
      const response = await api.post<StartSessionResponseDTO>(
        `/lists/${listId}/sessions?mode=${mode}&maxCards=${maxCards}`
      );
      return response.data;
    },
  });
}
