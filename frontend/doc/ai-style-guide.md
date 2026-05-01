# ICS-Guard Next 前端 AI 代码生成风格指南 (Frontend Style Guide)

本文档用于指导 AI 或前端开发者在编写 `ICS-Guard-Next` 系统页面时的 UI/UX 设计风格与代码规范。

## 1. 整体基调与核心关键词
- **核心业务**：工业控制网络安全智能监测与动态防护系统。
- **风格关键词**：物联网 (IoT)、智慧园区、网络安全、现代 B 端后台、高科技感、严谨专业。

## 2. 颜色规范 (Color Palette)
- **主背景**：
  - **侧边栏 (Sidebar)**：深空暗蓝渐变 `linear-gradient(180deg, #051328 0%, #0b1a37 100%)`，传递工业网络的深层安全感。
  - **内容区 (Main Content)**：浅灰灰白 `#f0f2f5`，以凸白和阴影突出数据卡片。
- **语义色 (Semantic Colors)** (基于 Element Plus 体系)：
  - **品牌蓝 (Primary)**：`#409eff` (关键操作、活跃状态、常规高亮)
  - **成功绿 (Success)**：`#67c23a` (设备在线、防护生效、已解决告警)
  - **警告黄 (Warning)**：`#e6a23c` (中度/高危告警、未确认异常)
  - **危险红 (Danger)**：`#f56c6c` (严重拦截、设备离线、网络攻击行为)
- **柔和背景搭配**：为了增加科技感，状态图标模块应使用 `10%` 透明度的背景色，例如包裹红色图标的背景可使用 `rgba(245, 108, 108, 0.1)`。

## 3. 排版与字体 (Typography)
- **标准字体**：系统默认无衬线黑体 (`'Helvetica Neue', 'PingFang SC', ...`)。
- **科技字体 (Monospace)（重点）**：针对 IP 地址、MAC 地址、协议、端口、微隔离规则 ID、Kafka Topic 等工业网络数据，**必须** 使用等宽微机字体，并配合蓝色或灰色：
  ```css
  .tech-font.mono {
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    color: #409eff; /* 或者 #303133 */
  }
  ```

## 4. 核心 UI 组件使用要求
### 4.1 卡片层 (Card)
- 强制使用 `<el-card shadow="hover">`。
- 取消原生边框，增加圆角：
  ```css
  .el-card {
    border: none;
    border-radius: 12px;
  }
  ```
- 卡片标题区 (`<template #header>`) 应包含对应意象的 `<el-icon>`，如 `PieChart`，`Warning` 等。

### 4.2 标签与状态 (Tags)
- 优先使用 `<el-tag>` 呈现状态和告警级别。
- **严重度**采用深色实心风格：`size="small" effect="dark"`。
- **处理状态**采用边框镂空风格：`effect="plain"` 或 `effect="light"` 以区分视觉层次。

### 4.3 数据可视化 (Data Visualization)
- **数字面板**：取消传统死板的数字，使用大字体（例如 `28px`，加粗 `700`），并带有相应的颜色标识。
- **统计进度与对比**：使用 `<el-progress>` 组件或基于 CSS width `transition` 封装的纯色横条。
- **排行组件**：源 IP、被阻断设备等排行，需使用带鲜艳颜色块的数字序号（如排行前三名分别是大红、亮黄、天蓝背景，后置名次为浅灰底）。

## 5. 代码编写规范 (Vue 3)
- **核心技术栈**：强制使用 `<script setup lang="ts">`。
- **样式隔离**：所有的 `.vue` 页面组件均须加 `<style scoped>`。
- **动效 (Animation)**：
  - 核心状态指示物（如系统 Logo 旁的标灯），可加入物联网设备“心跳”或“呼吸”动效 (`animation: pulse 2s infinite`)。
  - 数据变动或表格元素推荐带有简单的渐变过渡效果 (`transition: all 0.3s;`)。
- **图标引入**：统一使用 `@element-plus/icons-vue` 的具名导出按需引入。

## 6. 指向 AI 的 Prompt 示例要求
**在未来的所有 Prompt 或系统对话中，要求 AI 生成页面时可带上此句约束：**

> "请参考 `frontend/doc/ai-style-guide.md` 中的工业网络与 B 端后台规范设计您的代码。要求 Vue3 ts，布局应包含大圆角无边框 hover 卡片、等宽字体显式 IP 或核心技术字段代码，以及深浅互补的带色标和 Icon 模块。"