# Requirements Document

## Introduction

本需求文档旨在改进IPTV播放器的播放列表管理和显示功能。主要包括三个方面：允许用户重命名播放列表并显示其类型（M3U或Xtream）、修复M3U直播链接播放时的崩溃问题、以及改进Xtream播放列表的分类显示方式，使其按照category进行分级展示而不是一次性显示所有频道。

## Glossary

- **System**: IPTV播放器应用程序
- **Playlist**: 播放列表，包含多个直播频道的集合
- **M3U**: 一种播放列表文件格式，通过URL或文件提供频道列表
- **Xtream**: Xtream Codes API格式的播放列表，通过API接口提供频道和分类信息
- **Category**: 频道分类，用于组织和分组直播频道
- **Live Stream**: 直播流，实时播放的视频内容
- **User**: 应用程序的使用者

## Requirements

### Requirement 1: 播放列表重命名功能

**User Story:** 作为用户，我希望能够重命名播放列表并看到其类型标识，以便更好地管理和识别不同来源的播放列表。

#### Acceptance Criteria

1. WHEN 用户查看播放列表 THEN THE System SHALL 显示播放列表名称和类型标识（M3U或Xtream）
2. WHEN 用户选择重命名播放列表 THEN THE System SHALL 提供重命名界面并保留类型标识
3. WHEN 用户输入新名称并确认 THEN THE System SHALL 更新播放列表名称并持久化到数据库
4. WHEN 用户输入空名称 THEN THE System SHALL 拒绝更新并保持原名称不变
5. WHEN 播放列表名称更新成功 THEN THE System SHALL 在播放列表界面立即反映新名称

### Requirement 2: 修复M3U直播流播放崩溃

**User Story:** 作为用户，我希望打开M3U播放列表中的直播链接时应用不会崩溃，以便能够正常观看直播内容。

#### Acceptance Criteria

1. WHEN 用户点击M3U播放列表中的频道 THEN THE System SHALL 安全地加载并播放直播流
2. WHEN 直播流URL无效或无法访问 THEN THE System SHALL 显示错误消息而不是崩溃
3. WHEN 播放器初始化失败 THEN THE System SHALL 捕获异常并提供重试选项
4. WHEN 切换M3U频道 THEN THE System SHALL 正确释放前一个流的资源并加载新流
5. IF 播放过程中发生错误 THEN THE System SHALL 记录错误详情并允许用户返回频道列表

### Requirement 3: Xtream播放列表分类显示

**User Story:** 作为用户，我希望Xtream播放列表按照分类分级展示，以便更容易浏览和查找特定类型的频道。

#### Acceptance Criteria

1. WHEN 用户打开Xtream播放列表 THEN THE System SHALL 首先显示所有可用的频道分类列表
2. WHEN 用户选择一个分类 THEN THE System SHALL 显示该分类下的所有频道
3. WHEN 分类列表为空 THEN THE System SHALL 显示提示消息说明没有可用分类
4. WHEN 用户在分类视图中 THEN THE System SHALL 提供返回分类列表的导航选项
5. WHEN 加载分类或频道数据 THEN THE System SHALL 显示加载状态指示器
6. WHEN 分类数据加载失败 THEN THE System SHALL 显示错误消息并提供重试选项

### Requirement 4: 数据模型扩展

**User Story:** 作为开发者，我希望数据模型支持播放列表重命名和分类信息存储，以便实现新的功能需求。

#### Acceptance Criteria

1. WHEN 存储播放列表 THEN THE System SHALL 包含可编辑的名称字段和类型字段
2. WHEN 存储Xtream频道 THEN THE System SHALL 包含分类ID和分类名称字段
3. WHEN 查询播放列表 THEN THE System SHALL 返回名称、类型和所有相关元数据
4. WHEN 查询Xtream频道 THEN THE System SHALL 支持按分类ID过滤
5. WHEN 更新播放列表名称 THEN THE System SHALL 保持其他字段不变

### Requirement 5: 用户界面改进

**User Story:** 作为用户，我希望界面清晰地展示播放列表类型和分类结构，以便快速理解和导航内容。

#### Acceptance Criteria

1. WHEN 显示播放列表项 THEN THE System SHALL 使用不同的图标或标签区分M3U和Xtream类型
2. WHEN 显示Xtream分类列表 THEN THE System SHALL 显示分类名称和频道数量
3. WHEN 用户进入重命名模式 THEN THE System SHALL 高亮显示当前名称并提供键盘输入
4. WHEN 显示错误消息 THEN THE System SHALL 使用用户友好的语言并提供可操作的建议
5. WHEN 执行长时间操作 THEN THE System SHALL 显示进度指示器防止用户困惑
