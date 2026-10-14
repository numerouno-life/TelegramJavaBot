import React from 'react';
import { NavLink } from 'react-router-dom';
import { Home, Calendar, User, Shield } from 'lucide-react';
import { useAuthStore } from '../auth/useAuthStore';
import clsx from 'clsx';

export const BottomNav: React.FC = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';

  const triggerHaptic = () => {
    try {
      window.Telegram?.WebApp?.HapticFeedback?.impactOccurred('light');
    } catch {
      // Игнорируем если не поддерживается
    }
  };

  const navItems = [
    { to: '/', label: 'Главная', icon: Home },
    { to: '/appointments', label: 'Мои записи', icon: Calendar },
    { to: '/profile', label: 'Профиль', icon: User },
    ...(isAdmin ? [{ to: '/admin', label: 'Админ', icon: Shield }] : []),
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 bg-tg-bg/90 backdrop-blur-md border-t border-tg-hint/15 pb-[env(safe-area-inset-bottom)]">
      <div className="flex items-center justify-around h-14 max-w-md mx-auto px-2">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={triggerHaptic}
              className={({ isActive }) =>
                clsx(
                  'flex flex-col items-center justify-center flex-1 h-full py-1 text-[11px] font-medium transition-colors select-none',
                  isActive ? 'text-tg-button' : 'text-tg-hint hover:text-tg-text'
                )
              }
            >
              {({ isActive }) => (
                <>
                  <Icon size={20} strokeWidth={isActive ? 2.5 : 2} className="mb-0.5" />
                  <span>{item.label}</span>
                </>
              )}
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
};
