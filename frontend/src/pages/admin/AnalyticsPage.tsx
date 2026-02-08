import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  TrendingUp,
  Users,
  DollarSign,
  ArrowUpRight,
  ArrowDownRight,
  CreditCard,
  UserCheck,
  CheckCircle,
  Target,
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { adminApi, type MonthlyStats } from '@/api/admin';

type Period = 'daily' | 'weekly' | 'monthly';

function StatCard({
  title,
  value,
  change,
  changeLabel,
  icon: Icon,
  color,
}: {
  title: string;
  value: string;
  change?: number;
  changeLabel?: string;
  icon: React.ElementType;
  color: string;
}) {
  const isPositive = change && change >= 0;

  return (
    <div className="bg-white rounded-xl shadow-sm p-6">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
          {change !== undefined && (
            <div className={`flex items-center gap-1 mt-2 text-sm ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
              {isPositive ? <ArrowUpRight className="w-4 h-4" /> : <ArrowDownRight className="w-4 h-4" />}
              <span>{Math.abs(change).toFixed(1)}%</span>
              {changeLabel && <span className="text-gray-500">{changeLabel}</span>}
            </div>
          )}
        </div>
        <div className={`p-3 rounded-lg ${color}`}>
          <Icon className="w-6 h-6 text-white" />
        </div>
      </div>
    </div>
  );
}

function SimpleBarChart({
  data,
  label,
  color = 'bg-cyan-500',
}: {
  data: { label: string; value: number }[];
  label: string;
  color?: string;
}) {
  const maxValue = Math.max(...data.map((d) => d.value), 1);

  return (
    <div className="bg-white rounded-xl shadow-sm p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">{label}</h3>
      <div className="space-y-3">
        {data.map((item, index) => (
          <div key={index} className="flex items-center gap-3">
            <span className="w-16 text-xs text-gray-500 text-right">{item.label}</span>
            <div className="flex-1 h-6 bg-gray-100 rounded-full overflow-hidden">
              <div
                className={`h-full ${color} rounded-full transition-all duration-500`}
                style={{ width: `${(item.value / maxValue) * 100}%` }}
              />
            </div>
            <span className="w-12 text-sm font-medium text-gray-700">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ConversionCard({
  title,
  value,
  icon: Icon,
  description,
}: {
  title: string;
  value: number;
  icon: React.ElementType;
  description: string;
}) {
  return (
    <div className="bg-white rounded-xl shadow-sm p-6">
      <div className="flex items-center gap-3 mb-3">
        <Icon className="w-5 h-5 text-gray-400" />
        <span className="text-sm text-gray-500">{title}</span>
      </div>
      <div className="flex items-end gap-2">
        <span className="text-3xl font-bold text-gray-900">{value.toFixed(1)}%</span>
      </div>
      <p className="text-xs text-gray-500 mt-2">{description}</p>
      <div className="mt-3 h-2 bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full bg-cyan-500 rounded-full"
          style={{ width: `${Math.min(value, 100)}%` }}
        />
      </div>
    </div>
  );
}

export function AdminAnalyticsPage() {
  const [period, setPeriod] = useState<Period>('monthly');

  const { data: analytics, isLoading } = useQuery({
    queryKey: ['admin-analytics'],
    queryFn: () => adminApi.getAnalytics(),
  });

  if (isLoading) {
    return (
      <AdminLayout>
        <div className="animate-pulse space-y-6">
          <div className="h-8 bg-gray-200 rounded w-48" />
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-32 bg-gray-200 rounded-xl" />
            ))}
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="h-80 bg-gray-200 rounded-xl" />
            <div className="h-80 bg-gray-200 rounded-xl" />
          </div>
        </div>
      </AdminLayout>
    );
  }

  const subscriptions = analytics?.subscriptions;
  const conversions = analytics?.conversions;

  // Calculate revenue change
  const revenueChange = subscriptions?.revenueLastMonth
    ? ((subscriptions.revenueThisMonth - subscriptions.revenueLastMonth) / subscriptions.revenueLastMonth) * 100
    : 0;

  // Prepare chart data based on period
  const getChartData = () => {
    if (period === 'daily' && analytics?.dailyStats) {
      return analytics.dailyStats.slice(-14).map((d) => ({
        label: new Date(d.date).toLocaleDateString('ru', { day: 'numeric', month: 'short' }),
        users: d.newUsers,
        orders: d.newOrders,
        revenue: d.revenue,
      }));
    }
    if (period === 'weekly' && analytics?.weeklyStats) {
      return analytics.weeklyStats.map((d) => ({
        label: new Date(d.weekStart).toLocaleDateString('ru', { day: 'numeric', month: 'short' }),
        users: d.newUsers,
        orders: d.newOrders,
        revenue: d.revenue,
      }));
    }
    if (period === 'monthly' && analytics?.monthlyStats) {
      return analytics.monthlyStats.map((d: MonthlyStats) => ({
        label: `${d.monthName} ${d.year}`,
        users: d.newUsers,
        orders: d.newOrders,
        revenue: d.revenue,
      }));
    }
    return [];
  };

  const chartData = getChartData();

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Аналитика</h1>
            <p className="text-gray-500">Детальная статистика платформы</p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setPeriod('daily')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                period === 'daily' ? 'bg-cyan-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              По дням
            </button>
            <button
              onClick={() => setPeriod('weekly')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                period === 'weekly' ? 'bg-cyan-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              По неделям
            </button>
            <button
              onClick={() => setPeriod('monthly')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                period === 'monthly' ? 'bg-cyan-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              По месяцам
            </button>
          </div>
        </div>

        {/* Revenue Stats */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Доход за этот месяц"
            value={`${(subscriptions?.revenueThisMonth || 0).toLocaleString()} сом`}
            change={revenueChange}
            changeLabel="vs прошлый"
            icon={DollarSign}
            color="bg-green-500"
          />
          <StatCard
            title="Общий доход"
            value={`${(subscriptions?.totalRevenue || 0).toLocaleString()} сом`}
            icon={TrendingUp}
            color="bg-blue-500"
          />
          <StatCard
            title="Активные подписки"
            value={String(subscriptions?.activeSubscriptions || 0)}
            icon={CreditCard}
            color="bg-purple-500"
          />
          <StatCard
            title="Триал подписки"
            value={String(subscriptions?.trialSubscriptions || 0)}
            icon={Users}
            color="bg-amber-500"
          />
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <SimpleBarChart
            data={chartData.map((d) => ({ label: d.label, value: d.users }))}
            label="Новые пользователи"
            color="bg-blue-500"
          />
          <SimpleBarChart
            data={chartData.map((d) => ({ label: d.label, value: d.orders }))}
            label="Новые заказы"
            color="bg-green-500"
          />
        </div>

        {/* Revenue Chart */}
        <SimpleBarChart
          data={chartData.map((d) => ({ label: d.label, value: d.revenue }))}
          label="Доход (сом)"
          color="bg-purple-500"
        />

        {/* Conversion Rates */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Конверсии</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <ConversionCard
              title="Регистрация → Исполнитель"
              value={conversions?.registrationToExecutorRate || 0}
              icon={Users}
              description="Пользователей стали исполнителями"
            />
            <ConversionCard
              title="Исполнитель → Верификация"
              value={conversions?.executorToVerifiedRate || 0}
              icon={UserCheck}
              description="Исполнителей прошли верификацию"
            />
            <ConversionCard
              title="Завершение заказов"
              value={conversions?.orderCompletionRate || 0}
              icon={CheckCircle}
              description="Заказов успешно завершено"
            />
            <ConversionCard
              title="Отклик → Выбор"
              value={conversions?.responseToSelectionRate || 0}
              icon={Target}
              description="Откликов привели к выбору"
            />
          </div>
        </div>

        {/* Subscription Details */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Статистика подписок</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-gray-900">{subscriptions?.totalSubscriptions || 0}</p>
              <p className="text-sm text-gray-500 mt-1">Всего подписок</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-green-600">{subscriptions?.activeSubscriptions || 0}</p>
              <p className="text-sm text-gray-500 mt-1">Активных</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-amber-600">{subscriptions?.trialSubscriptions || 0}</p>
              <p className="text-sm text-gray-500 mt-1">На триале</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-gray-400">{subscriptions?.expiredSubscriptions || 0}</p>
              <p className="text-sm text-gray-500 mt-1">Истекших</p>
            </div>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
