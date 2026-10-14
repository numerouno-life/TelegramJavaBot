export interface WorkScheduleResponse {
  dayOfWeek: number; // 1 = Понедельник, ..., 7 = Воскресенье
  dayName: string;
  isWorkingDay: boolean;
  startTime: string | null; // e.g. "10:00:00"
  endTime: string | null;   // e.g. "20:00:00"
}

export interface LunchBreakResponse {
  dayOfWeek: number;
  startTime: string | null;
  endTime: string | null;
  isActive: boolean;
}
