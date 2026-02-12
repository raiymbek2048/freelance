import 'package:dio/dio.dart';
import 'package:freelance_kg/core/constants.dart';
import 'package:freelance_kg/core/storage/secure_storage.dart';

class ApiClient {
  static final ApiClient _instance = ApiClient._internal();
  factory ApiClient() => _instance;

  late final Dio dio;

  ApiClient._internal() {
    dio = Dio(BaseOptions(
      baseUrl: AppConstants.apiBaseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 15),
      headers: {'Content-Type': 'application/json'},
    ));

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await SecureStorage.getAccessToken();
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onError: (error, handler) async {
        if (error.response?.statusCode == 401) {
          final refreshed = await _refreshToken();
          if (refreshed) {
            // Retry the failed request
            final opts = error.requestOptions;
            final token = await SecureStorage.getAccessToken();
            opts.headers['Authorization'] = 'Bearer $token';
            final response = await dio.fetch(opts);
            return handler.resolve(response);
          }
        }
        handler.next(error);
      },
    ));
  }

  Future<bool> _refreshToken() async {
    try {
      final refreshToken = await SecureStorage.getRefreshToken();
      if (refreshToken == null) return false;

      final response = await Dio(BaseOptions(
        baseUrl: AppConstants.apiBaseUrl,
      )).post('/auth/refresh', data: {'refreshToken': refreshToken});

      final data = response.data;
      await SecureStorage.saveTokens(
        accessToken: data['accessToken'],
        refreshToken: data['refreshToken'],
      );
      return true;
    } catch (_) {
      await SecureStorage.clearTokens();
      return false;
    }
  }
}
