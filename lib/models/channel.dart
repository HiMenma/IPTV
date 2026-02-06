class Channel {
  final String id;
  final String name;
  final String streamUrl;
  final String? logoUrl;
  final String? category;
  final String configId;

  Channel({
    required this.id,
    required this.name,
    required this.streamUrl,
    this.logoUrl,
    this.category,
    required this.configId,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'streamUrl': streamUrl,
      'logoUrl': logoUrl,
      'category': category,
      'configId': configId,
    };
  }

  factory Channel.fromJson(Map<String, dynamic> json) {
    return Channel(
      id: json['id'] as String,
      name: json['name'] as String,
      streamUrl: json['streamUrl'] as String,
      logoUrl: json['logoUrl'] as String?,
      category: json['category'] as String?,
      configId: json['configId'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Channel &&
        other.id == id &&
        other.name == name &&
        other.streamUrl == streamUrl &&
        other.logoUrl == logoUrl &&
        other.category == category &&
        other.configId == configId;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      name,
      streamUrl,
      logoUrl,
      category,
      configId,
    );
  }
}
