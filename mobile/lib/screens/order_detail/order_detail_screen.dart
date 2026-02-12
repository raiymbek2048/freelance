import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/auth_provider.dart';
import 'package:intl/intl.dart';

final orderDetailProvider =
    FutureProvider.family<Map<String, dynamic>, int>((ref, id) async {
  final response = await ApiClient().dio.get('/orders/$id');
  return response.data;
});

final _orderResponsesProvider =
    FutureProvider.family<List<Map<String, dynamic>>, int>((ref, orderId) async {
  final response = await ApiClient().dio.get('/orders/$orderId/responses');
  return (response.data as List).cast<Map<String, dynamic>>();
});

final _orderReviewProvider =
    FutureProvider.family<Map<String, dynamic>?, int>((ref, orderId) async {
  try {
    final response = await ApiClient().dio.get('/reviews/orders/$orderId');
    return response.data as Map<String, dynamic>;
  } catch (_) {
    return null;
  }
});

class OrderDetailScreen extends ConsumerStatefulWidget {
  final int orderId;
  const OrderDetailScreen({super.key, required this.orderId});

  @override
  ConsumerState<OrderDetailScreen> createState() => _OrderDetailScreenState();
}

class _OrderDetailScreenState extends ConsumerState<OrderDetailScreen> {
  final _responseController = TextEditingController();
  bool _sending = false;

  @override
  void dispose() {
    _responseController.dispose();
    super.dispose();
  }

  Future<void> _respond() async {
    if (_responseController.text.trim().isEmpty) return;
    setState(() => _sending = true);
    try {
      await ApiClient().dio.post(
        '/orders/${widget.orderId}/responses',
        data: {'coverLetter': _responseController.text.trim()},
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Отклик отправлен!')));
        _responseController.clear();
        ref.invalidate(orderDetailProvider(widget.orderId));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка отправки отклика')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _submitForReview() async {
    setState(() => _sending = true);
    try {
      await ApiClient().dio.post('/orders/${widget.orderId}/submit-for-review');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Работа отправлена на проверку!')));
        ref.invalidate(orderDetailProvider(widget.orderId));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка отправки')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _approveWork() async {
    setState(() => _sending = true);
    try {
      await ApiClient().dio.post('/orders/${widget.orderId}/approve');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Работа принята!')));
        ref.invalidate(orderDetailProvider(widget.orderId));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _requestRevision() async {
    setState(() => _sending = true);
    try {
      await ApiClient().dio.post('/orders/${widget.orderId}/request-revision',
          data: {'reason': 'Требуется доработка'});
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Запрос на доработку отправлен')));
        ref.invalidate(orderDetailProvider(widget.orderId));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _selectExecutor(int responseId) async {
    setState(() => _sending = true);
    try {
      await ApiClient().dio.post(
        '/orders/${widget.orderId}/select-executor',
        data: {'responseId': responseId},
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Исполнитель выбран!')));
        ref.invalidate(orderDetailProvider(widget.orderId));
        ref.invalidate(_orderResponsesProvider(widget.orderId));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка выбора исполнителя')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _submitReview(int rating, String comment) async {
    setState(() => _sending = true);
    try {
      await ApiClient().dio.post(
        '/reviews/orders/${widget.orderId}',
        data: {'rating': rating, 'comment': comment},
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Отзыв оставлен!')));
        ref.invalidate(_orderReviewProvider(widget.orderId));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка отправки отзыва')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detailAsync = ref.watch(orderDetailProvider(widget.orderId));
    final auth = ref.watch(authProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Задание')),
      body: detailAsync.when(
        data: (data) {
          final isOwner = data['isOwner'] == true;
          final isExecutor = data['isExecutor'] == true;
          final hasResponded = data['hasResponded'] == true;
          final status = data['status'] as String? ?? 'NEW';

          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Status + owner badges
                Row(
                  children: [
                    if (isOwner)
                      _badge(Icons.edit_note, 'Моё задание', AppTheme.primary),
                    if (isExecutor) ...[
                      if (isOwner) const SizedBox(width: 8),
                      _badge(Icons.engineering, 'Я исполнитель', Colors.blue),
                    ],
                  ],
                ),
                if (isOwner || isExecutor) const SizedBox(height: 10),

                // Status bar
                _statusBar(status),
                const SizedBox(height: 12),

                // Title
                Text(
                  data['title'] ?? '',
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 12),

                // Meta
                Wrap(
                  spacing: 12,
                  runSpacing: 8,
                  children: [
                    if (data['categoryName'] != null)
                      Chip(
                        label: Text(data['categoryName'],
                            style: const TextStyle(fontSize: 12)),
                        backgroundColor:
                            AppTheme.primary.withValues(alpha: 0.1),
                      ),
                    if (data['location'] != null)
                      Chip(
                        avatar: const Icon(Icons.location_on, size: 16),
                        label: Text(data['location'],
                            style: const TextStyle(fontSize: 12)),
                      ),
                    if (data['budgetMax'] != null)
                      Chip(
                        label: Text(
                            '${(data['budgetMax'] as num).toInt()} сом',
                            style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primary)),
                      ),
                  ],
                ),
                if (data['deadline'] != null) ...[
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Icon(Icons.schedule,
                          size: 16, color: Colors.orange[600]),
                      const SizedBox(width: 4),
                      Text(
                        'До ${_formatDate(data['deadline'])}',
                        style: TextStyle(
                            fontSize: 13, color: Colors.orange[600]),
                      ),
                    ],
                  ),
                ],
                const SizedBox(height: 16),
                const Divider(),
                const SizedBox(height: 12),

                // Description
                const Text('Описание',
                    style:
                        TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
                const SizedBox(height: 8),
                Text(
                  data['description'] ?? '',
                  style: const TextStyle(
                      fontSize: 14,
                      color: AppTheme.textSecondary,
                      height: 1.5),
                ),
                const SizedBox(height: 16),

                // Client info
                const Divider(),
                const SizedBox(height: 12),
                Row(
                  children: [
                    CircleAvatar(
                      radius: 20,
                      backgroundColor:
                          AppTheme.primary.withValues(alpha: 0.15),
                      child: const Icon(Icons.person,
                          color: AppTheme.primary, size: 20),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            data['clientName'] ?? '',
                            style: const TextStyle(
                                fontWeight: FontWeight.w600, fontSize: 15),
                          ),
                          const Text('Заказчик',
                              style: TextStyle(
                                  fontSize: 12, color: AppTheme.textMuted)),
                        ],
                      ),
                    ),
                  ],
                ),

                // Executor info (if assigned)
                if (data['executorName'] != null) ...[
                  const SizedBox(height: 12),
                  InkWell(
                    onTap: () {
                      final executorId = data['executorId'];
                      if (executorId != null) {
                        context.push('/executors/$executorId');
                      }
                    },
                    borderRadius: BorderRadius.circular(8),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 20,
                          backgroundColor: Colors.blue.withValues(alpha: 0.15),
                          child: const Icon(Icons.engineering,
                              color: Colors.blue, size: 20),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                data['executorName'],
                                style: const TextStyle(
                                    fontWeight: FontWeight.w600, fontSize: 15),
                              ),
                              const Text('Исполнитель',
                                  style: TextStyle(
                                      fontSize: 12, color: AppTheme.textMuted)),
                            ],
                          ),
                        ),
                        const Icon(Icons.chevron_right,
                            color: AppTheme.textMuted, size: 20),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 20),

                // Responses list for owner
                if (isOwner && status == 'NEW')
                  _ResponsesSection(
                    orderId: widget.orderId,
                    onSelect: _selectExecutor,
                    sending: _sending,
                  ),

                // Review section (for completed orders)
                if (status == 'COMPLETED' && isOwner)
                  _ReviewSection(
                    orderId: widget.orderId,
                    onSubmit: _submitReview,
                    sending: _sending,
                  ),

                // Action section
                _buildActionSection(
                  isOwner: isOwner,
                  isExecutor: isExecutor,
                  hasResponded: hasResponded,
                  status: status,
                  isAuthenticated: auth.isAuthenticated,
                ),
              ],
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => const Center(child: Text('Ошибка загрузки')),
      ),
    );
  }

  Widget _buildActionSection({
    required bool isOwner,
    required bool isExecutor,
    required bool hasResponded,
    required String status,
    required bool isAuthenticated,
  }) {
    // --- Executor actions ---
    if (isExecutor) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Divider(),
          const SizedBox(height: 12),
          if (status == 'IN_PROGRESS') ...[
            _infoBox(
              Icons.work,
              'Вы выбраны исполнителем. Выполните задание и сдайте работу.',
              Colors.blue,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _sending ? null : _submitForReview,
              icon: _sending
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.upload, size: 18),
              label: Text(_sending ? 'Отправка...' : 'Сдать работу'),
            ),
          ] else if (status == 'REVISION') ...[
            _infoBox(
              Icons.edit_note,
              'Заказчик запросил доработку. Внесите изменения и сдайте работу повторно.',
              Colors.orange,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _sending ? null : _submitForReview,
              icon: _sending
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.upload, size: 18),
              label: Text(_sending ? 'Отправка...' : 'Сдать работу'),
            ),
          ] else if (status == 'ON_REVIEW') ...[
            _infoBox(
              Icons.hourglass_top,
              'Работа отправлена на проверку. Ожидайте решения заказчика.',
              Colors.orange,
            ),
          ] else if (status == 'COMPLETED') ...[
            _infoBox(
              Icons.check_circle,
              'Задание завершено. Работа принята заказчиком.',
              AppTheme.success,
            ),
          ] else if (status == 'DISPUTED') ...[
            _infoBox(
              Icons.gavel,
              'По заданию открыт спор. Ожидайте решения модератора.',
              Colors.red,
            ),
          ],
        ],
      );
    }

    // --- Owner actions ---
    if (isOwner) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Divider(),
          const SizedBox(height: 12),
          if (status == 'ON_REVIEW') ...[
            _infoBox(
              Icons.rate_review,
              'Исполнитель сдал работу. Проверьте результат.',
              Colors.orange,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _sending ? null : _approveWork,
              icon: const Icon(Icons.check, size: 18),
              label: const Text('Принять работу'),
              style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.success),
            ),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: _sending ? null : _requestRevision,
              icon: const Icon(Icons.replay, size: 18),
              label: const Text('Отправить на доработку'),
            ),
          ] else if (status == 'COMPLETED') ...[
            _infoBox(
              Icons.check_circle,
              'Задание завершено.',
              AppTheme.success,
            ),
          ] else if (status == 'IN_PROGRESS') ...[
            _infoBox(
              Icons.work,
              'Задание в работе у исполнителя.',
              Colors.blue,
            ),
          ] else if (status == 'REVISION') ...[
            _infoBox(
              Icons.edit_note,
              'Задание на доработке у исполнителя.',
              Colors.orange,
            ),
          ] else if (status == 'DISPUTED') ...[
            _infoBox(
              Icons.gavel,
              'По заданию открыт спор. Ожидайте решения модератора.',
              Colors.red,
            ),
          ],
        ],
      );
    }

    // --- Regular user actions ---
    if (!isAuthenticated) return const SizedBox.shrink();

    // Can only respond to NEW orders
    if (status != 'NEW') {
      return _infoBox(
        Icons.lock,
        'Это задание больше не принимает отклики.',
        AppTheme.textMuted,
      );
    }

    if (hasResponded) {
      return _infoBox(
        Icons.check_circle,
        'Вы уже откликнулись на это задание.',
        AppTheme.success,
      );
    }

    // Response form
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Divider(),
        const SizedBox(height: 12),
        const Text('Ваш отклик',
            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
        const SizedBox(height: 8),
        TextField(
          controller: _responseController,
          maxLines: 3,
          decoration: const InputDecoration(
              hintText: 'Напишите сообщение заказчику...'),
        ),
        const SizedBox(height: 12),
        ElevatedButton(
          onPressed: _sending ? null : _respond,
          child: _sending
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                      strokeWidth: 2, color: Colors.white),
                )
              : const Text('Откликнуться'),
        ),
      ],
    );
  }

  Widget _badge(IconData icon, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: 4),
          Text(label,
              style: TextStyle(
                  fontSize: 12, fontWeight: FontWeight.w600, color: color)),
        ],
      ),
    );
  }

  Widget _statusBar(String status) {
    final (label, color, icon) = switch (status) {
      'NEW' => ('Открыт', AppTheme.success, Icons.fiber_new),
      'IN_PROGRESS' => ('В работе', Colors.blue, Icons.work),
      'REVISION' => ('На доработке', Colors.orange, Icons.edit_note),
      'ON_REVIEW' => ('На проверке', Colors.orange, Icons.rate_review),
      'COMPLETED' => ('Завершён', AppTheme.textMuted, Icons.check_circle),
      'CANCELLED' => ('Отменён', AppTheme.error, Icons.cancel),
      'DISPUTED' => ('Спор', Colors.red, Icons.gavel),
      _ => ('Открыт', AppTheme.success, Icons.fiber_new),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: 6),
          Text('Статус: $label',
              style: TextStyle(
                  fontSize: 13, fontWeight: FontWeight.w600, color: color)),
        ],
      ),
    );
  }

  Widget _infoBox(IconData icon, String text, Color color) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(text, style: TextStyle(fontSize: 13, color: color)),
          ),
        ],
      ),
    );
  }

  String _formatDate(String iso) {
    try {
      final date = DateTime.parse(iso);
      return DateFormat('dd.MM.yyyy').format(date);
    } catch (_) {
      return iso;
    }
  }
}

// --- Responses section for order owner ---
class _ResponsesSection extends ConsumerWidget {
  final int orderId;
  final Future<void> Function(int responseId) onSelect;
  final bool sending;

  const _ResponsesSection({
    required this.orderId,
    required this.onSelect,
    required this.sending,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final responsesAsync = ref.watch(_orderResponsesProvider(orderId));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Divider(),
        const SizedBox(height: 12),
        const Text('Отклики',
            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
        const SizedBox(height: 12),
        responsesAsync.when(
          data: (responses) {
            if (responses.isEmpty) {
              return Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Center(
                  child: Text('Пока нет откликов',
                      style: TextStyle(color: AppTheme.textMuted)),
                ),
              );
            }
            return Column(
              children: responses
                  .map((r) => _ResponseCard(
                        response: r,
                        onSelect: () => onSelect(r['id'] as int),
                        sending: sending,
                      ))
                  .toList(),
            );
          },
          loading: () => const Center(
            child: Padding(
              padding: EdgeInsets.all(16),
              child: CircularProgressIndicator(),
            ),
          ),
          error: (_, __) => const Text('Ошибка загрузки откликов',
              style: TextStyle(color: AppTheme.error)),
        ),
        const SizedBox(height: 16),
      ],
    );
  }
}

class _ResponseCard extends StatelessWidget {
  final Map<String, dynamic> response;
  final VoidCallback onSelect;
  final bool sending;

  const _ResponseCard({
    required this.response,
    required this.onSelect,
    required this.sending,
  });

  @override
  Widget build(BuildContext context) {
    final name = response['executorName'] as String? ?? '';
    final specialization = response['executorSpecialization'] as String? ?? '';
    final rating = (response['executorRating'] as num?)?.toDouble() ?? 0.0;
    final completed = response['executorCompletedOrders'] as int? ?? 0;
    final coverLetter = response['coverLetter'] as String? ?? '';
    final isSelected = response['isSelected'] == true;
    final executorId = response['executorId'] as int?;
    final createdAt = response['createdAt'] as String? ?? '';

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      color: isSelected ? AppTheme.success.withValues(alpha: 0.05) : null,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Executor info row
            InkWell(
              onTap: executorId != null
                  ? () => context.push('/executors/$executorId')
                  : null,
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 18,
                    backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                    child: Text(
                      name.isNotEmpty ? name[0].toUpperCase() : '?',
                      style: const TextStyle(
                          fontWeight: FontWeight.bold, color: AppTheme.primary),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(name,
                            style: const TextStyle(
                                fontWeight: FontWeight.w600, fontSize: 14)),
                        if (specialization.isNotEmpty)
                          Text(specialization,
                              style: const TextStyle(
                                  fontSize: 11, color: AppTheme.textSecondary)),
                      ],
                    ),
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.star, size: 14, color: Colors.amber),
                      const SizedBox(width: 2),
                      Text(rating.toStringAsFixed(1),
                          style: const TextStyle(fontSize: 12)),
                      const SizedBox(width: 8),
                      Text('$completed вып.',
                          style: const TextStyle(
                              fontSize: 11, color: AppTheme.textMuted)),
                    ],
                  ),
                ],
              ),
            ),
            if (coverLetter.isNotEmpty) ...[
              const SizedBox(height: 10),
              Text(coverLetter,
                  style: const TextStyle(
                      fontSize: 13, color: AppTheme.textSecondary, height: 1.4),
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis),
            ],
            const SizedBox(height: 10),
            Row(
              children: [
                Text(_formatTimeAgo(createdAt),
                    style: const TextStyle(
                        fontSize: 11, color: AppTheme.textMuted)),
                const Spacer(),
                if (isSelected)
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppTheme.success.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.check_circle,
                            size: 14, color: AppTheme.success),
                        SizedBox(width: 4),
                        Text('Выбран',
                            style: TextStyle(
                                fontSize: 12,
                                color: AppTheme.success,
                                fontWeight: FontWeight.w600)),
                      ],
                    ),
                  )
                else
                  SizedBox(
                    height: 32,
                    child: ElevatedButton(
                      onPressed: sending ? null : onSelect,
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        textStyle: const TextStyle(fontSize: 12),
                      ),
                      child: const Text('Выбрать'),
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatTimeAgo(String iso) {
    try {
      final date = DateTime.parse(iso);
      final diff = DateTime.now().difference(date);
      if (diff.inMinutes < 60) return '${diff.inMinutes} мин назад';
      if (diff.inHours < 24) return '${diff.inHours} ч назад';
      if (diff.inDays < 7) return '${diff.inDays} дн назад';
      return DateFormat('dd.MM.yyyy').format(date);
    } catch (_) {
      return '';
    }
  }
}

// --- Review section ---
class _ReviewSection extends ConsumerStatefulWidget {
  final int orderId;
  final Future<void> Function(int rating, String comment) onSubmit;
  final bool sending;

  const _ReviewSection({
    required this.orderId,
    required this.onSubmit,
    required this.sending,
  });

  @override
  ConsumerState<_ReviewSection> createState() => _ReviewSectionState();
}

class _ReviewSectionState extends ConsumerState<_ReviewSection> {
  int _rating = 0;
  final _commentController = TextEditingController();

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final reviewAsync = ref.watch(_orderReviewProvider(widget.orderId));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Divider(),
        const SizedBox(height: 12),
        const Text('Отзыв',
            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
        const SizedBox(height: 12),
        reviewAsync.when(
          data: (review) {
            if (review != null) {
              // Show existing review
              final r = review['rating'] as int? ?? 0;
              final comment = review['comment'] as String? ?? '';
              return Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppTheme.success.withValues(alpha: 0.05),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                      color: AppTheme.success.withValues(alpha: 0.2)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        ...List.generate(
                            5,
                            (i) => Icon(
                                  i < r ? Icons.star : Icons.star_border,
                                  size: 20,
                                  color: Colors.amber,
                                )),
                        const Spacer(),
                        const Icon(Icons.check_circle,
                            size: 16, color: AppTheme.success),
                        const SizedBox(width: 4),
                        const Text('Отзыв оставлен',
                            style: TextStyle(
                                fontSize: 12,
                                color: AppTheme.success,
                                fontWeight: FontWeight.w600)),
                      ],
                    ),
                    if (comment.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(comment,
                          style: const TextStyle(
                              fontSize: 13, color: AppTheme.textSecondary)),
                    ],
                  ],
                ),
              );
            }

            // Review form
            return Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text('Оцените работу исполнителя',
                        style: TextStyle(fontSize: 13)),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(
                          5,
                          (i) => GestureDetector(
                                onTap: () =>
                                    setState(() => _rating = i + 1),
                                child: Padding(
                                  padding:
                                      const EdgeInsets.symmetric(horizontal: 4),
                                  child: Icon(
                                    i < _rating
                                        ? Icons.star
                                        : Icons.star_border,
                                    size: 36,
                                    color: Colors.amber,
                                  ),
                                ),
                              )),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _commentController,
                      maxLines: 2,
                      decoration: const InputDecoration(
                        hintText: 'Комментарий (необязательно)...',
                      ),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton(
                      onPressed: _rating > 0 && !widget.sending
                          ? () => widget.onSubmit(
                              _rating, _commentController.text.trim())
                          : null,
                      child: const Text('Оставить отзыв'),
                    ),
                  ],
                ),
              ),
            );
          },
          loading: () => const SizedBox.shrink(),
          error: (_, __) => const SizedBox.shrink(),
        ),
        const SizedBox(height: 16),
      ],
    );
  }
}
