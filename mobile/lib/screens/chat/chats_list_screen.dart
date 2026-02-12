import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/chat_provider.dart';
import 'package:freelance_kg/providers/auth_provider.dart';
import 'package:intl/intl.dart';

class ChatsListScreen extends ConsumerWidget {
  const ChatsListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);
    final chatsAsync = ref.watch(chatRoomsProvider);

    if (!auth.isAuthenticated) {
      return Scaffold(
        appBar: AppBar(title: const Text('Сообщения')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.chat_bubble_outline,
                  size: 64, color: AppTheme.textMuted),
              const SizedBox(height: 16),
              const Text('Войдите, чтобы видеть сообщения',
                  style: TextStyle(color: AppTheme.textSecondary)),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: () => context.go('/login'),
                child: const Text('Войти'),
              ),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Сообщения')),
      body: chatsAsync.when(
        data: (page) {
          if (page.content.isEmpty) {
            return const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.chat_bubble_outline,
                      size: 64, color: AppTheme.textMuted),
                  SizedBox(height: 16),
                  Text('Нет сообщений',
                      style: TextStyle(
                          color: AppTheme.textSecondary, fontSize: 16)),
                  SizedBox(height: 4),
                  Text('Откликнитесь на задание, чтобы начать чат',
                      style: TextStyle(
                          color: AppTheme.textMuted, fontSize: 13)),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(chatRoomsProvider),
            child: ListView.separated(
              itemCount: page.content.length,
              separatorBuilder: (_, __) =>
                  const Divider(height: 1, indent: 72),
              itemBuilder: (context, index) {
                final chat = page.content[index];
                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16, vertical: 8),
                  leading: _buildAvatar(chat.otherUserName, chat.otherUserAvatar),
                  title: Text(
                    chat.otherUserName.isNotEmpty
                        ? chat.otherUserName
                        : 'Пользователь',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        chat.orderTitle,
                        style: const TextStyle(
                            fontSize: 12, color: AppTheme.primary),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      if (chat.lastMessage != null)
                        Text(
                          chat.lastMessage!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 13,
                            color: chat.unreadCount > 0
                                ? AppTheme.textPrimary
                                : AppTheme.textSecondary,
                            fontWeight: chat.unreadCount > 0
                                ? FontWeight.w600
                                : FontWeight.normal,
                          ),
                        ),
                    ],
                  ),
                  trailing: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      if (chat.lastMessageAt != null)
                        Text(
                          _formatTime(chat.lastMessageAt!),
                          style: const TextStyle(
                              fontSize: 11, color: AppTheme.textMuted),
                        ),
                      if (chat.unreadCount > 0) ...[
                        const SizedBox(height: 4),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppTheme.primary,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            '${chat.unreadCount}',
                            style: const TextStyle(
                                color: Colors.white,
                                fontSize: 11,
                                fontWeight: FontWeight.bold),
                          ),
                        ),
                      ],
                    ],
                  ),
                  onTap: () => context.push(
                    '/chats/${chat.id}',
                    extra: chat.otherUserName,
                  ),
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
                onPressed: () => ref.invalidate(chatRoomsProvider),
                child: const Text('Повторить'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAvatar(String name, String? avatarUrl) {
    if (avatarUrl != null && avatarUrl.isNotEmpty) {
      return CircleAvatar(
        radius: 24,
        backgroundImage: CachedNetworkImageProvider(avatarUrl),
        backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
      );
    }
    return CircleAvatar(
      radius: 24,
      backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
      child: Text(
        name.isNotEmpty ? name[0].toUpperCase() : '?',
        style: const TextStyle(
            color: AppTheme.primary,
            fontWeight: FontWeight.bold,
            fontSize: 18),
      ),
    );
  }

  String _formatTime(String iso) {
    try {
      final date = DateTime.parse(iso);
      final now = DateTime.now();
      if (date.year == now.year &&
          date.month == now.month &&
          date.day == now.day) {
        return DateFormat('HH:mm').format(date);
      }
      return DateFormat('dd.MM').format(date);
    } catch (_) {
      return '';
    }
  }
}
