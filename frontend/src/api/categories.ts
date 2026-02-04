import apiClient from './client';
import type { Category } from '@/types';

export const categoriesApi = {
  getAll: async (): Promise<Category[]> => {
    const response = await apiClient.get<Category[]>('/categories');
    return response.data;
  },

  getTree: async (): Promise<Category[]> => {
    const response = await apiClient.get<Category[]>('/categories/tree');
    return response.data;
  },

  getById: async (id: number): Promise<Category> => {
    const response = await apiClient.get<Category>(`/categories/${id}`);
    return response.data;
  },
};
