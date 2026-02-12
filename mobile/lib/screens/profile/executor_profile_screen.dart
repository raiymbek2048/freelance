import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:intl/intl.dart';

final _executorProvider =
    FutureProvider.family<Map<String, dynamic>, int>((ref, id) async {
  final response = await ApiClient().dio.get('/executors/$id');
  return response.data as Map<String, dynamic>;
});

class ExecutorProfileScreen extends ConsumerWidget {
  final int executorId;

  const ExecutorProfileScreen({super.key, required this.executorId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final executorAsync = ref.watch(_executorProvider(executorId));

    return Scaffold(
      appBar: AppBar(title: const Text('Профиль исполнителя')),
      body: executorAsync.when(
        data: (executor) => _ExecutorProfile(executor: executor),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Ошибка загрузки профиля'),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: () => ref.invalidate(_executorProvider(executorId)),
                child: const Text('Повторить'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ExecutorProfile extends StatelessWidget {
  final Map<String, dynamic> executor;

  const _ExecutorProfile({required this.executor});

  @override
  Widget build(BuildContext context) {
    final name = executor['fullName'] as String? ?? '';
    final bio = executor['bio'] as String? ?? '';
    final specialization = executor['specialization'] as String? ?? '';
    final avatarUrl = executor['avatarUrl'] as String?;
    final rating = (executor['rating'] as num?)?.toDouble() ?? 0.0;
    final reviewCount = executor['reviewCount'] as int? ?? 0;
    final completedOrders = executor['completedOrders'] as int? ?? 0;
    final totalOrders = executor['totalOrders'] as int? ?? 0;
    final avgDays = (executor['avgCompletionDays'] as num?)?.toDouble();
    final available = executor['availableForWork'] as bool? ?? false;
    final memberSince = executor['memberSince'] as String?;
    final lastActive = executor['lastActiveAt'] as String?;
    final reputationLevel = executor['reputationLevel'] as String? ?? '';
    final reputationColor = executor['reputationColor'] as String? ?? '';
    final categories = (executor['categories'] as List?)
            ?.map((c) => c['name'] as String)
            .toList() ??
        [];
    final whatsappLink = executor['whatsappLink'] as String?;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 40,
                    backgroundImage:
                        avatarUrl != null ? NetworkImage(avatarUrl) : null,
                    child: avatarUrl == null
                        ? Text(name.isNotEmpty ? name[0].toUpperCase() : '?',
                            style: const TextStyle(fontSize: 32))
                        : null,
                  ),
                  const SizedBox(height: 12),
                  Text(name,
                      style: const TextStyle(
                          fontSize: 20, fontWeight: FontWeight.bold)),
                  if (specialization.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(specialization,
                        style: const TextStyle(
                            fontSize: 14, color: AppTheme.textSecondary)),
                  ],
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      if (available)
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: AppTheme.success.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.circle,
                                  size: 8, color: AppTheme.success),
                              SizedBox(width: 6),
                              Text('Доступен',
                                  style: TextStyle(
                                      fontSize: 12,
                                      color: AppTheme.success,
                                      fontWeight: FontWeight.w600)),
                            ],
                          ),
                        ),
                      if (reputationLevel.isNotEmpty) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: _parseColor(reputationColor)
                                .withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(reputationLevel,
                              style: TextStyle(
                                  fontSize: 12,
                                  color: _parseColor(reputationColor),
                                  fontWeight: FontWeight.w600)),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Stats card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  _Stat(
                    icon: Icons.star,
                    iconColor: Colors.amber,
                    value: rating.toStringAsFixed(1),
                    label: '$reviewCount отзывов',
                  ),
                  _divider(),
                  _Stat(
                    icon: Icons.check_circle,
                    iconColor: AppTheme.success,
                    value: '$completedOrders',
                    label: 'выполнено',
                  ),
                  _divider(),
                  _Stat(
                    icon: Icons.assignment,
                    iconColor: AppTheme.primary,
                    value: '$totalOrders',
                    label: 'всего',
                  ),
                  if (avgDays != null) ...[
                    _divider(),
                    _Stat(
                      icon: Icons.schedule,
                      iconColor: Colors.orange,
                      value: '${avgDays.toStringAsFixed(0)}д',
                      label: 'среднее',
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Bio
          if (bio.isNotEmpty)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.person, size: 18, color: AppTheme.textMuted),
                        SizedBox(width: 8),
                        Text('О себе',
                            style: TextStyle(
                                fontSize: 15, fontWeight: FontWeight.w600)),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(bio,
                        style: const TextStyle(
                            fontSize: 14,
                            color: AppTheme.textSecondary,
                            height: 1.5)),
                  ],
                ),
              ),
            ),
          if (bio.isNotEmpty) const SizedBox(height: 12),

          // Categories
          if (categories.isNotEmpty)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.category,
                            size: 18, color: AppTheme.textMuted),
                        SizedBox(width: 8),
                        Text('Категории',
                            style: TextStyle(
                                fontSize: 15, fontWeight: FontWeight.w600)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: categories
                          .map((c) => Chip(
                                label: Text(c, style: const TextStyle(fontSize: 12)),
                                backgroundColor:
                                    AppTheme.primary.withValues(alpha: 0.1),
                                side: BorderSide.none,
                                padding: EdgeInsets.zero,
                                materialTapTargetSize:
                                    MaterialTapTargetSize.shrinkWrap,
                              ))
                          .toList(),
                    ),
                  ],
                ),
              ),
            ),
          if (categories.isNotEmpty) const SizedBox(height: 12),

          // Info card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.info_outline,
                          size: 18, color: AppTheme.textMuted),
                      SizedBox(width: 8),
                      Text('Информация',
                          style: TextStyle(
                              fontSize: 15, fontWeight: FontWeight.w600)),
                    ],
                  ),
                  const SizedBox(height: 12),
                  if (memberSince != null)
                    _InfoRow(
                        icon: Icons.calendar_today,
                        label: 'На платформе с',
                        value: _formatDate(memberSince)),
                  if (lastActive != null)
                    _InfoRow(
                        icon: Icons.access_time,
                        label: 'Последняя активность',
                        value: _formatDate(lastActive)),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Reviews
          _ReviewsCard(executorId: executor['id'] as int),
          const SizedBox(height: 12),

          // WhatsApp button
          if (whatsappLink != null && whatsappLink.isNotEmpty)
            ElevatedButton.icon(
              onPressed: () {
                // Could launch URL here
              },
              icon: const Icon(Icons.chat, size: 18),
              label: const Text('Написать в WhatsApp'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF25D366),
              ),
            ),

          const SizedBox(height: 32),
        ],
      ),
    );
  }

  Widget _divider() => Container(
      width: 1, height: 32, color: AppTheme.border, margin: const EdgeInsets.symmetric(horizontal: 8));

  String _formatDate(String iso) {
    try {
      final date = DateTime.parse(iso);
      return DateFormat('dd.MM.yyyy').format(date);
    } catch (_) {
      return '';
    }
  }

  Color _parseColor(String hex) {
    if (hex.isEmpty) return AppTheme.primary;
    try {
      final clean = hex.replaceAll('#', '');
      return Color(int.parse('FF$clean', radix: 16));
    } catch (_) {
      return AppTheme.primary;
    }
  }
}

class _Stat extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String value;
  final String label;

  const _Stat({
    required this.icon,
    required this.iconColor,
    required this.value,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Icon(icon, size: 20, color: iconColor),
          const SizedBox(height: 4),
          Text(value,
              style:
                  const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          Text(label,
              style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
        ],
      ),
    );
  }
}

final _executorReviewsProvider =
    FutureProvider.family<List<Map<String, dynamic>>, int>((ref, executorId) async {
  final response = await ApiClient()
      .dio
      .get('/reviews/executors/$executorId', queryParameters: {'page': 0, 'size': 10});
  return (response.data['content'] as List).cast<Map<String, dynamic>>();
});

class _ReviewsCard extends ConsumerWidget {
  final int executorId;
  const _ReviewsCard({required this.executorId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final reviewsAsync = ref.watch(_executorReviewsProvider(executorId));

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.star, size: 18, color: Colors.amber),
                SizedBox(width: 8),
                Text('Отзывы',
                    style:
                        TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 12),
            reviewsAsync.when(
              data: (reviews) {
                if (reviews.isEmpty) {
                  return const Text('Пока нет отзывов',
                      style: TextStyle(
                          color: AppTheme.textMuted, fontSize: 13));
                }
                return Column(
                  children: reviews.map((r) {
                    final rating = r['rating'] as int? ?? 0;
                    final comment = r['comment'] as String? ?? '';
                    final clientName = r['clientName'] as String? ?? '';
                    final createdAt = r['createdAt'] as String? ?? '';
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              ...List.generate(
                                  5,
                                  (i) => Icon(
                                        i < rating
                                            ? Icons.star
                                            : Icons.star_border,
                                        size: 16,
                                        color: Colors.amber,
                                      )),
                              const Spacer(),
                              Text(_formatReviewDate(createdAt),
                                  style: const TextStyle(
                                      fontSize: 11,
                                      color: AppTheme.textMuted)),
                            ],
                          ),
                          if (comment.isNotEmpty) ...[
                            const SizedBox(height: 6),
                            Text(comment,
                                style: const TextStyle(
                                    fontSize: 13,
                                    color: AppTheme.textSecondary,
                                    height: 1.4)),
                          ],
                          const SizedBox(height: 4),
                          Text('— $clientName',
                              style: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w500,
                                  color: AppTheme.textMuted)),
                          if (reviews.last != r) const Divider(height: 16),
                        ],
                      ),
                    );
                  }).toList(),
                );
              },
              loading: () => const Center(
                child: SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2)),
              ),
              error: (_, __) => const Text('Ошибка загрузки отзывов',
                  style: TextStyle(color: AppTheme.error, fontSize: 13)),
            ),
          ],
        ),
      ),
    );
  }

  String _formatReviewDate(String iso) {
    try {
      final date = DateTime.parse(iso);
      return DateFormat('dd.MM.yyyy').format(date);
    } catch (_) {
      return '';
    }
  }
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _InfoRow(
      {required this.icon, required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 16, color: AppTheme.textMuted),
          const SizedBox(width: 8),
          Text('$label: ',
              style:
                  const TextStyle(fontSize: 13, color: AppTheme.textSecondary)),
          Text(value,
              style:
                  const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}
