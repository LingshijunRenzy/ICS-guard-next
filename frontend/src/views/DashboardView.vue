<template>
  <div class="dashboard">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-primary"><DesktopIcon size="22px" /></span>
        <h2>{{ $t('dashboard.title') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" class="refresh-btn" @click="fetchData">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refreshStatus') }}
      </t-button>
    </div>

    <!-- Stat Cards Row -->
    <t-row :gutter="[16, 16]" class="stat-row">
      <template v-if="loading">
        <t-col v-for="n in 4" :key="'sk' + n" :span="3"><SkeletonCard /></t-col>
      </template>
      <template v-else>
        <t-col :span="3">
          <t-card :bordered="false" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper danger-bg"><ErrorCircleFilledIcon size="28px" /></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('dashboard.threatAlerts') }}</div>
                <div class="stat-value danger-text">{{ data.totalAlerts }}</div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="3">
          <t-card :bordered="false" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper success-bg"><CpuIcon size="28px" /></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('dashboard.onlineDevices') }}</div>
                <div class="stat-value success-text">{{ data.totalDevices }}</div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="3">
          <t-card :bordered="false" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper primary-bg"><SecuredIcon size="28px" /></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('dashboard.activeRules') }}</div>
                <div class="stat-value primary-text">
                  {{ data.activeRuleCount }}
                  <span class="stat-suffix">/ {{ data.totalRuleCount }}</span>
                </div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="3">
          <t-card :bordered="false" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper warning-bg"><ChartLineIcon size="28px" /></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('dashboard.trackedSources') }}</div>
                <div class="stat-value warning-text">{{ data.trafficSummary?.topSources?.length ?? 0 }}</div>
              </div>
            </div>
          </t-card>
        </t-col>
      </template>
    </t-row>

    <!-- ECharts Row 1: Alerts by Severity (Pie) + Alerts by Status (Doughnut) -->
    <t-row :gutter="[16, 16]" class="chart-row">
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="3" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><ChartPieIcon size="18px" class="card-header-icon" /> {{ $t('dashboard.alertsBySeverity') }}</span>
            </div>
          </template>
          <v-chart class="chart-wrapper" :option="severityChartOption" autoresize />
        </t-card>
      </t-col>
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="3" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><ViewListIcon size="18px" class="card-header-icon" /> {{ $t('dashboard.alertsByStatus') }}</span>
            </div>
          </template>
          <v-chart class="chart-wrapper" :option="statusChartOption" autoresize />
        </t-card>
      </t-col>
    </t-row>

    <!-- ECharts Row 2: Top Sources + Top Destinations (Horizontal Bar) -->
    <t-row :gutter="[16, 16]" class="chart-row">
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="4" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><UploadIcon size="18px" class="card-header-icon" /> {{ $t('dashboard.topSources') }}</span>
            </div>
          </template>
          <v-chart class="chart-wrapper" :option="sourceChartOption" autoresize />
        </t-card>
      </t-col>
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="4" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><DownloadIcon size="18px" class="card-header-icon" /> {{ $t('dashboard.topDestinations') }}</span>
            </div>
          </template>
          <v-chart class="chart-wrapper" :option="destChartOption" autoresize />
        </t-card>
      </t-col>
    </t-row>

    <!-- Recent Alerts Table -->
    <SkeletonTable v-if="loading" :rows="5" :cols="7" />
    <t-card v-else :bordered="false" class="table-card">
      <template #header>
        <div class="card-header">
          <span><ErrorCircleIcon size="18px" class="card-header-icon" /> {{ $t('dashboard.recentAlerts') }}</span>
        </div>
      </template>
      <template v-if="data.recentAlerts?.length">
        <t-table :data="data.recentAlerts" :columns="alertColumns" stripe size="medium" class="tech-table" row-key="id">
          <template #severity="{ row }">
            <t-tag :theme="severityTag(row.severity)" size="small" variant="dark">{{ $t(`severity.${row.severity}`, row.severity.toUpperCase()) }}</t-tag>
          </template>
          <template #alertType="{ row }"><span class="tech-font">{{ row.alertType }}</span></template>
          <template #sourceIp="{ row }"><span class="tech-font mono">{{ row.sourceIp }}</span></template>
          <template #destIp="{ row }"><span class="tech-font mono">{{ row.destIp }}</span></template>
          <template #status="{ row }">
            <t-tag :theme="statusTag(row.status)" variant="light">{{ $t(`alertStatus.${row.status}`, row.status.toUpperCase()) }}</t-tag>
          </template>
          <template #triggeredAt="{ row }"><span class="tech-font">{{ formatDateTime(row.triggeredAt) }}</span></template>
        </t-table>
      </template>
      <t-empty v-else :description="$t('dashboard.noAlerts')" />
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { graphic } from 'echarts/core'
import VChart from 'vue-echarts'
import {
  RefreshIcon, ErrorCircleFilledIcon, CpuIcon, ChartLineIcon,
  DesktopIcon, ChartPieIcon, ViewListIcon, UploadIcon, DownloadIcon,
  ErrorCircleIcon, SecuredIcon,
} from 'tdesign-icons-vue-next'
import { get } from '@/api/client'
import type { DashboardResponse } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { formatBytes, formatDateTime } from '@/composables/useFormat'
import { severityTag, statusTag } from '@/composables/useSeverity'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'


const { t } = useI18n()
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

const alertColumns = computed(() => [
  { colKey: 'severity', title: t('dashboard.severity'), width: 100 },
  { colKey: 'alertType', title: t('dashboard.ruleType'), width: 140 },
  { colKey: 'sourceIp', title: t('dashboard.sourceIp'), width: 150 },
  { colKey: 'destIp', title: t('dashboard.destIp'), width: 150 },
  { colKey: 'protocol', title: t('dashboard.protocol'), width: 80 },
  { colKey: 'description', title: t('dashboard.description'), minWidth: 220, ellipsis: true },
  { colKey: 'status', title: t('common.status'), width: 120 },
  { colKey: 'triggeredAt', title: t('dashboard.triggeredAt'), width: 180 },
])

// ── ECharts colour mapping ──
const SEVERITY_COLORS: Record<string, string> = {
  critical: '#e34d59',
  high: '#ed7b2f',
  medium: '#0052d9',
  low: '#00a870',
  info: '#909399',
}

const STATUS_COLORS: Record<string, string> = {
  new: '#e34d59',
  acknowledged: '#ed7b2f',
  investigating: '#0052d9',
  resolved: '#00a870',
  closed: '#909399',
}

// ── Severity Pie Chart ──
const severityChartOption = computed(() => {
  const entries = Object.entries(data.alertsBySeverity)
  if (!entries.length) return noDataOption(t('common.noData'))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; percent: number }) =>
        `<strong>${p.name}</strong><br/>${t('dashboard.threatAlerts')}: ${p.value} (${p.percent}%)`,
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
      data: entries.map(([name, value]) => ({
        name: t(`severity.${name}`, name.toUpperCase()),
        value,
        itemStyle: { color: SEVERITY_COLORS[name] ?? '#909399' },
      })),
    }],
  }
})

// ── Status Doughnut Chart ──
const statusChartOption = computed(() => {
  const entries = Object.entries(data.alertsByStatus)
  if (!entries.length) return noDataOption(t('common.noData'))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; percent: number }) =>
        `<strong>${p.name}</strong><br/>${t('dashboard.threatAlerts')}: ${p.value} (${p.percent}%)`,
    },
    legend: {
      bottom: 0,
      textStyle: { fontSize: 12 },
      itemWidth: 10,
      itemHeight: 10,
    },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      padAngle: 2,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontWeight: 'bold', fontSize: 14 },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.15)' },
      },
      data: entries.map(([name, value]) => ({
        name: t(`alertStatus.${name}`, name.toUpperCase()),
        value,
        itemStyle: { color: STATUS_COLORS[name] ?? '#909399' },
      })),
    }],
  }
})

// ── Top Sources Horizontal Bar ──
const sourceChartOption = computed(() => {
  const entries = data.trafficSummary?.topSources
  if (!entries?.length) return noDataOption(t('dashboard.noTraffic'))

  const ips = entries.map(e => e.ip).reverse()
  const bytes = entries.map(e => e.totalBytes).reverse()

  return horizontalBarOption(ips, bytes, t('dashboard.bytes'))
})

// ── Top Destinations Horizontal Bar ──
const destChartOption = computed(() => {
  const entries = data.trafficSummary?.topDestinations
  if (!entries?.length) return noDataOption(t('dashboard.noTraffic'))

  const ips = entries.map(e => e.ip).reverse()
  const bytes = entries.map(e => e.totalBytes).reverse()

  return horizontalBarOption(ips, bytes, t('dashboard.bytes'), '#00a870')
})

// ── Shared helpers ──
function noDataOption(msg: string) {
  return {
    title: { text: msg, left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } },
    series: [{ type: 'pie', data: [], radius: 0 }],
    xAxis: undefined as any,
    yAxis: undefined as any,
    tooltip: undefined as any,
    legend: undefined as any,
  }
}

function horizontalBarOption(
  labels: string[],
  values: number[],
  unit: string,
  barColor = '#0052d9',
) {
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
            { offset: 0, color: barColor + '66' },
            { offset: 1, color: barColor },
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

async function fetchData() {
  loading.value = true
  try {
    const res = await get<DashboardResponse>('/dashboard')
    if (res.code === 200) Object.assign(data, res.data)
  } finally { loading.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.dashboard { padding: 0; }

.header-icon-primary { color: var(--color-primary); background: var(--color-primary-light); }

.refresh-btn {
  border-radius: var(--radius-lg);
  font-weight: 500;
  box-shadow: var(--shadow-button);
}

.card-header-icon { color: var(--color-primary); }

/* Chart wrapper inside card */
:deep(.chart-wrapper) {
  width: 100%;
  height: 280px;
  min-height: 200px;
}
</style>