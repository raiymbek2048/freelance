import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/storage/secure_storage.dart';
import 'package:freelance_kg/models/user.dart';

class AuthState {
  final User? user;
  final bool isLoading;
  final bool isAuthenticated;
  final String? error;

  AuthState({
    this.user,
    this.isLoading = false,
    this.isAuthenticated = false,
    this.error,
  });

  AuthState copyWith({
    User? user,
    bool? isLoading,
    bool? isAuthenticated,
    String? error,
  }) =>
      AuthState(
        user: user ?? this.user,
        isLoading: isLoading ?? this.isLoading,
        isAuthenticated: isAuthenticated ?? this.isAuthenticated,
        error: error,
      );
}

class AuthNotifier extends StateNotifier<AuthState> {
  final _api = ApiClient().dio;

  AuthNotifier() : super(AuthState()) {
    _tryAutoLogin();
  }

  Future<void> _tryAutoLogin() async {
    state = state.copyWith(isLoading: true);
    final token = await SecureStorage.getAccessToken();
    if (token != null) {
      try {
        final response = await _api.get('/users/me');
        state = AuthState(
          user: User.fromJson(response.data),
          isAuthenticated: true,
        );
      } catch (_) {
        await SecureStorage.clearTokens();
        state = AuthState();
      }
    } else {
      state = AuthState();
    }
  }

  Future<void> login(String email, String password) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final response = await _api.post('/auth/login', data: {
        'email': email,
        'password': password,
      });
      await SecureStorage.saveTokens(
        accessToken: response.data['accessToken'],
        refreshToken: response.data['refreshToken'],
      );
      final userResponse = await _api.get('/users/me');
      state = AuthState(
        user: User.fromJson(userResponse.data),
        isAuthenticated: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: 'Неверный email или пароль',
      );
    }
  }

  Future<void> register(String fullName, String email, String password) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final response = await _api.post('/auth/register', data: {
        'fullName': fullName,
        'email': email,
        'password': password,
      });
      await SecureStorage.saveTokens(
        accessToken: response.data['accessToken'],
        refreshToken: response.data['refreshToken'],
      );
      final userResponse = await _api.get('/users/me');
      state = AuthState(
        user: User.fromJson(userResponse.data),
        isAuthenticated: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: 'Ошибка регистрации. Попробуйте другой email.',
      );
    }
  }

  Future<void> loginWithGoogle() async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final googleUser = await GoogleSignIn(
        clientId:
            '846005102702-nflbb4tp8jpruort2lv5ms1ntraigu1i.apps.googleusercontent.com',
        serverClientId:
            '846005102702-hnj41dcqe0v9ljjl9btj7fpnjffs8a43.apps.googleusercontent.com',
      ).signIn();
      if (googleUser == null) {
        state = state.copyWith(isLoading: false);
        return;
      }
      final googleAuth = await googleUser.authentication;
      final idToken = googleAuth.idToken;
      if (idToken == null) {
        state = state.copyWith(
            isLoading: false, error: 'Не удалось получить токен Google');
        return;
      }
      final response = await _api.post(
        '/auth/oauth2/google/token',
        data: {'idToken': idToken},
      );
      await SecureStorage.saveTokens(
        accessToken: response.data['accessToken'],
        refreshToken: response.data['refreshToken'],
      );
      final userResponse = await _api.get('/users/me');
      state = AuthState(
        user: User.fromJson(userResponse.data),
        isAuthenticated: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: 'Ошибка входа через Google',
      );
    }
  }

  Future<void> logout() async {
    try {
      await GoogleSignIn().signOut();
    } catch (_) {}
    await SecureStorage.clearTokens();
    state = AuthState();
  }

  Future<void> refreshUser() async {
    try {
      final response = await _api.get('/users/me');
      state = state.copyWith(user: User.fromJson(response.data));
    } catch (_) {}
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>(
  (ref) => AuthNotifier(),
);
