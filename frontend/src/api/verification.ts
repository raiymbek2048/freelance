import api from './client';
import type { VerificationResponse, VerificationSubmitRequest, AdminVerificationResponse, PageResponse } from '../types';

// User verification endpoints
export const verificationApi = {
  // Get my verification status
  getMyStatus: async (): Promise<VerificationResponse> => {
    const response = await api.get('/verification/status');
    return response.data;
  },

  // Submit verification request
  submit: async (request: VerificationSubmitRequest): Promise<VerificationResponse> => {
    const response = await api.post('/verification/submit', request);
    return response.data;
  },
};

// Admin verification endpoints
export const adminVerificationApi = {
  // Get all verifications
  getAll: async (page = 0, size = 20): Promise<PageResponse<AdminVerificationResponse>> => {
    const response = await api.get('/admin/verifications', {
      params: { page, size },
    });
    return response.data;
  },

  // Get pending verifications
  getPending: async (page = 0, size = 20): Promise<PageResponse<AdminVerificationResponse>> => {
    const response = await api.get('/admin/verifications/pending', {
      params: { page, size },
    });
    return response.data;
  },

  // Get pending count
  getPendingCount: async (): Promise<{ pending: number }> => {
    const response = await api.get('/admin/verifications/count');
    return response.data;
  },

  // Get verification details
  getDetails: async (userId: number): Promise<AdminVerificationResponse> => {
    const response = await api.get(`/admin/verifications/${userId}`);
    return response.data;
  },

  // Approve verification
  approve: async (userId: number): Promise<void> => {
    await api.put(`/admin/verifications/${userId}/approve`);
  },

  // Reject verification
  reject: async (userId: number, reason?: string): Promise<void> => {
    await api.put(`/admin/verifications/${userId}/reject`, null, {
      params: reason ? { reason } : {},
    });
  },
};
