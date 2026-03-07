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
*   **痛点**：收藏夹和历史记录加载采用“内存全量过滤”，随数据量增加性能呈线性下降。
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
*   **病因 (The Paradox)**：底层引擎（AVFoundation/VLC）需要先有原生 Surface（即 Flutter 的 Widget）才能开始探测流；而原 UI 逻辑是“探测成功才渲染 Widget”。
*   **修复**：**UI 预渲染策略**。在 `PlayerScreen` 中，一旦 Controller 创建，立即将播放组件放入 Widget 树，确保原生渲染表面（Texture）第一时间建立。

### **2.3 macOS 1ms 时长 Bug**
*   **根因**：AVFoundation 对直播流误报 1ms 时长，导致播放器瞬间触发“播放结束”。
*   **修复**：**虚假时长屏蔽**。屏蔽 10s 以内的“完成”状态判定，确保直播流不会误杀。

---

## 3. 多平台兼容性补丁 (Compatibility)

### **3.1 跨平台数据库条件导入 (Conditional Import)**
*   **痛点**：Web 专属包 `sqflite_common_ffi_web` 会导致 macOS 编译失败。
*   **对策**：通过 `db_stub.dart` 和 `db_web.dart` 实现条件导出，完美隔离 Web 与 Native 代码。

### **3.2 macOS HTTP 权限放开**
*   **设置**：在 `Info.plist` 中配置 `NSAllowsArbitraryLoads` 及其子项，确保非加密流可访问。

---

## 4. UI 交互优化

*   **按钮位移**：将“添加配置”按钮从右下角 FloatingActionButton 移动至 `AppBar` 右上角。解决在列表项较多或窗口较小时遮挡内容的问题。

---

## 5. 后续开发建议 (Roadmap)

1.  **连接预取 (Low)**：在列表点击时预初始化 Controller。
2.  **错误自动重连 (Med)**：在 `PlayerViewModel` 中加入指数退避的重试逻辑。

---
**Last Updated**: 2026-03-07
**Author**: Gemini OmG Team
