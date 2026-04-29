# Kafka 消息队列契约与格式

应对 10Gbps 线速处理的关键在于异步化。本文档规定了关键数据全生命周期在消息中心流转时的 Topic 与 Payload 契约。

> **团队边界**：Kafka 是 Backend+WebUI 侧（Java）与 Controller+Engine 侧（Python）之间的集成边界。
> 本文档的 Mock 格式供双方评审确认后进入开发。Topic 命名遵循 `ics.<domain>.<entity>` 规范。

## 1. 流量特征生产队列
**通信链路**：SDN Controller (探针) ➡️ Kafka ➡️ AI Inference-Engine
**Topic Name**：`ics.traffic.features`
**用途**：Controller 对镜像流量包完成 DPI 深层解析（免存盘），提取特征向量并发往 AI 引擎进行异常检测。

**Mock Message Format:**
```json
{
  "trace_id": "pkt-uuid-8888",
  "timestamp": 1714301234567,
  "datapath_id": "0000000000000001",
  "src_ip": "192.168.10.5",
  "dst_ip": "192.168.10.100",
  "src_port": 45321,
  "dst_port": 502,
  "protocol": "ModbusTCP",
  "features": {
    "flow_duration_ms": 1500,
    "packet_rate": 500,
    "byte_rate": 10240,
    "modbus_func_code": 15,
    "illegal_addr_access_count": 0,
    "retransmission_count": 2
  }
}
```

## 2. 威胁告警事件队列
**通信链路**：AI Inference-Engine ➡️ Kafka ➡️ App-Backend
**Topic Name**：`ics.threat.alerts`
**用途**：当 AI 引擎认为特征序列命中三层级联异常判定后，立刻产出告警消息，交由后端处理持久化与可视化分发。引擎保持无状态。

**Mock Message Format:**
```json
{
  "alert_id": "alt-uuid-9999",
  "trace_id": "pkt-uuid-8888", 
  "timestamp": 1714301234580,
  "severity": "CRITICAL",
  "category": "Zero-Day Anomaly",
  "confidence_score": 0.958,
  "src_ip": "192.168.10.5",
  "dst_ip": "192.168.10.100",
  "evidence": {
    "layer_triggered": "DBSCAN_Clustering",
    "distance_to_baseline": 4.5
  },
  "suggested_action": "ISOLATE_NODE"
}
```

## 3. 拓扑与节点状态事件队列
**通信链路**：SDN Controller ➡️ Kafka ➡️ App-Backend
**Topic Name**：`ics.topology.events`
**用途**：Controller 通过 OpenFlow 事件（`EventOFPStateChange`、`EventSwitchEnter`/`EventSwitchLeave`）探测到节点上下线、链路状态变化、新设备发现时，实时推送至 Backend。Backend 消费后更新 PostgreSQL 并推送到 Web UI。

**Mock Message Format:**
```json
{
  "event_id": "e1f2g3h4-i5j6-7890-1234-56789abcdef0",
  "event_type": "node_online",
  "timestamp": 1714301234567,
  "node_id": "0000000000000001",
  "node_name": "access-switch-zone-a",
  "node_type": "switch",
  "node_ip": "192.168.1.1",
  "datapath_id": "0000000000000001"
}
```

**event_type 枚举：**
| 值 | 说明 |
|---|------|
| `node_online` | 交换机/设备上线（MAIN_DISPATCHER） |
| `node_offline` | 交换机/设备离线（DEAD_DISPATCHER） |
| `link_up` | 链路连通 |
| `link_down` | 链路断开 |
| `device_discovered` | 被动发现新设备（如通过流量指纹识别） |
| `device_removed` | 设备长期离线被移除 |

## 4. 聚合流量指标队列
**通信链路**：SDN Controller ➡️ Kafka ➡️ App-Backend
**Topic Name**：`ics.traffic.metrics`
**用途**：Controller 定期（如每 5 秒）通过 OpenFlow PortStats/FlowStats 轮询交换机，聚合为节点和端口级流量指标后推送至 Backend，供 Web UI 仪表盘展示实时流量。**注意：这是聚合后的低频 topic，与 `ics.traffic.features`（每包/每流级别的极高频 topic）互补。**

**Mock Message Format:**
```json
{
  "timestamp": 1714301235000,
  "node_id": "0000000000000001",
  "datapath_id": "0000000000000001",
  "ports": [
    {
      "port_no": 1,
      "tx_rate_mbps": 320.5,
      "rx_rate_mbps": 85.4,
      "tx_bytes_total": 1234567890123,
      "rx_bytes_total": 9876543210987,
      "packet_loss_pct": 0.01
    }
  ],
  "flows": [
    {
      "flow_key": "192.168.1.10:502->10.0.0.5:12345-TCP",
      "pkt_rate": 500.0,
      "byte_rate": 10240.0,
      "status": "active"
    }
  ],
  "flow_count": 42
}
```

## 5. 模型与阈值管理队列
**通信链路**：App-Backend ➡️ Kafka ➡️ AI Inference-Engine
**Topic Name**：`ics.model.events`
**用途**：运维人员在 Web UI 部署新模型或调整检测阈值后，Backend 通过此 topic 通知 Engine 热加载模型或更新阈值配置，避免重启容器。

**Mock Message Format:**
```json
{
  "event_id": "f1a2b3c4-d5e6-7890-1234-56789abcdef0",
  "event_type": "model_update",
  "timestamp": 1714305000000,
  "model_version": "251225_105021",
  "model_path": "/models/lightgbm_model.pkl",
  "operator": "admin"
}
```

**event_type 枚举：**
| 值 | 说明 |
|---|------|
| `model_update` | 新模型部署，Engine 须热加载 |
| `threshold_update` | 检测阈值调整（随消息携带 `thresholds` 字段） |
| `reload_config` | 通用配置重载（非模型非阈值的其他配置） |

## 6. 全局审计与操作日志队列

**通信链路**：App-Backend ➡️ Kafka ➡️ ElasticSearch/Logstash (后期架构)
**Topic Name**：`ics.audit.logs`
**用途**：操作员下发规则、登录、RBAC 授权等，均发送审计日志，满足等保与关键信息基础设施的等保合规要求。

**Mock Message Format:**
```json
{
  "log_id": "audit-20260429-001",
  "timestamp": 1714301234567,
  "operator": "admin",
  "action": "RULE_CREATE",
  "resource": "rule",
  "resource_id": "rule-uuid-0001",
  "status": "success",
  "details": "Created blocking rule 'Block-Modbus-Scanner' for Modbus TCP 502",
  "client_ip": "192.168.1.100"
}
```