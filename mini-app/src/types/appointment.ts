export type ServiceType = 'HAIRCUT' | 'BEARD' | 'SHAVE' | 'COLORING' | 'ANOTHER';

export type StatusAppointment = 'ACTIVE' | 'PENDING' | 'CONFIRMED' | 'CANCELED';

export interface AppointmentResponse {
  id: number;
  dateTime: string; // ISO-8601 string, e.g. "2026-10-15T14:00:00"
  status: StatusAppointment;
  serviceType: ServiceType | null; // nullable для записей, созданных через бота
  clientName: string;
  clientPhone: string | null;
  clientTelegramId: number | null;
}

export interface CreateAppointmentRequest {
  dateTime: string; // ISO-8601 string "YYYY-MM-DDTHH:mm:ss"
  serviceType: ServiceType;
}

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  HAIRCUT: 'Стрижка',
  BEARD: 'Уход за бородой',
  SHAVE: 'Бритье головы',
  COLORING: 'Окрашивание волос',
  ANOTHER: 'Другое',
};

export const SERVICE_PRICES_ESTIMATE: Record<ServiceType, string> = {
  HAIRCUT: '1 500 ₽',
  BEARD: '800 ₽',
  SHAVE: '1 200 ₽',
  COLORING: '2 500 ₽',
  ANOTHER: 'По договорённости',
};

export const STATUS_LABELS: Record<StatusAppointment, string> = {
  ACTIVE: 'Активна',
  CONFIRMED: 'Подтверждена',
  PENDING: 'Ожидает',
  CANCELED: 'Отменена',
};
