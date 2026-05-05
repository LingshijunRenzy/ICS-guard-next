<template>
  <div class="rules-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-primary"><SettingIcon size="22px" /></span>
        <h2>{{ $t('rules.title') }}</h2>
      </div>
      <t-button v-if="auth.hasPermission('rules:manage')" theme="primary" @click="openCreate">
        <template #icon><AddIcon /></template>
        {{ $t('rules.newRule') }}
      </t-button>
    </div>

    <t-card :bordered="false" class="filter-card">
      <t-form :data="filters" layout="inline">
        <t-form-item :label="$t('rules.ruleType')">
          <t-select v-model="filters.ruleType" :placeholder="$t('common.dash')" clearable style="width:150px">
            <t-option :label="$t('ruleType.anomaly')" value="anomaly" />
            <t-option :label="$t('ruleType.signature')" value="signature" />
            <t-option :label="$t('ruleType.threshold')" value="threshold" />
          </t-select>
        </t-form-item>
        <t-form-item :label="$t('rules.enabled')">
          <t-select v-model="filters.enabled" clearable style="width:110px">
            <t-option :label="$t('yes')" :value="true" />
            <t-option :label="$t('no')" :value="false" />
          </t-select>
        </t-form-item>
        <t-form-item :label="$t('common.search')">
          <t-input v-model="filters.search" :placeholder="$t('rules.searchName')" clearable style="width:180px" />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" @click="fetchList(1)">{{ $t('common.search') }}</t-button>
          <t-button variant="outline" @click="resetFilters">{{ $t('common.reset') }}</t-button>
        </t-form-item>
      </t-form>
    </t-card>

    <SkeletonTable v-if="loading && !rules.length" :rows="5" :cols="6" />
    <t-card v-else :bordered="false" class="table-card">
      <template #header>
        <div class="card-header">
          <span><SettingIcon size="18px" class="card-header-icon-primary" /> {{ $t('rules.ruleList') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="rules.length">
        <t-table :loading="loading" :data="rules" :columns="ruleColumns" stripe size="medium" class="tech-table" row-key="id">
          <template #name="{ row }"><span class="rule-name">{{ row.name }}</span></template>
          <template #ruleType="{ row }"><span class="tech-font mono">{{ $t(`ruleType.${row.ruleType}`, row.ruleType) }}</span></template>
          <template #target="{ row }"><span class="tech-font mono">{{ row.target ?? $t('common.dash') }}</span></template>
          <template #action="{ row }">
            <t-tag :theme="actionTag(row.action)" size="small" variant="dark">{{ $t(`ruleAction.${row.action}`, row.action.toUpperCase()) }}</t-tag>
          </template>
          <template #enabled="{ row }">
            <t-switch v-if="auth.hasPermission('rules:manage')" :value="row.enabled" @change="(val: boolean) => toggleEnabled(row, val)" />
            <t-tag v-else :theme="row.enabled ? 'success' : 'default'" variant="light" size="small">{{ row.enabled ? $t('common.enabled') : $t('common.disabled') }}</t-tag>
          </template>
          <template #createdAt="{ row }"><span class="tech-font">{{ formatDateTime(row.createdAt) }}</span></template>
          <template #actions="{ row }">
            <t-button v-if="auth.hasPermission('rules:manage')" variant="text" theme="primary" size="small" @click="openEdit(row)">{{ $t('common.edit') }}</t-button>
            <t-popconfirm v-if="auth.hasPermission('rules:manage')" :content="$t('rules.deleteConfirm')" @confirm="handleDelete(row.id)">
              <t-button variant="text" theme="danger" size="small">{{ $t('common.delete') }}</t-button>
            </t-popconfirm>
          </template>
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
      <t-empty v-else :description="$t('rules.noRules')" />
    </t-card>

    <t-dialog
      v-model:visible="dialogVisible"
      :header="editingId ? $t('rules.editRule') : $t('rules.createRule')"
      width="600px"
      destroy-on-close
    >
      <t-form ref="formRef" :data="form" :rules="formRules" label-width="110px">
        <t-form-item :label="$t('rules.name')" name="name">
          <t-input v-model="form.name" :placeholder="$t('rules.name')" />
        </t-form-item>
        <t-form-item :label="$t('rules.description')">
          <t-textarea v-model="form.description" :rows="2" :placeholder="$t('rules.descriptionPlaceholder')" />
        </t-form-item>
        <t-form-item :label="$t('rules.ruleType')" name="ruleType">
          <t-select v-model="form.ruleType" style="width:100%">
            <t-option :label="$t('ruleType.anomaly')" value="anomaly" />
            <t-option :label="$t('ruleType.signature')" value="signature" />
            <t-option :label="$t('ruleType.threshold')" value="threshold" />
          </t-select>
        </t-form-item>
        <t-form-item :label="$t('rules.target')">
          <t-input v-model="form.target" :placeholder="$t('rules.targetPlaceholder')" />
        </t-form-item>
        <t-form-item :label="$t('rules.action')" name="action">
          <t-select v-model="form.action" style="width:100%">
            <t-option :label="$t('ruleAction.block')" value="block" />
            <t-option :label="$t('ruleAction.alert')" value="alert" />
            <t-option :label="$t('ruleAction.log')" value="log" />
          </t-select>
        </t-form-item>
        <t-form-item :label="$t('rules.priority')">
          <t-input-number v-model="form.priority" :min="0" :max="100" />
        </t-form-item>
        <t-form-item :label="$t('rules.ruleConfig')">
          <t-textarea v-model="form.ruleConfig" :rows="3" :placeholder="$t('rules.ruleConfigPlaceholder')" />
        </t-form-item>
      </t-form>
      <template #footer>
        <t-button @click="dialogVisible = false">{{ $t('common.cancel') }}</t-button>
        <t-button theme="primary" :loading="saving" @click="handleSave">{{ $t('common.save') }}</t-button>
      </template>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { SettingIcon, AddIcon } from 'tdesign-icons-vue-next'
import { get, post, put, del, patch } from '@/api/client'
import type { RuleResponse, RuleRequest, PageDTO } from '@/api/types'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import type { FormInstanceFunctions, FormRules } from 'tdesign-vue-next'

const auth = useAuthStore()
import { formatDateTime } from '@/composables/useFormat'
import { actionTag } from '@/composables/useSeverity'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const rules = ref<RuleResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<RuleResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstanceFunctions>()
const form = reactive<RuleRequest>({
  name: '', description: '', ruleType: '', target: '', action: '', priority: 0, enabled: true, ruleConfig: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: t('rules.nameRequired'), trigger: 'blur' }],
  ruleType: [{ required: true, message: t('rules.ruleTypeRequired'), trigger: 'change' }],
  action: [{ required: true, message: t('rules.actionRequired'), trigger: 'change' }],
}

const filters = reactive({ ruleType: '', search: '', enabled: undefined as boolean | undefined })

const ruleColumns = computed(() => [
  { colKey: 'name', title: t('rules.name'), minWidth: 160 },
  { colKey: 'ruleType', title: t('rules.type'), width: 120 },
  { colKey: 'target', title: t('rules.target'), width: 160 },
  { colKey: 'action', title: t('rules.action'), width: 110 },
  { colKey: 'priority', title: t('rules.priority'), width: 90, align: 'center' as const },
  { colKey: 'enabled', title: t('common.status'), width: 100 },
  { colKey: 'createdAt', title: t('rules.created'), width: 170 },
  { colKey: 'actions', title: t('common.actions'), width: 180, fixed: 'right' as const },
])

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
  } finally { loading.value = false }
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

function openCreate() { resetForm(); dialogVisible.value = true }
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
  const result = await formRef.value?.validate()
  if (result !== true) return
  saving.value = true
  try {
    if (editingId.value) {
      await put(`/rules/${editingId.value}`, form)
    } else {
      await post('/rules', form)
    }
    dialogVisible.value = false
    fetchList(pageNum.value)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try { await del(`/rules/${id}`); fetchList(pageNum.value) } catch { /* handled */ }
}

function resetFilters() {
  filters.ruleType = ''
  filters.search = ''
  filters.enabled = undefined
  fetchList(1)
}

onMounted(() => fetchList(1))
</script>

<style scoped>
.rules-page { padding: 0; }

.header-icon-primary { color: var(--color-primary); background: var(--color-primary-light); }

.card-header-icon-primary { color: var(--color-primary); }
</style>
