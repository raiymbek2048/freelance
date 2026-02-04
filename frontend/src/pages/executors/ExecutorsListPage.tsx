import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, Filter, Users } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card, Input, Select, Avatar, Rating, Badge } from '@/components/ui';
import { executorsApi } from '@/api/executors';
import { categoriesApi } from '@/api/categories';
import type { ExecutorFilters } from '@/types';

export function ExecutorsListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);

  const filters: ExecutorFilters = {
    categoryId: searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : undefined,
    minRating: searchParams.get('minRating') ? Number(searchParams.get('minRating')) : undefined,
    availableOnly: searchParams.get('availableOnly') === 'true',
    search: searchParams.get('search') || undefined,
  };

  const { data: executorsData, isLoading } = useQuery({
    queryKey: ['executors', filters, page],
    queryFn: () => executorsApi.getAll(filters, page, 20),
  });

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.getAll,
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

  const categoryOptions = [
    { value: '', label: 'Все категории' },
    ...(categories?.map((c) => ({ value: c.id, label: c.name })) || []),
  ];

  const ratingOptions = [
    { value: '', label: 'Любой рейтинг' },
    { value: '4', label: 'От 4 звёзд' },
    { value: '4.5', label: 'От 4.5 звёзд' },
  ];

  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Исполнители</h1>
          <p className="text-gray-600 mt-1">
            {executorsData?.totalElements || 0} специалистов
          </p>
        </div>

        {/* Search & Filters */}
        <Card padding="md" className="mb-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                placeholder="Поиск исполнителей..."
                className="pl-10"
                value={filters.search || ''}
                onChange={(e) => updateFilter('search', e.target.value || undefined)}
              />
            </div>
            <Select
              options={categoryOptions}
              value={filters.categoryId || ''}
              onChange={(e) => updateFilter('categoryId', e.target.value || undefined)}
              className="w-full md:w-48"
            />
            <Button
              variant="outline"
              onClick={() => setShowFilters(!showFilters)}
              className="flex items-center gap-2"
            >
              <Filter className="w-4 h-4" />
              Фильтры
            </Button>
          </div>

          {showFilters && (
            <div className="mt-4 pt-4 border-t border-gray-200 grid grid-cols-1 md:grid-cols-3 gap-4">
              <Select
                label="Минимальный рейтинг"
                options={ratingOptions}
                value={filters.minRating || ''}
                onChange={(e) => updateFilter('minRating', e.target.value || undefined)}
              />
              <div className="flex items-end">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={filters.availableOnly}
                    onChange={(e) => updateFilter('availableOnly', e.target.checked)}
                    className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm text-gray-700">Только доступные для работы</span>
                </label>
              </div>
            </div>
          )}
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
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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

                  {executor.categories && executor.categories.length > 0 && (
                    <div className="flex flex-wrap gap-2 mb-4">
                      {executor.categories.slice(0, 3).map((category) => (
                        <span
                          key={category.id}
                          className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded"
                        >
                          {category.name}
                        </span>
                      ))}
                      {executor.categories.length > 3 && (
                        <span className="text-xs text-gray-500">
                          +{executor.categories.length - 3}
                        </span>
                      )}
                    </div>
                  )}

                  <div className="flex items-center justify-between text-sm text-gray-500 pt-4 border-t border-gray-100">
                    <span>{executor.completedOrders} выполнено</span>
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
