# ICS-Guard 开发团队基础约定

本文档确立了 Dev A（Java/Web 管控层）与 Dev B（Python/AI 流控层）在项目开发过程中的基础协作约定。

## 1. 架构与数据边界协议
为确保极致性能和应用解耦，双方必须遵守以下数据“互不侵犯”原则：
* **数据库隔离**：PostgreSQL 归属 Dev A 全权管理。Dev B 的 Python 组件禁止直接连接数据库读写状态，所有策略获取和告警落库必须通过 Kafka 消息或 HTTP API。
* **模型与特征隔离**：AI 模型（ONNX/LightGBM）及底层离线特征文件归 Dev B 全权管理。Dev A 不需要理解特征工程，只需关心“报警结果”和“阻断策略”。
* **容器隔离**：所有服务必须提供 `Dockerfile`，且双方联调必须能在开发机的 `docker-compose.yml` 中一键拉起，禁止“在我的电脑上能跑”的现象。

## 2. API 设计与联调规范
* **契约优先 (Contract-First)**：新增功能前，必须先在 `api-contracts.md` 或 `kafka-messages.md` (或 Swagger/Apifox) 中定义 Mock 格式，双方评审确认后才能进入开发。
* **时区统一**：所有跨模块产生的时间戳，一律使用 **单调递增的 Unix 毫秒级时间戳 (Long)** 或严格的 **UTC+0 ISO8601 格式**，禁止在报文中携带带本地时区的模糊字符串。
* **唯一标识溯源**：每条流量特征和告警信息必须在流量抓取端生成全局唯一的 `trace_id`（如 UUID），方便跨中间件的日志追踪。

## 3. Git 协作基础工作流
* 坚守 `main`（生产稳定版）和 `dev`（开发集成分支）的双分支模型。
* Dev A 和 Dev B 在各自的 `feature/XXX` 分支开发，通过 PR 合并入 `dev`。
* 严禁无意义的跨层代码提交（如 Dev A 去修改 `inference-engine` 导致环境破坏）。