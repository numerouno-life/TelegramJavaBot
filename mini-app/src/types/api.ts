import { UserResponse } from './user';

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface TelegramInitDataRequest {
  initData: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
