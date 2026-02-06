class Favorite {
  final String channelId;
  final DateTime addedAt;

  Favorite({
    required this.channelId,
    required this.addedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      'channelId': channelId,
      'addedAt': addedAt.toIso8601String(),
    };
  }

  factory Favorite.fromJson(Map<String, dynamic> json) {
    return Favorite(
      channelId: json['channelId'] as String,
      addedAt: DateTime.parse(json['addedAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Favorite &&
        other.channelId == channelId &&
        other.addedAt == addedAt;
  }

  @override
  int get hashCode {
    return Object.hash(channelId, addedAt);
  }
}
