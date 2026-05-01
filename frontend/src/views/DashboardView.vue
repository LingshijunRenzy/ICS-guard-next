<template>
  <div class="dashboard">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon"><Monitor /></el-icon>
        <h2>{{ $t('dashboard.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchData" class="refresh-btn">
        {{ $t('common.refreshStatus') }}
      </el-button>
    </div>

    <el-row :gutter="24" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card alert-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper danger-bg">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ $t('dashboard.threatAlerts') }}</div>
              <div class="stat-value danger-text">{{ data.totalAlerts }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card device-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper success-bg">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ $t('dashboard.onlineDevices') }}</div>
              <div class="stat-value success-text">{{ data.totalDevices }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card rule-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper primary-bg">
              <el-icon><Key /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ $t('dashboard.activeRules') }}</div>
              <div class="stat-value primary-text">
                {{ data.activeRuleCount }}
                <span class="stat-suffix">/ {{ data.totalRuleCount }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card traffic-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper warning-bg">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ $t('dashboard.trackedSources') }}</div>
              <div class="stat-value warning-text">{{ data.trafficSummary?.topSources?.length ?? 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><PieChart /></el-icon> {{ $t('dashboard.alertsBySeverity') }}</span>
            </div>
          </template>
          <div class="list-container">
            <template v-if="hasEntries(data.alertsBySeverity)">
              <div v-for="(count, sev) in data.alertsBySeverity" :key="sev" class="list-item">
                <span class="list-label">{{ $t(`severity.${sev}`, sev.toUpperCase()) }}</span>
                <div class="progress-wrapper">
                  <el-progress :percentage="calcPercent(count, data.totalAlerts)" :color="severityColor(sev)" :show-text="false" :stroke-width="10" />
                </div>
                <span class="list-value">{{ count }}</span>
              </div>
            </template>
            <el-empty v-else :description="$t('common.noData')" :image-size="80" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><List /></el-icon> {{ $t('dashboard.alertsByStatus') }}</span>
            </div>
          </template>
          <div class="list-container">
            <template v-if="hasEntries(data.alertsByStatus)">
              <div v-for="(count, st) in data.alertsByStatus" :key="st" class="list-item">
                <span class="list-label">{{ $t(`alertStatus.${st}`, st.toUpperCase()) }}</span>
                <div class="progress-wrapper">
                  <el-progress :percentage="calcPercent(count, data.totalAlerts)" :color="statusColor(st)" :show-text="false" :stroke-width="10" />
                </div>
                <span class="list-value">{{ count }}</span>
              </div>
            </template>
            <el-empty v-else :description="$t('common.noData')" :image-size="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Upload /></el-icon> {{ $t('dashboard.topSources') }}</span>
            </div>
          </template>
          <div class="list-container">
            <template v-if="data.trafficSummary?.topSources?.length">
              <div v-for="(e, idx) in data.trafficSummary.topSources" :key="e.ip" class="list-item-traffic">
                <div class="traffic-info">
                  <span class="traffic-index" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                  <span class="traffic-ip">{{ e.ip }}</span>
                  <span class="traffic-bytes">{{ formatBytes(e.totalBytes) }}</span>
                </div>
                <div class="traffic-bar" :style="{ width: calcTrafficPercent(e.totalBytes, data.trafficSummary.topSources[0].totalBytes) + '%', backgroundColor: '#409eff' }"></div>
              </div>
            </template>
            <el-empty v-else :description="$t('dashboard.noTraffic')" :image-size="80" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Download /></el-icon> {{ $t('dashboard.topDestinations') }}</span>
            </div>
          </template>
          <div class="list-container">
            <template v-if="data.trafficSummary?.topDestinations?.length">
              <div v-for="(e, idx) in data.trafficSummary.topDestinations" :key="e.ip" class="list-item-traffic">
                <div class="traffic-info">
                  <span class="traffic-index" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                  <span class="traffic-ip">{{ e.ip }}</span>
                  <span class="traffic-bytes">{{ formatBytes(e.totalBytes) }}</span>
                </div>
                <div class="traffic-bar" :style="{ width: calcTrafficPercent(e.totalBytes, data.trafficSummary.topDestinations[0].totalBytes) + '%', backgroundColor: '#67c23a' }"></div>
              </div>
            </template>
            <el-empty v-else :description="$t('dashboard.noTraffic')" :image-size="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Warning /></el-icon> {{ $t('dashboard.recentAlerts') }}</span>
        </div>
      </template>
      <template v-if="data.recentAlerts?.length">
        <el-table :data="data.recentAlerts" stripe size="default" class="tech-table">
          <el-table-column :label="$t('dashboard.severity')" width="100">
            <template #default="{ row }">
              <el-tag :type="severityTag(row.severity)" size="small" effect="dark" class="status-tag">{{ $t(`severity.${row.severity}`, row.severity.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.ruleType')" width="140">
            <template #default="{ row }">
              <span class="tech-font">{{ row.alertType }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.sourceIp')" width="150">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.sourceIp }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.destIp')" width="150">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.destIp }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.protocol')" width="80" />
          <el-table-column :label="$t('dashboard.description')" min-width="220" show-overflow-tooltip />
          <el-table-column :label="$t('common.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="plain" class="status-tag">{{ $t(`alertStatus.${row.status}`, row.status.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.triggeredAt')" width="180">
            <template #default="{ row }">
              <span class="tech-font">{{ fmt(row.triggeredAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else :description="$t('dashboard.noAlerts')" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, WarningFilled, Cpu, Key, DataLine, Monitor, PieChart, List, Upload, Download, Warning } from '@element-plus/icons-vue'
import { get } from '@/api/client'
import type { DashboardResponse } from '@/api/types'

const loading = ref(false)

const data = reactive<DashboardResponse>({
  totalAlerts: 0,
  alertsBySeverity: {},
  alertsByStatus: {},
  totalDevices: 0,
  activeRuleCount: 0,
  totalRuleCount: 0,
  recentAlerts: [],
  trafficSummary: { topSources: [], topDestinations: [] },
})

async function fetchData() {
  loading.value = true
  try {
    const res = await get<DashboardResponse>('/dashboard')
    if (res.code === 200) Object.assign(data, res.data)
  } finally {
    loading.value = false
  }
}

function hasEntries(obj: Record<string, number> | undefined): boolean {
  return obj != null && Object.keys(obj).length > 0
}

function calcPercent(count: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((count / total) * 100))
}

function calcTrafficPercent(bytes: number, maxBytes: number) {
  if (!maxBytes) return 0
  return Math.min(100, Math.max(2, Math.round((bytes / maxBytes) * 100)))
}

function severityColor(sev: string) {
  if (sev === 'critical') return '#f56c6c'
  if (sev === 'high') return '#e6a23c'
  return '#909399'
}

function statusColor(st: string) {
  if (st === 'resolved') return '#67c23a'
  if (st === 'acknowledged') return '#e6a23c'
  if (st === 'escalated') return '#f56c6c'
  return '#409eff'
}

function severityTag(sev: string): 'danger' | 'warning' | 'info' | '' {
  if (sev === 'critical') return 'danger'
  if (sev === 'high') return 'warning'
  return 'info'
}

function statusTag(st: string): 'success' | 'warning' | 'danger' | 'info' | '' {
  if (st === 'resolved') return 'success'
  if (st === 'acknowledged') return 'warning'
  if (st === 'escalated') return 'danger'
  return 'info'
}

function formatBytes(bytes: number): string {
  if (bytes >= 1e9) return (bytes / 1e9).toFixed(1) + ' GB'
  if (bytes >= 1e6) return (bytes / 1e6).toFixed(1) + ' MB'
  if (bytes >= 1e3) return (bytes / 1e3).toFixed(1) + ' KB'
  return bytes + ' B'
}

function fmt(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('en-CA', { hour12: false })
}

onMounted(fetchData)
</script>

<style scoped>
.dashboard {
  padding: 0;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #2c3e50;
  letter-spacing: 0.5px;
}
.header-icon {
  font-size: 28px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  padding: 8px;
  border-radius: 8px;
}
.refresh-btn {
  border-radius: 8px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.stat-row {
  margin-bottom: 24px;
}
.stat-card {
  border-radius: 12px;
  border: none;
  background: #fff;
  transition: transform 0.3s;
}
.stat-card:hover {
  transform: translateY(-4px);
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 4px;
}
.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}
.danger-bg { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.success-bg { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.primary-bg { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.warning-bg { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }

.danger-text { color: #f56c6c; }
.success-text { color: #67c23a; }
.primary-text { color: #409eff; }
.warning-text { color: #e6a23c; }

.stat-info {
  flex: 1;
}
.stat-title {
  font-size: 14px;
  color: #8c939d;
  margin-bottom: 4px;
  font-weight: 500;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-suffix {
  font-size: 16px;
  color: #c0c4cc;
  font-weight: 500;
}

.chart-row {
  margin-bottom: 24px;
}
.data-card {
  border-radius: 12px;
  border: none;
}
.card-header {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-header .el-icon {
  color: #409eff;
  font-size: 18px;
}
.list-container {
  min-height: 160px;
  padding: 8px 0;
}

.list-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.list-item:last-child {
  margin-bottom: 0;
}
.list-label {
  width: 100px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}
.progress-wrapper {
  flex: 1;
  padding: 0 16px;
}
.list-value {
  width: 40px;
  text-align: right;
  font-weight: 700;
  color: #303133;
  font-size: 15px;
}

.list-item-traffic {
  margin-bottom: 16px;
}
.list-item-traffic:last-child {
  margin-bottom: 0;
}
.traffic-info {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.traffic-index {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: #f4f4f5;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  margin-right: 12px;
}
.rank-1 { background: #f56c6c; color: #fff; }
.rank-2 { background: #e6a23c; color: #fff; }
.rank-3 { background: #409eff; color: #fff; }
.traffic-ip {
  flex: 1;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  color: #303133;
}
.traffic-bytes {
  font-weight: 600;
  color: #606266;
  font-size: 13px;
}
.traffic-bar {
  height: 6px;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.table-card {
  border-radius: 12px;
  border: none;
}
.tech-table {
  border-radius: 8px;
  overflow: hidden;
}
.tech-font {
  font-weight: 500;
  color: #606266;
}
.mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #409eff;
}
.status-tag {
  font-weight: bold;
  border-radius: 4px;
}
</style>
