import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { User, Shield, Eye, EyeOff, Save, Mail, Phone, CheckCircle, Send } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card, Input, Textarea, Toggle } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { usersApi } from '@/api/users';
import type { UpdateProfileRequest } from '@/api/users';
import { Navigate, Link } from 'react-router-dom';
import { contactVerificationApi, type ContactVerificationType } from '@/api/contactVerification';

export function ProfilePage() {
  const { user, isAuthenticated, fetchUser } = useAuthStore();

  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [whatsappLink, setWhatsappLink] = useState(user?.whatsappLink || '');
  const [bio, setBio] = useState(user?.bio || '');
  const [hideFromExecutorList, setHideFromExecutorList] = useState(user?.hideFromExecutorList || false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Contact verification states
  const [emailCode, setEmailCode] = useState('');
  const [phoneCode, setPhoneCode] = useState('');
  const [emailCodeSent, setEmailCodeSent] = useState(false);
  const [phoneCodeSent, setPhoneCodeSent] = useState(false);
  const [verificationError, setVerificationError] = useState<string | null>(null);
  const [verificationSuccess, setVerificationSuccess] = useState<string | null>(null);

  const updateMutation = useMutation({
    mutationFn: (data: UpdateProfileRequest) => usersApi.updateProfile(data),
    onSuccess: () => {
      fetchUser();
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    },
  });

  const sendCodeMutation = useMutation({
    mutationFn: (type: ContactVerificationType) => contactVerificationApi.sendCode(type),
    onSuccess: (_, type) => {
      if (type === 'EMAIL') {
        setEmailCodeSent(true);
      } else {
        setPhoneCodeSent(true);
      }
      setVerificationError(null);
      setVerificationSuccess('Код отправлен');
      setTimeout(() => setVerificationSuccess(null), 3000);
    },
    onError: (error: Error) => {
      setVerificationError(error.message || 'Ошибка отправки кода');
      setTimeout(() => setVerificationError(null), 5000);
    },
  });

  const verifyCodeMutation = useMutation({
    mutationFn: ({ type, code }: { type: ContactVerificationType; code: string }) =>
      contactVerificationApi.verifyCode(type, code),
    onSuccess: (response, { type }) => {
      if (response.success) {
        fetchUser();
        if (type === 'EMAIL') {
          setEmailCodeSent(false);
          setEmailCode('');
        } else {
          setPhoneCodeSent(false);
          setPhoneCode('');
        }
        setVerificationSuccess('Успешно подтверждено!');
        setVerificationError(null);
        setTimeout(() => setVerificationSuccess(null), 3000);
      } else {
        setVerificationError(response.message);
        setTimeout(() => setVerificationError(null), 5000);
      }
    },
    onError: (error: Error) => {
      setVerificationError(error.message || 'Неверный код');
      setTimeout(() => setVerificationError(null), 5000);
    },
  });

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate({
      fullName: fullName.trim(),
      phone: phone.trim() || undefined,
      whatsappLink: whatsappLink.trim() || undefined,
      hideFromExecutorList,
      bio: bio.trim() || undefined,
    });
  };

  const hasChanges =
    fullName !== user?.fullName ||
    phone !== (user?.phone || '') ||
    whatsappLink !== (user?.whatsappLink || '') ||
    bio !== (user?.bio || '') ||
    hideFromExecutorList !== user?.hideFromExecutorList;

  return (
    <Layout>
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-8">Настройки профиля</h1>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Personal Info */}
          <Card padding="lg">
            <div className="flex items-center gap-3 mb-6">
              <User className="w-5 h-5 text-gray-600" />
              <h2 className="text-lg font-semibold">Личные данные</h2>
            </div>

            <div className="space-y-4">
              <Input
                label="Полное имя"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Иван Иванов"
              />

              <Input
                label="Телефон"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+996 XXX XXX XXX"
              />

              <Input
                label="Ссылка на WhatsApp"
                value={whatsappLink}
                onChange={(e) => setWhatsappLink(e.target.value)}
                placeholder="https://wa.me/996..."
              />

              <div className="text-sm text-gray-500">
                Email: <span className="font-medium text-gray-700">{user?.email}</span>
              </div>

              <div>
                <Textarea
                  label="О себе"
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  placeholder="Расскажите, какие задачи вы выполняете и в чём ваша специализация..."
                  rows={3}
                />
                <p className="text-xs text-gray-400 mt-1">
                  Необязательно. Это описание будет видно на вашем профиле исполнителя.
                </p>
              </div>
            </div>
          </Card>

          {/* Contact Verification */}
          <Card padding="lg">
            <div className="flex items-center gap-3 mb-6">
              <CheckCircle className="w-5 h-5 text-gray-600" />
              <h2 className="text-lg font-semibold">Подтверждение контактов</h2>
            </div>

            {verificationError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
                {verificationError}
              </div>
            )}
            {verificationSuccess && (
              <div className="mb-4 p-3 bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg">
                {verificationSuccess}
              </div>
            )}

            <div className="space-y-4">
              {/* Email Verification */}
              <div className="flex items-center justify-between gap-2 p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3 min-w-0 flex-1">
                  <Mail className="w-5 h-5 text-gray-500 flex-shrink-0" />
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate">{user?.email}</p>
                    <p className="text-sm text-gray-500">Email</p>
                  </div>
                </div>
                {user?.emailVerified ? (
                  <span className="flex items-center gap-1 text-green-600 text-sm font-medium flex-shrink-0">
                    <CheckCircle className="w-4 h-4" />
                    Подтверждён
                  </span>
                ) : emailCodeSent ? (
                  <div className="flex items-center gap-2">
                    <Input
                      value={emailCode}
                      onChange={(e) => setEmailCode(e.target.value)}
                      placeholder="Код"
                      className="w-24"
                      maxLength={6}
                    />
                    <Button
                      size="sm"
                      onClick={() => verifyCodeMutation.mutate({ type: 'EMAIL', code: emailCode })}
                      loading={verifyCodeMutation.isPending}
                      disabled={emailCode.length !== 6}
                    >
                      OK
                    </Button>
                  </div>
                ) : (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => sendCodeMutation.mutate('EMAIL')}
                    loading={sendCodeMutation.isPending}
                  >
                    <Send className="w-4 h-4 mr-1" />
                    Подтвердить
                  </Button>
                )}
              </div>

              {/* Phone Verification */}
              <div className="flex items-center justify-between gap-2 p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3 min-w-0 flex-1">
                  <Phone className="w-5 h-5 text-gray-500 flex-shrink-0" />
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate">{user?.phone || 'Не указан'}</p>
                    <p className="text-sm text-gray-500">Телефон</p>
                  </div>
                </div>
                {!user?.phone ? (
                  <span className="text-sm text-gray-400 flex-shrink-0">Укажите телефон выше</span>
                ) : user?.phoneVerified ? (
                  <span className="flex items-center gap-1 text-green-600 text-sm font-medium flex-shrink-0">
                    <CheckCircle className="w-4 h-4" />
                    Подтверждён
                  </span>
                ) : phoneCodeSent ? (
                  <div className="flex items-center gap-2">
                    <Input
                      value={phoneCode}
                      onChange={(e) => setPhoneCode(e.target.value)}
                      placeholder="Код"
                      className="w-24"
                      maxLength={6}
                    />
                    <Button
                      size="sm"
                      onClick={() => verifyCodeMutation.mutate({ type: 'PHONE', code: phoneCode })}
                      loading={verifyCodeMutation.isPending}
                      disabled={phoneCode.length !== 6}
                    >
                      OK
                    </Button>
                  </div>
                ) : (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => sendCodeMutation.mutate('PHONE')}
                    loading={sendCodeMutation.isPending}
                  >
                    <Send className="w-4 h-4 mr-1" />
                    Подтвердить
                  </Button>
                )}
              </div>

              <p className="text-xs text-gray-500 mt-2">
                Подтверждённые контакты повышают доверие к вашему профилю
              </p>
            </div>
          </Card>

          {/* Executor Settings */}
          <Card padding="lg">
            <div className="flex items-center gap-3 mb-6">
              {hideFromExecutorList ? (
                <EyeOff className="w-5 h-5 text-gray-600" />
              ) : (
                <Eye className="w-5 h-5 text-gray-600" />
              )}
              <h2 className="text-lg font-semibold">Настройки исполнителя</h2>
            </div>

            <div className="space-y-4">
              <Toggle
                checked={!hideFromExecutorList}
                onChange={(checked) => setHideFromExecutorList(!checked)}
                label="Показывать в списке исполнителей"
                description="Когда включено, ваш профиль будет виден в разделе «Исполнители» и заказчики смогут найти вас"
              />

              {!user?.executorVerified && (
                <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
                  <p className="text-sm text-amber-800">
                    Ваш профиль появится в списке исполнителей только после прохождения <strong>верификации</strong>.{' '}
                    <Link to="/verification" className="underline font-medium">Пройти верификацию</Link>
                  </p>
                </div>
              )}

              <div className="border-t border-gray-200 pt-4 mt-4">
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <p className="text-sm text-blue-800">
                    {hideFromExecutorList ? (
                      <>
                        Ваш профиль <strong>скрыт</strong> из списка исполнителей.
                        Вы по-прежнему можете откликаться на заказы.
                      </>
                    ) : (
                      <>
                        Ваш профиль <strong>виден</strong> в списке исполнителей.
                        Заказчики могут найти вас и пригласить к сотрудничеству.
                      </>
                    )}
                  </p>
                </div>
              </div>
            </div>
          </Card>

          {/* Verification */}
          <Card padding="lg">
            <div className="flex items-center gap-3 mb-4">
              <Shield className="w-5 h-5 text-gray-600" />
              <h2 className="text-lg font-semibold">Верификация</h2>
            </div>

            <p className="text-sm text-gray-600 mb-4">
              Верифицированные исполнители получают доступ к полному описанию заказов
              и могут откликаться на все заказы.
            </p>

            <Link to="/verification">
              <Button variant="outline" type="button">
                Перейти к верификации
              </Button>
            </Link>
          </Card>

          {/* Save Button */}
          <div className="flex items-center justify-between">
            {saveSuccess && (
              <span className="text-green-600 text-sm font-medium">
                Изменения сохранены!
              </span>
            )}
            {updateMutation.isError && (
              <span className="text-red-600 text-sm">
                Ошибка сохранения. Попробуйте ещё раз.
              </span>
            )}
            {!saveSuccess && !updateMutation.isError && <span />}

            <Button
              type="submit"
              loading={updateMutation.isPending}
              disabled={!hasChanges}
            >
              <Save className="w-4 h-4 mr-2" />
              Сохранить изменения
            </Button>
          </div>
        </form>
      </div>
    </Layout>
  );
}
