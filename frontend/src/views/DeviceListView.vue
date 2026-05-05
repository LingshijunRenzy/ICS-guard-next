<template>
  <div class="device-list-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-primary"><CpuIcon size="22px" /></span>
        <h2>{{ $t('topology.devices') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" @click="fetchDevices">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refresh') }}
      </t-button>
    </div>

    <t-card :bordered="false" class="section-card">
      <template #header>
        <div class="card-header">
          <span><CpuIcon size="18px" class="card-header-icon-primary" /> {{ $t('topology.connectedDevices') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ devices.length }}</span>
        </div>
      </template>

      <t-table
        v-if="devices.length"
        :loading="loading"
        :data="devices"
        :columns="deviceColumns"
        stripe
        size="medium"
        class="tech-table"
        row-key="deviceId"
      >
        <template #deviceType="{ row }">
          <t-tag variant="dark" size="small">{{ row.deviceType }}</t-tag>
        </template>
        <template #status="{ row }">
          <t-tag :theme="deviceStatusTheme(row.status)" variant="light">
            {{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}
          </t-tag>
        </template>
        <template #ipAddress="{ row }">
          <span v-if="row.ipAddress" class="tech-font mono">{{ row.ipAddress }}</span>
          <span v-else class="tech-font">{{ $t('common.dash') }}</span>
        </template>
        <template #macAddress="{ row }">
          <span v-if="row.macAddress" class="tech-font mono">{{ row.macAddress }}</span>
          <span v-else class="tech-font">{{ $t('common.dash') }}</span>
        </template>
        <template #port="{ row }">
          <span class="tech-font mono">{{ row.port ?? $t('common.dash') }}</span>
        </template>
        <template #actions="{ row }">
          <t-button variant="text" theme="primary" @click="viewHistory(row.deviceId)">
            {{ $t('topology.deviceHistory', { id: '' }).split(' — ')[0] }}
          </t-button>
        </template>
      </t-table>
      <t-empty v-else :description="$t('topology.noDevices')" />
    </t-card>

    <t-dialog v-model:visible="historyVisible" :header="$t('topology.deviceHistory', { id: historyId })" width="800px" destroy-on-close>
      <template v-if="deviceHistory.length">
        <t-table :data="deviceHistory" :columns="historyColumns" size="small" max-height="400" row-key="id">
          <template #eventType="{ row }">
            <t-tag :theme="eventTag(row.eventType)" size="small" variant="dark">
              {{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}
            </t-tag>
          </template>
          <template #status="{ row }">
            <t-tag :theme="deviceStatusTheme(row.status)" variant="light">
              {{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}
            </t-tag>
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
import { computed, onMounted, ref } from 'vue'
import { CpuIcon, RefreshIcon } from 'tdesign-icons-vue-next'
import { get } from '@/api/client'
import type { TopologyEventResponse } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '@/composables/useFormat'
import { eventTag } from '@/composables/useSeverity'

const { t } = useI18n()
const loading = ref(false)
const devices = ref<TopologyEventResponse[]>([])
const historyVisible = ref(false)
const historyId = ref('')
const deviceHistory = ref<TopologyEventResponse[]>([])

function deviceStatusTheme(status: string) {
  return status === 'online' ? 'success' : 'danger'
}

const deviceColumns = computed(() => [
  { colKey: 'deviceName', title: t('topology.deviceName'), minWidth: 160 },
  { colKey: 'deviceType', title: t('topology.deviceType'), width: 100 },
  { colKey: 'ipAddress', title: t('topology.ipAddress'), width: 160 },
  { colKey: 'macAddress', title: t('topology.macAddress'), width: 160 },
  { colKey: 'port', title: t('topology.port'), width: 80 },
  { colKey: 'status', title: t('common.status'), width: 100 },
  { colKey: 'actions', title: t('common.actions'), width: 100 },
])

const historyColumns = computed(() => [
  { colKey: 'eventType', title: t('topology.eventType'), width: 130 },
  { colKey: 'status', title: t('common.status'), width: 100 },
  { colKey: 'ipAddress', title: t('topology.ipAddress'), width: 150 },
  { colKey: 'macAddress', title: t('topology.macAddress'), width: 160 },
  { colKey: 'port', title: t('topology.port'), width: 80 },
  { colKey: 'occurredAt', title: t('topology.occurredAt'), width: 170 },
])

async function fetchDevices() {
  loading.value = true
  try {
    const res = await get<TopologyEventResponse[]>('/topology/devices')
    if (res.code === 200) devices.value = res.data
  } finally { loading.value = false }
}

async function viewHistory(deviceId: string) {
  historyId.value = deviceId
  historyVisible.value = true
  try {
    const res = await get<TopologyEventResponse[]>(`/topology/devices/${deviceId}/history`)
    if (res.code === 200) deviceHistory.value = res.data
  } catch { deviceHistory.value = [] }
}

onMounted(fetchDevices)
</script>

<style scoped>
.device-list-page { padding: 0; }
.header-icon-primary { color: var(--color-primary); background: var(--color-primary-light); }
.card-header-icon-primary { color: var(--color-primary); }
</style>