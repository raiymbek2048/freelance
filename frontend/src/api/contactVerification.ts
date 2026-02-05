import { apiClient } from './client';

export type ContactVerificationType = 'EMAIL' | 'PHONE';

interface SendCodeResponse {
  message: string;
}

interface VerifyCodeResponse {
  success: boolean;
  message: string;
}

interface CanRequestResponse {
  canRequest: boolean;
}

export const contactVerificationApi = {
  sendCode: async (type: ContactVerificationType): Promise<SendCodeResponse> => {
    const response = await apiClient.post('/contact-verification/send-code', { type });
    return response.data;
  },

  verifyCode: async (type: ContactVerificationType, code: string): Promise<VerifyCodeResponse> => {
    const response = await apiClient.post('/contact-verification/verify', { type, code });
    return response.data;
  },

  canRequest: async (type: ContactVerificationType): Promise<CanRequestResponse> => {
    const response = await apiClient.get(`/contact-verification/can-request?type=${type}`);
    return response.data;
  },
};
