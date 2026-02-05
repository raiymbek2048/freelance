import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { Layout } from '@/components/layout';

export function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { fetchUser } = useAuthStore();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');
    const errorParam = searchParams.get('error');

    if (errorParam) {
      setError('Ошибка авторизации через Google. Попробуйте ещё раз.');
      setTimeout(() => navigate('/login'), 3000);
      return;
    }

    if (accessToken && refreshToken) {
      // Save tokens
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // Fetch user data and redirect
      fetchUser().then(() => {
        navigate('/');
      }).catch(() => {
        setError('Ошибка загрузки профиля');
        setTimeout(() => navigate('/login'), 3000);
      });
    } else {
      setError('Токены не получены');
      setTimeout(() => navigate('/login'), 3000);
    }
  }, [searchParams, navigate, fetchUser]);

  return (
    <Layout>
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="text-center">
          {error ? (
            <>
              <div className="text-red-500 text-lg mb-4">{error}</div>
              <p className="text-gray-500">Перенаправление на страницу входа...</p>
            </>
          ) : (
            <>
              <div className="animate-spin w-12 h-12 border-4 border-primary-600 border-t-transparent rounded-full mx-auto mb-4" />
              <p className="text-gray-600">Выполняется вход...</p>
            </>
          )}
        </div>
      </div>
    </Layout>
  );
}
