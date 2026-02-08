import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  Clock, MessageCircle, Eye, Calendar, Check, X, MessageSquare,
  AlertTriangle, Shield, Edit2, Trash2, CreditCard
} from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Layout } from '@/components/layout';
import { Button, Card, Badge, Avatar, Textarea, Input, Modal, Rating } from '@/components/ui';
import { ordersApi } from '@/api/orders';
import { chatApi } from '@/api/chat';
import { useAuthStore } from '@/stores/authStore';
import type { OrderStatus, OrderResponse } from '@/types';

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

const responseSchema = z.object({
  coverLetter: z.string().min(10, 'Минимум 10 символов'),
  proposedPrice: z.number().optional(),
  proposedDays: z.number().optional(),
});

type ResponseForm = z.infer<typeof responseSchema>;

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuthStore();
  const [showResponseModal, setShowResponseModal] = useState(false);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [showRevisionModal, setShowRevisionModal] = useState(false);
  const [showDisputeModal, setShowDisputeModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [revisionReason, setRevisionReason] = useState('');
  const [disputeReason, setDisputeReason] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editBudget, setEditBudget] = useState<number | undefined>();
  const [editDeadline, setEditDeadline] = useState('');

  const { data: order, isLoading } = useQuery({
    queryKey: ['order', id],
    queryFn: () => ordersApi.getById(Number(id)),
    enabled: !!id,
  });

  const { data: responses } = useQuery({
    queryKey: ['order-responses', id],
    queryFn: () => ordersApi.getResponses(Number(id)),
    enabled: !!id && order?.status === 'NEW',
  });

  const responseForm = useForm<ResponseForm>({
    resolver: zodResolver(responseSchema),
  });

  const respondMutation = useMutation({
    mutationFn: (data: ResponseForm) => {
      // Clean up NaN values from empty number inputs
      const cleanData = {
        coverLetter: data.coverLetter,
        proposedPrice: data.proposedPrice && !isNaN(data.proposedPrice) ? data.proposedPrice : undefined,
        proposedDays: data.proposedDays && !isNaN(data.proposedDays) ? data.proposedDays : undefined,
      };
      return ordersApi.respond(Number(id), cleanData);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      queryClient.invalidateQueries({ queryKey: ['order-responses', id] });
      setShowResponseModal(false);
      responseForm.reset();
    },
  });

  const selectExecutorMutation = useMutation({
    mutationFn: (responseId: number) => ordersApi.selectExecutor(Number(id), responseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
    },
  });

  const startChatMutation = useMutation({
    mutationFn: (executorId: number) => chatApi.getOrCreateChat(Number(id), executorId),
    onSuccess: (chatRoom) => {
      navigate(`/chats?room=${chatRoom.id}`);
    },
  });

  const approveWorkMutation = useMutation({
    mutationFn: () => ordersApi.approveWork(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      setShowReviewModal(true);
    },
  });

  const submitForReviewMutation = useMutation({
    mutationFn: () => ordersApi.submitForReview(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
    },
  });

  const reviewMutation = useMutation({
    mutationFn: () => ordersApi.createReview(Number(id), { rating: reviewRating, comment: reviewComment }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      setShowReviewModal(false);
    },
  });

  const revisionMutation = useMutation({
    mutationFn: () => ordersApi.requestRevision(Number(id), revisionReason || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      setShowRevisionModal(false);
      setRevisionReason('');
    },
  });

  const disputeMutation = useMutation({
    mutationFn: () => ordersApi.openDispute(Number(id), disputeReason || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      setShowDisputeModal(false);
      setDisputeReason('');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => ordersApi.delete(Number(id)),
    onSuccess: () => {
      navigate('/');
    },
  });

  const updateMutation = useMutation({
    mutationFn: () => ordersApi.update(Number(id), {
      title: editTitle,
      description: editDescription,
      budgetMax: editBudget,
      deadline: editDeadline || undefined,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      setShowEditModal(false);
    },
  });

  const openEditModal = () => {
    if (order) {
      setEditTitle(order.title);
      setEditDescription(order.description);
      setEditBudget(order.budgetMax);
      setEditDeadline(order.deadline ? order.deadline.split('T')[0] : '');
      setShowEditModal(true);
    }
  };

  const isClient = order?.isOwner ?? false;
  const isExecutor = order?.isExecutor ?? false;
  const canRespond = isAuthenticated && !isClient && order?.status === 'NEW';
  const hasResponded = order?.hasResponded ?? false;

  if (isLoading) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto px-4 py-8">
          <div className="animate-pulse space-y-4">
            <div className="h-8 bg-gray-200 rounded w-3/4" />
            <div className="h-4 bg-gray-200 rounded w-full" />
            <div className="h-4 bg-gray-200 rounded w-2/3" />
          </div>
        </div>
      </Layout>
    );
  }

  if (!order) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto px-4 py-8 text-center">
          <h1 className="text-2xl font-bold text-gray-900">Заказ не найден</h1>
          <Link to="/orders" className="text-primary-600 hover:underline mt-4 inline-block">
            Вернуться к списку заказов
          </Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-6">
          <div className="flex items-center gap-3 mb-4">
            <Badge variant={statusVariants[order.status]} size="md">
              {statusLabels[order.status]}
            </Badge>
          </div>
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{order.title}</h1>
          <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500">
            <span className="flex items-center gap-1">
              <Eye className="w-4 h-4" /> {order.viewCount} просмотров
            </span>
            <span className="flex items-center gap-1">
              <MessageCircle className="w-4 h-4" /> {order.responseCount} откликов
            </span>
            <span className="flex items-center gap-1">
              <Clock className="w-4 h-4" />
              {format(new Date(order.createdAt), 'd MMMM yyyy', { locale: ru })}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            <Card padding="lg">
              <h2 className="text-lg font-semibold mb-4">Описание</h2>
              <p className="text-gray-700 whitespace-pre-wrap break-words overflow-hidden">{order.description}</p>

              {/* Verification Banner */}
              {order.descriptionTruncated && order.requiresVerification && (
                <div className="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-lg">
                  <div className="flex items-start gap-3">
                    <Shield className="w-5 h-5 text-amber-600 mt-0.5" />
                    <div>
                      <h4 className="font-medium text-amber-800">Описание скрыто</h4>
                      <p className="text-sm text-amber-700 mt-1">
                        Для просмотра полного описания заказа необходимо пройти верификацию.
                        Это помогает нам защитить заказчиков от спама и мошенничества.
                      </p>
                      <Link
                        to="/verification"
                        className="inline-flex items-center gap-1 mt-2 text-sm font-medium text-amber-800 hover:text-amber-900"
                      >
                        Пройти верификацию <Shield className="w-4 h-4" />
                      </Link>
                    </div>
                  </div>
                </div>
              )}

              {/* Subscription Banner */}
              {order.descriptionTruncated && order.requiresSubscription && (
                <div className="mt-4 p-4 bg-cyan-50 border border-cyan-200 rounded-lg">
                  <div className="flex items-start gap-3">
                    <CreditCard className="w-5 h-5 text-cyan-600 mt-0.5" />
                    <div>
                      <h4 className="font-medium text-cyan-800">Требуется подписка</h4>
                      <p className="text-sm text-cyan-700 mt-1">
                        Для просмотра полного описания заказа и отклика требуется активная подписка.
                      </p>
                      <Link
                        to="/profile"
                        className="inline-flex items-center gap-1 mt-2 text-sm font-medium text-cyan-800 hover:text-cyan-900"
                      >
                        Оформить подписку <CreditCard className="w-4 h-4" />
                      </Link>
                    </div>
                  </div>
                </div>
              )}
            </Card>

            {/* Responses (for client) */}
            {isClient && order.status === 'NEW' && responses && responses.length > 0 && (
              <Card padding="lg">
                <h2 className="text-lg font-semibold mb-4">Отклики ({responses.length})</h2>
                <div className="space-y-4">
                  {responses.map((response: OrderResponse) => (
                    <div key={response.id} className="p-4 border border-gray-200 rounded-lg">
                      <div className="flex items-start justify-between mb-3">
                        <Link
                          to={`/executors/${response.executorId}`}
                          className="flex items-center gap-3"
                        >
                          <Avatar
                            src={response.executorAvatarUrl}
                            name={response.executorName}
                          />
                          <div>
                            <p className="font-medium text-gray-900">
                              {response.executorName}
                            </p>
                            <div className="flex items-center gap-2 text-sm text-gray-500">
                              <Rating value={response.executorRating} size="sm" />
                              <span>({response.executorCompletedOrders} заказов)</span>
                            </div>
                          </div>
                        </Link>
                        {response.proposedPrice && (
                          <span className="font-semibold text-gray-900">
                            {response.proposedPrice.toLocaleString()} сом
                          </span>
                        )}
                      </div>
                      <p className="text-gray-700 mb-4">{response.coverLetter}</p>
                      <div className="flex items-center gap-2">
                        <Button
                          size="sm"
                          onClick={() => selectExecutorMutation.mutate(response.id)}
                          loading={selectExecutorMutation.isPending}
                        >
                          <Check className="w-4 h-4 mr-1" /> Выбрать
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => startChatMutation.mutate(response.executorId)}
                          loading={startChatMutation.isPending}
                        >
                          <MessageSquare className="w-4 h-4 mr-1" /> Написать
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            )}

            {/* Actions for executor */}
            {isExecutor && (order.status === 'IN_PROGRESS' || order.status === 'REVISION') && (
              <Card padding="lg">
                <h2 className="text-lg font-semibold mb-4">Действия</h2>
                {order.status === 'REVISION' && (
                  <div className="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                    <p className="text-amber-800 text-sm">
                      Заказчик запросил доработку. Проверьте сообщения в чате для уточнения деталей.
                    </p>
                  </div>
                )}
                <div className="flex gap-3">
                  <Button
                    onClick={() => submitForReviewMutation.mutate()}
                    loading={submitForReviewMutation.isPending}
                  >
                    {order.status === 'REVISION' ? 'Сдать доработку на проверку' : 'Сдать работу на проверку'}
                  </Button>
                  <Link to={`/chats`}>
                    <Button variant="outline">
                      <MessageSquare className="w-4 h-4 mr-1" /> Чат с заказчиком
                    </Button>
                  </Link>
                </div>
              </Card>
            )}

            {/* Actions for client */}
            {isClient && order.status === 'ON_REVIEW' && (
              <Card padding="lg">
                <h2 className="text-lg font-semibold mb-4">Проверка работы</h2>
                <p className="text-gray-600 mb-4">
                  Исполнитель сдал работу на проверку. Примите работу или запросите доработку.
                </p>
                <div className="flex flex-wrap gap-3">
                  <Button
                    onClick={() => approveWorkMutation.mutate()}
                    loading={approveWorkMutation.isPending}
                  >
                    <Check className="w-4 h-4 mr-1" /> Принять работу
                  </Button>
                  <Button variant="outline" onClick={() => setShowRevisionModal(true)}>
                    <X className="w-4 h-4 mr-1" /> Запросить доработку
                  </Button>
                  <Button variant="danger" onClick={() => setShowDisputeModal(true)}>
                    <AlertTriangle className="w-4 h-4 mr-1" /> Открыть спор
                  </Button>
                </div>
              </Card>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Budget */}
            <Card padding="md">
              <h3 className="text-sm font-medium text-gray-500 mb-2">Бюджет</h3>
              <p className="text-2xl font-bold text-gray-900">
                {order.agreedPrice
                  ? `${order.agreedPrice.toLocaleString()} сом`
                  : order.budgetMin && order.budgetMax
                  ? `${order.budgetMin.toLocaleString()} - ${order.budgetMax.toLocaleString()} сом`
                  : order.budgetMax
                  ? `до ${order.budgetMax.toLocaleString()} сом`
                  : 'По договорённости'}
              </p>
              {order.deadline && (
                <div className="mt-4">
                  <h3 className="text-sm font-medium text-gray-500 mb-1">Срок</h3>
                  <p className="flex items-center gap-2 text-gray-900">
                    <Calendar className="w-4 h-4" />
                    {format(new Date(order.deadline), 'd MMMM yyyy', { locale: ru })}
                  </p>
                </div>
              )}
            </Card>

            {/* Client */}
            <Card padding="md">
              <h3 className="text-sm font-medium text-gray-500 mb-3">Заказчик</h3>
              <div className="flex items-center gap-3">
                <Avatar src={order.clientAvatarUrl} name={order.clientName} size="lg" />
                <div>
                  <p className="font-medium text-gray-900">{order.clientName}</p>
                </div>
              </div>
            </Card>

            {/* Edit/Delete buttons for client */}
            {isClient && order.status === 'NEW' && (
              <Card padding="md">
                <h3 className="text-sm font-medium text-gray-500 mb-3">Управление заказом</h3>
                <div className="space-y-2">
                  <Button
                    variant="outline"
                    className="w-full"
                    onClick={openEditModal}
                  >
                    <Edit2 className="w-4 h-4 mr-2" /> Редактировать
                  </Button>
                  <Button
                    variant="danger"
                    className="w-full"
                    onClick={() => setShowDeleteModal(true)}
                  >
                    <Trash2 className="w-4 h-4 mr-2" /> Удалить
                  </Button>
                </div>
              </Card>
            )}

            {/* Executor (if selected) */}
            {order.executorId && (
              <Card padding="md">
                <h3 className="text-sm font-medium text-gray-500 mb-3">Исполнитель</h3>
                <Link to={`/executors/${order.executorId}`} className="flex items-center gap-3">
                  <Avatar src={order.executorAvatarUrl} name={order.executorName || ''} size="lg" />
                  <div>
                    <p className="font-medium text-gray-900">{order.executorName}</p>
                  </div>
                </Link>
              </Card>
            )}

            {/* Respond Button */}
            {canRespond && !hasResponded && !order.requiresVerification && !order.requiresSubscription && (
              <Button className="w-full" size="lg" onClick={() => setShowResponseModal(true)}>
                Откликнуться
              </Button>
            )}
            {canRespond && !hasResponded && order.requiresVerification && (
              <div className="space-y-3">
                <Button className="w-full" size="lg" disabled>
                  Откликнуться
                </Button>
                <p className="text-sm text-center text-gray-500">
                  Для отклика необходимо{' '}
                  <Link to="/verification" className="text-primary-600 hover:underline">
                    пройти верификацию
                  </Link>
                </p>
              </div>
            )}
            {canRespond && !hasResponded && order.requiresSubscription && !order.requiresVerification && (
              <div className="space-y-3">
                <Button className="w-full" size="lg" disabled>
                  Откликнуться
                </Button>
                <p className="text-sm text-center text-gray-500">
                  Для отклика необходима{' '}
                  <Link to="/profile" className="text-primary-600 hover:underline">
                    активная подписка
                  </Link>
                </p>
              </div>
            )}
            {hasResponded && (
              <div className="text-center text-green-600 font-medium">
                <Check className="w-5 h-5 inline mr-1" /> Вы уже откликнулись
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Response Modal */}
      <Modal
        isOpen={showResponseModal}
        onClose={() => setShowResponseModal(false)}
        title="Откликнуться на заказ"
        size="lg"
      >
        <form onSubmit={responseForm.handleSubmit((data) => respondMutation.mutate(data))}>
          <div className="space-y-4">
            <Textarea
              label="Сопроводительное письмо"
              placeholder="Расскажите, почему вы подходите для этого заказа..."
              rows={5}
              error={responseForm.formState.errors.coverLetter?.message}
              {...responseForm.register('coverLetter')}
            />
            <Input
              label="Предлагаемая цена (сом)"
              type="number"
              placeholder="Оставьте пустым, если согласны с бюджетом"
              {...responseForm.register('proposedPrice', { valueAsNumber: true })}
            />
            <Input
              label="Срок выполнения (дней)"
              type="number"
              placeholder="Сколько дней вам потребуется"
              {...responseForm.register('proposedDays', { valueAsNumber: true })}
            />
          </div>
          <div className="mt-6 flex justify-end gap-3">
            <Button variant="outline" type="button" onClick={() => setShowResponseModal(false)}>
              Отмена
            </Button>
            <Button type="submit" loading={respondMutation.isPending}>
              Отправить отклик
            </Button>
          </div>
        </form>
      </Modal>

      {/* Review Modal */}
      <Modal
        isOpen={showReviewModal}
        onClose={() => setShowReviewModal(false)}
        title="Оставить отзыв"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Оценка</label>
            <Rating value={reviewRating} onChange={setReviewRating} size="lg" />
          </div>
          <Textarea
            label="Комментарий"
            placeholder="Расскажите о вашем опыте работы..."
            rows={4}
            value={reviewComment}
            onChange={(e) => setReviewComment(e.target.value)}
          />
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setShowReviewModal(false)}>
            Пропустить
          </Button>
          <Button onClick={() => reviewMutation.mutate()} loading={reviewMutation.isPending}>
            Отправить отзыв
          </Button>
        </div>
      </Modal>

      {/* Revision Modal */}
      <Modal
        isOpen={showRevisionModal}
        onClose={() => setShowRevisionModal(false)}
        title="Запросить доработку"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Опишите, что необходимо доработать. Исполнитель получит уведомление.
          </p>
          <Textarea
            label="Причина доработки"
            placeholder="Опишите, что нужно исправить или доработать..."
            rows={4}
            value={revisionReason}
            onChange={(e) => setRevisionReason(e.target.value)}
          />
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setShowRevisionModal(false)}>
            Отмена
          </Button>
          <Button onClick={() => revisionMutation.mutate()} loading={revisionMutation.isPending}>
            Отправить на доработку
          </Button>
        </div>
      </Modal>

      {/* Dispute Modal */}
      <Modal
        isOpen={showDisputeModal}
        onClose={() => setShowDisputeModal(false)}
        title="Открыть спор"
      >
        <div className="space-y-4">
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-sm text-red-700">
              Спор — это крайняя мера. Модератор рассмотрит вашу жалобу и примет решение.
              Пожалуйста, сначала попробуйте решить вопрос напрямую с исполнителем.
            </p>
          </div>
          <Textarea
            label="Причина спора"
            placeholder="Опишите проблему подробно..."
            rows={4}
            value={disputeReason}
            onChange={(e) => setDisputeReason(e.target.value)}
          />
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setShowDisputeModal(false)}>
            Отмена
          </Button>
          <Button variant="danger" onClick={() => disputeMutation.mutate()} loading={disputeMutation.isPending}>
            Открыть спор
          </Button>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title="Удалить заказ"
      >
        <div className="space-y-4">
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-sm text-red-700">
              Вы уверены, что хотите удалить этот заказ? Это действие нельзя отменить.
              Все отклики также будут удалены.
            </p>
          </div>
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setShowDeleteModal(false)}>
            Отмена
          </Button>
          <Button variant="danger" onClick={() => deleteMutation.mutate()} loading={deleteMutation.isPending}>
            Удалить
          </Button>
        </div>
      </Modal>

      {/* Edit Modal */}
      <Modal
        isOpen={showEditModal}
        onClose={() => setShowEditModal(false)}
        title="Редактировать заказ"
        size="lg"
      >
        <div className="space-y-4">
          <Input
            label="Название"
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            placeholder="Название задания"
          />
          <Textarea
            label="Описание"
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
            placeholder="Подробное описание задания"
            rows={5}
          />
          <Input
            label="Бюджет (сом)"
            type="number"
            value={editBudget || ''}
            onChange={(e) => setEditBudget(e.target.value ? Number(e.target.value) : undefined)}
            placeholder="Максимальный бюджет"
          />
          <Input
            label="Выполнить до"
            type="date"
            value={editDeadline}
            onChange={(e) => setEditDeadline(e.target.value)}
          />
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => setShowEditModal(false)}>
            Отмена
          </Button>
          <Button onClick={() => updateMutation.mutate()} loading={updateMutation.isPending}>
            Сохранить
          </Button>
        </div>
      </Modal>
    </Layout>
  );
}
