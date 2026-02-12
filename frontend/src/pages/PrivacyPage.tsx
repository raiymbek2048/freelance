import { Layout } from '@/components/layout';
import { PageMeta } from '@/components/PageMeta';
import { Card } from '@/components/ui';

export function PrivacyPage() {
  return (
    <Layout>
      <div className="max-w-3xl mx-auto px-4 py-8">
        <PageMeta title="Политика конфиденциальности" description="Политика конфиденциальности платформы Freelance.kg. Порядок обработки и защиты персональных данных." />
        <h1 className="text-3xl font-bold text-white mb-2">Политика конфиденциальности</h1>
        <p className="text-gray-400 mb-8">Платформы Freelance.kg</p>

        <Card padding="lg">
          <div className="prose prose-sm max-w-none text-gray-700 space-y-6">

            <section>
              <h2 className="text-lg font-semibold text-gray-900">1. Общие положения</h2>
              <p>
                Настоящая Политика конфиденциальности регулирует порядок обработки и защиты персональных данных
                пользователей платформы Freelance.kg (далее — «Платформа»).
              </p>
              <p>
                Оператором персональных данных является:<br />
                ИП [ФИО полностью]<br />
                ИНН: [указать]<br />
                Адрес регистрации: [указать]<br />
                Email: freelancekg.info@gmail.com
              </p>
              <p>
                Обработка персональных данных осуществляется в соответствии с законодательством Кыргызской Республики.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">2. Персональные данные, которые мы собираем</h2>
              <p>При использовании Платформы могут обрабатываться следующие данные:</p>

              <p className="font-medium text-gray-800 mt-3 mb-1">2.1. При регистрации:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Имя</li>
                <li>Адрес электронной почты</li>
                <li>Номер телефона</li>
              </ul>

              <p className="font-medium text-gray-800 mt-3 mb-1">2.2. При верификации личности (KYC):</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Фото или скан документа, удостоверяющего личность</li>
                <li>Фотография пользователя с документом</li>
                <li>Иные данные, необходимые для подтверждения личности</li>
              </ul>

              <p className="font-medium text-gray-800 mt-3 mb-1">2.3. Автоматически собираемые данные:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>IP-адрес</li>
                <li>Данные файлов cookie</li>
                <li>Технические данные устройства</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">3. Правовые основания обработки</h2>
              <p>Обработка персональных данных осуществляется на основании:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Добровольного согласия пользователя</li>
                <li>Необходимости исполнения пользовательского соглашения</li>
                <li>Требований законодательства Кыргызской Республики</li>
              </ul>
              <p>
                Регистрация на Платформе означает согласие пользователя на обработку его персональных данных.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">4. Цели обработки персональных данных</h2>
              <p>Персональные данные используются для:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Идентификации пользователя</li>
                <li>Обеспечения функционирования Платформы</li>
                <li>Верификации личности исполнителей (KYC)</li>
                <li>Предотвращения мошенничества</li>
                <li>Обработки обращений в службу поддержки</li>
                <li>Улучшения качества сервиса</li>
                <li>Соблюдения требований законодательства</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">5. Хранение и защита данных</h2>
              <p>Персональные данные:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Хранятся на защищённых серверах, расположенных на территории Кыргызской Республики</li>
                <li>Обрабатываются с применением технических и организационных мер защиты</li>
                <li>Доступны только уполномоченным лицам</li>
              </ul>
              <p>
                Данные хранятся в течение срока использования аккаунта, а также в течение срока,
                установленного законодательством.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">6. Передача данных третьим лицам</h2>
              <p>Платформа не передаёт персональные данные третьим лицам, за исключением случаев:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Прямого согласия пользователя</li>
                <li>Требований государственных органов в рамках закона</li>
                <li>Защиты прав и законных интересов Платформы</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">7. Публичная информация</h2>
              <p>Следующие данные могут отображаться публично:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Имя или псевдоним</li>
                <li>Аватар</li>
                <li>Рейтинг</li>
                <li>Специализация</li>
                <li>Отзывы</li>
              </ul>
              <p>Пользователь может скрыть профиль в настройках аккаунта.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">8. Права пользователя</h2>
              <p>Пользователь имеет право:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Получать информацию об обработке своих данных</li>
                <li>Требовать уточнения данных</li>
                <li>Требовать удаления данных</li>
                <li>Отозвать согласие на обработку</li>
              </ul>
              <p>
                Запрос направляется на: <a href="mailto:freelancekg.info@gmail.com" className="text-primary-600 hover:underline">freelancekg.info@gmail.com</a>
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">9. Удаление аккаунта</h2>
              <p>
                По запросу пользователя аккаунт может быть удалён. Данные, обязательные к хранению
                по законодательству, могут храниться в течение установленного законом срока.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">10. Изменения политики</h2>
              <p>
                Платформа вправе вносить изменения в настоящую Политику.
                Актуальная версия размещается на сайте.
              </p>
            </section>

            <p className="text-xs text-gray-400 pt-4 border-t border-gray-200">
              Дата вступления в силу: февраль 2026
            </p>
          </div>
        </Card>
      </div>
    </Layout>
  );
}
