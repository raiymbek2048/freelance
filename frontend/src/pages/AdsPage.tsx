import { Link } from 'react-router-dom';
import { Megaphone, Clock, ArrowLeft } from 'lucide-react';
import { Layout } from '@/components/layout';

export function AdsPage() {
  return (
    <Layout>
      <div className="max-w-2xl mx-auto px-4 py-16">
        <div
          className="rounded-2xl p-8 text-center"
          style={{
            backgroundColor: 'rgba(200, 220, 240, 0.5)',
            backdropFilter: 'blur(10px)',
          }}
        >
          <div className="flex items-center justify-center w-20 h-20 bg-cyan-100 rounded-full mx-auto mb-6">
            <Megaphone className="w-10 h-10 text-cyan-600" />
          </div>

          <h1 className="text-2xl font-bold text-gray-900 mb-3">
            Объявления
          </h1>

          <div className="flex items-center justify-center gap-2 text-amber-600 mb-4">
            <Clock className="w-5 h-5" />
            <span className="font-medium">Скоро будет доступно</span>
          </div>

          <p className="text-gray-600 mb-8 max-w-md mx-auto">
            Мы работаем над разделом объявлений. Здесь вы сможете размещать
            и находить различные объявления и услуги.
          </p>

          <Link
            to="/"
            className="inline-flex items-center gap-2 px-6 py-3 bg-cyan-500 text-white rounded-lg font-medium hover:bg-cyan-600 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Вернуться на главную
          </Link>
        </div>
      </div>
    </Layout>
  );
}
