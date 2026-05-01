<template>
  <div class="traffic-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon header-icon-warning"><Connection /></el-icon>
        <h2>{{ $t('traffic.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchAll">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <el-row :gutter="24" class="stat-row">
      <template v-if="loading">
        <el-col v-for="n in 4" :key="'sk' + n" :span="6"><SkeletonCard /></el-col>
      </template>
      <el-col v-for="s in statCards" v-else :key="s.label" :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper" :class="s.bg"><el-icon><component :is="s.icon" /></el-icon></div>
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
        <SkeletonList v-if="loading" :items="4" />
        <el-card v-else shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon-primary"><PieChart /></el-icon> {{ $t('traffic.byProtocol') }}</span>
            </div>
          </template>
          <div v-if="trafficStats.byProtocol?.length" class="list-container">
            <div v-for="(p, idx) in trafficStats.byProtocol" :key="p.protocol" class="proto-item">
              <div class="proto-info">
                <span class="proto-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="proto-label mono">{{ p.protocol }}</span>
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
        <SkeletonList v-if="loading" :items="4" />
        <el-card v-else shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon-primary"><Upload /></el-icon> {{ $t('traffic.topSourceIps') }}</span>
            </div>
          </template>
          <div v-if="trafficStats.topSources?.length" class="list-container">
            <div v-for="(e, idx) in trafficStats.topSources" :key="e.ip" class="traffic-item">
              <div class="traffic-info">
                <span class="traffic-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="traffic-ip mono">{{ e.ip }}</span>
                <span class="traffic-bytes">{{ formatBytes(e.totalBytes) }}</span>
              </div>
              <div class="traffic-bar" :style="{ width: calcBarPercent(e.totalBytes, trafficStats.topSources[0].totalBytes) + '%', backgroundColor: 'var(--color-primary)' }" />
            </div>
          </div>
          <el-empty v-else :description="$t('traffic.noSourceData')" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <SkeletonTable v-if="loading && !metrics.length" :rows="5" :cols="7" />
    <el-card v-else shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon-primary"><List /></el-icon> {{ $t('traffic.trafficRecords') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="metrics.length">
        <el-table v-loading="loading" :data="metrics" stripe size="default" class="tech-table">
          <el-table-column :label="$t('traffic.sourceIp')" width="160">
            <template #default="{ row }"><span class="tech-font mono">{{ row.sourceIp }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('traffic.destIp')" width="160">
            <template #default="{ row }"><span class="tech-font mono">{{ row.destIp }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('traffic.proto')" prop="protocol" width="80" />
          <el-table-column :label="$t('traffic.bytesIn')" width="110" align="right">
            <template #default="{ row }"><span class="tech-font">{{ formatBytes(row.bytesIn) }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('traffic.bytesOut')" width="110" align="right">
            <template #default="{ row }"><span class="tech-font">{{ formatBytes(row.bytesOut) }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('traffic.packets')" prop="packetCount" width="100" align="right" />
          <el-table-column :label="$t('traffic.duration')" width="120" align="right">
            <template #default="{ row }"><span class="tech-font">{{ row.flowDuration ? (row.flowDuration + 'ms') : $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('traffic.capturedAt')" width="180">
            <template #default="{ row }"><span class="tech-font">{{ formatDateTime(row.capturedAt) }}</span></template>
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
import { formatBytes, formatDateTime, calcBarPercent } from '@/composables/useFormat'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

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
  } finally { loading.value = false }
}

function calcProtoPercent(totalBytes: number): number {
  const all = trafficStats.byProtocol?.reduce((s, p) => s + p.totalBytes, 0) ?? 1
  return Math.round((totalBytes / all) * 100)
}

function protoColor(idx: number): string {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
  return colors[idx % colors.length]
}

function fetchAll() { fetchStats(); fetchMetrics(pageNum.value) }

onMounted(fetchAll)
</script>

<style scoped>
.traffic-page { padding: 0; }

.header-icon-warning { color: var(--color-warning); background: var(--color-warning-light); }

.card-header-icon-primary { color: var(--color-primary); font-size: 18px; }
</style>
