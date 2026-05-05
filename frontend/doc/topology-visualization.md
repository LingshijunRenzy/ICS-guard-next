# 水电厂拓扑可视化开发方案

## 1. 总体架构

### 1.1 菜单结构变更

当前单页面 `/topology`（TopologyView.vue）拆分为**三级菜单**：

```
网络拓扑 (ShareIcon)
├── 拓扑视图   /topology/view      ← 新增：3D 可视化画布（本文档核心）
├── 设备列表   /topology/devices    ← 迁移：当前设备卡片页
└── 拓扑事件   /topology/events     ← 迁移：当前事件表格页
```

### 1.2 三级菜单路由规划

| 菜单项 | 路由 path | Vue 组件 | 来源 |
|--------|-----------|---------|------|
| 拓扑视图 | `/topology/view` | `TopologyView.vue` | **重写**为 3D 可视化画布 |
| 设备列表 | `/topology/devices` | `DeviceListView.vue` | **新建**，从原 TopologyView 抽取设备卡片部分 |
| 拓扑事件 | `/topology/events` | `EventListView.vue` | **新建**，从原 TopologyView 抽取事件表格部分 |

### 1.3 复用逻辑

- `DeviceListView.vue` 和 `EventListView.vue` 直接复用原 `TopologyView.vue` 中的 `<script setup>` 逻辑（fetchDevices、fetchEvents、selectDevice 等），不改造后端 API
- 原 `TopologyView.vue` **保留文件名但全部重写**，改为拓扑可视化 SVG 画布

## 2. 技术路线

- **零额外依赖**：纯 CSS + SVG + HTML
- **SVG 画布**：用于绘制横平竖直的折线（正交连线）
- **HTML 绝对定位**：每个设备节点用 `<div>` 呈现在 SVG 上方，方便复用 TDesign 组件样式和交互事件
- **3D 体块效果**：设备节点通过 CSS `box-shadow` 和渐变模拟三维立体感

## 3. 布局方案

### 画布尺寸

- **宽 × 高**：1400 × 960 px
- **背景色**：深蓝科技渐变（`linear-gradient(180deg, #0a1628 0%, #132043 100%)`），类似 legacy 的暗色背景

### 区域位置

整个画面模拟水电厂俯视平面图，6 个区域按物理布局排列：

```
┌─────────────────────────────────────────────────────────────────┐
│                         控制中心 (Y: 0~140)                      │
│                    ┌── SCADA ──┐  ┌── HMI ──┐  ┌── WS ──┐     │
│                    └───────────┘  └──────────┘  └────────┘     │
│                                                                 │
│   ┌─ 机组1 ─┐      ┌─ 机组2 ─┐      ┌─ 机组3 ─┐               │
│   │ ┌──┐┌──┐│      │ ┌──┐┌──┐│      │ ┌──┐┌──┐│               │
│   │ │  ││  ││      │ │  ││  ││      │ │  ││  ││               │
│   │ └──┘└──┘│      │ └──┘└──┘│      │ └──┘└──┘│               │
│   │ ┌──┐┌──┐│      │ ┌──┐┌──┐│      │ ┌──┐┌──┐│               │
│   │ │  ││  ││      │ │  ││  ││      │ │  ││  ││               │
│   │ └──┘└──┘│      │ └──┘└──┘│      │ └──┘└──┘│               │
│   └─────────┘      └─────────┘      └─────────┘               │
│                                                                 │
│                    ┌── 开关站 ──┐                                │
│                    └───────────┘                                │
│                    ┌── 公用系统 ─┐                                │
│                    └───────────┘                                │
└─────────────────────────────────────────────────────────────────┘
```

### 区域坐标定义

| 区域 | 左上角 X | 左上角 Y | 宽度 | 高度 | 设备数 |
|------|---------|---------|------|------|--------|
| 控制中心 (Control Room) | 50 | 30 | 920 | 200 | 5 |
| 机组 1 (Unit 1) | 50 | 300 | 350 | 220 | 4 |
| 机组 2 (Unit 2) | 520 | 300 | 350 | 220 | 4 |
| 机组 3 (Unit 3) | 990 | 300 | 350 | 220 | 4 |
| 开关站 (Switchyard) | 520 | 590 | 350 | 170 | 3 |
| 公用系统 (BOP) | 520 | 800 | 350 | 130 | 3 |

> **说明**：机组 2/3 后续扩展时可调整，目前 Y 方向空间充足。

## 4. 设备坐标（固定像素定位）

### 控制中心 (Control Room)

| 设备 ID | 名称 | 类型 | X | Y | 所属区域 |
|---------|------|------|---|----|---------|
| SCADA-MAIN | Main SCADA Server | SCADA | 120 | 100 | control |
| SCADA-BACKUP | Backup SCADA Server | SCADA | 320 | 100 | control |
| ENG-WS | Engineering Workstation | Workstation | 620 | 100 | control |
| HMI-OP-01 | Operator Station 1 | HMI | 170 | 180 | control |
| HMI-OP-02 | Operator Station 2 | HMI | 370 | 180 | control |

### 机组 1 (Unit 1) — 50MW Francis Turbine

| 设备 ID | 名称 | 类型 | X | Y | 所属区域 |
|---------|------|------|---|----|---------|
| PLC-U1-MAIN | Unit 1 Main Controller | PLC | 100 | 350 | unit1 |
| PLC-U1-EXC | Unit 1 Excitation Controller | PLC | 240 | 350 | unit1 |
| PLC-U1-GOV | Unit 1 Governor Controller | PLC | 100 | 450 | unit1 |
| PLC-U1-PROT | Unit 1 Protection Relay | Protection | 240 | 450 | unit1 |

### 机组 2 (Unit 2) — 50MW Francis Turbine

| 设备 ID | 名称 | 类型 | X | Y | 所属区域 |
|---------|------|------|---|----|---------|
| PLC-U2-MAIN | Unit 2 Main Controller | PLC | 570 | 350 | unit2 |
| PLC-U2-EXC | Unit 2 Excitation Controller | PLC | 710 | 350 | unit2 |
| PLC-U2-GOV | Unit 2 Governor Controller | PLC | 570 | 450 | unit2 |
| PLC-U2-PROT | Unit 2 Protection Relay | Protection | 710 | 450 | unit2 |

### 机组 3 (Unit 3) — 50MW Francis Turbine

| 设备 ID | 名称 | 类型 | X | Y | 所属区域 |
|---------|------|------|---|----|---------|
| PLC-U3-MAIN | Unit 3 Main Controller | PLC | 1040 | 350 | unit3 |
| PLC-U3-EXC | Unit 3 Excitation Controller | PLC | 1180 | 350 | unit3 |
| PLC-U3-GOV | Unit 3 Governor Controller | PLC | 1040 | 450 | unit3 |
| PLC-U3-PROT | Unit 3 Protection Relay | Protection | 1180 | 450 | unit3 |

### 开关站 (Switchyard)

| 设备 ID | 名称 | 类型 | X | Y | 所属区域 |
|---------|------|------|---|----|---------|
| RTU-SWYD | Switchyard RTU | RTU | 570 | 630 | switchyard |
| PLC-SWYD-PROT1 | Line Protection Relay 1 | Protection | 680 | 630 | switchyard |
| PLC-SWYD-PROT2 | Line Protection Relay 2 | Protection | 790 | 630 | switchyard |

### 公用系统 (Balance of Plant)

| 设备 ID | 名称 | 类型 | X | Y | 所属区域 |
|---------|------|------|---|----|---------|
| PLC-AUX | Auxiliary Systems PLC | PLC | 570 | 840 | bop |
| RTU-DAM | Dam Monitoring RTU | RTU | 680 | 840 | bop |
| PLC-SERV | Turbine Servo Auxiliary PLC | PLC | 790 | 840 | bop |

## 5. 连线方案（正交折线）

### 连线规则

- 使用 **横平竖直** 的正交折线（Manhattan routing），不出现斜线或曲线
- 每条连线由 **水平→垂直→水平** 三段组成
- 线宽 2px
- 线色：`rgba(0, 168, 112, 0.6)`（半透明绿色，与在线状态一致）
- 数据流向用箭头标记（`marker-end`）

### 连线列表

#### 骨干连线（控制中心 → 各区域）

| 起点 | 终点 | 路由方式 |
|------|------|---------|
| SCADA-MAIN (320,160) | 机组1区域入口 (260,300) | 从上向下折 |
| SCADA-MAIN (320,160) | 机组2区域入口 (695,300) | 从上向下折 |
| SCADA-MAIN (320,160) | 机组3区域入口 (1130,300) | 从上向下折 |
| SCADA-MAIN (320,160) | 开关站入口 (695,590) | 从上向下折 |
| SCADA-MAIN (320,160) | 公用系统入口 (695,800) | 从上向下折 |

#### 区域内连线（控制中心内部）

| 起点 | 终点 |
|------|------|
| SCADA-MAIN (220,160) | HMI-OP-01 (220,180) |
| SCADA-MAIN (370,160) | HMI-OP-02 (420,180) |
| SCADA-MAIN (520,160) | ENG-WS (670,160) |

#### 区域内连线（机组内部）— 以机组1为例，其余同理

| 起点 | 终点 |
|------|------|
| PLC-U1-MAIN (150,400) | PLC-U1-EXC (290,400) |
| PLC-U1-MAIN (150,400) | PLC-U1-GOV (150,450) |
| PLC-U1-EXC (290,400) | PLC-U1-PROT (290,450) |

### SVG 路径格式示例

```
<!-- 控制中心 → 机组1 -->
<path d="M 320,160 L 320,260 L 260,260 L 260,300"
      fill="none" stroke="rgba(0,168,112,0.6)" stroke-width="2"
      marker-end="url(#arrow-green)" />

<!-- 控制中心内部 SCADA→HMI -->
<path d="M 220,160 L 220,180"
      fill="none" stroke="rgba(0,168,112,0.6)" stroke-width="2" />
```

## 6. 设备节点样式（3D 体块效果）

### 每个设备节点渲染为一个立体的「方块」

```
  ┌──────────────────┐
  │  ┌─┐  设备类型    │  ← 类型图标（CpuIcon / UsbIcon / ...）
  │  └─┘  设备名称    │  ← 名称，单行省略
  │        10.10     │  ← IP 简写
  │        ● 在线     │  ← 状态指示
  └──────────────────┘
```

### 3D 体块 CSS 实现

```css
.device-node {
  width: 130px;
  height: 80px;
  background: linear-gradient(135deg, #1a2a4a 0%, #0d1b36 100%);
  border: 1px solid rgba(0, 168, 112, 0.4);
  border-radius: 6px;
  box-shadow:
    4px 4px 0 rgba(0, 168, 112, 0.3),    /* 右侧底面阴影 - 立体感关键 */
    0 2px 8px rgba(0, 0, 0, 0.4);          /* 环境阴影 */
  transform: perspective(800px) rotateY(-2deg) rotateX(2deg); /* 轻微三维旋转 */
  transition: all 0.3s ease;
}

.device-node:hover {
  box-shadow:
    4px 4px 0 rgba(0, 168, 112, 0.5),
    0 4px 16px rgba(0, 168, 112, 0.2);
  transform: perspective(800px) rotateY(-2deg) rotateX(2deg) translateY(-2px);
}

/* 设备离线状态 */
.device-node.offline {
  border-color: rgba(227, 77, 89, 0.4);
  box-shadow:
    4px 4px 0 rgba(227, 77, 89, 0.3),
    0 2px 8px rgba(0, 0, 0, 0.4);
}
```

### 设备类型→图标映射

| 设备类型 | TDesign 图标 |
|---------|-------------|
| PLC | `CpuIcon` |
| SCADA | `ServerIcon` |
| HMI | `DesktopIcon` |
| RTU | `UsbIcon` |
| Protection | `ShieldIcon` |
| Workstation | `LaptopIcon` |

## 7. 交互功能

### 7.1 悬停浮窗 (Hover Tooltip)

- 使用 TDesign `<t-tooltip>` 组件或纯 CSS 浮窗
- 显示完整设备信息：设备 ID、全称、IP、MAC、端口、协议、描述

### 7.2 点击设备

- 复用现有 `selectDevice(deviceId)` 逻辑
- 弹出 TDesign `<t-dialog>` 显示该设备的历史拓扑事件

### 7.3 区域高亮

- 点击区域标题或区域背景区域，该区域所有设备高亮（border-color 加亮 + glow 效果）
- 其他区域设备半透明（opacity: 0.4）

### 7.4 状态动画

- 在线设备：绿色呼吸灯效果（CSS `@keyframes pulse-green`）
- 离线设备：红色固定灯
- 新发现设备：短暂闪烁动画

## 8. 后端适配

当前 `TopologyEventResponse` 缺少 zone 字段。两种方案：

**方案 A（推荐）**：前端维护设备→区域映射表，不改后端
```ts
const DEVICE_ZONE_MAP: Record<string, string> = {
  'SCADA-MAIN': 'control',
  'PLC-U1-MAIN': 'unit1',
  // ... 22 条映射
}
```

**方案 B**：后端 topology devices API 返回时补充 zone 字段。

**方案 C**：后端新增 `/topology/zones` 接口返回分区设备列表。

推荐方案 A，因为设备 ID 是固定的，映射关系稳定，不需要后端改动。

## 9. 路由改造方案

### 9.1 router 变更

将 `/topology` 从单一路由改为**父路由 + 三级子路由**：

```ts
// 当前（单页面路由）
{
  path: 'topology',
  name: 'Topology',
  component: () => import('@/views/TopologyView.vue'),
  meta: { title: 'Topology' },
}

// 改造后（三级菜单路由）
{
  path: 'topology',
  redirect: '/topology/view',   // 默认指向拓扑视图
  children: [
    {
      path: 'view',
      name: 'TopologyView',
      component: () => import('@/views/TopologyView.vue'),
      meta: { title: 'Topology View' },
    },
    {
      path: 'devices',
      name: 'DeviceList',
      component: () => import('@/views/DeviceListView.vue'),
      meta: { title: 'Device List' },
    },
    {
      path: 'events',
      name: 'EventList',
      component: () => import('@/views/EventListView.vue'),
      meta: { title: 'Topo Events' },
    },
  ],
},
```

### 9.2 侧边栏变更（MainLayout.vue）

```vue
<!-- 当前：单菜单项 -->
<t-menu-item value="/topology">
  <template #icon><ShareIcon /></template>
  {{ $t('layout.topology') }}
</t-menu-item>

<!-- 改造后：三级子菜单 -->
<t-submenu :value="'topology-root'" :title="$t('layout.topology')">
  <template #icon><ShareIcon /></template>
  <t-menu-item value="/topology/view">{{ $t('topology.view') }}</t-menu-item>
  <t-menu-item value="/topology/devices">{{ $t('topology.devices') }}</t-menu-item>
  <t-menu-item value="/topology/events">{{ $t('topology.events') }}</t-menu-item>
</t-submenu>
```

### 9.3 auto-expand 逻辑

MainLayout.vue 已有 expandedMenus 的 watch 逻辑，需补充对 `/topology` 路径的处理：

```ts
watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/topology')) {
      if (!expandedMenus.value.includes('topology-root')) {
        expandedMenus.value = [...expandedMenus.value, 'topology-root']
      }
    }
    // 原有的 /users /roles 逻辑保持不变
  },
  { immediate: true },
)
```

## 10. 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/router/index.ts` | **修改** | 将 `/topology` 改为父路由 + 三级子路由 |
| `frontend/src/layouts/MainLayout.vue` | **修改** | 将 Topology 菜单项改为 `<t-submenu>` 三级菜单 + 展开逻辑 |
| `frontend/src/views/TopologyView.vue` | **重写** | 改为拓扑可视化画布 |
| `frontend/src/views/DeviceListView.vue` | **新建** | 从原 TopologyView 抽取设备卡片列表 |
| `frontend/src/views/EventListView.vue` | **新建** | 从原 TopologyView 抽取事件表格 + 设备历史弹窗 |
| `frontend/src/styles/topology.css` | **新建** | 拓扑图专用样式（3D 体块、连线、区域卡片） |
| `frontend/src/locales/zh.ts` | **追加** | 三级菜单名、区域名、设备类型国际化 |
| `frontend/src/locales/en.ts` | **追加** | 对应英文 |

## 11. 实现步骤

1. 创建 `DeviceListView.vue`（从原 TopologyView 迁移设备卡片部分）
2. 创建 `EventListView.vue`（从原 TopologyView 迁移事件表格 + 历史弹窗部分）
3. 修改 `router/index.ts`：将 `/topology` 改为父路由 + 三级子路由
4. 修改 `MainLayout.vue`：拓扑菜单改为三级子菜单 + auto-expand
5. 更新国际化文件（中英文的 topology 相关 key）
6. 在 frontend/src 下创建拓扑配置常量文件（设备坐标、区域配置、连线定义）
7. 新建 `frontend/src/styles/topology.css`（3D 体块、状态色、呼吸动画）
8. 重写 `TopologyView.vue`（SVG + HTML overlay 结构）
9. 手动模拟数据测试渲染效果
10. 与后端真实 API 联调

## 12. 设备汇总

| 类型 | 数量 | 设备列表 |
|------|------|---------|
| PLC | 10 | PLC-U1-MAIN, PLC-U1-EXC, PLC-U1-GOV, PLC-U2-MAIN, PLC-U2-EXC, PLC-U2-GOV, PLC-U3-MAIN, PLC-U3-EXC, PLC-U3-GOV, PLC-AUX, PLC-SERV |
| SCADA | 2 | SCADA-MAIN, SCADA-BACKUP |
| HMI | 2 | HMI-OP-01, HMI-OP-02 |
| RTU | 2 | RTU-SWYD, RTU-DAM |
| Protection | 5 | PLC-U1-PROT, PLC-U2-PROT, PLC-U3-PROT, PLC-SWYD-PROT1, PLC-SWYD-PROT2 |
| Workstation | 1 | ENG-WS |
| **总计** | **22** | |
**注**：PLC 类型的设备 count 中 PLC-U1-MAIN~PLC-U3-PROT 为 12，加上 PLC-AUX 和 PLC-SERV，实际 PLC 类共 12 台。上面 TypeScript 代码实际定义了 22 台设备，此处分类按 device_type 字段统计，包含 PLC、SCADA、HMI、RTU、Protection、Workstation 共 6 类。
