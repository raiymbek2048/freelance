import apiClient from './client';
import type { Order, OrderListItem, OrderDetail, OrderCreateRequest, OrderFilters, OrderResponse, OrderResponseRequest, PageResponse, Review, ReviewCreateRequest } from '@/types';

export const ordersApi = {
  getAll: async (filters: OrderFilters = {}, page = 0, size = 20): Promise<PageResponse<OrderListItem>> => {
    const params = new URLSearchParams();
    if (filters.categoryId) params.append('categoryId', String(filters.categoryId));
    if (filters.status) params.append('status', filters.status);
    if (filters.minBudget) params.append('budgetMin', String(filters.minBudget));
    if (filters.maxBudget) params.append('budgetMax', String(filters.maxBudget));
    if (filters.search) params.append('search', filters.search);
    params.append('page', String(page));
    params.append('size', String(size));

    const response = await apiClient.get<PageResponse<OrderListItem>>(`/orders?${params}`);
    return response.data;
  },

  getById: async (id: number): Promise<OrderDetail> => {
    const response = await apiClient.get<OrderDetail>(`/orders/${id}`);
    return response.data;
  },

  create: async (data: OrderCreateRequest): Promise<Order> => {
    const response = await apiClient.post<Order>('/orders', data);
    return response.data;
  },

  update: async (id: number, data: Partial<OrderCreateRequest>): Promise<Order> => {
    const response = await apiClient.put<Order>(`/orders/${id}`, data);
    return response.data;
  },

  cancel: async (id: number): Promise<void> => {
    await apiClient.put(`/orders/${id}/cancel`);
  },

  getMyOrdersAsClient: async (page = 0, size = 20): Promise<PageResponse<OrderListItem>> => {
    const response = await apiClient.get<PageResponse<OrderListItem>>(`/orders/my/as-client?page=${page}&size=${size}`);
    return response.data;
  },

  getMyOrdersAsExecutor: async (page = 0, size = 20): Promise<PageResponse<OrderListItem>> => {
    const response = await apiClient.get<PageResponse<OrderListItem>>(`/orders/my/as-executor?page=${page}&size=${size}`);
    return response.data;
  },

  // Responses
  getResponses: async (orderId: number): Promise<OrderResponse[]> => {
    const response = await apiClient.get<OrderResponse[]>(`/orders/${orderId}/responses`);
    return response.data;
  },

  respond: async (orderId: number, data: OrderResponseRequest): Promise<OrderResponse> => {
    const response = await apiClient.post<OrderResponse>(`/orders/${orderId}/responses`, data);
    return response.data;
  },

  selectExecutor: async (orderId: number, responseId: number, agreedPrice?: number, agreedDeadline?: string): Promise<Order> => {
    const response = await apiClient.post<Order>(`/orders/${orderId}/select-executor`, {
      responseId,
      agreedPrice,
      agreedDeadline,
    });
    return response.data;
  },

  // Status changes
  submitForReview: async (id: number): Promise<void> => {
    await apiClient.post(`/orders/${id}/submit-for-review`);
  },

  approveWork: async (id: number): Promise<void> => {
    await apiClient.post(`/orders/${id}/approve`);
  },

  requestRevision: async (id: number, reason?: string): Promise<void> => {
    await apiClient.post(`/orders/${id}/request-revision`, null, { params: reason ? { reason } : {} });
  },

  openDispute: async (id: number, reason?: string): Promise<void> => {
    await apiClient.post(`/orders/${id}/dispute`, null, { params: reason ? { reason } : {} });
  },

  // Reviews
  createReview: async (orderId: number, data: ReviewCreateRequest): Promise<Review> => {
    const response = await apiClient.post<Review>(`/reviews/orders/${orderId}`, data);
    return response.data;
  },
};
