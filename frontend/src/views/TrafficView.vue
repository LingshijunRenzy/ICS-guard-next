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

    <!-- Stat Cards -->
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

    <!-- ECharts Row 1: Protocol Distribution (Pie) + Top Sources (Horizontal Bar) -->
    <t-row :gutter="[16, 16]" class="chart-row">
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="3" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><ChartPieIcon size="18px" class="card-header-icon-primary" /> {{ $t('traffic.byProtocol') }}</span>
            </div>
          </template>
          <v-chart class="chart-wrapper" :option="protocolChartOption" autoresize />
        </t-card>
      </t-col>
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="4" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><UploadIcon size="18px" class="card-header-icon-primary" /> {{ $t('traffic.topSourceIps') }}</span>
            </div>
          </template>
          <v-chart class="chart-wrapper" :option="sourceChartOption" autoresize />
        </t-card>
      </t-col>
    </t-row>

    <!-- Filters -->
    <t-card :bordered="false" class="filter-card">
      <t-form :data="filters" layout="inline" @keyup.enter="handleSearch">
        <t-form-item label="Source IP">
          <t-input v-model="filters.sourceIp" :placeholder="$t('traffic.sourceIp')" clearable style="width:150px" />
        </t-form-item>
        <t-form-item label="Dest IP">
          <t-input v-model="filters.destIp" :placeholder="$t('traffic.destIp')" clearable style="width:150px" />
        </t-form-item>
        <t-form-item :label="$t('traffic.proto')">
          <t-select v-model="filters.protocol" :placeholder="$t('common.dash')" clearable style="width:120px">
            <t-option :label="$t('protocol.tcp')" value="tcp" />
            <t-option :label="$t('protocol.udp')" value="udp" />
            <t-option :label="$t('protocol.modbus')" value="modbus" />
            <t-option :label="$t('protocol.dnp3')" value="dnp3" />
            <t-option :label="$t('protocol.bacnet')" value="bacnet" />
            <t-option :label="$t('protocol.s7comm')" value="s7comm" />
            <t-option :label="$t('protocol.ethernetIp')" value="ethernet_ip" />
          </t-select>
        </t-form-item>
        <t-form-item label="">
          <t-date-range-picker
            v-model="filters.dateRange"
            :placeholder="[$t('common.startDate'), $t('common.endDate')]"
            clearable
            style="width:260px"
          />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" @click="handleSearch">
            <template #icon><SearchIcon /></template>
            {{ $t('common.search') }}
          </t-button>
          <t-button variant="outline" @click="handleReset" style="margin-left:8px">
            <template #icon><RefreshIcon /></template>
            {{ $t('common.reset') }}
          </t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- Traffic Records Table -->
    <SkeletonTable v-if="loading && !metrics.length" :rows="5" :cols="7" />
    <t-card :bordered="false" class="table-card">
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
import { graphic } from 'echarts/core'
import VChart from 'vue-echarts'
import { LinkIcon, RefreshIcon, SearchIcon, ChartPieIcon, UploadIcon, ViewListIcon, ChartLineIcon, StarIcon } from 'tdesign-icons-vue-next'
import { get } from '@/api/client'
import { useI18n } from 'vue-i18n'
import type { TrafficMetricResponse, TrafficStatsResponse, PageDTO } from '@/api/types'
import { formatBytes, formatDateTime } from '@/composables/useFormat'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'


const { t } = useI18n()
const loading = ref(false)
const metrics = ref<TrafficMetricResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<TrafficMetricResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

interface TrafficFilters {
  sourceIp: string
  destIp: string
  protocol: string
  dateRange: string[]
}
const filters = reactive<TrafficFilters>({
  sourceIp: '',
  destIp: '',
  protocol: '',
  dateRange: [],
})

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

// ── Protocol colors ──
const PROTOCOL_COLORS = ['#0052d9', '#00a870', '#ed7b2f', '#e34d59', '#722ed1', '#13c2c2', '#eb2f96', '#909399']

// ── Protocol Distribution Pie ──
const protocolChartOption = computed(() => {
  const entries = trafficStats.byProtocol
  if (!entries?.length) return noDataOption(t('traffic.noProtocolData'))

  const total = entries.reduce((s, p) => s + p.totalBytes, 0)

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; percent: number }) =>
        `<strong>${p.name}</strong><br/>${t('traffic.totalTraffic')}: ${formatBytes(p.value)} (${p.percent}%)`,
    },
    legend: {
      bottom: 0,
      textStyle: { fontSize: 12 },
      itemWidth: 10,
      itemHeight: 10,
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      padAngle: 2,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontWeight: 'bold', fontSize: 14 },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.15)' },
      },
      data: entries.map((p, i) => ({
        name: p.protocol,
        value: p.totalBytes,
        itemStyle: { color: PROTOCOL_COLORS[i % PROTOCOL_COLORS.length] },
        flows: p.flows,
        percentLabel: `${((p.totalBytes / total) * 100).toFixed(1)}%`,
      })),
    }],
  }
})

// ── Top Sources Horizontal Bar ──
const sourceChartOption = computed(() => {
  const entries = trafficStats.topSources
  if (!entries?.length) return noDataOption(t('traffic.noSourceData'))

  const ips = entries.map(e => e.ip).reverse()
  const bytes = entries.map(e => e.totalBytes).reverse()

  return horizontalBarOption(ips, bytes, t('traffic.totalTraffic'))
})

// ── Shared helpers ──
function noDataOption(msg: string) {
  return {
    title: { text: msg, left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } },
    series: [],
    xAxis: undefined as any,
    yAxis: undefined as any,
    tooltip: undefined as any,
    legend: undefined as any,
  }
}

function resolveCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || '#0052d9'
}

function horizontalBarOption(
  labels: string[],
  values: number[],
  unit: string,
  barColor?: string,
) {
  const color = barColor ?? resolveCssVar('--color-primary')
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (p: { name: string; value: number }[]) =>
        `<strong>${p[0].name}</strong><br/>${unit}: ${formatBytes(p[0].value)}`,
      textStyle: { fontSize: 13 },
    },
    grid: { left: 20, right: 80, top: 10, bottom: 10, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: {
        formatter: (v: number) => formatBytes(v),
        fontSize: 11,
        color: '#909399',
      },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 12, fontFamily: 'SFMono-Regular, Consolas, monospace', width: 120, overflow: 'truncate' },
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [{
      type: 'bar',
      data: values.map(v => ({
        value: v,
        itemStyle: {
          color: new graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: color + '66' },
            { offset: 1, color: color },
          ]),
          borderRadius: [0, 4, 4, 0],
        },
      })),
      barWidth: 18,
      label: {
        show: true,
        position: 'right',
        formatter: (p: { value: number }) => formatBytes(p.value),
        fontSize: 11,
        fontFamily: 'SFMono-Regular, Consolas, monospace',
        color: '#666',
      },
    }],
  }
}

async function fetchStats() {
  try {
    const res = await get<TrafficStatsResponse>('/metrics/traffic/stats')
    if (res.code === 200) Object.assign(trafficStats, res.data)
  } catch { /* auxiliary */ }
}

function buildParams(p: number) {
  const params: Record<string, any> = {
    page: p - 1,
    size: pageSize.value,
    sort: 'capturedAt,desc',
  }
  if (filters.sourceIp) params.sourceIp = filters.sourceIp.trim()
  if (filters.destIp) params.destIp = filters.destIp.trim()
  if (filters.protocol) params.protocol = filters.protocol
  if (filters.dateRange?.length === 2) {
    if (filters.dateRange[0]) params.startTime = filters.dateRange[0]
    if (filters.dateRange[1]) params.endTime = filters.dateRange[1]
  }
  return params
}

async function fetchMetrics(p: number) {
  loading.value = true
  try {
    const res = await get<PageDTO<TrafficMetricResponse>>('/metrics/traffic', buildParams(p))
    if (res.code === 200) {
      Object.assign(page, res.data)
      metrics.value = res.data.content
      pageNum.value = p
    }
  } finally { loading.value = false }
}

function handleSearch() {
  fetchMetrics(1)
}

function handleReset() {
  filters.sourceIp = ''
  filters.destIp = ''
  filters.protocol = ''
  filters.dateRange = []
  fetchMetrics(1)
}

function fetchAll() { fetchStats(); fetchMetrics(pageNum.value) }

onMounted(fetchAll)
</script>

<style scoped>
.traffic-page { padding: 0; }

.header-icon-warning { color: var(--color-warning); background: var(--color-warning-light); }

.card-header-icon-primary { color: var(--color-primary); }

/* Chart wrapper inside card */
:deep(.chart-wrapper) {
  width: 100%;
  height: 280px;
  min-height: 200px;
}
</style>