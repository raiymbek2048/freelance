import { XCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Layout } from '@/components/layout';
import { PageMeta } from '@/components/PageMeta';
import { Button } from '@/components/ui';

export function PaymentFailurePage() {
  return (
    <Layout>
      <PageMeta title="Ошибка оплаты" />
      <div className="max-w-lg mx-auto px-4 py-16 text-center">
        <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <XCircle className="w-8 h-8 text-red-600" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-3">Ошибка оплаты</h1>
        <p className="text-gray-600 mb-8">
          К сожалению, оплата не прошла. Попробуйте ещё раз или свяжитесь с поддержкой.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link to="/profile">
            <Button>Попробовать снова</Button>
          </Link>
          <Link to="/">
            <Button variant="outline">На главную</Button>
          </Link>
        </div>
      </div>
    </Layout>
  );
}
