import React from 'react';
import clsx from 'clsx';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'secondary' | 'interactive';
}

export const Card: React.FC<CardProps> = ({
  children,
  className,
  variant = 'secondary',
  onClick,
  ...props
}) => {
  const isInteractive = variant === 'interactive' || !!onClick;

  return (
    <div
      onClick={onClick}
      className={clsx(
        'rounded-tg-lg p-4 transition-all',
        variant === 'default' && 'bg-tg-bg border border-tg-hint/15',
        variant === 'secondary' && 'bg-tg-secondary-bg',
        variant === 'interactive' && 'bg-tg-secondary-bg active:scale-[0.99] cursor-pointer hover:brightness-95',
        isInteractive && 'select-none',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
};
