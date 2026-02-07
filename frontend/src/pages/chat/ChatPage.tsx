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
  const [showSupportModal, setShowSupportModal] = useState(false);

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
              onClick={() => setShowSupportModal(true)}
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

      {/* Support Modal */}
      {showSupportModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl">
            <div className="flex items-center justify-center w-16 h-16 bg-cyan-100 rounded-full mx-auto mb-4">
              <Headphones className="w-8 h-8 text-cyan-600" />
            </div>
            <h3 className="text-xl font-bold text-center text-gray-900 mb-2">
              Freelance Поддержка
            </h3>
            <p className="text-gray-600 text-center mb-6">
              Выберите удобный способ связи
            </p>
            <div className="space-y-3">
              <button
                onClick={() => {
                  window.open('https://t.me/freelancekg_support', '_blank');
                  setShowSupportModal(false);
                }}
                className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-[#0088cc] text-white rounded-xl font-medium hover:bg-[#0077b5] transition-colors"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm5.562 8.161c-.18 1.897-.962 6.502-1.359 8.627-.168.9-.5 1.201-.82 1.23-.697.064-1.226-.461-1.901-.903-1.056-.692-1.653-1.123-2.678-1.799-1.185-.781-.417-1.21.258-1.911.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.139-5.062 3.345-.479.329-.913.489-1.302.481-.428-.009-1.252-.242-1.865-.44-.751-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.831-2.529 6.998-3.015 3.333-1.386 4.025-1.627 4.477-1.635.099-.002.321.023.465.141.12.098.153.228.168.327.015.099.034.323.019.498z"/>
                </svg>
                Telegram
              </button>
              <button
                onClick={() => {
                  window.open('https://wa.me/996888444999', '_blank');
                  setShowSupportModal(false);
                }}
                className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-[#25D366] text-white rounded-xl font-medium hover:bg-[#22c55e] transition-colors"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
                WhatsApp
              </button>
            </div>
            <button
              onClick={() => setShowSupportModal(false)}
              className="w-full mt-4 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-xl transition-colors"
            >
              Отмена
            </button>
          </div>
        </div>
      )}
    </Layout>
  );
}
