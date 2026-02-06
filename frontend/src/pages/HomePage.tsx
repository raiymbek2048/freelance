import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ChevronDown, ChevronUp, User, Clock, Menu, X } from 'lucide-react';
import { ordersApi } from '@/api/orders';
import { useAuthStore } from '@/stores/authStore';

type FilterTab = 'open' | 'history' | 'my-ads';

interface ExpandedOrder {
  id: number;
  responseText: string;
}

export function HomePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, user } = useAuthStore();
  const [activeTab, setActiveTab] = useState<FilterTab>('open');
  const [expandedOrder, setExpandedOrder] = useState<ExpandedOrder | null>(null);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const { data: ordersData } = useQuery({
    queryKey: ['orders', 'home', activeTab],
    queryFn: () => ordersApi.getAll({}, 0, 10),
  });

  const respondMutation = useMutation({
    mutationFn: ({ orderId, coverLetter }: { orderId: number; coverLetter: string }) =>
      ordersApi.respond(orderId, { coverLetter }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setExpandedOrder(null);
    },
  });

  const orders = ordersData?.content || [];

  const tabs: { key: FilterTab; label: string }[] = [
    { key: 'open', label: 'Открытые задания' },
    { key: 'history', label: 'Моя история' },
    { key: 'my-ads', label: 'Мои объявления' },
  ];

  const isVerified = user?.executorVerified === true;

  const handleOrderClick = (orderId: number) => {
    // Если не авторизован - редирект на логин
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    // Если не верифицирован - редирект на верификацию
    if (!isVerified) {
      navigate('/verification');
      return;
    }

    if (expandedOrder?.id === orderId) {
      setExpandedOrder(null);
    } else {
      setExpandedOrder({ id: orderId, responseText: '' });
    }
  };

  const handleRespond = (orderId: number) => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!isVerified) {
      navigate('/verification');
      return;
    }
    if (!expandedOrder?.responseText.trim()) return;

    respondMutation.mutate({
      orderId,
      coverLetter: expandedOrder.responseText,
    });
  };

  return (
    <div
      className="min-h-screen relative"
      style={{
        backgroundImage: 'url(/bishkek-bg.png)',
        backgroundSize: 'cover',
        backgroundPosition: 'center bottom',
        backgroundRepeat: 'no-repeat',
        backgroundAttachment: 'fixed',
      }}
    >
      {/* Header - Like Figma */}
      <header className="bg-cyan-500">
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

            {/* Right side - Buttons */}
            <div className="hidden md:flex items-center gap-3">
              <Link
                to="/orders/create"
                className="px-4 py-2 bg-cyan-600 text-white rounded-lg text-sm font-medium hover:bg-cyan-700 transition-colors"
              >
                Дать задание
              </Link>
              {isAuthenticated ? (
                <Link
                  to="/profile"
                  className="flex items-center gap-2 px-4 py-2 bg-white text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-100 transition-colors"
                >
                  <User className="w-4 h-4" />
                  {user?.fullName || 'Профиль'}
                </Link>
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

            {/* Mobile menu button */}
            <button
              className="md:hidden p-2 text-white"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-white/20 px-4 py-3 space-y-2">
            <Link to="/orders" className="block text-white py-2" onClick={() => setMobileMenuOpen(false)}>
              Задания
            </Link>
            <Link to="/executors" className="block text-white py-2" onClick={() => setMobileMenuOpen(false)}>
              Исполнители
            </Link>
            <Link to="/vacancies" className="block text-white py-2" onClick={() => setMobileMenuOpen(false)}>
              Вакансия
            </Link>
            <Link to="/ads" className="block text-white py-2" onClick={() => setMobileMenuOpen(false)}>
              Объявление
            </Link>
            <Link to="/orders/create" className="block text-white py-2" onClick={() => setMobileMenuOpen(false)}>
              Дать задание
            </Link>
            <Link to="/login" className="block text-white py-2" onClick={() => setMobileMenuOpen(false)}>
              Войти
            </Link>
          </div>
        )}
      </header>

      {/* Hero Section - Separate cyan card */}
      <div className="max-w-3xl mx-auto px-4 pt-6">
        <div className="bg-cyan-500 rounded-2xl p-6 md:p-8">
          <div className="flex items-center justify-between gap-6">
            <div className="flex-1">
              <h1 className="text-2xl md:text-3xl font-bold text-white mb-2">
                Исполнители для<br />любых заданий
              </h1>
              <p className="text-white/80 text-sm max-w-md">
                Тысячи исполнителей готовы выполнить любое ваше задание
              </p>
            </div>
            {/* Right side branding */}
            <div className="hidden md:block">
              <span className="text-white/70 text-lg font-medium">FREELANCE KG</span>
            </div>
          </div>
        </div>
      </div>

      {/* Cards Container - Light frosted glass effect */}
      <div className="max-w-3xl mx-auto px-4 pb-8 mt-4 relative z-10">
        <div
          className="rounded-2xl p-6 pt-8"
          style={{
            backgroundColor: 'rgba(200, 220, 240, 0.5)',
            backdropFilter: 'blur(10px)',
          }}
        >
          {/* Filter Tabs - Inside container */}
          <div className="flex items-center justify-center gap-2 mb-6">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-5 py-2.5 rounded-full text-sm font-medium transition-all duration-200 ${
                  activeTab === tab.key
                    ? 'bg-cyan-500 text-white shadow-md'
                    : 'bg-white text-gray-600 hover:bg-gray-50 hover:shadow-sm'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="space-y-3">
            {orders.length === 0 ? (
              <div className="bg-white rounded-lg p-6 text-center shadow-sm">
                <p className="text-gray-400 text-sm">Задачи не найдены</p>
              </div>
            ) : (
              orders.map((order) => {
                const isExpanded = expandedOrder?.id === order.id;

                return (
                  <div
                    key={order.id}
                    className="bg-white rounded-lg shadow-sm overflow-hidden"
                  >
                    {/* Order Header - Clickable */}
                    <div
                      onClick={() => handleOrderClick(order.id)}
                      className="p-4 cursor-pointer hover:bg-gray-50 transition-colors"
                    >
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-xs text-gray-400">
                          {order.clientName}
                        </span>
                        <div className="flex items-center gap-2">
                          {order.deadline && (
                            <span className="text-xs text-orange-500 flex items-center gap-1">
                              <Clock className="w-3 h-3" />
                              {new Date(order.deadline).toLocaleDateString('ru')}
                            </span>
                          )}
                          {order.budgetMax && (
                            <span className="text-xs text-green-600 font-medium">
                              до {order.budgetMax.toLocaleString()} сом
                            </span>
                          )}
                          {isExpanded ? (
                            <ChevronUp className="w-4 h-4 text-gray-400" />
                          ) : (
                            <ChevronDown className="w-4 h-4 text-gray-400" />
                          )}
                        </div>
                      </div>
                      {/* Title - Bold */}
                      <h3 className="text-sm font-bold text-gray-900 mb-1 break-words line-clamp-2">
                        {order.title}
                      </h3>
                      {/* Description preview */}
                      {order.description && (
                        <p className="text-sm text-gray-600 mb-2 line-clamp-2 break-words">
                          {order.description}
                        </p>
                      )}
                      {/* Response count */}
                      <div className="flex items-center gap-2 text-xs text-gray-400">
                        <span>{order.responseCount} откликов</span>
                        <span>•</span>
                        <span>{new Date(order.createdAt).toLocaleDateString('ru')}</span>
                      </div>
                    </div>

                    {/* Expanded Content with animation */}
                    <div
                      className={`overflow-hidden transition-all duration-300 ease-in-out ${
                        isExpanded ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'
                      }`}
                    >
                      <div className="border-t border-gray-100 p-4 bg-gray-50">
                        {/* Client Info */}
                        <div className="flex items-center gap-3 mb-4">
                          <div className="w-10 h-10 bg-cyan-100 rounded-full flex items-center justify-center">
                            <User className="w-5 h-5 text-cyan-600" />
                          </div>
                          <div>
                            <p className="text-sm font-medium text-gray-800">
                              {order.clientName}
                            </p>
                            <button className="text-xs text-cyan-600 hover:underline">
                              Посмотреть отзывы
                            </button>
                          </div>
                        </div>

                        {/* Response Form */}
                        <div className="mb-4">
                          <p className="text-xs text-gray-500 mb-2">
                            Поздоровайтесь с заказчиком и уточните подробности задания
                          </p>
                          <textarea
                            value={expandedOrder?.responseText || ''}
                            onChange={(e) =>
                              expandedOrder && setExpandedOrder({
                                ...expandedOrder,
                                responseText: e.target.value,
                              })
                            }
                            placeholder="Напишите сообщение заказчику..."
                            className="w-full p-3 border border-gray-200 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-cyan-500"
                            rows={3}
                          />
                        </div>

                        {/* Action Buttons */}
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => setExpandedOrder(null)}
                            className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-200 rounded-lg transition-colors"
                          >
                            Отказаться
                          </button>
                          <button
                            onClick={() => handleRespond(order.id)}
                            disabled={!expandedOrder?.responseText.trim() || respondMutation.isPending}
                            className="px-4 py-2 text-sm bg-cyan-500 text-white rounded-lg hover:bg-cyan-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                          >
                            {respondMutation.isPending ? 'Отправка...' : 'Согласиться'}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })
            )}

            {/* View All Button */}
            <div className="pt-2">
              <Link
                to="/orders"
                className="block w-full py-3 bg-cyan-500 text-white text-center text-sm font-medium rounded-lg hover:bg-cyan-600 transition-colors"
              >
                Смотреть все задачи
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
