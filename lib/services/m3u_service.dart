import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';
import '../models/channel.dart';
import '../utils/error_handler.dart';

/// Helper class for isolate parsing
class _M3UParseParams {
  final String content;
  final String configId;
  _M3UParseParams(this.content, this.configId);
}

class M3UService {
  final Dio _dio;
  final Uuid _uuid;
  
  // Pre-compiled RegEx patterns for performance
  static final _logoRegExp = RegExp(r'tvg-logo="([^"]*)"', caseSensitive: false);
  static final _groupRegExp = RegExp(r'group-title="([^"]*)"', caseSensitive: false);
  
  // Cache for parsed M3U data
  final Map<String, List<Channel>> _cache = {};
  final Map<String, DateTime> _cacheTimestamps = {};
  
  // Cache duration: 5 minutes
  static const Duration _cacheDuration = Duration(minutes: 5);

  M3UService({Dio? dio, Uuid? uuid})
      : _dio = dio ?? Dio(),
        _uuid = uuid ?? const Uuid();

  /// Parse a local M3U/M3U8 file and extract channels
  Future<List<Channel>> parseLocalFile(String filePath, String configId, {bool forceRefresh = false}) async {
    final cacheKey = 'local:$filePath:$configId';
    if (!forceRefresh && _isCacheValid(cacheKey)) {
      return _cache[cacheKey]!;
    }
    
    try {
      final file = File(filePath);
      if (!await file.exists()) {
        throw Exception('File not found: $filePath');
      }
      
      final content = await file.readAsString();
      // Use compute to run parsing in a background isolate to avoid UI jank
      final channels = await compute(_parseM3UIsolate, _M3UParseParams(content, configId));
      
      _updateCache(cacheKey, channels);
      return channels;
    } catch (e) {
      debugPrint('Error parsing local M3U: $e');
      rethrow;
    }
  }

  /// Fetch and parse a remote M3U/M3U8 file
  Future<List<Channel>> parseNetworkFile(String url, String configId, {bool forceRefresh = false}) async {
    final cacheKey = 'network:$url:$configId';
    if (!forceRefresh && _isCacheValid(cacheKey)) {
      return _cache[cacheKey]!;
    }
    
    return ErrorHandler.executeWithRetry(() async {
      final response = await _dio.get(
        url,
        options: Options(
          responseType: ResponseType.plain,
        ),
      );
      
      final content = response.data as String;
      // Use compute to run parsing in a background isolate
      final channels = await compute(_parseM3UIsolate, _M3UParseParams(content, configId));
      
      _updateCache(cacheKey, channels);
      return channels;
    });
  }

  /// Top-level function for compute()
  static List<Channel> _parseM3UIsolate(_M3UParseParams params) {
    const uuid = Uuid();
    // Namespace for UUID v5 generation (random but constant)
    const String namespace = '6ba7b810-9dad-11d1-80b4-00c04fd430c8'; 
    final content = params.content;
    final configId = params.configId;
    
    final lines = content.split('\n');
    if (lines.isEmpty || !lines[0].trim().startsWith('#EXTM3U')) {
      throw Exception('Invalid M3U format: Missing #EXTM3U header');
    }
    
    final channels = <Channel>[];
    String? currentExtinf;
    
    for (int i = 1; i < lines.length; i++) {
      final line = lines[i].trim();
      if (line.isEmpty) continue;
      
      if (line.startsWith('#EXTINF:')) {
        currentExtinf = line;
      } else if (!line.startsWith('#') && currentExtinf != null) {
        final commaIndex = currentExtinf.lastIndexOf(',');
        if (commaIndex != -1) {
          final attributesPart = currentExtinf.substring(0, commaIndex);
          final channelName = currentExtinf.substring(commaIndex + 1).trim();
          
          if (channelName.isNotEmpty) {
            final logoMatch = _logoRegExp.firstMatch(attributesPart);
            final groupMatch = _groupRegExp.firstMatch(attributesPart);
            
            // Use UUID v5 to generate a stable ID based on configId, name, and URL
            final stableId = uuid.v5(namespace, '$configId:$channelName:$line');
            
            channels.add(Channel(
              id: stableId,
              name: channelName,
              streamUrl: line,
              logoUrl: logoMatch?.group(1),
              category: groupMatch?.group(1),
              configId: configId,
            ));
          }
        }
        currentExtinf = null;
      }
    }
    return channels;
  }


  /// Export a list of channels to M3U format
  String exportToM3U(List<Channel> channels) {
    final buffer = StringBuffer();
    buffer.writeln('#EXTM3U');
    
    for (final channel in channels) {
      // Build EXTINF line with metadata
      buffer.write('#EXTINF:-1');
      
      // Add tvg-name attribute
      buffer.write(' tvg-name="${_escapeAttribute(channel.name)}"');
      
      // Add tvg-logo attribute if available
      if (channel.logoUrl != null && channel.logoUrl!.isNotEmpty) {
        buffer.write(' tvg-logo="${_escapeAttribute(channel.logoUrl!)}"');
      }
      
      // Add group-title attribute if category is available
      if (channel.category != null && channel.category!.isNotEmpty) {
        buffer.write(' group-title="${_escapeAttribute(channel.category!)}"');
      }
      
      // Add channel name at the end of EXTINF line
      buffer.writeln(',${channel.name}');
      
      // Add stream URL
      buffer.writeln(channel.streamUrl);
    }
    
    return buffer.toString();
  }

  /// Escape special characters in attribute values
  String _escapeAttribute(String value) {
    return value.replaceAll('"', '&quot;').replaceAll('\n', ' ').replaceAll('\r', '');
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
