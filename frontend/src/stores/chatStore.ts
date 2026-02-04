import { create } from 'zustand';
import SockJS from 'sockjs-client';
import { Client, type IMessage } from '@stomp/stompjs';
import type { ChatRoom, Message } from '@/types';
import { chatApi } from '@/api/chat';
import { useAdminNotificationStore } from './adminNotificationStore';

interface ChatState {
  chatRooms: ChatRoom[];
  activeChatId: number | null;
  messages: Record<number, Message[]>;
  typingUsers: Record<number, number[]>;
  connected: boolean;
  stompClient: Client | null;
  totalUnreadCount: number;

  connect: () => void;
  disconnect: () => void;
  fetchChatRooms: () => Promise<void>;
  setActiveChat: (chatId: number | null) => void;
  fetchMessages: (chatRoomId: number) => Promise<void>;
  sendMessage: (chatRoomId: number, content: string, attachments?: string[]) => Promise<void>;
  sendTypingIndicator: (chatRoomId: number) => void;
  markAsRead: (chatRoomId: number) => void;
  addMessage: (chatRoomId: number, message: Message) => void;
  getTotalUnreadCount: () => number;
}

export const useChatStore = create<ChatState>((set, get) => ({
  chatRooms: [],
  activeChatId: null,
  messages: {},
  typingUsers: {},
  connected: false,
  stompClient: null,
  totalUnreadCount: 0,

  getTotalUnreadCount: () => {
    return get().chatRooms.reduce((sum, room) => sum + (room.unreadCount || 0), 0);
  },

  connect: () => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    const socket = new SockJS('/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        set({ connected: true });

        // Subscribe to personal messages
        client.subscribe('/user/queue/messages', (message: IMessage) => {
          const msg: Message = JSON.parse(message.body);
          get().addMessage(msg.chatRoomId, msg);
        });

        // Subscribe to read receipts
        client.subscribe('/user/queue/read-receipts', (message: IMessage) => {
          const { chatRoomId, userId } = JSON.parse(message.body);
          // Update messages as read
          const messages = get().messages[chatRoomId] || [];
          set({
            messages: {
              ...get().messages,
              [chatRoomId]: messages.map((m) =>
                m.senderId !== userId ? { ...m, read: true, isRead: true } : m
              ),
            },
          });
        });

        // Subscribe to typing indicators
        client.subscribe('/user/queue/typing', (message: IMessage) => {
          const { chatRoomId, userId, typing } = JSON.parse(message.body);
          const typingUsers = get().typingUsers[chatRoomId] || [];
          set({
            typingUsers: {
              ...get().typingUsers,
              [chatRoomId]: typing
                ? [...typingUsers.filter((id) => id !== userId), userId]
                : typingUsers.filter((id) => id !== userId),
            },
          });
        });

        // Subscribe to admin notifications
        client.subscribe('/user/queue/admin-notifications', (message: IMessage) => {
          const notification = JSON.parse(message.body);
          useAdminNotificationStore.getState().addNotification({
            id: notification.id,
            content: notification.content,
            createdAt: notification.createdAt,
            read: false,
          });
        });
      },
      onDisconnect: () => {
        set({ connected: false });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      },
    });

    client.activate();
    set({ stompClient: client });
  },

  disconnect: () => {
    const { stompClient } = get();
    if (stompClient) {
      stompClient.deactivate();
      set({ stompClient: null, connected: false });
    }
  },

  fetchChatRooms: async () => {
    const chatRooms = await chatApi.getMyChats();
    const totalUnreadCount = chatRooms.reduce((sum, room) => sum + (room.unreadCount || 0), 0);
    set({ chatRooms, totalUnreadCount });
  },

  setActiveChat: (chatId: number | null) => {
    set({ activeChatId: chatId });
    if (chatId) {
      get().fetchMessages(chatId);
      get().markAsRead(chatId);
    }
  },

  fetchMessages: async (chatRoomId: number) => {
    const response = await chatApi.getMessages(chatRoomId);
    set({
      messages: {
        ...get().messages,
        [chatRoomId]: response.content.reverse(),
      },
    });
  },

  sendMessage: async (chatRoomId: number, content: string, attachments?: string[]) => {
    // Send via REST API for reliability
    try {
      const message = await chatApi.sendMessage(chatRoomId, content, attachments);
      // Add message to local state
      get().addMessage(chatRoomId, message);
    } catch (error) {
      console.error('Failed to send message:', error);
      throw error;
    }
  },

  sendTypingIndicator: (chatRoomId: number) => {
    const { stompClient } = get();
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/chat/${chatRoomId}/typing`,
        body: '',
      });
    }
  },

  markAsRead: (chatRoomId: number) => {
    const { stompClient, chatRooms } = get();
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/chat/${chatRoomId}/read`,
        body: '',
      });
    }
    // Also update via REST
    chatApi.markAsRead(chatRoomId);

    // Update unread count locally
    const room = chatRooms.find((r) => r.id === chatRoomId);
    if (room && room.unreadCount > 0) {
      const updatedRooms = chatRooms.map((r) =>
        r.id === chatRoomId ? { ...r, unreadCount: 0 } : r
      );
      const totalUnreadCount = updatedRooms.reduce((sum, r) => sum + (r.unreadCount || 0), 0);
      set({ chatRooms: updatedRooms, totalUnreadCount });
    }
  },

  addMessage: (chatRoomId: number, message: Message) => {
    const { messages: allMessages, activeChatId, chatRooms } = get();
    const messages = allMessages[chatRoomId] || [];
    // Check for duplicates by message id
    if (messages.some((m) => m.id === message.id)) {
      return; // Message already exists, skip
    }
    set({
      messages: {
        ...allMessages,
        [chatRoomId]: [...messages, message],
      },
    });

    // Update last message in chat rooms and increment unread if not in active chat
    const isActiveChat = activeChatId === chatRoomId;
    const isOwnMessage = message.isMine;

    const updatedRooms = chatRooms.map((room) =>
      room.id === chatRoomId
        ? {
            ...room,
            lastMessage: message.content,
            lastMessageAt: message.createdAt,
            lastMessageSenderId: message.senderId,
            // Increment unread only if not in active chat and not own message
            unreadCount: (!isActiveChat && !isOwnMessage)
              ? (room.unreadCount || 0) + 1
              : room.unreadCount,
          }
        : room
    );

    const totalUnreadCount = updatedRooms.reduce((sum, r) => sum + (r.unreadCount || 0), 0);
    set({ chatRooms: updatedRooms, totalUnreadCount });
  },
}));
