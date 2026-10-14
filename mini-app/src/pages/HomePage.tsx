import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Calendar, Clock, MapPin, Scissors, ChevronRight, Sparkles } from 'lucide-react';
import { useAuthStore } from '../auth/useAuthStore';
import { appointmentApi } from '../api/appointments';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { StatusBadge } from '../components/ui/StatusBadge';
import { Skeleton } from '../components/ui/Skeleton';
import {
  ServiceType,
  SERVICE_TYPE_LABELS,
  SERVICE_PRICES_ESTIMATE,
} from '../types/appointment';

export const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();

  // Загружаем записи текущего пользователя
  const { data: appointments, isLoading: isLoadingAppointments } = useQuery({
    queryKey: ['my-appointments'],
    queryFn: appointmentApi.getMyAppointments,
  });

  // Находим ближайшую активную запись
  const nearestAppointment = React.useMemo(() => {
    if (!appointments) return null;
    const now = new Date();
    return appointments
      .filter((app) => app.status !== 'CANCELED' && new Date(app.dateTime) > now)
      .sort((a, b) => new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime())[0];
  }, [appointments]);

  const handleSelectService = (service: ServiceType) => {
    navigate(`/book?service=${service}`);
  };

  const services: ServiceType[] = ['HAIRCUT', 'BEARD', 'SHAVE', 'COLORING', 'ANOTHER'];

  return (
    <div className="space-y-6">
      {/* Приветствие */}
      <div className="flex items-center justify-between pt-2">
        <div>
          <p className="text-xs text-tg-hint uppercase tracking-wider font-semibold">Добро пожаловать</p>
          <h1 className="text-2xl font-bold tracking-tight">
            {user?.firstName ? `Привет, ${user.firstName}!` : 'Салон красоты & Барбершоп'}
          </h1>
        </div>
        <div className="w-10 h-10 rounded-full bg-tg-button/10 flex items-center justify-center text-tg-button font-bold text-sm">
          {user?.firstName?.[0] || 'U'}
        </div>
      </div>

      {/* Ближайшая запись (если есть) */}
      {isLoadingAppointments ? (
        <Skeleton className="h-32 w-full" />
      ) : nearestAppointment ? (
        <Card variant="secondary" className="border-l-4 border-l-tg-button relative overflow-hidden">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-tg-button uppercase tracking-wide flex items-center gap-1">
              <Sparkles size={14} /> Ближайшая запись
            </span>
            <StatusBadge status={nearestAppointment.status} />
          </div>

          <h3 className="font-semibold text-base mb-2">
            {nearestAppointment.serviceType
              ? SERVICE_TYPE_LABELS[nearestAppointment.serviceType]
              : 'Услуга не указана'}
          </h3>

          <div className="flex items-center gap-4 text-xs text-tg-hint mb-3">
            <span className="flex items-center gap-1.5">
              <Calendar size={14} />
              {format(parseISO(nearestAppointment.dateTime), 'd MMMM (EEEE)', { locale: ru })}
            </span>
            <span className="flex items-center gap-1.5 font-medium text-tg-text">
              <Clock size={14} />
              {format(parseISO(nearestAppointment.dateTime), 'HH:mm')}
            </span>
          </div>

          <button
            onClick={() => navigate('/appointments')}
            className="text-xs text-tg-button font-medium flex items-center gap-1 active:opacity-70"
          >
            Все мои записи <ChevronRight size={14} />
          </button>
        </Card>
      ) : null}

      {/* Большая кнопка записи */}
      <Button
        size="lg"
        variant="primary"
        onClick={() => navigate('/book')}
        className="py-4 text-lg shadow-md"
      >
        <Scissors size={20} /> Записаться онлайн
      </Button>

      {/* Список услуг */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-bold">Наши услуги</h2>
          <span className="text-xs text-tg-hint">5 категорий</span>
        </div>

        <div className="grid grid-cols-1 gap-2.5">
          {services.map((serviceKey) => (
            <Card
              key={serviceKey}
              variant="interactive"
              onClick={() => handleSelectService(serviceKey)}
              className="flex items-center justify-between p-3.5"
            >
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-tg bg-tg-button/10 flex items-center justify-center text-tg-button">
                  <Scissors size={18} />
                </div>
                <div>
                  <h4 className="text-sm font-semibold">{SERVICE_TYPE_LABELS[serviceKey]}</h4>
                  <p className="text-xs text-tg-hint">{SERVICE_PRICES_ESTIMATE[serviceKey]}</p>
                </div>
              </div>
              <ChevronRight size={18} className="text-tg-hint" />
            </Card>
          ))}
        </div>
      </div>

      {/* Информация о салоне */}
      <Card variant="default" className="space-y-3">
        <h3 className="font-semibold text-sm">Контакты и адрес</h3>
        <div className="space-y-2 text-xs text-tg-hint">
          <div className="flex items-start gap-2">
            <MapPin size={16} className="text-tg-button shrink-0 mt-0.5" />
            <span>г. Ульяновск, ул. Славского, д. 6</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock size={16} className="text-tg-button shrink-0" />
            <span>Ежедневно с 10:00 до 20:00</span>
          </div>
        </div>
      </Card>
    </div>
  );
};
