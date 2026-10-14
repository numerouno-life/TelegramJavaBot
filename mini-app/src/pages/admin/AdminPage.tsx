import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { format, parseISO } from 'date-fns';
import { adminApi } from '../../api/admin';
import { useAuthStore } from '../../auth/useAuthStore';
import { SERVICE_TYPE_LABELS } from '../../types/appointment';
import { UserRole } from '../../types/user';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Skeleton } from '../../components/ui/Skeleton';

type AdminTab = 'appointments' | 'users';

export const AdminPage: React.FC = () => {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<AdminTab>('appointments');
  const [selectedDate, setSelectedDate] = useState<string>(() =>
    format(new Date(), 'yyyy-MM-dd')
  );
  const [userSearch, setUserSearch] = useState('');

  // Защита доступа
  if (user?.role !== 'ADMIN') {
    return (
      <Card className="text-center py-12 space-y-4">
        <div className="text-4xl">🔒</div>
        <h2 className="text-lg font-bold text-tg-destructive">Доступ ограничен</h2>
        <p className="text-sm text-tg-hint">
          Эта панель доступна только администраторам барбершопа.
        </p>
        <Link to="/">
          <Button variant="secondary">На главную</Button>
        </Link>
      </Card>
    );
  }

  // Запрос записей на выбранную дату
  const {
    data: appointments = [],
    isLoading: isLoadingAppointments,
  } = useQuery({
    queryKey: ['admin', 'appointments', selectedDate],
    queryFn: () => adminApi.getAppointmentsByDate(selectedDate),
    enabled: activeTab === 'appointments',
  });

  // Запрос всех пользователей
  const {
    data: users = [],
    isLoading: isLoadingUsers,
  } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: adminApi.getAllUsers,
    enabled: activeTab === 'users',
  });

  // Мутация смены роли
  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: UserRole }) =>
      adminApi.updateUserRole(id, role),
    onSuccess: () => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('success');
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (err: any) => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('error');
      alert(err.response?.data?.message || 'Ошибка изменения роли');
    },
  });

  // Мутация блокировки
  const blockMutation = useMutation({
    mutationFn: ({ id, blocked }: { id: number; blocked: boolean }) =>
      adminApi.updateUserBlock(id, blocked),
    onSuccess: () => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('success');
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (err: any) => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('error');
      alert(err.response?.data?.message || 'Ошибка изменения блокировки');
    },
  });

  const handleToggleRole = (userId: number, currentRole: UserRole, userName: string | null) => {
    const newRole: UserRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
    const message = `Изменить роль пользователя ${userName || userId} на ${newRole}?`;

    const execute = () => {
      window.Telegram?.WebApp?.HapticFeedback?.impactOccurred('medium');
      roleMutation.mutate({ id: userId, role: newRole });
    };

    if (window.Telegram?.WebApp?.showConfirm) {
      window.Telegram.WebApp.showConfirm(message, (c) => c && execute());
    } else if (window.confirm(message)) {
      execute();
    }
  };

  const handleToggleBlock = (userId: number, isBlocked: boolean, userName: string | null) => {
    const action = isBlocked ? 'разблокировать' : 'заблокировать';
    const message = `Вы действительно хотите ${action} пользователя ${userName || userId}?`;

    const execute = () => {
      window.Telegram?.WebApp?.HapticFeedback?.impactOccurred('medium');
      blockMutation.mutate({ id: userId, blocked: !isBlocked });
    };

    if (window.Telegram?.WebApp?.showConfirm) {
      window.Telegram.WebApp.showConfirm(message, (c) => c && execute());
    } else if (window.confirm(message)) {
      execute();
    }
  };

  const filteredUsers = users.filter((u) => {
    if (!userSearch) return true;
    const q = userSearch.toLowerCase();
    return (
      (u.firstName && u.firstName.toLowerCase().includes(q)) ||
      (u.lastName && u.lastName.toLowerCase().includes(q)) ||
      (u.username && u.username.toLowerCase().includes(q)) ||
      (u.clientPhoneNumber && u.clientPhoneNumber.includes(q)) ||
      u.telegramId.toString().includes(q)
    );
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-tg-text">Панель управления</h1>
        <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-purple-500/10 text-purple-600 border border-purple-500/20">
          ADMIN
        </span>
      </div>

      {/* Переключатель вкладок */}
      <div className="flex rounded-xl bg-tg-secondary-bg p-1">
        <button
          onClick={() => {
            window.Telegram?.WebApp?.HapticFeedback?.selectionChanged();
            setActiveTab('appointments');
          }}
          className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all ${
            activeTab === 'appointments'
              ? 'bg-tg-bg text-tg-button shadow-sm font-semibold'
              : 'text-tg-hint hover:text-tg-text'
          }`}
        >
          Записи
        </button>
        <button
          onClick={() => {
            window.Telegram?.WebApp?.HapticFeedback?.selectionChanged();
            setActiveTab('users');
          }}
          className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all ${
            activeTab === 'users'
              ? 'bg-tg-bg text-tg-button shadow-sm font-semibold'
              : 'text-tg-hint hover:text-tg-text'
          }`}
        >
          Клиенты ({users.length})
        </button>
      </div>

      {/* ВКЛАДКА 1: ЗАПИСИ */}
      {activeTab === 'appointments' && (
        <div className="space-y-3">
          {/* Селектор даты */}
          <Card className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-tg-hint uppercase">
                Дата расписания
              </label>
              <div className="flex gap-1.5">
                <button
                  type="button"
                  onClick={() => setSelectedDate(format(new Date(), 'yyyy-MM-dd'))}
                  className="px-2.5 py-1 text-xs rounded-lg bg-tg-secondary-bg hover:bg-tg-button/10 text-tg-text transition-colors"
                >
                  Сегодня
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const d = new Date();
                    d.setDate(d.getDate() + 1);
                    setSelectedDate(format(d, 'yyyy-MM-dd'));
                  }}
                  className="px-2.5 py-1 text-xs rounded-lg bg-tg-secondary-bg hover:bg-tg-button/10 text-tg-text transition-colors"
                >
                  Завтра
                </button>
              </div>
            </div>

            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl border border-tg-secondary-bg bg-tg-bg text-tg-text text-sm focus:outline-none focus:border-tg-button font-medium"
            />
          </Card>

          {/* Список записей */}
          {isLoadingAppointments ? (
            <div className="space-y-2">
              <Skeleton className="h-24 w-full rounded-2xl" />
              <Skeleton className="h-24 w-full rounded-2xl" />
            </div>
          ) : appointments.length === 0 ? (
            <Card className="text-center py-8">
              <p className="text-tg-hint text-sm">На эту дату записей нет</p>
            </Card>
          ) : (
            <div className="space-y-2.5">
              <div className="text-xs font-semibold text-tg-hint px-1">
                Всего записей на дату: {appointments.length}
              </div>
              {appointments.map((app) => {
                const dateObj = parseISO(app.dateTime);
                return (
                  <Card key={app.id} className="space-y-2">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-base font-bold text-tg-link">
                          {format(dateObj, 'HH:mm')}
                        </span>
                        <span className="text-sm font-semibold text-tg-text">
                          {app.serviceType
                            ? SERVICE_TYPE_LABELS[app.serviceType]
                            : 'Услуга не указана'}
                        </span>
                      </div>
                      <StatusBadge status={app.status} />
                    </div>

                    <div className="pt-1 border-t border-tg-secondary-bg flex items-center justify-between text-xs">
                      <div className="text-tg-text">
                        <span className="font-medium">{app.clientName || 'Без имени'}</span>
                        {app.clientTelegramId && (
                          <span className="text-tg-hint ml-1.5">(ID: {app.clientTelegramId})</span>
                        )}
                      </div>

                      {app.clientPhone ? (
                        <a
                          href={`tel:${app.clientPhone}`}
                          className="text-tg-link font-medium hover:underline flex items-center gap-1"
                        >
                          📞 {app.clientPhone}
                        </a>
                      ) : (
                        <span className="text-tg-hint">Тел. не указан</span>
                      )}
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ВКЛАДКА 2: ПОЛЬЗОВАТЕЛИ */}
      {activeTab === 'users' && (
        <div className="space-y-3">
          <input
            type="text"
            value={userSearch}
            onChange={(e) => setUserSearch(e.target.value)}
            placeholder="Поиск по имени, username или тел..."
            className="w-full px-3 py-2.5 rounded-xl border border-tg-secondary-bg bg-tg-bg text-tg-text text-sm focus:outline-none focus:border-tg-button"
          />

          {isLoadingUsers ? (
            <div className="space-y-2">
              <Skeleton className="h-28 w-full rounded-2xl" />
              <Skeleton className="h-28 w-full rounded-2xl" />
            </div>
          ) : filteredUsers.length === 0 ? (
            <Card className="text-center py-8">
              <p className="text-tg-hint text-sm">Пользователи не найдены</p>
            </Card>
          ) : (
            <div className="space-y-2.5">
              {filteredUsers.map((u) => {
                const isSelf = u.telegramId === user?.telegramId;

                return (
                  <Card key={u.telegramId} className="space-y-2.5">
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="font-semibold text-sm text-tg-text flex items-center gap-1.5">
                          <span>{u.firstName || 'Пользователь'} {u.lastName || ''}</span>
                          {u.role === 'ADMIN' && (
                            <span className="px-1.5 py-0.2 text-[10px] font-bold rounded bg-purple-500/10 text-purple-600">
                              ADMIN
                            </span>
                          )}
                          {u.isBlocked && (
                            <span className="px-1.5 py-0.2 text-[10px] font-bold rounded bg-red-500/10 text-red-600">
                              BLOCKED
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-tg-hint">
                          {u.username ? `@${u.username} • ` : ''}ID: {u.telegramId}
                        </div>
                        {u.clientPhoneNumber && (
                          <div className="text-xs text-tg-hint">
                            Тел: {u.clientPhoneNumber}
                          </div>
                        )}
                      </div>
                    </div>

                    {!isSelf && (
                      <div className="flex gap-2 pt-2 border-t border-tg-secondary-bg">
                        <Button
                          variant="secondary"
                          size="sm"
                          className="flex-1 text-xs"
                          loading={roleMutation.isPending}
                          onClick={() => handleToggleRole(u.telegramId, u.role, u.firstName)}
                        >
                          {u.role === 'ADMIN' ? 'Снять админа' : 'Сделать админом'}
                        </Button>
                        <Button
                          variant={u.isBlocked ? 'secondary' : 'destructive'}
                          size="sm"
                          className="flex-1 text-xs"
                          loading={blockMutation.isPending}
                          onClick={() => handleToggleBlock(u.telegramId, u.isBlocked, u.firstName)}
                        >
                          {u.isBlocked ? 'Разблокировать' : 'Заблокировать'}
                        </Button>
                      </div>
                    )}
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
