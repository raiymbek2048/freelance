import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Helmet } from 'react-helmet-async';
import { useAuthStore } from '@/stores/authStore';

// Pages
import { HomePage } from '@/pages/HomePage';
import { LoginPage } from '@/pages/auth/LoginPage';
import { RegisterPage } from '@/pages/auth/RegisterPage';
import { OAuthCallbackPage } from '@/pages/auth/OAuthCallbackPage';
import { OrdersListPage } from '@/pages/orders/OrdersListPage';
import { OrderDetailPage } from '@/pages/orders/OrderDetailPage';
import { CreateOrderPage } from '@/pages/orders/CreateOrderPage';
import { MyOrdersPage } from '@/pages/orders/MyOrdersPage';
import { ExecutorsListPage } from '@/pages/executors/ExecutorsListPage';
import { ExecutorDetailPage } from '@/pages/executors/ExecutorDetailPage';
import { ChatPage } from '@/pages/chat/ChatPage';
import { ProfilePage } from '@/pages/ProfilePage';
import { VerificationPage } from '@/pages/VerificationPage';
import { VacanciesPage } from '@/pages/VacanciesPage';
import { AdsPage } from '@/pages/AdsPage';
import { HelpPage } from '@/pages/HelpPage';
import { TermsPage } from '@/pages/TermsPage';
import { PrivacyPage } from '@/pages/PrivacyPage';
import { DisputeDetailPage } from '@/pages/orders/DisputeDetailPage';
import { PaymentSuccessPage } from '@/pages/payment/PaymentSuccessPage';
import { PaymentFailurePage } from '@/pages/payment/PaymentFailurePage';
import {
  AdminDashboardPage,
  AdminDisputesPage,
  AdminDisputeDetailPage,
  AdminOrdersPage,
  AdminUsersPage,
  AdminVerificationsPage,
  AdminSubscriptionPage,
  AdminAnalyticsPage,
} from '@/pages/admin';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60, // 1 minute
      retry: 1,
    },
  },
});

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading, user } = useAuthStore();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

function AppContent() {
  const { fetchUser, isAuthenticated } = useAuthStore();

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token && !isAuthenticated) {
      fetchUser();
    }
  }, []);

  return (
    <>
    <Helmet
      defaultTitle="FreelanceKG — биржа фриланса для Кыргызстана"
      titleTemplate="%s | FreelanceKG"
    />
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      <Route path="/orders" element={<OrdersListPage />} />
      <Route path="/orders/:id" element={<OrderDetailPage />} />
      <Route path="/executors" element={<ExecutorsListPage />} />
      <Route path="/executors/:id" element={<ExecutorDetailPage />} />
      <Route path="/vacancies" element={<VacanciesPage />} />
      <Route path="/ads" element={<AdsPage />} />
      <Route path="/help" element={<HelpPage />} />
      <Route path="/terms" element={<TermsPage />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route path="/payment/success" element={<PaymentSuccessPage />} />
      <Route path="/payment/failure" element={<PaymentFailurePage />} />

      {/* Protected routes */}
      <Route
        path="/orders/create"
        element={
          <ProtectedRoute>
            <CreateOrderPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/my-orders"
        element={
          <ProtectedRoute>
            <MyOrdersPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/chats"
        element={
          <ProtectedRoute>
            <ChatPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/verification"
        element={
          <ProtectedRoute>
            <VerificationPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/orders/:id/dispute"
        element={
          <ProtectedRoute>
            <DisputeDetailPage />
          </ProtectedRoute>
        }
      />

      {/* Admin routes */}
      <Route
        path="/admin"
        element={
          <AdminRoute>
            <AdminDashboardPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/disputes"
        element={
          <AdminRoute>
            <AdminDisputesPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/disputes/:id"
        element={
          <AdminRoute>
            <AdminDisputeDetailPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/orders"
        element={
          <AdminRoute>
            <AdminOrdersPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/users"
        element={
          <AdminRoute>
            <AdminUsersPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/verifications"
        element={
          <AdminRoute>
            <AdminVerificationsPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/subscription"
        element={
          <AdminRoute>
            <AdminSubscriptionPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/analytics"
        element={
          <AdminRoute>
            <AdminAnalyticsPage />
          </AdminRoute>
        }
      />

      {/* Catch all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
    </>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppContent />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
