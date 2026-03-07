import 'package:flutter/material.dart';
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
    return ListTile(
      onTap: onTap,
      onLongPress: onLongPress,
      selected: isSelected,
      leading: isSelected 
        ? const CircleAvatar(child: Icon(Icons.check))
        : channel.logoUrl != null && channel.logoUrl!.isNotEmpty
          ? Image.network(
              channel.logoUrl!,
              width: 40,
              height: 40,
              errorBuilder: (context, error, stackTrace) => const Icon(Icons.tv),
            )
          : const Icon(Icons.tv),
      title: Text(
        channel.name,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: channel.category != null ? Text(channel.category!) : null,
      trailing: isSelected ? null : IconButton(
        icon: Icon(
          isFavorite ? Icons.favorite : Icons.favorite_border,
          color: isFavorite ? Colors.red : null,
        ),
        onPressed: onFavoriteToggle,
      ),
    );
  }
}
