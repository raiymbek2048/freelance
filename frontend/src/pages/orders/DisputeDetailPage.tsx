import { useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  AlertTriangle, Upload, FileText, Image, MessageSquare, CheckCircle,
  Clock, Shield, ArrowLeft, ExternalLink,
} from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card, Badge, Modal, Textarea } from '@/components/ui';
import { disputesApi } from '@/api/disputes';
import { filesApi } from '@/api/files';
import type { DisputeStatusType, DisputeEvidenceResponse } from '@/types';

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

export function DisputeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showEvidenceModal, setShowEvidenceModal] = useState(false);
  const [evidenceDescription, setEvidenceDescription] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  const { data: dispute, isLoading } = useQuery({
    queryKey: ['dispute', id],
    queryFn: () => disputesApi.getByOrderId(Number(id)),
    enabled: !!id,
  });

  const addEvidenceMutation = useMutation({
    mutationFn: async () => {
      if (!selectedFile || !dispute) return;
      setUploading(true);
      try {
        const uploadResult = await filesApi.uploadEvidence(selectedFile);
        await disputesApi.addEvidence(dispute.id, {
          fileUrl: uploadResult.url,
          fileName: selectedFile.name,
          fileType: selectedFile.type,
          fileSize: selectedFile.size,
          description: evidenceDescription || undefined,
        });
      } finally {
        setUploading(false);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dispute', id] });
      setShowEvidenceModal(false);
      setSelectedFile(null);
      setEvidenceDescription('');
    },
  });

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setShowEvidenceModal(true);
    }
  };

  const isImage = (fileType?: string) =>
    fileType?.startsWith('image/');

  if (isLoading) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto py-8 px-4">
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-32 bg-gray-200 rounded-xl animate-pulse" />
            ))}
          </div>
        </div>
      </Layout>
    );
  }

  if (!dispute) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto py-8 px-4 text-center">
          <AlertTriangle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-900">Спор не найден</h2>
          <Link to={`/orders/${id}`} className="text-primary-600 hover:underline mt-2 block">
            Вернуться к заказу
          </Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-4xl mx-auto py-8 px-4 space-y-6">
        {/* Back link */}
        <Link to={`/orders/${dispute.orderId}`} className="inline-flex items-center text-gray-600 hover:text-gray-900">
          <ArrowLeft className="w-4 h-4 mr-1" /> Назад к заказу
        </Link>

        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Спор по заказу</h1>
            <p className="text-gray-600 mt-1">{dispute.orderTitle}</p>
          </div>
          <Badge variant={statusVariants[dispute.status]}>
            {statusLabels[dispute.status]}
          </Badge>
        </div>

        {/* Dispute info */}
        <Card padding="lg">
          <div className="space-y-4">
            <div>
              <span className="text-sm text-gray-500">Инициатор:</span>
              <p className="font-medium text-gray-900">
                {dispute.openedByName} ({dispute.openedByRole === 'CLIENT' ? 'Заказчик' : 'Исполнитель'})
              </p>
            </div>
            <div>
              <span className="text-sm text-gray-500">Причина спора:</span>
              <p className="text-gray-900 mt-1 whitespace-pre-wrap">{dispute.reason}</p>
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

        {/* Under review info */}
        {dispute.status === 'UNDER_REVIEW' && dispute.adminName && (
          <Card padding="lg">
            <div className="flex items-center gap-2 mb-2">
              <Shield className="w-5 h-5 text-yellow-500" />
              <h2 className="text-lg font-semibold">На рассмотрении</h2>
            </div>
            <p className="text-gray-600">
              Модератор <span className="font-medium">{dispute.adminName}</span> рассматривает ваш спор.
            </p>
          </Card>
        )}

        {/* Evidence */}
        <Card padding="lg">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">
              Доказательства ({dispute.evidence.length})
            </h2>
            {dispute.status !== 'RESOLVED' && (
              <>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp,application/pdf"
                  className="hidden"
                  onChange={handleFileSelect}
                />
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Upload className="w-4 h-4 mr-1" /> Загрузить
                </Button>
              </>
            )}
          </div>

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
                        className="w-16 h-16 object-cover rounded border"
                      />
                    </a>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* Chat link */}
        {dispute.chatRoomId && (
          <Card padding="lg">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold">Чат</h2>
                <p className="text-gray-500 text-sm">Общайтесь с другой стороной в чате заказа</p>
              </div>
              <Link to="/chats">
                <Button variant="outline">
                  <MessageSquare className="w-4 h-4 mr-1" /> Открыть чат
                </Button>
              </Link>
            </div>
          </Card>
        )}
      </div>

      {/* Evidence upload modal */}
      <Modal
        isOpen={showEvidenceModal}
        onClose={() => {
          setShowEvidenceModal(false);
          setSelectedFile(null);
          setEvidenceDescription('');
        }}
        title="Загрузить доказательство"
      >
        <div className="space-y-4">
          {selectedFile && (
            <div className="p-3 bg-gray-50 rounded-lg">
              <p className="font-medium text-gray-900">{selectedFile.name}</p>
              <p className="text-sm text-gray-500">
                {(selectedFile.size / 1024 / 1024).toFixed(2)} МБ
              </p>
            </div>
          )}
          <Textarea
            label="Описание (необязательно)"
            placeholder="Опишите, что показывает этот файл..."
            rows={3}
            value={evidenceDescription}
            onChange={(e) => setEvidenceDescription(e.target.value)}
          />
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => {
            setShowEvidenceModal(false);
            setSelectedFile(null);
          }}>
            Отмена
          </Button>
          <Button
            onClick={() => addEvidenceMutation.mutate()}
            loading={uploading || addEvidenceMutation.isPending}
          >
            Загрузить
          </Button>
        </div>
      </Modal>
    </Layout>
  );
}
