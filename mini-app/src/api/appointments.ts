import { apiClient } from './client';
import { AppointmentResponse, CreateAppointmentRequest } from '../types/appointment';

export const appointmentApi = {
  getMyAppointments: async (): Promise<AppointmentResponse[]> => {
    const response = await apiClient.get<AppointmentResponse[]>('/appointments/my');
    return response.data;
  },

  getAvailableDates: async (from: string, days = 30): Promise<string[]> => {
    const response = await apiClient.get<string[]>('/appointments/available-dates', {
      params: { from, days },
    });
    return response.data;
  },

  getAvailableSlots: async (date: string): Promise<string[]> => {
    const response = await apiClient.get<string[]>('/appointments/available-slots', {
      params: { date },
    });
    return response.data;
  },

  createAppointment: async (data: CreateAppointmentRequest): Promise<AppointmentResponse> => {
    const response = await apiClient.post<AppointmentResponse>('/appointments', data);
    return response.data;
  },

  cancelAppointment: async (id: number): Promise<void> => {
    await apiClient.delete(`/appointments/${id}`);
  },
};
