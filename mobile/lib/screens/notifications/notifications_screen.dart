import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/notifications_provider.dart';
import 'package:intl/intl.dart';

class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notificationsAsync = ref.watch(notificationsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Уведомления'),
        actions: [
          TextButton(
            onPressed: () => markAllAsRead(ref),
            child: const Text('Прочитать все',
                style: TextStyle(fontSize: 13)),
          ),
        ],
      ),
      body: notificationsAsync.when(
        data: (notifications) {
          if (notifications.isEmpty) {
            return const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.notifications_none,
                      size: 64, color: AppTheme.textMuted),
                  SizedBox(height: 16),
                  Text('Нет уведомлений',
                      style: TextStyle(
                          color: AppTheme.textSecondary, fontSize: 16)),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(notificationsProvider);
              await fetchUnreadCount(ref);
            },
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: notifications.length,
              separatorBuilder: (_, __) => const SizedBox(height: 6),
              itemBuilder: (context, index) {
                final n = notifications[index];
                return _NotificationCard(
                  notification: n,
                  onTap: () {
                    if (!n.isRead) {
                      markAsRead(ref, n.id);
                    }
                    if (n.orderId != null) {
                      context.push('/order/${n.orderId}');
                    }
                  },
                );
              },
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Ошибка загрузки'),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: () => ref.invalidate(notificationsProvider),
                child: const Text('Повторить'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NotificationCard extends StatelessWidget {
  final AppNotification notification;
  final VoidCallback? onTap;

  const _NotificationCard({required this.notification, this.onTap});

  @override
  Widget build(BuildContext context) {
    final isUnread = !notification.isRead;

    return Card(
      color: isUnread ? AppTheme.primary.withValues(alpha: 0.04) : null,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: _typeColor(notification.type).withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(_typeIcon(notification.type),
                    size: 18, color: _typeColor(notification.type)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            notification.title,
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight:
                                  isUnread ? FontWeight.w600 : FontWeight.w500,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        if (isUnread)
                          Container(
                            width: 8,
                            height: 8,
                            decoration: const BoxDecoration(
                              color: AppTheme.primary,
                              shape: BoxShape.circle,
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      notification.message,
                      style: const TextStyle(
                          fontSize: 13, color: AppTheme.textSecondary),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 6),
                    Text(
                      _formatDate(notification.createdAt),
                      style: const TextStyle(
                          fontSize: 11, color: AppTheme.textMuted),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  IconData _typeIcon(String type) => switch (type) {
        'NEW_RESPONSE' => Icons.reply,
        'RESPONSE_SELECTED' => Icons.check_circle,
        'ORDER_COMPLETED' => Icons.done_all,
        'ORDER_CANCELLED' => Icons.cancel,
        'ORDER_IN_PROGRESS' => Icons.play_circle,
        'ORDER_ON_REVIEW' => Icons.rate_review,
        'ORDER_REVISION' => Icons.refresh,
        'DISPUTE_OPENED' => Icons.gavel,
        'DISPUTE_RESOLVED' => Icons.handshake,
        'NEW_MESSAGE' => Icons.chat,
        'NEW_REVIEW' => Icons.star,
        'VERIFICATION_APPROVED' => Icons.verified,
        'VERIFICATION_REJECTED' => Icons.block,
        _ => Icons.notifications,
      };

  Color _typeColor(String type) => switch (type) {
        'NEW_RESPONSE' => AppTheme.primary,
        'RESPONSE_SELECTED' => AppTheme.success,
        'ORDER_COMPLETED' => AppTheme.success,
        'ORDER_CANCELLED' => Colors.red,
        'ORDER_IN_PROGRESS' => Colors.blue,
        'ORDER_ON_REVIEW' => Colors.orange,
        'ORDER_REVISION' => Colors.orange,
        'DISPUTE_OPENED' => Colors.red,
        'DISPUTE_RESOLVED' => Colors.blue,
        'NEW_MESSAGE' => AppTheme.primary,
        'NEW_REVIEW' => Colors.amber,
        'VERIFICATION_APPROVED' => AppTheme.success,
        'VERIFICATION_REJECTED' => Colors.red,
        _ => AppTheme.textMuted,
      };

  String _formatDate(String iso) {
    try {
      final date = DateTime.parse(iso);
      final now = DateTime.now();
      final diff = now.difference(date);

      if (diff.inMinutes < 1) return 'Только что';
      if (diff.inMinutes < 60) return '${diff.inMinutes} мин назад';
      if (diff.inHours < 24) return '${diff.inHours} ч назад';
      if (diff.inDays < 7) return '${diff.inDays} дн назад';
      return DateFormat('dd.MM.yyyy').format(date);
    } catch (_) {
      return '';
    }
  }
}
