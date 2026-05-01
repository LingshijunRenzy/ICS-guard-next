<template>
  <div class="rules-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon"><Setting /></el-icon>
        <h2>{{ $t('rules.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ $t('rules.newRule') }}</el-button>
    </div>

    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item :label="$t('rules.ruleType')">
          <el-select v-model="filters.ruleType" :placeholder="$t('common.dash')" clearable style="width:150px">
            <el-option :label="$t('ruleType.anomaly')" value="anomaly" />
            <el-option :label="$t('ruleType.signature')" value="signature" />
            <el-option :label="$t('ruleType.threshold')" value="threshold" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('rules.enabled')">
          <el-select v-model="filters.enabled" clearable style="width:110px">
            <el-option :label="$t('yes')" :value="true" />
            <el-option :label="$t('no')" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('common.search')">
          <el-input v-model="filters.search" :placeholder="$t('rules.searchName')" clearable style="width:180px" />
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
          <span><el-icon><Setting /></el-icon> {{ $t('rules.ruleList') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="rules.length">
        <el-table :data="rules" stripe size="default" class="tech-table">
          <el-table-column :label="$t('rules.name')" min-width="160">
            <template #default="{ row }">
              <span class="rule-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('rules.type')" width="120">
            <template #default="{ row }">
              <span class="tech-font mono">{{ $t(`ruleType.${row.ruleType}`, row.ruleType) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('rules.target')" width="160">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.target ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('rules.action')" width="110">
            <template #default="{ row }">
              <el-tag :type="actionTag(row.action)" size="small" effect="dark">{{ $t(`ruleAction.${row.action}`, row.action.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('rules.priority')" width="90" align="center" />
          <el-table-column :label="$t('common.status')" width="100">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                @change="(val: boolean) => toggleEnabled(row, val)"
                active-color="#67c23a"
                inactive-color="#f56c6c"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('rules.created')" width="170">
            <template #default="{ row }">
              <span class="tech-font">{{ fmt(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.actions')" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
              <el-popconfirm :title="$t('rules.deleteConfirm')" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" link type="danger">{{ $t('common.delete') }}</el-button>
                </template>
              </el-popconfirm>
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
      <el-empty v-else :description="$t('rules.noRules')" :image-size="100" />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? $t('rules.editRule') : $t('rules.createRule')"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item :label="$t('rules.name')" prop="name">
          <el-input v-model="form.name" :placeholder="$t('rules.name')" />
        </el-form-item>
        <el-form-item :label="$t('rules.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" :placeholder="$t('rules.descriptionPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('rules.ruleType')" prop="ruleType">
          <el-select v-model="form.ruleType" style="width:100%">
            <el-option :label="$t('ruleType.anomaly')" value="anomaly" />
            <el-option :label="$t('ruleType.signature')" value="signature" />
            <el-option :label="$t('ruleType.threshold')" value="threshold" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('rules.target')">
          <el-input v-model="form.target" :placeholder="$t('rules.targetPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('rules.action')" prop="action">
          <el-select v-model="form.action" style="width:100%">
            <el-option :label="$t('ruleAction.block')" value="block" />
            <el-option :label="$t('ruleAction.alert')" value="alert" />
            <el-option :label="$t('ruleAction.log')" value="log" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('rules.priority')">
          <el-input-number v-model="form.priority" :min="0" :max="100" />
        </el-form-item>
        <el-form-item :label="$t('rules.ruleConfig')">
          <el-input v-model="form.ruleConfig" type="textarea" :rows="3" :placeholder="$t('rules.ruleConfigPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Setting, Plus, Refresh } from '@element-plus/icons-vue'
import { get, post, put, del, patch } from '@/api/client'
import type { RuleResponse, RuleRequest, PageDTO } from '@/api/types'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const rules = ref<RuleResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<RuleResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<RuleRequest>({
  name: '',
  description: '',
  ruleType: '',
  target: '',
  action: '',
  priority: 0,
  enabled: true,
  ruleConfig: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: t('rules.nameRequired'), trigger: 'blur' }],
  ruleType: [{ required: true, message: t('rules.ruleTypeRequired'), trigger: 'change' }],
  action: [{ required: true, message: t('rules.actionRequired'), trigger: 'change' }],
}

const filters = reactive({
  ruleType: '',
  search: '',
  enabled: undefined as boolean | undefined,
})

async function fetchList(p: number) {
  loading.value = true
  try {
    const params: Record<string, any> = { page: p - 1, size: pageSize.value }
    if (filters.ruleType) params.ruleType = filters.ruleType
    if (filters.search) params.search = filters.search
    if (filters.enabled !== undefined && filters.enabled !== null) params.enabled = filters.enabled

    const res = await get<PageDTO<RuleResponse>>('/rules', params)
    if (res.code === 200) {
      Object.assign(page, res.data)
      rules.value = res.data.content
      pageNum.value = p
    }
  } finally {
    loading.value = false
  }
}

async function toggleEnabled(row: RuleResponse, val: boolean) {
  try {
    const res = await patch(`/rules/${row.id}/${val ? 'enable' : 'disable'}`)
    if (res.code === 200) row.enabled = val
  } catch { /* handled */ }
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.ruleType = ''
  form.target = ''
  form.action = ''
  form.priority = 0
  form.enabled = true
  form.ruleConfig = ''
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: RuleResponse) {
  editingId.value = row.id
  form.name = row.name
  form.description = row.description ?? ''
  form.ruleType = row.ruleType
  form.target = row.target ?? ''
  form.action = row.action
  form.priority = row.priority
  form.enabled = row.enabled
  form.ruleConfig = row.ruleConfig ?? ''
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await put(`/rules/${editingId.value}`, form)
    } else {
      await post('/rules', form)
    }
    dialogVisible.value = false
    fetchList(pageNum.value)
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await del(`/rules/${id}`)
    fetchList(pageNum.value)
  } catch { /* handled */ }
}

function resetFilters() {
  filters.ruleType = ''
  filters.search = ''
  filters.enabled = undefined
  fetchList(1)
}

function actionTag(a: string): 'danger' | 'warning' | 'info' {
  if (a === 'block') return 'danger'
  if (a === 'alert') return 'warning'
  return 'info'
}

function fmt(iso: string): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-CA', { hour12: false })
}

onMounted(() => fetchList(1))
</script>

<style scoped>
.rules-page { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
}
.header-title { display: flex; align-items: center; gap: 12px; }
.header-title h2 { margin: 0; font-size: 22px; font-weight: 600; color: #2c3e50; letter-spacing: 0.5px; }
.header-icon {
  font-size: 28px; color: #409eff; background: rgba(64, 158, 255, 0.1); padding: 8px; border-radius: 8px;
}

.filter-card { border-radius: 12px; border: none; margin-bottom: 24px; }
.table-card { border-radius: 12px; border: none; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; font-size: 16px; color: #303133; }
.card-header > :first-child { display: flex; align-items: center; gap: 8px; }
.card-header .el-icon { color: #409eff; font-size: 18px; }
.header-count { font-size: 13px; color: #909399; font-weight: 400; }

.tech-table { border-radius: 8px; overflow: hidden; }
.tech-font { font-weight: 500; color: #606266; font-size: 13px; }
.mono { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; color: #409eff; }
.rule-name { font-weight: 600; color: #303133; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
