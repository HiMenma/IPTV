enum ConfigType {
  xtream,
  m3uLocal,
  m3uNetwork;

  String toJson() => name;

  static ConfigType fromJson(String json) {
    return ConfigType.values.firstWhere(
      (type) => type.name == json,
      orElse: () => throw ArgumentError('Invalid ConfigType: $json'),
    );
  }
}

class Configuration {
  final String id;
  final String name;
  final ConfigType type;
  final Map<String, dynamic> credentials;
  final DateTime createdAt;
  final DateTime? lastRefreshed;
  final DateTime? expirationDate; // For Xtream accounts
  final String? accountStatus; // For Xtream accounts

  Configuration({
    required this.id,
    required this.name,
    required this.type,
    required this.credentials,
    required this.createdAt,
    this.lastRefreshed,
    this.expirationDate,
    this.accountStatus,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'type': type.toJson(),
      'credentials': credentials,
      'createdAt': createdAt.toIso8601String(),
      'lastRefreshed': lastRefreshed?.toIso8601String(),
      'expirationDate': expirationDate?.toIso8601String(),
      'accountStatus': accountStatus,
    };
  }

  factory Configuration.fromJson(Map<String, dynamic> json) {
    return Configuration(
      id: json['id'] as String,
      name: json['name'] as String,
      type: ConfigType.fromJson(json['type'] as String),
      credentials: Map<String, dynamic>.from(json['credentials'] as Map),
      createdAt: DateTime.parse(json['createdAt'] as String),
      lastRefreshed: json['lastRefreshed'] != null
          ? DateTime.parse(json['lastRefreshed'] as String)
          : null,
      expirationDate: json['expirationDate'] != null
          ? DateTime.parse(json['expirationDate'] as String)
          : null,
      accountStatus: json['accountStatus'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Configuration &&
        other.id == id &&
        other.name == name &&
        other.type == type &&
        _mapEquals(other.credentials, credentials) &&
        other.createdAt == createdAt &&
        other.lastRefreshed == lastRefreshed &&
        other.expirationDate == expirationDate &&
        other.accountStatus == accountStatus;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      name,
      type,
      Object.hashAll(credentials.entries.map((e) => Object.hash(e.key, e.value))),
      createdAt,
      lastRefreshed,
      expirationDate,
      accountStatus,
    );
  }

  bool _mapEquals(Map<String, dynamic> a, Map<String, dynamic> b) {
    if (a.length != b.length) return false;
    for (var key in a.keys) {
      if (!b.containsKey(key) || a[key] != b[key]) return false;
    }
    return true;
  }
}
