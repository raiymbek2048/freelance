class ChatRoom {
  final int id;
  final int orderId;
  final String orderTitle;
  final int otherUserId;
  final String otherUserName;
  final String? otherUserAvatar;
  final String? lastMessage;
  final String? lastMessageAt;
  final int? lastMessageSenderId;
  final int unreadCount;

  ChatRoom({
    required this.id,
    required this.orderId,
    required this.orderTitle,
    required this.otherUserId,
    required this.otherUserName,
    this.otherUserAvatar,
    this.lastMessage,
    this.lastMessageAt,
    this.lastMessageSenderId,
    this.unreadCount = 0,
  });

  factory ChatRoom.fromJson(Map<String, dynamic> json) => ChatRoom(
        id: json['id'],
        orderId: json['orderId'] ?? 0,
        orderTitle: json['orderTitle'] ?? '',
        // Backend uses participantId/participantName/participantAvatarUrl
        otherUserId: json['participantId'] ?? json['otherUserId'] ?? 0,
        otherUserName: json['participantName'] ?? json['otherUserName'] ?? '',
        otherUserAvatar: json['participantAvatarUrl'] ?? json['otherUserAvatar'],
        lastMessage: json['lastMessage'],
        lastMessageAt: json['lastMessageAt'],
        lastMessageSenderId: json['lastMessageSenderId'],
        unreadCount: json['unreadCount'] ?? 0,
      );
}

class ChatMessage {
  final int id;
  final int chatRoomId;
  final int senderId;
  final String senderName;
  final String content;
  final List<String> attachments;
  final bool isRead;
  final bool isMine;
  final String createdAt;

  ChatMessage({
    required this.id,
    required this.chatRoomId,
    required this.senderId,
    required this.senderName,
    required this.content,
    this.attachments = const [],
    this.isRead = false,
    this.isMine = false,
    required this.createdAt,
  });

  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
        id: json['id'],
        chatRoomId: json['chatRoomId'] ?? 0,
        senderId: json['senderId'] ?? 0,
        senderName: json['senderName'] ?? '',
        content: json['content'] ?? '',
        attachments: json['attachments'] != null
            ? List<String>.from(json['attachments'])
            : [],
        isRead: json['isRead'] ?? json['read'] ?? false,
        isMine: json['isMine'] ?? false,
        createdAt: json['createdAt'] ?? '',
      );
}
