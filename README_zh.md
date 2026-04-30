# ICS-Guard Next

下一代 ICS/OT 网络安全平台 — 面向工业控制系统的实时异常检测与自动化威胁响应。

## 架构

两个团队以消息驱动架构协作：

| 团队 | 范围 | 技术栈 |
|---|---|---|
| A | 后端 + Web 界面 | Java 21 / Spring Boot 3 + Vue 3 / TypeScript |
| B | 控制器 + AI 引擎 | Python 3 / Ryu + ONNX / LightGBM |

团队间通过 **Kafka**（异步数据面）和 **HTTP REST**（同步控制面）进行通信。完整架构和约定见 [CLAUDE.md](CLAUDE.md)。

### 服务

| 服务 | 用途 |
|---|---|
| `redpanda` | Kafka 兼容消息总线 |
| `postgres` | 持久化存储（A 组） |
| `redis` | 会话、限流、缓存 |
| `kafka-init` | 启动时自动创建 6 个 Kafka 主题 |
| `app-backend` | REST API、RBAC、策略配置、告警管理 |
| `web-ui` | 仪表盘、拓扑图、规则编辑器 |
| `sdn-controller` | 流表管理、流量镜像 |
| `inference-engine` | 三级级联异常检测 |
| `device-simulator` | ICS 设备仿真（可选，`--profile simulation`） |

## 快速启动

```bash
# 克隆并配置环境
git clone <repo-url> && cd ICS-guard-next
cp .env.example .env

# 启动全部服务
docker compose up -d

# 带设备模拟器启动
docker compose --profile simulation up -d

# 停止
docker compose down
```

## 本地开发

基础设施在 Docker 中运行，业务服务在宿主机上以 IDE 调试模式运行。

### 环境要求

- **Java 21** — 推荐使用 [Eclipse Temurin](https://adoptium.net/)
- **Node.js 22+** — Vue 前端构建
- **Python 3.11+** — B 组服务
- **Docker** — 运行基础设施容器

### 初始化

```bash
cp .env.example .env
```

### 启动基础设施

```bash
docker compose up -d redpanda postgres redis kafka-init

# 验证
docker compose ps
```

四个容器都应显示 `healthy` 或 `Up` 状态。

### 后端（A 组）

```bash
cd app-backend

# 首次构建 — 下载依赖
./gradlew build -x test

# 使用 local profile 启动（所有地址指向 localhost）
./gradlew bootRun --args='--spring.profiles.active=local'

# 或在 IDE 中设置 VM options: -Dspring.profiles.active=local
```

`local` profile 启用：`ddl-auto: update`（自动建表）、SQL 日志、DevTools 热加载。

验证：`curl http://localhost:8080/actuator/health`

### 前端（A 组）

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，API 请求代理到 `localhost:8080`。

### B 组服务

```bash
cd sdn-controller
pip install -r requirements.txt
python main.py
```

```bash
cd inference-engine
pip install -r requirements.txt
python main.py
```

端口：Controller `8000`、OpenFlow `6633` / `6653`。

### Docker 模式

如果某个服务不在本地运行，而是打包在 Docker 中：

```bash
# 构建并启动单个服务
docker compose up -d --build app-backend
```

Docker 环境中使用服务名访问（`redpanda`、`postgres`、`redis`）。`.env.example` 提供了两种模式的默认值。

## 项目结构

```
app-backend/          # A 组 — Java Spring Boot
frontend/             # A 组 — Vue 3 + Vite
sdn-controller/       # B 组 — Python Ryu
inference-engine/     # B 组 — Python AI 推理
simulator/            # B 组 — 设备仿真
models/               # B 组 — AI 模型文件
deploy/config/        # 共享 — 环境配置样本
docs/                 # 共享 — 契约、架构、约定
legacy/               # 仅参考 — 旧版代码
```

## Git 工作流

- `main` — 生产稳定分支
- `dev` — 集成分支
- `feature/XXX` — 功能分支，通过 PR 合入 `dev`
- 禁止跨团队提交（A 组不修改 `sdn-controller/`，B 组不修改 `app-backend/`）

## 当前阶段

第一阶段 — 脚手架与开发环境搭建。Docker Compose、目录结构和配置文件已就绪，各服务源码持续实现中。
