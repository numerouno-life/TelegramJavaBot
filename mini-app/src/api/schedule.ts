import { apiClient } from './client';
import { WorkScheduleResponse, LunchBreakResponse } from '../types/schedule';

export const scheduleApi = {
  getWorkDays: async (): Promise<WorkScheduleResponse[]> => {
    const response = await apiClient.get<WorkScheduleResponse[]>('/schedule/work-days');
    return response.data;
  },

  getLunchBreaks: async (): Promise<LunchBreakResponse[]> => {
    const response = await apiClient.get<LunchBreakResponse[]>('/schedule/lunch-breaks');
    return response.data;
  },
};
