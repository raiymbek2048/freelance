import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/push_notification_provider.dart';
import 'package:freelance_kg/screens/auth/login_screen.dart';
import 'package:freelance_kg/screens/auth/register_screen.dart';
import 'package:freelance_kg/screens/chat/chat_screen.dart';
import 'package:freelance_kg/screens/chat/chats_list_screen.dart';
import 'package:freelance_kg/screens/create_order/create_order_screen.dart';
import 'package:freelance_kg/screens/home/home_screen.dart';
import 'package:freelance_kg/screens/notifications/notifications_screen.dart';
import 'package:freelance_kg/screens/order_detail/order_detail_screen.dart';
import 'package:freelance_kg/screens/profile/executor_profile_screen.dart';
import 'package:freelance_kg/screens/profile/profile_screen.dart';
import 'package:freelance_kg/screens/profile/verification_screen.dart';
import 'package:freelance_kg/screens/profile/my_orders_screen.dart';
import 'package:freelance_kg/screens/profile/my_responses_screen.dart';
import 'package:freelance_kg/screens/profile/privacy_screen.dart';
import 'package:freelance_kg/screens/profile/terms_screen.dart';
import 'package:freelance_kg/screens/search/search_screen.dart';
import 'package:freelance_kg/screens/shell/main_shell.dart';

final _router = GoRouter(
  initialLocation: '/',
  routes: [
    // Auth routes (no bottom nav)
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/register',
      builder: (context, state) => const RegisterScreen(),
    ),
    // Notifications (no bottom nav)
    GoRoute(
      path: '/notifications',
      builder: (context, state) => const NotificationsScreen(),
    ),
    // Executor profile (no bottom nav)
    GoRoute(
      path: '/executors/:id',
      builder: (context, state) => ExecutorProfileScreen(
        executorId: int.parse(state.pathParameters['id'] ?? '0'),
      ),
    ),
    // Order detail (no bottom nav) — for navigation from notifications, etc.
    GoRoute(
      path: '/order/:id',
      builder: (context, state) => OrderDetailScreen(
        orderId: int.parse(state.pathParameters['id'] ?? '0'),
      ),
    ),

    // Main shell with bottom navigation
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) =>
          MainShell(navigationShell: navigationShell),
      branches: [
        // Tab 0: Home
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/',
              builder: (context, state) => const HomeScreen(),
              routes: [
                GoRoute(
                  path: 'orders/:id',
                  builder: (context, state) => OrderDetailScreen(
                    orderId:
                        int.parse(state.pathParameters['id'] ?? '0'),
                  ),
                ),
              ],
            ),
          ],
        ),
        // Tab 1: Search
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/search',
              builder: (context, state) => const SearchScreen(),
            ),
          ],
        ),
        // Tab 2: Create order
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/create-order',
              builder: (context, state) => const CreateOrderScreen(),
            ),
          ],
        ),
        // Tab 3: Chats
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/chats',
              builder: (context, state) => const ChatsListScreen(),
              routes: [
                GoRoute(
                  path: ':id',
                  builder: (context, state) => ChatScreen(
                    chatRoomId:
                        int.parse(state.pathParameters['id'] ?? '0'),
                    partnerName: state.extra as String?,
                  ),
                ),
              ],
            ),
          ],
        ),
        // Tab 4: Profile
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/profile',
              builder: (context, state) => const ProfileScreen(),
              routes: [
                GoRoute(
                  path: 'verification',
                  builder: (context, state) =>
                      const VerificationScreen(),
                ),
                GoRoute(
                  path: 'my-orders',
                  builder: (context, state) =>
                      const MyOrdersScreen(),
                ),
                GoRoute(
                  path: 'my-responses',
                  builder: (context, state) =>
                      const MyResponsesScreen(),
                ),
                GoRoute(
                  path: 'privacy',
                  builder: (context, state) =>
                      const PrivacyScreen(),
                ),
                GoRoute(
                  path: 'terms',
                  builder: (context, state) =>
                      const TermsScreen(),
                ),
              ],
            ),
          ],
        ),
      ],
    ),
  ],
);

class FreelanceApp extends ConsumerStatefulWidget {
  const FreelanceApp({super.key});

  @override
  ConsumerState<FreelanceApp> createState() => _FreelanceAppState();
}

class _FreelanceAppState extends ConsumerState<FreelanceApp> {
  @override
  void initState() {
    super.initState();
    _initPushNotifications();
  }

  Future<void> _initPushNotifications() async {
    try {
      final pushService = ref.read(pushNotificationProvider);
      pushService.onNavigate = (route) {
        _router.push(route);
      };
      await pushService.initialize();
    } catch (e) {
      debugPrint('Push notifications init failed: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Freelance KG',
      theme: AppTheme.theme,
      routerConfig: _router,
      debugShowCheckedModeBanner: false,
    );
  }
}
