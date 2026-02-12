class OrderItem {
  final int id;
  final String title;
  final String description;
  final int categoryId;
  final String categoryName;
  final int clientId;
  final String clientName;
  final double? budgetMin;
  final double? budgetMax;
  final String? location;
  final String? deadline;
  final String status;
  final int responseCount;
  final bool hasResponded;
  final String createdAt;

  OrderItem({
    required this.id,
    required this.title,
    required this.description,
    required this.categoryId,
    required this.categoryName,
    required this.clientId,
    required this.clientName,
    this.budgetMin,
    this.budgetMax,
    this.location,
    this.deadline,
    required this.status,
    required this.responseCount,
    this.hasResponded = false,
    required this.createdAt,
  });

  factory OrderItem.fromJson(Map<String, dynamic> json) => OrderItem(
        id: json['id'],
        title: json['title'] ?? '',
        description: json['description'] ?? '',
        categoryId: json['categoryId'] ?? 0,
        categoryName: json['categoryName'] ?? '',
        clientId: json['clientId'] ?? 0,
        clientName: json['clientName'] ?? '',
        budgetMin: (json['budgetMin'] as num?)?.toDouble(),
        budgetMax: (json['budgetMax'] as num?)?.toDouble(),
        location: json['location'],
        deadline: json['deadline'],
        status: json['status'] ?? 'NEW',
        responseCount: json['responseCount'] ?? 0,
        hasResponded: json['hasResponded'] ?? false,
        createdAt: json['createdAt'] ?? '',
      );
}

class PageResponse<T> {
  final List<T> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final bool first;
  final bool last;

  PageResponse({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.first,
    required this.last,
  });
}
