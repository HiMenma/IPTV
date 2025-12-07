import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/repositories/configuration_repository.dart';
import 'package:iptv_player/viewmodels/configuration_viewmodel.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Configuration Creation Flow Integration Tests', () {
    late ConfigurationRepository repository;
    late ConfigurationViewModel viewModel;

    setUp(() async {
      // Clear shared preferences before each test
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();

      repository = ConfigurationRepository();
      viewModel = ConfigurationViewModel(configRepository: repository);
    });

    tearDown(() async {
      // Clean up after each test
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();
    });

    test('Complete configuration creation flow - Xtream', () async {
      // Step 1: Start with empty configurations
      await viewModel.loadConfigurations();
      expect(viewModel.configurations.length, 0);

      // Step 2: Create a new Xtream configuration
      const configName = 'Test Xtream Config';
      final credentials = {
        'serverUrl': 'http://test.example.com',
        'username': 'testuser',
        'password': 'testpass',
      };

      await viewModel.createConfiguration(
        configName,
        ConfigType.xtream,
        credentials,
      );

      // Step 3: Verify configuration is in ViewModel
      expect(viewModel.configurations.length, 1);
      expect(viewModel.configurations[0].name, configName);
      expect(viewModel.configurations[0].type, ConfigType.xtream);
      expect(viewModel.configurations[0].credentials, credentials);

      // Step 4: Verify configuration is persisted in storage
      final savedConfigs = await repository.getAll();
      expect(savedConfigs.length, 1);
      expect(savedConfigs[0].name, configName);
      expect(savedConfigs[0].type, ConfigType.xtream);
      expect(savedConfigs[0].credentials, credentials);

      // Step 5: Verify configuration can be retrieved by ID
      final configId = viewModel.configurations[0].id;
      final retrievedConfig = await repository.getById(configId);
      expect(retrievedConfig, isNotNull);
      expect(retrievedConfig!.id, configId);
      expect(retrievedConfig.name, configName);
    });

    test('Complete configuration creation flow - M3U Local', () async {
      // Step 1: Start with empty configurations
      await viewModel.loadConfigurations();
      expect(viewModel.configurations.length, 0);

      // Step 2: Create a new M3U Local configuration
      const configName = 'Test M3U Local Config';
      final credentials = {
        'filePath': '/path/to/playlist.m3u',
      };

      await viewModel.createConfiguration(
        configName,
        ConfigType.m3uLocal,
        credentials,
      );

      // Step 3: Verify configuration is in ViewModel
      expect(viewModel.configurations.length, 1);
      expect(viewModel.configurations[0].name, configName);
      expect(viewModel.configurations[0].type, ConfigType.m3uLocal);
      expect(viewModel.configurations[0].credentials, credentials);

      // Step 4: Verify configuration is persisted in storage
      final savedConfigs = await repository.getAll();
      expect(savedConfigs.length, 1);
      expect(savedConfigs[0].name, configName);
      expect(savedConfigs[0].type, ConfigType.m3uLocal);
    });

    test('Complete configuration creation flow - M3U Network', () async {
      // Step 1: Start with empty configurations
      await viewModel.loadConfigurations();
      expect(viewModel.configurations.length, 0);

      // Step 2: Create a new M3U Network configuration
      const configName = 'Test M3U Network Config';
      final credentials = {
        'url': 'http://example.com/playlist.m3u',
      };

      await viewModel.createConfiguration(
        configName,
        ConfigType.m3uNetwork,
        credentials,
      );

      // Step 3: Verify configuration is in ViewModel
      expect(viewModel.configurations.length, 1);
      expect(viewModel.configurations[0].name, configName);
      expect(viewModel.configurations[0].type, ConfigType.m3uNetwork);
      expect(viewModel.configurations[0].credentials, credentials);

      // Step 4: Verify configuration is persisted in storage
      final savedConfigs = await repository.getAll();
      expect(savedConfigs.length, 1);
      expect(savedConfigs[0].name, configName);
      expect(savedConfigs[0].type, ConfigType.m3uNetwork);
    });

    test('Configuration creation persists across ViewModel instances', () async {
      // Step 1: Create configuration with first ViewModel instance
      const configName = 'Persistent Config';
      final credentials = {
        'serverUrl': 'http://test.example.com',
        'username': 'testuser',
        'password': 'testpass',
      };

      await viewModel.createConfiguration(
        configName,
        ConfigType.xtream,
        credentials,
      );

      expect(viewModel.configurations.length, 1);

      // Step 2: Create a new ViewModel instance (simulating app restart)
      final newViewModel = ConfigurationViewModel(configRepository: repository);
      await newViewModel.loadConfigurations();

      // Step 3: Verify configuration is still available
      expect(newViewModel.configurations.length, 1);
      expect(newViewModel.configurations[0].name, configName);
      expect(newViewModel.configurations[0].type, ConfigType.xtream);
      expect(newViewModel.configurations[0].credentials, credentials);
    });

    test('Multiple configurations can be created and persisted', () async {
      // Create multiple configurations
      await viewModel.createConfiguration(
        'Config 1',
        ConfigType.xtream,
        {'serverUrl': 'http://test1.com', 'username': 'user1', 'password': 'pass1'},
      );

      await viewModel.createConfiguration(
        'Config 2',
        ConfigType.m3uLocal,
        {'filePath': '/path/to/file.m3u'},
      );

      await viewModel.createConfiguration(
        'Config 3',
        ConfigType.m3uNetwork,
        {'url': 'http://example.com/playlist.m3u'},
      );

      // Verify all configurations are in ViewModel
      expect(viewModel.configurations.length, 3);

      // Verify all configurations are persisted
      final savedConfigs = await repository.getAll();
      expect(savedConfigs.length, 3);

      // Verify each configuration type
      expect(savedConfigs.where((c) => c.type == ConfigType.xtream).length, 1);
      expect(savedConfigs.where((c) => c.type == ConfigType.m3uLocal).length, 1);
      expect(savedConfigs.where((c) => c.type == ConfigType.m3uNetwork).length, 1);
    });

    test('Configuration rename flow', () async {
      // Create a configuration
      const originalName = 'Original Name';
      await viewModel.createConfiguration(
        originalName,
        ConfigType.xtream,
        {'serverUrl': 'http://test.com', 'username': 'user', 'password': 'pass'},
      );

      final configId = viewModel.configurations[0].id;
      expect(viewModel.configurations[0].name, originalName);

      // Rename the configuration
      const newName = 'New Name';
      await viewModel.renameConfiguration(configId, newName);

      // Verify name changed in ViewModel
      expect(viewModel.configurations[0].name, newName);

      // Verify name changed in storage
      final savedConfig = await repository.getById(configId);
      expect(savedConfig!.name, newName);

      // Verify other fields remain unchanged
      expect(savedConfig.id, configId);
      expect(savedConfig.type, ConfigType.xtream);
    });

    test('Configuration deletion flow', () async {
      // Create a configuration
      await viewModel.createConfiguration(
        'Config to Delete',
        ConfigType.xtream,
        {'serverUrl': 'http://test.com', 'username': 'user', 'password': 'pass'},
      );

      final configId = viewModel.configurations[0].id;
      expect(viewModel.configurations.length, 1);

      // Delete the configuration
      await viewModel.deleteConfiguration(configId);

      // Verify configuration removed from ViewModel
      expect(viewModel.configurations.length, 0);

      // Verify configuration removed from storage
      final savedConfig = await repository.getById(configId);
      expect(savedConfig, isNull);

      // Verify repository returns empty list
      final allConfigs = await repository.getAll();
      expect(allConfigs.length, 0);
    });
  });
}
