import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/models/order.dart';
import 'package:freelance_kg/widgets/order_card.dart';

// --- Order search providers ---
final _orderSearchQueryProvider = StateProvider<String>((ref) => '');

final _orderSearchResultsProvider =
    FutureProvider<PageResponse<OrderItem>>((ref) async {
  final query = ref.watch(_orderSearchQueryProvider);
  if (query.isEmpty) {
    return PageResponse<OrderItem>(
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      first: true,
      last: true,
    );
  }
  final response = await ApiClient()
      .dio
      .get('/orders', queryParameters: {'search': query, 'page': 0, 'size': 20});
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

// --- Executor search providers ---
final _executorSearchQueryProvider = StateProvider<String>((ref) => '');

final _executorSearchResultsProvider =
    FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final query = ref.watch(_executorSearchQueryProvider);
  final params = <String, dynamic>{'page': 0, 'size': 20};
  if (query.isNotEmpty) params['search'] = query;
  final response =
      await ApiClient().dio.get('/executors', queryParameters: params);
  final data = response.data;
  return (data['content'] as List).cast<Map<String, dynamic>>();
});

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  final _orderController = TextEditingController();
  final _executorController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _orderController.dispose();
    _executorController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Поиск'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'Задания'),
            Tab(text: 'Исполнители'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _OrdersSearchTab(controller: _orderController),
          _ExecutorsSearchTab(controller: _executorController),
        ],
      ),
    );
  }
}

// --- Orders search tab ---
class _OrdersSearchTab extends ConsumerWidget {
  final TextEditingController controller;
  const _OrdersSearchTab({required this.controller});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final results = ref.watch(_orderSearchResultsProvider);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            controller: controller,
            onSubmitted: (val) {
              ref.read(_orderSearchQueryProvider.notifier).state = val.trim();
            },
            decoration: InputDecoration(
              hintText: 'Поиск заданий...',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(
                icon: const Icon(Icons.clear),
                onPressed: () {
                  controller.clear();
                  ref.read(_orderSearchQueryProvider.notifier).state = '';
                },
              ),
            ),
          ),
        ),
        Expanded(
          child: results.when(
            data: (page) {
              if (ref.read(_orderSearchQueryProvider).isEmpty) {
                return const Center(
                  child: Text('Введите запрос для поиска',
                      style: TextStyle(color: AppTheme.textMuted)),
                );
              }
              if (page.content.isEmpty) {
                return const Center(child: Text('Ничего не найдено'));
              }
              return ListView.separated(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: page.content.length,
                separatorBuilder: (_, __) => const SizedBox(height: 8),
                itemBuilder: (_, i) => OrderCard(
                  order: page.content[i],
                  onTap: () => context.push('/orders/${page.content[i].id}'),
                ),
              );
            },
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (_, __) => const Center(child: Text('Ошибка поиска')),
          ),
        ),
      ],
    );
  }
}

// --- Executors search tab ---
class _ExecutorsSearchTab extends ConsumerWidget {
  final TextEditingController controller;
  const _ExecutorsSearchTab({required this.controller});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final results = ref.watch(_executorSearchResultsProvider);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            controller: controller,
            onSubmitted: (val) {
              ref.read(_executorSearchQueryProvider.notifier).state =
                  val.trim();
            },
            decoration: InputDecoration(
              hintText: 'Поиск исполнителей...',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(
                icon: const Icon(Icons.clear),
                onPressed: () {
                  controller.clear();
                  ref.read(_executorSearchQueryProvider.notifier).state = '';
                },
              ),
            ),
          ),
        ),
        Expanded(
          child: results.when(
            data: (executors) {
              if (executors.isEmpty) {
                return const Center(
                  child: Text('Исполнители не найдены',
                      style: TextStyle(color: AppTheme.textMuted)),
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: executors.length,
                separatorBuilder: (_, __) => const SizedBox(height: 8),
                itemBuilder: (_, i) =>
                    _ExecutorCard(executor: executors[i]),
              );
            },
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (_, __) => const Center(child: Text('Ошибка поиска')),
          ),
        ),
      ],
    );
  }
}

class _ExecutorCard extends StatelessWidget {
  final Map<String, dynamic> executor;
  const _ExecutorCard({required this.executor});

  @override
  Widget build(BuildContext context) {
    final name = executor['fullName'] as String? ?? '';
    final specialization = executor['specialization'] as String? ?? '';
    final rating = (executor['rating'] as num?)?.toDouble() ?? 0.0;
    final reviewCount = executor['reviewCount'] as int? ?? 0;
    final completedOrders = executor['completedOrders'] as int? ?? 0;
    final available = executor['availableForWork'] as bool? ?? false;
    final avatarUrl = executor['avatarUrl'] as String?;
    final categories = (executor['categories'] as List?)
            ?.map((c) => c['name'] as String)
            .toList() ??
        [];

    return Card(
      child: InkWell(
        onTap: () => context.push('/executors/${executor['id']}'),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              CircleAvatar(
                radius: 24,
                backgroundImage:
                    avatarUrl != null ? NetworkImage(avatarUrl) : null,
                child: avatarUrl == null
                    ? Text(
                        name.isNotEmpty ? name[0].toUpperCase() : '?',
                        style: const TextStyle(fontSize: 18),
                      )
                    : null,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(name,
                              style: const TextStyle(
                                  fontWeight: FontWeight.w600, fontSize: 15),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ),
                        if (available)
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 2),
                            decoration: BoxDecoration(
                              color: AppTheme.success.withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: const Text('Доступен',
                                style: TextStyle(
                                    fontSize: 10,
                                    color: AppTheme.success,
                                    fontWeight: FontWeight.w600)),
                          ),
                      ],
                    ),
                    if (specialization.isNotEmpty)
                      Text(specialization,
                          style: const TextStyle(
                              fontSize: 12, color: AppTheme.textSecondary),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        const Icon(Icons.star, size: 14, color: Colors.amber),
                        const SizedBox(width: 2),
                        Text(rating.toStringAsFixed(1),
                            style: const TextStyle(
                                fontSize: 12, fontWeight: FontWeight.w600)),
                        Text(' ($reviewCount)',
                            style: const TextStyle(
                                fontSize: 11, color: AppTheme.textMuted)),
                        const SizedBox(width: 12),
                        const Icon(Icons.check_circle,
                            size: 14, color: AppTheme.success),
                        const SizedBox(width: 2),
                        Text('$completedOrders вып.',
                            style: const TextStyle(
                                fontSize: 12, color: AppTheme.textSecondary)),
                      ],
                    ),
                    if (categories.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Wrap(
                        spacing: 4,
                        runSpacing: 4,
                        children: categories
                            .take(3)
                            .map((c) => Container(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: AppTheme.primary
                                        .withValues(alpha: 0.08),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(c,
                                      style: const TextStyle(
                                          fontSize: 10,
                                          color: AppTheme.primary)),
                                ))
                            .toList(),
                      ),
                    ],
                  ],
                ),
              ),
              const Icon(Icons.chevron_right,
                  size: 20, color: AppTheme.textMuted),
            ],
          ),
        ),
      ),
    );
  }
}
