import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:freelance_kg/core/api/api_client.dart';
import 'package:freelance_kg/core/theme.dart';
import 'package:freelance_kg/providers/auth_provider.dart';

class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _whatsappController = TextEditingController();
  final _bioController = TextEditingController();
  bool _hideFromList = false;
  bool _saving = false;
  bool _initialized = false;
  String? _saveMessage;

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _whatsappController.dispose();
    _bioController.dispose();
    super.dispose();
  }

  void _initFields() {
    final user = ref.read(authProvider).user;
    if (user == null || _initialized) return;
    _nameController.text = user.fullName;
    _phoneController.text = user.phone ?? '';
    _whatsappController.text = user.whatsappLink ?? '';
    _bioController.text = user.bio ?? '';
    _hideFromList = user.hideFromExecutorList;
    _initialized = true;
  }

  Future<void> _save() async {
    setState(() {
      _saving = true;
      _saveMessage = null;
    });
    try {
      await ApiClient().dio.put('/users/me', data: {
        'fullName': _nameController.text.trim(),
        'phone': _phoneController.text.trim().isEmpty
            ? null
            : _phoneController.text.trim(),
        'whatsappLink': _whatsappController.text.trim().isEmpty
            ? null
            : _whatsappController.text.trim(),
        'bio': _bioController.text.trim().isEmpty
            ? null
            : _bioController.text.trim(),
        'hideFromExecutorList': _hideFromList,
      });
      await ref.read(authProvider.notifier).refreshUser();
      if (mounted) {
        setState(() => _saveMessage = 'Сохранено!');
        Future.delayed(const Duration(seconds: 3),
            () => mounted ? setState(() => _saveMessage = null) : null);
      }
    } catch (_) {
      if (mounted) {
        setState(() => _saveMessage = 'Ошибка сохранения');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authProvider);

    if (!auth.isAuthenticated) {
      return Scaffold(
        appBar: AppBar(title: const Text('Профиль')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.person_outline,
                  size: 64, color: AppTheme.textMuted),
              const SizedBox(height: 16),
              const Text('Войдите в аккаунт',
                  style: TextStyle(
                      color: AppTheme.textSecondary, fontSize: 16)),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => context.go('/login'),
                child: const Text('Войти'),
              ),
              const SizedBox(height: 8),
              TextButton(
                onPressed: () => context.go('/register'),
                child: const Text('Зарегистрироваться'),
              ),
            ],
          ),
        ),
      );
    }

    final user = auth.user!;
    _initFields();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Настройки профиля'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () {
              ref.read(authProvider.notifier).logout();
              context.go('/');
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // My Activity Card
            _SectionCard(
              icon: Icons.dashboard,
              title: 'Моя активность',
              children: [
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.assignment,
                      color: AppTheme.primary),
                  title: const Text('Мои объявления',
                      style: TextStyle(fontSize: 14)),
                  subtitle: const Text('Задания, которые вы создали',
                      style: TextStyle(fontSize: 12)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/profile/my-orders'),
                ),
                const Divider(height: 1),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.send,
                      color: Colors.orange),
                  title: const Text('Мои отклики',
                      style: TextStyle(fontSize: 14)),
                  subtitle: const Text('Задания, на которые вы откликнулись',
                      style: TextStyle(fontSize: 12)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/profile/my-responses'),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Personal Info Card
            _SectionCard(
              icon: Icons.person,
              title: 'Личные данные',
              children: [
                TextField(
                  controller: _nameController,
                  decoration: const InputDecoration(labelText: 'Полное имя'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _phoneController,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Телефон',
                    hintText: '+996 XXX XXX XXX',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _whatsappController,
                  keyboardType: TextInputType.url,
                  decoration: const InputDecoration(
                    labelText: 'Ссылка на WhatsApp',
                    hintText: 'https://wa.me/996...',
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    const Icon(Icons.email, size: 16, color: AppTheme.textMuted),
                    const SizedBox(width: 8),
                    Text('Email: ${user.email}',
                        style: const TextStyle(
                            color: AppTheme.textSecondary, fontSize: 13)),
                  ],
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _bioController,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    labelText: 'О себе',
                    hintText: 'Расскажите о вашей специализации...',
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Необязательно. Видно на вашем профиле исполнителя.',
                  style: TextStyle(fontSize: 11, color: AppTheme.textMuted),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Contact Verification Card
            _SectionCard(
              icon: Icons.verified_user,
              title: 'Подтверждение контактов',
              children: [
                _VerificationRow(
                  icon: Icons.email,
                  label: 'Email',
                  value: user.email,
                  verified: user.emailVerified,
                  type: 'EMAIL',
                ),
                const Divider(height: 24),
                _VerificationRow(
                  icon: Icons.phone,
                  label: 'Телефон',
                  value: user.phone ?? 'Не указан',
                  verified: user.phoneVerified,
                  type: 'PHONE',
                ),
                const SizedBox(height: 8),
                const Text(
                  'Подтверждённые контакты повышают доверие к профилю',
                  style: TextStyle(fontSize: 11, color: AppTheme.textMuted),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Executor Settings Card
            _SectionCard(
              icon: _hideFromList ? Icons.visibility_off : Icons.visibility,
              title: 'Настройки исполнителя',
              children: [
                SwitchListTile(
                  value: !_hideFromList,
                  onChanged: (val) =>
                      setState(() => _hideFromList = !val),
                  title: const Text('Показывать в списке исполнителей',
                      style: TextStyle(fontSize: 14)),
                  subtitle: const Text(
                    'Заказчики смогут найти вас в разделе Исполнители',
                    style: TextStyle(fontSize: 12),
                  ),
                  contentPadding: EdgeInsets.zero,
                  activeColor: AppTheme.primary,
                ),
                if (!user.executorVerified)
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.orange.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                          color: Colors.orange.withValues(alpha: 0.3)),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.warning_amber,
                            color: Colors.orange, size: 20),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text.rich(
                            TextSpan(
                              text: 'Профиль появится после ',
                              style: const TextStyle(
                                  fontSize: 13, color: Colors.orange),
                              children: [
                                WidgetSpan(
                                  child: GestureDetector(
                                    onTap: () => context
                                        .push('/profile/verification'),
                                    child: const Text('верификации',
                                        style: TextStyle(
                                          fontSize: 13,
                                          color: Colors.orange,
                                          fontWeight: FontWeight.bold,
                                          decoration:
                                              TextDecoration.underline,
                                        )),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppTheme.primary.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    _hideFromList
                        ? 'Ваш профиль скрыт из списка исполнителей. Вы можете откликаться на заказы.'
                        : 'Ваш профиль виден в списке исполнителей. Заказчики могут найти вас.',
                    style: const TextStyle(
                        fontSize: 13, color: AppTheme.primaryDark),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Verification Card
            _SectionCard(
              icon: Icons.shield,
              title: 'Верификация',
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: user.executorVerified
                        ? AppTheme.success.withValues(alpha: 0.1)
                        : Colors.grey[100],
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        user.executorVerified
                            ? Icons.verified
                            : Icons.shield_outlined,
                        color: user.executorVerified
                            ? AppTheme.success
                            : AppTheme.textMuted,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          user.executorVerified
                              ? 'Вы верифицированный исполнитель'
                              : 'Верифицированные исполнители получают полный доступ к описаниям заказов',
                          style: TextStyle(
                            fontSize: 13,
                            color: user.executorVerified
                                ? AppTheme.success
                                : AppTheme.textSecondary,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                if (!user.executorVerified) ...[
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () =>
                        context.push('/profile/verification'),
                    icon: const Icon(Icons.arrow_forward, size: 18),
                    label: const Text('Пройти верификацию'),
                  ),
                ],
              ],
            ),
            const SizedBox(height: 16),

            // Legal links
            _SectionCard(
              icon: Icons.description,
              title: 'Правовая информация',
              children: [
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.privacy_tip,
                      color: AppTheme.primary),
                  title: const Text('Политика конфиденциальности',
                      style: TextStyle(fontSize: 14)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/profile/privacy'),
                ),
                const Divider(height: 1),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.gavel,
                      color: Colors.orange),
                  title: const Text('Условия использования',
                      style: TextStyle(fontSize: 14)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/profile/terms'),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Save button
            if (_saveMessage != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(
                  _saveMessage!,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: _saveMessage == 'Сохранено!'
                        ? AppTheme.success
                        : AppTheme.error,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ElevatedButton.icon(
              onPressed: _saving ? null : _save,
              icon: _saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.save, size: 18),
              label:
                  Text(_saving ? 'Сохранение...' : 'Сохранить изменения'),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final List<Widget> children;

  const _SectionCard(
      {required this.icon, required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Icon(icon, size: 20, color: AppTheme.textSecondary),
                const SizedBox(width: 10),
                Text(title,
                    style: const TextStyle(
                        fontSize: 16, fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }
}

class _VerificationRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final bool verified;
  final String type; // 'EMAIL' or 'PHONE'

  const _VerificationRow({
    required this.icon,
    required this.label,
    required this.value,
    required this.verified,
    this.type = 'EMAIL',
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 20, color: AppTheme.textMuted),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(value, style: const TextStyle(fontWeight: FontWeight.w500)),
              Text(label,
                  style: const TextStyle(
                      fontSize: 12, color: AppTheme.textMuted)),
            ],
          ),
        ),
        if (verified)
          const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.check_circle, color: AppTheme.success, size: 16),
              SizedBox(width: 4),
              Text('Подтверждён',
                  style: TextStyle(
                      color: AppTheme.success,
                      fontSize: 12,
                      fontWeight: FontWeight.w600)),
            ],
          )
        else
          SizedBox(
            height: 30,
            child: OutlinedButton(
              onPressed: () => _showVerificationDialog(context, type),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                textStyle: const TextStyle(fontSize: 11),
              ),
              child: const Text('Подтвердить'),
            ),
          ),
      ],
    );
  }

  void _showVerificationDialog(BuildContext context, String type) {
    showDialog(
      context: context,
      builder: (_) => _ContactVerificationDialog(type: type),
    );
  }
}

class _ContactVerificationDialog extends ConsumerStatefulWidget {
  final String type;
  const _ContactVerificationDialog({required this.type});

  @override
  ConsumerState<_ContactVerificationDialog> createState() =>
      _ContactVerificationDialogState();
}

class _ContactVerificationDialogState
    extends ConsumerState<_ContactVerificationDialog> {
  final _codeController = TextEditingController();
  bool _codeSent = false;
  bool _loading = false;
  String? _error;
  String? _success;

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ApiClient().dio.post(
        '/contact-verification/send-code',
        data: {'type': widget.type},
      );
      if (mounted) {
        setState(() {
          _codeSent = true;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Не удалось отправить код';
          _loading = false;
        });
      }
    }
  }

  Future<void> _verifyCode() async {
    if (_codeController.text.trim().length != 6) {
      setState(() => _error = 'Введите 6-значный код');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final response = await ApiClient().dio.post(
        '/contact-verification/verify',
        data: {
          'type': widget.type,
          'code': _codeController.text.trim(),
        },
      );
      final success = response.data['success'] == true;
      if (mounted) {
        if (success) {
          await ref.read(authProvider.notifier).refreshUser();
          if (mounted) {
            setState(() {
              _success = 'Успешно подтверждено!';
              _loading = false;
            });
            Future.delayed(const Duration(seconds: 1), () {
              if (mounted) Navigator.of(context).pop();
            });
          }
        } else {
          setState(() {
            _error = response.data['message'] as String? ??
                'Неверный или просроченный код';
            _loading = false;
          });
        }
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Ошибка проверки кода';
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final label = widget.type == 'EMAIL' ? 'email' : 'телефон';

    return AlertDialog(
      title: Text('Подтверждение ${widget.type == 'EMAIL' ? 'email' : 'телефона'}'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (_success != null)
            Column(
              children: [
                const Icon(Icons.check_circle,
                    color: AppTheme.success, size: 48),
                const SizedBox(height: 8),
                Text(_success!,
                    style: const TextStyle(color: AppTheme.success)),
              ],
            )
          else if (!_codeSent) ...[
            Text('Мы отправим код подтверждения на ваш $label.',
                style: const TextStyle(fontSize: 14)),
            if (_error != null) ...[
              const SizedBox(height: 8),
              Text(_error!,
                  style: const TextStyle(color: AppTheme.error, fontSize: 13)),
            ],
          ] else ...[
            Text('Введите 6-значный код, отправленный на ваш $label',
                style: const TextStyle(fontSize: 14)),
            const SizedBox(height: 16),
            TextField(
              controller: _codeController,
              keyboardType: TextInputType.number,
              maxLength: 6,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 24, letterSpacing: 8),
              decoration: const InputDecoration(
                counterText: '',
                hintText: '000000',
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 8),
              Text(_error!,
                  style: const TextStyle(color: AppTheme.error, fontSize: 13)),
            ],
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: _loading ? null : () => Navigator.of(context).pop(),
          child: const Text('Отмена'),
        ),
        if (_success == null)
          ElevatedButton(
            onPressed: _loading ? null : (_codeSent ? _verifyCode : _sendCode),
            child: _loading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                        strokeWidth: 2, color: Colors.white),
                  )
                : Text(_codeSent ? 'Подтвердить' : 'Отправить код'),
          ),
      ],
    );
  }
}
