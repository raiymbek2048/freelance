import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Users,
  Briefcase,
  AlertTriangle,
  TrendingUp,
  DollarSign,
  Star,
  Clock,
  CheckCircle,
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { adminApi } from '@/api/admin';

function StatCard({
  title,
  value,
  icon: Icon,
  color,
  change,
  link,
}: {
  title: string;
  value: string | number;
  icon: React.ElementType;
  color: string;
  change?: string;
  link?: string;
}) {
  const content = (
    <div className="bg-white rounded-xl shadow-sm p-6 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
          {change && <p className="text-sm text-green-600 mt-1">{change}</p>}
        </div>
        <div className={`p-3 rounded-lg ${color}`}>
          <Icon className="w-6 h-6 text-white" />
        </div>
      </div>
    </div>
  );

  if (link) {
    return <Link to={link}>{content}</Link>;
  }
  return content;
}

export function AdminDashboardPage() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => adminApi.getStats(),
  });

  if (isLoading) {
    return (
      <AdminLayout>
        <div className="animate-pulse space-y-6">
          <div className="h-8 bg-gray-200 rounded w-48" />
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="h-32 bg-gray-200 rounded-xl" />
            ))}
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Дашборд</h1>
          <p className="text-gray-500">Обзор платформы FreelanceKG</p>
        </div>

        {/* Main Stats */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Всего пользователей"
            value={stats?.totalUsers || 0}
            icon={Users}
            color="bg-blue-500"
            change={`+${stats?.newUsersToday || 0} сегодня`}
            link="/admin/users"
          />
          <StatCard
            title="Всего заказов"
            value={stats?.totalOrders || 0}
            icon={Briefcase}
            color="bg-green-500"
            link="/admin/orders"
          />
          <StatCard
            title="Активные споры"
            value={stats?.disputedOrders || 0}
            icon={AlertTriangle}
            color="bg-red-500"
            link="/admin/disputes"
          />
          <StatCard
            title="Средний чек"
            value={`${(stats?.averageOrderValue || 0).toLocaleString()} сом`}
            icon={DollarSign}
            color="bg-purple-500"
          />
        </div>

        {/* Orders Stats */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Статусы заказов</h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full bg-blue-500" />
                  <span className="text-gray-700">Новые</span>
                </div>
                <span className="font-semibold">{stats?.newOrders || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full bg-yellow-500" />
                  <span className="text-gray-700">В работе</span>
                </div>
                <span className="font-semibold">{stats?.inProgressOrders || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full bg-green-500" />
                  <span className="text-gray-700">Завершены</span>
                </div>
                <span className="font-semibold">{stats?.completedOrders || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full bg-red-500" />
                  <span className="text-gray-700">Споры</span>
                </div>
                <span className="font-semibold">{stats?.disputedOrders || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full bg-gray-400" />
                  <span className="text-gray-700">Отменены</span>
                </div>
                <span className="font-semibold">{stats?.cancelledOrders || 0}</span>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Пользователи</h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Users className="w-5 h-5 text-gray-400" />
                  <span className="text-gray-700">Активных</span>
                </div>
                <span className="font-semibold">{stats?.activeUsers || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Briefcase className="w-5 h-5 text-gray-400" />
                  <span className="text-gray-700">Исполнителей</span>
                </div>
                <span className="font-semibold">{stats?.executors || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Clock className="w-5 h-5 text-gray-400" />
                  <span className="text-gray-700">Новых за неделю</span>
                </div>
                <span className="font-semibold">{stats?.newUsersThisWeek || 0}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <TrendingUp className="w-5 h-5 text-gray-400" />
                  <span className="text-gray-700">Новых за месяц</span>
                </div>
                <span className="font-semibold">{stats?.newUsersThisMonth || 0}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Additional Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center gap-3 mb-4">
              <DollarSign className="w-6 h-6 text-green-600" />
              <h2 className="text-lg font-semibold text-gray-900">Финансы</h2>
            </div>
            <p className="text-3xl font-bold text-gray-900">
              {(stats?.totalOrdersValue || 0).toLocaleString()} сом
            </p>
            <p className="text-sm text-gray-500 mt-1">Общая сумма заказов</p>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center gap-3 mb-4">
              <Star className="w-6 h-6 text-yellow-500" />
              <h2 className="text-lg font-semibold text-gray-900">Отзывы</h2>
            </div>
            <p className="text-3xl font-bold text-gray-900">{stats?.totalReviews || 0}</p>
            <p className="text-sm text-gray-500 mt-1">
              Средний рейтинг: {(stats?.averageRating || 0).toFixed(1)}
            </p>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center gap-3 mb-4">
              <CheckCircle className="w-6 h-6 text-blue-600" />
              <h2 className="text-lg font-semibold text-gray-900">Модерация</h2>
            </div>
            <p className="text-3xl font-bold text-gray-900">{stats?.pendingModeration || 0}</p>
            <p className="text-sm text-gray-500 mt-1">Ожидают проверки</p>
          </div>
        </div>

        {/* Top Categories */}
        {stats?.topCategories && stats.topCategories.length > 0 && (
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Топ категорий</h2>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                      Категория
                    </th>
                    <th className="text-right py-3 px-4 text-sm font-medium text-gray-500">
                      Заказов
                    </th>
                    <th className="text-right py-3 px-4 text-sm font-medium text-gray-500">
                      Исполнителей
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {stats.topCategories.map((cat) => (
                    <tr key={cat.categoryId} className="border-b border-gray-100">
                      <td className="py-3 px-4 text-gray-900">{cat.categoryName}</td>
                      <td className="py-3 px-4 text-right text-gray-700">{cat.orderCount}</td>
                      <td className="py-3 px-4 text-right text-gray-700">{cat.executorCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
