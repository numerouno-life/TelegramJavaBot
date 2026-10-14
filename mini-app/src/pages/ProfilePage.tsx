import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { userApi } from '../api/users';
import { useAuthStore } from '../auth/useAuthStore';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';

const profileSchema = z.object({
  firstName: z
    .string()
    .min(1, 'Имя не может быть пустым')
    .max(64, 'Слишком длинное имя'),
  clientPhoneNumber: z
    .string()
    .min(10, 'Номер телефона должен содержать минимум 10 цифр')
    .regex(/^[+]?[0-9\s\-()]+$/, 'Некорректный формат телефона (допустимы цифры, +, -, пробелы)'),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export const ProfilePage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user: authUser, setUser: setAuthUser } = useAuthStore();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const {
    data: user,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['user', 'me'],
    queryFn: userApi.getMe,
    initialData: authUser || undefined,
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
    reset,
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    values: {
      firstName: user?.firstName || '',
      clientPhoneNumber: user?.clientPhoneNumber || '',
    },
  });

  const updateMutation = useMutation({
    mutationFn: userApi.updateMe,
    onSuccess: (updatedUser) => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('success');
      setAuthUser(updatedUser);
      queryClient.setQueryData(['user', 'me'], updatedUser);
      reset({
        firstName: updatedUser.firstName || '',
        clientPhoneNumber: updatedUser.clientPhoneNumber || '',
      });
      setSuccessMessage('Профиль успешно обновлён');
      setTimeout(() => setSuccessMessage(null), 3000);
    },
    onError: (err: any) => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('error');
      const msg = err.response?.data?.message || 'Ошибка обновления профиля';
      if (window.Telegram?.WebApp?.showAlert) {
        window.Telegram.WebApp.showAlert(msg);
      } else {
        alert(msg);
      }
    },
  });

  const onSubmit = (values: ProfileFormValues) => {
    window.Telegram?.WebApp?.HapticFeedback?.impactOccurred('medium');
    updateMutation.mutate(values);
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <h1 className="text-xl font-bold text-tg-text">Мой профиль</h1>
        <Skeleton className="h-40 w-full rounded-2xl" />
        <Skeleton className="h-48 w-full rounded-2xl" />
      </div>
    );
  }

  if (isError || !user) {
    return (
      <Card className="text-center py-8">
        <p className="text-tg-destructive font-medium mb-2">Не удалось загрузить данные профиля</p>
        <Button variant="secondary" size="sm" onClick={() => refetch()}>
          Попробовать снова
        </Button>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-tg-text">Мой профиль</h1>

      {/* Информационная карточка */}
      <Card className="flex items-center gap-4">
        <div className="w-16 h-16 rounded-full bg-tg-button/10 text-tg-button flex items-center justify-center text-2xl font-bold border border-tg-button/20">
          {(user.firstName || user.username || 'U').charAt(0).toUpperCase()}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold text-tg-text truncate">
              {user.firstName || 'Клиент'} {user.lastName || ''}
            </h2>
            {user.role === 'ADMIN' && (
              <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-purple-500/10 text-purple-600 border border-purple-500/20">
                ADMIN
              </span>
            )}
          </div>
          {user.username && (
            <p className="text-xs text-tg-hint truncate">@{user.username}</p>
          )}
          <p className="text-xs text-tg-hint mt-0.5">ID: {user.telegramId}</p>
        </div>
      </Card>

      {/* Быстрый переход для администратора */}
      {user.role === 'ADMIN' && (
        <Card className="bg-gradient-to-r from-purple-500/10 to-indigo-500/10 border-purple-500/20">
          <div className="flex items-center justify-between">
            <div>
              <div className="font-semibold text-tg-text">Панель администратора</div>
              <div className="text-xs text-tg-hint">Управление записями и клиентами</div>
            </div>
            <Link to="/admin">
              <Button size="sm" variant="secondary">
                Перейти
              </Button>
            </Link>
          </div>
        </Card>
      )}

      {/* Форма редактирования контактов */}
      <Card className="space-y-4">
        <h3 className="font-semibold text-tg-text text-base">Контактные данные</h3>

        {successMessage && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 rounded-xl text-sm font-medium">
            ✓ {successMessage}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-tg-hint mb-1">
              Ваше имя
            </label>
            <input
              type="text"
              {...register('firstName')}
              className={`w-full px-3 py-2.5 rounded-xl border bg-tg-bg text-tg-text text-sm focus:outline-none transition-colors ${
                errors.firstName
                  ? 'border-tg-destructive focus:border-tg-destructive'
                  : 'border-tg-secondary-bg focus:border-tg-button'
              }`}
              placeholder="Как к вам обращаться"
            />
            {errors.firstName && (
              <p className="text-xs text-tg-destructive mt-1">
                {errors.firstName.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-medium text-tg-hint mb-1">
              Номер телефона
            </label>
            <input
              type="tel"
              {...register('clientPhoneNumber')}
              className={`w-full px-3 py-2.5 rounded-xl border bg-tg-bg text-tg-text text-sm focus:outline-none transition-colors ${
                errors.clientPhoneNumber
                  ? 'border-tg-destructive focus:border-tg-destructive'
                  : 'border-tg-secondary-bg focus:border-tg-button'
              }`}
              placeholder="+7 (999) 000-00-00"
            />
            {errors.clientPhoneNumber && (
              <p className="text-xs text-tg-destructive mt-1">
                {errors.clientPhoneNumber.message}
              </p>
            )}
          </div>

          <Button
            type="submit"
            fullWidth
            loading={updateMutation.isPending}
            disabled={!isDirty}
          >
            Сохранить изменения
          </Button>
        </form>
      </Card>
    </div>
  );
};
