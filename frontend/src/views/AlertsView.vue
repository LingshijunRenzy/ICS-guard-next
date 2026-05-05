<template>
  <div class="alerts-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-warning"><NotificationIcon size="22px" /></span>
        <h2>{{ $t('alerts.title') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" @click="fetchAll">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refresh') }}
      </t-button>
    </div>

    <t-row :gutter="[16, 16]" class="stat-row">
      <t-col :span="4">
        <SkeletonCard v-if="loading" />
        <t-card v-else :bordered="false" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper primary-bg"><ErrorCircleFilledIcon size="28px" /></div>
            <div class="stat-info">
              <div class="stat-title">{{ $t('alerts.totalAlerts') }}</div>
              <div class="stat-value primary-text">{{ stats.total }}</div>
            </div>
          </div>
        </t-card>
      </t-col>
      <t-col :span="4">
        <SkeletonCard v-if="loading" />
        <t-card v-else :bordered="false" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper danger-bg"><ChartPieIcon size="28px" /></div>
            <div class="stat-info wide">
              <div class="stat-title">{{ $t('alerts.severitySummary') }}</div>
              <div class="stat-value-group">
                <span class="sv-danger">{{ stats.bySeverity?.critical ?? 0 }}</span>
                <span class="sv-sep">/</span>
                <span class="sv-warning">{{ stats.bySeverity?.high ?? 0 }}</span>
                <span class="sv-sep">/</span>
                <span class="sv-info">{{ (stats.bySeverity?.medium ?? 0) + (stats.bySeverity?.low ?? 0) }}</span>
              </div>
            </div>
          </div>
        </t-card>
      </t-col>
      <t-col :span="4">
        <SkeletonCard v-if="loading" />
        <t-card v-else :bordered="false" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper success-bg"><ViewListIcon size="28px" /></div>
            <div class="stat-info wide">
              <div class="stat-title">{{ $t('alerts.statusSummary') }}</div>
              <div class="stat-value-group">
                <span class="sv-success">{{ stats.byStatus?.resolved ?? 0 }}</span>
                <span class="sv-sep">/</span>
                <span class="sv-warning">{{ stats.byStatus?.acknowledged ?? 0 }}</span>
                <span class="sv-sep">/</span>
                <span class="sv-info">{{ stats.byStatus?.new ?? 0 }}</span>
                <span class="sv-sep">/</span>
                <span class="sv-danger">{{ stats.byStatus?.escalated ?? 0 }}</span>
              </div>
            </div>
          </div>
        </t-card>
      </t-col>
    </t-row>

    <t-card :bordered="false" class="filter-card">
      <t-form :data="filters" layout="inline">
        <t-form-item :label="$t('alerts.severity')">
          <t-select v-model="filters.severity" :placeholder="$t('common.dash')" clearable style="width:130px">
            <t-option :label="$t('severity.critical')" value="critical" />
            <t-option :label="$t('severity.high')" value="high" />
            <t-option :label="$t('severity.medium')" value="medium" />
            <t-option :label="$t('severity.low')" value="low" />
          </t-select>
        </t-form-item>
        <t-form-item :label="$t('alerts.status')">
          <t-select v-model="filters.status" :placeholder="$t('common.dash')" clearable style="width:140px">
            <t-option :label="$t('alertStatus.new')" value="new" />
            <t-option :label="$t('alertStatus.acknowledged')" value="acknowledged" />
            <t-option :label="$t('alertStatus.resolved')" value="resolved" />
            <t-option :label="$t('alertStatus.escalated')" value="escalated" />
          </t-select>
        </t-form-item>
        <t-form-item :label="$t('alerts.alertType')">
          <t-input v-model="filters.alertType" placeholder="e.g. anomaly" clearable style="width:160px" />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" @click="fetchList(1)">{{ $t('common.search') }}</t-button>
          <t-button variant="outline" @click="resetFilters">{{ $t('common.reset') }}</t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <SkeletonTable v-if="loading && !alerts.length" :rows="5" :cols="7" />
    <t-card v-else :bordered="false" class="table-card">
      <template #header>
        <div class="card-header">
          <span><ErrorCircleIcon size="18px" class="card-header-icon-warning" /> {{ $t('alerts.alertList') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="alerts.length">
        <t-table :loading="loading" :data="alerts" :columns="alertColumns" stripe size="medium" class="tech-table" row-key="id">
          <template #severity="{ row }">
            <t-tag :theme="severityTag(row.severity)" size="small" variant="dark">{{ $t(`severity.${row.severity}`, row.severity.toUpperCase()) }}</t-tag>
          </template>
          <template #alertType="{ row }"><span class="tech-font mono">{{ row.alertType }}</span></template>
          <template #sourceIp="{ row }"><span class="tech-font mono">{{ row.sourceIp }}</span></template>
          <template #destIp="{ row }"><span class="tech-font mono">{{ row.destIp }}</span></template>
          <template #status="{ row }">
            <t-tag :theme="statusTag(row.status)" variant="light">{{ $t(`alertStatus.${row.status}`, row.status.toUpperCase()) }}</t-tag>
          </template>
          <template #triggeredAt="{ row }"><span class="tech-font">{{ formatDateTime(row.triggeredAt) }}</span></template>
          <template #actions="{ row }">
            <t-dropdown v-if="auth.hasPermission('alerts:manage')" :options="getActionOptions(row)" @click="(item: any) => handleAction(item.value, row)">
              <t-button variant="text" theme="primary" size="small">
                {{ $t('common.actions') }} <ChevronDownIcon />
              </t-button>
            </t-dropdown>
          </template>
        </t-table>
        <div class="pagination-wrap">
          <t-pagination
            v-model:current="pageNum"
            v-model:page-size="pageSize"
            :total="page.totalElements"
            :page-size-options="[10, 20, 50]"
            show-page-number
            show-page-size
            @page-size-change="fetchList(1)"
            @current-change="fetchList"
          />
        </div>
      </template>
      <t-empty v-else :description="$t('alerts.noAlerts')" />
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { NotificationIcon, RefreshIcon, ErrorCircleFilledIcon, ChartPieIcon, ViewListIcon, ErrorCircleIcon, ChevronDownIcon } from 'tdesign-icons-vue-next'
import { get, patch, post } from '@/api/client'
import type { AlertResponse, AlertStatsResponse, PageDTO } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '@/composables/useFormat'
import { severityTag, statusTag } from '@/composables/useSeverity'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
const loading = ref(false)
const alerts = ref<AlertResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<AlertResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const stats = reactive<AlertStatsResponse>({ bySeverity: {}, byStatus: {}, total: 0 })

const filters = reactive({ severity: '', status: '', alertType: '' })

const alertColumns = computed(() => [
  { colKey: 'severity', title: t('alerts.severity'), width: 110 },
  { colKey: 'alertType', title: t('alerts.alertType'), width: 150 },
  { colKey: 'sourceIp', title: t('dashboard.sourceIp'), width: 160 },
  { colKey: 'destIp', title: t('dashboard.destIp'), width: 160 },
  { colKey: 'protocol', title: t('dashboard.protocol'), width: 80 },
  { colKey: 'description', title: t('dashboard.description'), minWidth: 200, ellipsis: true },
  { colKey: 'status', title: t('common.status'), width: 130 },
  { colKey: 'triggeredAt', title: t('dashboard.triggeredAt'), width: 180 },
  { colKey: 'actions', title: t('common.actions'), width: 200, fixed: 'right' as const },
])

function getActionOptions(row: AlertResponse) {
  return [
    { content: t('alerts.acknowledge'), value: 'ack' },
    { content: t('alerts.resolve'), value: 'resolve' },
    { content: t('alerts.escalate'), value: 'escalate', disabled: row.status === 'escalated' },
  ]
}

async function fetchStats() {
  try {
    const res = await get<AlertStatsResponse>('/alerts/stats')
    if (res.code === 200) Object.assign(stats, res.data)
  } catch { /* stats are auxiliary */ }
}

async function fetchList(p: number) {
  loading.value = true
  try {
    const params: Record<string, any> = { page: p - 1, size: pageSize.value }
    if (filters.severity) params.severity = filters.severity
    if (filters.status) params.status = filters.status
    if (filters.alertType) params.alertType = filters.alertType
    const res = await get<PageDTO<AlertResponse>>('/alerts', params)
    if (res.code === 200) {
      Object.assign(page, res.data)
      alerts.value = res.data.content
      pageNum.value = p
    }
  } finally { loading.value = false }
}

async function handleAction(cmd: string, row: AlertResponse) {
  try {
    if (cmd === 'ack') {
      await patch(`/alerts/${row.id}/status`, { status: 'acknowledged' })
    } else if (cmd === 'resolve') {
      await patch(`/alerts/${row.id}/status`, { status: 'resolved' })
    } else if (cmd === 'escalate') {
      await post(`/alerts/${row.id}/escalate`)
    }
    fetchAll()
  } catch { /* handled by interceptor */ }
}

function resetFilters() {
  filters.severity = ''
  filters.status = ''
  filters.alertType = ''
  fetchList(1)
}

function fetchAll() { fetchStats(); fetchList(pageNum.value) }

onMounted(fetchAll)
</script>

<style scoped>
.alerts-page { padding: 0; }

.header-icon-warning { color: var(--color-warning); background: var(--color-warning-light); }

.card-header-icon-warning { color: var(--color-warning); }
</style>
