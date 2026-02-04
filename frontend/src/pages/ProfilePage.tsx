import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { User, Shield, Eye, EyeOff, Save } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card, Input, Toggle } from '@/components/ui';
import { useAuthStore } from '@/stores/authStore';
import { usersApi } from '@/api/users';
import type { UpdateProfileRequest } from '@/api/users';
import { Navigate, Link } from 'react-router-dom';

export function ProfilePage() {
  const { user, isAuthenticated, fetchUser } = useAuthStore();

  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [whatsappLink, setWhatsappLink] = useState(user?.whatsappLink || '');
  const [hideFromExecutorList, setHideFromExecutorList] = useState(user?.hideFromExecutorList || false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const updateMutation = useMutation({
    mutationFn: (data: UpdateProfileRequest) => usersApi.updateProfile(data),
    onSuccess: () => {
      fetchUser();
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
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
    });
  };

  const hasChanges =
    fullName !== user?.fullName ||
    phone !== (user?.phone || '') ||
    whatsappLink !== (user?.whatsappLink || '') ||
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
