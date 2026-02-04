import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  Briefcase,
  Eye,
  Trash2,
  ChevronDown,
  ExternalLink,
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { Button, Badge, Modal } from '@/components/ui';
import { adminApi, type AdminOrder } from '@/api/admin';
import type { OrderStatus } from '@/types';

const statusLabels: Record<OrderStatus, string> = {
  NEW: 'Новый',
  IN_PROGRESS: 'В работе',
  REVISION: 'На доработке',
  ON_REVIEW: 'На проверке',
  COMPLETED: 'Завершён',
  DISPUTED: 'Спор',
  CANCELLED: 'Отменён',
};

const statusVariants: Record<OrderStatus, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  NEW: 'info',
  IN_PROGRESS: 'warning',
  REVISION: 'warning',
  ON_REVIEW: 'default',
  COMPLETED: 'success',
  DISPUTED: 'error',
  CANCELLED: 'default',
};

export function AdminOrdersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selectedOrder, setSelectedOrder] = useState<AdminOrder | null>(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [orderToDelete, setOrderToDelete] = useState<AdminOrder | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-orders', page, statusFilter],
    queryFn: () =>
      adminApi.getOrders(page, 20, statusFilter === 'ALL' ? undefined : statusFilter),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminApi.deleteOrder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      setShowDeleteModal(false);
      setOrderToDelete(null);
    },
  });

  const openDeleteModal = (order: AdminOrder) => {
    setOrderToDelete(order);
    setShowDeleteModal(true);
  };

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Заказы</h1>
            <p className="text-gray-500">Управление заказами платформы</p>
          </div>
          <Badge variant="info">
            {data?.totalElements || 0} всего
          </Badge>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-xl shadow-sm p-4">
          <div className="flex flex-col md:flex-row gap-4 items-center justify-between">
            <div className="relative">
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
                className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2 pr-10 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              >
                <option value="ALL">Все статусы</option>
                <option value="NEW">Новые</option>
                <option value="IN_PROGRESS">В работе</option>
                <option value="REVISION">На доработке</option>
                <option value="ON_REVIEW">На проверке</option>
                <option value="COMPLETED">Завершённые</option>
                <option value="DISPUTED">Споры</option>
                <option value="CANCELLED">Отменённые</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" />
            </div>
          </div>
        </div>

        {/* Orders Table */}
        {isLoading ? (
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <div className="animate-pulse">
              {[...Array(10)].map((_, i) => (
                <div key={i} className="h-16 bg-gray-100 border-b border-gray-200" />
              ))}
            </div>
          </div>
        ) : data?.content.length === 0 ? (
          <div className="bg-white rounded-xl shadow-sm p-12 text-center">
            <Briefcase className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-gray-900">Заказы не найдены</h2>
            <p className="text-gray-500 mt-2">Измените параметры фильтра</p>
          </div>
        ) : (
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">ID</th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Заказ
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Заказчик
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Исполнитель
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Статус
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Бюджет
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Создан
                  </th>
                  <th className="text-right py-3 px-4 text-sm font-medium text-gray-500">
                    Действия
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data?.content.map((order) => (
                  <tr key={order.id} className="hover:bg-gray-50">
                    <td className="py-3 px-4 text-sm text-gray-500">#{order.id}</td>
                    <td className="py-3 px-4">
                      <div className="max-w-xs">
                        <p className="font-medium text-gray-900 truncate">{order.title}</p>
                        <p className="text-sm text-gray-500 truncate">{order.categoryName}</p>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <p className="text-sm text-gray-900">{order.clientName}</p>
                      <p className="text-xs text-gray-500">{order.clientEmail}</p>
                    </td>
                    <td className="py-3 px-4">
                      {order.executorName ? (
                        <>
                          <p className="text-sm text-gray-900">{order.executorName}</p>
                          <p className="text-xs text-gray-500">{order.executorEmail}</p>
                        </>
                      ) : (
                        <span className="text-sm text-gray-400">-</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <Badge variant={statusVariants[order.status as OrderStatus]} size="sm">
                        {statusLabels[order.status as OrderStatus] || order.status}
                      </Badge>
                    </td>
                    <td className="py-3 px-4 text-sm text-gray-900">
                      {order.agreedPrice
                        ? `${order.agreedPrice.toLocaleString()} сом`
                        : order.budgetMax
                          ? `до ${order.budgetMax.toLocaleString()} сом`
                          : '-'}
                    </td>
                    <td className="py-3 px-4 text-sm text-gray-500">
                      {format(new Date(order.createdAt), 'd MMM yyyy', { locale: ru })}
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setSelectedOrder(order)}
                        >
                          <Eye className="w-4 h-4" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => window.open(`/orders/${order.id}`, '_blank')}
                        >
                          <ExternalLink className="w-4 h-4" />
                        </Button>
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => openDeleteModal(order)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-center gap-2">
            <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Назад
            </Button>
            <span className="px-4 py-2 text-gray-600">
              {page + 1} / {data.totalPages}
            </span>
            <Button
              variant="outline"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Вперёд
            </Button>
          </div>
        )}
      </div>

      {/* Order Detail Modal */}
      <Modal
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        title="Детали заказа"
        size="lg"
      >
        {selectedOrder && (
          <div className="space-y-6">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-xl font-semibold text-gray-900">{selectedOrder.title}</h3>
                <p className="text-gray-500">{selectedOrder.categoryName}</p>
              </div>
              <Badge variant={statusVariants[selectedOrder.status as OrderStatus]}>
                {statusLabels[selectedOrder.status as OrderStatus] || selectedOrder.status}
              </Badge>
            </div>

            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="text-gray-700 whitespace-pre-wrap break-words">
                {selectedOrder.description}
              </p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 border border-gray-200 rounded-lg">
                <h4 className="text-sm font-medium text-gray-500 mb-2">Заказчик</h4>
                <p className="font-medium text-gray-900">{selectedOrder.clientName}</p>
                <p className="text-sm text-gray-500">{selectedOrder.clientEmail}</p>
              </div>
              <div className="p-4 border border-gray-200 rounded-lg">
                <h4 className="text-sm font-medium text-gray-500 mb-2">Исполнитель</h4>
                {selectedOrder.executorName ? (
                  <>
                    <p className="font-medium text-gray-900">{selectedOrder.executorName}</p>
                    <p className="text-sm text-gray-500">{selectedOrder.executorEmail}</p>
                  </>
                ) : (
                  <p className="text-gray-400">Не назначен</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4 text-sm">
              <div>
                <span className="text-gray-500">Бюджет:</span>
                <p className="font-medium text-gray-900">
                  {selectedOrder.budgetMin && selectedOrder.budgetMax
                    ? `${selectedOrder.budgetMin.toLocaleString()} - ${selectedOrder.budgetMax.toLocaleString()} сом`
                    : selectedOrder.budgetMax
                      ? `до ${selectedOrder.budgetMax.toLocaleString()} сом`
                      : '-'}
                </p>
              </div>
              <div>
                <span className="text-gray-500">Согласованная цена:</span>
                <p className="font-medium text-gray-900">
                  {selectedOrder.agreedPrice
                    ? `${selectedOrder.agreedPrice.toLocaleString()} сом`
                    : '-'}
                </p>
              </div>
              <div>
                <span className="text-gray-500">Просмотров:</span>
                <p className="font-medium text-gray-900">{selectedOrder.viewCount}</p>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4 text-sm">
              <div>
                <span className="text-gray-500">Откликов:</span>
                <p className="font-medium text-gray-900">{selectedOrder.responseCount}</p>
              </div>
              <div>
                <span className="text-gray-500">Создан:</span>
                <p className="font-medium text-gray-900">
                  {format(new Date(selectedOrder.createdAt), 'd MMM yyyy, HH:mm', { locale: ru })}
                </p>
              </div>
              {selectedOrder.completedAt && (
                <div>
                  <span className="text-gray-500">Завершён:</span>
                  <p className="font-medium text-gray-900">
                    {format(new Date(selectedOrder.completedAt), 'd MMM yyyy, HH:mm', {
                      locale: ru,
                    })}
                  </p>
                </div>
              )}
            </div>

            <div className="flex gap-3 pt-4 border-t border-gray-200">
              <Button
                variant="outline"
                onClick={() => window.open(`/orders/${selectedOrder.id}`, '_blank')}
              >
                <ExternalLink className="w-4 h-4 mr-2" />
                Открыть на сайте
              </Button>
              <Button variant="danger" onClick={() => openDeleteModal(selectedOrder)}>
                <Trash2 className="w-4 h-4 mr-2" />
                Удалить заказ
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title="Удалить заказ"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Вы уверены, что хотите удалить заказ "{orderToDelete?.title}"? Это действие нельзя
            отменить.
          </p>
          <div className="flex gap-3 justify-end">
            <Button variant="outline" onClick={() => setShowDeleteModal(false)}>
              Отмена
            </Button>
            <Button
              variant="danger"
              onClick={() => orderToDelete && deleteMutation.mutate(orderToDelete.id)}
              loading={deleteMutation.isPending}
            >
              Удалить
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
