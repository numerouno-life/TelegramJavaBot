import React from 'react';
import { Outlet } from 'react-router-dom';
import { BottomNav } from './BottomNav';

export const Layout: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col bg-tg-bg text-tg-text">
      {/* Контент страницы с отступом под нижний таб-бар */}
      <main className="flex-1 max-w-md w-full mx-auto pb-20 px-4 pt-4">
        <Outlet />
      </main>

      {/* Фиксированный нижний таб-бар */}
      <BottomNav />
    </div>
  );
};
