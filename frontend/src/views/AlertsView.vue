<template>
  <div class="alerts-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon header-icon-warning"><Bell /></el-icon>
        <h2>{{ $t('alerts.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchAll">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <el-row :gutter="24" class="stat-row">
      <el-col :span="6">
        <SkeletonCard v-if="loading" />
        <el-card v-else shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper primary-bg"><el-icon><WarningFilled /></el-icon></div>
            <div class="stat-info">
              <div class="stat-title">{{ $t('alerts.totalAlerts') }}</div>
              <div class="stat-value primary-text">{{ stats.total }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="9">
        <SkeletonCard v-if="loading" />
        <el-card v-else shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper danger-bg"><el-icon><PieChart /></el-icon></div>
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
        </el-card>
      </el-col>
      <el-col :span="9">
        <SkeletonCard v-if="loading" />
        <el-card v-else shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper success-bg"><el-icon><List /></el-icon></div>
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
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item :label="$t('alerts.severity')">
          <el-select v-model="filters.severity" :placeholder="$t('common.dash')" clearable style="width:130px">
            <el-option :label="$t('severity.critical')" value="critical" />
            <el-option :label="$t('severity.high')" value="high" />
            <el-option :label="$t('severity.medium')" value="medium" />
            <el-option :label="$t('severity.low')" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('alerts.status')">
          <el-select v-model="filters.status" :placeholder="$t('common.dash')" clearable style="width:140px">
            <el-option :label="$t('alertStatus.new')" value="new" />
            <el-option :label="$t('alertStatus.acknowledged')" value="acknowledged" />
            <el-option :label="$t('alertStatus.resolved')" value="resolved" />
            <el-option :label="$t('alertStatus.escalated')" value="escalated" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('alerts.alertType')">
          <el-input v-model="filters.alertType" placeholder="e.g. anomaly" clearable style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList(1)">{{ $t('common.search') }}</el-button>
          <el-button @click="resetFilters">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <SkeletonTable v-if="loading && !alerts.length" :rows="5" :cols="7" />
    <el-card v-else shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon-warning"><Warning /></el-icon> {{ $t('alerts.alertList') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="alerts.length">
        <el-table v-loading="loading" :data="alerts" stripe size="default" class="tech-table">
          <el-table-column :label="$t('alerts.severity')" width="110">
            <template #default="{ row }">
              <el-tag :type="severityTag(row.severity)" size="small" effect="dark">{{ $t(`severity.${row.severity}`, row.severity.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('alerts.alertType')" width="150">
            <template #default="{ row }"><span class="tech-font mono">{{ row.alertType }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.sourceIp')" width="160">
            <template #default="{ row }"><span class="tech-font mono">{{ row.sourceIp }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.destIp')" width="160">
            <template #default="{ row }"><span class="tech-font mono">{{ row.destIp }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.protocol')" width="80" />
          <el-table-column :label="$t('dashboard.description')" min-width="200" show-overflow-tooltip />
          <el-table-column :label="$t('common.status')" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="plain">{{ $t(`alertStatus.${row.status}`, row.status.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.triggeredAt')" width="180">
            <template #default="{ row }"><span class="tech-font">{{ formatDateTime(row.triggeredAt) }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('common.actions')" width="200" fixed="right">
            <template #default="{ row }">
              <el-dropdown @command="(cmd: string) => handleAction(cmd, row)">
                <el-button size="small" type="primary" link>
                  {{ $t('common.actions') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="ack">{{ $t('alerts.acknowledge') }}</el-dropdown-item>
                    <el-dropdown-item command="resolve">{{ $t('alerts.resolve') }}</el-dropdown-item>
                    <el-dropdown-item command="escalate" :disabled="row.status === 'escalated'">{{ $t('alerts.escalate') }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
            @size-change="fetchList(1)"
            @current-change="fetchList"
          />
        </div>
      </template>
      <el-empty v-else :description="$t('alerts.noAlerts')" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Bell, Refresh, WarningFilled, PieChart, List, Warning, ArrowDown } from '@element-plus/icons-vue'
import { get, patch, post } from '@/api/client'
import type { AlertResponse, AlertStatsResponse, PageDTO } from '@/api/types'
import { formatDateTime } from '@/composables/useFormat'
import { severityTag, statusTag } from '@/composables/useSeverity'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

const loading = ref(false)
const alerts = ref<AlertResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<AlertResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const stats = reactive<AlertStatsResponse>({ bySeverity: {}, byStatus: {}, total: 0 })

const filters = reactive({ severity: '', status: '', alertType: '' })

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
  } finally {
    loading.value = false
  }
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

function fetchAll() {
  fetchStats()
  fetchList(pageNum.value)
}

onMounted(fetchAll)
</script>

<style scoped>
.alerts-page { padding: 0; }

.header-icon-warning { color: var(--color-warning); background: var(--color-warning-light); }

.card-header-icon-warning { color: var(--color-warning); font-size: 18px; }
</style>
