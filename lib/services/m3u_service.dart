import 'dart:io';
import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';
import '../models/channel.dart';
import '../utils/error_handler.dart';

class M3UService {
  final Dio _dio;
  final Uuid _uuid;
  
  // Cache for parsed M3U data
  final Map<String, List<Channel>> _cache = {};
  final Map<String, DateTime> _cacheTimestamps = {};
  
  // Cache duration: 5 minutes
  static const Duration _cacheDuration = Duration(minutes: 5);

  M3UService({Dio? dio, Uuid? uuid})
      : _dio = dio ?? Dio(),
        _uuid = uuid ?? const Uuid();

  /// Parse a local M3U/M3U8 file and extract channels
  /// Requirements: 3.5
  Future<List<Channel>> parseLocalFile(String filePath, String configId, {bool forceRefresh = false}) async {
    // Check cache first
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
      final channels = parseM3UContent(content, configId);
      
      // Cache the result
      _updateCache(cacheKey, channels);
      
      return channels;
    } on FormatException catch (e) {
      throw Exception('Invalid M3U file format: ${e.message}');
    } on FileSystemException catch (e) {
      throw Exception('Failed to read M3U file: ${e.message}');
    } catch (e) {
      if (e.toString().contains('Invalid M3U format')) {
        rethrow;
      }
      throw Exception('Failed to parse local M3U file: $e');
    }
  }

  /// Fetch and parse a remote M3U/M3U8 file
  /// Requirements: 3.5
  Future<List<Channel>> parseNetworkFile(String url, String configId, {bool forceRefresh = false}) async {
    // Check cache first
    final cacheKey = 'network:$url:$configId';
    if (!forceRefresh && _isCacheValid(cacheKey)) {
      return _cache[cacheKey]!;
    }
    
    return ErrorHandler.executeWithRetry(() async {
      final response = await _dio.get(
        url,
        options: Options(
          responseType: ResponseType.plain,
          followRedirects: true,
        ),
      ).timeout(
        const Duration(seconds: 30),
        onTimeout: () {
          throw DioException(
            requestOptions: RequestOptions(path: url),
            type: DioExceptionType.connectionTimeout,
          );
        },
      );
      
      if (response.statusCode != 200) {
        throw NetworkException(
          'Failed to fetch M3U file: HTTP ${response.statusCode}',
          type: NetworkErrorType.serverError,
        );
      }
      
      final content = response.data as String;
      try {
        final channels = parseM3UContent(content, configId);
        
        // Cache the result
        _updateCache(cacheKey, channels);
        
        return channels;
      } catch (e) {
        if (e.toString().contains('Invalid M3U format')) {
          throw Exception('Invalid M3U file format: ${e.toString()}');
        }
        rethrow;
      }
    });
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

  /// Parse M3U content and extract channels
  /// This method is visible for testing purposes
  List<Channel> parseM3UContent(String content, String configId) {
    final lines = content.split('\n').map((line) => line.trim()).toList();
    
    // Check for M3U header
    if (lines.isEmpty || !lines[0].startsWith('#EXTM3U')) {
      throw Exception('Invalid M3U format: Missing #EXTM3U header');
    }
    
    final channels = <Channel>[];
    String? currentExtinf;
    
    for (int i = 1; i < lines.length; i++) {
      final line = lines[i];
      
      // Skip empty lines
      if (line.isEmpty) continue;
      
      // Parse EXTINF line
      if (line.startsWith('#EXTINF:')) {
        currentExtinf = line;
      }
      // Parse stream URL (non-comment line after EXTINF)
      else if (!line.startsWith('#') && currentExtinf != null) {
        final channel = _parseExtinfLine(currentExtinf, line, configId);
        if (channel != null) {
          channels.add(channel);
        }
        currentExtinf = null;
      }
    }
    
    return channels;
  }

  /// Parse an EXTINF line and create a Channel object
  Channel? _parseExtinfLine(String extinfLine, String streamUrl, String configId) {
    try {
      // Extract metadata from EXTINF line
      // Format: #EXTINF:duration tvg-name="..." tvg-logo="..." group-title="...",Channel Name
      
      // Find the comma that separates attributes from channel name
      final commaIndex = extinfLine.lastIndexOf(',');
      if (commaIndex == -1) {
        return null;
      }
      
      final attributesPart = extinfLine.substring(0, commaIndex);
      final channelName = extinfLine.substring(commaIndex + 1).trim();
      
      if (channelName.isEmpty) {
        return null;
      }
      
      // Extract attributes
      String? logoUrl = _extractAttribute(attributesPart, 'tvg-logo');
      String? category = _extractAttribute(attributesPart, 'group-title');
      
      return Channel(
        id: _uuid.v4(),
        name: channelName,
        streamUrl: streamUrl,
        logoUrl: logoUrl,
        category: category,
        configId: configId,
      );
    } catch (e) {
      // Skip malformed entries
      return null;
    }
  }

  /// Extract an attribute value from the EXTINF line
  String? _extractAttribute(String line, String attributeName) {
    final pattern = RegExp('$attributeName="([^"]*)"');
    final match = pattern.firstMatch(line);
    if (match != null && match.groupCount >= 1) {
      final value = match.group(1);
      return (value != null && value.isNotEmpty) ? value : null;
    }
    return null;
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
