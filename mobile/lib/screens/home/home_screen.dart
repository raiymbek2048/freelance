import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/constants.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/auth_provider.dart';
import 'package:freelance_kg/providers/categories_provider.dart';
import 'package:freelance_kg/providers/notifications_provider.dart';
import 'package:freelance_kg/providers/orders_provider.dart';
import 'package:freelance_kg/widgets/order_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      // Load next page if not on last page
      final ordersAsync = ref.read(ordersProvider);
      ordersAsync.whenData((page) {
        if (!page.last) {
          final currentPage = ref.read(ordersPageProvider);
          if (currentPage == page.number) {
            ref.read(ordersPageProvider.notifier).state = currentPage + 1;
          }
        }
      });
    }
  }

  void _doSearch() {
    final query = _searchController.text.trim();
    ref.read(ordersFilterProvider.notifier).state =
        ref.read(ordersFilterProvider).copyWith(
              search: () => query.isEmpty ? null : query,
            );
    ref.read(ordersPageProvider.notifier).state = 0;
    ref.read(_accumulatedOrdersProvider.notifier).state = [];
  }

  @override
  Widget build(BuildContext context) {
    final filter = ref.watch(ordersFilterProvider);
    final categories = ref.watch(categoriesProvider);
    final ordersAsync = ref.watch(ordersProvider);
    final auth = ref.watch(authProvider);
    final unread = ref.watch(unreadCountProvider);

    // Accumulate orders across pages
    ref.listen(ordersProvider, (prev, next) {
      next.whenData((page) {
        final accumulated = ref.read(_accumulatedOrdersProvider);
        if (page.number == 0) {
          ref.read(_accumulatedOrdersProvider.notifier).state = page.content;
        } else {
          // Append new items, avoiding duplicates
          final existingIds = accumulated.map((o) => o.id).toSet();
          final newItems =
              page.content.where((o) => !existingIds.contains(o.id)).toList();
          ref.read(_accumulatedOrdersProvider.notifier).state = [
            ...accumulated,
            ...newItems,
          ];
        }
      });
    });

    final allOrders = ref.watch(_accumulatedOrdersProvider);

    return Scaffold(
      body: Stack(
        children: [
          // Background image
          Positioned.fill(
            child: ImageFiltered(
              imageFilter: ImageFilter.blur(sigmaX: 8, sigmaY: 8),
              child: Image.asset(
                'assets/bishkek-bg.png',
                fit: BoxFit.cover,
              ),
            ),
          ),
          Positioned.fill(
            child: Container(color: Colors.black.withValues(alpha: 0.5)),
          ),
          // Content
          SafeArea(
            child: Column(
              children: [
                // App bar
                Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  child: Row(
                    children: [
                      const Text(
                        'FREELANCE KG',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const Spacer(),
                      Stack(
                        children: [
                          IconButton(
                            icon: const Icon(Icons.notifications_outlined,
                                color: Colors.white),
                            onPressed: () {
                              if (auth.isAuthenticated) {
                                context.push('/notifications');
                              } else {
                                context.push('/login');
                              }
                            },
                          ),
                          if (auth.isAuthenticated && unread > 0)
                            Positioned(
                              right: 6,
                              top: 6,
                              child: Container(
                                padding: const EdgeInsets.all(4),
                                decoration: const BoxDecoration(
                                  color: Colors.red,
                                  shape: BoxShape.circle,
                                ),
                                constraints: const BoxConstraints(
                                    minWidth: 18, minHeight: 18),
                                child: Text(
                                  unread > 99 ? '99+' : '$unread',
                                  style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 10,
                                      fontWeight: FontWeight.bold),
                                  textAlign: TextAlign.center,
                                ),
                              ),
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
                // Scrollable content
                Expanded(
                  child: Container(
                    margin: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFC8DCF0).withValues(alpha: 0.5),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: BackdropFilter(
                        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                        child: Column(
                          children: [
                            Padding(
                              padding: const EdgeInsets.fromLTRB(
                                  16, 16, 16, 0),
                              child: Column(
                                children: [
                                  // Search row
                                  Row(
                                    children: [
                                      Expanded(
                                        child: SizedBox(
                                          height: 40,
                                          child: TextField(
                                            controller: _searchController,
                                            onSubmitted: (_) => _doSearch(),
                                            style:
                                                const TextStyle(fontSize: 14),
                                            decoration: InputDecoration(
                                              hintText: 'Поиск...',
                                              prefixIcon: const Icon(
                                                  Icons.search,
                                                  size: 20),
                                              contentPadding:
                                                  const EdgeInsets.symmetric(
                                                      vertical: 0,
                                                      horizontal: 12),
                                              border: OutlineInputBorder(
                                                borderRadius:
                                                    BorderRadius.circular(10),
                                                borderSide: BorderSide.none,
                                              ),
                                              filled: true,
                                              fillColor: Colors.white,
                                            ),
                                          ),
                                        ),
                                      ),
                                      const SizedBox(width: 8),
                                      SizedBox(
                                        height: 40,
                                        child: ElevatedButton(
                                          onPressed: _doSearch,
                                          style: ElevatedButton.styleFrom(
                                            padding: const EdgeInsets.symmetric(
                                                horizontal: 16),
                                          ),
                                          child: const Text('Найти',
                                              style: TextStyle(fontSize: 13)),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 12),
                                  // Filters row
                                  Row(
                                    children: [
                                      // Category dropdown
                                      Expanded(
                                        child: Container(
                                          height: 40,
                                          padding: const EdgeInsets.symmetric(
                                              horizontal: 12),
                                          decoration: BoxDecoration(
                                            color: Colors.white,
                                            borderRadius:
                                                BorderRadius.circular(10),
                                            border: Border.all(
                                                color: AppTheme.border),
                                          ),
                                          child: categories.when(
                                            data: (cats) =>
                                                DropdownButtonHideUnderline(
                                              child:
                                                  DropdownButton<int?>(
                                                value: filter.categoryId,
                                                isExpanded: true,
                                                icon: const Icon(
                                                    Icons
                                                        .keyboard_arrow_down,
                                                    size: 20),
                                                style: const TextStyle(
                                                    fontSize: 13,
                                                    color: AppTheme
                                                        .textPrimary),
                                                items: [
                                                  const DropdownMenuItem(
                                                    value: null,
                                                    child: Text(
                                                        'Все категории'),
                                                  ),
                                                  ...cats.map((c) =>
                                                      DropdownMenuItem(
                                                        value: c.id,
                                                        child: Text(
                                                            c.name,
                                                            overflow:
                                                                TextOverflow
                                                                    .ellipsis),
                                                      )),
                                                ],
                                                onChanged: (val) {
                                                  ref
                                                      .read(ordersFilterProvider
                                                          .notifier)
                                                      .state =
                                                      filter.copyWith(
                                                          categoryId: () =>
                                                              val);
                                                  ref
                                                      .read(ordersPageProvider
                                                          .notifier)
                                                      .state = 0;
                                                  ref
                                                      .read(
                                                          _accumulatedOrdersProvider
                                                              .notifier)
                                                      .state = [];
                                                },
                                              ),
                                            ),
                                            loading: () => const Center(
                                                child:
                                                    SizedBox(
                                                        width: 16,
                                                        height: 16,
                                                        child:
                                                            CircularProgressIndicator(
                                                                strokeWidth:
                                                                    2))),
                                            error: (_, __) =>
                                                const Text('Ошибка'),
                                          ),
                                        ),
                                      ),
                                      const SizedBox(width: 10),
                                      // City dropdown
                                      Expanded(
                                        child: Container(
                                          height: 40,
                                          padding: const EdgeInsets.symmetric(
                                              horizontal: 12),
                                          decoration: BoxDecoration(
                                            color: Colors.white,
                                            borderRadius:
                                                BorderRadius.circular(10),
                                            border: Border.all(
                                                color: AppTheme.border),
                                          ),
                                          child:
                                              DropdownButtonHideUnderline(
                                            child:
                                                DropdownButton<String>(
                                              value: filter.city ??
                                                  'Все города',
                                              isExpanded: true,
                                              icon: const Icon(
                                                  Icons
                                                      .keyboard_arrow_down,
                                                  size: 20),
                                              style: const TextStyle(
                                                  fontSize: 13,
                                                  color: AppTheme
                                                      .textPrimary),
                                              items: AppConstants.cities
                                                  .map((city) =>
                                                      DropdownMenuItem(
                                                        value: city,
                                                        child: Text(city,
                                                            overflow:
                                                                TextOverflow
                                                                    .ellipsis),
                                                      ))
                                                  .toList(),
                                              onChanged: (val) {
                                                ref
                                                    .read(ordersFilterProvider
                                                        .notifier)
                                                    .state =
                                                    filter.copyWith(
                                                        city: () => val);
                                                ref
                                                    .read(ordersPageProvider
                                                        .notifier)
                                                    .state = 0;
                                                ref
                                                    .read(
                                                        _accumulatedOrdersProvider
                                                            .notifier)
                                                    .state = [];
                                              },
                                            ),
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 12),
                                ],
                              ),
                            ),
                            // Orders list with infinite scroll
                            Expanded(
                              child: _buildOrdersList(
                                  ordersAsync, allOrders),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOrdersList(
    AsyncValue ordersAsync,
    List allOrders,
  ) {
    // Show initial loading only when no accumulated data
    if (allOrders.isEmpty) {
      return ordersAsync.when(
        data: (page) {
          if (page.content.isEmpty) {
            return const Center(
              child: Text('Задания не найдены',
                  style: TextStyle(color: AppTheme.textSecondary)),
            );
          }
          // Data will be populated via listener, this shouldn't normally show
          return const Center(child: CircularProgressIndicator());
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Ошибка загрузки'),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: () {
                  ref.read(_accumulatedOrdersProvider.notifier).state = [];
                  ref.read(ordersPageProvider.notifier).state = 0;
                  ref.invalidate(ordersProvider);
                },
                child: const Text('Повторить'),
              ),
            ],
          ),
        ),
      );
    }

    final isLastPage = ordersAsync.whenOrNull(data: (page) => page.last) as bool? ?? false;

    return RefreshIndicator(
      onRefresh: () async {
        ref.read(_accumulatedOrdersProvider.notifier).state = [];
        ref.read(ordersPageProvider.notifier).state = 0;
        ref.invalidate(ordersProvider);
      },
      child: ListView.separated(
        controller: _scrollController,
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        itemCount: allOrders.length + (isLastPage ? 0 : 1),
        separatorBuilder: (_, __) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          if (index >= allOrders.length) {
            // Loading indicator at the bottom
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Center(
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            );
          }
          return OrderCard(
            order: allOrders[index],
            onTap: () =>
                context.push('/orders/${allOrders[index].id}'),
          );
        },
      ),
    );
  }
}

// Accumulated orders across all loaded pages
final _accumulatedOrdersProvider = StateProvider<List>((ref) => []);
