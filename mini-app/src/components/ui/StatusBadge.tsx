import React from 'react';
import clsx from 'clsx';
import { StatusAppointment, STATUS_LABELS } from '../../types/appointment';

interface StatusBadgeProps {
  status: StatusAppointment;
  className?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, className }) => {
  const styles: Record<StatusAppointment, string> = {
    ACTIVE: 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20',
    CONFIRMED: 'bg-blue-500/15 text-blue-600 dark:text-blue-400 border border-blue-500/20',
    PENDING: 'bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/20',
    CANCELED: 'bg-red-500/15 text-red-600 dark:text-red-400 border border-red-500/20',
  };

  return (
    <span
      className={clsx(
        'inline-flex items-center text-xs px-2.5 py-0.5 rounded-full font-medium',
        styles[status],
        className
      )}
    >
      {STATUS_LABELS[status] || status}
    </span>
  );
};
