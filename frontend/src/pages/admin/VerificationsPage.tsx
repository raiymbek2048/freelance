import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  Shield, CheckCircle, XCircle, Clock, Eye, ChevronLeft, ChevronRight
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { Button, Card, Badge, Avatar, Modal, Textarea } from '@/components/ui';
import { adminVerificationApi } from '@/api/verification';
import type { AdminVerificationResponse, VerificationStatus } from '@/types';

const statusLabels: Record<VerificationStatus, string> = {
  NONE: 'Нет заявки',
  PENDING: 'Ожидает',
  APPROVED: 'Одобрен',
  REJECTED: 'Отклонён',
};

const statusVariants: Record<VerificationStatus, 'default' | 'success' | 'warning' | 'error'> = {
  NONE: 'default',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
};

type TabType = 'pending' | 'all';

export function AdminVerificationsPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabType>('pending');
  const [page, setPage] = useState(0);
  const [selectedVerification, setSelectedVerification] = useState<AdminVerificationResponse | null>(null);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [previewImage, setPreviewImage] = useState<string | null>(null);

  const { data: pendingCount } = useQuery({
    queryKey: ['verification-pending-count'],
    queryFn: adminVerificationApi.getPendingCount,
  });

  const { data: verifications, isLoading } = useQuery({
    queryKey: ['admin-verifications', activeTab, page],
    queryFn: () =>
      activeTab === 'pending'
        ? adminVerificationApi.getPending(page, 20)
        : adminVerificationApi.getAll(page, 20),
  });

  const approveMutation = useMutation({
    mutationFn: (userId: number) => adminVerificationApi.approve(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-verifications'] });
      queryClient.invalidateQueries({ queryKey: ['verification-pending-count'] });
      setSelectedVerification(null);
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ userId, reason }: { userId: number; reason?: string }) =>
      adminVerificationApi.reject(userId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-verifications'] });
      queryClient.invalidateQueries({ queryKey: ['verification-pending-count'] });
      setSelectedVerification(null);
      setShowRejectModal(false);
      setRejectReason('');
    },
  });

  const handleApprove = (userId: number) => {
    if (confirm('Одобрить верификацию этого пользователя?')) {
      approveMutation.mutate(userId);
    }
  };

  const handleReject = () => {
    if (selectedVerification) {
      rejectMutation.mutate({
        userId: selectedVerification.userId,
        reason: rejectReason || undefined,
      });
    }
  };

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Верификация пользователей</h1>
            <p className="text-gray-600 mt-1">
              Проверка документов исполнителей
            </p>
          </div>
          {pendingCount && pendingCount.pending > 0 && (
            <Badge variant="warning" size="md">
              {pendingCount.pending} ожидают
            </Badge>
          )}
        </div>

        {/* Tabs */}
        <div className="flex gap-4 mb-6 border-b border-gray-200">
          <button
            onClick={() => { setActiveTab('pending'); setPage(0); }}
            className={`px-4 py-2 -mb-px border-b-2 transition-colors ${
              activeTab === 'pending'
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Clock className="w-4 h-4 inline mr-2" />
            Ожидающие
            {pendingCount && pendingCount.pending > 0 && (
              <span className="ml-2 px-2 py-0.5 bg-amber-100 text-amber-800 text-xs rounded-full">
                {pendingCount.pending}
              </span>
            )}
          </button>
          <button
            onClick={() => { setActiveTab('all'); setPage(0); }}
            className={`px-4 py-2 -mb-px border-b-2 transition-colors ${
              activeTab === 'all'
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Все заявки
          </button>
        </div>

        {isLoading ? (
          <div className="animate-pulse space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-24 bg-gray-200 rounded-lg" />
            ))}
          </div>
        ) : verifications?.content.length === 0 ? (
          <Card padding="lg" className="text-center">
            <Shield className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900">Нет заявок</h3>
            <p className="text-gray-500 mt-1">
              {activeTab === 'pending'
                ? 'Нет ожидающих заявок на верификацию'
                : 'Заявок на верификацию пока нет'}
            </p>
          </Card>
        ) : (
          <>
            <div className="space-y-4">
              {verifications?.content.map((verification) => (
                <Card key={verification.userId} padding="md">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <Avatar
                        src={verification.userAvatarUrl}
                        name={verification.userFullName}
                        size="lg"
                      />
                      <div>
                        <h3 className="font-medium text-gray-900">
                          {verification.userFullName}
                        </h3>
                        <p className="text-sm text-gray-500">{verification.userEmail}</p>
                        <p className="text-sm text-gray-500">{verification.userPhone}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-4">
                      <Badge variant={statusVariants[verification.status]}>
                        {statusLabels[verification.status]}
                      </Badge>
                      <span className="text-sm text-gray-500">
                        {format(new Date(verification.submittedAt), 'd MMM yyyy, HH:mm', { locale: ru })}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setSelectedVerification(verification)}
                      >
                        <Eye className="w-4 h-4 mr-1" /> Просмотр
                      </Button>
                    </div>
                  </div>
                </Card>
              ))}
            </div>

            {/* Pagination */}
            {verifications && verifications.totalPages > 1 && (
              <div className="flex items-center justify-center gap-4 mt-6">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={verifications.first}
                  onClick={() => setPage(page - 1)}
                >
                  <ChevronLeft className="w-4 h-4" />
                </Button>
                <span className="text-sm text-gray-600">
                  Страница {page + 1} из {verifications.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={verifications.last}
                  onClick={() => setPage(page + 1)}
                >
                  <ChevronRight className="w-4 h-4" />
                </Button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Detail Modal */}
      <Modal
        isOpen={!!selectedVerification}
        onClose={() => setSelectedVerification(null)}
        title="Детали верификации"
        size="lg"
      >
        {selectedVerification && (
          <div className="space-y-6">
            {/* User Info */}
            <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
              <Avatar
                src={selectedVerification.userAvatarUrl}
                name={selectedVerification.userFullName}
                size="xl"
              />
              <div>
                <h3 className="font-semibold text-lg">{selectedVerification.userFullName}</h3>
                <p className="text-gray-600">{selectedVerification.userEmail}</p>
                <p className="text-gray-600">{selectedVerification.userPhone}</p>
              </div>
              <Badge variant={statusVariants[selectedVerification.status]} className="ml-auto">
                {statusLabels[selectedVerification.status]}
              </Badge>
            </div>

            {/* Documents */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <h4 className="font-medium mb-2">Паспорт</h4>
                <img
                  src={selectedVerification.passportUrl}
                  alt="Паспорт"
                  className="w-full h-48 object-cover rounded-lg cursor-pointer hover:opacity-90"
                  onClick={() => setPreviewImage(selectedVerification.passportUrl)}
                />
              </div>
              <div>
                <h4 className="font-medium mb-2">Селфи с паспортом</h4>
                <img
                  src={selectedVerification.selfieUrl}
                  alt="Селфи"
                  className="w-full h-48 object-cover rounded-lg cursor-pointer hover:opacity-90"
                  onClick={() => setPreviewImage(selectedVerification.selfieUrl)}
                />
              </div>
            </div>

            {/* Rejection Reason */}
            {selectedVerification.rejectionReason && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-800">
                  <strong>Причина отказа:</strong> {selectedVerification.rejectionReason}
                </p>
              </div>
            )}

            {/* Review Info */}
            {selectedVerification.reviewedAt && (
              <p className="text-sm text-gray-500">
                Проверено: {format(new Date(selectedVerification.reviewedAt), 'd MMM yyyy, HH:mm', { locale: ru })}
                {selectedVerification.reviewedByName && ` (${selectedVerification.reviewedByName})`}
              </p>
            )}

            {/* Actions */}
            {selectedVerification.status === 'PENDING' && (
              <div className="flex gap-3 pt-4 border-t">
                <Button
                  className="flex-1"
                  onClick={() => handleApprove(selectedVerification.userId)}
                  loading={approveMutation.isPending}
                >
                  <CheckCircle className="w-4 h-4 mr-2" /> Одобрить
                </Button>
                <Button
                  variant="danger"
                  className="flex-1"
                  onClick={() => setShowRejectModal(true)}
                >
                  <XCircle className="w-4 h-4 mr-2" /> Отклонить
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Reject Modal */}
      <Modal
        isOpen={showRejectModal}
        onClose={() => setShowRejectModal(false)}
        title="Отклонить верификацию"
      >
        <div className="space-y-4">
          <Textarea
            label="Причина отказа (опционально)"
            placeholder="Укажите причину отказа..."
            rows={3}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
          <div className="flex gap-3">
            <Button
              variant="outline"
              className="flex-1"
              onClick={() => setShowRejectModal(false)}
            >
              Отмена
            </Button>
            <Button
              variant="danger"
              className="flex-1"
              onClick={handleReject}
              loading={rejectMutation.isPending}
            >
              Отклонить
            </Button>
          </div>
        </div>
      </Modal>

      {/* Image Preview Modal */}
      <Modal
        isOpen={!!previewImage}
        onClose={() => setPreviewImage(null)}
        title="Просмотр документа"
        size="xl"
      >
        {previewImage && (
          <img
            src={previewImage}
            alt="Документ"
            className="w-full max-h-[70vh] object-contain"
          />
        )}
      </Modal>
    </AdminLayout>
  );
}
