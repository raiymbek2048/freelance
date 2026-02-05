import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, MapPin, ChevronDown, ChevronUp, User, Clock, ChevronLeft, ChevronRight } from 'lucide-react';
import { Header } from '@/components/layout';
import { Button, Card } from '@/components/ui';
import { ordersApi } from '@/api/orders';
import { useAuthStore } from '@/stores/authStore';
import type { OrderFilters } from '@/types';

interface ExpandedOrder {
  id: number;
  responseText: string;
}

const cities = [
  'Все города',
  'Бишкек',
  'Ош',
  'Джалал-Абад',
  'Каракол',
  'Токмок',
  'Нарын',
  'Талас',
  'Баткен',
];

export function OrdersListPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuthStore();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [expandedOrder, setExpandedOrder] = useState<ExpandedOrder | null>(null);
  const [selectedCity, setSelectedCity] = useState('Все города');
  const [searchQuery, setSearchQuery] = useState(searchParams.get('search') || '');

  const filters: OrderFilters = {
    search: searchParams.get('search') || undefined,
  };

  const { data: ordersData, isLoading } = useQuery({
    queryKey: ['orders', filters, page],
    queryFn: () => ordersApi.getAll(filters, page, 10),
  });

  const respondMutation = useMutation({
    mutationFn: ({ orderId, coverLetter }: { orderId: number; coverLetter: string }) =>
      ordersApi.respond(orderId, { coverLetter }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setExpandedOrder(null);
    },
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (searchQuery.trim()) {
      params.set('search', searchQuery);
    }
    setSearchParams(params);
    setPage(0);
  };

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

  const orders = ordersData?.content || [];
  const totalPages = ordersData?.totalPages || 0;

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
      {/* Dark overlay for better readability */}
      <div
        className="absolute inset-0"
        style={{
          background: 'linear-gradient(to bottom, rgba(0,0,0,0.3) 0%, rgba(0,0,0,0.5) 100%)'
        }}
      />
      <div className="relative min-h-screen">
        <Header />
        <div className="max-w-3xl mx-auto px-4 py-6">
          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-2xl font-bold text-white">Все задания</h1>
            <Link to="/orders/create">
              <Button size="sm">Создать задание</Button>
            </Link>
          </div>

        {/* Search */}
        <form onSubmit={handleSearch} className="mb-4">
          <div className="flex gap-2">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Поиск заданий..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-cyan-500"
              />
            </div>
            <Button type="submit">Найти</Button>
          </div>
        </form>

        {/* City Filter */}
        <div className="mb-6 flex items-center gap-2 overflow-x-auto pb-2">
          <MapPin className="w-4 h-4 text-gray-400 flex-shrink-0" />
          {cities.map((city) => (
            <button
              key={city}
              onClick={() => setSelectedCity(city)}
              className={`px-3 py-1.5 rounded-full text-sm whitespace-nowrap transition-colors ${
                selectedCity === city
                  ? 'bg-cyan-500 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {city}
            </button>
          ))}
        </div>

        {/* Results count */}
        <p className="text-sm text-white/80 mb-4">
          Найдено {ordersData?.totalElements || 0} заданий
        </p>

        {/* Orders List */}
        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <Card key={i} padding="md" className="animate-pulse">
                <div className="h-4 bg-gray-200 rounded w-1/4 mb-3" />
                <div className="h-5 bg-gray-200 rounded w-3/4 mb-2" />
                <div className="h-4 bg-gray-200 rounded w-1/2" />
              </Card>
            ))}
          </div>
        ) : orders.length === 0 ? (
          <Card padding="lg" className="text-center">
            <p className="text-gray-500">Задания не найдены</p>
          </Card>
        ) : (
          <div className="space-y-3">
            {orders.map((order) => {
              const isExpanded = expandedOrder?.id === order.id;

              return (
                <div
                  key={order.id}
                  className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden"
                >
                  {/* Order Header */}
                  <div
                    onClick={() => handleOrderClick(order.id)}
                    className="p-4 cursor-pointer hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs text-gray-400">{order.clientName}</span>
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
                    <h3 className="font-bold text-gray-900 mb-1 break-words line-clamp-2">{order.title}</h3>
                    {/* Description preview */}
                    {order.description && (
                      <p className="text-sm text-gray-600 mb-2 line-clamp-2 break-words">
                        {order.description}
                      </p>
                    )}
                    <div className="flex items-center gap-3 text-xs text-gray-400">
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
                          <p className="text-sm font-medium text-gray-800">{order.clientName}</p>
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
                          className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-200 rounded-lg"
                        >
                          Отказаться
                        </button>
                        <button
                          onClick={() => handleRespond(order.id)}
                          disabled={!expandedOrder?.responseText.trim() || respondMutation.isPending}
                          className="px-4 py-2 text-sm bg-cyan-500 text-white rounded-lg hover:bg-cyan-600 disabled:opacity-50"
                        >
                          {respondMutation.isPending ? 'Отправка...' : 'Согласиться'}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="mt-6 flex items-center justify-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="p-2 rounded-lg border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-1">
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                let pageNum;
                if (totalPages <= 5) {
                  pageNum = i;
                } else if (page < 3) {
                  pageNum = i;
                } else if (page > totalPages - 4) {
                  pageNum = totalPages - 5 + i;
                } else {
                  pageNum = page - 2 + i;
                }

                return (
                  <button
                    key={pageNum}
                    onClick={() => setPage(pageNum)}
                    className={`w-10 h-10 rounded-lg text-sm font-medium ${
                      page === pageNum
                        ? 'bg-cyan-500 text-white'
                        : 'border border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    {pageNum + 1}
                  </button>
                );
              })}
            </div>

            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="p-2 rounded-lg border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        )}
        </div>
      </div>
    </div>
  );
}
