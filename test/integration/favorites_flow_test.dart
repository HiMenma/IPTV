import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/models/channel.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/repositories/favorite_repository.dart';
import 'package:iptv_player/repositories/configuration_repository.dart';
import 'package:iptv_player/viewmodels/channel_viewmodel.dart';
import 'package:iptv_player/services/m3u_service.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Favorites Flow Integration Tests', () {
    late FavoriteRepository favoriteRepository;
    late ConfigurationRepository configRepository;
    late M3UService m3uService;
    late ChannelViewModel viewModel;

    setUp(() async {
      // Clear shared preferences before each test
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();

      favoriteRepository = FavoriteRepository();
      configRepository = ConfigurationRepository();
      m3uService = M3UService();
      viewModel = ChannelViewModel(
        favoriteRepository: favoriteRepository,
        configRepository: configRepository,
        m3uService: m3uService,
      );
    });

    tearDown(() async {
      // Clean up after each test
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();
    });

    test('Complete favorites flow - add and remove', () async {
      // Step 1: Verify no favorites initially
      await viewModel.loadFavorites();
      expect(viewModel.favorites.length, 0);

      // Step 2: Add a channel to favorites
      const channelId = 'test-channel-1';
      await viewModel.toggleFavorite(channelId);

      // Step 3: Verify favorite was added in repository
      final isFavorite = await favoriteRepository.isFavorite(channelId);
      expect(isFavorite, true);

      // Step 4: Remove the channel from favorites
      await viewModel.toggleFavorite(channelId);

      // Step 5: Verify favorite was removed
      final isStillFavorite = await favoriteRepository.isFavorite(channelId);
      expect(isStillFavorite, false);
    });

    test('Favorites persist across ViewModel instances', () async {
      // Step 1: Add favorites with first ViewModel instance
      const channelId1 = 'test-channel-persist-1';
      const channelId2 = 'test-channel-persist-2';

      await viewModel.toggleFavorite(channelId1);
      await viewModel.toggleFavorite(channelId2);

      // Verify favorites were added
      expect(await favoriteRepository.isFavorite(channelId1), true);
      expect(await favoriteRepository.isFavorite(channelId2), true);

      // Step 2: Create a new ViewModel instance (simulating app restart)
      final newViewModel = ChannelViewModel(
        favoriteRepository: favoriteRepository,
        configRepository: configRepository,
        m3uService: m3uService,
      );

      // Step 3: Verify favorites are still available
      expect(await favoriteRepository.isFavorite(channelId1), true);
      expect(await favoriteRepository.isFavorite(channelId2), true);

      // Step 4: Remove a favorite with new ViewModel
      await newViewModel.toggleFavorite(channelId1);

      // Step 5: Verify removal persisted
      expect(await favoriteRepository.isFavorite(channelId1), false);
      expect(await favoriteRepository.isFavorite(channelId2), true);
    });

    test('Multiple favorites can be added and removed', () async {
      // Add multiple favorites
      const channelIds = [
        'channel-1',
        'channel-2',
        'channel-3',
        'channel-4',
        'channel-5',
      ];

      for (final channelId in channelIds) {
        await viewModel.toggleFavorite(channelId);
      }

      // Verify all were added
      for (final channelId in channelIds) {
        expect(await favoriteRepository.isFavorite(channelId), true);
      }

      // Remove some favorites
      await viewModel.toggleFavorite(channelIds[1]);
      await viewModel.toggleFavorite(channelIds[3]);

      // Verify correct favorites remain
      expect(await favoriteRepository.isFavorite(channelIds[0]), true);
      expect(await favoriteRepository.isFavorite(channelIds[1]), false);
      expect(await favoriteRepository.isFavorite(channelIds[2]), true);
      expect(await favoriteRepository.isFavorite(channelIds[3]), false);
      expect(await favoriteRepository.isFavorite(channelIds[4]), true);
    });

    test('Favorites persist across app restart simulation', () async {
      // Step 1: Add favorites
      const channelId1 = 'restart-test-1';
      const channelId2 = 'restart-test-2';
      const channelId3 = 'restart-test-3';

      await favoriteRepository.add(channelId1);
      await favoriteRepository.add(channelId2);
      await favoriteRepository.add(channelId3);

      // Step 2: Verify favorites exist
      final favorites1 = await favoriteRepository.getAll();
      expect(favorites1.length, 3);

      // Step 3: Simulate app restart by creating new repository instance
      final newFavoriteRepository = FavoriteRepository();
      final favorites2 = await newFavoriteRepository.getAll();

      // Step 4: Verify favorites persisted
      expect(favorites2.length, 3);
      expect(favorites2.any((f) => f.channelId == channelId1), true);
      expect(favorites2.any((f) => f.channelId == channelId2), true);
      expect(favorites2.any((f) => f.channelId == channelId3), true);

      // Step 5: Remove a favorite after restart
      await newFavoriteRepository.remove(channelId2);

      // Step 6: Verify removal persisted
      final favorites3 = await newFavoriteRepository.getAll();
      expect(favorites3.length, 2);
      expect(favorites3.any((f) => f.channelId == channelId1), true);
      expect(favorites3.any((f) => f.channelId == channelId2), false);
      expect(favorites3.any((f) => f.channelId == channelId3), true);
    });

    test('Toggle favorite is idempotent', () async {
      const channelId = 'idempotent-test';

      // Add favorite
      await viewModel.toggleFavorite(channelId);
      expect(await favoriteRepository.isFavorite(channelId), true);

      // Remove favorite
      await viewModel.toggleFavorite(channelId);
      expect(await favoriteRepository.isFavorite(channelId), false);

      // Add again
      await viewModel.toggleFavorite(channelId);
      expect(await favoriteRepository.isFavorite(channelId), true);

      // Remove again
      await viewModel.toggleFavorite(channelId);
      expect(await favoriteRepository.isFavorite(channelId), false);
    });

    test('Favorites data integrity across operations', () async {
      // Add initial favorites
      await favoriteRepository.add('channel-a');
      await favoriteRepository.add('channel-b');
      await favoriteRepository.add('channel-c');

      // Get initial state
      final initialFavorites = await favoriteRepository.getAll();
      expect(initialFavorites.length, 3);

      // Perform various operations
      await favoriteRepository.add('channel-d');
      await favoriteRepository.remove('channel-b');
      await favoriteRepository.add('channel-e');

      // Verify final state
      final finalFavorites = await favoriteRepository.getAll();
      expect(finalFavorites.length, 4);
      expect(finalFavorites.any((f) => f.channelId == 'channel-a'), true);
      expect(finalFavorites.any((f) => f.channelId == 'channel-b'), false);
      expect(finalFavorites.any((f) => f.channelId == 'channel-c'), true);
      expect(finalFavorites.any((f) => f.channelId == 'channel-d'), true);
      expect(finalFavorites.any((f) => f.channelId == 'channel-e'), true);
    });

    test('Favorites timestamps are recorded', () async {
      const channelId = 'timestamp-test';
      
      final beforeAdd = DateTime.now();
      await favoriteRepository.add(channelId);
      final afterAdd = DateTime.now();

      final favorites = await favoriteRepository.getAll();
      expect(favorites.length, 1);
      
      final favorite = favorites[0];
      expect(favorite.channelId, channelId);
      
      // Verify timestamp is within reasonable range
      expect(favorite.addedAt.isAfter(beforeAdd.subtract(const Duration(seconds: 1))), true);
      expect(favorite.addedAt.isBefore(afterAdd.add(const Duration(seconds: 1))), true);
    });

    test('Complete favorites lifecycle with ViewModel', () async {
      // Step 1: Start with no favorites
      await viewModel.loadFavorites();
      expect(viewModel.favorites.length, 0);

      // Step 2: Add first favorite
      const channelId1 = 'lifecycle-1';
      await viewModel.toggleFavorite(channelId1);
      expect(await favoriteRepository.isFavorite(channelId1), true);

      // Step 3: Add second favorite
      const channelId2 = 'lifecycle-2';
      await viewModel.toggleFavorite(channelId2);
      expect(await favoriteRepository.isFavorite(channelId2), true);

      // Step 4: Add third favorite
      const channelId3 = 'lifecycle-3';
      await viewModel.toggleFavorite(channelId3);
      expect(await favoriteRepository.isFavorite(channelId3), true);

      // Step 5: Remove middle favorite
      await viewModel.toggleFavorite(channelId2);
      expect(await favoriteRepository.isFavorite(channelId2), false);

      // Step 6: Verify final state
      expect(await favoriteRepository.isFavorite(channelId1), true);
      expect(await favoriteRepository.isFavorite(channelId2), false);
      expect(await favoriteRepository.isFavorite(channelId3), true);

      // Step 7: Verify persistence
      final allFavorites = await favoriteRepository.getAll();
      expect(allFavorites.length, 2);
    });

    test('Favorites survive multiple restart cycles', () async {
      // Cycle 1: Add favorites
      await favoriteRepository.add('cycle-1');
      await favoriteRepository.add('cycle-2');
      
      var favorites = await favoriteRepository.getAll();
      expect(favorites.length, 2);

      // Cycle 2: Simulate restart and add more
      var newRepo = FavoriteRepository();
      await newRepo.add('cycle-3');
      
      favorites = await newRepo.getAll();
      expect(favorites.length, 3);

      // Cycle 3: Simulate restart and remove one
      newRepo = FavoriteRepository();
      await newRepo.remove('cycle-2');
      
      favorites = await newRepo.getAll();
      expect(favorites.length, 2);
      expect(favorites.any((f) => f.channelId == 'cycle-1'), true);
      expect(favorites.any((f) => f.channelId == 'cycle-2'), false);
      expect(favorites.any((f) => f.channelId == 'cycle-3'), true);

      // Cycle 4: Simulate restart and verify state
      newRepo = FavoriteRepository();
      favorites = await newRepo.getAll();
      expect(favorites.length, 2);
      expect(favorites.any((f) => f.channelId == 'cycle-1'), true);
      expect(favorites.any((f) => f.channelId == 'cycle-3'), true);
    });
  });
}
