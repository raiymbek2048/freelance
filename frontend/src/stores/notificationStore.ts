import { create } from 'zustand';
import type { NotificationItem } from '@/types';
import { notificationsApi } from '@/api/notifications';

interface NotificationState {
  notifications: NotificationItem[];
  unreadCount: number;
  isLoading: boolean;
  hasMore: boolean;
  currentPage: number;

  fetchNotifications: () => Promise<void>;
  fetchMore: () => Promise<void>;
  fetchUnreadCount: () => Promise<void>;
  addNotification: (notification: NotificationItem) => void;
  markAsRead: (id: number) => void;
  markAllAsRead: () => void;
  clearNotifications: () => void;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  unreadCount: 0,
  isLoading: false,
  hasMore: true,
  currentPage: 0,

  fetchNotifications: async () => {
    set({ isLoading: true });
    try {
      const response = await notificationsApi.getNotifications(0, 20);
      set({
        notifications: response.content,
        hasMore: !response.last,
        currentPage: 0,
        isLoading: false,
      });
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
      set({ isLoading: false });
    }
  },

  fetchMore: async () => {
    const { currentPage, isLoading, hasMore } = get();
    if (isLoading || !hasMore) return;

    set({ isLoading: true });
    try {
      const nextPage = currentPage + 1;
      const response = await notificationsApi.getNotifications(nextPage, 20);
      set({
        notifications: [...get().notifications, ...response.content],
        hasMore: !response.last,
        currentPage: nextPage,
        isLoading: false,
      });
    } catch (error) {
      console.error('Failed to fetch more notifications:', error);
      set({ isLoading: false });
    }
  },

  fetchUnreadCount: async () => {
    try {
      const count = await notificationsApi.getUnreadCount();
      set({ unreadCount: count });
    } catch (error) {
      console.error('Failed to fetch unread count:', error);
    }
  },

  addNotification: (notification) => {
    const { notifications } = get();
    if (notifications.some((n) => n.id === notification.id)) {
      return;
    }
    const newNotifications = [notification, ...notifications].slice(0, 100);
    set({
      notifications: newNotifications,
      unreadCount: get().unreadCount + (notification.isRead ? 0 : 1),
    });
  },

  markAsRead: (id) => {
    const { notifications } = get();
    const notification = notifications.find((n) => n.id === id);
    if (!notification || notification.isRead) return;

    const updated = notifications.map((n) =>
      n.id === id ? { ...n, isRead: true } : n
    );
    set({
      notifications: updated,
      unreadCount: Math.max(0, get().unreadCount - 1),
    });

    notificationsApi.markAsRead(id).catch(console.error);
  },

  markAllAsRead: () => {
    const { notifications } = get();
    const updated = notifications.map((n) => ({ ...n, isRead: true }));
    set({ notifications: updated, unreadCount: 0 });

    notificationsApi.markAllAsRead().catch(console.error);
  },

  clearNotifications: () => {
    set({ notifications: [], unreadCount: 0, currentPage: 0, hasMore: true });
  },
}));
