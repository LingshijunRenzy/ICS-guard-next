<template>
  <div class="topology-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon header-icon-success"><Share /></el-icon>
        <h2>{{ $t('topology.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchAll">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon-success"><Cpu /></el-icon> {{ $t('topology.connectedDevices') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ devices.length }}</span>
        </div>
      </template>
      <template v-if="loading && !devices.length">
        <el-row :gutter="16">
          <el-col v-for="n in 4" :key="n" :span="6"><SkeletonCard /></el-col>
        </el-row>
      </template>
      <template v-else-if="devices.length">
        <el-row :gutter="16">
          <el-col v-for="d in devices" :key="d.deviceId" :span="6">
            <el-card shadow="hover" class="device-card" @click="selectDevice(d.deviceId)">
              <div class="device-head">
                <div class="device-type-tag" :class="deviceStatusClass(d.status)">{{ $t(`deviceStatus.${d.status}`, d.status.toUpperCase()) }}</div>
              </div>
              <div class="device-name">{{ d.deviceName }}</div>
              <div class="device-info">
                <div class="info-row"><span class="lbl">{{ $t('topology.deviceType') }}</span><span class="tech-font mono">{{ d.deviceType }}</span></div>
                <div v-if="d.ipAddress" class="info-row"><span class="lbl">{{ $t('topology.ipAddress') }}</span><span class="tech-font mono">{{ d.ipAddress }}</span></div>
                <div v-if="d.macAddress" class="info-row"><span class="lbl">{{ $t('topology.macAddress') }}</span><span class="tech-font mono">{{ d.macAddress }}</span></div>
                <div v-if="d.port" class="info-row"><span class="lbl">{{ $t('topology.port') }}</span><span class="tech-font mono">{{ d.port }}</span></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
      <el-empty v-else :description="$t('topology.noDevices')" :image-size="80" />
    </el-card>

    <SkeletonTable v-if="loading && !events.length" :rows="5" :cols="6" />
    <el-card v-else shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon-success"><List /></el-icon> {{ $t('topology.topologyEvents') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="events.length">
        <el-table v-loading="loading" :data="events" stripe size="default" class="tech-table">
          <el-table-column :label="$t('topology.eventType')" width="140">
            <template #default="{ row }">
              <el-tag :type="eventTag(row.eventType)" size="small" effect="dark">{{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.deviceName')" min-width="140">
            <template #default="{ row }"><span class="tech-font">{{ row.deviceName }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('topology.deviceType')" width="100">
            <template #default="{ row }"><span class="tech-font mono">{{ row.deviceType }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('topology.ipAddress')" width="160">
            <template #default="{ row }">
              <span v-if="row.ipAddress" class="tech-font mono">{{ row.ipAddress }}</span>
              <span v-else class="tech-font">{{ $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.macAddress')" width="160">
            <template #default="{ row }">
              <span v-if="row.macAddress" class="tech-font mono">{{ row.macAddress }}</span>
              <span v-else class="tech-font">{{ $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.port')" width="80">
            <template #default="{ row }"><span class="tech-font mono">{{ row.port ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('common.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="topoStatusTag(row.status)" effect="plain">{{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.occurredAt')" width="180">
            <template #default="{ row }"><span class="tech-font">{{ formatDateTime(row.occurredAt) }}</span></template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="page.totalElements"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchEvents(1)"
            @current-change="fetchEvents"
          />
        </div>
      </template>
      <el-empty v-else :description="$t('topology.noEvents')" :image-size="100" />
    </el-card>

    <el-dialog v-model="historyVisible" :title="$t('topology.deviceHistory', { id: historyId })" width="800px" destroy-on-close>
      <template v-if="deviceHistory.length">
        <el-table :data="deviceHistory" stripe size="small" max-height="400">
          <el-table-column :label="$t('topology.eventType')" width="120">
            <template #default="{ row }">
              <el-tag :type="eventTag(row.eventType)" size="small" effect="dark">{{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.status')" width="100">
            <template #default="{ row }">
              <el-tag :type="topoStatusTag(row.status)" effect="plain">{{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.ipAddress')" width="150">
            <template #default="{ row }"><span class="tech-font mono">{{ row.ipAddress ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('topology.macAddress')" width="160">
            <template #default="{ row }"><span class="tech-font mono">{{ row.macAddress ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('topology.port')" width="80">
            <template #default="{ row }"><span class="tech-font mono">{{ row.port ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('topology.occurredAt')" width="170">
            <template #default="{ row }"><span class="tech-font">{{ formatDateTime(row.occurredAt) }}</span></template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else :description="$t('topology.noHistory')" :image-size="80" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Share, Refresh, Cpu, List } from '@element-plus/icons-vue'
import { get } from '@/api/client'
import type { TopologyEventResponse, PageDTO } from '@/api/types'
import { formatDateTime } from '@/composables/useFormat'
import { eventTag, topoStatusTag, deviceStatusClass } from '@/composables/useSeverity'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

const loading = ref(false)
const devices = ref<TopologyEventResponse[]>([])
const events = ref<TopologyEventResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<TopologyEventResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const historyVisible = ref(false)
const historyId = ref('')
const deviceHistory = ref<TopologyEventResponse[]>([])

async function fetchDevices() {
  try {
    const res = await get<TopologyEventResponse[]>('/topology/devices')
    if (res.code === 200) devices.value = res.data
  } catch { /* auxiliary */ }
}

async function fetchEvents(p: number) {
  loading.value = true
  try {
    const res = await get<PageDTO<TopologyEventResponse>>('/topology/events', { page: p - 1, size: pageSize.value })
    if (res.code === 200) {
      Object.assign(page, res.data)
      events.value = res.data.content
      pageNum.value = p
    }
  } finally { loading.value = false }
}

async function selectDevice(deviceId: string) {
  historyId.value = deviceId
  historyVisible.value = true
  try {
    const res = await get<TopologyEventResponse[]>(`/topology/devices/${deviceId}/history`)
    if (res.code === 200) deviceHistory.value = res.data
  } catch { deviceHistory.value = [] }
}

function fetchAll() {
  fetchDevices()
  fetchEvents(pageNum.value)
}

onMounted(fetchAll)
</script>

<style scoped>
.topology-page { padding: 0; }

.header-icon-success { color: var(--color-success); background: var(--color-success-light); }

.card-header-icon-success { color: var(--color-success); font-size: 18px; }
</style>
