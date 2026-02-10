import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Users } from 'lucide-react';
import { Layout } from '@/components/layout';
import { PageMeta } from '@/components/PageMeta';
import { Button, Card, Input, Select, Avatar, Rating, Badge } from '@/components/ui';
import { executorsApi } from '@/api/executors';
import { categoriesApi } from '@/api/categories';
import type { ExecutorFilters } from '@/types';

export function ExecutorsListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | undefined>(undefined);

  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.getAll,
  });

  const filters: ExecutorFilters = {
    minRating: searchParams.get('minRating') ? Number(searchParams.get('minRating')) : undefined,
    search: searchParams.get('search') || undefined,
    categoryId: selectedCategoryId,
  };

  const { data: executorsData, isLoading } = useQuery({
    queryKey: ['executors', filters, page],
    queryFn: () => executorsApi.getAll(filters, page, 20),
  });

  const updateFilter = (key: keyof ExecutorFilters, value: string | number | boolean | undefined) => {
    const params = new URLSearchParams(searchParams);
    if (value !== undefined && value !== '' && value !== false) {
      params.set(key, String(value));
    } else {
      params.delete(key);
    }
    setSearchParams(params);
    setPage(0);
  };

  const ratingOptions = [
    { value: '', label: 'Любой рейтинг' },
    { value: '1', label: 'От 1 звезды' },
    { value: '2', label: 'От 2 звёзд' },
    { value: '3', label: 'От 3 звёзд' },
    { value: '4', label: 'От 4 звёзд' },
    { value: '5', label: '5 звёзд' },
  ];

  const reputationColorMap: Record<string, string> = {
    gray: 'bg-gray-100 text-gray-700',
    blue: 'bg-blue-100 text-blue-700',
    green: 'bg-green-100 text-green-700',
    purple: 'bg-purple-100 text-purple-700',
    amber: 'bg-amber-100 text-amber-700',
  };

  return (
    <Layout>
      <PageMeta title="Исполнители" description="Каталог верифицированных исполнителей на FreelanceKG. Найдите специалиста для вашей задачи." />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white">Исполнители</h1>
          <p className="text-white/80 mt-1">
            {executorsData?.totalElements || 0} специалистов
          </p>
        </div>

        {/* Search & Filters */}
        <Card padding="md" className="mb-6">
          <div className="flex flex-col sm:flex-row gap-4 items-stretch sm:items-center">
            <div className="flex-1">
              <Input
                placeholder="Поиск по имени или специализации..."
                value={filters.search || ''}
                onChange={(e) => updateFilter('search', e.target.value || undefined)}
              />
            </div>
            <div className="w-full sm:w-52 flex-shrink-0">
              <div className="relative">
                <select
                  value={selectedCategoryId ?? ''}
                  onChange={(e) => {
                    setSelectedCategoryId(e.target.value ? Number(e.target.value) : undefined);
                    setPage(0);
                  }}
                  className="w-full appearance-none pl-3 pr-8 py-2.5 bg-white border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500 cursor-pointer"
                >
                  <option value="">Все категории</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
                <svg className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" /></svg>
              </div>
            </div>
            <div className="w-full sm:w-48 flex-shrink-0">
              <Select
                options={ratingOptions}
                value={filters.minRating || ''}
                onChange={(e) => updateFilter('minRating', e.target.value || undefined)}
              />
            </div>
          </div>
        </Card>

        {/* Executors Grid */}
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <Card key={i} padding="md" className="animate-pulse">
                <div className="flex items-center gap-4 mb-4">
                  <div className="w-16 h-16 bg-gray-200 rounded-full" />
                  <div className="flex-1">
                    <div className="h-5 bg-gray-200 rounded w-3/4 mb-2" />
                    <div className="h-4 bg-gray-200 rounded w-1/2" />
                  </div>
                </div>
                <div className="h-4 bg-gray-200 rounded w-full mb-2" />
                <div className="h-4 bg-gray-200 rounded w-2/3" />
              </Card>
            ))}
          </div>
        ) : executorsData?.content.length === 0 ? (
          <Card padding="lg" className="text-center">
            <Users className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">Исполнители не найдены</h3>
            <p className="text-gray-500">Попробуйте изменить параметры поиска</p>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6">
            {executorsData?.content.map((executor) => (
              <Link key={executor.id} to={`/executors/${executor.id}`}>
                <Card hover padding="md">
                  <div className="flex items-start gap-4 mb-4">
                    <Avatar
                      src={executor.avatarUrl}
                      name={executor.fullName}
                      size="lg"
                    />
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-gray-900 truncate">
                        {executor.fullName}
                      </h3>
                      {executor.specialization && (
                        <p className="text-sm text-gray-500 truncate">{executor.specialization}</p>
                      )}
                      <div className="flex items-center gap-2 mt-1">
                        <Rating value={executor.rating} size="sm" />
                        <span className="text-sm text-gray-500">({executor.reviewCount})</span>
                      </div>
                    </div>
                    {executor.availableForWork && (
                      <Badge variant="success" size="sm">Доступен</Badge>
                    )}
                  </div>

                  {executor.bio && (
                    <p className="text-sm text-gray-600 line-clamp-2 mb-4">{executor.bio}</p>
                  )}

                  <div className="flex items-center justify-between text-sm text-gray-500 pt-4 border-t border-gray-100">
                    <span>{executor.completedOrders} выполнено</span>
                    {executor.reputationLevel && (
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${reputationColorMap[executor.reputationColor || 'gray'] || reputationColorMap.gray}`}>
                        {executor.reputationLevel}
                      </span>
                    )}
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}

        {/* Pagination */}
        {executorsData && executorsData.totalPages > 1 && (
          <div className="mt-8 flex items-center justify-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={executorsData.first}
              onClick={() => setPage((p) => p - 1)}
            >
              Назад
            </Button>
            <span className="px-4 text-sm text-gray-600">
              Страница {executorsData.number + 1} из {executorsData.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={executorsData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Далее
            </Button>
          </div>
        )}
      </div>
    </Layout>
  );
}
