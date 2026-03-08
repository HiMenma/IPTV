import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';
import '../models/channel.dart';
import '../utils/error_handler.dart';

/// Xtream account information
class XtreamAccountInfo {
  final String username;
  final String status;
  final DateTime? expiryDate;
  final String? serverUrl;

  XtreamAccountInfo({
    required this.username,
    required this.status,
    this.expiryDate,
    this.serverUrl,
  });
}

class XtreamService {
  final Dio _dio;
  final Uuid _uuid = const Uuid();
  
  final Map<String, List<Channel>> _cache = {};
  final Map<String, DateTime> _cacheTimestamps = {};
  final Map<String, XtreamAccountInfo> _accountInfoCache = {};
  
  static const Duration _cacheDuration = Duration(minutes: 10);

  XtreamService({Dio? dio}) : _dio = dio ?? Dio();

  /// Get account information from Xtream server
  Future<XtreamAccountInfo> getAccountInfo(String serverUrl, String username, String password) async {
    final cacheKey = '$serverUrl:$username';
    if (_accountInfoCache.containsKey(cacheKey) && _isCacheValid(cacheKey)) {
      return _accountInfoCache[cacheKey]!;
    }

    return ErrorHandler.executeWithRetry(() async {
      final response = await _dio.get(
        '$serverUrl/player_api.php',
        queryParameters: {
          'username': username,
          'password': password,
        },
      );

      final data = response.data;
      if (data == null || data is! Map) {
        throw NetworkException('Invalid server response', type: NetworkErrorType.serverError);
      }

      final userInfo = data['user_info'];
      if (userInfo == null || userInfo['auth'] == 0) {
        throw NetworkException('Authentication failed', type: NetworkErrorType.unauthorized);
      }

      final expiryTimestamp = userInfo['exp_date'];
      DateTime? expiryDate;
      if (expiryTimestamp != null && expiryTimestamp is String) {
        expiryDate = DateTime.fromMillisecondsSinceEpoch(int.parse(expiryTimestamp) * 1000);
      } else if (expiryTimestamp != null && expiryTimestamp is int) {
        expiryDate = DateTime.fromMillisecondsSinceEpoch(expiryTimestamp * 1000);
      }

      final info = XtreamAccountInfo(
        username: userInfo['username'] ?? username,
        status: userInfo['status'] ?? 'Unknown',
        expiryDate: expiryDate,
        serverUrl: serverUrl,
      );

      _accountInfoCache[cacheKey] = info;
      return info;
    });
  }

  /// Get all live channels from Xtream server
  Future<List<Channel>> getChannels(String serverUrl, String username, String password, String configId, {bool forceRefresh = false}) async {
    final cacheKey = 'channels:$configId';
    if (!forceRefresh && _cache.containsKey(cacheKey) && _isCacheValid(cacheKey)) {
      return _cache[cacheKey]!;
    }

    return ErrorHandler.executeWithRetry(() async {
      // 1. Get Categories
      final categoryResponse = await _dio.get(
        '$serverUrl/player_api.php',
        queryParameters: {
          'username': username,
          'password': password,
          'action': 'get_live_categories',
        },
      );

      final Map<String, String> categoryMap = {};
      if (categoryResponse.data is List) {
        for (var item in categoryResponse.data) {
          categoryMap[item['category_id'].toString()] = item['category_name'].toString();
        }
      }

      // 2. Get Channels
      final response = await _dio.get(
        '$serverUrl/player_api.php',
        queryParameters: {
          'username': username,
          'password': password,
          'action': 'get_live_streams',
        },
      );

      if (response.data is! List) {
        throw NetworkException('Failed to fetch channels', type: NetworkErrorType.serverError);
      }

      final channels = <Channel>[];
      const String namespace = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

      for (var item in response.data) {
        final name = item['name']?.toString() ?? 'Unknown Channel';
        final streamId = item['stream_id']?.toString();
        if (streamId == null) continue;

        final streamUrl = '$serverUrl/live/$username/$password/$streamId.m3u8';
        final logoUrl = item['stream_icon']?.toString() ?? item['logo']?.toString();
        
        String? category;
        final categoryId = item['category_id']?.toString();
        if (categoryId != null && categoryMap.containsKey(categoryId)) {
          category = categoryMap[categoryId];
        }

        final stableId = _uuid.v5(namespace, '$configId:$streamId');

        channels.add(Channel(
          id: stableId,
          name: name,
          streamUrl: streamUrl,
          logoUrl: logoUrl?.isNotEmpty == true ? logoUrl : null,
          category: category?.isNotEmpty == true ? category : null,
          configId: configId,
        ));
      }

      _cache[cacheKey] = channels;
      _cacheTimestamps[cacheKey] = DateTime.now();
      return channels;
    });
  }

  bool _isCacheValid(String key) {
    if (!_cacheTimestamps.containsKey(key)) return false;
    return DateTime.now().difference(_cacheTimestamps[key]!) < _cacheDuration;
  }

  void clearCache(String configId) {
    _cache.remove('channels:$configId');
    _cacheTimestamps.remove('channels:$configId');
  }
}
