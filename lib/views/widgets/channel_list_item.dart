import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:shimmer/shimmer.dart';
import '../../models/channel.dart';

class ChannelListItem extends StatelessWidget {
  final Channel channel;
  final bool isFavorite;
  final bool isSelected;
  final VoidCallback onTap;
  final VoidCallback onLongPress;
  final VoidCallback onFavoriteToggle;

  const ChannelListItem({
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
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      child: Material(
        color: isSelected 
          ? Theme.of(context).colorScheme.primaryContainer 
          : Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(12),
        elevation: isSelected ? 4 : 0.5,
        child: ListTile(
          onTap: onTap,
          onLongPress: onLongPress,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          leading: _buildLeading(context),
          title: Text(
            channel.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          subtitle: channel.category != null 
            ? Text(
                channel.category!,
                style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12),
              ) 
            : null,
          trailing: isSelected 
            ? Icon(Icons.check_circle, color: Theme.of(context).colorScheme.primary)
            : IconButton(
                icon: Icon(
                  isFavorite ? Icons.favorite : Icons.favorite_border,
                  color: isFavorite ? Colors.red : null,
                ),
                onPressed: onFavoriteToggle,
              ),
        ),
      ),
    );
  }

  Widget _buildLeading(BuildContext context) {
    if (channel.logoUrl == null || channel.logoUrl!.isEmpty) {
      return Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceVariant,
          borderRadius: BorderRadius.circular(8),
        ),
        child: const Icon(Icons.tv),
      );
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: CachedNetworkImage(
        imageUrl: channel.logoUrl!,
        width: 48,
        height: 48,
        fit: BoxFit.cover,
        placeholder: (context, url) => Shimmer.fromColors(
          baseColor: Colors.grey[300]!,
          highlightColor: Colors.grey[100]!,
          child: Container(color: Colors.white),
        ),
        errorWidget: (context, url, error) => Container(
          color: Theme.of(context).colorScheme.surfaceVariant,
          child: const Icon(Icons.broken_image),
        ),
      ),
    );
  }
}
