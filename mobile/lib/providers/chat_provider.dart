import 'dart:async';
import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/constants.dart';
import 'package:freelance_kg/core/storage/secure_storage.dart';
import 'package:freelance_kg/models/chat.dart';
import 'package:freelance_kg/models/order.dart';

final chatRoomsProvider =
    FutureProvider<PageResponse<ChatRoom>>((ref) async {
  final response = await ApiClient()
      .dio
      .get('/chats', queryParameters: {'page': 0, 'size': 50});
  final data = response.data;
  return PageResponse<ChatRoom>(
    content: (data['content'] as List)
        .map((json) => ChatRoom.fromJson(json))
        .toList(),
    totalElements: data['totalElements'] ?? 0,
    totalPages: data['totalPages'] ?? 0,
    number: data['number'] ?? 0,
    first: data['first'] ?? true,
    last: data['last'] ?? true,
  );
});

/// Chat messages notifier that keeps state during refresh
class ChatMessagesNotifier extends FamilyAsyncNotifier<List<ChatMessage>, int> {
  @override
  Future<List<ChatMessage>> build(int chatRoomId) async {
    return _fetchMessages(chatRoomId);
  }

  Future<List<ChatMessage>> _fetchMessages(int chatRoomId) async {
    final response = await ApiClient().dio.get(
        '/chats/$chatRoomId/messages',
        queryParameters: {'page': 0, 'size': 100});
    final data = response.data;
    return (data['content'] as List)
        .map((json) => ChatMessage.fromJson(json))
        .toList();
  }

  /// Refresh messages without losing current state
  Future<void> refresh() async {
    try {
      final messages = await _fetchMessages(arg);
      state = AsyncData(messages);
    } catch (e, st) {
      // Keep previous data if refresh fails
      if (state.hasValue) return;
      state = AsyncError(e, st);
    }
  }
}

final chatMessagesProvider = AsyncNotifierProvider.family<
    ChatMessagesNotifier, List<ChatMessage>, int>(
  ChatMessagesNotifier.new,
);

// STOMP WebSocket service for real-time chat
class StompService {
  StompClient? _client;
  final _messageController = StreamController<ChatMessage>.broadcast();

  Stream<ChatMessage> get messageStream => _messageController.stream;

  Future<void> connect() async {
    if (_client?.connected == true) return;

    final token = await SecureStorage.getAccessToken();
    if (token == null) return;

    _client = StompClient(
      config: StompConfig(
        url: AppConstants.wsUrl,
        stompConnectHeaders: {'Authorization': 'Bearer $token'},
        onConnect: _onConnect,
        onWebSocketError: (_) {},
        onStompError: (_) {},
        onDisconnect: (_) {},
        reconnectDelay: const Duration(seconds: 5),
      ),
    );
    _client!.activate();
  }

  void _onConnect(StompFrame frame) {
    _client?.subscribe(
      destination: '/user/queue/messages',
      callback: (frame) {
        if (frame.body != null) {
          try {
            final json = jsonDecode(frame.body!);
            final message = ChatMessage.fromJson(json);
            _messageController.add(message);
          } catch (_) {}
        }
      },
    );
  }

  void disconnect() {
    _client?.deactivate();
    _client = null;
  }

  void dispose() {
    disconnect();
    _messageController.close();
  }
}

final stompServiceProvider = Provider<StompService>((ref) {
  final service = StompService();
  ref.onDispose(() => service.dispose());
  return service;
});
