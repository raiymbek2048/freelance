import { apiClient } from './client';

interface GoogleAuthUrlResponse {
  url: string;
}

export const oauthApi = {
  getGoogleAuthUrl: async (): Promise<string> => {
    const response = await apiClient.get<GoogleAuthUrlResponse>('/auth/oauth2/google/url');
    return response.data.url;
  },
};
