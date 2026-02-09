import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Link } from 'react-router-dom';
import {
  AlertTriangle, Eye, Shield, CheckCircle, FileText,
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { Button, Badge } from '@/components/ui';
import { adminApi } from '@/api/admin';
import type { DisputeStatusType, DisputeResponse } from '@/types';

const statusTabs: { label: string; value: DisputeStatusType | 'ALL' }[] = [
  { label: 'Все', value: 'ALL' },
  { label: 'Открытые', value: 'OPEN' },
  { label: 'На рассмотрении', value: 'UNDER_REVIEW' },
  { label: 'Решённые', value: 'RESOLVED' },
];

const statusVariants: Record<DisputeStatusType, 'error' | 'warning' | 'success'> = {
  OPEN: 'error',
  UNDER_REVIEW: 'warning',
  RESOLVED: 'success',
};

const statusLabels: Record<DisputeStatusType, string> = {
  OPEN: 'Открыт',
  UNDER_REVIEW: 'На рассмотрении',
  RESOLVED: 'Решён',
};

export function AdminDisputesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<DisputeStatusType | 'ALL'>('ALL');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-disputes', page, statusFilter],
    queryFn: () =>
      adminApi.getDisputesList(page, 20, statusFilter === 'ALL' ? undefined : statusFilter),
  });

  const takeMutation = useMutation({
    mutationFn: (id: number) => adminApi.takeDisputeForReview(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-disputes'] });
    },
  });

  const handleTabChange = (value: DisputeStatusType | 'ALL') => {
    setStatusFilter(value);
    setPage(0);
  };

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Споры</h1>
            <p className="text-gray-500">Управление спорными заказами</p>
          </div>
          <Badge variant="error">
            {data?.totalElements || 0} споров
          </Badge>
        </div>

        {/* Status filter tabs */}
        <div className="flex gap-2 border-b border-gray-200 pb-1">
          {statusTabs.map((tab) => (
            <button
              key={tab.value}
              onClick={() => handleTabChange(tab.value)}
              className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-colors ${
                statusFilter === tab.value
                  ? 'bg-white text-primary-600 border border-b-white border-gray-200 -mb-px'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-32 bg-gray-200 rounded-xl animate-pulse" />
            ))}
          </div>
        ) : data?.content.length === 0 ? (
          <div className="bg-white rounded-xl shadow-sm p-12 text-center">
            <AlertTriangle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-gray-900">Нет споров</h2>
            <p className="text-gray-500 mt-2">
              {statusFilter === 'ALL' ? 'Споры ещё не были открыты' : 'Нет споров с таким статусом'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {data?.content.map((dispute: DisputeResponse) => (
              <div key={dispute.id} className="bg-white rounded-xl shadow-sm p-6">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <Badge variant={statusVariants[dispute.status]}>
                        {statusLabels[dispute.status]}
                      </Badge>
                      <span className="text-sm text-gray-500">#{dispute.id}</span>
                      {dispute.evidenceCount > 0 && (
                        <span className="inline-flex items-center gap-1 text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                          <FileText className="w-3 h-3" />
                          {dispute.evidenceCount} док.
                        </span>
                      )}
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-1">{dispute.orderTitle}</h3>
                    <p className="text-gray-600 line-clamp-2 mb-4">{dispute.reason}</p>

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-gray-500">Инициатор:</span>
                        <p className="font-medium text-gray-900">
                          {dispute.openedByName}
                          <span className="text-gray-500 font-normal">
                            {' '}({dispute.openedByRole === 'CLIENT' ? 'Заказчик' : 'Исполнитель'})
                          </span>
                        </p>
                      </div>
                      <div>
                        <span className="text-gray-500">Заказчик:</span>
                        <p className="font-medium text-gray-900">{dispute.clientName}</p>
                      </div>
                      <div>
                        <span className="text-gray-500">Исполнитель:</span>
                        <p className="font-medium text-gray-900">{dispute.executorName || '-'}</p>
                      </div>
                      <div>
                        <span className="text-gray-500">Открыт:</span>
                        <p className="font-medium text-gray-900">
                          {format(new Date(dispute.createdAt), 'd MMM yyyy', { locale: ru })}
                        </p>
                      </div>
                    </div>

                    {dispute.status === 'UNDER_REVIEW' && dispute.adminName && (
                      <div className="flex items-center gap-1 mt-3 text-sm text-yellow-600">
                        <Shield className="w-4 h-4" />
                        Рассматривает: {dispute.adminName}
                      </div>
                    )}

                    {dispute.status === 'RESOLVED' && dispute.resolution && (
                      <div className="flex items-center gap-1 mt-3 text-sm text-green-600">
                        <CheckCircle className="w-4 h-4" />
                        {dispute.resolution === 'FAVOR_CLIENT'
                          ? 'Решено в пользу заказчика'
                          : 'Решено в пользу исполнителя'}
                        {dispute.resolvedAt && (
                          <span className="text-gray-400 ml-2">
                            {format(new Date(dispute.resolvedAt), 'd MMM yyyy', { locale: ru })}
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-3 mt-4 pt-4 border-t border-gray-100">
                  <Link to={`/admin/disputes/${dispute.id}`}>
                    <Button variant="outline" size="sm">
                      <Eye className="w-4 h-4 mr-1" />
                      Подробнее
                    </Button>
                  </Link>
                  {dispute.status === 'OPEN' && (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => takeMutation.mutate(dispute.id)}
                      loading={takeMutation.isPending}
                    >
                      <Shield className="w-4 h-4 mr-1" />
                      Взять на рассмотрение
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-center gap-2">
            <Button
              variant="outline"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
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
    </AdminLayout>
  );
}
