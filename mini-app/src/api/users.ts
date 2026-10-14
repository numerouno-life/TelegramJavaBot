import { apiClient } from './client';
import { UserResponse, UpdateUserRequest } from '../types/user';

export const userApi = {
  getMe: async (): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>('/users/me');
    return response.data;
  },

  updateMe: async (data: UpdateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.patch<UserResponse>('/users/me', data);
    return response.data;
  },
};
