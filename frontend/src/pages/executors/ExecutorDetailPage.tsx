import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Star, Clock, CheckCircle, MessageSquare, ExternalLink } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card, Avatar, Rating, Badge } from '@/components/ui';
import { executorsApi } from '@/api/executors';
import { useAuthStore } from '@/stores/authStore';

export function ExecutorDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();

  const { data: executor, isLoading, error } = useQuery({
    queryKey: ['executor', id],
    queryFn: () => executorsApi.getById(Number(id)),
    enabled: !!id,
  });

  const { data: reviews } = useQuery({
    queryKey: ['executor-reviews', id],
    queryFn: () => executorsApi.getReviews(Number(id), 0, 5),
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto px-4 py-8">
          <div className="animate-pulse">
            <div className="h-8 bg-gray-200 rounded w-1/4 mb-8" />
            <Card padding="lg">
              <div className="flex gap-6">
                <div className="w-24 h-24 bg-gray-200 rounded-full" />
                <div className="flex-1">
                  <div className="h-6 bg-gray-200 rounded w-1/3 mb-2" />
                  <div className="h-4 bg-gray-200 rounded w-1/4 mb-4" />
                  <div className="h-4 bg-gray-200 rounded w-full mb-2" />
                  <div className="h-4 bg-gray-200 rounded w-2/3" />
                </div>
              </div>
            </Card>
          </div>
        </div>
      </Layout>
    );
  }

  if (error || !executor) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto px-4 py-8">
          <Card padding="lg" className="text-center">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Исполнитель не найден</h2>
            <p className="text-gray-500 mb-4">Возможно, профиль был удалён или не существует</p>
            <Link to="/executors">
              <Button variant="outline">
                <ArrowLeft className="w-4 h-4 mr-2" />
                К списку исполнителей
              </Button>
            </Link>
          </Card>
        </div>
      </Layout>
    );
  }

  const isOwnProfile = user?.id === executor.id;

  return (
    <Layout>
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Back link */}
        <Link
          to="/executors"
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-6"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          К списку исполнителей
        </Link>

        {/* Main profile card */}
        <Card padding="lg" className="mb-6">
          <div className="flex flex-col md:flex-row gap-6">
            <Avatar
              src={executor.avatarUrl}
              name={executor.fullName}
              size="xl"
              className="w-24 h-24"
            />
            <div className="flex-1">
              <div className="flex items-start justify-between mb-2">
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">{executor.fullName}</h1>
                  {executor.specialization && (
                    <p className="text-gray-600">{executor.specialization}</p>
                  )}
                </div>
                {executor.availableForWork && (
                  <Badge variant="success">Доступен для работы</Badge>
                )}
              </div>

              <div className="flex items-center gap-4 mb-4">
                <div className="flex items-center gap-1">
                  <Rating value={executor.rating} size="sm" />
                  <span className="text-sm text-gray-600">
                    {executor.rating.toFixed(1)} ({executor.reviewCount} отзывов)
                  </span>
                </div>
              </div>

              {executor.bio && (
                <p className="text-gray-700 mb-4">{executor.bio}</p>
              )}

              {/* Categories */}
              {executor.categories && executor.categories.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-4">
                  {executor.categories.map((category) => (
                    <span
                      key={category.id}
                      className="text-sm bg-gray-100 text-gray-700 px-3 py-1 rounded-full"
                    >
                      {category.name}
                    </span>
                  ))}
                </div>
              )}

              {/* Contact buttons */}
              {!isOwnProfile && (
                <div className="flex flex-col gap-3">
                  <div className="flex gap-3">
                    <Button
                      onClick={() => {
                        if (!isAuthenticated) {
                          navigate('/login');
                        } else {
                          navigate('/orders/create');
                        }
                      }}
                    >
                      <MessageSquare className="w-4 h-4 mr-2" />
                      Создать заказ
                    </Button>
                    {executor.whatsappLink && (
                      <a
                        href={executor.whatsappLink}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <Button variant="outline">
                          WhatsApp
                          <ExternalLink className="w-4 h-4 ml-2" />
                        </Button>
                      </a>
                    )}
                  </div>
                  <p className="text-sm text-gray-500">
                    Для связи с исполнителем создайте заказ - после отклика откроется чат
                  </p>
                </div>
              )}
            </div>
          </div>
        </Card>

        {/* Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <Card padding="md" className="text-center">
            <div className="flex items-center justify-center w-10 h-10 bg-blue-100 rounded-full mx-auto mb-2">
              <CheckCircle className="w-5 h-5 text-blue-600" />
            </div>
            <p className="text-2xl font-bold text-gray-900">{executor.completedOrders}</p>
            <p className="text-sm text-gray-500">Выполнено</p>
          </Card>
          <Card padding="md" className="text-center">
            <div className="flex items-center justify-center w-10 h-10 bg-yellow-100 rounded-full mx-auto mb-2">
              <Star className="w-5 h-5 text-yellow-600" />
            </div>
            <p className="text-2xl font-bold text-gray-900">{executor.rating.toFixed(1)}</p>
            <p className="text-sm text-gray-500">Рейтинг</p>
          </Card>
          <Card padding="md" className="text-center">
            <div className="flex items-center justify-center w-10 h-10 bg-green-100 rounded-full mx-auto mb-2">
              <Clock className="w-5 h-5 text-green-600" />
            </div>
            <p className="text-2xl font-bold text-gray-900">
              {executor.avgCompletionDays ? `${executor.avgCompletionDays.toFixed(0)}д` : '-'}
            </p>
            <p className="text-sm text-gray-500">Ср. срок</p>
          </Card>
          <Card padding="md" className="text-center">
            <div className="flex items-center justify-center w-10 h-10 bg-purple-100 rounded-full mx-auto mb-2">
              <MessageSquare className="w-5 h-5 text-purple-600" />
            </div>
            <p className="text-2xl font-bold text-gray-900">{executor.reviewCount}</p>
            <p className="text-sm text-gray-500">Отзывов</p>
          </Card>
        </div>

        {/* Reviews */}
        {reviews && reviews.content.length > 0 && (
          <Card padding="lg">
            <h2 className="text-lg font-semibold mb-4">Отзывы</h2>
            <div className="space-y-4">
              {reviews.content.map((review) => (
                <div key={review.id} className="border-b border-gray-100 pb-4 last:border-0 last:pb-0">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-gray-900">{review.clientName}</span>
                      <Rating value={review.rating} size="sm" />
                    </div>
                    <span className="text-sm text-gray-500">
                      {new Date(review.createdAt).toLocaleDateString('ru-RU')}
                    </span>
                  </div>
                  {review.comment && (
                    <p className="text-gray-700">{review.comment}</p>
                  )}
                  <Link
                    to={`/orders/${review.orderId}`}
                    className="text-sm text-primary-600 hover:underline"
                  >
                    {review.orderTitle}
                  </Link>
                </div>
              ))}
            </div>
          </Card>
        )}

        {/* Member since */}
        {executor.memberSince && (
          <p className="text-center text-sm text-gray-500 mt-6">
            На платформе с {new Date(executor.memberSince).toLocaleDateString('ru-RU', { month: 'long', year: 'numeric' })}
          </p>
        )}
      </div>
    </Layout>
  );
}
