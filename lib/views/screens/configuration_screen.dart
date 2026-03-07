import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:uuid/uuid.dart';
import '../../models/configuration.dart';
import '../../viewmodels/configuration_viewmodel.dart';

class ConfigurationScreen extends StatefulWidget {
  final Configuration? configuration;

  const ConfigurationScreen({super.key, this.configuration});

  @override
  State<ConfigurationScreen> createState() => _ConfigurationScreenState();
}

class _ConfigurationScreenState extends State<ConfigurationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _urlController = TextEditingController();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  
  ConfigType _selectedType = ConfigType.m3uNetwork;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    if (widget.configuration != null) {
      _nameController.text = widget.configuration!.name;
      _selectedType = widget.configuration!.type;
      
      final creds = widget.configuration!.credentials;
      if (_selectedType == ConfigType.xtream) {
        _urlController.text = creds['serverUrl'] ?? '';
        _usernameController.text = creds['username'] ?? '';
        _passwordController.text = creds['password'] ?? '';
      } else if (_selectedType == ConfigType.m3uNetwork || _selectedType == ConfigType.directLink) {
        _urlController.text = creds['url'] ?? '';
      }
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _urlController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.configuration == null ? 'Add Configuration' : 'Edit Configuration'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Type Selection
              const Text('Source Type', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              SegmentedButton<ConfigType>(
                segments: const [
                  ButtonSegment(value: ConfigType.m3uNetwork, label: Text('M3U URL'), icon: Icon(Icons.link)),
                  ButtonSegment(value: ConfigType.xtream, label: Text('Xtream'), icon: Icon(Icons.dns)),
                  ButtonSegment(value: ConfigType.directLink, label: Text('Direct'), icon: Icon(Icons.play_circle)),
                ],
                selected: {_selectedType},
                onSelectionChanged: (Set<ConfigType> newSelection) {
                  setState(() {
                    _selectedType = newSelection.first;
                  });
                },
              ),
              const SizedBox(height: 24),

              // Common Name Field
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Friendly Name',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.label),
                ),
                validator: (value) => value == null || value.isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 16),

              // Conditional Fields based on Type
              if (_selectedType == ConfigType.xtream) ...[
                TextFormField(
                  controller: _urlController,
                  decoration: const InputDecoration(
                    labelText: 'Server URL (e.g. http://provider.com:8080)',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.computer),
                  ),
                  validator: (value) => value == null || value.isEmpty ? 'Required' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _usernameController,
                  decoration: const InputDecoration(
                    labelText: 'Username',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.person),
                  ),
                  validator: (value) => value == null || value.isEmpty ? 'Required' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _passwordController,
                  decoration: const InputDecoration(
                    labelText: 'Password',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.lock),
                  ),
                  obscureText: true,
                  validator: (value) => value == null || value.isEmpty ? 'Required' : null,
                ),
              ] else ...[
                TextFormField(
                  controller: _urlController,
                  decoration: InputDecoration(
                    labelText: _selectedType == ConfigType.directLink ? 'Stream URL (.m3u8, .ts, etc)' : 'M3U Playlist URL',
                    border: const OutlineInputBorder(),
                    prefixIcon: const Icon(Icons.link),
                  ),
                  validator: (value) => value == null || value.isEmpty ? 'Required' : null,
                ),
              ],

              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _saveConfiguration,
                  child: _isLoading 
                    ? const CircularProgressIndicator() 
                    : Text(widget.configuration == null ? 'Save & Add' : 'Update'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _saveConfiguration() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final id = widget.configuration?.id ?? const Uuid().v4();
      final Map<String, dynamic> credentials = {};

      if (_selectedType == ConfigType.xtream) {
        credentials['serverUrl'] = _urlController.text.trim();
        credentials['username'] = _usernameController.text.trim();
        credentials['password'] = _passwordController.text.trim();
      } else {
        credentials['url'] = _urlController.text.trim();
      }

      final config = Configuration(
        id: id,
        name: _nameController.text.trim(),
        type: _selectedType,
        credentials: credentials,
        createdAt: widget.configuration?.createdAt ?? DateTime.now(),
        updatedAt: DateTime.now(),
        orderIndex: widget.configuration?.orderIndex ?? 0,
      );

      if (widget.configuration == null) {
        await context.read<ConfigurationViewModel>().addConfiguration(config);
      } else {
        await context.read<ConfigurationViewModel>().updateConfiguration(config);
      }

      if (mounted) {
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }
}
