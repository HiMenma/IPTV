import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:faker/faker.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/models/channel.dart';
import 'package:iptv_player/models/favorite.dart';
import 'package:iptv_player/models/browse_history.dart';

void main() {
  final faker = Faker();

  group('Property 12: Data persistence round-trip preserves content', () {
    // Feature: iptv-player, Property 12: Data persistence round-trip preserves content
    // Validates: Requirements 4.4, 6.3, 7.2

    test('Configuration round-trip preserves all fields', () {
      // Run 100 iterations as specified in the design document
      for (int i = 0; i < 100; i++) {
        // Generate random configuration
        final configType = ConfigType.values[faker.randomGenerator.integer(ConfigType.values.length)];
        final credentials = <String, dynamic>{
          if (configType == ConfigType.xtream) ...{
            'serverUrl': faker.internet.httpsUrl(),
            'username': faker.internet.userName(),
            'password': faker.internet.password(),
          } else if (configType == ConfigType.m3uLocal) ...{
            'filePath': '/path/to/${faker.lorem.word()}.m3u',
          } else ...{
            'url': faker.internet.httpsUrl(),
          }
        };

        final original = Configuration(
          id: faker.guid.guid(),
          name: faker.lorem.words(2).join(' '),
          type: configType,
          credentials: credentials,
          createdAt: faker.date.dateTime(minYear: 2020, maxYear: 2025),
          lastRefreshed: faker.randomGenerator.boolean()
              ? faker.date.dateTime(minYear: 2020, maxYear: 2025)
              : null,
        );

        // Serialize to JSON
        final json = original.toJson();

        // Deserialize from JSON
        final restored = Configuration.fromJson(json);

        // Verify all fields are preserved
        expect(restored, equals(original),
            reason: 'Configuration round-trip should preserve all fields');
        expect(restored.id, equals(original.id));
        expect(restored.name, equals(original.name));
        expect(restored.type, equals(original.type));
        expect(restored.credentials, equals(original.credentials));
        expect(restored.createdAt, equals(original.createdAt));
        expect(restored.lastRefreshed, equals(original.lastRefreshed));
      }
    });

    test('Channel round-trip preserves all fields', () {
      for (int i = 0; i < 100; i++) {
        final original = Channel(
          id: faker.guid.guid(),
          name: faker.lorem.words(2).join(' '),
          streamUrl: faker.internet.httpsUrl(),
          logoUrl: faker.randomGenerator.boolean() ? faker.internet.httpsUrl() : null,
          category: faker.randomGenerator.boolean() ? faker.lorem.word() : null,
          configId: faker.guid.guid(),
        );

        final json = original.toJson();
        final restored = Channel.fromJson(json);

        expect(restored, equals(original),
            reason: 'Channel round-trip should preserve all fields');
        expect(restored.id, equals(original.id));
        expect(restored.name, equals(original.name));
        expect(restored.streamUrl, equals(original.streamUrl));
        expect(restored.logoUrl, equals(original.logoUrl));
        expect(restored.category, equals(original.category));
        expect(restored.configId, equals(original.configId));
      }
    });

    test('Favorite round-trip preserves all fields', () {
      for (int i = 0; i < 100; i++) {
        final original = Favorite(
          channelId: faker.guid.guid(),
          addedAt: faker.date.dateTime(minYear: 2020, maxYear: 2025),
        );

        final json = original.toJson();
        final restored = Favorite.fromJson(json);

        expect(restored, equals(original),
            reason: 'Favorite round-trip should preserve all fields');
        expect(restored.channelId, equals(original.channelId));
        expect(restored.addedAt, equals(original.addedAt));
      }
    });

    test('BrowseHistory round-trip preserves all fields', () {
      for (int i = 0; i < 100; i++) {
        final original = BrowseHistory(
          channelId: faker.guid.guid(),
          watchedAt: faker.date.dateTime(minYear: 2020, maxYear: 2025),
        );

        final json = original.toJson();
        final restored = BrowseHistory.fromJson(json);

        expect(restored, equals(original),
            reason: 'BrowseHistory round-trip should preserve all fields');
        expect(restored.channelId, equals(original.channelId));
        expect(restored.watchedAt, equals(original.watchedAt));
      }
    });

    test('Configuration with JSON string encoding round-trip', () {
      // Test with actual JSON string encoding/decoding
      for (int i = 0; i < 100; i++) {
        final configType = ConfigType.values[faker.randomGenerator.integer(ConfigType.values.length)];
        final credentials = <String, dynamic>{
          if (configType == ConfigType.xtream) ...{
            'serverUrl': faker.internet.httpsUrl(),
            'username': faker.internet.userName(),
            'password': faker.internet.password(),
          } else if (configType == ConfigType.m3uLocal) ...{
            'filePath': '/path/to/${faker.lorem.word()}.m3u',
          } else ...{
            'url': faker.internet.httpsUrl(),
          }
        };

        final original = Configuration(
          id: faker.guid.guid(),
          name: faker.lorem.words(2).join(' '),
          type: configType,
          credentials: credentials,
          createdAt: faker.date.dateTime(minYear: 2020, maxYear: 2025),
          lastRefreshed: faker.randomGenerator.boolean()
              ? faker.date.dateTime(minYear: 2020, maxYear: 2025)
              : null,
        );

        // Encode to JSON string (simulating storage)
        final jsonString = jsonEncode(original.toJson());

        // Decode from JSON string
        final jsonMap = jsonDecode(jsonString) as Map<String, dynamic>;
        final restored = Configuration.fromJson(jsonMap);

        expect(restored, equals(original),
            reason: 'Configuration JSON string round-trip should preserve all fields');
      }
    });
  });
}
