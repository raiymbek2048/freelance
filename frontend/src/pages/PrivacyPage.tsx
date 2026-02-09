import { Layout } from '@/components/layout';
import { Card } from '@/components/ui';

export function PrivacyPage() {
  return (
    <Layout>
      <div className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-white mb-8">Политика конфиденциальности</h1>

        <Card padding="lg">
          <div className="prose prose-sm max-w-none text-gray-700 space-y-6">
            <section>
              <h2 className="text-lg font-semibold text-gray-900">1. Сбор данных</h2>
              <p>
                При регистрации мы собираем следующие данные: имя, адрес электронной почты, номер телефона.
                Для верификации исполнителей может потребоваться загрузка документа, удостоверяющего личность.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">2. Использование данных</h2>
              <p>Мы используем ваши данные для:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Идентификации на платформе</li>
                <li>Обеспечения работы сервиса</li>
                <li>Связи с вами по вопросам заданий и поддержки</li>
                <li>Верификации личности исполнителей</li>
                <li>Улучшения качества сервиса</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">3. Хранение данных</h2>
              <p>
                Ваши данные хранятся на защищённых серверах. Мы принимаем необходимые технические
                и организационные меры для защиты ваших персональных данных от несанкционированного доступа.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">4. Передача данных третьим лицам</h2>
              <p>
                Мы не передаём ваши персональные данные третьим лицам, за исключением случаев,
                предусмотренных законодательством Кыргызской Республики.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">5. Публичная информация</h2>
              <p>
                Имя, аватар, рейтинг и специализация исполнителя отображаются публично в каталоге исполнителей.
                Вы можете скрыть свой профиль из списка исполнителей в настройках.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">6. Удаление данных</h2>
              <p>
                Вы можете запросить удаление своих данных, обратившись в службу поддержки
                по адресу <a href="mailto:freelancekg.info@gmail.com" className="text-primary-600 hover:underline">freelancekg.info@gmail.com</a>.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">7. Файлы cookie</h2>
              <p>
                Платформа использует файлы cookie для обеспечения работы авторизации и улучшения
                пользовательского опыта.
              </p>
            </section>

            <p className="text-xs text-gray-400 pt-4 border-t border-gray-200">
              Последнее обновление: февраль 2026
            </p>
          </div>
        </Card>
      </div>
    </Layout>
  );
}
