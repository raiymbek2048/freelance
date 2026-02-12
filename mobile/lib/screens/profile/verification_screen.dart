import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:dio/dio.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/auth_provider.dart';
import 'package:freelance_kg/providers/categories_provider.dart';

class VerificationScreen extends ConsumerStatefulWidget {
  const VerificationScreen({super.key});

  @override
  ConsumerState<VerificationScreen> createState() =>
      _VerificationScreenState();
}

class _VerificationScreenState extends ConsumerState<VerificationScreen> {
  final _picker = ImagePicker();
  File? _frontDoc;
  File? _backDoc;
  final Set<int> _selectedCategories = {};
  bool _submitting = false;

  Future<void> _pickImage(bool isFront) async {
    final image = await _picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 1200,
      imageQuality: 85,
    );
    if (image != null) {
      setState(() {
        if (isFront) {
          _frontDoc = File(image.path);
        } else {
          _backDoc = File(image.path);
        }
      });
    }
  }

  Future<void> _submit() async {
    if (_frontDoc == null) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Загрузите фото документа (лицевая сторона)')));
      return;
    }
    if (_selectedCategories.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Выберите хотя бы одну категорию')));
      return;
    }

    setState(() => _submitting = true);
    try {
      final formData = FormData.fromMap({
        'frontDocument': await MultipartFile.fromFile(_frontDoc!.path,
            filename: 'front.jpg'),
        if (_backDoc != null)
          'backDocument': await MultipartFile.fromFile(_backDoc!.path,
              filename: 'back.jpg'),
        'categoryIds': _selectedCategories.toList(),
      });

      await ApiClient().dio.post(
        '/verification/submit',
        data: formData,
        options: Options(contentType: 'multipart/form-data'),
      );

      await ref.read(authProvider.notifier).refreshUser();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Заявка отправлена на проверку!')));
        context.pop();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Ошибка отправки')));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final categoriesAsync = ref.watch(categoriesProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Верификация')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Для работы исполнителем необходимо пройти верификацию',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 14),
            ),
            const SizedBox(height: 20),

            // Document photos
            const Text('Документ, удостоверяющий личность',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _DocUploadCard(
                    label: 'Лицевая сторона',
                    file: _frontDoc,
                    onTap: () => _pickImage(true),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _DocUploadCard(
                    label: 'Обратная сторона',
                    file: _backDoc,
                    onTap: () => _pickImage(false),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Categories
            const Text('Выберите категории услуг',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
            const SizedBox(height: 4),
            const Text('Отметьте категории, в которых вы хотите работать',
                style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
            const SizedBox(height: 12),
            categoriesAsync.when(
              data: (cats) => Column(
                children: cats
                    .map((cat) => CheckboxListTile(
                          value: _selectedCategories.contains(cat.id),
                          onChanged: (val) {
                            setState(() {
                              if (val == true) {
                                _selectedCategories.add(cat.id);
                              } else {
                                _selectedCategories.remove(cat.id);
                              }
                            });
                          },
                          title: Text(cat.name),
                          subtitle: cat.description != null
                              ? Text(cat.description!,
                                  style: const TextStyle(fontSize: 12))
                              : null,
                          activeColor: AppTheme.primary,
                          controlAffinity: ListTileControlAffinity.leading,
                          contentPadding: EdgeInsets.zero,
                        ))
                    .toList(),
              ),
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (_, __) => const Text('Ошибка загрузки категорий'),
            ),
            const SizedBox(height: 24),

            // Submit
            ElevatedButton(
              onPressed: _submitting ? null : _submit,
              child: _submitting
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Text('Отправить на верификацию'),
            ),
          ],
        ),
      ),
    );
  }
}

class _DocUploadCard extends StatelessWidget {
  final String label;
  final File? file;
  final VoidCallback onTap;

  const _DocUploadCard(
      {required this.label, required this.file, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 120,
        decoration: BoxDecoration(
          color: Colors.grey[100],
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
              color: file != null ? AppTheme.primary : AppTheme.border),
        ),
        child: file != null
            ? ClipRRect(
                borderRadius: BorderRadius.circular(11),
                child: Image.file(file!, fit: BoxFit.cover,
                    width: double.infinity),
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.add_a_photo,
                      color: AppTheme.textMuted, size: 28),
                  const SizedBox(height: 6),
                  Text(label,
                      style: const TextStyle(
                          fontSize: 11, color: AppTheme.textMuted),
                      textAlign: TextAlign.center),
                ],
              ),
      ),
    );
  }
}
