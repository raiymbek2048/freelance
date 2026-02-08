import apiClient from './client';
import type {
  AnnouncementResponse,
  MySubscriptionResponse,
  SubscriptionSettingsResponse,
  SubscriptionSettingsRequest,
  UserSubscriptionResponse,
  GrantSubscriptionRequest,
  PageResponse,
} from '@/types';

// Public subscription API
export const subscriptionApi = {
  // Get announcement for main page (public)
  getAnnouncement: async (): Promise<AnnouncementResponse> => {
    const response = await apiClient.get<AnnouncementResponse>('/subscription/announcement');
    return response.data;
  },

  // Get current user's subscription status
  getMySubscription: async (): Promise<MySubscriptionResponse> => {
    const response = await apiClient.get<MySubscriptionResponse>('/subscription/my');
    return response.data;
  },

  // Start free trial
  startTrial: async (): Promise<MySubscriptionResponse> => {
    const response = await apiClient.post<MySubscriptionResponse>('/subscription/start-trial');
    return response.data;
  },
};

// Admin subscription API
export const adminSubscriptionApi = {
  // Get subscription settings
  getSettings: async (): Promise<SubscriptionSettingsResponse> => {
    const response = await apiClient.get<SubscriptionSettingsResponse>('/admin/subscription/settings');
    return response.data;
  },

  // Update subscription settings
  updateSettings: async (data: SubscriptionSettingsRequest): Promise<SubscriptionSettingsResponse> => {
    const response = await apiClient.put<SubscriptionSettingsResponse>('/admin/subscription/settings', data);
    return response.data;
  },

  // Get all user subscriptions
  getSubscriptions: async (page = 0, size = 20): Promise<PageResponse<UserSubscriptionResponse>> => {
    const response = await apiClient.get<PageResponse<UserSubscriptionResponse>>(
      `/admin/subscription/users?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Grant subscription to a user
  grantSubscription: async (userId: number, data: GrantSubscriptionRequest): Promise<UserSubscriptionResponse> => {
    const response = await apiClient.post<UserSubscriptionResponse>(
      `/admin/subscription/users/${userId}/grant`,
      data
    );
    return response.data;
  },

  // Revoke user's subscription
  revokeSubscription: async (userId: number): Promise<void> => {
    await apiClient.delete(`/admin/subscription/users/${userId}`);
  },
};
