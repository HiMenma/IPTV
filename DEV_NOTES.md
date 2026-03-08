# 📺 IPTV Player 开发与技术审计总结 (2026-03-09)

本文档记录了项目在性能优化、播放稳定性修复及多平台构建过程中的关键技术决策与填坑记录。

---

## 1. 核心性能优化 (Performance)

### **1.1 M3U/Xtream 海量数据解析**
*   **痛点**：解析数万行 M3U 文件导致主线程掉帧（UI 卡死）。
*   **对策**：使用 Isolate 并行化处理解析压力。
*   **效果**：即使导入 50,000+ 频道，应用依然保持 120fps 流畅。

### **1.2 数据库查询优化 (SQLite)**
*   **痛点**：收藏夹和历史记录加载采用“内存全量过滤”，性能随数据量增加线性下降。
*   **对策**：增加索引并实现 SQL 批量查询。

---

## 2. 播放稳定性与渲染深度加固 (Stability)

### **2.1 彻底解决 macOS “渲染死锁”**
*   **现象**：macOS 下部分源加载 20-30 秒无任何反应。
*   **修复**：**UI 预渲染策略**。在 `PlayerScreen` 中，一旦 Controller 创建，立即将播放组件放入 Widget 树，确保原生渲染表面（Texture）第一时间建立。

### **2.2 播放自愈看门狗 (Auto-Reconnect)**
*   **功能**：在 `PlayerService` 中增加 15 秒停顿监测，超时自动重连。

---

## 3. 数据库架构升级与自愈补丁 (Database Hardening)

### **3.1 幽灵表名与跨平台同步 Bug [2026-03-09]**
*   **现象**：同样的安装包，macOS 正常运行而 Android 在播放时报 SQL 错误。
*   **病因**：
    *   **代码冲突**：`HistoryRepository` 使用 `history` 表，而 `DatabaseHelper` 误创建为 `browse_history`。
    *   **环境差异**：macOS 存在旧版遗留的物理 DB 文件，其中的表名恰好匹配 Repository，掩盖了代码 Bug；Android 因经常卸载重装（干净安装）导致 Bug 立即爆发。
*   **教训**：不可依赖 `_onCreate` 进行版本管理，新旧用户环境差异是跨平台开发的巨大隐患。

### **3.2 终极自愈逻辑 (Self-Healing Strategy)**
*   **方案**：在 `DatabaseHelper` 中引入 **`_ensureSchemaConsistency`**。
*   **实现**：不信任版本号，启动时通过 `PRAGMA table_info` 强行扫描物理表结构。
*   **自愈内容**：
    1.  检测 `history` 表是否存在，缺失则自动补建。
    2.  检测 `favorites` 字段，若为旧版 `created_at` 自动重命名为 `added_at`。
    3.  动态执行 `ALTER TABLE` 补齐 `configurations` 表缺失的 Xtream 扩展字段。

---

## 4. 导航鲁棒性与逃生通道 (UX)

*   **防止 UI “死锁”**：播放报错时，确保 AppBar（含返回按钮）始终处于最顶层且可点击。
*   **显式操作**：在 Error Overlay 中加入显式的【Retry】和【Back Home】按钮。

---

## 5. 跨平台补丁集

*   **Android 防止熄屏**：添加 `WAKE_LOCK` 权限并手动同步锁状态。
*   **Web 模块移除**：为了追求极致 Native 性能，项目已剥离所有 Web 相关冗余代码。

---
**Last Updated**: 2026-03-09
**Author**: Gemini OmG Team
