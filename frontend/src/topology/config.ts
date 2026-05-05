/**
 * 拓扑可视化配置常量
 * 包含区域定义、设备坐标、连线定义
 */

/** 画布尺寸 */
export const CANVAS_WIDTH = 1400
export const CANVAS_HEIGHT = 960

/** 设备节点尺寸 */
export const NODE_WIDTH = 130
export const NODE_HEIGHT = 80

/** 区域定义 */
export interface ZoneDef {
  id: string
  nameKey: string
  x: number
  y: number
  width: number
  height: number
}

export const ZONES: ZoneDef[] = [
  { id: 'control', nameKey: 'control', x: 50, y: 30, width: 920, height: 200 },
  { id: 'unit1', nameKey: 'unit1', x: 40, y: 290, width: 380, height: 240 },
  { id: 'unit2', nameKey: 'unit2', x: 510, y: 290, width: 380, height: 240 },
  { id: 'unit3', nameKey: 'unit3', x: 980, y: 290, width: 380, height: 240 },
  { id: 'switchyard', nameKey: 'switchyard', x: 510, y: 580, width: 380, height: 190 },
  { id: 'bop', nameKey: 'bop', x: 510, y: 800, width: 380, height: 150 },
]

/** 设备节点定义 */
export interface DeviceNode {
  deviceId: string
  name: string
  type: 'PLC' | 'SCADA' | 'HMI' | 'RTU' | 'Protection' | 'Workstation'
  x: number
  y: number
  zone: string
}

export const DEVICES: DeviceNode[] = [
  // 控制中心
  { deviceId: 'SCADA-MAIN', name: 'Main SCADA Server', type: 'SCADA', x: 120, y: 100, zone: 'control' },
  { deviceId: 'SCADA-BACKUP', name: 'Backup SCADA Server', type: 'SCADA', x: 320, y: 100, zone: 'control' },
  { deviceId: 'ENG-WS', name: 'Engineering Workstation', type: 'Workstation', x: 620, y: 100, zone: 'control' },
  { deviceId: 'HMI-OP-01', name: 'Operator Station 1', type: 'HMI', x: 170, y: 180, zone: 'control' },
  { deviceId: 'HMI-OP-02', name: 'Operator Station 2', type: 'HMI', x: 370, y: 180, zone: 'control' },

  // 机组1
  { deviceId: 'PLC-U1-MAIN', name: 'Unit 1 Main Controller', type: 'PLC', x: 90, y: 340, zone: 'unit1' },
  { deviceId: 'PLC-U1-EXC', name: 'Unit 1 Excitation Controller', type: 'PLC', x: 230, y: 340, zone: 'unit1' },
  { deviceId: 'PLC-U1-GOV', name: 'Unit 1 Governor Controller', type: 'PLC', x: 90, y: 440, zone: 'unit1' },
  { deviceId: 'PLC-U1-PROT', name: 'Unit 1 Protection Relay', type: 'Protection', x: 230, y: 440, zone: 'unit1' },

  // 机组2
  { deviceId: 'PLC-U2-MAIN', name: 'Unit 2 Main Controller', type: 'PLC', x: 560, y: 340, zone: 'unit2' },
  { deviceId: 'PLC-U2-EXC', name: 'Unit 2 Excitation Controller', type: 'PLC', x: 700, y: 340, zone: 'unit2' },
  { deviceId: 'PLC-U2-GOV', name: 'Unit 2 Governor Controller', type: 'PLC', x: 560, y: 440, zone: 'unit2' },
  { deviceId: 'PLC-U2-PROT', name: 'Unit 2 Protection Relay', type: 'Protection', x: 700, y: 440, zone: 'unit2' },

  // 机组3
  { deviceId: 'PLC-U3-MAIN', name: 'Unit 3 Main Controller', type: 'PLC', x: 1030, y: 340, zone: 'unit3' },
  { deviceId: 'PLC-U3-EXC', name: 'Unit 3 Excitation Controller', type: 'PLC', x: 1170, y: 340, zone: 'unit3' },
  { deviceId: 'PLC-U3-GOV', name: 'Unit 3 Governor Controller', type: 'PLC', x: 1030, y: 440, zone: 'unit3' },
  { deviceId: 'PLC-U3-PROT', name: 'Unit 3 Protection Relay', type: 'Protection', x: 1170, y: 440, zone: 'unit3' },

  // 开关站
  { deviceId: 'RTU-SWYD', name: 'Switchyard RTU', type: 'RTU', x: 560, y: 620, zone: 'switchyard' },
  { deviceId: 'PLC-SWYD-PROT1', name: 'Line Protection Relay 1', type: 'Protection', x: 670, y: 620, zone: 'switchyard' },
  { deviceId: 'PLC-SWYD-PROT2', name: 'Line Protection Relay 2', type: 'Protection', x: 780, y: 620, zone: 'switchyard' },

  // 公用系统
  { deviceId: 'PLC-AUX', name: 'Auxiliary Systems PLC', type: 'PLC', x: 560, y: 840, zone: 'bop' },
  { deviceId: 'RTU-DAM', name: 'Dam Monitoring RTU', type: 'RTU', x: 670, y: 840, zone: 'bop' },
  { deviceId: 'PLC-SERV', name: 'Turbine Servo Auxiliary PLC', type: 'PLC', x: 780, y: 840, zone: 'bop' },
]

/** 设备ID → 区域映射（供前端查询） */
export const DEVICE_ZONE_MAP: Record<string, string> = {}
DEVICES.forEach(d => { DEVICE_ZONE_MAP[d.deviceId] = d.zone })

/** 连线定义 */
export interface ConnectionLine {
  from: string
  to: string
}

export const BACKBONE_CONNECTIONS: ConnectionLine[] = [
  { from: 'SCADA-MAIN', to: 'PLC-U1-MAIN' },
  { from: 'SCADA-MAIN', to: 'PLC-U2-MAIN' },
  { from: 'SCADA-MAIN', to: 'PLC-U3-MAIN' },
  { from: 'SCADA-MAIN', to: 'RTU-SWYD' },
  { from: 'SCADA-MAIN', to: 'PLC-AUX' },
]

export const INTRACONNECTIONS: ConnectionLine[] = [
  // 控制中心内部
  { from: 'SCADA-MAIN', to: 'HMI-OP-01' },
  { from: 'SCADA-MAIN', to: 'HMI-OP-02' },
  { from: 'SCADA-MAIN', to: 'ENG-WS' },

  // 机组1
  { from: 'PLC-U1-MAIN', to: 'PLC-U1-EXC' },
  { from: 'PLC-U1-MAIN', to: 'PLC-U1-GOV' },
  { from: 'PLC-U1-EXC', to: 'PLC-U1-PROT' },

  // 机组2
  { from: 'PLC-U2-MAIN', to: 'PLC-U2-EXC' },
  { from: 'PLC-U2-MAIN', to: 'PLC-U2-GOV' },
  { from: 'PLC-U2-EXC', to: 'PLC-U2-PROT' },

  // 机组3
  { from: 'PLC-U3-MAIN', to: 'PLC-U3-EXC' },
  { from: 'PLC-U3-MAIN', to: 'PLC-U3-GOV' },
  { from: 'PLC-U3-EXC', to: 'PLC-U3-PROT' },
]

/** 设备类型 → TDesign 图标名映射 */
export const DEVICE_TYPE_ICONS: Record<string, string> = {
  PLC: 'CpuIcon',
  SCADA: 'ServerIcon',
  HMI: 'DesktopIcon',
  RTU: 'UsbIcon',
  Protection: 'ShieldIcon',
  Workstation: 'LaptopIcon',
}

/** 计算正交折线路径：从起点到终点，三线段（水平→垂直→水平） */
export function computeOrthogonalPath(
  x1: number, y1: number,
  x2: number, y2: number,
): string {
  const midY = (y1 + y2) / 2
  return `M ${x1},${y1} L ${x1},${midY} L ${x2},${midY} L ${x2},${y2}`
}