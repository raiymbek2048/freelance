import 'package:flutter/material.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/models/order.dart';
import 'package:intl/intl.dart';

class OrderCard extends StatelessWidget {
  final OrderItem order;
  final VoidCallback? onTap;

  const OrderCard({super.key, required this.order, this.onTap});

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
              // Title row
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      order.title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: AppTheme.textPrimary,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 12),
                  if (order.budgetMax != null)
                    Text(
                      '${order.budgetMax!.toInt()} \u0441',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: AppTheme.primary,
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 6),
              // Description
              Text(
                order.description,
                style: const TextStyle(
                  fontSize: 13,
                  color: AppTheme.textSecondary,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 10),
              // Bottom row: deadline + responses
              Row(
                children: [
                  if (order.deadline != null) ...[
                    Icon(Icons.schedule, size: 14, color: Colors.orange[400]),
                    const SizedBox(width: 4),
                    Text(
                      'до ${_formatDate(order.deadline!)}',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.orange[600],
                      ),
                    ),
                    const SizedBox(width: 12),
                  ],
                  Text(
                    '${order.responseCount} откликов',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppTheme.textMuted,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
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
