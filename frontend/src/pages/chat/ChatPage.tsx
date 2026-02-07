import { useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { format } from 'date-fns';
import { Send, MessageSquare, Check, CheckCheck, Paperclip, X, FileText, Headphones } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Avatar, Input, Button } from '@/components/ui';
import { useChatStore } from '@/stores/chatStore';
import { useAuthStore } from '@/stores/authStore';
import { filesApi } from '@/api/files';
import type { Message } from '@/types';

export function ChatPage() {
  const [searchParams] = useSearchParams();
  const initialChatId = searchParams.get('room') || searchParams.get('chatId');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [messageText, setMessageText] = useState('');
  const [attachments, setAttachments] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);

  const { user } = useAuthStore();
  const {
    chatRooms,
    activeChatId,
    messages,
    typingUsers,
    connected,
    connect,
    disconnect,
    fetchChatRooms,
    setActiveChat,
    sendMessage,
    sendTypingIndicator,
  } = useChatStore();

  useEffect(() => {
    connect();
    fetchChatRooms();

    return () => {
      disconnect();
    };
  }, []);

  useEffect(() => {
    if (initialChatId && !activeChatId) {
      setActiveChat(Number(initialChatId));
    }
  }, [initialChatId, activeChatId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, activeChatId]);

  const activeChat = chatRooms.find((r) => r.id === activeChatId);
  const activeMessages = activeChatId ? messages[activeChatId] || [] : [];
  const activeTyping = activeChatId ? typingUsers[activeChatId] || [] : [];

  const handleSendMessage = () => {
    if ((messageText.trim() || attachments.length > 0) && activeChatId) {
      sendMessage(activeChatId, messageText.trim() || ' ', attachments.length > 0 ? attachments : undefined);
      setMessageText('');
      setAttachments([]);
    }
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    try {
      const uploadPromises = Array.from(files).map((file) => filesApi.upload(file, 'chat'));
      const results = await Promise.all(uploadPromises);
      setAttachments((prev) => [...prev, ...results.map((r) => r.url)]);
    } catch (error) {
      console.error('Failed to upload file:', error);
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const removeAttachment = (index: number) => {
    setAttachments((prev) => prev.filter((_, i) => i !== index));
  };

  const isImageUrl = (url: string) => {
    return /\.(jpg|jpeg|png|gif|webp)$/i.test(url);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleTyping = () => {
    if (activeChatId) {
      sendTypingIndicator(activeChatId);
    }
  };

  return (
    <Layout showFooter={false}>
      <div className="h-[calc(100vh-64px)] flex">
        {/* Chat List */}
        <div className="w-80 border-r border-gray-200 bg-white flex flex-col">
          <div className="p-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Сообщения</h2>
            {!connected && (
              <span className="text-xs text-yellow-600">Подключение...</span>
            )}
          </div>
          <div className="flex-1 overflow-y-auto">
            {/* Pinned Support Chat */}
            <div
              onClick={() => window.open('https://t.me/freelancekg_support', '_blank')}
              className="p-4 border-b-2 border-cyan-200 cursor-pointer hover:bg-cyan-50 bg-cyan-50/50"
            >
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-full bg-cyan-500 flex items-center justify-center">
                  <Headphones className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-gray-900">Freelance Поддержка</p>
                    <span className="text-xs bg-cyan-500 text-white px-1.5 py-0.5 rounded">Support</span>
                  </div>
                  <p className="text-sm text-gray-500">
                    Нужна помощь? Напишите нам!
                  </p>
                </div>
              </div>
            </div>

            {chatRooms.length === 0 ? (
              <div className="p-4 text-center text-gray-500">
                <MessageSquare className="w-12 h-12 mx-auto mb-2 text-gray-300" />
                <p>Нет активных чатов</p>
              </div>
            ) : (
              chatRooms.map((chat) => {
                const isActive = chat.id === activeChatId;

                return (
                  <div
                    key={chat.id}
                    onClick={() => setActiveChat(chat.id)}
                    className={`p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-50 ${
                      isActive ? 'bg-primary-50' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <Avatar src={chat.participantAvatarUrl} name={chat.participantName} size="md" />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <p className="font-medium text-gray-900 truncate">
                            {chat.participantName}
                          </p>
                          {chat.lastMessageAt && (
                            <span className="text-xs text-gray-500">
                              {format(new Date(chat.lastMessageAt), 'HH:mm')}
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-500 truncate">
                          {chat.orderTitle}
                        </p>
                        {chat.lastMessage && (
                          <p className="text-sm text-gray-600 truncate">
                            {chat.lastMessage}
                          </p>
                        )}
                      </div>
                      {chat.unreadCount > 0 && (
                        <span className="bg-primary-600 text-white text-xs font-medium px-2 py-0.5 rounded-full">
                          {chat.unreadCount}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Chat Window */}
        <div className="flex-1 flex flex-col bg-gray-50">
          {activeChat ? (
            <>
              {/* Chat Header */}
              <div className="p-4 bg-white border-b border-gray-200 flex items-center gap-3">
                <Avatar
                  src={activeChat.participantAvatarUrl}
                  name={activeChat.participantName}
                  size="md"
                />
                <div>
                  <p className="font-medium text-gray-900">
                    {activeChat.participantName}
                  </p>
                  <p className="text-sm text-gray-500">{activeChat.orderTitle}</p>
                </div>
              </div>

              {/* Messages */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {activeMessages.map((message: Message) => {
                  const isOwn = message.senderId === user?.id;

                  return (
                    <div
                      key={message.id}
                      className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
                    >
                      <div
                        className={`max-w-[70%] rounded-2xl px-4 py-2 ${
                          isOwn
                            ? 'bg-primary-600 text-white'
                            : 'bg-white text-gray-900 shadow-sm'
                        }`}
                      >
                        {message.content && message.content.trim() && (
                          <p className="whitespace-pre-wrap break-words">{message.content}</p>
                        )}
                        {/* Attachments */}
                        {message.attachments && message.attachments.length > 0 && (
                          <div className="mt-2 space-y-2">
                            {message.attachments.map((url, idx) => (
                              isImageUrl(url) ? (
                                <a
                                  key={idx}
                                  href={url}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="block"
                                >
                                  <img
                                    src={url}
                                    alt="Attachment"
                                    className="max-w-full rounded-lg max-h-60 object-cover"
                                  />
                                </a>
                              ) : (
                                <a
                                  key={idx}
                                  href={url}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className={`flex items-center gap-2 p-2 rounded-lg ${
                                    isOwn ? 'bg-primary-700' : 'bg-gray-100'
                                  }`}
                                >
                                  <FileText className="w-4 h-4" />
                                  <span className="text-sm truncate">
                                    {url.split('/').pop()}
                                  </span>
                                </a>
                              )
                            ))}
                          </div>
                        )}
                        <div
                          className={`flex items-center justify-end gap-1 mt-1 text-xs ${
                            isOwn ? 'text-primary-200' : 'text-gray-400'
                          }`}
                        >
                          <span>{format(new Date(message.createdAt), 'HH:mm')}</span>
                          {isOwn && (
                            message.read ? (
                              <CheckCheck className="w-3 h-3" />
                            ) : (
                              <Check className="w-3 h-3" />
                            )
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}

                {activeTyping.length > 0 && (
                  <div className="flex justify-start">
                    <div className="bg-white rounded-2xl px-4 py-2 shadow-sm">
                      <div className="flex gap-1">
                        <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
                        <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-100" />
                        <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-200" />
                      </div>
                    </div>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Input */}
              <div className="p-4 bg-white border-t border-gray-200">
                {/* Attachment Preview */}
                {attachments.length > 0 && (
                  <div className="mb-3 flex flex-wrap gap-2">
                    {attachments.map((url, idx) => (
                      <div key={idx} className="relative group">
                        {isImageUrl(url) ? (
                          <img
                            src={url}
                            alt="Attachment"
                            className="h-16 w-16 object-cover rounded-lg border border-gray-200"
                          />
                        ) : (
                          <div className="h-16 w-16 flex items-center justify-center bg-gray-100 rounded-lg border border-gray-200">
                            <FileText className="w-6 h-6 text-gray-500" />
                          </div>
                        )}
                        <button
                          onClick={() => removeAttachment(idx)}
                          className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
                <div className="flex gap-3">
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept="image/*,.pdf,.doc,.docx,.txt"
                    onChange={handleFileSelect}
                    className="hidden"
                  />
                  <Button
                    variant="outline"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                  >
                    {uploading ? (
                      <div className="w-4 h-4 border-2 border-gray-300 border-t-primary-600 rounded-full animate-spin" />
                    ) : (
                      <Paperclip className="w-4 h-4" />
                    )}
                  </Button>
                  <Input
                    value={messageText}
                    onChange={(e) => {
                      setMessageText(e.target.value);
                      handleTyping();
                    }}
                    onKeyPress={handleKeyPress}
                    placeholder="Введите сообщение..."
                    className="flex-1"
                  />
                  <Button
                    onClick={handleSendMessage}
                    disabled={!messageText.trim() && attachments.length === 0}
                  >
                    <Send className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center">
              <div className="text-center text-gray-500">
                <MessageSquare className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                <p className="text-lg">Выберите чат для начала общения</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}
