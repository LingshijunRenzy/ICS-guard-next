<template>
  <div class="audit-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-info"><FileIcon size="22px" /></span>
        <h2>{{ $t('audit.title') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" @click="fetchList(pageNum)">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refresh') }}
      </t-button>
    </div>

    <t-card :bordered="false" class="filter-card">
      <t-form :data="filters" layout="inline">
        <t-form-item :label="$t('audit.action')">
          <t-input v-model="filters.action" placeholder="e.g. login" clearable style="width:160px" />
        </t-form-item>
        <t-form-item :label="$t('audit.username')">
          <t-input v-model="filters.username" :placeholder="$t('audit.usernamePlaceholder')" clearable style="width:160px" />
        </t-form-item>
        <t-form-item :label="$t('audit.resource')">
          <t-input v-model="filters.resource" placeholder="e.g. alert" clearable style="width:160px" />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" @click="fetchList(1)">{{ $t('common.search') }}</t-button>
          <t-button variant="outline" @click="resetFilters">{{ $t('common.reset') }}</t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <SkeletonTable v-if="loading && !logs.length" :rows="5" :cols="7" />
    <t-card v-else :bordered="false" class="table-card">
      <template #header>
        <div class="card-header">
          <span><ViewListIcon size="18px" class="card-header-icon-info" /> {{ $t('audit.auditRecords') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="logs.length">
        <t-table :loading="loading" :data="logs" :columns="columns" stripe size="medium" class="tech-table" row-key="id">
          <template #action="{ row }">
            <t-tag :theme="auditActionTag(row.action)" size="small" variant="dark">{{ row.action.toUpperCase() }}</t-tag>
          </template>
          <template #username="{ row }"><span class="tech-font">{{ row.username ?? $t('common.dash') }}</span></template>
          <template #resource="{ row }"><span class="tech-font mono">{{ row.resource ?? $t('common.dash') }}</span></template>
          <template #resourceId="{ row }"><span class="tech-font mono">{{ row.resourceId ?? $t('common.dash') }}</span></template>
          <template #ipAddress="{ row }">
            <span v-if="row.ipAddress" class="tech-font mono">{{ row.ipAddress }}</span>
            <span v-else class="tech-font">{{ $t('common.dash') }}</span>
          </template>
          <template #traceId="{ row }"><span class="tech-font mono trace-id">{{ row.traceId }}</span></template>
          <template #createdAt="{ row }"><span class="tech-font">{{ formatDateTime(row.createdAt) }}</span></template>
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
      <t-empty v-else :description="$t('audit.noLogs')" />
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { FileIcon, RefreshIcon, ViewListIcon } from 'tdesign-icons-vue-next'
import { get } from '@/api/client'
import type { AuditLogResponse, PageDTO } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '@/composables/useFormat'
import { auditActionTag } from '@/composables/useSeverity'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

const { t } = useI18n()
const loading = ref(false)
const logs = ref<AuditLogResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<AuditLogResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const filters = reactive({ action: '', username: '', resource: '' })

const columns = computed(() => [
  { colKey: 'action', title: t('audit.action'), width: 160 },
  { colKey: 'username', title: t('audit.user'), width: 130 },
  { colKey: 'resource', title: t('audit.resource'), width: 130 },
  { colKey: 'resourceId', title: t('audit.resourceId'), width: 130 },
  { colKey: 'ipAddress', title: t('audit.ipAddress'), width: 160 },
  { colKey: 'traceId', title: t('audit.traceId'), width: 280, ellipsis: true },
  { colKey: 'createdAt', title: t('audit.timestamp'), width: 180 },
])

async function fetchList(p: number) {
  loading.value = true
  try {
    const params: Record<string, any> = { page: p - 1, size: pageSize.value }
    if (filters.action) params.action = filters.action
    if (filters.username) params.username = filters.username
    if (filters.resource) params.resource = filters.resource
    const res = await get<PageDTO<AuditLogResponse>>('/audit-logs', params)
    if (res.code === 200) {
      Object.assign(page, res.data)
      logs.value = res.data.content
      pageNum.value = p
    }
  } finally { loading.value = false }
}

function resetFilters() { filters.action = ''; filters.username = ''; filters.resource = ''; fetchList(1) }

onMounted(() => fetchList(1))
</script>

<style scoped>
.audit-page { padding: 0; }

.header-icon-info { color: var(--color-info); background: var(--color-info-light); }

.card-header-icon-info { color: var(--color-info); }
</style>
