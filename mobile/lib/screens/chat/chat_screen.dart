import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/models/chat.dart';
import 'package:freelance_kg/providers/auth_provider.dart';
import 'package:freelance_kg/providers/chat_provider.dart';
import 'package:intl/intl.dart';

class ChatScreen extends ConsumerStatefulWidget {
  final int chatRoomId;
  final String? partnerName;
  const ChatScreen({super.key, required this.chatRoomId, this.partnerName});

  @override
  ConsumerState<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends ConsumerState<ChatScreen> {
  final _controller = TextEditingController();
  final _scrollController = ScrollController();
  bool _sending = false;
  Timer? _pollTimer;
  StreamSubscription? _stompSub;

  @override
  void initState() {
    super.initState();
    _markAsRead();
    _connectStomp();
    // Polling every 3 seconds as fallback - uses refresh() to keep existing data
    _pollTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      ref.read(chatMessagesProvider(widget.chatRoomId).notifier).refresh();
    });
  }

  /// Mark all messages as read when opening the chat
  Future<void> _markAsRead() async {
    try {
      await ApiClient().dio.put('/chats/${widget.chatRoomId}/messages/read');
      ref.invalidate(chatRoomsProvider);
    } catch (_) {}
  }

  void _connectStomp() async {
    final stomp = ref.read(stompServiceProvider);
    await stomp.connect();
    _stompSub = stomp.messageStream.listen((message) {
      if (message.chatRoomId == widget.chatRoomId) {
        // Refresh messages without losing current data
        ref.read(chatMessagesProvider(widget.chatRoomId).notifier).refresh();
        _markAsRead();
      }
      ref.invalidate(chatRoomsProvider);
    });
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _stompSub?.cancel();
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    setState(() => _sending = true);
    try {
      await ApiClient().dio.post(
        '/chats/${widget.chatRoomId}/messages',
        data: {'content': text},
      );
      _controller.clear();
      // Refresh messages - keeps existing messages visible while fetching
      await ref.read(chatMessagesProvider(widget.chatRoomId).notifier).refresh();
      ref.invalidate(chatRoomsProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Ошибка отправки: $e')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final messagesAsync =
        ref.watch(chatMessagesProvider(widget.chatRoomId));
    final auth = ref.watch(authProvider);
    final currentUserId = auth.user?.id ?? 0;

    return Scaffold(
      appBar: AppBar(title: Text(widget.partnerName ?? 'Чат')),
      body: Column(
        children: [
          Expanded(
            child: messagesAsync.when(
              skipLoadingOnReload: true,
              data: (messages) {
                if (messages.isEmpty) {
                  return const Center(
                    child: Text('Нет сообщений. Напишите первым!',
                        style: TextStyle(color: AppTheme.textMuted)),
                  );
                }
                return ListView.builder(
                  controller: _scrollController,
                  reverse: true,
                  padding: const EdgeInsets.all(16),
                  itemCount: messages.length,
                  itemBuilder: (context, index) {
                    // Messages come in DESC order (newest first) from API
                    // With reverse:true, index 0 = bottom = should be newest
                    final msg = messages[index];
                    final isMe = msg.isMine || msg.senderId == currentUserId;
                    return _MessageBubble(message: msg, isMe: isMe);
                  },
                );
              },
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (e, __) =>
                  Center(child: Text('Ошибка загрузки: $e')),
            ),
          ),
          Container(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
            decoration: const BoxDecoration(
              color: Colors.white,
              border: Border(
                  top: BorderSide(color: AppTheme.border, width: 0.5)),
            ),
            child: SafeArea(
              top: false,
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _send(),
                      decoration: const InputDecoration(
                        hintText: 'Сообщение...',
                        contentPadding: EdgeInsets.symmetric(
                            horizontal: 14, vertical: 10),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    onPressed: _sending ? null : _send,
                    icon: _sending
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                                strokeWidth: 2),
                          )
                        : const Icon(Icons.send, color: AppTheme.primary),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  final ChatMessage message;
  final bool isMe;

  const _MessageBubble({required this.message, required this.isMe});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: isMe ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.75,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: isMe ? AppTheme.primary : Colors.grey[200],
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(isMe ? 16 : 4),
            bottomRight: Radius.circular(isMe ? 4 : 16),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (!isMe)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(
                  message.senderName,
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.primary,
                  ),
                ),
              ),
            Text(
              message.content,
              style: TextStyle(
                color: isMe ? Colors.white : AppTheme.textPrimary,
                fontSize: 14,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              _formatTime(message.createdAt),
              style: TextStyle(
                fontSize: 10,
                color: isMe
                    ? Colors.white.withValues(alpha: 0.7)
                    : AppTheme.textMuted,
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatTime(String iso) {
    try {
      final date = DateTime.parse(iso);
      return DateFormat('HH:mm').format(date);
    } catch (_) {
      return '';
    }
  }
}
