# IPTV Player 第一批 P0/P1 缺陷修复交付报告

> 日期：2026-10-14｜基线：`698b552`（分支 `dev_flutter`）｜对应审计报告：`docs/CODE-ANALYSIS-2026-10-14.md`
> 协作方式：软件团队分工——工程实现 → QA 独立实证验证 → 交付总监逐条复核。**未提交，工作区待你 review 后自行 commit。**

---

## 一、交付概览

| 指标 | 修复前 | 修复后 |
|---|---|---|
| `flutter analyze lib` | 86 issues（6 warning / 0 error） | **80 issues（0 warning / 0 error）** |
| `flutter analyze test` | 113 issues | **106 issues**（无新增） |
| P0/P1 缺陷 | 12 项待修 | **11/11 已修并验证**，1 项需你在平台侧操作（密钥轮换） |
| 凭证泄露路径 | 4 条（明文日志 / UI 文案 / DB 明文 / 签名密钥入库） | 日志与 UI 侧 **6 类输入形态 + 1 类崩溃全部封堵**；DB 明文属第二批 |

改动规模：19 个文件被修改，+2 个新增文件（`lib/utils/log_redactor.dart` 306 行、`test/unit/utils/log_redactor_test.dart` 41 个用例）。

**验证强度说明**：本批 F2 经历了 5 轮"实现 → 对抗性验证 → 修复"，其中 QA 与交付总监的独立脚本共击穿 7 个缺陷（3 个阻断级、3 个泄露路径、1 个新引入崩溃）。**自建测试无法作为验收依据**——三次出现"自检全绿但实测击穿"，最终改为由交付总监用独立 `dart run` 脚本逐轮复测，验收标准是能击穿实现的第三方用例集。

---

## 二、逐项修复与验证证据

### F1【P0】签名密钥移出版本跟踪
- `git rm --cached release.keystore release.keystore.base64`（文件本体保留在磁盘，2578/3441 字节）
- `.gitignore:224/226/228` 新增 `*.keystore`、`*.keystore.base64`、`key.properties`
- **验证**：`git check-ignore -v` 实测命中新规则。原文件第 166–170 行存在 `!release.keystore` 反向规则，新规则在其后，"后匹配者胜"确保忽略生效。

> 🔴 **P0 独立跟踪项：密钥泄露未闭环，本批仅解决了"未来不再入库"，历史与远端均未清理。**
>
> 交付总监实测（`git ls-tree` / `git cat-file` 复核）：
> ```
> $ git ls-tree -r origin/dev_flutter --name-only | grep keystore
> release.keystore
> release.keystore.base64        ← 远端当前树里仍有
> $ git ls-tree -r HEAD --name-only | grep keystore
> release.keystore
> release.keystore.base64        ← 本地 HEAD 也仍有
> $ git log --all --oneline -- release.keystore
> 92f96cd 收藏和历史优化          ← 历史中可直接恢复二进制私钥
> ```
> 也就是说：`git rm --cached` 产生的 `D` **只是暂存态，尚未提交、尚未推送**；任何人 clone 仓库即可拿到私钥。
>
> **必须按顺序完成以下 4 步，缺一不可：**
> 1. **轮换密钥对**（生成新的 keystore）——私钥已公开，这是唯一能真正止损的动作，代码改动替代不了。
> 2. **更新 CI secrets**：`KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`。
> 3. **提交并推送密钥删除**（当前 `D` 状态需要 commit 才生效）。
> 4. **清理 git 历史**：`git filter-repo` 或 BFG（大仓库先备份）。
>
> **紧迫性修正（2026-10-14 打包时补充取证，推翻了初版报告"所有历史发布包均可被伪造"的判断）：**
> | 证据 | 命令 | 结论 |
> | --- | --- | --- |
> | `dist/` 从未入库 | `git ls-files dist/` 为空 | 历史产物不在版本控制中 |
> | 唯一历史 APK（2026-03-09） | `apksigner verify --print-certs` | **debug 证书签名**（`CN=Android Debug`，SHA-256 `3017c89a…`），与本次新构建的 APK 证书完全相同 |
> | CI 运行历史 | `gh run list` | 共 3 次运行**全部失败**（v0.0.2 tag 运行 1m33s 后败在根目录 `./gradlew`——Flutter 项目根目录无该任务），**从未产出任何工件** |
> | 历史 macOS 包 | `codesign -dv` | 完全未签名 |
> | tag 推送状态 | `git ls-remote origin refs/tags` | v0.0.1/v0.0.2 已不在 origin |
>
> **修正结论：`release.keystore` 从未签发过任何分发出的产物，实际影响面为零。** 不存在"APK 签名信任链已失效"的既成事实；且该密钥若要用于 Google Play，本就必须先在 Play Console 注册上传密钥——既然从未注册过，也就不存在"通知用户升级"的场景。泄露本身仍需按上面 4 步处理（私钥公开是事实，且未来若启用 release 签名会直接踩中），但**不再是"影响所有 Android 用户"的 P0，降级为安全卫生债**。与此同时，`secrets.KEYSTORE_FILE` 是否等于该泄露密钥已无法从发布记录反推（无成功发布），按"是"处理直接轮换即可。

### F2【P1】凭证日志脱敏（本批最复杂项，经 3 轮修复 + 4 轮验证）
新增 `lib/utils/log_redactor.dart`，统一处理 Service/ViewModel 日志点与 UI 错误文案。

修复过程中被验证逐轮挖出并封堵的问题：

| 编号 | 问题 | 触发条件 | 状态 |
|---|---|---|---|
| B-1 | 异常文本内嵌 URL 整体失效 | `DioException [x]: http://host/live/u/p/1.m3u8` | ✅ |
| B-2 | `/movie/`、`/series/` 线路未覆盖 | Xtream 三类线路前缀 | ✅ |
| B-3 | `error_handler` 两条旁路 | unknown 分支 + `_mapDioError` | ✅ |
| N-1 | 密码含 `()[]{}"` 泄露 | 用户设密码为 `pa(ss`（`xtream_service.dart:123` 裸插值不编码） | ✅ |
| N-2 | `user:pass@host` 形态泄露 | HTTP Basic Auth | ✅ |
| N-3 | IPv6 字面量 `[2001:db8::1]` 泄露 | 用户在配置中手填 IPv6 | ✅ |
| C-1 | **相邻双 URL 触发 `RangeError` 崩溃** | 两个 URL 无分隔符相连 | ✅ |

> **C-1 是本批最值得记录的缺陷**：它由 N-1 的新实现引入，且因 `redactUrl` 在 catch 块内调用，崩溃会让同一 catch 块后面的 `_state = PlayerState.error; notifyListeners()` 永不执行，播放器界面**永久卡在转圈且无重试按钮**。更关键的是——**只加边界防御会造成"不崩但泄漏"**：相邻 URL 被解析成单个 URI，其 `pathSegments` 中出现两组 `{prefix}/{user}/{pass}`，仅脱敏第一组会漏掉第二组。最终修复是三处叠加：游标语义修正（`redacted == null` 分支也推进）+ 路径脱敏遍历**所有**前缀 + 顶层 try-catch 兜底。

**根因与解法**：早期实现用正则模拟 URL 边界（`\w+://[^\s\[\]{}()<>"']+`），人为把 `(` `[` 当作 URL 结束符，而 `Uri.tryParse` 本身能正确处理这些字符——IPv6 场景下正则甚至**一个字符都匹配不到**，导致整段文本原样输出。最终改为**扫描式区间提取 + `Uri.tryParse` 权威解析**：宁可过度捕获尾部标点，也绝不截断密码；并以"绝不向外抛异常"作为脱敏器的硬约束（`:53-56` 的注释记录了这条设计理由）。

**验证**（交付总监独立实测 `dart run`，两脚本共 30 条全通过，零 CRASH）：
- 24 条基线：5 类特殊字符密码、3 类 userinfo、4 类 IPv6、3 类 B-1 回退、7 类不过度脱敏、幂等性、可诊断性（`DioException [x]: http://myhost:8080/live/***/***/123.m3u8` host/port/id/异常类型全保留）
- 6 条崩溃用例：相邻双 URL / 三连 / 不同 host 相邻 / 异常文本内相邻 / 单 URL 对照 / 空白分隔双 URL
- 测试文件 `test/unit/utils/log_redactor_test.dart` 共 **41 个用例**，`flutter analyze` 无 issue

### F3【P1】"刷新配置"功能无效
- `channel_viewmodel.dart:89/98/107` 三个数据源调用点透传 `forceRefresh`
- `configuration_viewmodel.dart` 注入两个 Service 并在 `refreshConfiguration` 中三清（DB 缓存 + Xtream 内存缓存 + M3U 内存缓存）
- **`main.dart:50-69` 改为 `MyApp extends StatefulWidget`**，在 State 中持有单份 `XtreamService`/`M3UService` 实例注入两个 VM

> 关键点：仅加依赖注入**修不好这个 Bug**——原代码 `ConfigurationViewModel()` 与 `ChannelViewModel()` 各自 `?? XtreamService()` 会创建两个独立实例，清的是 A 的缓存、读的是 B 的缓存。不改实例共享，F3 等于没修。
- `home_screen.dart` SnackBar 文案改为「已清除缓存，下次打开将重新加载」
- **验证**：`xtream_service.dart:78` 写入 key `'channels:$configId'` 与 `:156-157` 删除用字面量完全一致（若前缀不一致则缓存删不掉）

### F4【P1】数据库文件路径不一致
- 新增 `_resolveDbPath()`，`_initDatabase`/`deleteDatabase`/`getDatabaseSize` 三处统一；全库已无 `getDatabasesPath` 实际调用
- `deleteDatabase()` 补上**先 `close()` 再删文件**（原实现直接删已打开的文件，句柄占用下必失败）
- **验证**：路径仍基于 `getApplicationDocumentsDirectory()`，**未迁移用户数据**

### F5【P1】加载失败清空已有列表
- `channel_viewmodel.dart` catch 中移除 `_channels = []`
- `channel_list_screen.dart` 改为「error 非空**且**列表为空」才全屏报错；列表非空时正常渲染并在顶部显示非阻断错误条

### F6【P1】写历史失败误报播放错误
- `player_viewmodel.dart:143-148` `_historyRepository.add` 移出主 try，独立 try/catch，失败仅记日志

### F7【P1】播放/重连竞态
- `player_viewmodel.playChannel()` 开头取消 `_reconnectTimer`
- `player_service.dart` 引入**代次守卫**（`int _playGeneration`），并采用局部变量 + `identical()` 校验：被新 play 取代的旧调用在 await 之后不再操作已被替换的 `_videoController`；`stop()`/`dispose()` 同样递增代次；listener 按实例绑定；`_disposeControllers` 先置空字段再 await
- **验证**：5 项子检查全部落实。`await controller.dispose()` 与 `_disposeControllers` 的理论重叠经确认非缺陷（video_player 2.11.0 的 `dispose()` 有 `_isDisposed` 幂等守卫）

### F8【P1】全局错误兜底
`main.dart:18-36` `FlutterError.onError`（先 `presentError` 不吞异常）+ `runZonedGuarded`，两者均写入 AppLogger；`runApp` 仍在 zone 内，启动异常不会导致白屏。

### F9【P1】CI 工作流
Compose Multiplatform 模板残留全部清除（`composeApp`、`packageDistributionForCurrentOS` 均 0 命中）；改用 `subosito/flutter-action@v2`；Android job 保留 keystore 解码与 `android.injected.signing.*` 注入，产物路径改为真实的 `build/app/outputs/flutter-apk/*.apk`；删除重复的 debug/二次 release 构建。**验证**：YAML 可被 `yaml.safe_load` 解析，无 tab，步骤顺序正确。

### F10【P1】Android release 真签名
`build.gradle.kts` 读取根目录可选 `key.properties`（已被 .gitignore 忽略），存在则用其构建 release signingConfig，缺失则回落 debug 并打印警告（不阻断本地构建），全文无硬编码密码。**验证**：`./gradlew :app:signingReport` 双路径实测均 BUILD SUCCESSFUL。

### F11 配套
`pubspec.yaml` dev_dependencies 加 `sqflite_common_ffi: ^2.3.4`（第二批测试重建所需）；`BUILD_GUIDE.md` 签名说明重写（含轮换提醒）；`README.md` 移除 "token expiry 自动重连" 与 "Glassmorphism UI" 两处不实宣称。顺带清零 6 个既有 warning（未使用字段/导入）。

---

## 三、已知遗留风险

| 风险 | 等级 | 说明 |
|---|---|---|
| **密钥已在 git 历史中** | **P0** | 必须去 Play Console 轮换上传密钥，代码改动无法替代 |
| 凭证在 SQLite 中明文存储 | P1（第二批） | `configurations.credentials` 与 `channel_cache.stream_url` 仍含明文密码，需 `flutter_secure_storage` 或 SQLCipher |
| F7 代次守卫无测试覆盖 | P2（第二批） | `VideoPlayerController` 需真实平台实现，建议补可注入的 controller 工厂测试缝 |
| M3U 缓存只能全清 | P2 | key 含 url/filePath 而非 configId，多配置时清 A 会连带清 B（仅略多一次请求，不影响正确性）。彻底解法是改缓存 key 结构 |
| 老用户残留空 DB 文件 | P3 | F4 统一路径后，原本用 `getDatabasesPath` 的那个空文件会留在磁盘，占几 MB，不影响功能 |
| `_performReconnect` 仍是空壳 | P2 | token 刷新只有日志没有实际动作，代码注释仍有误导，建议第二批清理 |
| `flutter test` 无法在本沙箱执行 | — | 环境禁用 WebSocket（`flutter_tester` 无法启动）。已用 `flutter analyze` + 独立 `dart run` 脚本做等价验证，**建议你在本地跑一次完整 `flutter test` 复核** |

---

## 四、第二批建议范围（未开始）

1. **测试套件重建**（20 个文件 3819 行、49 处 API 失配）：`Configuration` 缺 `updatedAt` ×12、`ConfigurationRepository.save`→`add` ×9、`ConfigurationViewModel.createConfiguration`→`addConfiguration` ×9、`PlayerService.closeStreams`→`dispose` ×8、`XtreamService.authenticate`→`getAccountInfo` ×4、`sqflite_common_ffi` 未声明等。**优先修 `error_handler_test.dart`**——本批改动了该模块，它当前的编译错误会掩盖真实回归。
2. **结构倒挂**：大量测试覆盖死代码（SharedPreferences 版仓库、Validators），生产在用的 SQLite 层覆盖反而薄。
3. 凭证加密（`flutter_secure_storage` / SQLCipher）。
4. 其余 P2 项见 `docs/CODE-ANALYSIS-2026-10-14.md` 第 3.2/3.3 节（M13–M27、L28–L51）。

---

## 五、提交前建议检查项

```bash
# 1. 确认密钥已移出索引但文件仍在
git ls-files | grep keystore          # 应为空
ls -la release.keystore              # 应存在

# 2. 回归
flutter analyze lib                  # 应为 80 issues, 0 error, 0 warning
flutter analyze test                 # 应为 106 issues
flutter test                         # 本地跑，第二批修复后应从红转绿

# 3. 改动范围核对（应无 .dart_tool / 构建产物 / 临时文件）
git status --short
```

提交时建议按逻辑拆分：`chore(security): 移出签名密钥跟踪` / `fix(refresh): 修复刷新链路缓存失效` / `fix(storage): 统一数据库路径` / `fix(player): 消除播放重连竞态` / `feat(security): 新增日志凭证脱敏` / `ci: 修复发布工作流`。
