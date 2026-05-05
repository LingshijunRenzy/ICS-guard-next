# 模拟设备列表

> 项目基于**水电站仿真场景**，共定义 **23 台** ICS 设备，分布在 6 个区域。
>
> 数据来源：`frontend/src/topology/config.ts`（前端拓扑布局） + `temp_mock/devices.py`（模拟器运行时定义）

---

## 一、控制中心（Control Room）— 5 台

| 设备ID | 名称 | 类型 | IP | 端口 | 协议 | 说明 |
|--------|------|------|------|------|------|------|
| SCADA-MAIN | Main SCADA Server | SCADA | 192.168.1.10 | 4840 | OPC UA | Windows Server + KepServerEX |
| SCADA-BACKUP | Backup SCADA Server | SCADA | 192.168.1.11 | 4840 | OPC UA | 热备 SCADA 服务器 |
| HMI-OP-01 | Operator Station 1 | HMI | 192.168.1.20 | 80 | HTTP | 主操作员站 HMI |
| HMI-OP-02 | Operator Station 2 | HMI | 192.168.1.21 | 80 | HTTP | 副操作员站（值班长） |
| ENG-WS | Engineering Workstation | Workstation | 192.168.1.30 | 22 | — | PLC 编程与组态站 |

## 二、机组 1（Unit 1, 50MW 混流式）— 4 台

| 设备ID | 名称 | 类型 | IP | 端口 | 协议 | 说明 |
|--------|------|------|------|------|------|------|
| PLC-U1-MAIN | Unit 1 Main Controller | PLC | 192.168.10.10 | 502 | ModbusTCP | Siemens S7-1500，机组主控 |
| PLC-U1-EXC | Unit 1 Excitation Controller | PLC | 192.168.10.11 | 502 | ModbusTCP | ABB UNITROL 6000 励磁 |
| PLC-U1-GOV | Unit 1 Governor Controller | PLC | 192.168.10.12 | 502 | ModbusTCP | Voith 液压调速器 |
| PLC-U1-PROT | Unit 1 Protection Relay | Protection | 192.168.10.13 | 102 | IEC61850 | SEL-700G 发电机保护 |

## 三、机组 2（Unit 2, 50MW 混流式）— 4 台

| 设备ID | 名称 | 类型 | IP | 端口 | 协议 | 说明 |
|--------|------|------|------|------|------|------|
| PLC-U2-MAIN | Unit 2 Main Controller | PLC | 192.168.20.10 | 502 | ModbusTCP | Siemens S7-1500，机组主控 |
| PLC-U2-EXC | Unit 2 Excitation Controller | PLC | 192.168.20.11 | 502 | ModbusTCP | ABB UNITROL 6000 励磁 |
| PLC-U2-GOV | Unit 2 Governor Controller | PLC | 192.168.20.12 | 502 | ModbusTCP | Voith 液压调速器 |
| PLC-U2-PROT | Unit 2 Protection Relay | Protection | 192.168.20.13 | 102 | IEC61850 | SEL-700G 发电机保护 |

## 四、机组 3（Unit 3, 50MW 混流式）— 4 台

| 设备ID | 名称 | 类型 | IP | 端口 | 协议 | 说明 |
|--------|------|------|------|------|------|------|
| PLC-U3-MAIN | Unit 3 Main Controller | PLC | 192.168.30.10 | 502 | ModbusTCP | Siemens S7-1500，机组主控 |
| PLC-U3-EXC | Unit 3 Excitation Controller | PLC | 192.168.30.11 | 502 | ModbusTCP | ABB UNITROL 6000 励磁 |
| PLC-U3-GOV | Unit 3 Governor Controller | PLC | 192.168.30.12 | 502 | ModbusTCP | Voith 液压调速器 |
| PLC-U3-PROT | Unit 3 Protection Relay | Protection | 192.168.30.13 | 102 | IEC61850 | SEL-700G 发电机保护 |

## 五、开关站（Switchyard）— 3 台

| 设备ID | 名称 | 类型 | IP | 端口 | 协议 | 说明 |
|--------|------|------|------|------|------|------|
| RTU-SWYD | Switchyard RTU | RTU | 192.168.100.10 | 20000 | DNP3 | SEL RTAC，DNP3 上报 SCADA |
| PLC-SWYD-PROT1 | Line Protection Relay 1 | Protection | 192.168.100.11 | 102 | IEC61850 | SEL-751 馈线保护 |
| PLC-SWYD-PROT2 | Line Protection Relay 2 | Protection | 192.168.100.12 | 102 | IEC61850 | SEL-751 馈线保护 |

## 六、公用系统（BOP, Balance of Plant）— 3 台

| 设备ID | 名称 | 类型 | IP | 端口 | 协议 | 说明 |
|--------|------|------|------|------|------|------|
| PLC-AUX | Auxiliary Systems PLC | PLC | 192.168.200.10 | 502 | ModbusTCP | 冷却水、排水、暖通 |
| RTU-DAM | Dam Monitoring RTU | RTU | 192.168.200.20 | 20000 | DNP3 | 水位、闸门、溢洪道监测 |
| PLC-SERV | Turbine Servo Auxiliary PLC | PLC | 192.168.200.30 | 502 | ModbusTCP | 液压油、轴承温度监控 |

---

## 汇总

| 区域 | 设备数 | 网段 | 包含类型 |
|------|--------|------|----------|
| 控制中心 | 5 | 192.168.1.0/24 | SCADA、HMI、Workstation |
| 机组 1 | 4 | 192.168.10.0/24 | PLC、Protection |
| 机组 2 | 4 | 192.168.20.0/24 | PLC、Protection |
| 机组 3 | 4 | 192.168.30.0/24 | PLC、Protection |
| 开关站 | 3 | 192.168.100.0/24 | RTU、Protection |
| 公用系统 | 3 | 192.168.200.0/24 | PLC、RTU |
| **合计** | **23** | — | — |

### 设备类型分布

| 类型 | 数量 | 说明 |
|------|------|------|
| PLC | 10 | Siemens S7-1500 机组主控/励磁/调速器/辅控 |
| Protection | 6 | SEL 700G/751 保护继电器 |
| SCADA | 2 | 主备 SCADA 服务器 |
| RTU | 2 | 开关站/大坝 RTU |
| HMI | 2 | 操作员站 |
| Workstation | 1 | 工程师站 |

### 通信协议

| 协议 | 使用设备 |
|------|----------|
| ModbusTCP (502) | 全部 PLC 设备 |
| IEC61850 (102) | 全部 Protection 设备 |
| DNP3 (20000) | RTU-SWYD、RTU-DAM |
| OPC UA (4840) | SCADA-MAIN、SCADA-BACKUP |
| HTTP (80) | HMI-OP-01、HMI-OP-02 |

---

> **注**：模拟运行时设备状态动态变化，模拟器会按场景随机切换设备在线/离线/告警状态。设备 IP 和网段划分参考了水电厂经典网络架构设计。