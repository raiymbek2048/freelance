import apiClient from './client';
import type { PageResponse } from '@/types';

// Admin Types
export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  executors: number;
  newUsersToday: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  totalOrders: number;
  newOrders: number;
  inProgressOrders: number;
  completedOrders: number;
  disputedOrders: number;
  cancelledOrders: number;
  totalOrdersValue: number;
  averageOrderValue: number;
  totalReviews: number;
  pendingModeration: number;
  averageRating: number;
  totalCategories: number;
  topCategories: CategoryStats[];
}

export interface CategoryStats {
  categoryId: number;
  categoryName: string;
  orderCount: number;
  executorCount: number;
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  avatarUrl?: string;
  role: 'USER' | 'ADMIN';
  active: boolean;
  emailVerified: boolean;
  executorVerified: boolean;
  createdAt: string;
  lastLoginAt?: string;
  ordersAsClient: number;
  ordersAsExecutor: number;
}

export interface AdminOrder {
  id: number;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  clientId: number;
  clientName: string;
  clientEmail: string;
  executorId?: number;
  executorName?: string;
  executorEmail?: string;
  budgetMin?: number;
  budgetMax?: number;
  agreedPrice?: number;
  status: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  responseCount: number;
  viewCount: number;
}

export interface AdminVerification {
  userId: number;
  userEmail: string;
  userName: string;
  userAvatarUrl?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  passportUrl?: string;
  selfieUrl?: string;
  submittedAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
}

// Analytics Types
export interface DailyStats {
  date: string;
  newUsers: number;
  newOrders: number;
  completedOrders: number;
  revenue: number;
}

export interface WeeklyStats {
  weekStart: string;
  newUsers: number;
  newOrders: number;
  completedOrders: number;
  revenue: number;
}

export interface MonthlyStats {
  year: number;
  month: number;
  monthName: string;
  newUsers: number;
  newOrders: number;
  completedOrders: number;
  revenue: number;
}

export interface SubscriptionByPeriod {
  date: string;
  newSubscriptions: number;
  revenue: number;
}

export interface SubscriptionAnalytics {
  totalSubscriptions: number;
  activeSubscriptions: number;
  trialSubscriptions: number;
  expiredSubscriptions: number;
  totalRevenue: number;
  revenueThisMonth: number;
  revenueLastMonth: number;
  byPeriod: SubscriptionByPeriod[];
}

export interface ConversionStats {
  registrationToExecutorRate: number;
  executorToVerifiedRate: number;
  orderCompletionRate: number;
  responseToSelectionRate: number;
}

export interface AnalyticsData {
  dailyStats: DailyStats[];
  weeklyStats: WeeklyStats[];
  monthlyStats: MonthlyStats[];
  subscriptions: SubscriptionAnalytics;
  conversions: ConversionStats;
}

export const adminApi = {
  // Stats
  getStats: async (): Promise<AdminStats> => {
    const response = await apiClient.get<AdminStats>('/admin/stats/overview');
    return response.data;
  },

  getAnalytics: async (): Promise<AnalyticsData> => {
    const response = await apiClient.get<AnalyticsData>('/admin/stats/analytics');
    return response.data;
  },

  // Users
  getUsers: async (
    page = 0,
    size = 20,
    search?: string,
    role?: string,
    active?: boolean
  ): Promise<PageResponse<AdminUser>> => {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (search) params.append('search', search);
    if (role) params.append('role', role);
    if (active !== undefined) params.append('active', String(active));
    const response = await apiClient.get<PageResponse<AdminUser>>(`/admin/users?${params}`);
    return response.data;
  },

  getUser: async (id: number): Promise<AdminUser> => {
    const response = await apiClient.get<AdminUser>(`/admin/users/${id}`);
    return response.data;
  },

  blockUser: async (id: number): Promise<void> => {
    await apiClient.put(`/admin/users/${id}/block`);
  },

  unblockUser: async (id: number): Promise<void> => {
    await apiClient.put(`/admin/users/${id}/unblock`);
  },

  changeUserRole: async (id: number, role: string): Promise<void> => {
    await apiClient.put(`/admin/users/${id}/role?role=${role}`);
  },

  // Orders
  getOrders: async (
    page = 0,
    size = 20,
    status?: string,
    categoryId?: number
  ): Promise<PageResponse<AdminOrder>> => {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (status) params.append('status', status);
    if (categoryId) params.append('categoryId', String(categoryId));
    const response = await apiClient.get<PageResponse<AdminOrder>>(`/admin/orders?${params}`);
    return response.data;
  },

  getDisputes: async (page = 0, size = 20): Promise<PageResponse<AdminOrder>> => {
    const response = await apiClient.get<PageResponse<AdminOrder>>(
      `/admin/orders/disputes?page=${page}&size=${size}`
    );
    return response.data;
  },

  getOrder: async (id: number): Promise<AdminOrder> => {
    const response = await apiClient.get<AdminOrder>(`/admin/orders/${id}`);
    return response.data;
  },

  resolveDispute: async (id: number, favorClient: boolean, resolution?: string): Promise<void> => {
    const params = new URLSearchParams();
    params.append('favorClient', String(favorClient));
    if (resolution) params.append('resolution', resolution);
    await apiClient.put(`/admin/orders/${id}/resolve-dispute?${params}`);
  },

  deleteOrder: async (id: number): Promise<void> => {
    await apiClient.delete(`/admin/orders/${id}`);
  },

  // Verifications
  getVerifications: async (
    page = 0,
    size = 20,
    status?: string
  ): Promise<PageResponse<AdminVerification>> => {
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(size));
    if (status) params.append('status', status);
    const response = await apiClient.get<PageResponse<AdminVerification>>(`/admin/verifications?${params}`);
    return response.data;
  },

  approveVerification: async (userId: number): Promise<void> => {
    await apiClient.put(`/admin/verifications/${userId}/approve`);
  },

  rejectVerification: async (userId: number, reason: string): Promise<void> => {
    await apiClient.put(`/admin/verifications/${userId}/reject?reason=${encodeURIComponent(reason)}`);
  },
};
