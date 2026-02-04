import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, Users, Briefcase, Shield, ArrowRight } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card } from '@/components/ui';
import { categoriesApi } from '@/api/categories';
import { ordersApi } from '@/api/orders';

export function HomePage() {
  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.getAll,
  });

  const { data: ordersData } = useQuery({
    queryKey: ['orders', 'recent'],
    queryFn: () => ordersApi.getAll({}, 0, 6),
  });

  const topCategories = categories?.slice(0, 8) || [];
  const recentOrders = ordersData?.content || [];

  return (
    <Layout>
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-primary-600 to-primary-800 text-white py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto">
            <h1 className="text-4xl md:text-5xl font-bold mb-6">
              Найдите исполнителя для любой задачи
            </h1>
            <p className="text-xl text-primary-100 mb-8">
              FreelanceKG — первая биржа фриланса в Кыргызстане. Тысячи специалистов готовы помочь с вашими проектами.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/orders/create">
                <button className="px-6 py-3 text-base font-medium rounded-lg bg-white text-primary-600 hover:bg-gray-100 transition-colors">
                  Создать заказ
                </button>
              </Link>
              <Link to="/executors">
                <button className="px-6 py-3 text-base font-medium rounded-lg border border-white text-white hover:bg-white/10 transition-colors">
                  Найти исполнителя
                </button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Search Section */}
      <section className="py-8 -mt-8">
        <div className="max-w-3xl mx-auto px-4">
          <Card padding="sm">
            <div className="flex items-center gap-3">
              <Search className="w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Поиск заказов или исполнителей..."
                className="flex-1 outline-none text-lg"
              />
              <Button>Найти</Button>
            </div>
          </Card>
        </div>
      </section>

      {/* Features */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="text-center">
              <div className="w-16 h-16 bg-primary-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <Briefcase className="w-8 h-8 text-primary-600" />
              </div>
              <h3 className="text-xl font-semibold mb-2">Тысячи заказов</h3>
              <p className="text-gray-600">
                Новые заказы каждый день. Найдите работу по своему профилю.
              </p>
            </div>
            <div className="text-center">
              <div className="w-16 h-16 bg-green-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <Users className="w-8 h-8 text-green-600" />
              </div>
              <h3 className="text-xl font-semibold mb-2">Проверенные исполнители</h3>
              <p className="text-gray-600">
                Рейтинги и отзывы помогут выбрать лучшего специалиста.
              </p>
            </div>
            <div className="text-center">
              <div className="w-16 h-16 bg-blue-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <Shield className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl font-semibold mb-2">Безопасные сделки</h3>
              <p className="text-gray-600">
                Встроенный чат и система разрешения споров.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-2xl font-bold text-gray-900">Популярные категории</h2>
            <Link to="/categories" className="text-primary-600 hover:underline flex items-center gap-1">
              Все категории <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {topCategories.map((category) => (
              <Link key={category.id} to={`/orders?categoryId=${category.id}`}>
                <Card hover padding="md" className="text-center">
                  <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mx-auto mb-3">
                    {category.iconUrl ? (
                      <img src={category.iconUrl} alt="" className="w-6 h-6" />
                    ) : (
                      <Briefcase className="w-6 h-6 text-gray-400" />
                    )}
                  </div>
                  <h3 className="font-medium text-gray-900">{category.name}</h3>
                  {category.orderCount !== undefined && (
                    <p className="text-sm text-gray-500 mt-1">{category.orderCount} заказов</p>
                  )}
                </Card>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Recent Orders */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-2xl font-bold text-gray-900">Новые заказы</h2>
            <Link to="/orders" className="text-primary-600 hover:underline flex items-center gap-1">
              Все заказы <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {recentOrders.map((order) => (
              <Link key={order.id} to={`/orders/${order.id}`}>
                <Card hover padding="md">
                  <div className="flex items-start justify-between mb-3">
                    <span className="text-xs font-medium text-primary-600 bg-primary-50 px-2 py-1 rounded">
                      {order.categoryName}
                    </span>
                    {order.budgetMax && (
                      <span className="text-sm font-semibold text-gray-900">
                        до {order.budgetMax.toLocaleString()} сом
                      </span>
                    )}
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-2 line-clamp-2">{order.title}</h3>
                  <div className="flex items-center justify-between text-sm text-gray-500">
                    <span>{order.responseCount} откликов</span>
                    <span>{new Date(order.createdAt).toLocaleDateString('ru')}</span>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 bg-primary-600">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h2 className="text-3xl font-bold text-white mb-4">
            Готовы начать работу?
          </h2>
          <p className="text-xl text-primary-100 mb-8">
            Зарегистрируйтесь бесплатно и найдите первый заказ уже сегодня
          </p>
          <Link to="/register">
            <button className="px-6 py-3 text-base font-medium rounded-lg bg-white text-primary-600 hover:bg-gray-100 transition-colors">
              Создать аккаунт
            </button>
          </Link>
        </div>
      </section>
    </Layout>
  );
}
