import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { Menu, X, User, LogOut, Settings, Briefcase, MessageSquare, Shield, CheckCircle } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';

export function Header() {
  const { user, isAuthenticated, logout } = useAuthStore();
  const { totalUnreadCount, connected, connect, fetchChatRooms } = useChatStore();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);

  // Connect to WebSocket and fetch chat rooms when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      if (!connected) {
        connect();
      }
      fetchChatRooms();
    }
  }, [isAuthenticated]);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <header className="bg-cyan-500 relative z-10">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex items-center justify-between h-14">
          {/* Logo */}
          <Link to="/" className="text-white font-bold text-xl">
            FREELANCE KG
          </Link>

          {/* Desktop Navigation - Center */}
          <nav className="hidden md:flex items-center gap-2">
            <Link
              to="/orders"
              className="px-4 py-2 bg-white/90 text-gray-700 rounded-lg text-sm font-medium hover:bg-white transition-colors"
            >
              Задания
            </Link>
            <Link
              to="/executors"
              className="px-4 py-2 bg-white/90 text-gray-700 rounded-lg text-sm font-medium hover:bg-white transition-colors"
            >
              Исполнители
            </Link>
            <Link
              to="/vacancies"
              className="px-4 py-2 bg-white/90 text-gray-700 rounded-lg text-sm font-medium hover:bg-white transition-colors"
            >
              Вакансия
            </Link>
            <Link
              to="/ads"
              className="px-4 py-2 bg-white/90 text-gray-700 rounded-lg text-sm font-medium hover:bg-white transition-colors"
            >
              Объявление
            </Link>
          </nav>

          {/* Desktop Auth */}
          <div className="hidden md:flex items-center gap-3">
            <Link
              to="/orders/create"
              className="px-4 py-2 bg-cyan-600 text-white rounded-lg text-sm font-medium hover:bg-cyan-700 transition-colors"
            >
              Дать задание
            </Link>
            {isAuthenticated ? (
              <div className="flex items-center gap-2">
                {/* Chat button */}
                <Link to="/chats" className="p-2 text-white/90 hover:text-white relative">
                  <MessageSquare className="w-5 h-5" />
                  {totalUnreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold min-w-[18px] h-[18px] flex items-center justify-center rounded-full px-1">
                      {totalUnreadCount > 99 ? '99+' : totalUnreadCount}
                    </span>
                  )}
                </Link>

                {/* Profile dropdown */}
                <div className="relative">
                  <button
                    onClick={() => setUserMenuOpen(!userMenuOpen)}
                    className="flex items-center gap-2 px-4 py-2 bg-white text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-100 transition-colors"
                  >
                    <User className="w-4 h-4" />
                    <span>{user?.fullName || 'Профиль'}</span>
                  </button>
                  {userMenuOpen && (
                    <>
                      <div
                        className="fixed inset-0 z-10"
                        onClick={() => setUserMenuOpen(false)}
                      />
                      <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-20">
                        <div className="px-4 py-2 border-b border-gray-100">
                          <p className="font-medium text-gray-900">{user?.fullName}</p>
                          <p className="text-sm text-gray-500">{user?.email}</p>
                        </div>
                        <Link
                          to="/profile"
                          className="flex items-center gap-2 px-4 py-2 text-gray-700 hover:bg-gray-50"
                          onClick={() => setUserMenuOpen(false)}
                        >
                          <User className="w-4 h-4" />
                          Профиль
                        </Link>
                        <Link
                          to="/my-orders"
                          className="flex items-center gap-2 px-4 py-2 text-gray-700 hover:bg-gray-50"
                          onClick={() => setUserMenuOpen(false)}
                        >
                          <Briefcase className="w-4 h-4" />
                          Мои задания
                        </Link>
                        <Link
                          to="/verification"
                          className="flex items-center gap-2 px-4 py-2 text-gray-700 hover:bg-gray-50"
                          onClick={() => setUserMenuOpen(false)}
                        >
                          <Shield className="w-4 h-4" />
                          Верификация
                          {user?.executorVerified ? (
                            <CheckCircle className="w-4 h-4 text-green-500 ml-auto" />
                          ) : (
                            <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded">
                              Не пройдена
                            </span>
                          )}
                        </Link>
                        <Link
                          to="/profile"
                          className="flex items-center gap-2 px-4 py-2 text-gray-700 hover:bg-gray-50"
                          onClick={() => setUserMenuOpen(false)}
                        >
                          <Settings className="w-4 h-4" />
                          Настройки
                        </Link>
                        {user?.role === 'ADMIN' && (
                          <Link
                            to="/admin"
                            className="flex items-center gap-2 px-4 py-2 text-cyan-600 hover:bg-gray-50"
                            onClick={() => setUserMenuOpen(false)}
                          >
                            <Shield className="w-4 h-4" />
                            Админ-панель
                          </Link>
                        )}
                        <hr className="my-1" />
                        <button
                          onClick={() => {
                            setUserMenuOpen(false);
                            handleLogout();
                          }}
                          className="flex items-center gap-2 px-4 py-2 text-red-600 hover:bg-gray-50 w-full"
                        >
                          <LogOut className="w-4 h-4" />
                          Выйти
                        </button>
                      </div>
                    </>
                  )}
                </div>
              </div>
            ) : (
              <Link
                to="/login"
                className="flex items-center gap-2 px-4 py-2 bg-white text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-100 transition-colors"
              >
                <User className="w-4 h-4" />
                Войти
              </Link>
            )}
          </div>

          {/* Mobile: Дать задание button + menu button */}
          <div className="md:hidden flex items-center gap-2">
            <Link
              to="/orders/create"
              className="px-3 py-1.5 bg-white text-cyan-600 rounded-lg text-sm font-medium"
            >
              Дать задание
            </Link>
            <button
              className="p-2 text-white"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu - Sidebar */}
      {mobileMenuOpen && (
        <>
          {/* Backdrop */}
          <div
            className="md:hidden fixed inset-0 bg-black/60 backdrop-blur-sm z-40"
            onClick={() => setMobileMenuOpen(false)}
          />
          {/* Sidebar */}
          <div className="md:hidden fixed top-0 right-0 h-full w-4/5 max-w-72 bg-white shadow-xl z-50 overflow-y-auto">
            {/* Close button */}
            <button
              className="absolute top-4 right-4 p-2 text-gray-500 hover:text-gray-700"
              onClick={() => setMobileMenuOpen(false)}
            >
              <X className="w-6 h-6" />
            </button>

            {/* User info */}
            {isAuthenticated && user ? (
              <div className="pt-6 pb-4 px-6 border-b border-gray-200">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 bg-gray-200 rounded-full flex items-center justify-center">
                    {user.avatarUrl ? (
                      <img src={user.avatarUrl} alt="" className="w-12 h-12 rounded-full object-cover" />
                    ) : (
                      <User className="w-6 h-6 text-gray-500" />
                    )}
                  </div>
                  <div>
                    <p className="font-semibold text-gray-900">{user.fullName}</p>
                    {user.executorVerified && (
                      <span className="text-xs text-green-600 flex items-center gap-1">
                        <CheckCircle className="w-3 h-3" /> Верифицирован
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="pt-12 pb-4 px-6 border-b border-gray-200">
                <Link
                  to="/login"
                  className="block w-full py-2.5 bg-cyan-500 text-white text-center rounded-lg font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Войти
                </Link>
              </div>
            )}

            {/* Menu items */}
            <nav className="py-4 px-4">
              {isAuthenticated && (
                <>
                  <Link
                    to="/profile"
                    className="block py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Профиль
                  </Link>
                  <Link
                    to="/verification"
                    className="flex items-center justify-between py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Верификация
                    {!user?.executorVerified && (
                      <span className="text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded">
                        Не пройдена
                      </span>
                    )}
                  </Link>
                </>
              )}
              <Link
                to="/vacancies"
                className="block py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                onClick={() => setMobileMenuOpen(false)}
              >
                Вакансия
              </Link>
              <Link
                to="/ads"
                className="block py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                onClick={() => setMobileMenuOpen(false)}
              >
                Объявление
              </Link>
              <Link
                to="/orders/create"
                className="block py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                onClick={() => setMobileMenuOpen(false)}
              >
                Дать задание
              </Link>
              {isAuthenticated && (
                <Link
                  to="/chats"
                  className="flex items-center justify-between py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Сообщения
                  {totalUnreadCount > 0 && (
                    <span className="bg-red-500 text-white text-xs font-bold min-w-[20px] h-[20px] flex items-center justify-center rounded-full px-1">
                      {totalUnreadCount > 99 ? '99+' : totalUnreadCount}
                    </span>
                  )}
                </Link>
              )}
              <Link
                to="/orders"
                className="block py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                onClick={() => setMobileMenuOpen(false)}
              >
                Задания
              </Link>
              {isAuthenticated && (
                <Link
                  to="/my-orders"
                  className="block py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Мои задания
                </Link>
              )}
              <Link
                to="/executors"
                className="block py-3 px-2 text-gray-700 hover:bg-gray-50 rounded-lg"
                onClick={() => setMobileMenuOpen(false)}
              >
                Исполнители
              </Link>
              {isAuthenticated && user?.role === 'ADMIN' && (
                <Link
                  to="/admin"
                  className="block py-3 px-2 text-cyan-600 hover:bg-gray-50 rounded-lg"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Админ-панель
                </Link>
              )}
              {isAuthenticated && (
                <>
                  <hr className="my-3" />
                  <button
                    onClick={() => {
                      setMobileMenuOpen(false);
                      handleLogout();
                    }}
                    className="block w-full py-3 px-2 text-red-600 hover:bg-gray-50 rounded-lg text-left"
                  >
                    Выйти
                  </button>
                </>
              )}
            </nav>
          </div>
        </>
      )}
    </header>
  );
}
