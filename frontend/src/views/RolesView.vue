<template>
  <div class="roles-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-primary"><UserIcon size="22px" /></span>
        <h2>{{ $t('roles.title') }}</h2>
      </div>
      <div class="header-actions">
        <t-button v-if="auth.hasPermission('roles:manage')" theme="primary" @click="openCreate">
          <template #icon><AddIcon /></template>
          {{ $t('roles.newRole') }}
        </t-button>
      </div>
    </div>

    <SkeletonTable v-if="loading && !roles.length" :rows="5" :cols="4" />
    <t-card v-else :bordered="false" class="table-card">
      <template #header>
        <div class="card-header">
          <span><ViewListIcon size="18px" class="card-header-icon-primary" /> {{ $t('roles.roleList') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ roles.length }}</span>
        </div>
      </template>
      <template v-if="roles.length">
        <t-table :loading="loading" :data="roles" :columns="roleColumns" stripe size="medium" class="tech-table" row-key="id">
          <template #name="{ row }"><span class="role-name">{{ row.name }}</span></template>
          <template #description="{ row }"><span class="tech-font">{{ row.description ?? $t('common.dash') }}</span></template>
          <template #permissions="{ row }">
            <t-tag v-for="p in row.permissions" :key="p" size="small" variant="light" style="margin-right:4px;margin-bottom:2px">{{ p }}</t-tag>
            <span v-if="!row.permissions?.length" class="tech-font">{{ $t('common.dash') }}</span>
          </template>
          <template #actions="{ row }">
            <t-button v-if="auth.hasPermission('roles:manage')" variant="text" theme="primary" size="small" @click="openEdit(row)">{{ $t('common.edit') }}</t-button>
            <t-popconfirm v-if="auth.hasPermission('roles:manage')" :content="$t('roles.deleteConfirm')" @confirm="handleDelete(row.id)">
              <t-button variant="text" theme="danger" size="small">{{ $t('common.delete') }}</t-button>
            </t-popconfirm>
          </template>
        </t-table>
      </template>
      <t-empty v-else :description="$t('roles.noRoles')" />
    </t-card>

    <t-dialog v-model:visible="dialogVisible" :header="editingId ? $t('roles.editRole') : $t('roles.createRole')" width="550px" destroy-on-close>
      <t-form ref="formRef" :data="form" :rules="formRules" label-width="100px">
        <t-form-item :label="$t('roles.name')" name="name">
          <t-input v-model="form.name" :placeholder="$t('roles.name')" />
        </t-form-item>
        <t-form-item :label="$t('roles.description')">
          <t-input v-model="form.description" :placeholder="$t('roles.descriptionHint')" />
        </t-form-item>
        <t-form-item :label="$t('roles.permissions')">
          <t-select v-model="form.permissionIds" multiple :placeholder="$t('roles.selectPerms')" style="width:100%">
            <t-option v-for="p in allPermissions" :key="p.id" :label="p.name" :value="p.id" />
          </t-select>
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
import { UserIcon, AddIcon, ViewListIcon } from 'tdesign-icons-vue-next'
import { get, post, put, del } from '@/api/client'
import { useI18n } from 'vue-i18n'
import type { RoleResponse, PermissionResponse, CreateRoleRequest } from '@/api/types'
import type { FormInstanceFunctions, FormRules } from 'tdesign-vue-next'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const roles = ref<RoleResponse[]>([])
const allPermissions = ref<PermissionResponse[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstanceFunctions>()

const form = reactive<CreateRoleRequest>({
  name: '',
  description: '',
  permissionIds: [],
})

const formRules: FormRules = {
  name: [{ required: true, message: t('roles.nameRequired'), trigger: 'blur' }],
}

const roleColumns = computed(() => [
  { colKey: 'name', title: t('roles.name'), width: 180 },
  { colKey: 'description', title: t('roles.description'), minWidth: 200 },
  { colKey: 'permissions', title: t('roles.permissions'), minWidth: 240 },
  { colKey: 'actions', title: t('common.actions'), width: 140, fixed: 'right' as const },
])

async function fetchRoles() {
  loading.value = true
  try {
    const res = await get<RoleResponse[]>('/roles')
    roles.value = res.data ?? []
  } catch {
    // handled by client
  } finally {
    loading.value = false
  }
}

async function fetchPermissions() {
  try {
    const res = await get<PermissionResponse[]>('/permissions')
    allPermissions.value = res.data ?? []
  } catch {
    // handled by client
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.description = ''
  form.permissionIds = []
  dialogVisible.value = true
}

function openEdit(row: RoleResponse) {
  editingId.value = row.id
  form.name = row.name
  form.description = row.description ?? ''
  form.permissionIds = []
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate?.()
  if (valid !== true) return
  saving.value = true
  try {
    if (editingId.value) {
      await put(`/roles/${editingId.value}`, form)
    } else {
      await post('/roles', form)
    }
    dialogVisible.value = false
    await fetchRoles()
  } catch {
    // handled by client
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await del(`/roles/${id}`)
    await fetchRoles()
  } catch {
    // handled by client
  }
}

onMounted(() => {
  fetchRoles()
  fetchPermissions()
})
</script>

<style scoped>
.role-name {
  font-weight: 600;
  color: var(--color-primary);
}
</style>