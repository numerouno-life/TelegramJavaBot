import React from 'react';
import clsx from 'clsx';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'destructive';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
  haptic?: 'light' | 'medium' | 'heavy' | 'none';
}

export const Button: React.FC<ButtonProps> = ({
  children,
  className,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  loading = false,
  fullWidth = false,
  disabled = false,
  haptic = 'light',
  onClick,
  ...props
}) => {
  const isBusy = isLoading || loading;
  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled || isBusy) return;

    if (haptic !== 'none' && window.Telegram?.WebApp?.HapticFeedback) {
      try {
        window.Telegram.WebApp.HapticFeedback.impactOccurred(haptic);
      } catch {
        // Игнорируем если не поддерживается платформой
      }
    }

    onClick?.(e);
  };

  const baseStyles =
    'relative inline-flex items-center justify-center font-medium transition-all active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none select-none';

  const sizeStyles = {
    sm: 'text-xs px-3 py-1.5 rounded-lg gap-1.5',
    md: 'text-sm px-4 py-2.5 rounded-tg gap-2',
    lg: 'text-base px-5 py-3.5 rounded-tg-lg gap-2.5 font-semibold w-full',
  };

  const variantStyles = {
    primary: 'bg-tg-button text-tg-button-text shadow-sm hover:brightness-105 active:brightness-95',
    secondary: 'bg-tg-secondary-bg text-tg-text hover:brightness-95',
    outline: 'border border-tg-hint/30 text-tg-text hover:bg-tg-secondary-bg',
    destructive: 'bg-tg-destructive/10 text-tg-destructive hover:bg-tg-destructive/20',
  };

  return (
    <button
      className={clsx(
        baseStyles,
        sizeStyles[size],
        variantStyles[variant],
        fullWidth && 'w-full',
        className
      )}
      disabled={disabled || isBusy}
      onClick={handleClick}
      {...props}
    >
      {isBusy ? (
        <span className="inline-block w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
      ) : null}
      {children}
    </button>
  );
};
