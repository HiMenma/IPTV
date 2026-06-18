# IPTV Player 开发与技术审计总结 (2026-03-07)

本文档记录项目在性能优化、播放稳定性修复及多平台构建过程中的关键技术决策与问题修复记录。

---

## 1. 核心性能优化 (Performance)

### M3U/Xtream 海量数据解析
*   **问题**：解析数万行 M3U 文件导致主线程掉帧（UI 卡死）。
*   **方案**：
    *   使用 `compute` 将解析压力移至后台线程。
    *   预编译正则表达式，减少重复编译开销。
*   **效果**：即使导入 50,000+ 频道，应用依然保持 120fps 流畅。

### 数据库查询优化 (SQLite)
*   **问题**：收藏夹和历史记录加载采用"内存全量过滤"，随数据量增加性能呈线性下降。
*   **方案**：
    *   为 `channel_id` 增加数据库索引。
    *   实现 `getChannelsByIds` 方法，将检索复杂度从 O(N) 降低到 O(log N)。
*   **效果**：海量数据下的收藏页实现"秒开"。

### 确定性 ID (UUID v5)
*   **问题**：原逻辑使用随机 UUID，每次刷新频道源会导致收藏和历史记录全部失效。
*   **方案**：改用 UUID v5，基于 `configId + streamUrl` 生成确定性 ID。
*   **效果**：确保了跨设备、跨刷新周期的用户数据一致性。

---

## 2. 播放稳定性修复 (Stability)

### Android 端播放中断 (1分钟终止)
*   **根因**：服务器端 User-Agent 屏蔽 + 错误的流时长 metadata。
*   **修复**：
    *   注入 `VLC/3.0.12` 的 HTTP Header。
    *   屏蔽 10s 以内的"播放完成"状态判定，确保直播流不会因为 metadata 报错而中断。

### macOS 端有声无画 (黑屏/秒黑)
*   **根因**：AVFoundation 纹理绑定延迟 + 1ms 异常时长导致的渲染层过早销毁。
*   **修复**：
    *   初始化后强制延迟 500ms 并执行二次 `play()` 激活纹理。
    *   强制 fallback 16:9 比例，解决因比例识别失败导致的黑屏。
    *   忽略 macOS 常见的 1ms 虚假时长，阻止播放器进入 `stopped` 状态。

---

## 3. 多平台构建方案 (CI/CD)

### 自动化脚本
*   Bash (`build_all.sh`): 支持 macOS/Linux，集成参数化构建（`--apk`, `--mac`, `--web`）。
*   Batch (`build_all.bat`): 支持 Windows 平台。
*   自动版本命名、产物自动压缩归档至 `dist/`。

### Web 端持久化 (SQLite WASM)
*   **问题**：Web 浏览器不原生支持 SQLite 文件系统。
*   **方案**：
    *   集成 `sqflite_common_ffi_web` 并自动生成 `sqlite3.wasm` 驱动。
    *   在 `main.dart` 顶层同步初始化 `databaseFactory`，杜绝竞态导致的 `Bad state` 报错。

---

## 4. 平台差异手册

| 平台 | 底层内核 | 兼容性描述 |
|:---|:---|:---|
| Android | ExoPlayer | 能处理各种非标 TS/HLS 视频流 |
| macOS | AVFoundation | 较挑剔，若流不符合标准常报 Network Error |
| Web | HTML5 Video | 受限于 CORS 跨域政策，建议部署代理服务器转发 |

---

## 5. 后续开发路线图

1. **内核重构**: 针对 macOS 端的兼容性问题，建议未来引入 `flutter_vlc_player` 或自建 FFmpeg 渲染层。
2. **日志导出**: 为设置页面增加"导出 Debug 日志"功能，通过 `share_plus` 发送 `iptv_debug.log`。
3. **列表虚拟化**: 针对超过 10 万频道的极端场景，进一步优化 `ListView.builder` 的预取逻辑。

---
**Last Updated**: 2026-03-07
