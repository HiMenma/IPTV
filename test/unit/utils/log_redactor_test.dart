import 'package:flutter_test/flutter_test.dart';
import 'package:iptv_player/utils/log_redactor.dart';

/// 断言结果中不残留凭证形态（用于兜底校验，避免"看似脱敏实则原样返回"）。
void expectNoCredentialLeak(String redacted) {
  expect(redacted, isNot(contains('/u1/p1/')),
      reason: '结果中残留用户名/密码路径段: $redacted');
  expect(redacted, isNot(contains('user123')));
  expect(redacted, isNot(contains('pass456')));
  expect(redacted, isNot(contains('s3cr3t')));
  expect(redacted, isNot(contains('u1&')));
}

void main() {
  group('redactUrl - 裸 URL', () {
    test('脱敏 Xtream /live/ 路径中的用户名与密码', () {
      expect(
        redactUrl('http://host.com/live/john123/p%40ss/456.m3u8'),
        'http://host.com/live/***/***/456.m3u8',
      );
    });

    test('脱敏 /movie/ 与 /series/ 线路', () {
      expect(
        redactUrl('http://host:8080/movie/user123/pass456/55.m3u8'),
        'http://host:8080/movie/***/***/55.m3u8',
      );
      expect(
        redactUrl('http://host:8080/series/user123/pass456/77.m3u8'),
        'http://host:8080/series/***/***/77.m3u8',
      );
    });

    test('脱敏查询参数中的敏感字段并保留其他参数', () {
      expect(
        redactUrl('http://h/s.m3u8?username=u1&password=p1&token=t1&cid=99'),
        'http://h/s.m3u8?username=***&password=***&token=***&cid=99',
      );
    });

    test('敏感参数名大小写不敏感', () {
      expect(
        redactUrl('https://a.b/live/u/p/x.m3u8?PASS=s3cr3t&mac=AA:BB'),
        'https://a.b/live/***/***/x.m3u8?PASS=***&mac=***',
      );
    });

    test('支持 rtsp 协议', () {
      expect(
        redactUrl('rtsp://10.0.0.1:554/live/u/p/stream'),
        'rtsp://10.0.0.1:554/live/***/***/stream',
      );
    });

    test('无凭证时原样返回', () {
      const url = 'http://host.com/live/123.m3u8?cid=1';
      expect(redactUrl(url), url);
    });

    test('路径段不足时不改动（避免误伤普通 /live/xxx.m3u8）', () {
      const url = 'http://host.com/live/only.m3u8';
      expect(redactUrl(url), url);
      const movie = 'http://host/movie/only.m3u8';
      expect(redactUrl(movie), movie);
    });

    test('路径与查询参数同时脱敏', () {
      expect(
        redactUrl('http://host.com/live/u/p/9.ts?auth=abc&x=1'),
        'http://host.com/live/***/***/9.ts?auth=***&x=1',
      );
    });

    test('非常规输入不抛异常', () {
      expect(redactUrl('not a url'), 'not a url');
      expect(redactUrl(''), '');
    });
  });

  // 生产调用点传入的几乎都是 e.toString() 这类异常文本，
  // 而非裸 URL——这类文本含方括号/圆括号，是本模块最容易漏掉的形态。
  group('redactUrl - 异常文本内嵌 URL', () {
    test('DioException 方括号包裹', () {
      const input =
          'DioException [http get error]: http://host/live/user123/pass456/123.m3u8';
      expect(
        redactUrl(input),
        'DioException [http get error]: http://host/live/***/***/123.m3u8',
      );
      expectNoCredentialLeak(redactUrl(input));
    });

    test('Flutter Invalid argument(s) 圆括号包裹', () {
      const input = 'Invalid argument(s): http://host/live/u1/p1/1.m3u8';
      expect(
        redactUrl(input),
        'Invalid argument(s): http://host/live/***/***/1.m3u8',
      );
      expectNoCredentialLeak(redactUrl(input));
    });

    test('花括号包裹', () {
      const input = '{http://host/live/u1/p1/1.m3u8}';
      expect(redactUrl(input), '{http://host/live/***/***/1.m3u8}');
      expectNoCredentialLeak(redactUrl(input));
    });

    test('Exception 前缀不被吞入 scheme', () {
      const input = 'Exception: Failed to open http://host/live/u1/p1/1.m3u8';
      expect(
        redactUrl(input),
        'Exception: Failed to open http://host/live/***/***/1.m3u8',
      );
    });

    test('同一文本中多个 URL 全部脱敏', () {
      expect(
        redactUrl('a http://h/live/u1/p1/1.m3u8 b http://h/live/u2/p2/2.m3u8 c'),
        'a http://h/live/***/***/1.m3u8 b http://h/live/***/***/2.m3u8 c',
      );
    });

    test('URL 尾随标点不被吞入', () {
      expect(
        redactUrl('see http://h/live/u1/p1/1.m3u8.'),
        'see http://h/live/***/***/1.m3u8.',
      );
    });

    test('不含 URL 的纯文本原样返回', () {
      expect(redactUrl('Connection timed out'), 'Connection timed out');
    });

    test('带凭证的异常文本绝不原样返回（核心兜底断言）', () {
      const inputs = [
        'DioException [x]: http://h/live/u1/p1/1.m3u8',
        'Invalid argument(s): http://h/live/u1/p1/1.m3u8',
        '{http://h/live/u1/p1/1.m3u8}',
        'PlatformException(channel, http://h/live/u1/p1/1.m3u8)',
        'http://h/movie/u1/p1/1.m3u8',
        'http://h/series/u1/p1/1.m3u8',
      ];
      for (final input in inputs) {
        expect(redactUrl(input), isNot(equals(input)),
            reason: '未脱敏: $input');
        expectNoCredentialLeak(redactUrl(input));
      }
    });
  });

  // N-1：密码含括号/引号时不得泄露。
  // 依据：Uri 本身完全能解析这些字符作为路径内容，是正则人为设限导致的泄露。
  group('redactUrl - 密码含特殊字符', () {
    test('密码含左/右括号', () {
      expect(
        redactUrl('Error: http://host/live/user123/pass(456/1.m3u8'),
        'Error: http://host/live/***/***/1.m3u8',
      );
      expect(
        redactUrl('Error: http://host/live/user123/pass)456/1.m3u8'),
        'Error: http://host/live/***/***/1.m3u8',
      );
    });

    test('密码含方括号', () {
      expect(
        redactUrl('Error: http://host/live/user123/pass[456/1.m3u8'),
        'Error: http://host/live/***/***/1.m3u8',
      );
    });

    test('密码含花括号', () {
      expect(
        redactUrl('Error: http://host/live/user123/pass{456/1.m3u8'),
        'Error: http://host/live/***/***/1.m3u8',
      );
    });

    test('密码含双引号（且后续仍有内容）', () {
      expect(
        redactUrl('Error: http://host/live/user123/pa"ss/1.m3u8 done'),
        'Error: http://host/live/***/***/1.m3u8 done',
      );
      expect(
        redactUrl('Error: http://host/live/user123/pass"456/1.m3u8'),
        'Error: http://host/live/***/***/1.m3u8',
      );
    });

    test('密码同时含括号与引号', () {
      expect(
        redactUrl('Error: http://host/live/u1/p(a"ss/1.m3u8'),
        'Error: http://host/live/***/***/1.m3u8',
      );
    });

    test('URL 本身被引号/括号包裹时正确剥离', () {
      expect(
        redactUrl('Error: "http://host/live/user123/pass/1.m3u8"'),
        'Error: "http://host/live/***/***/1.m3u8"',
      );
      expect(
        redactUrl('Error: (http://host/live/user123/pass/1.m3u8)'),
        'Error: (http://host/live/***/***/1.m3u8)',
      );
      expect(
        redactUrl('[E] http://host/live/user123/pass/1.m3u8'),
        '[E] http://host/live/***/***/1.m3u8',
      );
    });
  });

  // N-2：http://user:pass@host/... 形态（HTTP Basic Auth 等）
  group('redactUrl - userinfo 形态', () {
    test('userinfo 整体被脱敏（与路径式凭证语义一致）', () {
      expect(
        redactUrl('http://user123:pass456@host/live/u1/p1/1.m3u8'),
        'http://***@host/live/***/***/1.m3u8',
      );
    });

    test('仅凭证无路径时同样脱敏且保留端口', () {
      expect(
        redactUrl('http://user123:pass456@host:8080/playlist.m3u'),
        'http://***@host:8080/playlist.m3u',
      );
    });

    test('userinfo 无密码段（token 型）同样整体掩码', () {
      expect(
        redactUrl('http://s3cr3tToken@host/live/u1/p1/1.m3u8'),
        'http://***@host/live/***/***/1.m3u8',
      );
    });
  });

  group('redactUrl - 不应过度脱敏', () {
    test('百分号编码形态正确脱敏', () {
      expect(redactUrl('http://h/live/u/pa%28ss%29/1.m3u8'),
          'http://h/live/***/***/1.m3u8');
      expect(redactUrl('http://h/live/u/pa%5Bss%5D/1.m3u8'),
          'http://h/live/***/***/1.m3u8');
    });

    test('保留 scheme/host 原始大小写', () {
      expect(redactUrl('HTTP://H/LIVE/u1/p1/1.m3u8'),
          'HTTP://H/LIVE/***/***/1.m3u8');
    });

    test('保留原文换行等空白结构', () {
      expect(redactUrl('line1\nhttp://h/live/u1/p1/1.m3u8\nline3'),
          'line1\nhttp://h/live/***/***/1.m3u8\nline3');
    });

    test('前缀在末段或路径过短时不误伤', () {
      const a = 'http://h/a/b/movie';
      expect(redactUrl(a), a);
      const b = 'http://h/series/only.m3u8';
      expect(redactUrl(b), b);
    });

    test('幂等：脱敏结果再脱敏不变', () {
      const withParen = 'Error: http://host/live/user123/pass(456/1.m3u8';
      final once = redactUrl(withParen);
      expect(redactUrl(once), once);

      const withUserInfo = 'http://user123:pass456@host/live/u1/p1/1.m3u8';
      final onceUi = redactUrl(withUserInfo);
      expect(redactUrl(onceUi), onceUi);
    });
  });

  // N-3：IPv6 字面量形如 http://[::1]:8080/，其 `[` 不应导致 URL 被整段跳过。
  //旧实现用 `\w+://[^\s\[\]...]+` 提取 URL，遇到 `[` 直接零匹配 → 凭证 100% 泄露。
  group('redactUrl - IPv6 主机', () {
    test('IPv6 含端口，host 与端口均保留', () {
      expect(
        redactUrl('http://[2001:db8::1]:8080/live/user123/pass456/1.m3u8'),
        'http://[2001:db8::1]:8080/live/***/***/1.m3u8',
      );
    });

    test('IPv6 不含端口', () {
      expect(
        redactUrl('http://[::1]/live/user123/pass456/1.m3u8'),
        'http://[::1]/live/***/***/1.m3u8',
      );
    });

    test('IPv6 内嵌于异常文本', () {
      expectNoCredentialLeak(
        redactUrl('err http://[2001:db8::1]:8080/live/user123/pass456/1.m3u8'),
      );
    });

    test('IPv6 无凭证时不误伤', () {
      const url = 'http://[::1]:8080/plain/path';
      expect(redactUrl(url), url);
    });
  });

  // C-1 回归：相邻 URL（无分隔）曾触发 RangeError 崩溃。
  // 根因是按原文位置推进游标，而脱敏后片段长度变短，导致后续区间被误判为已越过。
  group('redactUrl - 相邻 URL 不崩溃且不泄露', () {
    test('相邻双 URL 不抛异常且全部脱敏', () {
      const u = 'http://h/live/u/p/1.m3u8';
      const masked = 'http://h/live/***/***/1.m3u8';
      expect(redactUrl('$u$u'), '$masked$masked');
    });

    test('三连 URL', () {
      const u = 'http://h/live/u/p/1.m3u8';
      expect(() => redactUrl('$u$u$u'), returnsNormally);
      expectNoCredentialLeak(redactUrl('$u$u$u'));
    });

    test('不同 host 相邻', () {
      const a = 'http://a/live/u/p/1.m3u8';
      const b = 'http://b/live/u/p/2.m3u8';
      expectNoCredentialLeak(redactUrl('$a$b'));
    });

    test('异常文本内相邻双 URL', () {
      const u = 'http://h/live/u/p/1.m3u8';
      final out = redactUrl('DioException [x]: $u$u');
      expectNoCredentialLeak(out);
      expect(out, startsWith('DioException [x]: '));
    });

    test('相邻 URL + 密码含括号组合', () {
      const a = 'http://h/live/u1/p(1/1.m3u8';
      const b = 'http://h/live/u2/p)2/2.m3u8';
      expect(() => redactUrl('$a$b'), returnsNormally);
      expectNoCredentialLeak(redactUrl('$a$b'));
    });

    test('IPv6 + userinfo + 相邻组合不抛异常', () {
      final inputs = <String>[
        'http://[::1]:8080/live/u/p/1.m3u8http://[::2]:9090/live/a/b/2.m3u8',
        'http://user:pass@h/live/u/p/1.m3u8http://h/live/u/p/1.m3u8',
        List.filled(30, 'http://h/live/u/p/1.m3u8').join(),
      ];
      for (final input in inputs) {
        expect(() => redactUrl(input), returnsNormally);
        expectNoCredentialLeak(redactUrl(input));
      }
    });

    test('兜底路径：任何输入都不抛异常', () {
      final inputs = <String>[
        '',
        '://',
        'http://',
        'http://h',
        '://live/u/p/1',
        'a://b://c://d',
        'http://[::1]:8080/live/u/p/1.m3u8http://[::2]:9090/live/a/b/2.m3u8',
        'x' * 10000,
      ];
      for (final input in inputs) {
        expect(() => redactUrl(input), returnsNormally, reason: 'input=$input');
      }
    });
  });
}
