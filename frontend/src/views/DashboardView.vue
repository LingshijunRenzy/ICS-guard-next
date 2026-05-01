<template>
  <div class="dashboard">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon header-icon-primary"><Monitor /></el-icon>
        <h2>{{ $t('dashboard.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" class="refresh-btn" @click="fetchData">
        {{ $t('common.refreshStatus') }}
      </el-button>
    </div>

    <el-row :gutter="24" class="stat-row">
      <template v-if="loading">
        <el-col v-for="n in 4" :key="'sk' + n" :span="6">
          <SkeletonCard />
        </el-col>
      </template>
      <template v-else>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper danger-bg"><el-icon><WarningFilled /></el-icon></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('dashboard.threatAlerts') }}</div>
                <div class="stat-value danger-text">{{ data.totalAlerts }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper success-bg"><el-icon><Cpu /></el-icon></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('dashboard.onlineDevices') }}</div>
                <div class="stat-value success-text">{{ data.totalDevices }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper primary-bg"><el-icon><Key /></el-icon></div>
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
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper warning-bg"><el-icon><DataLine /></el-icon></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('dashboard.trackedSources') }}</div>
                <div class="stat-value warning-text">{{ data.trafficSummary?.topSources?.length ?? 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </template>
    </el-row>

    <el-row :gutter="24" class="chart-row">
      <el-col :span="12">
        <SkeletonList v-if="loading" :items="3" />
        <el-card v-else shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon"><PieChart /></el-icon> {{ $t('dashboard.alertsBySeverity') }}</span>
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
        <SkeletonList v-if="loading" :items="3" />
        <el-card v-else shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon"><List /></el-icon> {{ $t('dashboard.alertsByStatus') }}</span>
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
        <SkeletonList v-if="loading" :items="4" />
        <el-card v-else shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon"><Upload /></el-icon> {{ $t('dashboard.topSources') }}</span>
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
            <el-empty v-else :description="$t('dashboard.noTraffic')" :image-size="80" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <SkeletonList v-if="loading" :items="4" />
        <el-card v-else shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon"><Download /></el-icon> {{ $t('dashboard.topDestinations') }}</span>
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
            <el-empty v-else :description="$t('dashboard.noTraffic')" :image-size="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <SkeletonTable v-if="loading" :rows="5" :cols="7" />
    <el-card v-else shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon"><Warning /></el-icon> {{ $t('dashboard.recentAlerts') }}</span>
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
            <template #default="{ row }"><span class="tech-font">{{ row.alertType }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.sourceIp')" width="150">
            <template #default="{ row }"><span class="tech-font mono">{{ row.sourceIp }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.destIp')" width="150">
            <template #default="{ row }"><span class="tech-font mono">{{ row.destIp }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.protocol')" width="80" />
          <el-table-column :label="$t('dashboard.description')" min-width="220" show-overflow-tooltip />
          <el-table-column :label="$t('common.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="plain" class="status-tag">{{ $t(`alertStatus.${row.status}`, row.status.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('dashboard.triggeredAt')" width="180">
            <template #default="{ row }"><span class="tech-font">{{ formatDateTime(row.triggeredAt) }}</span></template>
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
import { formatBytes, formatDateTime, calcPercent, calcBarPercent, hasEntries } from '@/composables/useFormat'
import { severityTag, statusTag, severityColor, statusColor } from '@/composables/useSeverity'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonList from '@/components/skeleton/SkeletonList.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

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

.card-header-icon { color: var(--color-primary); font-size: 18px; }
</style>
