import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/models/category.dart';

final categoriesProvider = FutureProvider<List<Category>>((ref) async {
  final response = await ApiClient().dio.get('/categories');
  return (response.data as List)
      .map((json) => Category.fromJson(json))
      .toList();
});
