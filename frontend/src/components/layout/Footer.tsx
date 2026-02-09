import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="bg-white border-t border-gray-200 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Logo & Description */}
          <div className="col-span-1 md:col-span-2">
            <Link to="/" className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">F</span>
              </div>
              <span className="text-xl font-bold text-gray-900">FreelanceKG</span>
            </Link>
            <p className="text-gray-600 max-w-md">
              Биржа фриланса для Кыргызстана. Находите исполнителей для любых задач или предлагайте свои услуги.
            </p>
          </div>

          {/* Links */}
          <div>
            <h4 className="font-semibold text-gray-900 mb-4">Платформа</h4>
            <ul className="space-y-2">
              <li>
                <Link to="/orders" className="text-gray-600 hover:text-gray-900">
                  Задания
                </Link>
              </li>
              <li>
                <Link to="/executors" className="text-gray-600 hover:text-gray-900">
                  Исполнители
                </Link>
              </li>
              <li>
                <Link to="/vacancies" className="text-gray-600 hover:text-gray-900">
                  Вакансии
                </Link>
              </li>
            </ul>
          </div>

          {/* Support */}
          <div>
            <h4 className="font-semibold text-gray-900 mb-4">Поддержка</h4>
            <ul className="space-y-2">
              <li>
                <Link to="/help" className="text-gray-600 hover:text-gray-900">
                  Помощь
                </Link>
              </li>
              <li>
                <Link to="/terms" className="text-gray-600 hover:text-gray-900">
                  Условия использования
                </Link>
              </li>
              <li>
                <Link to="/privacy" className="text-gray-600 hover:text-gray-900">
                  Политика конфиденциальности
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-8 pt-8 border-t border-gray-200 text-center text-gray-500 text-sm">
          <p>&copy; {new Date().getFullYear()} FreelanceKG. Все права защищены.</p>
        </div>
      </div>
    </footer>
  );
}
