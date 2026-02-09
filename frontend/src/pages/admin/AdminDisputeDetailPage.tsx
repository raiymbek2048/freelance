import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  ArrowLeft, Shield, CheckCircle, Clock, AlertTriangle, FileText, Image,
  ExternalLink, MessageSquare, User,
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { Button, Card, Badge, Textarea, Modal } from '@/components/ui';
import { adminApi } from '@/api/admin';
import type { DisputeStatusType, DisputeEvidenceResponse, Message } from '@/types';

const statusLabels: Record<DisputeStatusType, string> = {
  OPEN: 'Открыт',
  UNDER_REVIEW: 'На рассмотрении',
  RESOLVED: 'Решён',
};

const statusVariants: Record<DisputeStatusType, 'error' | 'warning' | 'success'> = {
  OPEN: 'error',
  UNDER_REVIEW: 'warning',
  RESOLVED: 'success',
};

export function AdminDisputeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [adminNotes, setAdminNotes] = useState('');
  const [showResolveModal, setShowResolveModal] = useState(false);
  const [favorClient, setFavorClient] = useState(true);
  const [resolutionNotes, setResolutionNotes] = useState('');

  const { data: dispute, isLoading } = useQuery({
    queryKey: ['admin-dispute', id],
    queryFn: () => adminApi.getDisputeDetail(Number(id)),
    enabled: !!id,
  });

  const { data: messagesData } = useQuery({
    queryKey: ['admin-dispute-messages', id],
    queryFn: () => adminApi.getDisputeMessages(Number(id)),
    enabled: !!id,
  });

  const takeMutation = useMutation({
    mutationFn: () => adminApi.takeDisputeForReview(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-dispute', id] });
      queryClient.invalidateQueries({ queryKey: ['admin-disputes'] });
    },
  });

  const notesMutation = useMutation({
    mutationFn: () => adminApi.addDisputeNotes(Number(id), adminNotes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-dispute', id] });
    },
  });

  const resolveMutation = useMutation({
    mutationFn: () =>
      adminApi.resolveDisputeNew(Number(id), {
        favorClient,
        resolutionNotes: resolutionNotes || undefined,
        adminNotes: adminNotes || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-dispute', id] });
      queryClient.invalidateQueries({ queryKey: ['admin-disputes'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      setShowResolveModal(false);
    },
  });

  const isImage = (fileType?: string) => fileType?.startsWith('image/');

  if (isLoading) {
    return (
      <AdminLayout>
        <div className="max-w-5xl mx-auto space-y-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-32 bg-gray-200 rounded-xl animate-pulse" />
          ))}
        </div>
      </AdminLayout>
    );
  }

  if (!dispute) {
    return (
      <AdminLayout>
        <div className="max-w-5xl mx-auto text-center py-12">
          <AlertTriangle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-900">Спор не найден</h2>
          <Link to="/admin/disputes" className="text-primary-600 hover:underline mt-2 block">
            Вернуться к списку
          </Link>
        </div>
      </AdminLayout>
    );
  }

  const messages = messagesData?.content || [];

  return (
    <AdminLayout>
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Back link */}
        <Link to="/admin/disputes" className="inline-flex items-center text-gray-600 hover:text-gray-900">
          <ArrowLeft className="w-4 h-4 mr-1" /> Назад к списку споров
        </Link>

        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Спор #{dispute.id}</h1>
            <p className="text-gray-600 mt-1">{dispute.orderTitle}</p>
          </div>
          <Badge variant={statusVariants[dispute.status]}>
            {statusLabels[dispute.status]}
          </Badge>
        </div>

        {/* Order & Participants info */}
        <Card padding="lg">
          <h2 className="text-lg font-semibold mb-4">Информация о заказе</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <span className="text-sm text-gray-500">Заказ</span>
              <Link to={`/orders/${dispute.orderId}`} className="block font-medium text-primary-600 hover:underline">
                #{dispute.orderId} — {dispute.orderTitle}
              </Link>
            </div>
            <div>
              <span className="text-sm text-gray-500">Заказчик</span>
              <div className="flex items-center gap-2 mt-1">
                {dispute.clientAvatarUrl ? (
                  <img src={dispute.clientAvatarUrl} alt="" className="w-6 h-6 rounded-full" />
                ) : (
                  <User className="w-6 h-6 text-gray-400" />
                )}
                <span className="font-medium text-gray-900">{dispute.clientName}</span>
              </div>
            </div>
            <div>
              <span className="text-sm text-gray-500">Исполнитель</span>
              <div className="flex items-center gap-2 mt-1">
                {dispute.executorAvatarUrl ? (
                  <img src={dispute.executorAvatarUrl} alt="" className="w-6 h-6 rounded-full" />
                ) : (
                  <User className="w-6 h-6 text-gray-400" />
                )}
                <span className="font-medium text-gray-900">{dispute.executorName}</span>
              </div>
            </div>
          </div>
        </Card>

        {/* Dispute info */}
        <Card padding="lg">
          <h2 className="text-lg font-semibold mb-4">Детали спора</h2>
          <div className="space-y-4">
            <div>
              <span className="text-sm text-gray-500">Инициатор:</span>
              <p className="font-medium text-gray-900">
                {dispute.openedByName} ({dispute.openedByRole === 'CLIENT' ? 'Заказчик' : 'Исполнитель'})
              </p>
            </div>
            <div>
              <span className="text-sm text-gray-500">Причина спора:</span>
              <p className="text-gray-900 mt-1 whitespace-pre-wrap bg-gray-50 p-4 rounded-lg">
                {dispute.reason}
              </p>
            </div>
            <div className="flex gap-6 text-sm text-gray-500">
              <span className="flex items-center gap-1">
                <Clock className="w-4 h-4" />
                Открыт: {format(new Date(dispute.createdAt), 'd MMM yyyy HH:mm', { locale: ru })}
              </span>
              {dispute.resolvedAt && (
                <span className="flex items-center gap-1">
                  <CheckCircle className="w-4 h-4" />
                  Решён: {format(new Date(dispute.resolvedAt), 'd MMM yyyy HH:mm', { locale: ru })}
                </span>
              )}
            </div>
          </div>
        </Card>

        {/* Resolution (if resolved) */}
        {dispute.status === 'RESOLVED' && dispute.resolution && (
          <Card padding="lg">
            <div className="flex items-center gap-2 mb-3">
              <CheckCircle className="w-5 h-5 text-green-500" />
              <h2 className="text-lg font-semibold">Решение</h2>
            </div>
            <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
              <p className="font-medium text-green-800">
                {dispute.resolution === 'FAVOR_CLIENT'
                  ? 'Решено в пользу заказчика'
                  : 'Решено в пользу исполнителя'}
              </p>
              {dispute.resolutionNotes && (
                <p className="text-green-700 mt-2">{dispute.resolutionNotes}</p>
              )}
            </div>
          </Card>
        )}

        {/* Evidence gallery */}
        <Card padding="lg">
          <h2 className="text-lg font-semibold mb-4">
            Доказательства ({dispute.evidence.length})
          </h2>

          {dispute.evidence.length === 0 ? (
            <p className="text-gray-500 text-center py-8">
              Доказательства ещё не загружены
            </p>
          ) : (
            <div className="space-y-3">
              {dispute.evidence.map((ev: DisputeEvidenceResponse) => (
                <div key={ev.id} className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                  <div className="flex-shrink-0">
                    {isImage(ev.fileType) ? (
                      <Image className="w-8 h-8 text-blue-500" />
                    ) : (
                      <FileText className="w-8 h-8 text-red-500" />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <a
                      href={ev.fileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="font-medium text-primary-600 hover:underline flex items-center gap-1"
                    >
                      {ev.fileName} <ExternalLink className="w-3 h-3" />
                    </a>
                    {ev.description && (
                      <p className="text-sm text-gray-600 mt-1">{ev.description}</p>
                    )}
                    <p className="text-xs text-gray-400 mt-1">
                      {ev.uploadedByName} ({ev.uploadedByRole === 'CLIENT' ? 'Заказчик' : 'Исполнитель'})
                      {' · '}
                      {format(new Date(ev.createdAt), 'd MMM yyyy HH:mm', { locale: ru })}
                    </p>
                  </div>
                  {isImage(ev.fileType) && (
                    <a href={ev.fileUrl} target="_blank" rel="noopener noreferrer">
                      <img
                        src={ev.fileUrl}
                        alt={ev.fileName}
                        className="w-20 h-20 object-cover rounded border"
                      />
                    </a>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* Chat history (read-only) */}
        <Card padding="lg">
          <div className="flex items-center gap-2 mb-4">
            <MessageSquare className="w-5 h-5 text-gray-500" />
            <h2 className="text-lg font-semibold">
              История чата ({messages.length})
            </h2>
          </div>

          {messages.length === 0 ? (
            <p className="text-gray-500 text-center py-8">Сообщений нет</p>
          ) : (
            <div className="max-h-96 overflow-y-auto space-y-3 border border-gray-200 rounded-lg p-4">
              {messages.map((msg: Message) => (
                <div key={msg.id} className="p-3 rounded-lg bg-gray-50">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-sm font-medium text-gray-900">{msg.senderName}</span>
                    <span className="text-xs text-gray-400">
                      {format(new Date(msg.createdAt), 'd MMM HH:mm', { locale: ru })}
                    </span>
                  </div>
                  <p className="text-sm text-gray-700">{msg.content}</p>
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* Admin actions */}
        {dispute.status !== 'RESOLVED' && (
          <Card padding="lg">
            <div className="flex items-center gap-2 mb-4">
              <Shield className="w-5 h-5 text-primary-600" />
              <h2 className="text-lg font-semibold">Действия модератора</h2>
            </div>

            <div className="space-y-4">
              {/* Take for review */}
              {dispute.status === 'OPEN' && (
                <Button
                  variant="primary"
                  onClick={() => takeMutation.mutate()}
                  loading={takeMutation.isPending}
                >
                  <Shield className="w-4 h-4 mr-1" />
                  Взять на рассмотрение
                </Button>
              )}

              {dispute.status === 'UNDER_REVIEW' && dispute.adminName && (
                <p className="text-sm text-gray-600">
                  Рассматривает: <span className="font-medium">{dispute.adminName}</span>
                </p>
              )}

              {/* Admin notes */}
              <div>
                <Textarea
                  label="Заметки модератора"
                  placeholder="Добавьте заметки по спору..."
                  rows={3}
                  value={adminNotes || dispute.adminNotes || ''}
                  onChange={(e) => setAdminNotes(e.target.value)}
                />
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-2"
                  onClick={() => notesMutation.mutate()}
                  loading={notesMutation.isPending}
                  disabled={!adminNotes}
                >
                  Сохранить заметки
                </Button>
              </div>

              {/* Resolve buttons */}
              <div className="pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-700 mb-3">Разрешить спор:</p>
                <div className="flex gap-3">
                  <Button
                    variant="primary"
                    onClick={() => {
                      setFavorClient(true);
                      setShowResolveModal(true);
                    }}
                  >
                    <CheckCircle className="w-4 h-4 mr-1" />
                    В пользу заказчика
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => {
                      setFavorClient(false);
                      setShowResolveModal(true);
                    }}
                  >
                    <CheckCircle className="w-4 h-4 mr-1" />
                    В пользу исполнителя
                  </Button>
                </div>
              </div>
            </div>
          </Card>
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
            <p className="font-medium text-gray-900">{dispute?.orderTitle}</p>
            <p className="text-sm text-gray-500 mt-1">
              {favorClient ? dispute?.clientName : dispute?.executorName} получит
              положительное решение
            </p>
          </div>
          <Textarea
            label="Комментарий к решению (необязательно)"
            placeholder="Опишите причину решения..."
            rows={4}
            value={resolutionNotes}
            onChange={(e) => setResolutionNotes(e.target.value)}
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
