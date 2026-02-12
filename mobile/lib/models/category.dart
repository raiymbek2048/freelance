class Category {
  final int id;
  final String name;
  final String slug;
  final String? description;
  final int? sortOrder;

  Category({
    required this.id,
    required this.name,
    required this.slug,
    this.description,
    this.sortOrder,
  });

  factory Category.fromJson(Map<String, dynamic> json) => Category(
        id: json['id'],
        name: json['name'] ?? '',
        slug: json['slug'] ?? '',
        description: json['description'],
        sortOrder: json['sortOrder'],
      );
}
