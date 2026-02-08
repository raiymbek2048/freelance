import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  CreditCard, Settings, Users, ChevronLeft, ChevronRight, Gift, X, Megaphone, Calendar, Clock
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { Button, Card, Badge, Input, Textarea, Toggle, Modal } from '@/components/ui';
import { adminSubscriptionApi } from '@/api/subscription';
import type { SubscriptionStatus, UserSubscriptionResponse } from '@/types';

const statusLabels: Record<SubscriptionStatus, string> = {
  TRIAL: 'Пробный',
  ACTIVE: 'Активна',
  EXPIRED: 'Истекла',
};

const statusVariants: Record<SubscriptionStatus, 'default' | 'success' | 'warning' | 'error'> = {
  TRIAL: 'warning',
  ACTIVE: 'success',
  EXPIRED: 'error',
};

type TabType = 'settings' | 'users';

export function AdminSubscriptionPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabType>('settings');
  const [page, setPage] = useState(0);
  const [showGrantModal, setShowGrantModal] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [grantDays, setGrantDays] = useState('30');

  // Settings form state
  const [price, setPrice] = useState('');
  const [subscriptionStartDate, setSubscriptionStartDate] = useState('');
  const [trialDays, setTrialDays] = useState('');
  const [announcementMessage, setAnnouncementMessage] = useState('');
  const [announcementEnabled, setAnnouncementEnabled] = useState(false);

  // Load settings
  const { data: settings, isLoading: settingsLoading } = useQuery({
    queryKey: ['subscription-settings'],
    queryFn: adminSubscriptionApi.getSettings,
  });

  // Sync settings state when data loads
  useEffect(() => {
    if (settings) {
      setPrice(settings.price?.toString() || '');
      setSubscriptionStartDate(settings.subscriptionStartDate || '');
      setTrialDays(settings.trialDays?.toString() || '');
      setAnnouncementMessage(settings.announcementMessage || '');
      setAnnouncementEnabled(settings.announcementEnabled || false);
    }
  }, [settings]);

  // Load subscriptions
  const { data: subscriptions, isLoading: subscriptionsLoading } = useQuery({
    queryKey: ['admin-subscriptions', page],
    queryFn: () => adminSubscriptionApi.getSubscriptions(page, 20),
    enabled: activeTab === 'users',
  });

  // Update settings mutation
  const updateSettingsMutation = useMutation({
    mutationFn: adminSubscriptionApi.updateSettings,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription-settings'] });
      alert('Настройки сохранены');
    },
  });

  // Grant subscription mutation
  const grantMutation = useMutation({
    mutationFn: ({ userId, days }: { userId: number; days: number }) =>
      adminSubscriptionApi.grantSubscription(userId, { days }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-subscriptions'] });
      setShowGrantModal(false);
      setSelectedUserId(null);
      setGrantDays('30');
    },
  });

  // Revoke subscription mutation
  const revokeMutation = useMutation({
    mutationFn: (userId: number) => adminSubscriptionApi.revokeSubscription(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-subscriptions'] });
    },
  });

  const handleSaveSettings = () => {
    updateSettingsMutation.mutate({
      price: price ? parseFloat(price) : undefined,
      subscriptionStartDate: subscriptionStartDate || undefined,
      trialDays: trialDays ? parseInt(trialDays) : undefined,
      announcementMessage: announcementMessage || undefined,
      announcementEnabled,
    });
  };

  const handleGrant = () => {
    if (selectedUserId && grantDays) {
      grantMutation.mutate({ userId: selectedUserId, days: parseInt(grantDays) });
    }
  };

  const handleRevoke = (userId: number) => {
    if (confirm('Отозвать подписку у этого пользователя?')) {
      revokeMutation.mutate(userId);
    }
  };

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Управление подписками</h1>
            <p className="text-gray-600 mt-1">
              Настройки подписок и управление пользователями
            </p>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex gap-4 mb-6 border-b border-gray-200">
          <button
            onClick={() => setActiveTab('settings')}
            className={`px-4 py-2 -mb-px border-b-2 transition-colors ${
              activeTab === 'settings'
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Settings className="w-4 h-4 inline mr-2" />
            Настройки
          </button>
          <button
            onClick={() => setActiveTab('users')}
            className={`px-4 py-2 -mb-px border-b-2 transition-colors ${
              activeTab === 'users'
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Users className="w-4 h-4 inline mr-2" />
            Подписки пользователей
          </button>
        </div>

        {/* Settings Tab */}
        {activeTab === 'settings' && (
          <div className="space-y-6">
            {settingsLoading ? (
              <div className="animate-pulse space-y-4">
                <div className="h-32 bg-gray-200 rounded-lg" />
                <div className="h-32 bg-gray-200 rounded-lg" />
              </div>
            ) : (
              <>
                {/* Pricing Settings */}
                <Card padding="lg">
                  <div className="flex items-center gap-3 mb-6">
                    <CreditCard className="w-5 h-5 text-gray-600" />
                    <h2 className="text-lg font-semibold">Цена и триал</h2>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <Input
                      label="Цена подписки (сом)"
                      type="number"
                      value={price}
                      onChange={(e) => setPrice(e.target.value)}
                      placeholder="500"
                    />
                    <Input
                      label="Дней триала"
                      type="number"
                      value={trialDays}
                      onChange={(e) => setTrialDays(e.target.value)}
                      placeholder="7"
                    />
                    <Input
                      label="Дата начала обязательной подписки"
                      type="date"
                      value={subscriptionStartDate}
                      onChange={(e) => setSubscriptionStartDate(e.target.value)}
                    />
                  </div>

                  <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-sm text-blue-800">
                      {subscriptionStartDate ? (
                        <>
                          <Calendar className="w-4 h-4 inline mr-1" />
                          Подписка станет обязательной с{' '}
                          <strong>{format(new Date(subscriptionStartDate), 'd MMMM yyyy', { locale: ru })}</strong>
                        </>
                      ) : (
                        <>
                          <Clock className="w-4 h-4 inline mr-1" />
                          Подписка <strong>не обязательна</strong> - дата начала не установлена
                        </>
                      )}
                    </p>
                  </div>
                </Card>

                {/* Announcement Settings */}
                <Card padding="lg">
                  <div className="flex items-center gap-3 mb-6">
                    <Megaphone className="w-5 h-5 text-gray-600" />
                    <h2 className="text-lg font-semibold">Объявление на главной</h2>
                  </div>

                  <div className="space-y-4">
                    <Toggle
                      checked={announcementEnabled}
                      onChange={setAnnouncementEnabled}
                      label="Показывать объявление"
                      description="Объявление будет отображаться на главной странице"
                    />

                    <Textarea
                      label="Текст объявления"
                      value={announcementMessage}
                      onChange={(e) => setAnnouncementMessage(e.target.value)}
                      placeholder="Например: С 1 марта 2024 для работы на платформе потребуется оплатить подписку"
                      rows={3}
                    />

                    {announcementEnabled && announcementMessage && (
                      <div className="mt-4">
                        <p className="text-sm text-gray-600 mb-2">Предпросмотр:</p>
                        <div className="bg-amber-50 border border-amber-200 px-4 py-3 rounded-lg text-center">
                          <p className="text-amber-800">{announcementMessage}</p>
                        </div>
                      </div>
                    )}
                  </div>
                </Card>

                {/* Save Button */}
                <div className="flex justify-end">
                  <Button
                    onClick={handleSaveSettings}
                    loading={updateSettingsMutation.isPending}
                  >
                    Сохранить настройки
                  </Button>
                </div>
              </>
            )}
          </div>
        )}

        {/* Users Tab */}
        {activeTab === 'users' && (
          <div className="space-y-4">
            {subscriptionsLoading ? (
              <div className="animate-pulse space-y-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-20 bg-gray-200 rounded-lg" />
                ))}
              </div>
            ) : subscriptions?.content.length === 0 ? (
              <Card padding="lg" className="text-center">
                <CreditCard className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900">Нет подписок</h3>
                <p className="text-gray-500 mt-1">
                  Пользователи ещё не оформляли подписки
                </p>
              </Card>
            ) : (
              <>
                <div className="space-y-4">
                  {subscriptions?.content.map((sub: UserSubscriptionResponse) => (
                    <Card key={sub.id} padding="md">
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="font-medium text-gray-900">{sub.userFullName}</h3>
                          <p className="text-sm text-gray-500">{sub.userEmail}</p>
                        </div>

                        <div className="flex items-center gap-4">
                          <Badge variant={statusVariants[sub.status]}>
                            {statusLabels[sub.status]}
                          </Badge>
                          <div className="text-right">
                            <p className="text-sm text-gray-600">
                              До {format(new Date(sub.endDate), 'd MMM yyyy', { locale: ru })}
                            </p>
                            {sub.isActive && sub.daysRemaining > 0 && (
                              <p className="text-xs text-gray-500">
                                Осталось {sub.daysRemaining} дн.
                              </p>
                            )}
                          </div>
                          <div className="flex gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setSelectedUserId(sub.userId);
                                setShowGrantModal(true);
                              }}
                            >
                              <Gift className="w-4 h-4 mr-1" /> Продлить
                            </Button>
                            {sub.isActive && (
                              <Button
                                variant="danger"
                                size="sm"
                                onClick={() => handleRevoke(sub.userId)}
                                loading={revokeMutation.isPending}
                              >
                                <X className="w-4 h-4" />
                              </Button>
                            )}
                          </div>
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>

                {/* Pagination */}
                {subscriptions && subscriptions.totalPages > 1 && (
                  <div className="flex items-center justify-center gap-4 mt-6">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={subscriptions.first}
                      onClick={() => setPage(page - 1)}
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </Button>
                    <span className="text-sm text-gray-600">
                      Страница {page + 1} из {subscriptions.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={subscriptions.last}
                      onClick={() => setPage(page + 1)}
                    >
                      <ChevronRight className="w-4 h-4" />
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>

      {/* Grant Subscription Modal */}
      <Modal
        isOpen={showGrantModal}
        onClose={() => {
          setShowGrantModal(false);
          setSelectedUserId(null);
        }}
        title="Выдать подписку"
      >
        <div className="space-y-4">
          <Input
            label="Количество дней"
            type="number"
            value={grantDays}
            onChange={(e) => setGrantDays(e.target.value)}
            placeholder="30"
          />
          <div className="flex gap-3">
            <Button
              variant="outline"
              className="flex-1"
              onClick={() => setShowGrantModal(false)}
            >
              Отмена
            </Button>
            <Button
              className="flex-1"
              onClick={handleGrant}
              loading={grantMutation.isPending}
            >
              Выдать
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
