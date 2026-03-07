import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../models/configuration.dart';

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
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _buildTypeIcon(context),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          configuration.name,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.bold,
                              ),
                        ),
                        const SizedBox(height: 4),
                        if (configuration.lastRefreshed != null)
                          Text(
                            'Updated: ${_formatDate(configuration.lastRefreshed!)}',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                      ],
                    ),
                  ),
                  if (configuration.type == ConfigType.xtream && configuration.expirationDate != null)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: _getExpirationColor(context, configuration.expirationDate!)
                            .withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        'Expires: ${_formatExpirationDate(configuration.expirationDate!)}',
                        style: TextStyle(
                          fontSize: 12,
                          color: _getExpirationColor(context, configuration.expirationDate!),
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                ],
              ),
              if (configuration.type == ConfigType.xtream && configuration.accountStatus != null) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(
                      configuration.accountStatus?.toLowerCase() == 'active'
                          ? Icons.check_circle
                          : Icons.error,
                      size: 16,
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
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: onRefresh,
                    icon: const Icon(Icons.refresh, size: 18),
                    label: const Text('Refresh'),
                  ),
                  const SizedBox(width: 8),
                  PopupMenuButton<String>(
                    onSelected: (value) {
                      if (value == 'edit') onEdit();
                      if (value == 'delete') onDelete();
                    },
                    itemBuilder: (context) => [
                      const PopupMenuItem(
                        value: 'edit',
                        child: Row(
                          children: [
                            Icon(Icons.edit, size: 20),
                            SizedBox(width: 8),
                            Text('Edit'),
                          ],
                        ),
                      ),
                      const PopupMenuItem(
                        value: 'delete',
                        child: Row(
                          children: [
                            Icon(Icons.delete, size: 20, color: Colors.red),
                            SizedBox(width: 8),
                            Text('Delete', style: TextStyle(color: Colors.red)),
                          ],
                        ),
                      ),
                    ],
                    child: const Padding(
                      padding: EdgeInsets.all(8.0),
                      child: Icon(Icons.more_vert),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTypeIcon(BuildContext context) {
    IconData icon;
    Color color;

    switch (configuration.type) {
      case ConfigType.xtream:
        icon = Icons.dns;
        color = Colors.purple;
        break;
      case ConfigType.m3uNetwork:
        icon = Icons.link;
        color = Colors.blue;
        break;
      case ConfigType.m3uLocal:
        icon = Icons.folder;
        color = Colors.orange;
        break;
      case ConfigType.directLink:
        icon = Icons.play_circle;
        color = Colors.green;
        break;
    }

    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        shape: BoxShape.circle,
      ),
      child: Icon(icon, color: color),
    );
  }

  String _formatDate(DateTime date) {
    return DateFormat('MMM d, h:mm a').format(date);
  }

  String _formatExpirationDate(DateTime date) {
    return DateFormat('yyyy-MM-dd').format(date);
  }

  Color _getExpirationColor(BuildContext context, DateTime expiration) {
    final daysLeft = expiration.difference(DateTime.now()).inDays;
    if (daysLeft < 7) return Colors.red;
    if (daysLeft < 30) return Colors.orange;
    return Colors.green;
  }
}
