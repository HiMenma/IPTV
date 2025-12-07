import 'package:flutter_test/flutter_test.dart';
import 'package:iptv_player/utils/validators.dart';

void main() {
  group('Validators - Configuration Name', () {
    test('valid configuration name returns null', () {
      expect(Validators.validateConfigurationName('My IPTV'), null);
      expect(Validators.validateConfigurationName('Test Config 123'), null);
      expect(Validators.validateConfigurationName('A'), null);
    });

    test('empty configuration name returns error', () {
      expect(Validators.validateConfigurationName(''), isNotNull);
      expect(Validators.validateConfigurationName('   '), isNotNull);
      expect(Validators.validateConfigurationName(null), isNotNull);
    });

    test('configuration name too long returns error', () {
      final longName = 'A' * 101;
      expect(Validators.validateConfigurationName(longName), isNotNull);
    });

    test('configuration name at max length is valid', () {
      final maxName = 'A' * 100;
      expect(Validators.validateConfigurationName(maxName), null);
    });
  });

  group('Validators - URL', () {
    test('valid HTTP URL returns null', () {
      expect(Validators.validateUrl('http://example.com'), null);
      expect(Validators.validateUrl('http://example.com:8080'), null);
      expect(Validators.validateUrl('http://example.com/path'), null);
    });

    test('valid HTTPS URL returns null', () {
      expect(Validators.validateUrl('https://example.com'), null);
      expect(Validators.validateUrl('https://example.com:443'), null);
      expect(Validators.validateUrl('https://example.com/path/to/file'), null);
    });

    test('empty URL returns error', () {
      expect(Validators.validateUrl(''), isNotNull);
      expect(Validators.validateUrl('   '), isNotNull);
      expect(Validators.validateUrl(null), isNotNull);
    });

    test('URL without protocol returns error', () {
      expect(Validators.validateUrl('example.com'), isNotNull);
      expect(Validators.validateUrl('www.example.com'), isNotNull);
    });

    test('URL with invalid protocol returns error', () {
      expect(Validators.validateUrl('ftp://example.com'), isNotNull);
      expect(Validators.validateUrl('file://path/to/file'), isNotNull);
    });

    test('malformed URL returns error', () {
      expect(Validators.validateUrl('http://'), isNotNull);
      expect(Validators.validateUrl('https://'), isNotNull);
      expect(Validators.validateUrl('not a url'), isNotNull);
    });
  });

  group('Validators - Xtream Credentials', () {
    test('valid server URL returns null', () {
      expect(Validators.validateXtreamServerUrl('http://xtream.server.com'), null);
      expect(Validators.validateXtreamServerUrl('https://xtream.server.com:8080'), null);
    });

    test('invalid server URL returns error', () {
      expect(Validators.validateXtreamServerUrl(''), isNotNull);
      expect(Validators.validateXtreamServerUrl('not-a-url'), isNotNull);
      expect(Validators.validateXtreamServerUrl(null), isNotNull);
    });

    test('valid username returns null', () {
      expect(Validators.validateXtreamUsername('user123'), null);
      expect(Validators.validateXtreamUsername('test@user'), null);
    });

    test('empty username returns error', () {
      expect(Validators.validateXtreamUsername(''), isNotNull);
      expect(Validators.validateXtreamUsername('   '), isNotNull);
      expect(Validators.validateXtreamUsername(null), isNotNull);
    });

    test('valid password returns null', () {
      expect(Validators.validateXtreamPassword('password123'), null);
      expect(Validators.validateXtreamPassword('P@ssw0rd!'), null);
    });

    test('empty password returns error', () {
      expect(Validators.validateXtreamPassword(''), isNotNull);
      expect(Validators.validateXtreamPassword('   '), isNotNull);
      expect(Validators.validateXtreamPassword(null), isNotNull);
    });

    test('validateXtreamCredentials returns empty map for valid credentials', () {
      final errors = Validators.validateXtreamCredentials(
        serverUrl: 'http://example.com',
        username: 'user',
        password: 'pass',
      );
      expect(errors, isEmpty);
    });

    test('validateXtreamCredentials returns errors for invalid credentials', () {
      final errors = Validators.validateXtreamCredentials(
        serverUrl: '',
        username: '',
        password: '',
      );
      expect(errors, isNotEmpty);
      expect(errors.containsKey('serverUrl'), true);
      expect(errors.containsKey('username'), true);
      expect(errors.containsKey('password'), true);
    });

    test('validateXtreamCredentials returns partial errors', () {
      final errors = Validators.validateXtreamCredentials(
        serverUrl: 'http://example.com',
        username: '',
        password: 'pass',
      );
      expect(errors.length, 1);
      expect(errors.containsKey('username'), true);
    });
  });

  group('Validators - M3U', () {
    test('valid file path returns null', () {
      expect(Validators.validateFilePath('/path/to/file.m3u'), null);
      expect(Validators.validateFilePath('file.m3u8'), null);
    });

    test('empty file path returns error', () {
      expect(Validators.validateFilePath(''), isNotNull);
      expect(Validators.validateFilePath('   '), isNotNull);
      expect(Validators.validateFilePath(null), isNotNull);
    });

    test('valid M3U network URL returns null', () {
      expect(Validators.validateM3UNetworkUrl('http://example.com/playlist.m3u'), null);
      expect(Validators.validateM3UNetworkUrl('https://example.com/playlist.m3u8'), null);
      expect(Validators.validateM3UNetworkUrl('http://example.com/stream'), null);
    });

    test('invalid M3U network URL returns error', () {
      expect(Validators.validateM3UNetworkUrl(''), isNotNull);
      expect(Validators.validateM3UNetworkUrl('not-a-url'), isNotNull);
      expect(Validators.validateM3UNetworkUrl(null), isNotNull);
    });
  });
}
