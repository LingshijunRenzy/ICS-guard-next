export type Severity = 'critical' | 'high' | 'medium' | 'low'
export type AlertStatus = 'new' | 'acknowledged' | 'resolved' | 'escalated'
// TDesign theme values: no 'info' — use 'default' instead
export type TagType = 'danger' | 'warning' | 'success' | 'primary' | 'default' | ''

export function severityTag(sev: string): TagType {
  if (sev === 'critical') return 'danger'
  if (sev === 'high') return 'warning'
  return 'default'
}

export function statusTag(st: string): TagType {
  if (st === 'resolved') return 'success'
  if (st === 'acknowledged') return 'warning'
  if (st === 'escalated') return 'danger'
  return 'default'
}

export function severityColor(sev: string): string {
  if (sev === 'critical') return 'var(--td-error-color)'
  if (sev === 'high') return 'var(--td-warning-color)'
  return 'var(--td-gray-color-7)'
}

export function statusColor(st: string): string {
  if (st === 'resolved') return 'var(--td-success-color)'
  if (st === 'acknowledged') return 'var(--td-warning-color)'
  if (st === 'escalated') return 'var(--td-error-color)'
  return 'var(--td-brand-color)'
}

export function eventTag(et: string): TagType {
  if (et === 'device_discovered' || et === 'device_online') return 'success'
  if (et === 'device_offline') return 'danger'
  return 'default'
}

export function topoStatusTag(st: string): TagType {
  if (st === 'online') return 'success'
  if (st === 'offline') return 'danger'
  return 'default'
}

export function deviceStatusClass(st: string): string {
  if (st === 'online') return 'status-online'
  if (st === 'offline') return 'status-offline'
  return 'status-unknown'
}

export function actionTag(action: string): TagType {
  if (action === 'block') return 'danger'
  if (action === 'alert') return 'warning'
  return 'default'
}

export function auditActionTag(action: string): TagType {
  if (action.includes('delete') || action.includes('purge')) return 'danger'
  if (action.includes('create') || action.includes('enable')) return 'success'
  if (action.includes('update') || action.includes('disable') || action.includes('escalate')) return 'warning'
  return 'default'
}
