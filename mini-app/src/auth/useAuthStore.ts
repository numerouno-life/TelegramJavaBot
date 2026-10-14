import { create } from 'zustand';
import { UserResponse } from '../types/user';

interface AuthState {
  token: string | null;
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  setAuth: (token: string, user: UserResponse) => void;
  setUser: (user: UserResponse) => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('miniapp_token'),
  user: null,
  isAuthenticated: !!localStorage.getItem('miniapp_token'),
  isLoading: true,
  error: null,

  setAuth: (token: string, user: UserResponse) => {
    localStorage.setItem('miniapp_token', token);
    set({
      token,
      user,
      isAuthenticated: true,
      isLoading: false,
      error: null,
    });
  },

  setUser: (user: UserResponse) => set({ user }),

  logout: () => {
    localStorage.removeItem('miniapp_token');
    set({
      token: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  },

  setLoading: (isLoading: boolean) => set({ isLoading }),
  setError: (error: string | null) => set({ error, isLoading: false }),
}));
