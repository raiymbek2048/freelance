import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  Briefcase,
  Clock,
  Users,
  MessageSquare,
  UserCheck,
  ChevronDown,
  ChevronUp,
  Star,
  Calendar,
  Wallet,
  Eye,
  ClipboardList,
  Hammer,
} from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Avatar, Badge } from '@/components/ui';
import { ordersApi } from '@/api/orders';
import { chatApi } from '@/api/chat';
import type { OrderListItem, OrderResponse } from '@/types';

const statusLabels: Record<string, string> = {
  NEW: 'Новый',
  IN_PROGRESS: 'В работе',
  REVISION: 'На доработке',
  ON_REVIEW: 'На проверке',
  COMPLETED: 'Завершён',
  DISPUTED: 'Спор',
  CANCELLED: 'Отменён',
};

const statusColors: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
  NEW: 'info',
  IN_PROGRESS: 'warning',
  REVISION: 'warning',
  ON_REVIEW: 'warning',
  COMPLETED: 'success',
  DISPUTED: 'error',
  CANCELLED: 'default',
};

// Card for orders as client (with responses)
function ClientOrderCard({ order }: { order: OrderListItem }) {
  const [expanded, setExpanded] = useState(false);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: responses, isLoading: responsesLoading } = useQuery({
    queryKey: ['order-responses', order.id],
    queryFn: () => ordersApi.getResponses(order.id),
    enabled: expanded,
  });

  const selectExecutorMutation = useMutation({
    mutationFn: ({ responseId }: { responseId: number }) =>
      ordersApi.selectExecutor(order.id, responseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-orders-client'] });
      queryClient.invalidateQueries({ queryKey: ['order-responses', order.id] });
    },
  });

  const startChatMutation = useMutation({
    mutationFn: (executorId: number) => chatApi.getOrCreateChat(order.id, executorId),
    onSuccess: (chatRoom) => {
      navigate(`/chats?room=${chatRoom.id}`);
    },
  });

  const formatBudget = () => {
    if (order.budgetMin && order.budgetMax) {
      return `${order.budgetMin.toLocaleString()} - ${order.budgetMax.toLocaleString()} сом`;
    }
    if (order.budgetMin) {
      return `от ${order.budgetMin.toLocaleString()} сом`;
    }
    if (order.budgetMax) {
      return `${order.budgetMax.toLocaleString()} сом`;
    }
    return 'Договорная';
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
      <div className="p-4 sm:p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-2">
              <Badge variant={statusColors[order.status]}>
                {statusLabels[order.status]}
              </Badge>
              <span className="text-sm text-gray-500">{order.categoryName}</span>
            </div>
            <h3
              className="text-lg font-semibold text-gray-900 hover:text-primary-600 cursor-pointer truncate"
              onClick={() => navigate(`/orders/${order.id}`)}
            >
              {order.title}
            </h3>
            <div className="mt-2 flex flex-wrap items-center gap-4 text-sm text-gray-500">
              <span className="flex items-center gap-1 text-green-600 font-semibold text-base">
                <Wallet className="w-4 h-4" />
                {formatBudget()}
              </span>
              {order.deadline && (
                <span className="flex items-center gap-1">
                  <Calendar className="w-4 h-4" />
                  до {format(new Date(order.deadline), 'd MMM yyyy', { locale: ru })}
                </span>
              )}
              <span className="flex items-center gap-1">
                <Clock className="w-4 h-4" />
                {format(new Date(order.createdAt), 'd MMM yyyy', { locale: ru })}
              </span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate(`/orders/${order.id}`)}
            >
              <Eye className="w-4 h-4 mr-1" />
              Подробнее
            </Button>
          </div>
        </div>

        {order.status === 'NEW' && order.responseCount > 0 && (
          <button
            onClick={() => setExpanded(!expanded)}
            className="mt-4 flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium"
          >
            <Users className="w-4 h-4" />
            <span>
              {order.responseCount} {order.responseCount === 1 ? 'отклик' : 'откликов'}
            </span>
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        )}
      </div>

      {expanded && (
        <div className="border-t border-gray-200 bg-gray-50">
          {responsesLoading ? (
            <div className="p-4 text-center text-gray-500">
              <div className="animate-spin w-6 h-6 border-2 border-primary-600 border-t-transparent rounded-full mx-auto" />
              <p className="mt-2">Загрузка откликов...</p>
            </div>
          ) : responses && responses.length > 0 ? (
            <div className="divide-y divide-gray-200">
              {responses.map((response: OrderResponse) => (
                <div key={response.id} className="p-4 sm:p-6">
                  <div className="flex flex-col sm:flex-row gap-4">
                    <div className="flex items-start gap-3 flex-1">
                      <div
                        className="cursor-pointer"
                        onClick={() => navigate(`/executors/${response.executorId}`)}
                      >
                        <Avatar
                          src={response.executorAvatarUrl}
                          name={response.executorName}
                          size="md"
                        />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <h4
                            className="font-medium text-gray-900 hover:text-primary-600 cursor-pointer"
                            onClick={() => navigate(`/executors/${response.executorId}`)}
                          >
                            {response.executorName}
                          </h4>
                          {response.isSelected && <Badge variant="success">Выбран</Badge>}
                        </div>
                        {response.executorSpecialization && (
                          <p className="text-sm text-gray-500">{response.executorSpecialization}</p>
                        )}
                        <div className="flex items-center gap-4 mt-1 text-sm text-gray-500">
                          <span className="flex items-center gap-1">
                            <Star className="w-4 h-4 text-yellow-400" />
                            {response.executorRating.toFixed(1)}
                          </span>
                          <span className="flex items-center gap-1">
                            <Briefcase className="w-4 h-4" />
                            {response.executorCompletedOrders} заказов
                          </span>
                        </div>
                        <p className="mt-2 text-gray-700">{response.coverLetter}</p>
                        <div className="mt-2 flex flex-wrap gap-4 text-sm">
                          {response.proposedPrice && (
                            <span className="text-primary-600 font-medium">
                              {response.proposedPrice.toLocaleString()} сом
                            </span>
                          )}
                          {response.proposedDays && (
                            <span className="text-gray-500">{response.proposedDays} дней</span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="flex sm:flex-col gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => startChatMutation.mutate(response.executorId)}
                        loading={startChatMutation.isPending}
                      >
                        <MessageSquare className="w-4 h-4 mr-1" />
                        Написать
                      </Button>
                      {!response.isSelected && order.status === 'NEW' && (
                        <Button
                          size="sm"
                          onClick={() => selectExecutorMutation.mutate({ responseId: response.id })}
                          loading={selectExecutorMutation.isPending}
                        >
                          <UserCheck className="w-4 h-4 mr-1" />
                          Выбрать
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="p-4 text-center text-gray-500">Пока нет откликов на этот заказ</div>
          )}
        </div>
      )}
    </div>
  );
}

// Simple card for orders as executor
function ExecutorOrderCard({ order }: { order: OrderListItem }) {
  const navigate = useNavigate();

  const formatBudget = () => {
    if (order.budgetMin && order.budgetMax) {
      return `${order.budgetMin.toLocaleString()} - ${order.budgetMax.toLocaleString()} сом`;
    }
    if (order.budgetMin) {
      return `от ${order.budgetMin.toLocaleString()} сом`;
    }
    if (order.budgetMax) {
      return `${order.budgetMax.toLocaleString()} сом`;
    }
    return 'Договорная';
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 sm:p-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            <Badge variant={statusColors[order.status]}>{statusLabels[order.status]}</Badge>
            <span className="text-sm text-gray-500">{order.categoryName}</span>
          </div>
          <h3
            className="text-lg font-semibold text-gray-900 hover:text-primary-600 cursor-pointer truncate"
            onClick={() => navigate(`/orders/${order.id}`)}
          >
            {order.title}
          </h3>
          <p className="text-sm text-gray-500 mt-1">Заказчик: {order.clientName}</p>
          <div className="mt-2 flex flex-wrap items-center gap-4 text-sm text-gray-500">
            <span className="flex items-center gap-1 text-green-600 font-semibold text-base">
              <Wallet className="w-4 h-4" />
              {formatBudget()}
            </span>
            {order.deadline && (
              <span className="flex items-center gap-1">
                <Calendar className="w-4 h-4" />
                до {format(new Date(order.deadline), 'd MMM yyyy', { locale: ru })}
              </span>
            )}
            <span className="flex items-center gap-1">
              <Clock className="w-4 h-4" />
              {format(new Date(order.createdAt), 'd MMM yyyy', { locale: ru })}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={() => navigate(`/orders/${order.id}`)}>
            <Eye className="w-4 h-4 mr-1" />
            Подробнее
          </Button>
        </div>
      </div>
    </div>
  );
}

type TabType = 'client' | 'executor';

export function MyOrdersPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<TabType>('client');
  const [clientPage, setClientPage] = useState(0);
  const [executorPage, setExecutorPage] = useState(0);

  const clientOrders = useQuery({
    queryKey: ['my-orders-client', clientPage],
    queryFn: () => ordersApi.getMyOrdersAsClient(clientPage, 10),
    enabled: activeTab === 'client',
  });

  const executorOrders = useQuery({
    queryKey: ['my-orders-executor', executorPage],
    queryFn: () => ordersApi.getMyOrdersAsExecutor(executorPage, 10),
    enabled: activeTab === 'executor',
  });

  const tabs = [
    { id: 'client' as TabType, label: 'Я заказчик', icon: ClipboardList },
    { id: 'executor' as TabType, label: 'Я исполнитель', icon: Hammer },
  ];

  return (
    <Layout>
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Мои заказы</h1>
          </div>
          <Button onClick={() => navigate('/orders/create')}>Создать заказ</Button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-gray-200 mb-6">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-4 py-3 border-b-2 font-medium transition-colors ${
                  activeTab === tab.id
                    ? 'border-primary-600 text-primary-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <Icon className="w-5 h-5" />
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Client Orders Tab */}
        {activeTab === 'client' && (
          <>
            {clientOrders.isLoading ? (
              <div className="flex justify-center py-12">
                <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full" />
              </div>
            ) : clientOrders.isError ? (
              <div className="text-center py-12">
                <p className="text-red-500">Ошибка загрузки заказов</p>
              </div>
            ) : clientOrders.data && clientOrders.data.content.length > 0 ? (
              <>
                <div className="space-y-4">
                  {clientOrders.data.content.map((order) => (
                    <ClientOrderCard key={order.id} order={order} />
                  ))}
                </div>
                {clientOrders.data.totalPages > 1 && (
                  <div className="mt-6 flex justify-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={clientOrders.data.first}
                      onClick={() => setClientPage((p) => p - 1)}
                    >
                      Назад
                    </Button>
                    <span className="flex items-center px-4 text-sm text-gray-500">
                      Страница {clientOrders.data.number + 1} из {clientOrders.data.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={clientOrders.data.last}
                      onClick={() => setClientPage((p) => p + 1)}
                    >
                      Вперёд
                    </Button>
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
                <ClipboardList className="w-16 h-16 mx-auto text-gray-300 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">У вас нет созданных заказов</h3>
                <p className="text-gray-500 mb-4">Создайте заказ и найдите исполнителя</p>
                <Button onClick={() => navigate('/orders/create')}>Создать заказ</Button>
              </div>
            )}
          </>
        )}

        {/* Executor Orders Tab */}
        {activeTab === 'executor' && (
          <>
            {executorOrders.isLoading ? (
              <div className="flex justify-center py-12">
                <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full" />
              </div>
            ) : executorOrders.isError ? (
              <div className="text-center py-12">
                <p className="text-red-500">Ошибка загрузки заказов</p>
              </div>
            ) : executorOrders.data && executorOrders.data.content.length > 0 ? (
              <>
                <div className="space-y-4">
                  {executorOrders.data.content.map((order) => (
                    <ExecutorOrderCard key={order.id} order={order} />
                  ))}
                </div>
                {executorOrders.data.totalPages > 1 && (
                  <div className="mt-6 flex justify-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={executorOrders.data.first}
                      onClick={() => setExecutorPage((p) => p - 1)}
                    >
                      Назад
                    </Button>
                    <span className="flex items-center px-4 text-sm text-gray-500">
                      Страница {executorOrders.data.number + 1} из {executorOrders.data.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={executorOrders.data.last}
                      onClick={() => setExecutorPage((p) => p + 1)}
                    >
                      Вперёд
                    </Button>
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
                <Hammer className="w-16 h-16 mx-auto text-gray-300 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">Вы пока не взяли ни одного заказа</h3>
                <p className="text-gray-500 mb-4">Найдите заказы и откликнитесь на них</p>
                <Button onClick={() => navigate('/orders')}>Найти заказы</Button>
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  );
}
