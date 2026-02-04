import apiClient from './client';
import type { ChatRoom, Message, PageResponse } from '@/types';

export const chatApi = {
  getMyChats: async (): Promise<ChatRoom[]> => {
    const response = await apiClient.get<PageResponse<ChatRoom>>('/chats');
    return response.data.content;
  },

  getChatRoom: async (id: number): Promise<ChatRoom> => {
    const response = await apiClient.get<ChatRoom>(`/chats/${id}`);
    return response.data;
  },

  getMessages: async (chatRoomId: number, page = 0, size = 50): Promise<PageResponse<Message>> => {
    const response = await apiClient.get<PageResponse<Message>>(`/chats/${chatRoomId}/messages?page=${page}&size=${size}`);
    return response.data;
  },

  sendMessage: async (chatRoomId: number, content: string, attachments?: string[]): Promise<Message> => {
    const response = await apiClient.post<Message>(`/chats/${chatRoomId}/messages`, { content, attachments });
    return response.data;
  },

  markAsRead: async (chatRoomId: number): Promise<void> => {
    await apiClient.put(`/chats/${chatRoomId}/messages/read`);
  },

  getOrCreateChat: async (orderId: number, executorId: number): Promise<ChatRoom> => {
    const response = await apiClient.post<ChatRoom>(`/chats/order/${orderId}/with/${executorId}`);
    return response.data;
  },
};
