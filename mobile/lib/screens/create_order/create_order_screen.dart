import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/constants.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/categories_provider.dart';
import 'package:freelance_kg/providers/orders_provider.dart';

class CreateOrderScreen extends ConsumerStatefulWidget {
  const CreateOrderScreen({super.key});

  @override
  ConsumerState<CreateOrderScreen> createState() => _CreateOrderScreenState();
}

class _CreateOrderScreenState extends ConsumerState<CreateOrderScreen> {
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _budgetController = TextEditingController();

  int? _categoryId;
  String _selectedCity = 'Бишкек';
  bool _remote = false;
  DateTime _deadlineDate = DateTime.now().add(const Duration(days: 7));
  bool _loading = false;

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _budgetController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_categoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Выберите категорию')));
      return;
    }
    if (_titleController.text.trim().length < 10) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Название минимум 10 символов')));
      return;
    }
    if (_descriptionController.text.trim().length < 20) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Описание минимум 20 символов')));
      return;
    }

    setState(() => _loading = true);

    final deadline =
        '${_deadlineDate.year}-${_deadlineDate.month.toString().padLeft(2, '0')}-${_deadlineDate.day.toString().padLeft(2, '0')}';
    final location =
        _remote ? 'Удаленно' : '$_selectedCity, Кыргызстан';
    final budget = double.tryParse(_budgetController.text);

    try {
      await ApiClient().dio.post('/orders', data: {
        'title': _titleController.text.trim(),
        'description': _descriptionController.text.trim(),
        'categoryId': _categoryId,
        'budgetMin': budget,
        'budgetMax': budget,
        'deadline': deadline,
        'location': location,
        'attachments': [],
      });
      ref.invalidate(ordersProvider);
      if (mounted) context.pop();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка создания задания')));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(categoriesProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Создание задания')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Category
            const Text('Категория',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
            const SizedBox(height: 6),
            categories.when(
              data: (cats) => DropdownButtonFormField<int>(
                initialValue: _categoryId,
                decoration: const InputDecoration(hintText: 'Выберите категорию'),
                items: cats
                    .map((c) =>
                        DropdownMenuItem(value: c.id, child: Text(c.name)))
                    .toList(),
                onChanged: (val) => setState(() => _categoryId = val),
              ),
              loading: () => const LinearProgressIndicator(),
              error: (_, __) => const Text('Ошибка загрузки категорий'),
            ),
            const SizedBox(height: 16),

            // Location
            const Text('Локация',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
            const SizedBox(height: 6),
            Row(
              children: [
                ChoiceChip(
                  label: const Text('Указать город'),
                  selected: !_remote,
                  onSelected: (_) => setState(() => _remote = false),
                  selectedColor: AppTheme.primary.withValues(alpha: 0.15),
                ),
                const SizedBox(width: 8),
                ChoiceChip(
                  label: const Text('Удалённо'),
                  selected: _remote,
                  onSelected: (_) => setState(() => _remote = true),
                  selectedColor: AppTheme.primary.withValues(alpha: 0.15),
                ),
              ],
            ),
            if (!_remote) ...[
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                initialValue: _selectedCity,
                items: AppConstants.cities
                    .where((c) => c != 'Все города')
                    .map((c) => DropdownMenuItem(value: c, child: Text(c)))
                    .toList(),
                onChanged: (val) =>
                    setState(() => _selectedCity = val ?? 'Бишкек'),
              ),
            ],
            const SizedBox(height: 16),

            // Description
            const Text('Описание',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
            const SizedBox(height: 6),
            TextField(
              controller: _descriptionController,
              maxLines: 5,
              decoration:
                  const InputDecoration(hintText: 'Опишите задание подробно...'),
            ),
            const SizedBox(height: 16),

            // Title
            const Text('Название задания',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
            const SizedBox(height: 6),
            TextField(
              controller: _titleController,
              decoration:
                  const InputDecoration(hintText: 'Краткое название задания'),
            ),
            const SizedBox(height: 16),

            // Deadline
            const Text('Выполнить до',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
            const SizedBox(height: 6),
            OutlinedButton.icon(
              icon: const Icon(Icons.calendar_today, size: 16),
              label: Text(
                  '${_deadlineDate.day.toString().padLeft(2, '0')}.${_deadlineDate.month.toString().padLeft(2, '0')}.${_deadlineDate.year}'),
              onPressed: () async {
                final date = await showDatePicker(
                  context: context,
                  initialDate: _deadlineDate,
                  firstDate: DateTime.now(),
                  lastDate:
                      DateTime.now().add(const Duration(days: 365)),
                );
                if (date != null) {
                  setState(() => _deadlineDate = date);
                }
              },
            ),
            const SizedBox(height: 16),

            // Budget
            const Text('Стоимость (сом)',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
            const SizedBox(height: 6),
            TextField(
              controller: _budgetController,
              keyboardType: TextInputType.number,
              decoration:
                  const InputDecoration(hintText: 'Укажите стоимость'),
            ),
            const SizedBox(height: 24),

            // Submit
            ElevatedButton(
              onPressed: _loading ? null : _submit,
              child: _loading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Text('Опубликовать задание'),
            ),
          ],
        ),
      ),
    );
  }
}
