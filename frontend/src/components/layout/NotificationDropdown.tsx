import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCircle, RefreshCw, MessageSquare, AlertTriangle, Scale } from 'lucide-react';
import { useNotificationStore } from '@/stores/notificationStore';
import type { NotificationType } from '@/types';

const typeConfig: Record<NotificationType, { icon: React.ElementType; bgColor: string; iconColor: string }> = {
  EXECUTOR_SELECTED: { icon: CheckCircle, bgColor: 'bg-green-100', iconColor: 'text-green-600' },
  WORK_APPROVED: { icon: CheckCircle, bgColor: 'bg-green-100', iconColor: 'text-green-600' },
  REVISION_REQUESTED: { icon: RefreshCw, bgColor: 'bg-amber-100', iconColor: 'text-amber-600' },
  NEW_RESPONSE: { icon: MessageSquare, bgColor: 'bg-blue-100', iconColor: 'text-blue-600' },
  DISPUTE_OPENED: { icon: AlertTriangle, bgColor: 'bg-red-100', iconColor: 'text-red-600' },
  DISPUTE_RESOLVED: { icon: Scale, bgColor: 'bg-purple-100', iconColor: 'text-purple-600' },
};

function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMin < 1) return 'только что';
  if (diffMin < 60) return `${diffMin} мин назад`;
  if (diffHours < 24) return `${diffHours} ч назад`;
  if (diffDays < 7) return `${diffDays} д назад`;

  return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
}

export function NotificationDropdown() {
  const navigate = useNavigate();
  const {
    notifications,
    unreadCount,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
  } = useNotificationStore();
  const [open, setOpen] = useState(false);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    fetchUnreadCount();
  }, []);

  useEffect(() => {
    if (open && !initialized) {
      fetchNotifications();
      setInitialized(true);
    }
  }, [open]);

  const handleNotificationClick = (notification: typeof notifications[0]) => {
    if (!notification.isRead) {
      markAsRead(notification.id);
    }
    if (notification.link) {
      navigate(notification.link);
    } else if (notification.orderId) {
      navigate(`/orders/${notification.orderId}`);
    }
    setOpen(false);
  };

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="p-2 text-white/90 hover:text-white relative"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold min-w-[18px] h-[18px] flex items-center justify-center rounded-full px-1">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setOpen(false)}
          />
          <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-lg shadow-lg border border-gray-200 z-20 max-h-[500px] overflow-hidden flex flex-col">
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
              <h3 className="font-semibold text-gray-900">Уведомления</h3>
              {unreadCount > 0 && (
                <button
                  onClick={markAllAsRead}
                  className="text-sm text-cyan-600 hover:text-cyan-700"
                >
                  Прочитать все
                </button>
              )}
            </div>

            <div className="overflow-y-auto flex-1">
              {notifications.length === 0 ? (
                <div className="p-8 text-center text-gray-500">
                  <Bell className="w-10 h-10 mx-auto mb-2 text-gray-300" />
                  <p>Нет уведомлений</p>
                </div>
              ) : (
                notifications.slice(0, 20).map((notification) => {
                  const config = typeConfig[notification.type] || typeConfig.NEW_RESPONSE;
                  const Icon = config.icon;
                  return (
                    <div
                      key={notification.id}
                      className={`px-4 py-3 border-b border-gray-50 hover:bg-gray-50 cursor-pointer ${
                        !notification.isRead ? 'bg-blue-50/50' : ''
                      }`}
                      onClick={() => handleNotificationClick(notification)}
                    >
                      <div className="flex items-start gap-3">
                        <div className={`p-2 ${config.bgColor} rounded-full flex-shrink-0`}>
                          <Icon className={`w-4 h-4 ${config.iconColor}`} />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-900">
                            {notification.title}
                          </p>
                          <p className="text-sm text-gray-600 mt-0.5 line-clamp-2">
                            {notification.message}
                          </p>
                          <p className="text-xs text-gray-400 mt-1">
                            {formatTime(notification.createdAt)}
                          </p>
                        </div>
                        {!notification.isRead && (
                          <div className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0 mt-2" />
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
