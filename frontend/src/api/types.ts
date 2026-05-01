export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface PageDTO<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ── Auth ──────────────────────────────────────────────────────────

export interface LoginRequest {
  username: string
  password: string
}

export interface UserResponse {
  id: number
  username: string
  email: string | null
  displayName: string | null
  enabled: boolean
  roles: string[]
  createdAt: string
  updatedAt: string
  profile: UserProfileResponse | null
}

export interface UserProfileResponse {
  language: string
  timezone: string
  theme: string
}

export interface UpdateProfileRequest {
  language?: string
  timezone?: string
  theme?: string
}

// ── Dashboard ─────────────────────────────────────────────────────

export interface DashboardResponse {
  totalAlerts: number
  alertsBySeverity: Record<string, number>
  alertsByStatus: Record<string, number>
  totalDevices: number
  activeRuleCount: number
  totalRuleCount: number
  recentAlerts: AlertResponse[]
  trafficSummary: TrafficSummary
}

export interface TrafficSummary {
  topSources: TopEntry[]
  topDestinations: TopEntry[]
}

export interface TopEntry {
  ip: string
  totalBytes: number
}

// ── Alerts ────────────────────────────────────────────────────────

export interface AlertResponse {
  id: number
  traceId: string
  alertType: string
  severity: string
  sourceIp: string
  destIp: string
  protocol: string
  description: string | null
  status: string
  triggeredAt: string
  createdAt: string
}

export interface AlertStatsResponse {
  bySeverity: Record<string, number>
  byStatus: Record<string, number>
  total: number
}

// ── Rules ─────────────────────────────────────────────────────────

export interface RuleResponse {
  id: number
  name: string
  description: string | null
  ruleType: string
  target: string | null
  action: string
  priority: number
  enabled: boolean
  ruleConfig: string | null
  createdBy: string | null
  createdAt: string
  updatedAt: string
  metadata: Record<string, string>
}

export interface RuleRequest {
  name: string
  description?: string
  ruleType: string
  target?: string
  action: string
  priority?: number
  enabled?: boolean
  ruleConfig?: string
  metadata?: { key: string; value: string }[]
}

// ── Topology ──────────────────────────────────────────────────────

export interface TopologyEventResponse {
  id: number
  traceId: string
  eventType: string
  deviceId: string
  deviceName: string
  deviceType: string
  ipAddress: string | null
  macAddress: string | null
  port: string | null
  status: string
  occurredAt: string
  createdAt: string
}

// ── Traffic Metrics ───────────────────────────────────────────────

export interface TrafficMetricResponse {
  id: number
  traceId: string
  metricType: string
  sourceIp: string
  destIp: string
  protocol: string
  bytesIn: number
  bytesOut: number
  packetCount: number
  flowDuration: number | null
  capturedAt: string
  createdAt: string
}

export interface TrafficStatsResponse {
  topSources: TopEntry[]
  topDestinations: TopEntry[]
  byProtocol: { protocol: string; totalBytes: number; flows: number }[]
}

// ── Audit Log ─────────────────────────────────────────────────────

export interface AuditLogResponse {
  id: number
  traceId: string
  userId: number | null
  username: string | null
  action: string
  resource: string | null
  resourceId: string | null
  ipAddress: string | null
  createdAt: string
}

// ── RBAC ──────────────────────────────────────────────────────────

export interface RoleResponse {
  id: number
  name: string
  description: string | null
  permissions: string[]
}

export interface CreateRoleRequest {
  name: string
  description?: string
  permissionIds?: number[]
}

export interface PermissionResponse {
  id: number
  name: string
  description: string | null
}

export interface CreateUserRequest {
  username: string
  password: string
  email?: string
  displayName?: string
  roleIds?: number[]
}

// ── SDN ───────────────────────────────────────────────────────────

export interface FlowBlockRequest {
  srcIp: string
  dstIp: string
  protocol: string
  reason: string
}

export interface SdnOperationResponse {
  operation: string
  status: string
  details: Record<string, any> | null
}
