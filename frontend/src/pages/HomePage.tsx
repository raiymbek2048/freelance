import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ChevronDown, ChevronUp, User, Clock } from 'lucide-react';
import { Header } from '@/components/layout';
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
  const { isAuthenticated } = useAuthStore();
  const [activeTab, setActiveTab] = useState<FilterTab>('open');
  const [expandedOrder, setExpandedOrder] = useState<ExpandedOrder | null>(null);

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

  const handleOrderClick = (orderId: number) => {
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

      {/* Header */}
      <Header />

      {/* Site Description Box */}
      <div className="px-4 pt-6 pb-4 relative z-10">
        <div className="max-w-lg mx-auto bg-white/90 backdrop-blur rounded-xl p-4 shadow-sm">
          <h1 className="text-lg font-bold text-cyan-600 text-center mb-2">
            Исполнители для любых заданий
          </h1>
          <p className="text-sm text-gray-600 text-center">
            Первая биржа фриланса в Кыргызстане. Найдите исполнителя для любой задачи:
            ремонт, доставка, IT, дизайн и многое другое.
          </p>
        </div>
      </div>

      {/* Semi-transparent container for content - like Figma */}
      <div className="px-4 pb-8 relative z-10">
        <div
          className="max-w-lg mx-auto rounded-2xl p-4"
          style={{ backgroundColor: 'rgba(128, 128, 128, 0.4)' }}
        >
          {/* Filter Tabs */}
          <div className="flex items-center justify-center gap-2 mb-4">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-3 py-1.5 rounded text-xs font-medium transition-colors ${
                  activeTab === tab.key
                    ? 'bg-cyan-500 text-white'
                    : 'bg-white/80 text-gray-700 hover:bg-white'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Orders List */}
          <div className="space-y-2">
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
            <div className="pt-4">
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
