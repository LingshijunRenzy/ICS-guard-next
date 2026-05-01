<template>
  <div class="traffic-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon"><Connection /></el-icon>
        <h2>{{ $t('traffic.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchAll">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <el-row :gutter="24" class="stat-row">
      <el-col :span="6" v-for="s in statCards" :key="s.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper" :class="s.bg">
              <el-icon><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ s.label }}</div>
              <div class="stat-value" :class="s.color">{{ s.value }}</div>
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
              <span><el-icon><PieChart /></el-icon> {{ $t('traffic.byProtocol') }}</span>
            </div>
          </template>
          <div class="list-container" v-if="trafficStats.byProtocol?.length">
            <div v-for="(p, idx) in trafficStats.byProtocol" :key="p.protocol" class="proto-item">
              <div class="proto-info">
                <span class="proto-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="tech-font mono proto-label">{{ p.protocol }}</span>
                <span class="proto-flows">{{ p.flows }} {{ $t('traffic.flows') }}</span>
              </div>
              <el-progress :percentage="calcProtoPercent(p.totalBytes)" :color="protoColor(idx)" :show-text="false" :stroke-width="8" />
              <span class="proto-bytes">{{ formatBytes(p.totalBytes) }}</span>
            </div>
          </div>
          <el-empty v-else :description="$t('traffic.noProtocolData')" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Upload /></el-icon> {{ $t('traffic.topSourceIps') }}</span>
            </div>
          </template>
          <div class="list-container" v-if="trafficStats.topSources?.length">
            <div v-for="(e, idx) in trafficStats.topSources" :key="e.ip" class="traffic-item">
              <div class="traffic-info">
                <span class="traffic-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="tech-font mono traffic-ip">{{ e.ip }}</span>
                <span class="traffic-bytes">{{ formatBytes(e.totalBytes) }}</span>
              </div>
              <div class="traffic-bar" :style="{ width: calcBarPct(e.totalBytes, trafficStats.topSources[0].totalBytes) + '%', backgroundColor: '#409eff' }"></div>
            </div>
          </div>
          <el-empty v-else :description="$t('traffic.noSourceData')" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> {{ $t('traffic.trafficRecords') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="metrics.length">
        <el-table :data="metrics" stripe size="default" class="tech-table">
          <el-table-column :label="$t('traffic.sourceIp')" width="160">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.sourceIp }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('traffic.destIp')" width="160">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.destIp }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('traffic.proto')" prop="protocol" width="80" />
          <el-table-column :label="$t('traffic.bytesIn')" width="110" align="right">
            <template #default="{ row }">
              <span class="tech-font">{{ formatBytes(row.bytesIn) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('traffic.bytesOut')" width="110" align="right">
            <template #default="{ row }">
              <span class="tech-font">{{ formatBytes(row.bytesOut) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('traffic.packets')" prop="packetCount" width="100" align="right" />
          <el-table-column :label="$t('traffic.duration')" width="120" align="right">
            <template #default="{ row }">
              <span class="tech-font">{{ row.flowDuration ? (row.flowDuration + 'ms') : $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('traffic.capturedAt')" width="180">
            <template #default="{ row }">
              <span class="tech-font">{{ fmt(row.capturedAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="page.totalElements"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchMetrics(1)"
            @current-change="fetchMetrics"
          />
        </div>
      </template>
      <el-empty v-else :description="$t('traffic.noRecords')" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Connection, Refresh, PieChart, Upload, List, DataLine, MagicStick } from '@element-plus/icons-vue'
import { get } from '@/api/client'
import { useI18n } from 'vue-i18n'
import type { TrafficMetricResponse, TrafficStatsResponse, PageDTO } from '@/api/types'

const { t } = useI18n()
const loading = ref(false)
const metrics = ref<TrafficMetricResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<TrafficMetricResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const trafficStats = reactive<TrafficStatsResponse>({ topSources: [], topDestinations: [], byProtocol: [] })

const statCards = computed(() => {
  const totalBytes = trafficStats.byProtocol?.reduce((s, p) => s + p.totalBytes, 0) ?? 0
  const totalFlows = trafficStats.byProtocol?.reduce((s, p) => s + p.flows, 0) ?? 0
  return [
    { label: t('traffic.totalProtocols'), value: trafficStats.byProtocol?.length ?? 0, icon: PieChart, bg: 'primary-bg', color: 'primary-text' },
    { label: t('traffic.totalTraffic'), value: formatBytes(totalBytes), icon: DataLine, bg: 'warning-bg', color: 'warning-text' },
    { label: t('traffic.totalFlows'), value: totalFlows, icon: MagicStick, bg: 'success-bg', color: 'success-text' },
    { label: t('traffic.topSources'), value: trafficStats.topSources?.length ?? 0, icon: Upload, bg: 'danger-bg', color: 'danger-text' },
  ]
})

async function fetchStats() {
  try {
    const res = await get<TrafficStatsResponse>('/metrics/traffic/stats')
    if (res.code === 200) Object.assign(trafficStats, res.data)
  } catch { /* auxiliary */ }
}

async function fetchMetrics(p: number) {
  loading.value = true
  try {
    const res = await get<PageDTO<TrafficMetricResponse>>('/metrics/traffic', { page: p - 1, size: pageSize.value })
    if (res.code === 200) {
      Object.assign(page, res.data)
      metrics.value = res.data.content
      pageNum.value = p
    }
  } finally {
    loading.value = false
  }
}

function fetchAll() {
  fetchStats()
  fetchMetrics(pageNum.value)
}

function calcProtoPercent(totalBytes: number): number {
  const all = trafficStats.byProtocol?.reduce((s, p) => s + p.totalBytes, 0) ?? 1
  return Math.round((totalBytes / all) * 100)
}

function calcBarPct(bytes: number, maxBytes: number): number {
  if (!maxBytes) return 0
  return Math.min(100, Math.max(2, Math.round((bytes / maxBytes) * 100)))
}

function protoColor(idx: number): string {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
  return colors[idx % colors.length]
}

function formatBytes(bytes: number): string {
  if (bytes >= 1e9) return (bytes / 1e9).toFixed(1) + ' GB'
  if (bytes >= 1e6) return (bytes / 1e6).toFixed(1) + ' MB'
  if (bytes >= 1e3) return (bytes / 1e3).toFixed(1) + ' KB'
  return bytes + ' B'
}

function fmt(iso: string): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-CA', { hour12: false })
}

onMounted(fetchAll)
</script>

<style scoped>
.traffic-page { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
}
.header-title { display: flex; align-items: center; gap: 12px; }
.header-title h2 { margin: 0; font-size: 22px; font-weight: 600; color: #2c3e50; letter-spacing: 0.5px; }
.header-icon {
  font-size: 28px; color: #e6a23c; background: rgba(230, 162, 60, 0.1); padding: 8px; border-radius: 8px;
}

.stat-row { margin-bottom: 24px; }
.stat-card { border-radius: 12px; border: none; transition: transform 0.3s; }
.stat-card:hover { transform: translateY(-4px); }
.stat-content { display: flex; align-items: center; gap: 16px; padding: 8px 4px; }
.stat-icon-wrapper {
  width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 28px;
}
.primary-bg { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.warning-bg { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }
.success-bg { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.danger-bg { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.stat-info { flex: 1; }
.stat-title { font-size: 14px; color: #8c939d; margin-bottom: 4px; font-weight: 500; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.primary-text { color: #409eff; } .warning-text { color: #e6a23c; } .success-text { color: #67c23a; } .danger-text { color: #f56c6c; }

.chart-row { margin-bottom: 24px; }
.data-card { border-radius: 12px; border: none; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; font-size: 16px; color: #303133; }
.card-header > :first-child { display: flex; align-items: center; gap: 8px; }
.card-header .el-icon { color: #409eff; font-size: 18px; }
.header-count { font-size: 13px; color: #909399; font-weight: 400; }
.list-container { min-height: 120px; padding: 8px 0; }

.proto-item { margin-bottom: 14px; }
.proto-item:last-child { margin-bottom: 0; }
.proto-info { display: flex; align-items: center; margin-bottom: 4px; }
.proto-rank {
  width: 22px; height: 22px; border-radius: 4px; background: #f4f4f5; color: #909399;
  display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; margin-right: 12px;
}
.rank-1 { background: #f56c6c; color: #fff; } .rank-2 { background: #e6a23c; color: #fff; } .rank-3 { background: #409eff; color: #fff; }
.proto-label { flex: 1; font-size: 14px; }
.proto-flows { font-size: 12px; color: #909399; }
.proto-bytes { font-size: 12px; color: #606266; font-weight: 600; display: block; text-align: right; margin-top: 2px; }

.traffic-item { margin-bottom: 14px; }
.traffic-item:last-child { margin-bottom: 0; }
.traffic-info { display: flex; align-items: center; margin-bottom: 4px; }
.traffic-rank {
  width: 22px; height: 22px; border-radius: 4px; background: #f4f4f5; color: #909399;
  display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; margin-right: 12px;
}
.traffic-ip { flex: 1; font-size: 14px; }
.traffic-bytes { font-weight: 600; color: #606266; font-size: 13px; }
.traffic-bar { height: 6px; border-radius: 3px; transition: width 0.5s ease; }

.table-card { border-radius: 12px; border: none; }
.tech-table { border-radius: 8px; overflow: hidden; }
.tech-font { font-weight: 500; color: #606266; font-size: 13px; }
.mono { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; color: #409eff; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
