import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';
import '../models/channel.dart';
import '../utils/error_handler.dart';

/// Xtream account information
class XtreamAccountInfo {
  final String username;
  final String status;
  final DateTime? expirationDate;
  final DateTime? createdAt;
  final bool isActive;
  final int? maxConnections;

  XtreamAccountInfo({
    required this.username,
    required this.status,
    this.expirationDate,
    this.createdAt,
    required this.isActive,
    this.maxConnections,
  });

  factory XtreamAccountInfo.fromJson(Map<String, dynamic> json) {
    final expDate = json['exp_date'];
    final createDate = json['created_at'];
    
    return XtreamAccountInfo(
      username: json['username']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      expirationDate: expDate != null && expDate.toString() != '0' && expDate.toString() != 'null'
          ? DateTime.fromMillisecondsSinceEpoch(int.tryParse(expDate.toString()) ?? 0 * 1000)
          : null,
      createdAt: createDate != null && createDate.toString() != '0' && createDate.toString() != 'null'
          ? DateTime.fromMillisecondsSinceEpoch(int.tryParse(createDate.toString()) ?? 0 * 1000)
          : null,
      isActive: json['status']?.toString().toLowerCase() == 'active',
      maxConnections: json['max_connections'] != null 
          ? int.tryParse(json['max_connections'].toString())
          : null,
    );
  }
}

class XtreamService {
  final Dio _dio;
  final Uuid _uuid;
  
  // Cache for Xtream channel data
  final Map<String, List<Channel>> _cache = {};
  final Map<String, DateTime> _cacheTimestamps = {};
  final Map<String, XtreamAccountInfo> _accountInfoCache = {};
  
  // Cache duration: 5 minutes
  static const Duration _cacheDuration = Duration(minutes: 5);

  XtreamService({Dio? dio, Uuid? uuid})
      : _dio = dio ?? Dio(),
        _uuid = uuid ?? const Uuid();

  /// Authenticate with Xtream Codes API and retrieve available channels
  /// Returns list of channels if authentication succeeds
  /// Throws exception if authentication fails
  /// Requirements: 2.1, 2.3
  Future<List<Channel>> authenticate(
    String serverUrl,
    String username,
    String password,
    String configId, {
    bool forceRefresh = false,
  }) async {
    // Check cache first
    final cacheKey = 'xtream:$serverUrl:$username:$configId';
    if (!forceRefresh && _isCacheValid(cacheKey)) {
      return _cache[cacheKey]!;
    }
    
    return ErrorHandler.executeWithRetry(() async {
      // Normalize server URL (remove trailing slash)
      final normalizedUrl = serverUrl.endsWith('/')
          ? serverUrl.substring(0, serverUrl.length - 1)
          : serverUrl;

      // First, get categories to build a category map
      final categoryMap = await _getCategoryMap(normalizedUrl, username, password);

      // Xtream API authentication endpoint
      final authUrl = '$normalizedUrl/player_api.php';
      
      final response = await _dio.get(
        authUrl,
        queryParameters: {
          'username': username,
          'password': password,
          'action': 'get_live_streams',
        },
        options: Options(
          validateStatus: (status) => status != null && status < 500,
        ),
      ).timeout(
        const Duration(seconds: 30),
        onTimeout: () {
          throw DioException(
            requestOptions: RequestOptions(path: authUrl),
            type: DioExceptionType.connectionTimeout,
          );
        },
      );

      // Check for authentication failure
      if (response.statusCode == 401 || response.statusCode == 403) {
        throw NetworkException(
          'Invalid Xtream credentials. Please check your username and password.',
          type: NetworkErrorType.authenticationError,
        );
      }

      if (response.statusCode != 200) {
        throw NetworkException(
          'Xtream API error: HTTP ${response.statusCode}',
          type: NetworkErrorType.serverError,
        );
      }

      // Parse response
      final data = response.data;
      
      // Check if response indicates authentication failure
      if (data is Map && data.containsKey('user_info')) {
        final userInfo = data['user_info'];
        if (userInfo is Map && userInfo['auth'] == 0) {
          throw NetworkException(
            'Invalid Xtream credentials. Please check your username and password.',
            type: NetworkErrorType.authenticationError,
          );
        }
      }

      // Parse channels from response
      List<Channel> channels;
      if (data is List) {
        channels = _parseChannels(data, configId, normalizedUrl, username, password, categoryMap);
      } else if (data is Map && data.containsKey('streams')) {
        channels = _parseChannels(data['streams'], configId, normalizedUrl, username, password, categoryMap);
      } else {
        // Empty channel list is valid for successful authentication
        channels = [];
      }
      
      // Cache the result
      final cacheKey = 'xtream:$normalizedUrl:$username:$configId';
      _updateCache(cacheKey, channels);
      
      return channels;
    });
  }

  /// Get channels from an existing Xtream configuration
  Future<List<Channel>> getChannels(
    String serverUrl,
    String username,
    String password,
    String configId, {
    bool forceRefresh = false,
  }) async {
    // For Xtream API, getting channels is the same as authentication
    return authenticate(serverUrl, username, password, configId, forceRefresh: forceRefresh);
  }

  /// Get account information from Xtream API
  Future<XtreamAccountInfo?> getAccountInfo(
    String serverUrl,
    String username,
    String password,
  ) async {
    final cacheKey = 'account:$serverUrl:$username';
    
    // Check cache first
    if (_accountInfoCache.containsKey(cacheKey)) {
      return _accountInfoCache[cacheKey];
    }
    
    return ErrorHandler.executeWithRetry(() async {
      // Normalize server URL
      final normalizedUrl = serverUrl.endsWith('/')
          ? serverUrl.substring(0, serverUrl.length - 1)
          : serverUrl;

      final response = await _dio.get(
        '$normalizedUrl/player_api.php',
        queryParameters: {
          'username': username,
          'password': password,
        },
        options: Options(
          validateStatus: (status) => status != null && status < 500,
        ),
      ).timeout(
        const Duration(seconds: 30),
        onTimeout: () {
          throw DioException(
            requestOptions: RequestOptions(path: '$normalizedUrl/player_api.php'),
            type: DioExceptionType.connectionTimeout,
          );
        },
      );

      if (response.statusCode != 200) {
        return null;
      }

      final data = response.data;
      if (data is Map && data.containsKey('user_info')) {
        final accountInfo = XtreamAccountInfo.fromJson(data['user_info']);
        _accountInfoCache[cacheKey] = accountInfo;
        return accountInfo;
      }

      return null;
    });
  }

  /// Get category map (ID -> Name) from Xtream API
  Future<Map<String, String>> _getCategoryMap(
    String serverUrl,
    String username,
    String password,
  ) async {
    try {
      final response = await _dio.get(
        '$serverUrl/player_api.php',
        queryParameters: {
          'username': username,
          'password': password,
          'action': 'get_live_categories',
        },
      ).timeout(
        const Duration(seconds: 30),
        onTimeout: () {
          throw DioException(
            requestOptions: RequestOptions(path: '$serverUrl/player_api.php'),
            type: DioExceptionType.connectionTimeout,
          );
        },
      );

      if (response.statusCode != 200) {
        return {};
      }

      final data = response.data;
      final categoryMap = <String, String>{};

      if (data is List) {
        for (final item in data) {
          if (item is Map) {
            final categoryId = item['category_id']?.toString();
            final categoryName = item['category_name']?.toString();
            if (categoryId != null && categoryName != null && categoryName.isNotEmpty) {
              categoryMap[categoryId] = categoryName;
            }
          }
        }
      }

      return categoryMap;
    } catch (e) {
      // Return empty map if categories can't be fetched
      return {};
    }
  }

  /// Get categories from Xtream API
  /// Requirements: 2.3
  Future<List<String>> getCategories(
    String serverUrl,
    String username,
    String password,
  ) async {
    return ErrorHandler.executeWithRetry(() async {
      // Normalize server URL
      final normalizedUrl = serverUrl.endsWith('/')
          ? serverUrl.substring(0, serverUrl.length - 1)
          : serverUrl;

      final categoryMap = await _getCategoryMap(normalizedUrl, username, password);
      return categoryMap.values.toList()..sort();
    });
  }

  /// Parse channel list from Xtream API response
  List<Channel> _parseChannels(
    dynamic channelsData,
    String configId,
    String serverUrl,
    String username,
    String password,
    Map<String, String> categoryMap,
  ) {
    if (channelsData is! List) {
      return [];
    }

    final channels = <Channel>[];

    for (final item in channelsData) {
      if (item is! Map) continue;

      try {
        // Extract channel information
        final streamId = item['stream_id']?.toString() ?? item['id']?.toString();
        final name = item['name']?.toString() ?? item['title']?.toString();
        
        if (streamId == null || name == null || name.isEmpty) {
          continue;
        }

        // Build stream URL
        // Format: http://server:port/live/username/password/streamId.ext
        final streamUrl = '$serverUrl/live/$username/$password/$streamId.m3u8';

        // Extract optional fields
        final logoUrl = item['stream_icon']?.toString() ?? item['logo']?.toString();
        
        // Get category name from category_id using the category map
        String? category;
        final categoryId = item['category_id']?.toString();
        if (categoryId != null && categoryMap.containsKey(categoryId)) {
          category = categoryMap[categoryId];
        } else {
          // Fallback to direct category_name if available
          category = item['category_name']?.toString() ?? item['category']?.toString();
        }

        // Use UUID v5 for stable ID
        const String namespace = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';
        final stableId = _uuid.v5(namespace, '$configId:$streamId');

        channels.add(Channel(
          id: stableId,
          name: name,
          streamUrl: streamUrl,
          logoUrl: logoUrl?.isNotEmpty == true ? logoUrl : null,
          category: category?.isNotEmpty == true ? category : null,
          configId: configId,
        ));
      } catch (e) {
        // Skip malformed entries
        continue;
      }
    }

    return channels;
  }
  
  /// Check if cache is valid for a given key
  bool _isCacheValid(String key) {
    if (!_cache.containsKey(key)) {
      return false;
    }
    
    final timestamp = _cacheTimestamps[key];
    if (timestamp == null) {
      return false;
    }
    
    final now = DateTime.now();
    return now.difference(timestamp) < _cacheDuration;
  }
  
  /// Update cache with new data
  void _updateCache(String key, List<Channel> channels) {
    _cache[key] = channels;
    _cacheTimestamps[key] = DateTime.now();
  }
  
  /// Clear cache for a specific key
  void clearCache(String key) {
    _cache.remove(key);
    _cacheTimestamps.remove(key);
    _accountInfoCache.remove(key);
  }
  
  /// Clear all cache
  void clearAllCache() {
    _cache.clear();
    _cacheTimestamps.clear();
    _accountInfoCache.clear();
  }
}
