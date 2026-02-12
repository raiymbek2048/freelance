import 'package:flutter/material.dart';
import 'package:freelance_kg/core/theme.dart';

class TermsScreen extends StatelessWidget {
  const TermsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Условия использования')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Условия использования',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Платформы Freelance.kg',
                  style: TextStyle(fontSize: 14, color: AppTheme.textMuted),
                ),
                const SizedBox(height: 24),

                _section('1. Статус Платформы', [
                  'Freelance.kg является цифровой площадкой (агрегатором), предоставляющей техническую возможность размещения и выполнения заданий.',
                  '',
                  'Платформа:',
                ]),
                _bulletList([
                  'Не является работодателем',
                  'Не является стороной договора между заказчиком и исполнителем',
                  'Не несёт ответственности за выполнение обязательств между пользователями',
                ]),
                _paragraph('Отношения между заказчиком и исполнителем носят гражданско-правовой характер.'),

                _section('2. Регистрация', [
                  'Для использования сервиса пользователь обязан:',
                ]),
                _bulletList([
                  'Предоставить достоверные данные',
                  'Поддерживать актуальность информации',
                  'Не создавать дублирующие аккаунты',
                ]),
                _paragraph('Администрация вправе отказать в регистрации или заблокировать аккаунт при нарушении правил.'),

                _section('3. Верификация личности (KYC)', [
                  'Платформа вправе запросить документы для подтверждения личности исполнителя.',
                  '',
                  'Документы используются исключительно для:',
                ]),
                _bulletList([
                  'Подтверждения личности',
                  'Снижения мошенничества',
                  'Повышения доверия',
                ]),
                _paragraph('Предоставление поддельных документов является основанием для блокировки.'),

                _section('4. Размещение заданий', [
                  'Запрещается размещать задания, связанные с:',
                ]),
                _bulletList([
                  'Незаконной деятельностью',
                  'Нарушением законодательства',
                  'Нарушением прав третьих лиц',
                ]),
                _paragraph('Администрация вправе удалить такие задания без объяснения причин.'),

                _section('5. Выполнение заданий', [
                  'Исполнитель обязуется:',
                ]),
                _bulletList([
                  'Оказывать услуги добросовестно',
                  'Соблюдать согласованные сроки',
                  'Соблюдать законодательство КР',
                ]),
                _paragraph('Исполнитель самостоятельно несёт ответственность за качество оказанных услуг.'),

                _section('6. Оплата', [
                  'Оплата осуществляется напрямую между пользователями.',
                  '',
                  'Платформа:',
                ]),
                _bulletList([
                  'Не является платёжным агентом',
                  'Не удерживает денежные средства',
                  'Не несёт ответственности за расчёты между сторонами',
                ]),

                _section('7. Разрешение споров', [
                  'В случае спора стороны могут обратиться к администрации Платформы.',
                  '',
                  'Администрация:',
                ]),
                _bulletList([
                  'Анализирует предоставленные материалы',
                  'Выносит рекомендательное решение',
                ]),
                _paragraph('Решение Платформы не заменяет судебную защиту.'),

                _section('8. Ответственность', [
                  'Платформа предоставляется по принципу «как есть».',
                  '',
                  'Администрация не несёт ответственности за:',
                ]),
                _bulletList([
                  'Качество выполненных работ',
                  'Финансовые потери',
                  'Упущенную выгоду',
                ]),

                _section('9. Персональные данные', [
                  'Используя Платформу, пользователь соглашается с Политикой конфиденциальности.',
                ]),

                _section('10. Блокировка аккаунта', [
                  'Администрация вправе заблокировать аккаунт в случае:',
                ]),
                _bulletList([
                  'Мошенничества',
                  'Предоставления ложных данных',
                  'Нарушения настоящих Условий',
                ]),

                _section('11. Изменения условий', [
                  'Администрация вправе изменять условия использования. Продолжение использования Платформы означает согласие с изменениями.',
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
