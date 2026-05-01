import { describe, it, expect } from 'vitest'
import {
  severityTag,
  statusTag,
  severityColor,
  statusColor,
  eventTag,
  topoStatusTag,
  deviceStatusClass,
  actionTag,
  auditActionTag,
} from '@/composables/useSeverity'

describe('severityTag', () => {
  it('returns danger for critical', () => expect(severityTag('critical')).toBe('danger'))
  it('returns warning for high', () => expect(severityTag('high')).toBe('warning'))
  it('returns info for medium', () => expect(severityTag('medium')).toBe('info'))
  it('returns info for low', () => expect(severityTag('low')).toBe('info'))
  it('returns info for unknown', () => expect(severityTag('unknown')).toBe('info'))
})

describe('statusTag', () => {
  it('returns success for resolved', () => expect(statusTag('resolved')).toBe('success'))
  it('returns warning for acknowledged', () => expect(statusTag('acknowledged')).toBe('warning'))
  it('returns danger for escalated', () => expect(statusTag('escalated')).toBe('danger'))
  it('returns info for new', () => expect(statusTag('new')).toBe('info'))
  it('returns info for unknown', () => expect(statusTag('unknown')).toBe('info'))
})

describe('severityColor', () => {
  it('returns red for critical', () => expect(severityColor('critical')).toBe('#f56c6c'))
  it('returns orange for high', () => expect(severityColor('high')).toBe('#e6a23c'))
  it('returns grey for others', () => expect(severityColor('low')).toBe('#909399'))
})

describe('statusColor', () => {
  it('returns green for resolved', () => expect(statusColor('resolved')).toBe('#67c23a'))
  it('returns orange for acknowledged', () => expect(statusColor('acknowledged')).toBe('#e6a23c'))
  it('returns red for escalated', () => expect(statusColor('escalated')).toBe('#f56c6c'))
  it('returns blue for new', () => expect(statusColor('new')).toBe('#409eff'))
})

describe('eventTag', () => {
  it('returns success for device_discovered', () => expect(eventTag('device_discovered')).toBe('success'))
  it('returns success for device_online', () => expect(eventTag('device_online')).toBe('success'))
  it('returns danger for device_offline', () => expect(eventTag('device_offline')).toBe('danger'))
  it('returns info for others', () => expect(eventTag('topology_change')).toBe('info'))
})

describe('topoStatusTag', () => {
  it('returns success for online', () => expect(topoStatusTag('online')).toBe('success'))
  it('returns danger for offline', () => expect(topoStatusTag('offline')).toBe('danger'))
  it('returns info for unknown', () => expect(topoStatusTag('unknown')).toBe('info'))
})

describe('deviceStatusClass', () => {
  it('returns status-online for online', () => expect(deviceStatusClass('online')).toBe('status-online'))
  it('returns status-offline for offline', () => expect(deviceStatusClass('offline')).toBe('status-offline'))
  it('returns status-unknown for others', () => expect(deviceStatusClass('unknown')).toBe('status-unknown'))
})

describe('actionTag', () => {
  it('returns danger for block', () => expect(actionTag('block')).toBe('danger'))
  it('returns warning for alert', () => expect(actionTag('alert')).toBe('warning'))
  it('returns info for log', () => expect(actionTag('log')).toBe('info'))
  it('returns info for unknown', () => expect(actionTag('allow')).toBe('info'))
})

describe('auditActionTag', () => {
  it('returns danger for delete actions', () => expect(auditActionTag('user_delete')).toBe('danger'))
  it('returns danger for purge actions', () => expect(auditActionTag('purge_logs')).toBe('danger'))
  it('returns success for create actions', () => expect(auditActionTag('rule_create')).toBe('success'))
  it('returns success for enable actions', () => expect(auditActionTag('user_enable')).toBe('success'))
  it('returns warning for update actions', () => expect(auditActionTag('rule_update')).toBe('warning'))
  it('returns warning for disable actions', () => expect(auditActionTag('user_disable')).toBe('warning'))
  it('returns warning for escalate actions', () => expect(auditActionTag('alert_escalate')).toBe('warning'))
  it('returns info for login', () => expect(auditActionTag('login')).toBe('info'))
})
