import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:intl/intl.dart';

final _myResponsesProvider =
    FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final response = await ApiClient()
      .dio
      .get('/orders/my/responses', queryParameters: {'page': 0, 'size': 50});
  final data = response.data;
  return (data['content'] as List).cast<Map<String, dynamic>>();
});

class MyResponsesScreen extends ConsumerWidget {
  const MyResponsesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final responsesAsync = ref.watch(_myResponsesProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Мои отклики')),
      body: responsesAsync.when(
        data: (responses) {
          if (responses.isEmpty) {
            return const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.send_outlined,
                      size: 64, color: AppTheme.textMuted),
                  SizedBox(height: 16),
                  Text('У вас нет откликов',
                      style: TextStyle(
                          color: AppTheme.textSecondary, fontSize: 16)),
                  SizedBox(height: 4),
                  Text('Откликнитесь на задание, чтобы предложить услуги',
                      style: TextStyle(
                          color: AppTheme.textMuted, fontSize: 13)),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(_myResponsesProvider),
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: responses.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final resp = responses[index];
                return _ResponseCard(
                  response: resp,
                  onTap: () {
                    final orderId = resp['orderId'];
                    if (orderId != null) {
                      context.push('/orders/$orderId');
                    }
                  },
                );
              },
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Ошибка загрузки'),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: () => ref.invalidate(_myResponsesProvider),
                child: const Text('Повторить'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ResponseCard extends StatelessWidget {
  final Map<String, dynamic> response;
  final VoidCallback? onTap;

  const _ResponseCard({required this.response, this.onTap});

  @override
  Widget build(BuildContext context) {
    final isSelected = response['isSelected'] == true;
    final orderTitle = response['orderTitle'] as String? ?? '';
    final orderStatus = response['orderStatus'] as String?;
    final coverLetter = response['coverLetter'] ?? '';
    final createdAt = response['createdAt'] ?? '';

    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Order title + status badge
              Row(
                children: [
                  Expanded(
                    child: Text(
                      orderTitle.isNotEmpty ? orderTitle : 'Задание #${response['orderId']}',
                      style: const TextStyle(
                          fontSize: 15, fontWeight: FontWeight.w600),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 8),
                  _statusBadge(orderStatus, isSelected),
                ],
              ),
              if (coverLetter.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  coverLetter,
                  style: const TextStyle(
                      fontSize: 13, color: AppTheme.textSecondary),
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 8),
              Row(
                children: [
                  const Icon(Icons.arrow_forward,
                      size: 14, color: AppTheme.primary),
                  const SizedBox(width: 4),
                  const Text('Перейти к заданию',
                      style:
                          TextStyle(fontSize: 12, color: AppTheme.primary)),
                  const Spacer(),
                  if (createdAt.isNotEmpty)
                    Text(
                      _formatDate(createdAt),
                      style: const TextStyle(
                          fontSize: 11, color: AppTheme.textMuted),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _statusBadge(String? orderStatus, bool isSelected) {
    final (label, color) = switch (orderStatus) {
      'NEW' => isSelected ? ('Выбран', AppTheme.success) : ('На рассмотрении', Colors.orange),
      'IN_PROGRESS' => ('В работе', Colors.blue),
      'REVISION' => ('На доработке', Colors.orange),
      'ON_REVIEW' => ('На проверке', Colors.orange),
      'COMPLETED' => ('Завершён', AppTheme.textMuted),
      'CANCELLED' => ('Отменён', AppTheme.error),
      'DISPUTED' => ('Спор', Colors.red),
      _ => isSelected ? ('Выбран', AppTheme.success) : ('На рассмотрении', Colors.orange),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(label,
          style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: color)),
    );
  }

  String _formatDate(String iso) {
    try {
      final date = DateTime.parse(iso);
      return DateFormat('dd.MM.yyyy').format(date);
    } catch (_) {
      return '';
    }
  }
}
