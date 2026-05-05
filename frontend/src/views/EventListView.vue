<template>
  <div class="event-list-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-primary"><ViewListIcon size="22px" /></span>
        <h2>{{ $t('topology.events') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" @click="fetchEvents(1)">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refresh') }}
      </t-button>
    </div>

    <t-card :bordered="false" class="section-card">
      <template #header>
        <div class="card-header">
          <span><ViewListIcon size="18px" class="card-header-icon-primary" /> {{ $t('topology.topologyEvents') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>

      <t-table
        v-if="events.length"
        :loading="loading"
        :data="events"
        :columns="eventColumns"
        stripe
        size="medium"
        class="tech-table"
        row-key="id"
      >
        <template #eventType="{ row }">
          <t-tag :theme="eventTag(row.eventType)" size="small" variant="dark">
            {{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}
          </t-tag>
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
          <t-tag :theme="topoStatusTag(row.status)" variant="light">
            {{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}
          </t-tag>
        </template>
        <template #occurredAt="{ row }"><span class="tech-font">{{ formatDateTime(row.occurredAt) }}</span></template>
      </t-table>
      <t-empty v-else :description="$t('topology.noEvents')" />

      <div v-if="events.length" class="pagination-wrap">
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
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ViewListIcon, RefreshIcon } from 'tdesign-icons-vue-next'
import { get } from '@/api/client'
import type { TopologyEventResponse, PageDTO } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '@/composables/useFormat'
import { eventTag, topoStatusTag } from '@/composables/useSeverity'

const { t } = useI18n()
const loading = ref(false)
const events = ref<TopologyEventResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<TopologyEventResponse>>({
  content: [],
  page: 1,
  size: 10,
  totalElements: 0,
  totalPages: 0,
})

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

async function fetchEvents(p: number) {
  loading.value = true
  try {
    const res = await get<PageDTO<TopologyEventResponse>>('/topology/events', {
      page: p - 1,
      size: pageSize.value,
    })
    if (res.code === 200) {
      Object.assign(page, res.data)
      events.value = res.data.content
      pageNum.value = p
    }
  } finally { loading.value = false }
}

onMounted(() => fetchEvents(1))
</script>

<style scoped>
.event-list-page { padding: 0; }
.header-icon-primary { color: var(--color-primary); background: var(--color-primary-light); }
.card-header-icon-primary { color: var(--color-primary); }
</style>