import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  Users,
  Search,
  Shield,
  ShieldOff,
  UserX,
  UserCheck,
  Eye,
  ChevronDown,
  Mail,
  Calendar,
  Briefcase,
} from 'lucide-react';
import { AdminLayout } from '@/components/layout';
import { Button, Badge, Modal, Avatar } from '@/components/ui';
import { adminApi, type AdminUser } from '@/api/admin';

type FilterRole = 'ALL' | 'USER' | 'ADMIN';
type FilterStatus = 'ALL' | 'ACTIVE' | 'BLOCKED';

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [roleFilter, setRoleFilter] = useState<FilterRole>('ALL');
  const [statusFilter, setStatusFilter] = useState<FilterStatus>('ALL');
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-users', page, search, roleFilter, statusFilter],
    queryFn: () =>
      adminApi.getUsers(
        page,
        20,
        search || undefined,
        roleFilter === 'ALL' ? undefined : roleFilter,
        statusFilter === 'ALL' ? undefined : statusFilter === 'ACTIVE'
      ),
  });

  const blockMutation = useMutation({
    mutationFn: (id: number) => adminApi.blockUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      setSelectedUser(null);
    },
  });

  const unblockMutation = useMutation({
    mutationFn: (id: number) => adminApi.unblockUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      setSelectedUser(null);
    },
  });

  const changeRoleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: string }) =>
      adminApi.changeUserRole(id, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setSelectedUser(null);
    },
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    setPage(0);
  };

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Пользователи</h1>
            <p className="text-gray-500">Управление пользователями платформы</p>
          </div>
          <Badge variant="info">
            {data?.totalElements || 0} всего
          </Badge>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-xl shadow-sm p-4">
          <div className="flex flex-col md:flex-row gap-4">
            <form onSubmit={handleSearch} className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Поиск по имени или email..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
            </form>

            <div className="flex gap-3">
              <div className="relative">
                <select
                  value={roleFilter}
                  onChange={(e) => {
                    setRoleFilter(e.target.value as FilterRole);
                    setPage(0);
                  }}
                  className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2 pr-8 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="ALL">Все роли</option>
                  <option value="USER">Пользователи</option>
                  <option value="ADMIN">Админы</option>
                </select>
                <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" />
              </div>

              <div className="relative">
                <select
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value as FilterStatus);
                    setPage(0);
                  }}
                  className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2 pr-8 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="ALL">Все статусы</option>
                  <option value="ACTIVE">Активные</option>
                  <option value="BLOCKED">Заблокированные</option>
                </select>
                <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" />
              </div>
            </div>
          </div>
        </div>

        {/* Users Table */}
        {isLoading ? (
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <div className="animate-pulse">
              {[...Array(10)].map((_, i) => (
                <div key={i} className="h-16 bg-gray-100 border-b border-gray-200" />
              ))}
            </div>
          </div>
        ) : data?.content.length === 0 ? (
          <div className="bg-white rounded-xl shadow-sm p-12 text-center">
            <Users className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-gray-900">Пользователи не найдены</h2>
            <p className="text-gray-500 mt-2">Измените параметры поиска</p>
          </div>
        ) : (
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Пользователь
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Статус
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Роль
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Заказы
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Регистрация
                  </th>
                  <th className="text-right py-3 px-4 text-sm font-medium text-gray-500">
                    Действия
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data?.content.map((user) => (
                  <tr key={user.id} className="hover:bg-gray-50">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <Avatar src={user.avatarUrl} name={user.fullName} size="sm" />
                        <div>
                          <p className="font-medium text-gray-900">{user.fullName}</p>
                          <p className="text-sm text-gray-500">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex flex-col gap-1">
                        <Badge variant={user.active ? 'success' : 'error'} size="sm">
                          {user.active ? 'Активен' : 'Заблокирован'}
                        </Badge>
                        {user.executorVerified && (
                          <Badge variant="info" size="sm">
                            <Shield className="w-3 h-3 mr-1" />
                            Верифицирован
                          </Badge>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <Badge variant={user.role === 'ADMIN' ? 'warning' : 'default'}>
                        {user.role === 'ADMIN' ? 'Админ' : 'Пользователь'}
                      </Badge>
                    </td>
                    <td className="py-3 px-4">
                      <div className="text-sm">
                        <p className="text-gray-900">
                          Заказчик: <span className="font-medium">{user.ordersAsClient}</span>
                        </p>
                        <p className="text-gray-500">
                          Исполнитель: <span className="font-medium">{user.ordersAsExecutor}</span>
                        </p>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-sm text-gray-500">
                      {format(new Date(user.createdAt), 'd MMM yyyy', { locale: ru })}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Button variant="outline" size="sm" onClick={() => setSelectedUser(user)}>
                        <Eye className="w-4 h-4" />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-center gap-2">
            <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Назад
            </Button>
            <span className="px-4 py-2 text-gray-600">
              {page + 1} / {data.totalPages}
            </span>
            <Button
              variant="outline"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Вперёд
            </Button>
          </div>
        )}
      </div>

      {/* User Detail Modal */}
      <Modal
        isOpen={!!selectedUser}
        onClose={() => setSelectedUser(null)}
        title="Информация о пользователе"
        size="lg"
      >
        {selectedUser && (
          <div className="space-y-6">
            <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
              <Avatar src={selectedUser.avatarUrl} name={selectedUser.fullName} size="xl" />
              <div className="flex-1">
                <h3 className="text-xl font-semibold text-gray-900">{selectedUser.fullName}</h3>
                <div className="flex items-center gap-2 text-gray-500 mt-1">
                  <Mail className="w-4 h-4" />
                  {selectedUser.email}
                </div>
                <div className="flex gap-2 mt-2">
                  <Badge variant={selectedUser.active ? 'success' : 'error'}>
                    {selectedUser.active ? 'Активен' : 'Заблокирован'}
                  </Badge>
                  <Badge variant={selectedUser.role === 'ADMIN' ? 'warning' : 'default'}>
                    {selectedUser.role === 'ADMIN' ? 'Админ' : 'Пользователь'}
                  </Badge>
                  {selectedUser.executorVerified && (
                    <Badge variant="info">
                      <Shield className="w-3 h-3 mr-1" />
                      Верифицирован
                    </Badge>
                  )}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-2 text-gray-500 mb-1">
                  <Calendar className="w-4 h-4" />
                  <span className="text-sm">Регистрация</span>
                </div>
                <p className="font-medium text-gray-900">
                  {format(new Date(selectedUser.createdAt), 'd MMMM yyyy', { locale: ru })}
                </p>
              </div>
              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-2 text-gray-500 mb-1">
                  <Calendar className="w-4 h-4" />
                  <span className="text-sm">Последний вход</span>
                </div>
                <p className="font-medium text-gray-900">
                  {selectedUser.lastLoginAt
                    ? format(new Date(selectedUser.lastLoginAt), 'd MMMM yyyy, HH:mm', {
                        locale: ru,
                      })
                    : 'Нет данных'}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 border border-gray-200 rounded-lg">
                <div className="flex items-center gap-2 text-gray-500 mb-1">
                  <Briefcase className="w-4 h-4" />
                  <span className="text-sm">Заказы как заказчик</span>
                </div>
                <p className="text-2xl font-bold text-gray-900">{selectedUser.ordersAsClient}</p>
              </div>
              <div className="p-4 border border-gray-200 rounded-lg">
                <div className="flex items-center gap-2 text-gray-500 mb-1">
                  <Briefcase className="w-4 h-4" />
                  <span className="text-sm">Заказы как исполнитель</span>
                </div>
                <p className="text-2xl font-bold text-gray-900">{selectedUser.ordersAsExecutor}</p>
              </div>
            </div>

            <div className="flex items-center gap-2 text-sm text-gray-500">
              <span>Email подтвержден:</span>
              <Badge variant={selectedUser.emailVerified ? 'success' : 'warning'} size="sm">
                {selectedUser.emailVerified ? 'Да' : 'Нет'}
              </Badge>
            </div>

            <div className="flex gap-3 pt-4 border-t border-gray-200">
              {selectedUser.active ? (
                <Button
                  variant="danger"
                  onClick={() => blockMutation.mutate(selectedUser.id)}
                  loading={blockMutation.isPending}
                >
                  <UserX className="w-4 h-4 mr-2" />
                  Заблокировать
                </Button>
              ) : (
                <Button
                  variant="primary"
                  onClick={() => unblockMutation.mutate(selectedUser.id)}
                  loading={unblockMutation.isPending}
                >
                  <UserCheck className="w-4 h-4 mr-2" />
                  Разблокировать
                </Button>
              )}

              {selectedUser.role === 'USER' ? (
                <Button
                  variant="outline"
                  onClick={() =>
                    changeRoleMutation.mutate({ id: selectedUser.id, role: 'ADMIN' })
                  }
                  loading={changeRoleMutation.isPending}
                >
                  <Shield className="w-4 h-4 mr-2" />
                  Сделать админом
                </Button>
              ) : (
                <Button
                  variant="outline"
                  onClick={() =>
                    changeRoleMutation.mutate({ id: selectedUser.id, role: 'USER' })
                  }
                  loading={changeRoleMutation.isPending}
                >
                  <ShieldOff className="w-4 h-4 mr-2" />
                  Снять права админа
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>
    </AdminLayout>
  );
}
