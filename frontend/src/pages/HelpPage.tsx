import { Layout } from '@/components/layout';
import { PageMeta } from '@/components/PageMeta';
import { Card } from '@/components/ui';
import { HelpCircle, MessageCircle, Mail, Send } from 'lucide-react';

export function HelpPage() {
  return (
    <Layout>
      <div className="max-w-3xl mx-auto px-4 py-8">
        <PageMeta title="Помощь" description="Часто задаваемые вопросы и контакты поддержки FreelanceKG." />
        <h1 className="text-3xl font-bold text-white mb-8">Помощь</h1>

        <div className="space-y-6">
          <Card padding="lg">
            <div className="flex items-center gap-3 mb-4">
              <HelpCircle className="w-5 h-5 text-primary-600" />
              <h2 className="text-lg font-semibold">Часто задаваемые вопросы</h2>
            </div>
            <div className="space-y-4">
              <div>
                <h3 className="font-medium text-gray-900 mb-1">Как разместить задание?</h3>
                <p className="text-sm text-gray-600">
                  Нажмите кнопку «Дать задание» в верхнем меню, заполните форму с описанием задачи, укажите бюджет и срок выполнения.
                </p>
              </div>
              <div>
                <h3 className="font-medium text-gray-900 mb-1">Как стать исполнителем?</h3>
                <p className="text-sm text-gray-600">
                  Зарегистрируйтесь на платформе, пройдите верификацию и заполните профиль исполнителя. После этого вы сможете откликаться на задания.
                </p>
              </div>
              <div>
                <h3 className="font-medium text-gray-900 mb-1">Как работает оплата?</h3>
                <p className="text-sm text-gray-600">
                  Оплата производится напрямую между заказчиком и исполнителем. Платформа не взимает комиссию за переводы.
                </p>
              </div>
              <div>
                <h3 className="font-medium text-gray-900 mb-1">Что делать, если возник спор?</h3>
                <p className="text-sm text-gray-600">
                  Вы можете открыть диспут на странице заказа. Администратор рассмотрит обращение и примет решение.
                </p>
              </div>
              <div>
                <h3 className="font-medium text-gray-900 mb-1">Как пройти верификацию?</h3>
                <p className="text-sm text-gray-600">
                  Перейдите в настройки профиля и нажмите «Перейти к верификации». Загрузите фото паспорта и селфи — администратор проверит данные.
                </p>
              </div>
            </div>
          </Card>

          <Card padding="lg">
            <div className="flex items-center gap-3 mb-4">
              <MessageCircle className="w-5 h-5 text-primary-600" />
              <h2 className="text-lg font-semibold">Связаться с нами</h2>
            </div>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <Mail className="w-4 h-4 text-gray-500" />
                <a href="mailto:freelancekg.info@gmail.com" className="text-primary-600 hover:underline">
                  freelancekg.info@gmail.com
                </a>
              </div>
              <div className="flex items-center gap-3">
                <Send className="w-4 h-4 text-gray-500" />
                <a href="https://t.me/freelancekg_support" target="_blank" rel="noopener noreferrer" className="text-primary-600 hover:underline">
                  Telegram
                </a>
              </div>
              <div className="flex items-center gap-3">
                <MessageCircle className="w-4 h-4 text-gray-500" />
                <a href="https://wa.me/996888444999" target="_blank" rel="noopener noreferrer" className="text-primary-600 hover:underline">
                  WhatsApp
                </a>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </Layout>
  );
}
