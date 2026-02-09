import apiClient from './client';
import type { NotificationItem, PageResponse } from '@/types';

export const notificationsApi = {
  getNotifications: async (page = 0, size = 20): Promise<PageResponse<NotificationItem>> => {
    const response = await apiClient.get<PageResponse<NotificationItem>>(
      `/notifications?page=${page}&size=${size}`
    );
    return response.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await apiClient.get<{ count: number }>('/notifications/unread-count');
    return response.data.count;
  },

  markAsRead: async (id: number): Promise<void> => {
    await apiClient.put(`/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.put('/notifications/read-all');
  },
};
