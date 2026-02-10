import apiClient from './client';
import type { ExecutorProfile, ExecutorListItem, ExecutorProfileRequest, ExecutorFilters, PageResponse, PortfolioItem, PortfolioCreateRequest, Review } from '@/types';

export const executorsApi = {
  getAll: async (filters: ExecutorFilters = {}, page = 0, size = 20): Promise<PageResponse<ExecutorListItem>> => {
    const params = new URLSearchParams();
    if (filters.categoryId) params.append('categoryId', String(filters.categoryId));
    if (filters.minRating) params.append('minRating', String(filters.minRating));
    if (filters.availableOnly) params.append('availableOnly', 'true');
    if (filters.search) params.append('search', filters.search);
    params.append('page', String(page));
    params.append('size', String(size));

    const response = await apiClient.get<PageResponse<ExecutorListItem>>(`/executors?${params}`);
    return response.data;
  },

  getById: async (id: number): Promise<ExecutorProfile> => {
    const response = await apiClient.get<ExecutorProfile>(`/executors/${id}`);
    return response.data;
  },

  getMyProfile: async (): Promise<ExecutorProfile> => {
    const response = await apiClient.get<ExecutorProfile>('/executors/me/profile');
    return response.data;
  },

  createProfile: async (data: ExecutorProfileRequest): Promise<ExecutorProfile> => {
    const response = await apiClient.post<ExecutorProfile>('/executors/me/profile', data);
    return response.data;
  },

  updateProfile: async (data: ExecutorProfileRequest): Promise<ExecutorProfile> => {
    const response = await apiClient.put<ExecutorProfile>('/executors/me/profile', data);
    return response.data;
  },

  getPortfolio: async (executorId: number): Promise<PortfolioItem[]> => {
    const response = await apiClient.get<PortfolioItem[]>(`/executors/${executorId}/portfolio`);
    return response.data;
  },

  getMyPortfolio: async (): Promise<PortfolioItem[]> => {
    const response = await apiClient.get<PortfolioItem[]>('/portfolio');
    return response.data;
  },

  addPortfolioItem: async (data: PortfolioCreateRequest): Promise<PortfolioItem> => {
    const response = await apiClient.post<PortfolioItem>('/portfolio', data);
    return response.data;
  },

  updatePortfolioItem: async (id: number, data: PortfolioCreateRequest): Promise<PortfolioItem> => {
    const response = await apiClient.put<PortfolioItem>(`/portfolio/${id}`, data);
    return response.data;
  },

  deletePortfolioItem: async (id: number): Promise<void> => {
    await apiClient.delete(`/portfolio/${id}`);
  },

  updateCategories: async (categoryIds: number[]): Promise<ExecutorProfile> => {
    const response = await apiClient.put<ExecutorProfile>('/executors/me/categories', categoryIds);
    return response.data;
  },

  getReviews: async (executorId: number, page = 0, size = 10): Promise<PageResponse<Review>> => {
    const response = await apiClient.get<PageResponse<Review>>(`/executors/${executorId}/reviews?page=${page}&size=${size}`);
    return response.data;
  },
};
