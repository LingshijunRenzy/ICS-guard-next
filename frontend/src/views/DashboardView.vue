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

    <t-row :gutter="[16, 16]" class="chart-row">
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="3" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><ChartPieIcon size="18px" class="card-header-icon" /> {{ $t('dashboard.alertsBySeverity') }}</span>
            </div>
          </template>
          <div class="list-container">
            <template v-if="hasEntries(data.alertsBySeverity)">
              <div v-for="(count, sev) in data.alertsBySeverity" :key="sev" class="list-item">
                <span class="list-label">{{ $t(`severity.${sev}`, sev.toUpperCase()) }}</span>
                <div class="progress-wrapper">
                  <t-progress :percentage="calcPercent(count, data.totalAlerts)" :color="severityColor(sev)" :label="false" :stroke-width="10" />
                </div>
                <span class="list-value">{{ count }}</span>
              </div>
            </template>
            <t-empty v-else :description="$t('common.noData')" />
          </div>
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
          <div class="list-container">
            <template v-if="hasEntries(data.alertsByStatus)">
              <div v-for="(count, st) in data.alertsByStatus" :key="st" class="list-item">
                <span class="list-label">{{ $t(`alertStatus.${st}`, st.toUpperCase()) }}</span>
                <div class="progress-wrapper">
                  <t-progress :percentage="calcPercent(count, data.totalAlerts)" :color="statusColor(st)" :label="false" :stroke-width="10" />
                </div>
                <span class="list-value">{{ count }}</span>
              </div>
            </template>
            <t-empty v-else :description="$t('common.noData')" />
          </div>
        </t-card>
      </t-col>
    </t-row>

    <t-row :gutter="[16, 16]" class="chart-row">
      <t-col :span="6">
        <SkeletonList v-if="loading" :items="4" />
        <t-card v-else :bordered="false" class="data-card">
          <template #header>
            <div class="card-header">
              <span><UploadIcon size="18px" class="card-header-icon" /> {{ $t('dashboard.topSources') }}</span>
            </div>
          </template>
          <div class="list-container">
            <template v-if="data.trafficSummary?.topSources?.length">
              <div v-for="(e, idx) in data.trafficSummary.topSources" :key="e.ip" class="list-item-traffic">
                <div class="traffic-info">
                  <span class="traffic-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                  <span class="traffic-ip mono">{{ e.ip }}</span>
                  <span class="traffic-bytes">{{ formatBytes(e.totalBytes) }}</span>
                </div>
                <div class="traffic-bar" :style="{ width: calcBarPercent(e.totalBytes, data.trafficSummary.topSources[0].totalBytes) + '%', backgroundColor: 'var(--color-primary)' }" />
              </div>
            </template>
            <t-empty v-else :description="$t('dashboard.noTraffic')" />
          </div>
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
          <div class="list-container">
            <template v-if="data.trafficSummary?.topDestinations?.length">
              <div v-for="(e, idx) in data.trafficSummary.topDestinations" :key="e.ip" class="list-item-traffic">
                <div class="traffic-info">
                  <span class="traffic-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                  <span class="traffic-ip mono">{{ e.ip }}</span>
                  <span class="traffic-bytes">{{ formatBytes(e.totalBytes) }}</span>
                </div>
                <div class="traffic-bar" :style="{ width: calcBarPercent(e.totalBytes, data.trafficSummary.topDestinations[0].totalBytes) + '%', backgroundColor: 'var(--color-success)' }" />
              </div>
            </template>
            <t-empty v-else :description="$t('dashboard.noTraffic')" />
          </div>
        </t-card>
      </t-col>
    </t-row>

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
import {
  RefreshIcon, ErrorCircleFilledIcon, CpuIcon, ChartLineIcon,
  DesktopIcon, ChartPieIcon, ViewListIcon, UploadIcon, DownloadIcon,
  ErrorCircleIcon, SecuredIcon,
} from 'tdesign-icons-vue-next'
import { get } from '@/api/client'
import type { DashboardResponse } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { formatBytes, formatDateTime, calcPercent, calcBarPercent, hasEntries } from '@/composables/useFormat'
import { severityTag, statusTag, severityColor, statusColor } from '@/composables/useSeverity'
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
</style>
