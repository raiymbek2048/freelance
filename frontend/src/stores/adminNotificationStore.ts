import { create } from 'zustand';

export interface AdminNotification {
  id: number;
  content: string;
  createdAt: string;
  read: boolean;
}

interface AdminNotificationState {
  notifications: AdminNotification[];
  unreadCount: number;
  addNotification: (notification: AdminNotification) => void;
  markAsRead: (id: number) => void;
  markAllAsRead: () => void;
  clearNotifications: () => void;
}

export const useAdminNotificationStore = create<AdminNotificationState>((set, get) => ({
  notifications: [],
  unreadCount: 0,

  addNotification: (notification) => {
    const { notifications } = get();
    // Prevent duplicates
    if (notifications.some((n) => n.id === notification.id)) {
      return;
    }
    const newNotifications = [notification, ...notifications].slice(0, 50); // Keep last 50
    const unreadCount = newNotifications.filter((n) => !n.read).length;
    set({ notifications: newNotifications, unreadCount });
  },

  markAsRead: (id) => {
    const { notifications } = get();
    const updated = notifications.map((n) =>
      n.id === id ? { ...n, read: true } : n
    );
    const unreadCount = updated.filter((n) => !n.read).length;
    set({ notifications: updated, unreadCount });
  },

  markAllAsRead: () => {
    const { notifications } = get();
    const updated = notifications.map((n) => ({ ...n, read: true }));
    set({ notifications: updated, unreadCount: 0 });
  },

  clearNotifications: () => {
    set({ notifications: [], unreadCount: 0 });
  },
}));
