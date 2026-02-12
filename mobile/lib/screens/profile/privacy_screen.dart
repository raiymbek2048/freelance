import 'package:flutter/material.dart';
import 'package:freelance_kg/core/theme.dart';

class PrivacyScreen extends StatelessWidget {
  const PrivacyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Политика конфиденциальности')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Политика конфиденциальности',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Платформы Freelance.kg',
                  style: TextStyle(fontSize: 14, color: AppTheme.textMuted),
                ),
                const SizedBox(height: 24),

                _section('1. Общие положения', [
                  'Настоящая Политика конфиденциальности регулирует порядок обработки и защиты персональных данных пользователей платформы Freelance.kg (далее — «Платформа»).',
                  '',
                  'Оператором персональных данных является:\nИП [ФИО полностью]\nИНН: [указать]\nАдрес регистрации: [указать]\nEmail: freelancekg.info@gmail.com',
                  '',
                  'Обработка персональных данных осуществляется в соответствии с законодательством Кыргызской Республики.',
                ]),

                _section('2. Персональные данные, которые мы собираем', [
                  'При использовании Платформы могут обрабатываться следующие данные:',
                ]),
                _subSection('2.1. При регистрации:'),
                _bulletList([
                  'Имя',
                  'Адрес электронной почты',
                  'Номер телефона',
                ]),
                _subSection('2.2. При верификации личности (KYC):'),
                _bulletList([
                  'Фото или скан документа, удостоверяющего личность',
                  'Фотография пользователя с документом',
                  'Иные данные, необходимые для подтверждения личности',
                ]),
                _subSection('2.3. Автоматически собираемые данные:'),
                _bulletList([
                  'IP-адрес',
                  'Данные файлов cookie',
                  'Технические данные устройства',
                ]),

                _section('3. Правовые основания обработки', [
                  'Обработка персональных данных осуществляется на основании:',
                ]),
                _bulletList([
                  'Добровольного согласия пользователя',
                  'Необходимости исполнения пользовательского соглашения',
                  'Требований законодательства Кыргызской Республики',
                ]),
                _paragraph('Регистрация на Платформе означает согласие пользователя на обработку его персональных данных.'),

                _section('4. Цели обработки персональных данных', [
                  'Персональные данные используются для:',
                ]),
                _bulletList([
                  'Идентификации пользователя',
                  'Обеспечения функционирования Платформы',
                  'Верификации личности исполнителей (KYC)',
                  'Предотвращения мошенничества',
                  'Обработки обращений в службу поддержки',
                  'Улучшения качества сервиса',
                  'Соблюдения требований законодательства',
                ]),

                _section('5. Хранение и защита данных', [
                  'Персональные данные:',
                ]),
                _bulletList([
                  'Хранятся на защищённых серверах, расположенных на территории Кыргызской Республики',
                  'Обрабатываются с применением технических и организационных мер защиты',
                  'Доступны только уполномоченным лицам',
                ]),
                _paragraph('Данные хранятся в течение срока использования аккаунта, а также в течение срока, установленного законодательством.'),

                _section('6. Передача данных третьим лицам', [
                  'Платформа не передаёт персональные данные третьим лицам, за исключением случаев:',
                ]),
                _bulletList([
                  'Прямого согласия пользователя',
                  'Требований государственных органов в рамках закона',
                  'Защиты прав и законных интересов Платформы',
                ]),

                _section('7. Публичная информация', [
                  'Следующие данные могут отображаться публично:',
                ]),
                _bulletList([
                  'Имя или псевдоним',
                  'Аватар',
                  'Рейтинг',
                  'Специализация',
                  'Отзывы',
                ]),
                _paragraph('Пользователь может скрыть профиль в настройках аккаунта.'),

                _section('8. Права пользователя', [
                  'Пользователь имеет право:',
                ]),
                _bulletList([
                  'Получать информацию об обработке своих данных',
                  'Требовать уточнения данных',
                  'Требовать удаления данных',
                  'Отозвать согласие на обработку',
                ]),
                _paragraph('Запрос направляется на: freelancekg.info@gmail.com'),

                _section('9. Удаление аккаунта', [
                  'По запросу пользователя аккаунт может быть удалён. Данные, обязательные к хранению по законодательству, могут храниться в течение установленного законом срока.',
                ]),

                _section('10. Изменения политики', [
                  'Платформа вправе вносить изменения в настоящую Политику. Актуальная версия размещается на сайте.',
                ]),

                const SizedBox(height: 16),
                const Divider(),
                const SizedBox(height: 8),
                const Text(
                  'Дата вступления в силу: февраль 2026',
                  style: TextStyle(fontSize: 11, color: AppTheme.textMuted),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

Widget _section(String title, List<String> paragraphs) {
  return Padding(
    padding: const EdgeInsets.only(top: 20),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
        const SizedBox(height: 8),
        ...paragraphs.map((p) => p.isEmpty
            ? const SizedBox(height: 8)
            : Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(p,
                    style: const TextStyle(
                        fontSize: 13, color: AppTheme.textSecondary, height: 1.5)),
              )),
      ],
    ),
  );
}

Widget _subSection(String title) {
  return Padding(
    padding: const EdgeInsets.only(top: 12, bottom: 4),
    child: Text(title,
        style: const TextStyle(
            fontSize: 13, fontWeight: FontWeight.w600, color: AppTheme.textSecondary)),
  );
}

Widget _bulletList(List<String> items) {
  return Padding(
    padding: const EdgeInsets.only(left: 8),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: items
          .map((item) => Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Padding(
                      padding: EdgeInsets.only(top: 6),
                      child: Icon(Icons.circle, size: 5, color: AppTheme.textMuted),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(item,
                          style: const TextStyle(
                              fontSize: 13, color: AppTheme.textSecondary, height: 1.4)),
                    ),
                  ],
                ),
              ))
          .toList(),
    ),
  );
}

Widget _paragraph(String text) {
  return Padding(
    padding: const EdgeInsets.only(top: 8),
    child: Text(text,
        style: const TextStyle(
            fontSize: 13, color: AppTheme.textSecondary, height: 1.5)),
  );
}
