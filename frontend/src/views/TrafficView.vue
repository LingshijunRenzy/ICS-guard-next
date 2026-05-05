<template>
  <div class="traffic-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-warning"><LinkIcon size="22px" /></span>
        <h2>{{ $t('traffic.title') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" @click="fetchAll">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refresh') }}
      </t-button>
    </div>

    <t-row :gutter="[16, 16]" class="stat-row">
      <template v-if="loading">
        <t-col v-for="n in 4" :key="'sk' + n" :span="3"><SkeletonCard /></t-col>
      </template>
      <t-col v-for="s in statCards" v-else :key="s.label" :span="3">
        <t-card :bordered="false" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper" :class="s.bg"><component :is="s.icon" size="28px" /></div>
            <div class="stat-info">
              <div class="stat-title">{{ s.label }}</div>
              <div class="stat-value" :class="s.color">{{ s.value }}</div>
            </div>
          </div>
        </t-card>
      </t-col>
    </t-row>

    <t-row :gutter="[16, 16]" class="chart-row">
      <t-col :span="12">
        <SkeletonList v-if="loading" :items="4" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><ChartPieIcon size="18px" class="card-header-icon-primary" /> {{ $t('traffic.byProtocol') }}</span>
            </div>
          </template>
          <div v-if="trafficStats.byProtocol?.length" class="list-container">
            <div v-for="(p, idx) in trafficStats.byProtocol" :key="p.protocol" class="proto-item">
              <div class="proto-info">
                <span class="proto-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="proto-label mono">{{ p.protocol }}</span>
                <span class="proto-flows">{{ p.flows }} {{ $t('traffic.flows') }}</span>
              </div>
              <t-progress :percentage="calcProtoPercent(p.totalBytes)" :color="protoColor(idx)" :label="false" :stroke-width="8" />
              <span class="proto-bytes">{{ formatBytes(p.totalBytes) }}</span>
            </div>
          </div>
          <t-empty v-else :description="$t('traffic.noProtocolData')" />
        </t-card>
      </t-col>
      <t-col :span="12">
        <SkeletonList v-if="loading" :items="4" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><UploadIcon size="18px" class="card-header-icon-primary" /> {{ $t('traffic.topSourceIps') }}</span>
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
          <t-empty v-else :description="$t('traffic.noSourceData')" />
        </t-card>
      </t-col>
    </t-row>

    <SkeletonTable v-if="loading && !metrics.length" :rows="5" :cols="7" />
    <t-card v-else :bordered="false" class="table-card">
      <template #header>
        <div class="card-header">
          <span><ViewListIcon size="18px" class="card-header-icon-primary" /> {{ $t('traffic.trafficRecords') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="metrics.length">
        <t-table :loading="loading" :data="metrics" :columns="metricColumns" stripe size="medium" class="tech-table" row-key="id">
          <template #sourceIp="{ row }"><span class="tech-font mono">{{ row.sourceIp }}</span></template>
          <template #destIp="{ row }"><span class="tech-font mono">{{ row.destIp }}</span></template>
          <template #bytesIn="{ row }"><span class="tech-font">{{ formatBytes(row.bytesIn) }}</span></template>
          <template #bytesOut="{ row }"><span class="tech-font">{{ formatBytes(row.bytesOut) }}</span></template>
          <template #flowDuration="{ row }"><span class="tech-font">{{ row.flowDuration ? (row.flowDuration + 'ms') : $t('common.dash') }}</span></template>
          <template #capturedAt="{ row }"><span class="tech-font">{{ formatDateTime(row.capturedAt) }}</span></template>
        </t-table>
        <div class="pagination-wrap">
          <t-pagination
            v-model:current="pageNum"
            v-model:page-size="pageSize"
            :total="page.totalElements"
            :page-size-options="[10, 20, 50]"
            show-page-number
            show-page-size
            @page-size-change="fetchMetrics(1)"
            @current-change="fetchMetrics"
          />
        </div>
      </template>
      <t-empty v-else :description="$t('traffic.noRecords')" />
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { LinkIcon, RefreshIcon, ChartPieIcon, UploadIcon, ViewListIcon, ChartLineIcon, StarIcon } from 'tdesign-icons-vue-next'
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
    { label: t('traffic.totalProtocols'), value: trafficStats.byProtocol?.length ?? 0, icon: ChartPieIcon, bg: 'primary-bg', color: 'primary-text' },
    { label: t('traffic.totalTraffic'), value: formatBytes(totalBytes), icon: ChartLineIcon, bg: 'warning-bg', color: 'warning-text' },
    { label: t('traffic.totalFlows'), value: totalFlows, icon: StarIcon, bg: 'success-bg', color: 'success-text' },
    { label: t('traffic.topSources'), value: trafficStats.topSources?.length ?? 0, icon: UploadIcon, bg: 'danger-bg', color: 'danger-text' },
  ]
})

const metricColumns = computed(() => [
  { colKey: 'sourceIp', title: t('traffic.sourceIp'), width: 160 },
  { colKey: 'destIp', title: t('traffic.destIp'), width: 160 },
  { colKey: 'protocol', title: t('traffic.proto'), width: 80 },
  { colKey: 'bytesIn', title: t('traffic.bytesIn'), width: 110, align: 'right' as const },
  { colKey: 'bytesOut', title: t('traffic.bytesOut'), width: 110, align: 'right' as const },
  { colKey: 'packetCount', title: t('traffic.packets'), width: 100, align: 'right' as const },
  { colKey: 'flowDuration', title: t('traffic.duration'), width: 120, align: 'right' as const },
  { colKey: 'capturedAt', title: t('traffic.capturedAt'), width: 180 },
])

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
  const colors = ['var(--td-brand-color)', 'var(--td-success-color)', 'var(--td-warning-color)', 'var(--td-error-color)', 'var(--td-gray-color-7)']
  return colors[idx % colors.length]
}

function fetchAll() { fetchStats(); fetchMetrics(pageNum.value) }

onMounted(fetchAll)
</script>

<style scoped>
.traffic-page { padding: 0; }

.header-icon-warning { color: var(--color-warning); background: var(--color-warning-light); }

.card-header-icon-primary { color: var(--color-primary); }
</style>
