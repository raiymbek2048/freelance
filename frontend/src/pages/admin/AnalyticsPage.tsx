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
  Download,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  Legend,
} from 'recharts';
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

const PIE_COLORS = ['#10b981', '#f59e0b', '#9ca3af'];

export function AdminAnalyticsPage() {
  const [period, setPeriod] = useState<Period>('monthly');
  const [exporting, setExporting] = useState(false);

  const { data: analytics, isLoading } = useQuery({
    queryKey: ['admin-analytics'],
    queryFn: () => adminApi.getAnalytics(),
  });

  const { data: stats } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => adminApi.getStats(),
  });

  const handleExportCsv = async () => {
    setExporting(true);
    try {
      await adminApi.exportAnalyticsCsv();
    } finally {
      setExporting(false);
    }
  };

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
        completed: d.completedOrders,
        revenue: d.revenue,
      }));
    }
    if (period === 'weekly' && analytics?.weeklyStats) {
      return analytics.weeklyStats.map((d) => ({
        label: new Date(d.weekStart).toLocaleDateString('ru', { day: 'numeric', month: 'short' }),
        users: d.newUsers,
        orders: d.newOrders,
        completed: d.completedOrders,
        revenue: d.revenue,
      }));
    }
    if (period === 'monthly' && analytics?.monthlyStats) {
      return analytics.monthlyStats.map((d: MonthlyStats) => ({
        label: `${d.monthName} ${d.year}`,
        users: d.newUsers,
        orders: d.newOrders,
        completed: d.completedOrders,
        revenue: d.revenue,
      }));
    }
    return [];
  };

  const chartData = getChartData();

  // Pie chart data for subscription breakdown
  const pieData = subscriptions
    ? [
        { name: 'Активные', value: subscriptions.activeSubscriptions },
        { name: 'Триал', value: subscriptions.trialSubscriptions },
        { name: 'Истекшие', value: subscriptions.expiredSubscriptions },
      ].filter((d) => d.value > 0)
    : [];

  // Top categories data
  const categoryData = stats?.topCategories?.map((cat) => ({
    name: cat.categoryName.length > 15 ? cat.categoryName.slice(0, 15) + '...' : cat.categoryName,
    orders: cat.orderCount,
  })) || [];

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
              onClick={handleExportCsv}
              disabled={exporting}
              className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors disabled:opacity-50"
            >
              <Download className="w-4 h-4" />
              {exporting ? 'Экспорт...' : 'CSV'}
            </button>
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

        {/* Charts — Users & Orders */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Пользователи и заказы</h3>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="colorOrders" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Area
                  type="monotone"
                  dataKey="users"
                  name="Пользователи"
                  stroke="#3b82f6"
                  fillOpacity={1}
                  fill="url(#colorUsers)"
                />
                <Area
                  type="monotone"
                  dataKey="orders"
                  name="Заказы"
                  stroke="#10b981"
                  fillOpacity={1}
                  fill="url(#colorOrders)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Revenue Chart */}
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Доход (сом)</h3>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip formatter={(value) => [`${Number(value).toLocaleString()} сом`, 'Доход']} />
                <Area
                  type="monotone"
                  dataKey="revenue"
                  name="Доход"
                  stroke="#8b5cf6"
                  fillOpacity={1}
                  fill="url(#colorRevenue)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Subscriptions Pie + Top Categories Bar */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Subscription Distribution */}
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Распределение подписок</h3>
            {pieData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={5}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                  >
                    {pieData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-[300px] text-gray-400">
                Нет данных о подписках
              </div>
            )}
          </div>

          {/* Top Categories */}
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Топ категории по заказам</h3>
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={categoryData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis type="number" tick={{ fontSize: 12 }} />
                  <YAxis dataKey="name" type="category" width={120} tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="orders" name="Заказы" fill="#06b6d4" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-[300px] text-gray-400">
                Нет данных о категориях
              </div>
            )}
          </div>
        </div>

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
