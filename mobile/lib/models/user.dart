class User {
  final int id;
  final String email;
  final String fullName;
  final String? phone;
  final String? avatarUrl;
  final String? bio;
  final String? whatsappLink;
  final bool executorVerified;
  final bool emailVerified;
  final bool phoneVerified;
  final bool hideFromExecutorList;
  final String role;

  User({
    required this.id,
    required this.email,
    required this.fullName,
    this.phone,
    this.avatarUrl,
    this.bio,
    this.whatsappLink,
    this.executorVerified = false,
    this.emailVerified = false,
    this.phoneVerified = false,
    this.hideFromExecutorList = false,
    this.role = 'USER',
  });

  factory User.fromJson(Map<String, dynamic> json) => User(
        id: json['id'],
        email: json['email'] ?? '',
        fullName: json['fullName'] ?? '',
        phone: json['phone'],
        avatarUrl: json['avatarUrl'],
        bio: json['bio'],
        whatsappLink: json['whatsappLink'],
        executorVerified: json['executorVerified'] ?? false,
        emailVerified: json['emailVerified'] ?? false,
        phoneVerified: json['phoneVerified'] ?? false,
        hideFromExecutorList: json['hideFromExecutorList'] ?? false,
        role: json['role'] ?? 'USER',
      );
}
