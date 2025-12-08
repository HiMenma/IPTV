import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:file_picker/file_picker.dart';
import '../../viewmodels/configuration_viewmodel.dart';
import '../../models/configuration.dart';
import '../../utils/validators.dart';

class ConfigurationScreen extends StatefulWidget {
  final Configuration? configuration;

  const ConfigurationScreen({super.key, this.configuration});

  @override
  State<ConfigurationScreen> createState() => _ConfigurationScreenState();
}

class _ConfigurationScreenState extends State<ConfigurationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _serverUrlController = TextEditingController();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _m3uUrlController = TextEditingController();
  final _m3uFilePathController = TextEditingController();

  ConfigType _selectedType = ConfigType.xtream;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    if (widget.configuration != null) {
      _loadConfiguration();
    }
  }

  void _loadConfiguration() {
    final config = widget.configuration!;
    _nameController.text = config.name;
    _selectedType = config.type;

    switch (config.type) {
      case ConfigType.xtream:
        _serverUrlController.text = config.credentials['serverUrl'] ?? '';
        _usernameController.text = config.credentials['username'] ?? '';
        _passwordController.text = config.credentials['password'] ?? '';
        break;
      case ConfigType.m3uNetwork:
        _m3uUrlController.text = config.credentials['url'] ?? '';
        break;
      case ConfigType.m3uLocal:
        _m3uFilePathController.text = config.credentials['filePath'] ?? '';
        break;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _serverUrlController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    _m3uUrlController.dispose();
    _m3uFilePathController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.configuration != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditing ? 'Edit Configuration' : 'Add Configuration'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Configuration Name
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Configuration Name',
                hintText: 'My IPTV',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.label),
              ),
              validator: (value) => Validators.validateConfigurationName(value),
            ),
            const SizedBox(height: 24),

            // Configuration Type Selector
            if (!isEditing) ...[
              const Text(
                'Configuration Type',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              SegmentedButton<ConfigType>(
                segments: const [
                  ButtonSegment(
                    value: ConfigType.xtream,
                    label: Text('Xtream'),
                    icon: Icon(Icons.cloud),
                  ),
                  ButtonSegment(
                    value: ConfigType.m3uNetwork,
                    label: Text('M3U URL'),
                    icon: Icon(Icons.link),
                  ),
                  ButtonSegment(
                    value: ConfigType.m3uLocal,
                    label: Text('M3U File'),
                    icon: Icon(Icons.folder),
                  ),
                ],
                selected: {_selectedType},
                onSelectionChanged: (Set<ConfigType> newSelection) {
                  setState(() {
                    _selectedType = newSelection.first;
                  });
                },
              ),
              const SizedBox(height: 24),
            ],

            // Type-specific fields
            if (_selectedType == ConfigType.xtream) ..._buildXtreamFields(),
            if (_selectedType == ConfigType.m3uNetwork) ..._buildM3UNetworkFields(),
            if (_selectedType == ConfigType.m3uLocal) ..._buildM3ULocalFields(),

            const SizedBox(height: 32),

            // Action Buttons
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: _isLoading ? null : () => Navigator.pop(context),
                    child: const Text('Cancel'),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _saveConfiguration,
                    child: _isLoading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Text(isEditing ? 'Update' : 'Save'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildXtreamFields() {
    return [
      TextFormField(
        controller: _serverUrlController,
        decoration: const InputDecoration(
          labelText: 'Server URL',
          hintText: 'http://example.com:8080',
          border: OutlineInputBorder(),
          prefixIcon: Icon(Icons.dns),
        ),
        keyboardType: TextInputType.url,
        validator: (value) => Validators.validateXtreamServerUrl(value),
      ),
      const SizedBox(height: 16),
      TextFormField(
        controller: _usernameController,
        decoration: const InputDecoration(
          labelText: 'Username',
          border: OutlineInputBorder(),
          prefixIcon: Icon(Icons.person),
        ),
        validator: (value) => Validators.validateXtreamUsername(value),
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
        validator: (value) => Validators.validateXtreamPassword(value),
      ),
    ];
  }

  List<Widget> _buildM3UNetworkFields() {
    return [
      TextFormField(
        controller: _m3uUrlController,
        decoration: const InputDecoration(
          labelText: 'M3U URL',
          hintText: 'http://example.com/playlist.m3u',
          border: OutlineInputBorder(),
          prefixIcon: Icon(Icons.link),
        ),
        keyboardType: TextInputType.url,
        validator: (value) => Validators.validateM3UNetworkUrl(value),
      ),
    ];
  }

  List<Widget> _buildM3ULocalFields() {
    return [
      TextFormField(
        controller: _m3uFilePathController,
        decoration: InputDecoration(
          labelText: 'M3U File Path',
          hintText: 'Select a file',
          border: const OutlineInputBorder(),
          prefixIcon: const Icon(Icons.file_present),
          suffixIcon: IconButton(
            icon: const Icon(Icons.folder_open),
            onPressed: _pickFile,
          ),
        ),
        readOnly: true,
        validator: (value) => Validators.validateFilePath(value),
      ),
    ];
  }

  Future<void> _pickFile() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['m3u', 'm3u8'],
      );

      if (result != null && result.files.single.path != null) {
        setState(() {
          _m3uFilePathController.text = result.files.single.path!;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to pick file: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }

  Future<void> _saveConfiguration() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final viewModel = context.read<ConfigurationViewModel>();
      final name = _nameController.text.trim();
      final credentials = _buildCredentials();

      if (widget.configuration != null) {
        // Update existing configuration (only name can be changed)
        await viewModel.renameConfiguration(widget.configuration!.id, name);
      } else {
        // Create new configuration
        await viewModel.createConfiguration(name, _selectedType, credentials);
      }

      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(widget.configuration != null
                ? 'Configuration updated'
                : 'Configuration created'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to save configuration: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Map<String, dynamic> _buildCredentials() {
    switch (_selectedType) {
      case ConfigType.xtream:
        return {
          'serverUrl': _serverUrlController.text.trim(),
          'username': _usernameController.text.trim(),
          'password': _passwordController.text.trim(),
        };
      case ConfigType.m3uNetwork:
        return {
          'url': _m3uUrlController.text.trim(),
        };
      case ConfigType.m3uLocal:
        return {
          'filePath': _m3uFilePathController.text.trim(),
        };
    }
  }
}
