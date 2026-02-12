import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freelance_kg/core/api/api_client.dart';

class AppNotification {
  final int id;
  final String type;
  final String title;
  final String message;
  final int? orderId;
  final String? link;
  final bool isRead;
  final String createdAt;

  AppNotification({
    required this.id,
    required this.type,
    required this.title,
    required this.message,
    this.orderId,
    this.link,
    required this.isRead,
    required this.createdAt,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) =>
      AppNotification(
        id: json['id'],
        type: json['type'] ?? '',
        title: json['title'] ?? '',
        message: json['message'] ?? '',
        orderId: json['orderId'],
        link: json['link'],
        isRead: json['isRead'] ?? false,
        createdAt: json['createdAt'] ?? '',
      );
}

final unreadCountProvider = StateProvider<int>((ref) => 0);

final notificationsProvider = FutureProvider.autoDispose<List<AppNotification>>((ref) async {
  final response = await ApiClient()
      .dio
      .get('/notifications', queryParameters: {'page': 0, 'size': 50});
  final data = response.data;
  final list = (data['content'] as List)
      .map((json) => AppNotification.fromJson(json))
      .toList();

  // Update unread count
  final unread = list.where((n) => !n.isRead).length;
  ref.read(unreadCountProvider.notifier).state = unread;

  return list;
});

Future<void> fetchUnreadCount(WidgetRef ref) async {
  try {
    final response = await ApiClient().dio.get('/notifications/unread-count');
    final count = response.data['count'] as int? ?? 0;
    ref.read(unreadCountProvider.notifier).state = count;
  } catch (_) {}
}

Future<void> markAsRead(WidgetRef ref, int notificationId) async {
  try {
    await ApiClient().dio.put('/notifications/$notificationId/read');
    ref.invalidate(notificationsProvider);
    await fetchUnreadCount(ref);
  } catch (_) {}
}

Future<void> markAllAsRead(WidgetRef ref) async {
  try {
    await ApiClient().dio.put('/notifications/read-all');
    ref.read(unreadCountProvider.notifier).state = 0;
    ref.invalidate(notificationsProvider);
  } catch (_) {}
}
