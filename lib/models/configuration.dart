import 'dart:convert';

enum ConfigType {
  xtream,
  m3uNetwork,
  m3uLocal,
  directLink,
}

class Configuration {
  final String id;
  final String name;
  final ConfigType type;
  final Map<String, dynamic> credentials;
  final DateTime createdAt;
  final DateTime updatedAt;
  final int orderIndex;
  
  // Xtream specific fields
  final DateTime? lastRefreshed;
  final DateTime? expirationDate;
  final String? accountStatus;

  Configuration({
    required this.id,
    required this.name,
    required this.type,
    required this.credentials,
    required this.createdAt,
    required this.updatedAt,
    this.orderIndex = 0,
    this.lastRefreshed,
    this.expirationDate,
    this.accountStatus,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'type': type.name,
      'credentials': jsonEncode(credentials),
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
      'order_index': orderIndex,
      'last_refreshed': lastRefreshed?.toIso8601String(),
      'expiration_date': expirationDate?.toIso8601String(),
      'account_status': accountStatus,
    };
  }

  factory Configuration.fromMap(Map<String, dynamic> map) {
    return Configuration(
      id: map['id'] as String,
      name: map['name'] as String,
      type: ConfigType.values.byName(map['type'] as String),
      credentials: jsonDecode(map['credentials'] as String) as Map<String, dynamic>,
      createdAt: DateTime.parse(map['created_at'] as String),
      updatedAt: DateTime.parse(map['updated_at'] as String),
      orderIndex: map['order_index'] as int? ?? 0,
      lastRefreshed: map['last_refreshed'] != null ? DateTime.parse(map['last_refreshed'] as String) : null,
      expirationDate: map['expiration_date'] != null ? DateTime.parse(map['expiration_date'] as String) : null,
      accountStatus: map['account_status'] as String?,
    );
  }

  Configuration copyWith({
    String? name,
    ConfigType? type,
    Map<String, dynamic>? credentials,
    DateTime? updatedAt,
    int? orderIndex,
    DateTime? lastRefreshed,
    DateTime? expirationDate,
    String? accountStatus,
  }) {
    return Configuration(
      id: id,
      name: name ?? this.name,
      type: type ?? this.type,
      credentials: credentials ?? this.credentials,
      createdAt: createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      orderIndex: orderIndex ?? this.orderIndex,
      lastRefreshed: lastRefreshed ?? this.lastRefreshed,
      expirationDate: expirationDate ?? this.expirationDate,
      accountStatus: accountStatus ?? this.accountStatus,
    );
  }
}
