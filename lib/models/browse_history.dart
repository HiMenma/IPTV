class BrowseHistory {
  final String channelId;
  final DateTime watchedAt;

  BrowseHistory({
    required this.channelId,
    required this.watchedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      'channelId': channelId,
      'watchedAt': watchedAt.toIso8601String(),
    };
  }

  factory BrowseHistory.fromJson(Map<String, dynamic> json) {
    return BrowseHistory(
      channelId: json['channelId'] as String,
      watchedAt: DateTime.parse(json['watchedAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is BrowseHistory &&
        other.channelId == channelId &&
        other.watchedAt == watchedAt;
  }

  @override
  int get hashCode {
    return Object.hash(channelId, watchedAt);
  }
}
