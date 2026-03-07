import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:shimmer/shimmer.dart';
import '../../models/channel.dart';

class ChannelGridItem extends StatelessWidget {
  final Channel channel;
  final bool isFavorite;
  final bool isSelected;
  final VoidCallback onTap;
  final VoidCallback onLongPress;
  final VoidCallback onFavoriteToggle;

  const ChannelGridItem({
    super.key,
    required this.channel,
    required this.isFavorite,
    required this.isSelected,
    required this.onTap,
    required this.onLongPress,
    required this.onFavoriteToggle,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: isSelected ? 8 : 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: isSelected 
          ? BorderSide(color: Theme.of(context).colorScheme.primary, width: 2)
          : BorderSide.none,
      ),
      clipBehavior: BorderAlphaAnalysis.none == null ? Clip.antiAlias : Clip.antiAlias, // Workaround
      child: InkWell(
        onTap: onTap,
        onLongPress: onLongPress,
        child: Stack(
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  flex: 3,
                  child: _buildLogo(context),
                ),
                Expanded(
                  flex: 2,
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          channel.name,
                          textAlign: TextAlign.center,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                        ),
                        if (channel.category != null)
                          Text(
                            channel.category!,
                            textAlign: TextAlign.center,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 10,
                              color: Theme.of(context).colorScheme.onSurfaceVariant,
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
            // Favorite Button
            Positioned(
              top: 4,
              right: 4,
              child: IconButton(
                icon: Icon(
                  isFavorite ? Icons.favorite : Icons.favorite_border,
                  size: 20,
                  color: isFavorite ? Colors.red : Colors.white70,
                ),
                onPressed: onFavoriteToggle,
              ),
            ),
            // Selection Overlay
            if (isSelected)
              Container(
                color: Theme.of(context).colorScheme.primary.withOpacity(0.2),
                child: const Center(
                  child: CircleAvatar(
                    backgroundColor: Colors.white,
                    child: Icon(Icons.check, color: Colors.blue),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildLogo(BuildContext context) {
    if (channel.logoUrl == null || channel.logoUrl!.isEmpty) {
      return Container(
        color: Theme.of(context).colorScheme.surfaceVariant,
        child: const Icon(Icons.tv, size: 40),
      );
    }

    return Padding(
      padding: const EdgeInsets.all(12),
      child: CachedNetworkImage(
        imageUrl: channel.logoUrl!,
        fit: BoxFit.contain,
        placeholder: (context, url) => Shimmer.fromColors(
          baseColor: Colors.grey[300]!,
          highlightColor: Colors.grey[100]!,
          child: Container(color: Colors.white),
        ),
        errorWidget: (context, url, error) => Container(
          color: Theme.of(context).colorScheme.surfaceVariant,
          child: const Icon(Icons.broken_image_outlined),
        ),
      ),
    );
  }
}

class BorderAlphaAnalysis {
  static const none = null;
}
