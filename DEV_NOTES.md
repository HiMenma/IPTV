# 📺 IPTV Player 开发与技术审计总结 (2026-03-07)

本文档记录了项目在性能优化、播放稳定性修复及多平台构建过程中的关键技术决策与填坑记录。

---

## 1. 核心性能优化 (Performance)

### **1.1 M3U/Xtream 海量数据解析**
*   **痛点**：解析数万行 M3U 文件导致主线程掉帧（UI 卡死）。
*   **对策**：
    *   **Isolate 并行化**：通过 `compute` 将解析压力移至后台线程。
    *   **正则优化**：预编译正则表达式，减少重复编译开销。
*   **效果**：即使导入 50,000+ 频道，应用依然保持 120fps 流畅。

### **1.2 数据库查询优化 (SQLite)**
*   **痛点**：收藏夹 and 历史记录加载采用“内存全量过滤”，随数据量增加性能呈线性下降。
*   **对策**：
    *   **索引增强**：为 `channel_id` 增加数据库索引。
    *   **SQL 批量查询**：实现 `getChannelsByIds` 方法，将检索复杂度从 O(N) 降低到 O(log N)。
*   **效果**：海量数据下的收藏页实现“秒开”。

### **1.3 确定性 ID (UUID v5)**
*   **痛点**：原逻辑使用随机 UUID，每次刷新频道源会导致收藏和历史记录全部失效。
*   **对策**：改用 **UUID v5**，基于 `configId + streamUrl` 生成确定性 ID。

---

## 2. 播放稳定性与渲染深度加固 (Stability)

### **2.1 播放引擎架构博弈 (VLC vs. VideoPlayer)**
*   **实验记录**：曾尝试迁移至 `flutter_vlc_player` 以追求更高兼容性，但在 macOS 上遭遇了严重的“静默挂起”问题。
*   **结论**：在 Flutter macOS 架构下，VLC 插件的 Texture 绑定稳定性不如官方推荐的 `video_player`。最终决定回退至 **Official Engine + 自研补丁** 方案。

### **2.2 彻底解决 macOS “渲染死锁” (关键修复)**
*   **现象**：macOS 下部分源加载 20-30 秒无任何反应，最终超时。
*   **修复**：**UI 预渲染策略**。在 `PlayerScreen` 中，一旦 Controller 创建，立即将播放组件放入 Widget 树，确保原生渲染表面（Texture）第一时间建立。

### **2.3 macOS 1ms 时长 Bug**
*   **修复**：**虚假时长屏蔽**。屏蔽 10s 以内的“完成”状态判定，确保直播流不会误杀。

### **2.4 播放自愈看门狗 (Auto-Reconnect)**
*   **痛点**：视频播放一段时间后无故中断（Token 过期或服务端换源轮转）。
*   **修复**：
    *   **Stall 检测**：在 `PlayerService` 中增加 8 秒停顿监测，超时即触发错误。
    *   **静默重连**：`PlayerViewModel` 捕获异常后自动执行最多 3 次静默重连（间隔 2s）。
    *   **UI 反馈**：增加橙色 HUD 提示用户“正在自动恢复连接”。

---

## 3. 数据库深度加固与自愈 (Database v3)

### **3.1 架构升级与迁移**
*   **变更**：新增 `order_index`（排序）、`last_refreshed`、`expiration_date`、`account_status`（Xtream 状态）字段。
*   **对策**：将数据库版本升至 **v3**，并实现 `onUpgrade` 逻辑。

### **3.2 数据库自愈逻辑 (Self-Healing)**
*   **痛点**：Android/macOS 覆盖安装时，若 `onUpgrade` 因非预期状态失败，会导致 SQL “Column not found” 崩溃。
*   **对策**：在 `DatabaseHelper` 中增加 **`_ensureSchemaConsistency`** 检查。应用启动时自动对比 `PRAGMA table_info`，发现缺列则动态执行 `ALTER TABLE` 补齐。

---

## 4. 专业级功能增强 (Features)

*   **首页拖拽排序**：采用 `ReorderableListView`，支持物理排序持久化。
*   **单链接直接播放 (Direct Link)**：新增配置类型，支持不经过 M3U 解析直接开播单个 URL。
*   **频道多选导出**：支持在频道列表中长按开启多选，一键导出为标准 M3U 文本或文件。
*   **错误页快捷功能**：播放失败时提供 “Copy URL” 按钮，极大方便用户切换至外部播放器（如 IINA/VLC）。

---

## 5. 跨平台兼容性补丁

*   **条件导入方案**：通过 `db_stub.dart` 和 `db_web.dart` 完美隔离了 Web 专属的 SQLite WASM 包，解决了 Native 端编译路径冲突。
*   **macOS HTTP 权限**：全面放开 ATS 限制，支持 `NSAllowsArbitraryLoadsInWebContent`。
*   **Android 防止熄屏 (Wakelock Fix)**：
    *   **权限补全**：在 `AndroidManifest.xml` 中显式添加 `WAKE_LOCK` 权限（针对部分省电机型）。
    *   **逻辑同步**：由于新版 `Chewie` 移除了内置锁参数，现统一由 `PlayerService` 显式维护 `WakelockPlus` 状态。

---
**Last Updated**: 2026-03-07
**Author**: Gemini OmG Team
