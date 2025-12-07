import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';
import '../models/channel.dart';
import '../utils/error_handler.dart';

class XtreamService {
  final Dio _dio;
  final Uuid _uuid;
  
  // Cache for Xtream channel data
  final Map<String, List<Channel>> _cache = {};
  final Map<String, DateTime> _cacheTimestamps = {};
  
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
        channels = _parseChannels(data, configId, normalizedUrl, username, password);
      } else if (data is Map && data.containsKey('streams')) {
        channels = _parseChannels(data['streams'], configId, normalizedUrl, username, password);
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

      final response = await _dio.get(
        '$normalizedUrl/player_api.php',
        queryParameters: {
          'username': username,
          'password': password,
          'action': 'get_live_categories',
        },
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
        throw NetworkException(
          'Failed to fetch categories: HTTP ${response.statusCode}',
          type: NetworkErrorType.serverError,
        );
      }

      final data = response.data;
      final categories = <String>[];

      if (data is List) {
        for (final item in data) {
          if (item is Map && item.containsKey('category_name')) {
            final categoryName = item['category_name'];
            if (categoryName != null && categoryName.toString().isNotEmpty) {
              categories.add(categoryName.toString());
            }
          }
        }
      }

      return categories;
    });
  }

  /// Parse channel list from Xtream API response
  List<Channel> _parseChannels(
    dynamic channelsData,
    String configId,
    String serverUrl,
    String username,
    String password,
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
        final category = item['category_name']?.toString() ?? item['category']?.toString();

        channels.add(Channel(
          id: _uuid.v4(),
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
  }
  
  /// Clear all cache
  void clearAllCache() {
    _cache.clear();
    _cacheTimestamps.clear();
  }
}
