<template>
  <div class="audit-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon"><Document /></el-icon>
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

    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> {{ $t('audit.auditRecords') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="logs.length">
        <el-table :data="logs" stripe size="default" class="tech-table">
          <el-table-column :label="$t('audit.action')" width="160">
            <template #default="{ row }">
              <el-tag :type="actionTag(row.action)" size="small" effect="dark">{{ row.action.toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.user')" width="130">
            <template #default="{ row }">
              <span class="tech-font">{{ row.username ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.resource')" width="130">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.resource ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.resourceId')" width="130">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.resourceId ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.ipAddress')" width="160">
            <template #default="{ row }">
              <span class="tech-font mono" v-if="row.ipAddress">{{ row.ipAddress }}</span>
              <span v-else class="tech-font">{{ $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.traceId')" width="280" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="tech-font mono trace-id">{{ row.traceId }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('audit.timestamp')" width="180">
            <template #default="{ row }">
              <span class="tech-font">{{ fmt(row.createdAt) }}</span>
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
      <el-empty v-else :description="$t('audit.noLogs')" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Document, Refresh, List } from '@element-plus/icons-vue'
import { get } from '@/api/client'
import type { AuditLogResponse, PageDTO } from '@/api/types'

const loading = ref(false)
const logs = ref<AuditLogResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<AuditLogResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const filters = reactive({
  action: '',
  username: '',
  resource: '',
})

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
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.action = ''
  filters.username = ''
  filters.resource = ''
  fetchList(1)
}

function actionTag(action: string): 'danger' | 'warning' | 'success' | 'info' {
  if (action.includes('delete') || action.includes('purge')) return 'danger'
  if (action.includes('create') || action.includes('enable')) return 'success'
  if (action.includes('update') || action.includes('disable') || action.includes('escalate')) return 'warning'
  return 'info'
}

function fmt(iso: string): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-CA', { hour12: false })
}

onMounted(() => fetchList(1))
</script>

<style scoped>
.audit-page { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
}
.header-title { display: flex; align-items: center; gap: 12px; }
.header-title h2 { margin: 0; font-size: 22px; font-weight: 600; color: #2c3e50; letter-spacing: 0.5px; }
.header-icon {
  font-size: 28px; color: #909399; background: rgba(144, 147, 153, 0.1); padding: 8px; border-radius: 8px;
}

.filter-card { border-radius: 12px; border: none; margin-bottom: 24px; }
.table-card { border-radius: 12px; border: none; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; font-size: 16px; color: #303133; }
.card-header > :first-child { display: flex; align-items: center; gap: 8px; }
.card-header .el-icon { color: #909399; font-size: 18px; }
.header-count { font-size: 13px; color: #909399; font-weight: 400; }

.tech-table { border-radius: 8px; overflow: hidden; }
.tech-font { font-weight: 500; color: #606266; font-size: 13px; }
.mono { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; color: #409eff; }
.trace-id { font-size: 12px; color: #909399; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
