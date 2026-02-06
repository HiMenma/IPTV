import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../models/channel.dart';

/// A reusable widget for displaying channel information in a list.
/// Supports tap to play and long-press for options menu.
/// Shows favorite indicator when channel is favorited.
///
/// Requirements: 6.1, 6.2
class ChannelItem extends StatelessWidget {
  final Channel channel;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;
  final bool isFavorite;
  final VoidCallback? onToggleFavorite;
  final bool showFavoriteButton;

  const ChannelItem({
    super.key,
    required this.channel,
    required this.onTap,
    this.onLongPress,
    this.isFavorite = false,
    this.onToggleFavorite,
    this.showFavoriteButton = true,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: ListTile(
        leading: _buildChannelLogo(),
        title: Text(
          channel.name,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontWeight: FontWeight.w500),
        ),
        subtitle: channel.category != null
            ? Text(
                channel.category!,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 12),
              )
            : null,
        trailing: _buildTrailing(context),
        onTap: onTap,
        onLongPress: onLongPress,
      ),
    );
  }

  Widget _buildChannelLogo() {
    if (channel.logoUrl != null && channel.logoUrl!.isNotEmpty) {
      return CachedNetworkImage(
        imageUrl: channel.logoUrl!,
        width: 50,
        height: 50,
        fit: BoxFit.contain,
        placeholder: (context, url) => const SizedBox(
          width: 50,
          height: 50,
          child: Center(
            child: SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          ),
        ),
        errorWidget: (context, url, error) => const Icon(Icons.tv, size: 40),
        // Cache configuration
        maxHeightDiskCache: 200,
        maxWidthDiskCache: 200,
        memCacheHeight: 200,
        memCacheWidth: 200,
      );
    }
    return const Icon(Icons.tv, size: 40);
  }

  Widget? _buildTrailing(BuildContext context) {
    if (!showFavoriteButton) {
      return const Icon(Icons.play_arrow);
    }

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (onToggleFavorite != null)
          IconButton(
            icon: Icon(
              isFavorite ? Icons.favorite : Icons.favorite_border,
              color: isFavorite ? Theme.of(context).colorScheme.error : null,
            ),
            onPressed: onToggleFavorite,
            tooltip: isFavorite ? 'Remove from favorites' : 'Add to favorites',
          ),
        const Icon(Icons.play_arrow),
      ],
    );
  }
}
