import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, Filter, Briefcase, Clock, MessageCircle } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card, Input, Select, Badge } from '@/components/ui';
import { ordersApi } from '@/api/orders';
import { categoriesApi } from '@/api/categories';
import type { OrderFilters, OrderStatus } from '@/types';

const statusLabels: Record<OrderStatus, string> = {
  NEW: 'Новый',
  IN_PROGRESS: 'В работе',
  REVISION: 'На доработке',
  ON_REVIEW: 'На проверке',
  COMPLETED: 'Завершён',
  DISPUTED: 'Спор',
  CANCELLED: 'Отменён',
};

const statusVariants: Record<OrderStatus, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
  NEW: 'info',
  IN_PROGRESS: 'warning',
  REVISION: 'warning',
  ON_REVIEW: 'info',
  COMPLETED: 'success',
  DISPUTED: 'error',
  CANCELLED: 'default',
};

export function OrdersListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);

  const filters: OrderFilters = {
    categoryId: searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : undefined,
    status: searchParams.get('status') as OrderStatus | undefined,
    search: searchParams.get('search') || undefined,
    minBudget: searchParams.get('minBudget') ? Number(searchParams.get('minBudget')) : undefined,
    maxBudget: searchParams.get('maxBudget') ? Number(searchParams.get('maxBudget')) : undefined,
  };

  const { data: ordersData, isLoading } = useQuery({
    queryKey: ['orders', filters, page],
    queryFn: () => ordersApi.getAll(filters, page, 20),
  });

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.getAll,
  });

  const updateFilter = (key: keyof OrderFilters, value: string | number | undefined) => {
    const params = new URLSearchParams(searchParams);
    if (value) {
      params.set(key, String(value));
    } else {
      params.delete(key);
    }
    setSearchParams(params);
    setPage(0);
  };

  const categoryOptions = [
    { value: '', label: 'Все категории' },
    ...(categories?.map((c) => ({ value: c.id, label: c.name })) || []),
  ];

  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Заказы</h1>
            <p className="text-gray-600 mt-1">
              {ordersData?.totalElements || 0} заказов найдено
            </p>
          </div>
          <Link to="/orders/create">
            <Button>Создать заказ</Button>
          </Link>
        </div>

        {/* Search & Filters */}
        <Card padding="md" className="mb-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                placeholder="Поиск заказов..."
                className="pl-10"
                value={filters.search || ''}
                onChange={(e) => updateFilter('search', e.target.value || undefined)}
              />
            </div>
            <Select
              options={categoryOptions}
              value={filters.categoryId || ''}
              onChange={(e) => updateFilter('categoryId', e.target.value || undefined)}
              className="w-full md:w-48"
            />
            <Button
              variant="outline"
              onClick={() => setShowFilters(!showFilters)}
              className="flex items-center gap-2"
            >
              <Filter className="w-4 h-4" />
              Фильтры
            </Button>
          </div>

          {showFilters && (
            <div className="mt-4 pt-4 border-t border-gray-200 grid grid-cols-1 md:grid-cols-3 gap-4">
              <Input
                label="Бюджет от"
                type="number"
                placeholder="0"
                value={filters.minBudget || ''}
                onChange={(e) => updateFilter('minBudget', e.target.value || undefined)}
              />
              <Input
                label="Бюджет до"
                type="number"
                placeholder="100000"
                value={filters.maxBudget || ''}
                onChange={(e) => updateFilter('maxBudget', e.target.value || undefined)}
              />
              <Select
                label="Статус"
                options={[
                  { value: '', label: 'Все статусы' },
                  { value: 'NEW', label: 'Новые' },
                  { value: 'IN_PROGRESS', label: 'В работе' },
                ]}
                value={filters.status || ''}
                onChange={(e) => updateFilter('status', e.target.value || undefined)}
              />
            </div>
          )}
        </Card>

        {/* Orders List */}
        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <Card key={i} padding="md" className="animate-pulse">
                <div className="h-6 bg-gray-200 rounded w-3/4 mb-4" />
                <div className="h-4 bg-gray-200 rounded w-full mb-2" />
                <div className="h-4 bg-gray-200 rounded w-2/3" />
              </Card>
            ))}
          </div>
        ) : ordersData?.content.length === 0 ? (
          <Card padding="lg" className="text-center">
            <Briefcase className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">Заказы не найдены</h3>
            <p className="text-gray-500">Попробуйте изменить параметры поиска</p>
          </Card>
        ) : (
          <div className="space-y-4">
            {ordersData?.content.map((order) => (
              <Link key={order.id} to={`/orders/${order.id}`}>
                <Card hover padding="md">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <Badge variant={statusVariants[order.status]} size="sm">
                        {statusLabels[order.status]}
                      </Badge>
                      <span className="text-sm text-gray-500">{order.categoryName}</span>
                    </div>
                    {(order.budgetMin || order.budgetMax) && (
                      <span className="font-semibold text-gray-900">
                        {order.budgetMin && order.budgetMax
                          ? `${order.budgetMin.toLocaleString()} - ${order.budgetMax.toLocaleString()} сом`
                          : order.budgetMax
                          ? `до ${order.budgetMax.toLocaleString()} сом`
                          : `от ${order.budgetMin?.toLocaleString()} сом`}
                      </span>
                    )}
                  </div>

                  <h3 className="text-lg font-semibold text-gray-900 mb-4">{order.title}</h3>

                  <div className="flex items-center gap-6 text-sm text-gray-500">
                    <span className="flex items-center gap-1">
                      <MessageCircle className="w-4 h-4" />
                      {order.responseCount} откликов
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-4 h-4" />
                      {new Date(order.createdAt).toLocaleDateString('ru', {
                        day: 'numeric',
                        month: 'short',
                      })}
                    </span>
                    {order.deadline && (
                      <span className="text-orange-600">
                        Срок: {new Date(order.deadline).toLocaleDateString('ru')}
                      </span>
                    )}
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}

        {/* Pagination */}
        {ordersData && ordersData.totalPages > 1 && (
          <div className="mt-8 flex items-center justify-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={ordersData.first}
              onClick={() => setPage((p) => p - 1)}
            >
              Назад
            </Button>
            <span className="px-4 text-sm text-gray-600">
              Страница {(ordersData.number ?? ordersData.page ?? 0) + 1} из {ordersData.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={ordersData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Далее
            </Button>
          </div>
        )}
      </div>
    </Layout>
  );
}
