import { apiClient } from './client';
import { AuthResponse } from '../types/api';
import { UserResponse } from '../types/user';

export const authApi = {
  authenticateWithTelegram: async (initData: string): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/telegram', { initData });
    return response.data;
  },

  getMe: async (): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>('/auth/me');
    return response.data;
  },
};
