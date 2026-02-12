import { Layout } from '@/components/layout';
import { PageMeta } from '@/components/PageMeta';
import { Card } from '@/components/ui';

export function TermsPage() {
  return (
    <Layout>
      <div className="max-w-3xl mx-auto px-4 py-8">
        <PageMeta title="Условия использования" description="Условия использования платформы Freelance.kg." />
        <h1 className="text-3xl font-bold text-white mb-2">Условия использования</h1>
        <p className="text-gray-400 mb-8">Платформы Freelance.kg</p>

        <Card padding="lg">
          <div className="prose prose-sm max-w-none text-gray-700 space-y-6">

            <section>
              <h2 className="text-lg font-semibold text-gray-900">1. Статус Платформы</h2>
              <p>
                Freelance.kg является цифровой площадкой (агрегатором), предоставляющей техническую
                возможность размещения и выполнения заданий.
              </p>
              <p>Платформа:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Не является работодателем</li>
                <li>Не является стороной договора между заказчиком и исполнителем</li>
                <li>Не несёт ответственности за выполнение обязательств между пользователями</li>
              </ul>
              <p>
                Отношения между заказчиком и исполнителем носят гражданско-правовой характер.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">2. Регистрация</h2>
              <p>Для использования сервиса пользователь обязан:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Предоставить достоверные данные</li>
                <li>Поддерживать актуальность информации</li>
                <li>Не создавать дублирующие аккаунты</li>
              </ul>
              <p>
                Администрация вправе отказать в регистрации или заблокировать аккаунт при нарушении правил.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">3. Верификация личности (KYC)</h2>
              <p>
                Платформа вправе запросить документы для подтверждения личности исполнителя.
              </p>
              <p>Документы используются исключительно для:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Подтверждения личности</li>
                <li>Снижения мошенничества</li>
                <li>Повышения доверия</li>
              </ul>
              <p>
                Предоставление поддельных документов является основанием для блокировки.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">4. Размещение заданий</h2>
              <p>Запрещается размещать задания, связанные с:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Незаконной деятельностью</li>
                <li>Нарушением законодательства</li>
                <li>Нарушением прав третьих лиц</li>
              </ul>
              <p>
                Администрация вправе удалить такие задания без объяснения причин.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">5. Выполнение заданий</h2>
              <p>Исполнитель обязуется:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Оказывать услуги добросовестно</li>
                <li>Соблюдать согласованные сроки</li>
                <li>Соблюдать законодательство КР</li>
              </ul>
              <p>
                Исполнитель самостоятельно несёт ответственность за качество оказанных услуг.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">6. Оплата</h2>
              <p>
                Оплата осуществляется напрямую между пользователями.
              </p>
              <p>Платформа:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Не является платёжным агентом</li>
                <li>Не удерживает денежные средства</li>
                <li>Не несёт ответственности за расчёты между сторонами</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">7. Разрешение споров</h2>
              <p>
                В случае спора стороны могут обратиться к администрации Платформы.
              </p>
              <p>Администрация:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Анализирует предоставленные материалы</li>
                <li>Выносит рекомендательное решение</li>
              </ul>
              <p>
                Решение Платформы не заменяет судебную защиту.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">8. Ответственность</h2>
              <p>
                Платформа предоставляется по принципу «как есть».
              </p>
              <p>Администрация не несёт ответственности за:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Качество выполненных работ</li>
                <li>Финансовые потери</li>
                <li>Упущенную выгоду</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">9. Персональные данные</h2>
              <p>
                Используя Платформу, пользователь соглашается с{' '}
                <a href="/privacy" className="text-primary-600 hover:underline">Политикой конфиденциальности</a>.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">10. Блокировка аккаунта</h2>
              <p>Администрация вправе заблокировать аккаунт в случае:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Мошенничества</li>
                <li>Предоставления ложных данных</li>
                <li>Нарушения настоящих Условий</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-gray-900">11. Изменения условий</h2>
              <p>
                Администрация вправе изменять условия использования.
                Продолжение использования Платформы означает согласие с изменениями.
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
