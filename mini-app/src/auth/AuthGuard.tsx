import React, { useEffect, useState } from 'react';
import { useAuthStore } from './useAuthStore';
import { authApi } from '../api/auth';
import { Smartphone, RefreshCw, AlertCircle } from 'lucide-react';

interface AuthGuardProps {
  children: React.ReactNode;
}

export const AuthGuard: React.FC<AuthGuardProps> = ({ children }) => {
  const { isAuthenticated, isLoading, error, setAuth, setUser, logout, setLoading, setError } = useAuthStore();
  const [isNotTelegram, setIsNotTelegram] = useState(false);

  const initAuth = async () => {
    setLoading(true);
    setError(null);
    setIsNotTelegram(false);

    try {
      // 1. Инициализация Telegram WebApp SDK
      const tg = window.Telegram?.WebApp;
      if (tg) {
        tg.ready();
        tg.expand();
      }

      let initData = tg?.initData;

      const isLocalhost =
        typeof window !== 'undefined' &&
        (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');

      // Режим devInitData (для import.meta.env.DEV и локального тестирования в браузере):
      // Если в URL есть ?devInitData=<urlencoded>, используем его вместо WebApp.initData для POST /api/auth/telegram
      if (import.meta.env.DEV || isLocalhost) {
        const urlParams = new URLSearchParams(window.location.search);
        const devInitData = urlParams.get('devInitData');
        if (devInitData) initData = devInitData;
      }

      // 2. Если initData отсутствует
      if (!initData) {
        // В продакшене блокируем доступ с сообщением (кроме локального тестирования на localhost)
        if (!import.meta.env.DEV && !isLocalhost) {
          setIsNotTelegram(true);
          setLoading(false);
          return;
        }

        // В DEV режиме / на localhost: fallback для локального тестирования в браузере
        console.warn('Локальный режим: initData отсутствует, используем режим отладки.');
        // Если уже есть токен в localStorage, пробуем получить пользователя
        const existingToken = localStorage.getItem('miniapp_token');
        if (existingToken) {
          try {
            const me = await authApi.getMe();
            setUser(me);
            setLoading(false);
            return;
          } catch {
            localStorage.removeItem('miniapp_token');
          }
        }

        // Иначе подставляем тестового пользователя для верстки в браузере
        setAuth('mock-dev-jwt-token', {
          telegramId: 999999999,
          username: 'local_dev',
          firstName: 'Тестовый',
          lastName: 'Мастер',
          clientPhoneNumber: '+7 (999) 000-00-00',
          role: 'ADMIN',
          isBlocked: false,
        });
        setLoading(false);
        return;
      }

      // 3. Есть initData: выполняем вход через бэкенд
      const authData = await authApi.authenticateWithTelegram(initData);
      setAuth(authData.token, authData.user);
    } catch (err: unknown) {
      console.error('Ошибка аутентификации Telegram:', err);
      const msg = err instanceof Error ? err.message : 'Не удалось войти в приложение';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    initAuth();

    // Слушатель 401 ошибки из axios
    const handleUnauthorized = () => {
      if (!window.Telegram?.WebApp?.initData) {
        console.warn('401 в режиме отладки: бэкенд отклонил mock-токен.');
        return;
      }
      logout();
      initAuth();
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, []);

  // Экран: не в Telegram (PROD)
  if (isNotTelegram) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-6 text-center bg-tg-bg text-tg-text">
        <div className="w-16 h-16 rounded-full bg-tg-button/10 flex items-center justify-center text-tg-button mb-4">
          <Smartphone size={32} />
        </div>
        <h1 className="text-xl font-bold mb-2">Откройте через Telegram</h1>
        <p className="text-sm text-tg-hint max-w-xs mb-6">
          Это приложение предназначено для работы внутри мессенджера Telegram. Откройте диалог с ботом и нажмите кнопку меню.
        </p>
      </div>
    );
  }

  // Экран: загрузка
  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-6 text-center bg-tg-bg text-tg-text">
        <RefreshCw className="w-8 h-8 text-tg-button animate-spin mb-4" />
        <p className="text-sm text-tg-hint">Инициализация приложения...</p>
      </div>
    );
  }

  // Экран: ошибка
  if (error && !isAuthenticated) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-6 text-center bg-tg-bg text-tg-text">
        <div className="w-16 h-16 rounded-full bg-tg-destructive/10 flex items-center justify-center text-tg-destructive mb-4">
          <AlertCircle size={32} />
        </div>
        <h1 className="text-xl font-bold mb-2">Ошибка авторизации</h1>
        <p className="text-sm text-tg-hint max-w-xs mb-6">{error}</p>
        <button
          onClick={initAuth}
          className="px-6 py-2.5 rounded-tg bg-tg-button text-tg-button-text font-medium text-sm active:opacity-80 transition-opacity"
        >
          Попробовать снова
        </button>
      </div>
    );
  }

  return <>{children}</>;
};
