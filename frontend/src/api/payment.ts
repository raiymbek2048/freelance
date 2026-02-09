import apiClient from './client';
import type { PaymentInitResponse } from '@/types';

export const paymentApi = {
  createSubscriptionPayment: async (days: number = 30): Promise<PaymentInitResponse> => {
    const response = await apiClient.post<PaymentInitResponse>('/payment/subscription', { days });
    return response.data;
  },
};
