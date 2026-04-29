# HTTP 接口契约与 Mock 数据

本文档定义了跨网络层面的同步 HTTP API 规范，包含：
- **管控 Backend ↔ SDN Controller**：流表下发、规则推送、拓扑与状态查询、节点控制
- **Web UI ↔ 管控 Backend**：告警、拓扑资产、规则配置等前端交互

响应体统一采用 `{ code, msg, data }` 结构，其中 `code` 为 HTTP 状态码（200 表示成功），`msg` 为描述信息，`data` 为业务数据。

---

## 一、管控 Backend ↔ SDN Controller

各端点均需在 HTTP Header 中携带 `Authorization: Bearer <access_token>`。认证流程参考 [team-conventions.md](team-conventions.md)。

### 1.1 流表管理

#### 下发阻断流表

**Endpoint**: `POST /api/v1/flows/block`
**方向**: App-Backend → SDN Controller
**职责**: 根据告警或人工决策向交换机下发 OpenFlow 阻断规则。

**Mock Request:**
```json
{
  "trace_id": "req-98765-abcd",
  "rule_type": "DROP",
  "priority": 100,
  "match": {
    "src_ip": "192.168.1.100",
    "dst_ip": "192.168.1.20",
    "protocol": "TCP",
    "dst_port": 502
  },
  "duration_seconds": 3600
}
```

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "flow_id": "flow-rule-001",
    "status": "APPLIED",
    "datapath_id": "0000000000000001"
  }
}
```

#### 撤销阻断流表

**Endpoint**: `DELETE /api/v1/flows/block/{flow_id}`
**方向**: App-Backend → SDN Controller
**职责**: 撤销已下发的阻断流表，恢复通信。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "flow_id": "flow-rule-001",
    "status": "REVOKED"
  }
}
```

---

### 1.2 规则管理（Rule）

规则（Rule）是安全策略的核心载体，由用户在 Web UI 中配置，App-Backend 持久化后推送至 SDN Controller 执行。对应 legacy 中的 Policy 概念。

> **元数据边界**：时间信息（`created_at`、`updated_at`）和作者信息（`created_by`、`updated_by`）属于 App-Backend 的管控范畴，用户管理仅在 Backend ↔ Web UI 层面进行，对 SDN Controller 完全不可见。Controller 仅关注规则的执行面字段（匹配条件、动作、优先级、有效期），下文所有 Backend → Controller 的 Rule 结构中已去除这些元数据字段。App-Backend 向前端返回 Rule 时会补充完整的元数据（详见 2.3 节）。

##### 匹配条件的协议扩展设计

`conditions` 字段采用两层分离结构，遵循开闭原则，支持协议类型的持续扩展：

- **`network`**（必填）— 网络/传输层通用匹配条件，所有协议共用
- **`protocol_match`**（可选）— 应用层协议专属匹配条件，通过 `type` 字段区分协议类型，不传时退化为纯 IP/端口级别过滤

`protocol_match` 按 `type` 字段进行 discriminator 分派，Controller 根据 `type` 选择对应的协议匹配器：

| `type` 值 | 协议 | 专属字段 |
|-----------|------|---------|
| `"modbus"` | Modbus/TCP | `func_codes[]`, `unit_id`, `address_range` |
| `"iec104"` | IEC 60870-5-104 | `type_ids[]`, `cot[]`, `common_address`, `ioa_range` |
| `"dnp3"` | DNP3 | `function_codes[]`, `object_groups[]`, `point_range` |
| `"iec61850"` | IEC 61850 | `service_type`, `appid`, `gocb_ref_pattern` |

新增协议只需实现对应的 Matcher 并在 registry 中注册，不触动外层 `conditions` 结构和 Controller 框架代码。

##### 创建规则

**Endpoint**: `POST /api/v1/rules`
**方向**: App-Backend → SDN Controller
**职责**: 在控制器中创建一条新的安全规则。

**Mock Request:**
```json
{
  "rule": {
    "name": "Block-Modbus-Scanner",
    "description": "阻断对 PLC 502 端口的扫描探测",
    "type": "flow",
    "priority": 200,
    "conditions": {
      "network": {
        "src_ip": "192.168.10.0/24",
        "dst_port": 502,
        "transport": "TCP"
      },
      "protocol_match": {
        "type": "modbus",
        "modbus": {
          "func_codes": [1, 3, 15, 16]
        }
      }
    },
    "action": "DROP",
    "duration_seconds": 7200,
    "status": "enabled"
  }
}
```

**Mock Response (201 Created):**
```json
{
  "code": 201,
  "msg": "success",
  "data": {
    "rule_id": "rule-uuid-0001",
    "status": "created"
  }
}
```

#### 列出所有规则

**Endpoint**: `GET /api/v1/rules`
**方向**: App-Backend → SDN Controller
**Query Params**: `?type=flow&status=enabled`

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rules": [
      {
        "rule_id": "rule-uuid-0001",
        "name": "Block-Modbus-Scanner",
        "type": "flow",
        "status": "enabled",
        "priority": 200
      }
    ]
  }
}
```

#### 获取规则详情

**Endpoint**: `GET /api/v1/rules/{rule_id}`
**方向**: App-Backend → SDN Controller

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rule": {
      "rule_id": "rule-uuid-0001",
      "name": "Block-Modbus-Scanner",
      "description": "阻断对 PLC 502 端口的扫描探测",
      "type": "flow",
      "priority": 200,
      "conditions": {
        "network": {
          "src_ip": "192.168.10.0/24",
          "dst_port": 502,
          "transport": "TCP"
        },
        "protocol_match": {
          "type": "modbus",
          "modbus": {
            "func_codes": [1, 3, 15, 16]
          }
        }
      },
      "action": "DROP",
      "duration_seconds": 7200,
      "status": "enabled"
    }
  }
}
```

#### 更新规则

**Endpoint**: `PUT /api/v1/rules/{rule_id}`
**方向**: App-Backend → SDN Controller
**职责**: 更新已存在的规则配置。

**Mock Request:**
```json
{
  "rule": {
    "priority": 150,
    "duration_seconds": 3600,
    "status": "disabled",
    "conditions": {
      "protocol_match": {
        "type": "modbus",
        "modbus": {
          "func_codes": [3, 16]
        }
      }
    }
  }
}
```

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rule_id": "rule-uuid-0001",
    "status": "updated"
  }
}
```

#### 删除规则

**Endpoint**: `DELETE /api/v1/rules/{rule_id}`
**方向**: App-Backend → SDN Controller

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rule_id": "rule-uuid-0001",
    "status": "deleted"
  }
}
```

##### 协议专属条件示例

以下展示不同协议的 `protocol_match` 请求片段：

**IEC 104 — 阻断特定类型的遥控指令：**

```json
{
  "protocol_match": {
    "type": "iec104",
    "iec104": {
      "type_ids": [45, 46],
      "cot": [6, 7],
      "common_address": 1,
      "ioa_range": { "start": 1000, "end": 5000 }
    }
  }
}
```

**DNP3 — 阻断对特定对象组的非法读写：**

```json
{
  "protocol_match": {
    "type": "dnp3",
    "dnp3": {
      "function_codes": [1, 2],
      "object_groups": [10, 12],
      "point_range": { "start": 0, "end": 100 }
    }
  }
}
```

**IEC 61850 — 阻断伪造的 GOOSE 跳闸报文：**

```json
{
  "protocol_match": {
    "type": "iec61850",
    "iec61850": {
      "service_type": "GOOSE",
      "appid": 0x1000,
      "gocb_ref_pattern": "simpleIOGenericIO/LLN0$GO$*"
    }
  }
}
```

#### 应用规则到目标

**Endpoint**: `POST /api/v1/rules/{rule_id}/apply`
**方向**: App-Backend → SDN Controller
**职责**: 将已创建的规则部署到指定节点、链路或流，由控制器转换为 OpenFlow 流表下发。

**Mock Request:**
```json
{
  "target_nodes": ["node-plc-1", "node-plc-2"],
  "target_links": [],
  "target_flows": ["192.168.1.100:502->10.0.0.5:*"]
}
```

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rule_id": "rule-uuid-0001",
    "status": "applied",
    "applied_count": {
      "nodes": 2,
      "links": 0,
      "flows": 1
    }
  }
}
```

#### 撤销已应用规则

**Endpoint**: `POST /api/v1/rules/{rule_id}/revoke`
**方向**: App-Backend → SDN Controller
**职责**: 撤销规则在指定目标上的应用，控制器移除对应流表。

**Mock Request:**
```json
{
  "target_nodes": ["node-plc-1", "node-plc-2"],
  "target_links": [],
  "target_flows": ["192.168.1.100:502->10.0.0.5:*"]
}
```

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rule_id": "rule-uuid-0001",
    "status": "revoked",
    "revoked_count": {
      "nodes": 2,
      "links": 0,
      "flows": 1
    }
  }
}
```

---

### 1.3 拓扑与状态

#### 获取完整网络拓扑

**Endpoint**: `GET /api/v1/topology`
**方向**: App-Backend → SDN Controller
**职责**: 获取控制器当前感知的完整网络拓扑（所有节点与链路）。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "nodes": [
      {
        "id": "0000000000000001",
        "name": "access-switch-zone-a",
        "type": "switch",
        "ip": "192.168.1.1",
        "status": "online",
        "datapath_id": "0000000000000001"
      },
      {
        "id": "node-plc-1",
        "name": "PLC-01",
        "type": "plc",
        "ip": "192.168.1.20",
        "status": "online"
      }
    ],
    "links": [
      {
        "id": "link-001",
        "source": "0000000000000001",
        "target": "node-plc-1",
        "src_port": 1,
        "dst_port": 2,
        "bandwidth_mbps": 1000,
        "status": "active"
      }
    ]
  }
}
```

#### 获取节点状态

**Endpoint**: `GET /api/v1/nodes/{node_id}/status`
**方向**: App-Backend → SDN Controller

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "node_id": "node-plc-1",
    "status": "online",
    "last_updated": "2026-04-29T10:30:00Z",
    "metrics": {
      "cpu_usage": 35.2,
      "memory_usage": 62.1,
      "network_throughput_mbps": 85.4
    }
  }
}
```

#### 获取连接状态

**Endpoint**: `GET /api/v1/links/{link_id}/status`
**方向**: App-Backend → SDN Controller

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "link_id": "link-001",
    "status": "active",
    "last_updated": "2026-04-29T10:30:00Z",
    "metrics": {
      "bandwidth_usage_mbps": 320.5,
      "latency_ms": 1.2,
      "packet_loss_pct": 0.01
    }
  }
}
```

#### 获取节点性能统计

**Endpoint**: `GET /api/v1/nodes/stats`
**方向**: App-Backend → SDN Controller
**Query Params**: `?start_time=2026-04-29T00:00:00Z&end_time=2026-04-29T10:30:00Z`

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "stats": [
      {
        "node_id": "node-plc-1",
        "avg_cpu": 34.1,
        "avg_memory": 60.5,
        "avg_throughput_mbps": 80.2,
        "sample_count": 120
      }
    ]
  }
}
```

#### 获取连接性能统计

**Endpoint**: `GET /api/v1/links/stats`
**方向**: App-Backend → SDN Controller
**Query Params**: `?start_time=2026-04-29T00:00:00Z&end_time=2026-04-29T10:30:00Z`

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "stats": [
      {
        "link_id": "link-001",
        "avg_bandwidth_mbps": 300.1,
        "avg_latency_ms": 1.3,
        "avg_packet_loss_pct": 0.02,
        "sample_count": 120
      }
    ]
  }
}
```

---

### 1.4 节点与链路控制

这些端点用于运维人员通过前端手动控制网络节点和链路状态，App-Backend 仅做权限验证后透传至 Controller。

#### 节点操作

| 端点 | 方法 | 职责 |
|------|------|------|
| `/api/v1/nodes/{node_id}/start` | POST | 启动节点 |
| `/api/v1/nodes/{node_id}/stop` | POST | 停止节点 |
| `/api/v1/nodes/{node_id}/restart` | POST | 重启节点 |

**Mock Response（以 start 为例，200 OK）:**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "node_id": "node-plc-1",
    "action": "start",
    "status": "started"
  }
}
```

#### 链路操作

| 端点 | 方法 | 职责 |
|------|------|------|
| `/api/v1/links/{link_id}/enable` | POST | 启用连接 |
| `/api/v1/links/{link_id}/disable` | POST | 禁用连接 |

**Mock Response（以 disable 为例，200 OK）:**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "link_id": "link-001",
    "action": "disable",
    "status": "disabled"
  }
}
```

---

### 1.5 交换机管理（Switch/Datapath）

交换机是 OpenFlow 流表的实际承载设备。以下端点用于查询交换机详情、当前流表项及端口统计，支撑运维排查策略是否生效。

#### 列出所有交换机

**Endpoint**: `GET /api/v1/switches`
**方向**: App-Backend → SDN Controller
**职责**: 获取 Controller 当前管理的所有交换机/Datapath 列表。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "switches": [
      {
        "dpid": "0000000000000001",
        "manufacturer": "Huawei",
        "hardware": "S6730-H48X6C",
        "software": "V200R020C10",
        "serial_number": "2102351XXX",
        "ports": 54,
        "status": "connected",
        "connected_since": "2026-04-29T10:00:00Z"
      }
    ]
  }
}
```

#### 获取交换机详情

**Endpoint**: `GET /api/v1/switches/{dpid}`
**方向**: App-Backend → SDN Controller
**职责**: 获取单个交换机的详细信息，包括端口列表和能力集。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dpid": "0000000000000001",
    "manufacturer": "Huawei",
    "hardware": "S6730-H48X6C",
    "software": "V200R020C10",
    "serial_number": "2102351XXX",
    "capabilities": ["FLOW_STATS", "TABLE_STATS", "PORT_STATS", "GROUP_STATS", "METER"],
    "ports": [
      {
        "port_no": 1,
        "name": "GigabitEthernet0/0/1",
        "mac": "00:1A:2B:3C:4D:01",
        "speed_mbps": 1000,
        "status": "up",
        "tx_bytes": 1234567890,
        "rx_bytes": 9876543210,
        "tx_packets": 1500000,
        "rx_packets": 8500000
      }
    ],
    "status": "connected",
    "connected_since": "2026-04-29T10:00:00Z"
  }
}
```

#### 查询交换机流表项

**Endpoint**: `GET /api/v1/switches/{dpid}/flows`
**方向**: App-Backend → SDN Controller
**职责**: 查询指定交换机上当前已下发的 OpenFlow 流表项。用于运维排查策略是否真正生效。

**Query Params**: `?table_id=0&priority_min=100`

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dpid": "0000000000000001",
    "flow_count": 3,
    "flows": [
      {
        "flow_id": "flow-entry-uuid-001",
        "table_id": 0,
        "priority": 200,
        "match": {
          "src_ip": "192.168.1.100",
          "dst_ip": "192.168.1.20",
          "protocol": "TCP",
          "dst_port": 502
        },
        "action": "DROP",
        "duration_seconds": 3600,
        "packet_count": 145,
        "byte_count": 34800,
        "status": "active"
      }
    ]
  }
}
```

#### 查询交换机端口统计

**Endpoint**: `GET /api/v1/switches/{dpid}/ports`
**方向**: App-Backend → SDN Controller
**职责**: 获取指定交换机的所有端口实时统计信息。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dpid": "0000000000000001",
    "ports": [
      {
        "port_no": 1,
        "name": "GigabitEthernet0/0/1",
        "status": "up",
        "tx_bytes": 1234567890,
        "rx_bytes": 9876543210,
        "tx_pps": 3500,
        "rx_pps": 12000,
        "errors": 2,
        "dropped": 0
      }
    ]
  }
}
```

---

### 1.6 Meter 表管理（限速）

Meter 表是 OpenFlow 实现 THROTTLE（限速）响应的技术基础。当检测到中威胁等级事件时，策略引擎生成限速 Meter，Controller 下发至交换机。

#### 创建 Meter 表

**Endpoint**: `POST /api/v1/meters`
**方向**: App-Backend → SDN Controller
**职责**: 在指定交换机上创建 Meter 表，用于限速控制。

**Mock Request:**
```json
{
  "dpid": "0000000000000001",
  "meter": {
    "name": "throttle-scanner-502",
    "flags": ["KBPS", "BURST"],
    "bands": [
      {
        "type": "DROP",
        "rate": 100,
        "burst_size": 10
      }
    ]
  }
}
```

**Mock Response (201 Created):**
```json
{
  "code": 201,
  "msg": "success",
  "data": {
    "meter_id": "meter-uuid-0001",
    "dpid": "0000000000000001",
    "status": "created"
  }
}
```

#### 列出 Meter 表

**Endpoint**: `GET /api/v1/meters`
**方向**: App-Backend → SDN Controller
**Query Params**: `?dpid=0000000000000001`

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "meters": [
      {
        "meter_id": "meter-uuid-0001",
        "dpid": "0000000000000001",
        "name": "throttle-scanner-502",
        "rate": 100,
        "packet_count": 25000,
        "byte_count": 1500000,
        "status": "active"
      }
    ]
  }
}
```

#### 删除 Meter 表

**Endpoint**: `DELETE /api/v1/meters/{meter_id}`
**方向**: App-Backend → SDN Controller

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "meter_id": "meter-uuid-0001",
    "status": "deleted"
  }
}
```

---

### 1.7 响应效果评估

响应效果评估是安全防护闭环的关键环节。下发流表或规则后，Backend 通过以下端点查询实际生效效果，用于评估响应是否达到预期并指导策略优化。

#### 查询流表生效效果

**Endpoint**: `GET /api/v1/flows/{flow_id}/effect`
**方向**: App-Backend → SDN Controller
**职责**: 查询已下发流表的实际命中统计，评估阻断是否生效。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "flow_id": "flow-rule-001",
    "packet_count": 1523,
    "byte_count": 365520,
    "last_hit": "2026-04-29T10:35:12Z",
    "duration_active_seconds": 312,
    "applied_on": [
      {
        "dpid": "0000000000000001",
        "table_id": 0
      }
    ]
  }
}
```

#### 查询规则生效效果

**Endpoint**: `GET /api/v1/rules/{rule_id}/effect`
**方向**: App-Backend → SDN Controller
**职责**: 汇总规则在所有目标交换机上的整体生效效果。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rule_id": "rule-uuid-0001",
    "total_packet_count": 4521,
    "total_byte_count": 1085040,
    "affected_nodes": 2,
    "affected_links": 0,
    "affected_flows": 1,
    "breakdown": [
      {
        "dpid": "0000000000000001",
        "flow_id": "flow-entry-uuid-001",
        "packet_count": 2500,
        "byte_count": 600000
      },
      {
        "dpid": "0000000000000002",
        "flow_id": "flow-entry-uuid-005",
        "packet_count": 2021,
        "byte_count": 485040
      }
    ]
  }
}
```

---

### 1.8 WebSocket 实时推送

Controller 通过 WebSocket 向 App-Backend 推送实时流量统计和拓扑变更事件，支撑前端大屏的实时监控能力。与 REST 接口不同，WebSocket 是长连接单向推送通道。

**连接方式**:
- **地址**: `ws://{controller_host}:8000/api/v1/ws/events`
- **认证**: 连接时在 Query String 中携带 `?token=<access_token>`，或在建立连接后发送 `{"type":"auth","token":"<access_token>"}`
- **心跳**: 客户端每 30 秒发送 `{"type":"ping"}`，服务端回应 `{"type":"pong"}`

#### 消息类型

**1. 实时流量统计 (`traffic_snapshot`)**

Controller 每 5 秒推一次全网流量快照：

```json
{
  "type": "traffic_snapshot",
  "timestamp": "2026-04-29T10:30:05Z",
  "data": {
    "total_throughput_mbps": 850.3,
    "total_pps": 125000,
    "switches": [
      {
        "dpid": "0000000000000001",
        "throughput_mbps": 450.1,
        "pps": 65000,
        "active_flows": 12
      }
    ],
    "top_talkers": [
      {
        "node_id": "node-plc-1",
        "throughput_mbps": 120.5,
        "top_protocol": "Modbus/TCP"
      }
    ]
  }
}
```

**2. 拓扑变更事件 (`topology_change`)**

当交换机上线/下线或链路状态变更时立即推送：

```json
{
  "type": "topology_change",
  "timestamp": "2026-04-29T10:32:00Z",
  "data": {
    "event": "link_down",
    "link_id": "link-001",
    "source": "0000000000000001",
    "target": "node-plc-1",
    "previous_status": "active",
    "current_status": "inactive"
  }
}
```

**3. 节点状态变更 (`node_status_change`)**

```json
{
  "type": "node_status_change",
  "timestamp": "2026-04-29T10:33:15Z",
  "data": {
    "node_id": "node-plc-1",
    "previous_status": "online",
    "current_status": "offline",
    "reason": "heartbeat_timeout"
  }
}
```

---

### 1.9 健康检查

Controller 健康检查端点，用于容器编排的存活和就绪探测。

#### 存活检查

**Endpoint**: `GET /api/v1/health`
**方向**: App-Backend / 容器编排 → SDN Controller
**职责**: 返回 Controller 进程是否在运行。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "status": "alive",
    "uptime_seconds": 86400,
    "version": "1.0.0"
  }
}
```

#### 就绪检查

**Endpoint**: `GET /api/v1/health/ready`
**方向**: App-Backend / 容器编排 → SDN Controller
**职责**: 返回 Controller 是否已就绪接收请求（如与交换机的 OpenFlow 连接已建立）。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "status": "ready",
    "openflow_connections": 3,
    "db_connected": true
  }
}
```

> 注意：`/health` 和 `/health/ready` 一般无需 Authorization Header，由容器编排层直接探测。

---

## 二、Web UI ↔ 管控 Backend

### 2.1 威胁告警列表

**Endpoint**: `GET /api/v1/alerts`
**职责**: 前端大屏展示历史告警记录。

**Query Params**: `?page=1&size=10&severity=HIGH`

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 1,
    "items": [
      {
        "alert_id": "alt-uuid-1111",
        "timestamp": 1714300000000,
        "severity": "HIGH",
        "attack_type": "Modbus Function Code Abuse",
        "src_ip": "192.168.1.55",
        "dst_ip": "10.0.0.8",
        "status": "UNHANDLED",
        "description": "检测到高频非法的 Modbus 写多个寄存器操作 (FC: 16)"
      }
    ]
  }
}
```

### 2.2 拓扑与资产发现

**Endpoint**: `GET /api/v1/topology/devices`
**职责**: 获取当前工厂/车间的设备资产列表。

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "device_id": "dev-001",
      "ip_address": "192.168.1.20",
      "mac_address": "00:1A:2B:3C:4D:5E",
      "device_type": "PLC",
      "vendor": "Siemens",
      "status": "ONLINE",
      "last_seen": 1714305000000
    }
  ]
}
```

### 2.3 规则配置界面

前端规则管理页面对应的接口。**与 1.2 节的关键区别**：Web UI ↔ App-Backend 之间的 Rule 结构会携带时间与作者元数据（`created_by`、`updated_by`、`created_at`、`updated_at`），这些字段由 App-Backend 在持久化层自动维护。App-Backend 向 SDN Controller 转发时剥离元数据，Controller 看到的是 1.2 节中不含元数据的执行面结构。

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/rules` | GET | 规则列表（分页），返回含元数据的 Rule |
| `/api/v1/rules` | POST | 创建规则，`created_by` 由 Backend 从当前登录会话注入 |
| `/api/v1/rules/{rule_id}` | GET | 获取规则详情，含完整元数据 |
| `/api/v1/rules/{rule_id}` | PUT | 更新规则，Backend 自动更新 `updated_by` 和 `updated_at` |
| `/api/v1/rules/{rule_id}` | DELETE | 删除规则 |

**请求/响应处理流程**：前端发出请求 → App-Backend 做权限校验 + 审计日志 → 剥离 `created_by`/`updated_by`/`created_at`/`updated_at` → 转发至 SDN Controller → Controller 返回执行结果 → App-Backend 补充元数据后返回前端。

**Mock Response — 规则列表（GET /api/v1/rules，Web→Backend）:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 5,
    "items": [
      {
        "rule_id": "rule-uuid-0001",
        "name": "Block-Modbus-Scanner",
        "type": "flow",
        "status": "enabled",
        "priority": 200,
        "created_by": "admin",
        "created_at": "2026-04-29T10:30:00Z",
        "updated_by": "operator-zhang",
        "updated_at": "2026-04-29T14:20:00Z"
      }
    ]
  }
}
```

**Mock Request — 创建规则（POST /api/v1/rules，Web→Backend）:**

```json
{
  "rule": {
    "name": "Block-Modbus-Scanner",
    "description": "阻断对 PLC 502 端口的扫描探测",
    "type": "flow",
    "priority": 200,
    "conditions": {
      "network": {
        "src_ip": "192.168.10.0/24",
        "dst_port": 502,
        "transport": "TCP"
      },
      "protocol_match": {
        "type": "modbus",
        "modbus": {
          "func_codes": [1, 3, 15, 16]
        }
      }
    },
    "action": "DROP",
    "duration_seconds": 7200,
    "status": "enabled"
  }
}
```
> 注意：`created_by` 由 Backend 从 JWT 中提取当前用户身份自动注入，前端无需传递。

**Mock Response — 规则详情（GET /api/v1/rules/{rule_id}，Web→Backend）:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rule": {
      "rule_id": "rule-uuid-0001",
      "name": "Block-Modbus-Scanner",
      "description": "阻断对 PLC 502 端口的扫描探测",
      "type": "flow",
      "priority": 200,
      "conditions": {
        "network": {
          "src_ip": "192.168.10.0/24",
          "dst_port": 502,
          "transport": "TCP"
        },
        "protocol_match": {
          "type": "modbus",
          "modbus": {
            "func_codes": [1, 3, 15, 16]
          }
        }
      },
      "action": "DROP",
      "duration_seconds": 7200,
      "status": "enabled",
      "created_by": "admin",
      "created_at": "2026-04-29T10:30:00Z",
      "updated_by": "operator-zhang",
      "updated_at": "2026-04-29T14:20:00Z"
    }
  }
}
```

### 2.4 审计日志查询（预留）

**Endpoint**: `GET /api/v1/audit/logs`
**Query Params**: `?page=1&size=20&action=POLICY_CREATE&start_time=...&end_time=...`

**Mock Response (200 OK):**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 156,
    "items": [
      {
        "log_id": "audit-001",
        "timestamp": "2026-04-29T10:30:00Z",
        "operator": "admin",
        "action": "RULE_CREATE",
        "resource": "rule",
        "resource_id": "rule-uuid-0001",
        "status": "success"
      }
    ]
  }
}
```

---

## 端点汇总

| # | 端点 | 方法 | 方向 | 分类 | 备注 |
|---|------|------|------|------|------|
| 1 | `/api/v1/flows/block` | POST | Backend→Controller | 流表 | |
| 2 | `/api/v1/flows/block/{flow_id}` | DELETE | Backend→Controller | 流表 | |
| 3 | `/api/v1/flows/{flow_id}/effect` | GET | Backend→Controller | 流表 | 效果评估（1.7 节） |
| 4 | `/api/v1/rules` | GET | Backend→Controller | 规则 | 不含元数据 |
| 5 | `/api/v1/rules` | POST | Backend→Controller | 规则 | 不含元数据 |
| 6 | `/api/v1/rules/{rule_id}` | GET | Backend→Controller | 规则 | 不含元数据 |
| 7 | `/api/v1/rules/{rule_id}` | PUT | Backend→Controller | 规则 | 不含元数据 |
| 8 | `/api/v1/rules/{rule_id}` | DELETE | Backend→Controller | 规则 | |
| 9 | `/api/v1/rules/{rule_id}/apply` | POST | Backend→Controller | 规则 | |
| 10 | `/api/v1/rules/{rule_id}/revoke` | POST | Backend→Controller | 规则 | |
| 11 | `/api/v1/rules/{rule_id}/effect` | GET | Backend→Controller | 规则 | 效果评估（1.7 节） |
| 12 | `/api/v1/topology` | GET | Backend→Controller | 拓扑 | |
| 13 | `/api/v1/nodes/{node_id}/status` | GET | Backend→Controller | 状态 | |
| 14 | `/api/v1/nodes/{node_id}/start` | POST | Backend→Controller | 控制 | |
| 15 | `/api/v1/nodes/{node_id}/stop` | POST | Backend→Controller | 控制 | |
| 16 | `/api/v1/nodes/{node_id}/restart` | POST | Backend→Controller | 控制 | |
| 17 | `/api/v1/nodes/stats` | GET | Backend→Controller | 统计 | |
| 18 | `/api/v1/links/{link_id}/status` | GET | Backend→Controller | 状态 | |
| 19 | `/api/v1/links/{link_id}/enable` | POST | Backend→Controller | 控制 | |
| 20 | `/api/v1/links/{link_id}/disable` | POST | Backend→Controller | 控制 | |
| 21 | `/api/v1/links/stats` | GET | Backend→Controller | 统计 | |
| 22 | `/api/v1/switches` | GET | Backend→Controller | 交换机 | |
| 23 | `/api/v1/switches/{dpid}` | GET | Backend→Controller | 交换机 | |
| 24 | `/api/v1/switches/{dpid}/flows` | GET | Backend→Controller | 交换机 | |
| 25 | `/api/v1/switches/{dpid}/ports` | GET | Backend→Controller | 交换机 | |
| 26 | `/api/v1/meters` | GET | Backend→Controller | Meter | |
| 27 | `/api/v1/meters` | POST | Backend→Controller | Meter | |
| 28 | `/api/v1/meters/{meter_id}` | DELETE | Backend→Controller | Meter | |
| 29 | `/api/v1/health` | GET | Backend→Controller | 健康 | 无需认证 |
| 30 | `/api/v1/health/ready` | GET | Backend→Controller | 健康 | 无需认证 |
| 31 | `/api/v1/ws/events` | WS | Backend↔Controller | WebSocket | 实时推送（1.8 节） |
| | | | | | |
| 32 | `/api/v1/rules` | GET | Web→Backend | 规则 | 含元数据（2.3 节） |
| 33 | `/api/v1/rules` | POST | Web→Backend | 规则 | 含元数据（2.3 节） |
| 34 | `/api/v1/rules/{rule_id}` | GET | Web→Backend | 规则 | 含元数据（2.3 节） |
| 35 | `/api/v1/rules/{rule_id}` | PUT | Web→Backend | 规则 | 含元数据（2.3 节） |
| 36 | `/api/v1/rules/{rule_id}` | DELETE | Web→Backend | 规则 | 含元数据（2.3 节） |
| 37 | `/api/v1/topology/devices` | GET | Web→Backend | 资产 | |
| 38 | `/api/v1/alerts` | GET | Web→Backend | 告警 | |
| 39 | `/api/v1/audit/logs` | GET | Web→Backend | 审计 | |
