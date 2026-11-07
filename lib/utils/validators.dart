/// Input validation utilities for IPTV Player

class Validators {
  static const int minNameLength = 1;
  static const int maxNameLength = 100;

  static String? validateConfigurationName(String? name) {
    if (name == null || name.trim().isEmpty) {
      return 'Configuration name cannot be empty';
    }

    final trimmedName = name.trim();

    if (trimmedName.length < minNameLength) {
      return 'Configuration name is too short';
    }

    if (trimmedName.length > maxNameLength) {
      return 'Configuration name must be $maxNameLength characters or less';
    }

    return null;
  }

  static String? validateUrl(String? url) {
    if (url == null || url.trim().isEmpty) {
      return 'URL cannot be empty';
    }

    final trimmedUrl = url.trim();

    // Check if URL starts with http:// or https://
    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      return 'URL must start with http:// or https://';
    }

    // Basic URL validation using Uri.parse
    try {
      final uri = Uri.parse(trimmedUrl);
      
      // Check if host is present (allow IP addresses and domains)
      if (uri.host.isEmpty) {
        return 'Invalid URL: missing host';
      }

      // Check for valid scheme
      if (uri.scheme != 'http' && uri.scheme != 'https') {
        return 'URL must use http or https protocol';
      }

      // Additional validation: ensure the URL has a valid structure
      // Allow URLs with or without path (e.g., http://example.com:8080 is valid)
      // The host can be a domain name or IP address
      if (uri.host.contains(' ')) {
        return 'Invalid URL: host cannot contain spaces';
      }

      return null;
    } catch (e) {
      return 'Invalid URL format';
    }
  }

  static String? validateXtreamServerUrl(String? serverUrl) {
    return validateUrl(serverUrl);
  }

  static String? validateXtreamUsername(String? username) {
    if (username == null || username.trim().isEmpty) {
      return 'Username is required';
    }

    return null;
  }

  static String? validateXtreamPassword(String? password) {
    if (password == null || password.trim().isEmpty) {
      return 'Password is required';
    }

    return null;
  }

  static Map<String, String> validateXtreamCredentials({
    String? serverUrl,
    String? username,
    String? password,
  }) {
    final errors = <String, String>{};

    final serverUrlError = validateXtreamServerUrl(serverUrl);
    if (serverUrlError != null) {
      errors['serverUrl'] = serverUrlError;
    }

    final usernameError = validateXtreamUsername(username);
    if (usernameError != null) {
      errors['username'] = usernameError;
    }

    final passwordError = validateXtreamPassword(password);
    if (passwordError != null) {
      errors['password'] = passwordError;
    }

    return errors;
  }

  /// Validate M3U file path (basic check)
  /// Returns null if valid, error message if invalid
  static String? validateFilePath(String? filePath) {
    if (filePath == null || filePath.trim().isEmpty) {
      return 'File path cannot be empty';
    }

    return null;
  }

  static String? validateM3UNetworkUrl(String? url) {
    final urlError = validateUrl(url);
    if (urlError != null) {
      return urlError;
    }

    // Additional check: M3U files typically end with .m3u or .m3u8
    final trimmedUrl = url!.trim().toLowerCase();
    if (!trimmedUrl.contains('.m3u') && !trimmedUrl.contains('.m3u8')) {
      // This is a warning, not an error - some M3U URLs don't have extensions
      // So we don't return an error, just validate the URL format
    }

    return null;
  }
}
