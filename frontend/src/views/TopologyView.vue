<template>
  <div class="topology-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-success"><ShareIcon size="22px" /></span>
        <h2>{{ $t('topology.title') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" @click="fetchAll">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refresh') }}
      </t-button>
    </div>

    <t-card :bordered="false" class="section-card">
      <template #header>
        <div class="card-header">
          <span><CpuIcon size="18px" class="card-header-icon-success" /> {{ $t('topology.connectedDevices') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ devices.length }}</span>
        </div>
      </template>
      <template v-if="loading && !devices.length">
        <t-row :gutter="16">
          <t-col v-for="n in 4" :key="n" :span="3"><SkeletonCard /></t-col>
        </t-row>
      </template>
      <template v-else-if="devices.length">
        <t-row :gutter="16">
          <t-col v-for="d in devices" :key="d.deviceId" :span="3">
            <t-card :bordered="false" class="device-card" @click="selectDevice(d.deviceId)">
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
            </t-card>
          </t-col>
        </t-row>
      </template>
      <t-empty v-else :description="$t('topology.noDevices')" />
    </t-card>

    <SkeletonTable v-if="loading && !events.length" :rows="5" :cols="6" />
    <t-card v-else :bordered="false" class="section-card">
      <template #header>
        <div class="card-header">
          <span><ViewListIcon size="18px" class="card-header-icon-success" /> {{ $t('topology.topologyEvents') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="events.length">
        <t-table :loading="loading" :data="events" :columns="eventColumns" stripe size="medium" class="tech-table" row-key="id">
          <template #eventType="{ row }">
            <t-tag :theme="eventTag(row.eventType)" size="small" variant="dark">{{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}</t-tag>
          </template>
          <template #deviceName="{ row }"><span class="tech-font">{{ row.deviceName }}</span></template>
          <template #deviceType="{ row }"><span class="tech-font mono">{{ row.deviceType }}</span></template>
          <template #ipAddress="{ row }">
            <span v-if="row.ipAddress" class="tech-font mono">{{ row.ipAddress }}</span>
            <span v-else class="tech-font">{{ $t('common.dash') }}</span>
          </template>
          <template #macAddress="{ row }">
            <span v-if="row.macAddress" class="tech-font mono">{{ row.macAddress }}</span>
            <span v-else class="tech-font">{{ $t('common.dash') }}</span>
          </template>
          <template #port="{ row }"><span class="tech-font mono">{{ row.port ?? $t('common.dash') }}</span></template>
          <template #status="{ row }">
            <t-tag :theme="topoStatusTag(row.status)" variant="light">{{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}</t-tag>
          </template>
          <template #occurredAt="{ row }"><span class="tech-font">{{ formatDateTime(row.occurredAt) }}</span></template>
        </t-table>
        <div class="pagination-wrap">
          <t-pagination
            v-model:current="pageNum"
            v-model:page-size="pageSize"
            :total="page.totalElements"
            :page-size-options="[10, 20, 50]"
            show-page-number
            show-page-size
            @page-size-change="fetchEvents(1)"
            @current-change="fetchEvents"
          />
        </div>
      </template>
      <t-empty v-else :description="$t('topology.noEvents')" />
    </t-card>

    <t-dialog v-model:visible="historyVisible" :header="$t('topology.deviceHistory', { id: historyId })" width="800px" destroy-on-close>
      <template v-if="deviceHistory.length">
        <t-table :data="deviceHistory" :columns="historyColumns" size="small" max-height="400" row-key="id">
          <template #eventType="{ row }">
            <t-tag :theme="eventTag(row.eventType)" size="small" variant="dark">{{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}</t-tag>
          </template>
          <template #status="{ row }">
            <t-tag :theme="topoStatusTag(row.status)" variant="light">{{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}</t-tag>
          </template>
          <template #ipAddress="{ row }"><span class="tech-font mono">{{ row.ipAddress ?? $t('common.dash') }}</span></template>
          <template #macAddress="{ row }"><span class="tech-font mono">{{ row.macAddress ?? $t('common.dash') }}</span></template>
          <template #port="{ row }"><span class="tech-font mono">{{ row.port ?? $t('common.dash') }}</span></template>
          <template #occurredAt="{ row }"><span class="tech-font">{{ formatDateTime(row.occurredAt) }}</span></template>
        </t-table>
      </template>
      <t-empty v-else :description="$t('topology.noHistory')" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ShareIcon, RefreshIcon, CpuIcon, ViewListIcon } from 'tdesign-icons-vue-next'
import { get } from '@/api/client'
import type { TopologyEventResponse, PageDTO } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '@/composables/useFormat'
import { eventTag, topoStatusTag, deviceStatusClass } from '@/composables/useSeverity'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

const { t } = useI18n()
const loading = ref(false)
const devices = ref<TopologyEventResponse[]>([])
const events = ref<TopologyEventResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<TopologyEventResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const historyVisible = ref(false)
const historyId = ref('')
const deviceHistory = ref<TopologyEventResponse[]>([])

const eventColumns = computed(() => [
  { colKey: 'eventType', title: t('topology.eventType'), width: 140 },
  { colKey: 'deviceName', title: t('topology.deviceName'), minWidth: 140 },
  { colKey: 'deviceType', title: t('topology.deviceType'), width: 100 },
  { colKey: 'ipAddress', title: t('topology.ipAddress'), width: 160 },
  { colKey: 'macAddress', title: t('topology.macAddress'), width: 160 },
  { colKey: 'port', title: t('topology.port'), width: 80 },
  { colKey: 'status', title: t('common.status'), width: 110 },
  { colKey: 'occurredAt', title: t('topology.occurredAt'), width: 180 },
])

const historyColumns = computed(() => [
  { colKey: 'eventType', title: t('topology.eventType'), width: 120 },
  { colKey: 'status', title: t('common.status'), width: 100 },
  { colKey: 'ipAddress', title: t('topology.ipAddress'), width: 150 },
  { colKey: 'macAddress', title: t('topology.macAddress'), width: 160 },
  { colKey: 'port', title: t('topology.port'), width: 80 },
  { colKey: 'occurredAt', title: t('topology.occurredAt'), width: 170 },
])

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

function fetchAll() { fetchDevices(); fetchEvents(pageNum.value) }

onMounted(fetchAll)
</script>

<style scoped>
.topology-page { padding: 0; }

.header-icon-success { color: var(--color-success); background: var(--color-success-light); }

.card-header-icon-success { color: var(--color-success); }
</style>
