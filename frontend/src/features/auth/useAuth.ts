import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';

interface AuthUser {
  name: string;
  email: string;
  picture: string;
  isAdmin: boolean;
}

export function useAuth() {
  return useQuery<AuthUser>({
    queryKey: ['auth', 'me'],
    queryFn: () => api.get<AuthUser>('/auth/me').then(r => r.data),
    retry: false,
    staleTime: 5 * 60 * 1000,
  });
}
