import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:faker/faker.dart';
import 'package:iptv_player/services/m3u_service.dart';
import 'package:iptv_player/models/channel.dart';
import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

void main() {
  late M3UService m3uService;
  late Faker faker;
  late String testConfigId;

  setUp(() {
    m3uService = M3UService();
    faker = Faker();
    testConfigId = const Uuid().v4();
  });

  group('M3U Parsing Properties', () {
    // Feature: iptv-player, Property 6: M3U/M3U8 parsing extracts all channels
    // Validates: Requirements 3.1, 3.2, 3.3, 3.4
    test('Property 6: M3U parsing extracts all channels with correct metadata',
        () async {
      // Run property test with multiple iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        // Generate random number of channels (1-50)
        final channelCount = faker.randomGenerator.integer(50, min: 1);
        final expectedChannels = <Map<String, String>>[];

        // Build M3U content with random channels
        final m3uContent = StringBuffer();
        m3uContent.writeln('#EXTM3U');

        for (int i = 0; i < channelCount; i++) {
          final channelName = faker.lorem.words(2).join(' ');
          final streamUrl = faker.internet.httpsUrl();
          final logoUrl =
              faker.randomGenerator.boolean() ? faker.internet.httpsUrl() : null;
          final category = faker.randomGenerator.boolean()
              ? faker.lorem.word()
              : null;

          // Store expected data
          expectedChannels.add({
            'name': channelName,
            'streamUrl': streamUrl,
            if (logoUrl != null) 'logoUrl': logoUrl,
            if (category != null) 'category': category,
          });

          // Build EXTINF line
          m3uContent.write('#EXTINF:-1');
          m3uContent.write(' tvg-name="$channelName"');
          if (logoUrl != null) {
            m3uContent.write(' tvg-logo="$logoUrl"');
          }
          if (category != null) {
            m3uContent.write(' group-title="$category"');
          }
          m3uContent.writeln(',$channelName');
          m3uContent.writeln(streamUrl);
        }

        // Parse the M3U content
        final parsedChannels =
            m3uService.parseM3UContent(m3uContent.toString(), testConfigId);

        // Verify all channels were extracted
        expect(
          parsedChannels.length,
          equals(channelCount),
          reason:
              'Iteration $iteration: Should extract all $channelCount channels',
        );

        // Verify each channel has correct metadata
        for (int i = 0; i < channelCount; i++) {
          final parsed = parsedChannels[i];
          final expected = expectedChannels[i];

          expect(parsed.name, equals(expected['name']),
              reason: 'Iteration $iteration: Channel $i name should match');
          expect(parsed.streamUrl, equals(expected['streamUrl']),
              reason: 'Iteration $iteration: Channel $i streamUrl should match');
          expect(parsed.logoUrl, equals(expected['logoUrl']),
              reason: 'Iteration $iteration: Channel $i logoUrl should match');
          expect(parsed.category, equals(expected['category']),
              reason: 'Iteration $iteration: Channel $i category should match');
          expect(parsed.configId, equals(testConfigId),
              reason: 'Iteration $iteration: Channel $i configId should match');
        }
      }
    });

    test('Property 6: M3U parsing handles channels without optional metadata',
        () async {
      // Test channels with minimal metadata (no logo, no category)
      for (int iteration = 0; iteration < 100; iteration++) {
        final channelCount = faker.randomGenerator.integer(20, min: 1);
        final m3uContent = StringBuffer();
        m3uContent.writeln('#EXTM3U');

        final expectedNames = <String>[];
        final expectedUrls = <String>[];

        for (int i = 0; i < channelCount; i++) {
          final channelName = faker.lorem.words(2).join(' ');
          final streamUrl = faker.internet.httpsUrl();

          expectedNames.add(channelName);
          expectedUrls.add(streamUrl);

          // Minimal EXTINF line (no logo, no category)
          m3uContent.writeln('#EXTINF:-1,$channelName');
          m3uContent.writeln(streamUrl);
        }

        final parsedChannels =
            m3uService.parseM3UContent(m3uContent.toString(), testConfigId);

        expect(parsedChannels.length, equals(channelCount));

        for (int i = 0; i < channelCount; i++) {
          expect(parsedChannels[i].name, equals(expectedNames[i]));
          expect(parsedChannels[i].streamUrl, equals(expectedUrls[i]));
          expect(parsedChannels[i].logoUrl, isNull);
          expect(parsedChannels[i].category, isNull);
        }
      }
    });

    test('Property 6: M3U parsing handles empty lines and comments', () async {
      // Test that parser correctly handles M3U files with extra whitespace and comments
      for (int iteration = 0; iteration < 50; iteration++) {
        final channelCount = faker.randomGenerator.integer(10, min: 1);
        final m3uContent = StringBuffer();
        m3uContent.writeln('#EXTM3U');

        final expectedChannelCount = channelCount;

        for (int i = 0; i < channelCount; i++) {
          // Add random empty lines
          if (faker.randomGenerator.boolean()) {
            m3uContent.writeln('');
          }

          // Add random comments
          if (faker.randomGenerator.boolean()) {
            m3uContent.writeln('# ${faker.lorem.sentence()}');
          }

          final channelName = faker.lorem.words(2).join(' ');
          final streamUrl = faker.internet.httpsUrl();

          m3uContent.writeln('#EXTINF:-1,$channelName');
          m3uContent.writeln(streamUrl);

          // Add random empty lines after URL
          if (faker.randomGenerator.boolean()) {
            m3uContent.writeln('');
          }
        }

        final parsedChannels =
            m3uService.parseM3UContent(m3uContent.toString(), testConfigId);

        expect(
          parsedChannels.length,
          equals(expectedChannelCount),
          reason:
              'Iteration $iteration: Should extract exactly $expectedChannelCount channels despite extra whitespace and comments',
        );
      }
    });
  });

  group('M3U Export Properties', () {
    // Feature: iptv-player, Property 5: Xtream to M3U export round-trip preserves channels
    // Validates: Requirements 2.5
    test('Property 5: M3U export and re-import preserves channel data',
        () async {
      // Run property test with multiple iterations
      for (int iteration = 0; iteration < 100; iteration++) {
        // Generate random channels
        final channelCount = faker.randomGenerator.integer(50, min: 1);
        final originalChannels = <Channel>[];

        for (int i = 0; i < channelCount; i++) {
          final channel = Channel(
            id: const Uuid().v4(),
            name: faker.lorem.words(2).join(' '),
            streamUrl: faker.internet.httpsUrl(),
            logoUrl: faker.randomGenerator.boolean()
                ? faker.internet.httpsUrl()
                : null,
            category:
                faker.randomGenerator.boolean() ? faker.lorem.word() : null,
            configId: testConfigId,
          );
          originalChannels.add(channel);
        }

        // Export to M3U format
        final m3uContent = m3uService.exportToM3U(originalChannels);

        // Verify M3U header is present
        expect(
          m3uContent.startsWith('#EXTM3U'),
          isTrue,
          reason: 'Iteration $iteration: M3U content should start with #EXTM3U',
        );

        // Re-import the M3U content
        final reimportedChannels =
            m3uService.parseM3UContent(m3uContent, testConfigId);

        // Verify channel count is preserved
        expect(
          reimportedChannels.length,
          equals(originalChannels.length),
          reason:
              'Iteration $iteration: Should preserve all ${originalChannels.length} channels',
        );

        // Verify each channel's data is preserved (except ID which is regenerated)
        for (int i = 0; i < channelCount; i++) {
          final original = originalChannels[i];
          final reimported = reimportedChannels[i];

          expect(
            reimported.name,
            equals(original.name),
            reason: 'Iteration $iteration: Channel $i name should be preserved',
          );
          expect(
            reimported.streamUrl,
            equals(original.streamUrl),
            reason:
                'Iteration $iteration: Channel $i streamUrl should be preserved',
          );
          expect(
            reimported.logoUrl,
            equals(original.logoUrl),
            reason:
                'Iteration $iteration: Channel $i logoUrl should be preserved',
          );
          expect(
            reimported.category,
            equals(original.category),
            reason:
                'Iteration $iteration: Channel $i category should be preserved',
          );
          expect(
            reimported.configId,
            equals(testConfigId),
            reason:
                'Iteration $iteration: Channel $i configId should be preserved',
          );
        }
      }
    });

    test('Property 5: M3U export handles channels with various names',
        () async {
      // Test that various channel names are properly handled
      for (int iteration = 0; iteration < 50; iteration++) {
        final channelCount = faker.randomGenerator.integer(20, min: 1);
        final originalChannels = <Channel>[];

        for (int i = 0; i < channelCount; i++) {
          // Create channel names with safe characters (alphanumeric, spaces, hyphens)
          var channelName = faker.lorem.words(2).join(' ');

          // Randomly add safe special characters
          if (faker.randomGenerator.boolean()) {
            channelName = '$channelName - ${faker.lorem.word()}';
          }

          final channel = Channel(
            id: const Uuid().v4(),
            name: channelName,
            streamUrl: faker.internet.httpsUrl(),
            logoUrl: faker.randomGenerator.boolean()
                ? faker.internet.httpsUrl()
                : null,
            category:
                faker.randomGenerator.boolean() ? faker.lorem.word() : null,
            configId: testConfigId,
          );
          originalChannels.add(channel);
        }

        // Export and re-import
        final m3uContent = m3uService.exportToM3U(originalChannels);
        final reimportedChannels =
            m3uService.parseM3UContent(m3uContent, testConfigId);

        // Verify channel count
        expect(reimportedChannels.length, equals(originalChannels.length));

        // Verify all channel data is preserved
        for (int i = 0; i < channelCount; i++) {
          final original = originalChannels[i];
          final reimported = reimportedChannels[i];

          expect(
            reimported.name,
            equals(original.name),
            reason: 'Iteration $iteration: Channel $i name should be preserved',
          );
          expect(
            reimported.streamUrl,
            equals(original.streamUrl),
            reason:
                'Iteration $iteration: Channel $i streamUrl should be preserved',
          );
        }
      }
    });

    test('Property 5: M3U export preserves empty optional fields', () async {
      // Test that channels without logo or category are properly handled
      for (int iteration = 0; iteration < 100; iteration++) {
        final channelCount = faker.randomGenerator.integer(20, min: 1);
        final originalChannels = <Channel>[];

        for (int i = 0; i < channelCount; i++) {
          // Create channels with no logo and no category
          final channel = Channel(
            id: const Uuid().v4(),
            name: faker.lorem.words(2).join(' '),
            streamUrl: faker.internet.httpsUrl(),
            logoUrl: null,
            category: null,
            configId: testConfigId,
          );
          originalChannels.add(channel);
        }

        // Export and re-import
        final m3uContent = m3uService.exportToM3U(originalChannels);
        final reimportedChannels =
            m3uService.parseM3UContent(m3uContent, testConfigId);

        // Verify all data is preserved
        expect(reimportedChannels.length, equals(originalChannels.length));

        for (int i = 0; i < channelCount; i++) {
          final original = originalChannels[i];
          final reimported = reimportedChannels[i];

          expect(reimported.name, equals(original.name));
          expect(reimported.streamUrl, equals(original.streamUrl));
          expect(reimported.logoUrl, isNull);
          expect(reimported.category, isNull);
        }
      }
    });
  });

  group('M3U Error Handling Properties', () {
    // Feature: iptv-player, Property 7: Invalid M3U content produces error
    // Validates: Requirements 3.5
    test('Property 7: Invalid M3U content without header produces error',
        () async {
      // Test that M3U content without #EXTM3U header is rejected
      for (int iteration = 0; iteration < 100; iteration++) {
        // Generate invalid M3U content (missing header)
        final invalidContent = StringBuffer();

        // Add some channel-like content but without proper header
        final channelCount = faker.randomGenerator.integer(10, min: 1);
        for (int i = 0; i < channelCount; i++) {
          invalidContent.writeln('#EXTINF:-1,${faker.lorem.words(2).join(' ')}');
          invalidContent.writeln(faker.internet.httpsUrl());
        }

        // Attempt to parse should throw an exception
        expect(
          () => m3uService.parseM3UContent(
              invalidContent.toString(), testConfigId),
          throwsException,
          reason:
              'Iteration $iteration: Should reject M3U content without #EXTM3U header',
        );
      }
    });

    test('Property 7: Empty M3U content produces error', () async {
      // Test that empty content is rejected
      for (int iteration = 0; iteration < 50; iteration++) {
        final emptyContent = '';

        expect(
          () => m3uService.parseM3UContent(emptyContent, testConfigId),
          throwsException,
          reason: 'Iteration $iteration: Should reject empty M3U content',
        );
      }
    });

    test('Property 7: M3U with only header returns empty channel list',
        () async {
      // Test that M3U with only header (no channels) returns empty list
      for (int iteration = 0; iteration < 50; iteration++) {
        final headerOnlyContent = '#EXTM3U\n';

        final channels =
            m3uService.parseM3UContent(headerOnlyContent, testConfigId);

        expect(
          channels,
          isEmpty,
          reason:
              'Iteration $iteration: M3U with only header should return empty channel list',
        );
      }
    });

    test('Property 7: M3U with malformed entries skips invalid channels',
        () async {
      // Test that parser skips malformed entries but continues parsing valid ones
      for (int iteration = 0; iteration < 50; iteration++) {
        final mixedContent = StringBuffer();
        mixedContent.writeln('#EXTM3U');

        // Add some valid channels
        final validChannelCount = faker.randomGenerator.integer(5, min: 1);
        for (int i = 0; i < validChannelCount; i++) {
          mixedContent
              .writeln('#EXTINF:-1,${faker.lorem.words(2).join(' ')}');
          mixedContent.writeln(faker.internet.httpsUrl());
        }

        // Add some malformed entries (EXTINF without channel name)
        mixedContent.writeln('#EXTINF:-1,');
        mixedContent.writeln(faker.internet.httpsUrl());

        // Add more valid channels
        for (int i = 0; i < validChannelCount; i++) {
          mixedContent
              .writeln('#EXTINF:-1,${faker.lorem.words(2).join(' ')}');
          mixedContent.writeln(faker.internet.httpsUrl());
        }

        final channels =
            m3uService.parseM3UContent(mixedContent.toString(), testConfigId);

        // Should parse only valid channels (skip the malformed one)
        expect(
          channels.length,
          equals(validChannelCount * 2),
          reason:
              'Iteration $iteration: Should parse ${validChannelCount * 2} valid channels and skip malformed entries',
        );
      }
    });

    test('Property 7: M3U with EXTINF but no URL skips incomplete entries',
        () async {
      // Test that entries with EXTINF but no following URL are skipped
      for (int iteration = 0; iteration < 50; iteration++) {
        final incompleteContent = StringBuffer();
        incompleteContent.writeln('#EXTM3U');

        // Add valid channel
        incompleteContent.writeln('#EXTINF:-1,Valid Channel');
        incompleteContent.writeln(faker.internet.httpsUrl());

        // Add EXTINF without URL (incomplete entry)
        incompleteContent.writeln('#EXTINF:-1,Incomplete Channel');
        // No URL follows

        // Add another valid channel
        incompleteContent.writeln('#EXTINF:-1,Another Valid Channel');
        incompleteContent.writeln(faker.internet.httpsUrl());

        final channels = m3uService.parseM3UContent(
            incompleteContent.toString(), testConfigId);

        // Should parse only the 2 complete channels
        expect(
          channels.length,
          equals(2),
          reason:
              'Iteration $iteration: Should parse only complete channel entries',
        );
      }
    });
  });
}
