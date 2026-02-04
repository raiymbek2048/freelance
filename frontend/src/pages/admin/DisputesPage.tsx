import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import { AlertTriangle, Eye, CheckCircle } from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { Button, Badge, Modal, Textarea } from '@/components/ui';
import { adminApi, type AdminOrder } from '@/api/admin';

export function AdminDisputesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [selectedDispute, setSelectedDispute] = useState<AdminOrder | null>(null);
  const [showResolveModal, setShowResolveModal] = useState(false);
  const [resolution, setResolution] = useState('');
  const [favorClient, setFavorClient] = useState(true);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-disputes', page],
    queryFn: () => adminApi.getDisputes(page, 20),
  });

  const resolveMutation = useMutation({
    mutationFn: () =>
      adminApi.resolveDispute(selectedDispute!.id, favorClient, resolution || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-disputes'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      setShowResolveModal(false);
      setSelectedDispute(null);
      setResolution('');
    },
  });

  const openResolveModal = (dispute: AdminOrder, favor: boolean) => {
    setSelectedDispute(dispute);
    setFavorClient(favor);
    setShowResolveModal(true);
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
            {data?.totalElements || 0} активных
          </Badge>
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
            <h2 className="text-xl font-semibold text-gray-900">Нет активных споров</h2>
            <p className="text-gray-500 mt-2">Все споры разрешены</p>
          </div>
        ) : (
          <div className="space-y-4">
            {data?.content.map((dispute) => (
              <div key={dispute.id} className="bg-white rounded-xl shadow-sm p-6">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <Badge variant="error">Спор</Badge>
                      <span className="text-sm text-gray-500">#{dispute.id}</span>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">{dispute.title}</h3>
                    <p className="text-gray-600 line-clamp-2 mb-4">{dispute.description}</p>

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-gray-500">Заказчик:</span>
                        <p className="font-medium text-gray-900">{dispute.clientName}</p>
                        <p className="text-gray-500 text-xs">{dispute.clientEmail}</p>
                      </div>
                      <div>
                        <span className="text-gray-500">Исполнитель:</span>
                        <p className="font-medium text-gray-900">{dispute.executorName || '-'}</p>
                        <p className="text-gray-500 text-xs">{dispute.executorEmail || '-'}</p>
                      </div>
                      <div>
                        <span className="text-gray-500">Сумма:</span>
                        <p className="font-medium text-gray-900">
                          {dispute.agreedPrice?.toLocaleString() || dispute.budgetMax?.toLocaleString() || '-'} сом
                        </p>
                      </div>
                      <div>
                        <span className="text-gray-500">Создан:</span>
                        <p className="font-medium text-gray-900">
                          {format(new Date(dispute.createdAt), 'd MMM yyyy', { locale: ru })}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-3 mt-4 pt-4 border-t border-gray-100">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => window.open(`/orders/${dispute.id}`, '_blank')}
                  >
                    <Eye className="w-4 h-4 mr-1" />
                    Просмотр
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => openResolveModal(dispute, true)}
                  >
                    <CheckCircle className="w-4 h-4 mr-1" />
                    В пользу заказчика
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openResolveModal(dispute, false)}
                  >
                    <CheckCircle className="w-4 h-4 mr-1" />
                    В пользу исполнителя
                  </Button>
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

      {/* Resolve Modal */}
      <Modal
        isOpen={showResolveModal}
        onClose={() => setShowResolveModal(false)}
        title={`Разрешить спор в пользу ${favorClient ? 'заказчика' : 'исполнителя'}`}
      >
        <div className="space-y-4">
          <div className="p-4 bg-gray-50 rounded-lg">
            <p className="font-medium text-gray-900">{selectedDispute?.title}</p>
            <p className="text-sm text-gray-500 mt-1">
              {favorClient ? selectedDispute?.clientName : selectedDispute?.executorName} получит
              положительное решение
            </p>
          </div>
          <Textarea
            label="Резолюция (необязательно)"
            placeholder="Опишите причину решения..."
            rows={4}
            value={resolution}
            onChange={(e) => setResolution(e.target.value)}
          />
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setShowResolveModal(false)}>
            Отмена
          </Button>
          <Button
            variant={favorClient ? 'primary' : 'outline'}
            onClick={() => resolveMutation.mutate()}
            loading={resolveMutation.isPending}
          >
            Подтвердить решение
          </Button>
        </div>
      </Modal>
    </AdminLayout>
  );
}
