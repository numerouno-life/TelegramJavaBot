export type UserRole = 'USER' | 'ADMIN';

export interface UserResponse {
  telegramId: number;
  username: string | null;
  firstName: string | null;
  lastName: string | null;
  clientPhoneNumber: string | null;
  role: UserRole;
  isBlocked: boolean;
}

export interface UpdateUserRequest {
  firstName?: string;
  clientPhoneNumber?: string;
}
