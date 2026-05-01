export type Severity = 'critical' | 'high' | 'medium' | 'low'
export type AlertStatus = 'new' | 'acknowledged' | 'resolved' | 'escalated'
export type TagType = 'danger' | 'warning' | 'success' | 'info' | ''

export function severityTag(sev: string): TagType {
  if (sev === 'critical') return 'danger'
  if (sev === 'high') return 'warning'
  return 'info'
}

export function statusTag(st: string): TagType {
  if (st === 'resolved') return 'success'
  if (st === 'acknowledged') return 'warning'
  if (st === 'escalated') return 'danger'
  return 'info'
}

export function severityColor(sev: string): string {
  if (sev === 'critical') return '#f56c6c'
  if (sev === 'high') return '#e6a23c'
  return '#909399'
}

export function statusColor(st: string): string {
  if (st === 'resolved') return '#67c23a'
  if (st === 'acknowledged') return '#e6a23c'
  if (st === 'escalated') return '#f56c6c'
  return '#409eff'
}

export function eventTag(et: string): TagType {
  if (et === 'device_discovered' || et === 'device_online') return 'success'
  if (et === 'device_offline') return 'danger'
  return 'info'
}

export function topoStatusTag(st: string): TagType {
  if (st === 'online') return 'success'
  if (st === 'offline') return 'danger'
  return 'info'
}

export function deviceStatusClass(st: string): string {
  if (st === 'online') return 'status-online'
  if (st === 'offline') return 'status-offline'
  return 'status-unknown'
}

export function actionTag(action: string): TagType {
  if (action === 'block') return 'danger'
  if (action === 'alert') return 'warning'
  return 'info'
}

export function auditActionTag(action: string): TagType {
  if (action.includes('delete') || action.includes('purge')) return 'danger'
  if (action.includes('create') || action.includes('enable')) return 'success'
  if (action.includes('update') || action.includes('disable') || action.includes('escalate')) return 'warning'
  return 'info'
}
