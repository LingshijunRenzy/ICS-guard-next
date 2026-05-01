<template>
  <div class="audit-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon header-icon-info"><Document /></el-icon>
        <h2>{{ $t('audit.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchList(pageNum)">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item :label="$t('audit.action')">
          <el-input v-model="filters.action" placeholder="e.g. login" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('audit.username')">
          <el-input v-model="filters.username" :placeholder="$t('audit.usernamePlaceholder')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('audit.resource')">
          <el-input v-model="filters.resource" placeholder="e.g. alert" clearable style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList(1)">{{ $t('common.search') }}</el-button>
          <el-button @click="resetFilters">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <SkeletonTable v-if="loading && !logs.length" :rows="5" :cols="7" />
    <el-card v-else shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon-info"><List /></el-icon> {{ $t('audit.auditRecords') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="logs.length">
        <el-table v-loading="loading" :data="logs" stripe size="default" class="tech-table">
          <el-table-column :label="$t('audit.action')" width="160">
            <template #default="{ row }">
              <el-tag :type="auditActionTag(row.action)" size="small" effect="dark">{{ row.action.toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.user')" width="130">
            <template #default="{ row }"><span class="tech-font">{{ row.username ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('audit.resource')" width="130">
            <template #default="{ row }"><span class="tech-font mono">{{ row.resource ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('audit.resourceId')" width="130">
            <template #default="{ row }"><span class="tech-font mono">{{ row.resourceId ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('audit.ipAddress')" width="160">
            <template #default="{ row }">
              <span v-if="row.ipAddress" class="tech-font mono">{{ row.ipAddress }}</span>
              <span v-else class="tech-font">{{ $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.traceId')" width="280" show-overflow-tooltip>
            <template #default="{ row }"><span class="tech-font mono trace-id">{{ row.traceId }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('audit.timestamp')" width="180">
            <template #default="{ row }"><span class="tech-font">{{ formatDateTime(row.createdAt) }}</span></template>
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
      <el-empty v-else :description="$t('audit.noLogs')" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Document, Refresh, List } from '@element-plus/icons-vue'
import { get } from '@/api/client'
import type { AuditLogResponse, PageDTO } from '@/api/types'
import { formatDateTime } from '@/composables/useFormat'
import { auditActionTag } from '@/composables/useSeverity'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

const loading = ref(false)
const logs = ref<AuditLogResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<AuditLogResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const filters = reactive({ action: '', username: '', resource: '' })

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

.card-header-icon-info { color: var(--color-info); font-size: 18px; }
</style>
