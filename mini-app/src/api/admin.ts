import { apiClient } from './client';
import { AppointmentResponse } from '../types/appointment';
import { UserResponse, UserRole } from '../types/user';

export const adminApi = {
  getAppointmentsByDate: async (date: string): Promise<AppointmentResponse[]> => {
    const response = await apiClient.get<AppointmentResponse[]>('/admin/appointments', {
      params: { date },
    });
    return response.data;
  },

  getAllUsers: async (): Promise<UserResponse[]> => {
    const response = await apiClient.get<UserResponse[]>('/admin/users');
    return response.data;
  },

  updateUserRole: async (id: number, role: UserRole): Promise<UserResponse> => {
    const response = await apiClient.patch<UserResponse>(`/admin/users/${id}/role`, { role });
    return response.data;
  },

  updateUserBlock: async (id: number, blocked: boolean): Promise<UserResponse> => {
    const response = await apiClient.patch<UserResponse>(`/admin/users/${id}/block`, { blocked });
    return response.data;
  },
};
