import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';
import { ArrowLeft, Check, Scissors, AlertCircle } from 'lucide-react';
import { appointmentApi } from '../api/appointments';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import {
  ServiceType,
  SERVICE_TYPE_LABELS,
  SERVICE_PRICES_ESTIMATE,
} from '../types/appointment';
import clsx from 'clsx';

const SERVICES: ServiceType[] = ['HAIRCUT', 'BEARD', 'SHAVE', 'COLORING', 'ANOTHER'];

export const BookingPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();

  const preselectedService = searchParams.get('service') as ServiceType | null;

  const [selectedService, setSelectedService] = useState<ServiceType | null>(
    preselectedService && SERVICES.includes(preselectedService) ? preselectedService : null
  );
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Дата для запроса available-dates (сегодня)
  const todayIso = format(new Date(), 'yyyy-MM-dd');

  // Загрузка доступных дат
  const { data: availableDates, isLoading: isLoadingDates } = useQuery({
    queryKey: ['available-dates', todayIso],
    queryFn: () => appointmentApi.getAvailableDates(todayIso, 30),
  });

  // Автовыбор первой даты при загрузке
  useEffect(() => {
    if (availableDates && availableDates.length > 0 && !selectedDate) {
      setSelectedDate(availableDates[0]);
    }
  }, [availableDates, selectedDate]);

  // Загрузка слотов времени на выбранную дату
  const { data: availableSlots, isLoading: isLoadingSlots } = useQuery({
    queryKey: ['available-slots', selectedDate],
    queryFn: () => (selectedDate ? appointmentApi.getAvailableSlots(selectedDate) : Promise.resolve([])),
    enabled: !!selectedDate,
  });

  // Мутация создания записи
  const createMutation = useMutation({
    mutationFn: appointmentApi.createAppointment,
    onSuccess: () => {
      try {
        window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('success');
      } catch {
        // ignore
      }
      queryClient.invalidateQueries({ queryKey: ['my-appointments'] });
      queryClient.invalidateQueries({ queryKey: ['available-slots'] });
      queryClient.invalidateQueries({ queryKey: ['available-dates'] });
      navigate('/appointments', { replace: true });
    },
    onError: (err: any) => {
      try {
        window.Telegram?.WebApp?.HapticFeedback?.notificationOccurred('error');
      } catch {
        // ignore
      }
      const message = err.response?.data?.message || 'Не удалось создать запись. Пожалуйста, выберите другое время.';
      setErrorMessage(message);
      if (window.Telegram?.WebApp?.showAlert) {
        window.Telegram.WebApp.showAlert(message);
      }
    },
  });

  const handleSelectService = (service: ServiceType) => {
    try {
      window.Telegram?.WebApp?.HapticFeedback?.selectionChanged();
    } catch {}
    setSelectedService(service);
  };

  const handleSelectDate = (date: string) => {
    try {
      window.Telegram?.WebApp?.HapticFeedback?.selectionChanged();
    } catch {}
    setSelectedDate(date);
    setSelectedSlot(null); // сбрасываем слот при смене даты
    setErrorMessage(null);
  };

  const handleSelectSlot = (slot: string) => {
    try {
      window.Telegram?.WebApp?.HapticFeedback?.selectionChanged();
    } catch {}
    setSelectedSlot(slot);
    setErrorMessage(null);
  };

  const handleConfirm = () => {
    if (!selectedService || !selectedSlot) return;
    createMutation.mutate({
      serviceType: selectedService,
      dateTime: selectedSlot,
    });
  };

  return (
    <div className="space-y-6 pb-6">
      {/* Шапка */}
      <div className="flex items-center gap-3 pt-2">
        <button
          onClick={() => navigate(-1)}
          className="w-8 h-8 rounded-full bg-tg-secondary-bg flex items-center justify-center text-tg-text active:scale-95"
        >
          <ArrowLeft size={18} />
        </button>
        <h1 className="text-xl font-bold">Онлайн запись</h1>
      </div>

      {errorMessage && (
        <div className="p-3.5 rounded-tg bg-tg-destructive/10 text-tg-destructive text-xs flex items-center gap-2">
          <AlertCircle size={16} className="shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* 1. Выбор услуги */}
      <div>
        <label className="text-xs font-semibold uppercase text-tg-hint tracking-wide mb-2 block">
          1. Выберите услугу
        </label>
        <div className="grid grid-cols-1 gap-2">
          {SERVICES.map((srv) => {
            const isSelected = selectedService === srv;
            return (
              <Card
                key={srv}
                variant={isSelected ? 'default' : 'secondary'}
                onClick={() => handleSelectService(srv)}
                className={clsx(
                  'flex items-center justify-between p-3 transition-all',
                  isSelected && 'ring-2 ring-tg-button bg-tg-button/5'
                )}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={clsx(
                      'w-8 h-8 rounded-lg flex items-center justify-center',
                      isSelected ? 'bg-tg-button text-tg-button-text' : 'bg-tg-hint/15 text-tg-text'
                    )}
                  >
                    <Scissors size={16} />
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold">{SERVICE_TYPE_LABELS[srv]}</h4>
                    <p className="text-xs text-tg-hint">{SERVICE_PRICES_ESTIMATE[srv]}</p>
                  </div>
                </div>
                {isSelected && <Check size={18} className="text-tg-button" />}
              </Card>
            );
          })}
        </div>
      </div>

      {/* 2. Выбор даты */}
      <div>
        <label className="text-xs font-semibold uppercase text-tg-hint tracking-wide mb-2 block">
          2. Выберите дату
        </label>
        {isLoadingDates ? (
          <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} className="h-16 w-14 shrink-0 rounded-tg" />
            ))}
          </div>
        ) : !availableDates || availableDates.length === 0 ? (
          <p className="text-xs text-tg-hint">Нет доступных дат для записи</p>
        ) : (
          <div className="flex gap-2 overflow-x-auto no-scrollbar py-1">
            {availableDates.map((dateStr) => {
              const dateObj = parseISO(dateStr);
              const isSelected = selectedDate === dateStr;
              return (
                <button
                  key={dateStr}
                  type="button"
                  onClick={() => handleSelectDate(dateStr)}
                  className={clsx(
                    'flex flex-col items-center justify-center min-w-[56px] h-16 rounded-tg text-xs transition-all shrink-0 select-none',
                    isSelected
                      ? 'bg-tg-button text-tg-button-text shadow-sm font-bold scale-[1.02]'
                      : 'bg-tg-secondary-bg text-tg-text hover:bg-tg-hint/10'
                  )}
                >
                  <span className="text-[10px] uppercase opacity-75">
                    {format(dateObj, 'eee', { locale: ru })}
                  </span>
                  <span className="text-base font-bold mt-0.5">{format(dateObj, 'd')}</span>
                  <span className="text-[9px] opacity-75">{format(dateObj, 'MMM', { locale: ru })}</span>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* 3. Выбор времени */}
      <div>
        <label className="text-xs font-semibold uppercase text-tg-hint tracking-wide mb-2 block">
          3. Выберите время
        </label>
        {isLoadingSlots ? (
          <div className="grid grid-cols-4 gap-2">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
              <Skeleton key={i} className="h-10 rounded-tg" />
            ))}
          </div>
        ) : !availableSlots || availableSlots.length === 0 ? (
          <p className="text-xs text-tg-hint">Нет свободных слотов на выбранную дату</p>
        ) : (
          <div className="grid grid-cols-4 gap-2">
            {availableSlots.map((slotStr) => {
              const timeStr = format(parseISO(slotStr), 'HH:mm');
              const isSelected = selectedSlot === slotStr;
              return (
                <button
                  key={slotStr}
                  type="button"
                  onClick={() => handleSelectSlot(slotStr)}
                  className={clsx(
                    'py-2 px-1 text-center rounded-tg text-xs font-semibold transition-all select-none',
                    isSelected
                      ? 'bg-tg-button text-tg-button-text shadow-sm scale-[1.03]'
                      : 'bg-tg-secondary-bg text-tg-text hover:bg-tg-hint/10'
                  )}
                >
                  {timeStr}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Итог и кнопка бронирования */}
      {selectedService && selectedSlot && (
        <Card variant="secondary" className="border border-tg-button/30 space-y-3">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-tg-hint">Итого</h4>
          <div className="space-y-1.5 text-xs">
            <div className="flex justify-between">
              <span className="text-tg-hint">Услуга:</span>
              <span className="font-semibold text-tg-text">{SERVICE_TYPE_LABELS[selectedService]}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-tg-hint">Дата:</span>
              <span className="font-semibold text-tg-text">
                {format(parseISO(selectedSlot), 'd MMMM yyyy (EEEE)', { locale: ru })}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-tg-hint">Время:</span>
              <span className="font-semibold text-tg-text">{format(parseISO(selectedSlot), 'HH:mm')}</span>
            </div>
            <div className="flex justify-between pt-1 border-t border-tg-hint/15">
              <span className="text-tg-hint">Ориентировочно:</span>
              <span className="font-bold text-tg-button">{SERVICE_PRICES_ESTIMATE[selectedService]}</span>
            </div>
          </div>

          <Button
            size="lg"
            variant="primary"
            onClick={handleConfirm}
            isLoading={createMutation.isPending}
            className="w-full mt-2"
          >
            Подтвердить запись
          </Button>
        </Card>
      )}
    </div>
  );
};
