/// 日志脱敏工具。
///
/// IPTV 播放地址（尤其是 Xtream Codes 线路）会把账号密码直接写进 URL：
/// `http://host/live/{username}/{password}/{id}.m3u8`。
/// 这些 URL 一旦进入日志文件（`iptv_debug.log`）或错误提示，就等同于凭证泄露。
///
/// 本工具提供统一脱敏实现，供 Service / ViewModel 的日志点与 UI 错误文案复用。
library;

/// 敏感查询参数名（小写比较）。
const Set<String> _sensitiveQueryKeys = <String>{
  'username',
  'user',
  'password',
  'pass',
  'passwd',
  'pwd',
  'token',
  'auth',
  'apikey',
  'api_key',
  'key',
  'secret',
  'mac',
};

/// Xtream Codes 的三类线路前缀，其后均为 `{username}/{password}/{stream_id}` 形态。
const Set<String> _credentialPathPrefixes = <String>{'live', 'movie', 'series'};

/// 紧随前缀之后需要脱敏的路径段数量（用户名 + 密码）。
const int _credentialSegmentCount = 2;

/// Xtream 线路形态为 `{prefix}/{username}/{password}/{stream_id}`，
/// 即前缀后至少还需 3 段才可判定为凭证地址，避免误伤 `/live/only.m3u8` 之类的普通路径。
const int _minSegmentsAfterPrefix = 3;

/// 脱敏占位符。
const String _mask = '***';

/// URL 起始：`scheme://`。
final RegExp _schemePrefixRegExp = RegExp(r'\w+://');

/// URL 候选区间的硬终止符（空白与包裹性引号）。
///
/// 注意：`()` 不在此列—— 见 [_expandCandidate]。
const String _urlTerminators = ' \t\n\r<>"\'';

/// 明显属于异常包装、需从候选区间尾部剥离的标点（不影响 URL 语义）。
const String _trailingPunctuation = ',;:.!?';

/// 对任意文本中的凭证做脱敏，可安全写入日志或展示给用户。
///
/// **本函数绝不向外抛异常。** 它被广泛用于 catch 块内的错误处理路径，
/// 若自身抛异常会覆盖原始异常，并跳过调用方后续的状态复位逻辑
/// （如 PlayerState.error / isLoading=false），导致 UI 永久卡在加载态。
/// 因此任何内部异常都降级为粗粒度兜底（宁可过度脱敏也不泄露）。
///
/// 处理四类泄露途径：
/// 1. **异常文本内嵌 URL** —— 扫描并逐个脱敏，**严格保留原文的空白与标点结构**：
///    `DioException [x]: http://host/live/u/p/1.m3u8` → `DioException [x]: http://host/live/***/***/1.m3u8`
/// 2. **Xtream 风格路径** —— `/live/`、`/movie/`、`/series/` 后的用户名与密码
/// 3. **敏感查询参数** —— username/password/token/auth/pass 等，值替换为 `***`
/// 4. **userinfo** —— `http://user:pass@host/...`，整体替换为 `***`
///    （与路径式凭证语义一致：`/live/{user}/{pass}/` 中 user 与 pass 均被掩码，
///    故userinfo 也整体掩码，避免同一份凭证在两种写法下待遇不同）
///
/// 未检出敏感信息时原样返回，保证日志可读性不受影响。
String redactUrl(String text) {
  if (text.isEmpty || !text.contains('://')) return text;
  try {
    return _redactInternal(text);
  } catch (_) {
    // 安全设施自身故障时降级为粗粒度兜底：绝不抛出，也绝不泄露
    return _coarseFallback(text);
  }
}

/// 粗粒度兜底：把整段 `scheme://...` 全部抹掉，保留非 URL 上下文。
///
/// 仅在结构化脱敏发生内部异常时使用。可诊断性会让位于安全性——
/// 最坏结果只是日志里少了细节，而不是泄露凭证或让调用方的错误处理链崩溃。
String _coarseFallback(String text) {
  try {
    return text.replaceAll(RegExp(r'\w+://\S+'), _mask);
  } catch (_) {
    return '[redacted]';
  }
}

/// 实际的脱敏实现。**允许抛出异常**，由 [redactUrl] 统一兜底。
///
/// 实现要点：用 `emitted` 记录**已写出的文本长度**，而不是原文的 `cursor`。
/// 因为脱敏后的片段长度通常短于原区间（凭证被 `***` 替换），
/// 若按原文位置推进游标，相邻 URL 场景下会导致后续片段被误判为「已越过」而跳过，
/// 反而造成明文残留。
String _redactInternal(String text) {
  final StringBuffer out = StringBuffer();
  // consumed = 原文中已被消费到的位置；emitted = 已写出的字符数
  int consumed = 0;

  for (final RegExpMatch match in _schemePrefixRegExp.allMatches(text)) {
    final int start = match.start;

    // 防御：相邻 URL（`url1url2` 无分隔）时，区间可能与前一区间重叠。
    // 重叠部分已在上一轮被消费，跳过即可——但跳过后仍需把它计入输出，
    // 否则会落入尾部原样输出造成泄露。
    if (start < consumed) continue;

    // 1) 扫描候选区间并适度扩张（把 URL 内部的括号纳入，把尾部包装括号排除）
    final int coreEnd = _expandCandidate(text, start);
    if (coreEnd <= start || coreEnd > text.length) continue;

    // 2) 尝试脱敏该区间
    final String candidate = text.substring(start, coreEnd);
    final String? redacted = _redactSingleUrl(candidate);

    if (redacted == null) {
      // 无敏感信息：本片段原样输出，游标推进
      out.write(text.substring(consumed, coreEnd));
      consumed = coreEnd;
      continue;
    }

    // 3) 原样输出 [consumed, start) 的文本，保证空白/标点结构零改动
    out.write(text.substring(consumed, start));
    out.write(redacted);
    consumed = coreEnd;
  }

  if (consumed == 0) return text; // 全程无改动
  out.write(text.substring(consumed));
  return out.toString();
}

/// 从 `start`（指向 scheme）向右扫描 URL 候选区间，返回结束位置（不含）。
///
/// 分三步：
/// 1. 以硬终止符（空白/引号）划出最大区间；
/// 2. 若尾部有 `)` `]` `}` 且它们无法与左括号配对，说明是异常包装的收尾括号而非 URL 内容，
///    逐个剥离；
/// 3. 剩下的尾部 `,;.!?` 等无害标点一并剥离。
///
/// 这样既能让 `pass(456` 这种含括号密码完整进入解析，又不会把
/// `...1.m3u8)` 的包装括号误算进URL。
int _expandCandidate(String text, int start) {
  int end = start;
  while (end < text.length && !_urlTerminators.contains(text[end])) {
    end++;
  }

  // 若区间被引号/尖括号提前截断，说明末尾可能还有一段本属 URL 的内容。
  // 例：`Error: .../pass"456/1.m3u8` —— `"` 之后仍有 `456/1.m3u8`（密码续段 + 流 id）。
  // 继续向右吞入非空白字符，并仅在吞入后仍是可解析 URI 时才采纳，
  // 避免把后续无关文本粘连进来。
  if (end < text.length && _isMidWrapper(text[end])) {
    int probe = end;
    while (probe < text.length && !_isHardStop(text[probe])) {
      probe++;
    }
    final Uri? probeUri = Uri.tryParse(text.substring(start, probe));
    if (probeUri != null && probeUri.scheme.isNotEmpty) {
      end = probe;
    }
  }

  // 剥离尾部不成对的收尾括号：`[E] http://h/live/u/p/1.m3u8)` -> 去掉 `)`
  while (end > start) {
    final String c = text[end - 1];
    final String? opener = _matchingOpener(c);
    if (opener == null) break;
    // 该closer 在区间内能找到配对的 opener 吗？找不到则它是包装括号
    if (_countIn(text.substring(start, end), opener) <=
        _countIn(text.substring(start, end), c)) {
      break;
    }
    end--;
  }

  // 剥离尾部无害标点
  while (end > start && _trailingPunctuation.contains(text[end - 1])) {
    end--;
  }
  return end;
}

/// 是否为真正的硬终止符（空白）。
bool _isHardStop(String c) => c.trim().isEmpty;

/// 是否为「可能只是包装、后续仍属 URL」的字符（引号/尖括号/括号）。
bool _isMidWrapper(String c) => '"\'<>'.contains(c) || _matchingOpener(c) != null;

/// 返回收尾括号对应的左括号；非括号返回 null。
String? _matchingOpener(String closer) {
  switch (closer) {
    case ')':
      return '(';
    case ']':
      return '[';
    case '}':
      return '{';
    default:
      return null;
  }
}

/// 统计子串中字符出现次数。
int _countIn(String source, String ch) {
  int n = 0;
  for (int i = 0; i < source.length; i++) {
    if (source[i] == ch) n++;
  }
  return n;
}

/// 对单个 URL 字符串脱敏。
///
/// 返回脱敏后的 URL；若不含敏感信息则返回 `null`，
/// 由调用方决定保留原值（**绝不返回原文**，避免上层误以为已脱敏）。
String? _redactSingleUrl(String url) {
  final Uri? uri = Uri.tryParse(url);
  if (uri == null || uri.scheme.isEmpty) return null;

  var changed = false;

  // 1) path 脱敏：{prefix}/{seg1}/{seg2}/... -> {prefix}/***/***/
  //
  // 遍历**所有**前缀出现处，而非只用 indexOf 找第一个：
  // 相邻 URL（`url1url2` 无分隔）会被 Uri 解析成单个 URI，
  // 其 pathSegments 中会出现多组 `{prefix}/{user}/{pass}`，只处理第一组会漏掉其余。
  final List<String> segments = List<String>.from(uri.pathSegments);
  for (var i = 0; i < segments.length; i++) {
    if (!_credentialPathPrefixes.contains(segments[i].toLowerCase())) continue;
    // 前缀后至少还需 3 段才可判定为凭证地址，避免误伤 /live/only.m3u8
    if (segments.length - i - 1 < _minSegmentsAfterPrefix) continue;
    for (var offset = 1; offset <= _credentialSegmentCount; offset++) {
      if (segments[i + offset].isEmpty) continue;
      segments[i + offset] = _mask;
      changed = true;
    }
  }

  // 2) query 脱敏
  final Map<String, List<String>> redactedQuery = <String, List<String>>{};
  uri.queryParametersAll.forEach((String key, List<String> values) {
    if (_sensitiveQueryKeys.contains(key.toLowerCase())) {
      redactedQuery[key] = values.map((_) => _mask).toList();
      changed = true;
    } else {
      redactedQuery[key] = values;
    }
  });

  // 3) userinfo 脱敏（http://user:pass@host/...）
  // uri.authority 含 userinfo，必须用 userInfo/host/port 单独重组。
  //
  // 语义一致性：路径式凭证 `/live/{user}/{pass}/` 中用户名与密码**都**被脱敏，
  // 因此 userinfo 式也统一整体掩码，避免同一份凭证在两种写法下一个脱敏一个保留。
  final String userInfo = uri.userInfo;
  String redactedUserInfo = '';
  if (userInfo.isNotEmpty) {
    // 无论是否含冒号（`user:pass` 或 `token`）都整体掩码——userinfo 整体即凭证
    redactedUserInfo = _mask;
    changed = true;
  }

  if (!changed) return null;

  // 4) 重组输出。
  // **不用 uri.scheme/uri.host 重建**——Uri 会把它们规范化成小写
  // （`HTTP://H/...` 会被改写成 `http://h/...`），破坏原文可诊断性。
  // 因此仅在「原文切片」与「Uri 解析结果」之间做映射，保留原大小写。
  final String prefixRaw = url.substring(0, url.indexOf('://') + 3);
  // 去掉 userinfo 后的裸 authority 原文（含端口），保持原始大小写
  final String authorityRaw = url
      .substring(prefixRaw.length)
      .split(RegExp(r'[/?#]'))
      .first;
  final String hostPortRaw = uri.userInfo.isEmpty
      ? authorityRaw
      : authorityRaw.substring(authorityRaw.lastIndexOf('@') + 1);

  final StringBuffer buffer = StringBuffer()
    ..write(prefixRaw);
  if (redactedUserInfo.isNotEmpty) {
    buffer
      ..write(redactedUserInfo)
      ..write('@');
  }
  buffer
    ..write(hostPortRaw)
    ..write(segments.isEmpty ? '' : '/${segments.join('/')}');

  if (redactedQuery.isNotEmpty) {
    // 敏感值固定为 `***` 不再编码（编码会变成 %2A%2A%2A 反而不可读）；
    // 非敏感值沿用标准编码，保证 URL 语义不变。
    final String query = redactedQuery.entries
        .map((entry) {
      final String encodedKey = Uri.encodeQueryComponent(entry.key);
      final bool isMasked = _sensitiveQueryKeys.contains(entry.key.toLowerCase());
      return entry.value
          .map((value) => isMasked
              ? '$encodedKey=$_mask'
              : '$encodedKey=${Uri.encodeQueryComponent(value)}')
          .join('&');
    }).join('&');
    buffer.write('?$query');
  }
  return buffer.toString();
}