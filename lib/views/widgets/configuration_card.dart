import 'package:flutter/material.dart';
import '../../models/configuration.dart';

/// A reusable card widget for displaying configuration information
/// with action buttons for edit, delete, and refresh operations.
///
/// Requirements: 4.2, 4.3, 2.4, 3.6
class ConfigurationCard extends StatelessWidget {
  final Configuration configuration;
  final VoidCallback onTap;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final VoidCallback onRefresh;

  const ConfigurationCard({
    super.key,
    required this.configuration,
    required this.onTap,
    required this.onEdit,
    required this.onDelete,
    required this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
      child: ListTile(
        leading: Icon(
          _getIconForType(configuration.type),
          size: 40,
          color: Theme.of(context).colorScheme.primary,
        ),
        title: Text(
          configuration.name,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_getTypeLabel(configuration.type)),
            if (configuration.lastRefreshed != null)
              Text(
                'Updated: ${_formatDate(configuration.lastRefreshed!)}',
                style: const TextStyle(fontSize: 12),
              ),
            if (configuration.type == ConfigType.xtream && configuration.expirationDate != null)
              Text(
                'Expires: ${_formatExpirationDate(configuration.expirationDate!)}',
                style: TextStyle(
                  fontSize: 12,
                  color: _getExpirationColor(context, configuration.expirationDate!),
                  fontWeight: FontWeight.w500,
                ),
              ),
            if (configuration.type == ConfigType.xtream && configuration.accountStatus != null)
              Row(
                children: [
                  Icon(
                    configuration.accountStatus?.toLowerCase() == 'active'
                        ? Icons.check_circle
                        : Icons.error,
                    size: 14,
                    color: configuration.accountStatus?.toLowerCase() == 'active'
                        ? Colors.green
                        : Colors.red,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    configuration.accountStatus!,
                    style: TextStyle(
                      fontSize: 12,
                      color: configuration.accountStatus?.toLowerCase() == 'active'
                          ? Colors.green
                          : Colors.red,
                    ),
                  ),
                ],
              ),
          ],
        ),
        trailing: PopupMenuButton<String>(
          onSelected: (value) {
            switch (value) {
              case 'edit':
                onEdit();
                break;
              case 'refresh':
                onRefresh();
                break;
              case 'delete':
                onDelete();
                break;
            }
          },
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'edit',
              child: Row(
                children: [
                  Icon(Icons.edit),
                  SizedBox(width: 8),
                  Text('Edit'),
                ],
              ),
            ),
            if (configuration.type != ConfigType.m3uLocal)
              const PopupMenuItem(
                value: 'refresh',
                child: Row(
                  children: [
                    Icon(Icons.refresh),
                    SizedBox(width: 8),
                    Text('Refresh'),
                  ],
                ),
              ),
            PopupMenuItem(
              value: 'delete',
              child: Builder(
                builder: (context) => Row(
                  children: [
                    Icon(Icons.delete, color: Theme.of(context).colorScheme.error),
                    const SizedBox(width: 8),
                    Text('Delete', style: TextStyle(color: Theme.of(context).colorScheme.error)),
                  ],
                ),
              ),
            ),
          ],
        ),
        onTap: onTap,
      ),
    );
  }

  IconData _getIconForType(ConfigType type) {
    switch (type) {
      case ConfigType.xtream:
        return Icons.cloud;
      case ConfigType.m3uNetwork:
        return Icons.link;
      case ConfigType.m3uLocal:
        return Icons.folder;
    }
  }

  String _getTypeLabel(ConfigType type) {
    switch (type) {
      case ConfigType.xtream:
        return 'Xtream Codes';
      case ConfigType.m3uNetwork:
        return 'M3U Network';
      case ConfigType.m3uLocal:
        return 'M3U Local';
    }
  }

  String _formatDate(DateTime date) {
    final now = DateTime.now();
    final difference = now.difference(date);

    if (difference.inMinutes < 1) {
      return 'Just now';
    } else if (difference.inHours < 1) {
      return '${difference.inMinutes}m ago';
    } else if (difference.inDays < 1) {
      return '${difference.inHours}h ago';
    } else {
      return '${difference.inDays}d ago';
    }
  }

  String _formatExpirationDate(DateTime date) {
    final now = DateTime.now();
    final difference = date.difference(now);

    if (difference.isNegative) {
      return 'Expired';
    } else if (difference.inDays < 1) {
      return 'Today';
    } else if (difference.inDays < 7) {
      return 'in ${difference.inDays}d';
    } else if (difference.inDays < 30) {
      return 'in ${(difference.inDays / 7).floor()}w';
    } else {
      return 'in ${(difference.inDays / 30).floor()}mo';
    }
  }

  Color _getExpirationColor(BuildContext context, DateTime expirationDate) {
    final now = DateTime.now();
    final difference = expirationDate.difference(now);

    if (difference.isNegative) {
      return Theme.of(context).colorScheme.error;
    } else if (difference.inDays < 7) {
      return Colors.orange;
    } else {
      return Colors.green;
    }
  }
}
