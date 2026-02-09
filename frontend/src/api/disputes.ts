import apiClient from './client';
import type {
  DisputeResponse,
  DisputeEvidenceResponse,
  OpenDisputeRequest,
  DisputeEvidenceRequest,
} from '@/types';

export const disputesApi = {
  openDispute: async (orderId: number, data: OpenDisputeRequest): Promise<DisputeResponse> => {
    const response = await apiClient.post<DisputeResponse>(`/disputes/orders/${orderId}`, data);
    return response.data;
  },

  getByOrderId: async (orderId: number): Promise<DisputeResponse> => {
    const response = await apiClient.get<DisputeResponse>(`/disputes/orders/${orderId}`);
    return response.data;
  },

  getById: async (id: number): Promise<DisputeResponse> => {
    const response = await apiClient.get<DisputeResponse>(`/disputes/${id}`);
    return response.data;
  },

  addEvidence: async (disputeId: number, data: DisputeEvidenceRequest): Promise<DisputeEvidenceResponse> => {
    const response = await apiClient.post<DisputeEvidenceResponse>(`/disputes/${disputeId}/evidence`, data);
    return response.data;
  },

  getEvidence: async (disputeId: number): Promise<DisputeEvidenceResponse[]> => {
    const response = await apiClient.get<DisputeEvidenceResponse[]>(`/disputes/${disputeId}/evidence`);
    return response.data;
  },
};
