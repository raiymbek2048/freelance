import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Shield, Upload, CheckCircle, Clock, XCircle, Camera, FileText } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card } from '@/components/ui';
import { verificationApi } from '@/api/verification';
import { filesApi } from '@/api/files';
import { useAuthStore } from '@/stores/authStore';
import { Navigate } from 'react-router-dom';
import type { VerificationStatus } from '@/types';

const statusConfig: Record<VerificationStatus, { icon: React.ReactNode; color: string; title: string; description: string }> = {
  NONE: {
    icon: <Shield className="w-12 h-12 text-gray-400" />,
    color: 'bg-gray-50 border-gray-200',
    title: 'Верификация не пройдена',
    description: 'Пройдите расширенную верификацию, чтобы повысить уровень доверия со стороны заказчиков и получить доступ к дополнительным возможностям платформы.',
  },
  PENDING: {
    icon: <Clock className="w-12 h-12 text-amber-500" />,
    color: 'bg-amber-50 border-amber-200',
    title: 'Заявка на рассмотрении',
    description: 'Ваша заявка на верификацию находится на рассмотрении. Обычно это занимает 1-2 рабочих дня.',
  },
  APPROVED: {
    icon: <CheckCircle className="w-12 h-12 text-green-500" />,
    color: 'bg-green-50 border-green-200',
    title: 'Верификация пройдена',
    description: 'Поздравляем! Вы успешно прошли верификацию и можете откликаться на все заказы.',
  },
  REJECTED: {
    icon: <XCircle className="w-12 h-12 text-red-500" />,
    color: 'bg-red-50 border-red-200',
    title: 'Заявка отклонена',
    description: 'К сожалению, ваша заявка была отклонена. Вы можете подать новую заявку.',
  },
};

export function VerificationPage() {
  const { isAuthenticated, user, fetchUser } = useAuthStore();
  const queryClient = useQueryClient();
  const passportInputRef = useRef<HTMLInputElement>(null);
  const selfieInputRef = useRef<HTMLInputElement>(null);

  const [passportFile, setPassportFile] = useState<File | null>(null);
  const [selfieFile, setSelfieFile] = useState<File | null>(null);
  const [passportPreview, setPassportPreview] = useState<string | null>(null);
  const [selfiePreview, setSelfiePreview] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [consentGiven, setConsentGiven] = useState(false);

  const { data: status, isLoading } = useQuery({
    queryKey: ['verification-status'],
    queryFn: verificationApi.getMyStatus,
    enabled: isAuthenticated,
    // Refresh every 30 seconds while on PENDING status
    refetchInterval: (query) =>
      query.state.data?.status === 'PENDING' ? 30000 : false,
  });

  // Update user data in auth store when verification is approved
  useEffect(() => {
    if (status?.status === 'APPROVED' && user && !user.executorVerified) {
      fetchUser();
    }
  }, [status?.status, user, fetchUser]);

  const submitMutation = useMutation({
    mutationFn: async () => {
      if (!passportFile || !selfieFile) {
        throw new Error('Необходимо загрузить оба документа');
      }

      setUploadError(null);

      // Upload passport
      const passportResult = await filesApi.uploadVerification(passportFile, 'passport');
      // Upload selfie
      const selfieResult = await filesApi.uploadVerification(selfieFile, 'selfie');

      // Submit verification
      return verificationApi.submit({
        passportUrl: passportResult.url,
        selfieUrl: selfieResult.url,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['verification-status'] });
      setPassportFile(null);
      setSelfieFile(null);
      setPassportPreview(null);
      setSelfiePreview(null);
    },
    onError: (error: unknown) => {
      // Check for 413 Payload Too Large error
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { status?: number } };
        if (axiosError.response?.status === 413) {
          setUploadError('Размер файла не должен превышать 10 МБ');
          return;
        }
      }
      setUploadError(error instanceof Error ? error.message : 'Произошла ошибка при загрузке');
    },
  });

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  const handleFileSelect = (type: 'passport' | 'selfie', file: File) => {
    if (!file.type.startsWith('image/')) {
      setUploadError('Пожалуйста, выберите изображение');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setUploadError('Размер файла не должен превышать 10 МБ');
      return;
    }

    setUploadError(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      if (type === 'passport') {
        setPassportFile(file);
        setPassportPreview(e.target?.result as string);
      } else {
        setSelfieFile(file);
        setSelfiePreview(e.target?.result as string);
      }
    };
    reader.readAsDataURL(file);
  };

  const currentStatus = status?.status || 'NONE';
  const config = statusConfig[currentStatus];
  const canSubmit = currentStatus === 'NONE' || currentStatus === 'REJECTED';

  if (isLoading) {
    return (
      <Layout>
        <div className="max-w-2xl mx-auto px-4 py-8">
          <div className="animate-pulse space-y-4">
            <div className="h-8 bg-gray-200 rounded w-1/2 mx-auto" />
            <div className="h-32 bg-gray-200 rounded" />
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 text-center mb-8">
          Верификация исполнителя
        </h1>

        {/* Status Card */}
        <Card padding="lg" className={`mb-8 border ${config.color}`}>
          <div className="flex flex-col items-center text-center">
            {config.icon}
            <h2 className="text-xl font-semibold mt-4">{config.title}</h2>
            <p className="text-gray-600 mt-2">{config.description}</p>
            {status?.rejectionReason && (
              <div className="mt-4 p-3 bg-red-100 rounded-lg w-full">
                <p className="text-sm text-red-800">
                  <strong>Причина отказа:</strong> {status.rejectionReason}
                </p>
              </div>
            )}
          </div>
        </Card>

        {/* Submit Form */}
        {canSubmit && (
          <div className="bg-white rounded-2xl p-6 shadow-lg space-y-6">
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <h3 className="font-medium text-blue-900 mb-2">Как пройти верификацию</h3>
              <ol className="list-decimal list-inside text-sm text-blue-800 space-y-1">
                <li>Загрузите фото документа, удостоверяющего личность (страница с фотографией)</li>
                <li>Сделайте селфи для подтверждения личности</li>
                <li>Отправьте заявку на проверку</li>
              </ol>
            </div>

            {uploadError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                {uploadError}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Passport Upload */}
              <div
                onClick={() => passportInputRef.current?.click()}
                className={`border-2 border-dashed rounded-lg p-6 cursor-pointer transition-colors ${
                  passportPreview
                    ? 'border-green-300 bg-green-50'
                    : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
                }`}
              >
                <input
                  ref={passportInputRef}
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => e.target.files?.[0] && handleFileSelect('passport', e.target.files[0])}
                />
                {passportPreview ? (
                  <div className="text-center">
                    <img
                      src={passportPreview}
                      alt="Паспорт"
                      className="w-full h-32 object-cover rounded-lg mb-2"
                    />
                    <p className="text-sm text-green-600 font-medium">Документ загружен</p>
                  </div>
                ) : (
                  <div className="text-center">
                    <FileText className="w-12 h-12 text-gray-400 mx-auto mb-2" />
                    <p className="font-medium text-gray-700">Фото документа</p>
                    <p className="text-sm text-gray-500 mt-1">Нажмите для загрузки</p>
                  </div>
                )}
              </div>

              {/* Selfie Upload */}
              <div
                onClick={() => selfieInputRef.current?.click()}
                className={`border-2 border-dashed rounded-lg p-6 cursor-pointer transition-colors ${
                  selfiePreview
                    ? 'border-green-300 bg-green-50'
                    : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
                }`}
              >
                <input
                  ref={selfieInputRef}
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => e.target.files?.[0] && handleFileSelect('selfie', e.target.files[0])}
                />
                {selfiePreview ? (
                  <div className="text-center">
                    <img
                      src={selfiePreview}
                      alt="Селфи"
                      className="w-full h-32 object-cover rounded-lg mb-2"
                    />
                    <p className="text-sm text-green-600 font-medium">Селфи загружено</p>
                  </div>
                ) : (
                  <div className="text-center">
                    <Camera className="w-12 h-12 text-gray-400 mx-auto mb-2" />
                    <p className="font-medium text-gray-700">Фото для подтверждения</p>
                    <p className="text-sm text-gray-500 mt-1">Нажмите для загрузки</p>
                  </div>
                )}
              </div>
            </div>

            {/* Privacy Notice */}
            <div className="bg-cyan-50 border border-cyan-300 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <Shield className="w-6 h-6 text-cyan-600 flex-shrink-0" />
                <div>
                  <h4 className="font-semibold text-gray-900 mb-1">Конфиденциальность данных</h4>
                  <p className="text-sm text-gray-700 leading-relaxed">
                    Предоставленные данные используются исключительно в целях подтверждения личности
                    пользователя и повышения безопасности на платформе. Обработка персональных данных
                    осуществляется в соответствии с законодательством Кыргызской Республики. Данные
                    хранятся в защищённом виде и не передаются третьим лицам.
                  </p>
                </div>
              </div>
            </div>

            {/* Consent Checkbox */}
            <label className="flex items-start gap-3 cursor-pointer p-4 rounded-lg border border-gray-300 hover:border-cyan-400 hover:bg-gray-50 transition-colors">
              <input
                type="checkbox"
                checked={consentGiven}
                onChange={(e) => setConsentGiven(e.target.checked)}
                className="mt-0.5 w-5 h-5 rounded border-gray-400 text-cyan-600 focus:ring-cyan-500 cursor-pointer accent-cyan-600"
              />
              <span className="text-sm text-gray-700 leading-relaxed">
                Я подтверждаю, что предоставленные данные являются достоверными, и даю согласие
                на их обработку в целях верификации моей личности в соответствии с{' '}
                <a href="/privacy" className="text-cyan-600 underline hover:text-cyan-700">Политикой конфиденциальности</a>.
              </span>
            </label>

            <Button
              className="w-full"
              size="lg"
              onClick={() => submitMutation.mutate()}
              loading={submitMutation.isPending}
              disabled={!passportFile || !selfieFile || !consentGiven}
            >
              <Upload className="w-5 h-5 mr-2" />
              Отправить на верификацию
            </Button>

            <p className="text-xs text-gray-500 text-center leading-relaxed">
              Прохождение расширенной верификации является добровольным.
              Пользователи без верификации могут пользоваться базовым функционалом платформы.
            </p>
          </div>
        )}
      </div>
    </Layout>
  );
}
