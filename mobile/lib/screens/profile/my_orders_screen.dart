import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/models/order.dart';


final _myOrdersProvider =
    FutureProvider<PageResponse<OrderItem>>((ref) async {
  final response = await ApiClient()
      .dio
      .get('/orders/my/as-client', queryParameters: {'page': 0, 'size': 50});
  final data = response.data;
  return PageResponse<OrderItem>(
    content: (data['content'] as List)
        .map((json) => OrderItem.fromJson(json))
        .toList(),
    totalElements: data['totalElements'] ?? 0,
    totalPages: data['totalPages'] ?? 0,
    number: data['number'] ?? 0,
    first: data['first'] ?? true,
    last: data['last'] ?? true,
  );
});

class MyOrdersScreen extends ConsumerWidget {
  const MyOrdersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordersAsync = ref.watch(_myOrdersProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Мои объявления')),
      body: ordersAsync.when(
        data: (page) {
          if (page.content.isEmpty) {
            return const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.assignment_outlined,
                      size: 64, color: AppTheme.textMuted),
                  SizedBox(height: 16),
                  Text('У вас нет объявлений',
                      style: TextStyle(
                          color: AppTheme.textSecondary, fontSize: 16)),
                  SizedBox(height: 4),
                  Text('Создайте задание, чтобы найти исполнителя',
                      style: TextStyle(
                          color: AppTheme.textMuted, fontSize: 13)),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(_myOrdersProvider),
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: page.content.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final order = page.content[index];
                return _MyOrderCard(
                  order: order,
                  onTap: () => context.push('/orders/${order.id}'),
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
                onPressed: () => ref.invalidate(_myOrdersProvider),
                child: const Text('Повторить'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MyOrderCard extends StatelessWidget {
  final OrderItem order;
  final VoidCallback? onTap;

  const _MyOrderCard({required this.order, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      order.title,
                      style: const TextStyle(
                          fontSize: 16, fontWeight: FontWeight.bold),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  _statusBadge(order.status),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                order.description,
                style: const TextStyle(
                    fontSize: 13, color: AppTheme.textSecondary),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  if (order.budgetMax != null) ...[
                    Text(
                      '${order.budgetMax!.toInt()} сом',
                      style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: AppTheme.primary),
                    ),
                    const SizedBox(width: 16),
                  ],
                  const Icon(Icons.people_outline,
                      size: 14, color: AppTheme.textMuted),
                  const SizedBox(width: 4),
                  Text(
                    '${order.responseCount} откликов',
                    style: const TextStyle(
                        fontSize: 12, color: AppTheme.textMuted),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _statusBadge(String? status) {
    final (label, color) = switch (status) {
      'NEW' => ('Открыт', AppTheme.success),
      'IN_PROGRESS' => ('В работе', Colors.blue),
      'REVISION' => ('На доработке', Colors.orange),
      'ON_REVIEW' => ('На проверке', Colors.orange),
      'COMPLETED' => ('Завершён', AppTheme.textMuted),
      'CANCELLED' => ('Отменён', AppTheme.error),
      'DISPUTED' => ('Спор', Colors.red),
      _ => ('Открыт', AppTheme.success),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(label,
          style: TextStyle(
              fontSize: 11, fontWeight: FontWeight.w600, color: color)),
    );
  }
}
