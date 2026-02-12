import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/models/order.dart';

class OrdersFilter {
  final int? categoryId;
  final String? city;
  final String? search;

  const OrdersFilter({this.categoryId, this.city, this.search});

  OrdersFilter copyWith({
    int? Function()? categoryId,
    String? Function()? city,
    String? Function()? search,
  }) =>
      OrdersFilter(
        categoryId: categoryId != null ? categoryId() : this.categoryId,
        city: city != null ? city() : this.city,
        search: search != null ? search() : this.search,
      );
}

final ordersFilterProvider =
    StateProvider<OrdersFilter>((ref) => const OrdersFilter());

final ordersPageProvider = StateProvider<int>((ref) => 0);

final ordersProvider =
    FutureProvider<PageResponse<OrderItem>>((ref) async {
  final filter = ref.watch(ordersFilterProvider);
  final page = ref.watch(ordersPageProvider);

  final params = <String, dynamic>{
    'page': page,
    'size': 10,
  };
  if (filter.categoryId != null) params['categoryId'] = filter.categoryId;
  if (filter.city != null && filter.city != 'Все города') {
    params['location'] = filter.city;
  }
  if (filter.search != null && filter.search!.isNotEmpty) {
    params['search'] = filter.search;
  }

  final response =
      await ApiClient().dio.get('/orders', queryParameters: params);
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
