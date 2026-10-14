import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { format, parseISO, isPast } from 'date-fns';
import { ru } from 'date-fns/locale';
import { appointmentApi } from '../api/appointments';
import { AppointmentResponse, SERVICE_TYPE_LABELS, SERVICE_PRICES_ESTIMATE } from '../types/appointment';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { StatusBadge } from '../components/ui/StatusBadge';
import { Skeleton } from '../components/ui/Skeleton';

type TabType = 'upcoming' | 'history';

export const AppointmentsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('upcoming');
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const queryClient = useQueryClient();

  const {
    data: appointments = [],
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['appointments', 'my'],
    queryFn: appointmentApi.getMyAppointments,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => appointmentApi.cancelAppointment(id),
    onSuccess: () => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('success');
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
      setCancellingId(null);
    },
    onError: (err: any) => {
      window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('error');
      const msg = err.response?.data?.message || 'Не удалось отменить запись';
      if (window.Telegram?.WebApp?.showAlert) {
        window.Telegram.WebApp.showAlert(msg);
      } else {
        alert(msg);
      }
      setCancellingId(null);
    },
  });

  const handleCancelClick = (appointment: AppointmentResponse) => {
    window.Telegram?.WebApp?.HapticFeedback?.impactOccurred('medium');

    const formattedDate = format(parseISO(appointment.dateTime), 'd MMMM в HH:mm', { locale: ru });
    const message = `Вы действительно хотите отменить запись на ${formattedDate}?`;

    const executeCancel = () => {
      setCancellingId(appointment.id);
      cancelMutation.mutate(appointment.id);
    };

    if (window.Telegram?.WebApp?.showConfirm) {
      window.Telegram.WebApp.showConfirm(message, (confirmed) => {
        if (confirmed) executeCancel();
      });
    } else {
      if (window.confirm(message)) {
        executeCancel();
      }
    }
  };

  const now = new Date();

  // Разделение на предстоящие и историю
  const upcomingAppointments = appointments
    .filter((a) => a.status !== 'CANCELED' && parseISO(a.dateTime) >= now)
    .sort((a, b) => new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime());

  const historyAppointments = appointments
    .filter((a) => a.status === 'CANCELED' || parseISO(a.dateTime) < now)
    .sort((a, b) => new Date(b.dateTime).getTime() - new Date(a.dateTime).getTime());

  const currentList = activeTab === 'upcoming' ? upcomingAppointments : historyAppointments;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-tg-text">Мои записи</h1>
        <Link to="/book">
          <Button size="sm" variant="secondary">
            + Новая запись
          </Button>
        </Link>
      </div>

      {/* Переключатель вкладок */}
      <div className="flex rounded-xl bg-tg-secondary-bg p-1">
        <button
          onClick={() => {
            window.Telegram?.WebApp?.HapticFeedback?.selectionChanged();
            setActiveTab('upcoming');
          }}
          className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all ${
            activeTab === 'upcoming'
              ? 'bg-tg-bg text-tg-button shadow-sm font-semibold'
              : 'text-tg-hint hover:text-tg-text'
          }`}
        >
          Предстоящие ({upcomingAppointments.length})
        </button>
        <button
          onClick={() => {
            window.Telegram?.WebApp?.HapticFeedback?.selectionChanged();
            setActiveTab('history');
          }}
          className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all ${
            activeTab === 'history'
              ? 'bg-tg-bg text-tg-button shadow-sm font-semibold'
              : 'text-tg-hint hover:text-tg-text'
          }`}
        >
          История ({historyAppointments.length})
        </button>
      </div>

      {/* Список записей */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-32 w-full rounded-2xl" />
          <Skeleton className="h-32 w-full rounded-2xl" />
          <Skeleton className="h-32 w-full rounded-2xl" />
        </div>
      ) : isError ? (
        <Card className="text-center py-8">
          <p className="text-tg-destructive font-medium mb-2">Не удалось загрузить записи</p>
          <Button variant="secondary" size="sm" onClick={() => refetch()}>
            Попробовать снова
          </Button>
        </Card>
      ) : currentList.length === 0 ? (
        <Card className="text-center py-12 space-y-4">
          <div className="text-4xl">📅</div>
          <p className="text-tg-hint text-sm">
            {activeTab === 'upcoming'
              ? 'У вас пока нет активных записей'
              : 'История записей пуста'}
          </p>
          {activeTab === 'upcoming' && (
            <Link to="/book">
              <Button>Записаться к барберу</Button>
            </Link>
          )}
        </Card>
      ) : (
        <div className="space-y-3">
          {currentList.map((appointment) => {
            const dateObj = parseISO(appointment.dateTime);
            const isUpcoming = appointment.status !== 'CANCELED' && !isPast(dateObj);
            const isCancelling = cancellingId === appointment.id;

            return (
              <Card key={appointment.id} className="space-y-3">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="text-base font-semibold text-tg-text">
                      {appointment.serviceType
                        ? SERVICE_TYPE_LABELS[appointment.serviceType]
                        : 'Услуга не указана'}
                    </div>
                    {appointment.serviceType && (
                      <div className="text-xs text-tg-hint">
                        {SERVICE_PRICES_ESTIMATE[appointment.serviceType]}
                      </div>
                    )}
                  </div>
                  <StatusBadge status={appointment.status} />
                </div>

                <div className="flex items-center gap-2 text-sm text-tg-hint border-t border-tg-secondary-bg pt-2">
                  <svg className="w-4 h-4 text-tg-link" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <span className="capitalize font-medium text-tg-text">
                    {format(dateObj, 'd MMMM yyyy (EEEE)', { locale: ru })}
                  </span>
                  <span className="ml-auto font-semibold text-tg-link">
                    {format(dateObj, 'HH:mm')}
                  </span>
                </div>

                {isUpcoming && (
                  <div className="pt-1">
                    <Button
                      variant="destructive"
                      size="sm"
                      fullWidth
                      loading={isCancelling}
                      onClick={() => handleCancelClick(appointment)}
                    >
                      Отменить запись
                    </Button>
                  </div>
                )}
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};
