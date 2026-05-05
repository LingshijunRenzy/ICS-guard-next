<template>
  <div class="users-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-success"><UserIcon size="22px" /></span>
        <h2>{{ $t('users.title') }}</h2>
      </div>
      <div class="header-actions">
        <t-button variant="outline" @click="openRoleDialog">{{ $t('users.manageRoles') }}</t-button>
        <t-button theme="primary" @click="openCreate">
          <template #icon><AddIcon /></template>
          {{ $t('users.newUser') }}
        </t-button>
      </div>
    </div>

    <SkeletonTable v-if="loading && !users.length" :rows="5" :cols="5" />
    <t-card v-else :bordered="false" class="table-card">
      <template #header>
        <div class="card-header">
          <span><ViewListIcon size="18px" class="card-header-icon-success" /> {{ $t('users.users') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ users.length }}</span>
        </div>
      </template>
      <template v-if="users.length">
        <t-table :loading="loading" :data="users" :columns="userColumns" stripe size="medium" class="tech-table" row-key="id">
          <template #username="{ row }"><span class="user-name">{{ row.username }}</span></template>
          <template #email="{ row }"><span class="tech-font">{{ row.email ?? $t('common.dash') }}</span></template>
          <template #displayName="{ row }"><span class="tech-font">{{ row.displayName ?? $t('common.dash') }}</span></template>
          <template #roles="{ row }">
            <t-tag v-for="r in row.roles" :key="r" size="small" variant="light" style="margin-right:4px">{{ r }}</t-tag>
            <span v-if="!row.roles?.length" class="tech-font">{{ $t('common.dash') }}</span>
          </template>
          <template #enabled="{ row }">
            <t-switch :value="row.enabled" @change="(val: boolean) => toggleEnabled(row, val)" />
          </template>
          <template #createdAt="{ row }"><span class="tech-font">{{ formatDateTime(row.createdAt) }}</span></template>
          <template #actions="{ row }">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">{{ $t('common.edit') }}</t-button>
            <t-popconfirm :content="$t('users.deleteConfirm')" @confirm="handleDelete(row.id)">
              <t-button variant="text" theme="danger" size="small">{{ $t('common.delete') }}</t-button>
            </t-popconfirm>
          </template>
        </t-table>
      </template>
      <t-empty v-else :description="$t('users.noUsers')" />
    </t-card>

    <t-dialog v-model:visible="userDialogVisible" :header="editingId ? $t('users.editUser') : $t('users.createUser')" width="500px" destroy-on-close>
      <t-form ref="userFormRef" :data="userForm" :rules="userFormRules" label-width="110px">
        <t-form-item :label="$t('users.username')" name="username">
          <t-input v-model="userForm.username" :disabled="!!editingId" :placeholder="$t('users.username')" />
        </t-form-item>
        <t-form-item :label="$t('users.password')" :name="editingId ? undefined : 'password'">
          <t-input v-model="userForm.password" type="password" :placeholder="$t('users.passwordHint')" />
        </t-form-item>
        <t-form-item :label="$t('users.displayName')">
          <t-input v-model="userForm.displayName" :placeholder="$t('users.displayName')" />
        </t-form-item>
        <t-form-item :label="$t('users.email')">
          <t-input v-model="userForm.email" :placeholder="$t('users.email')" />
        </t-form-item>
        <t-form-item :label="$t('users.roles')">
          <t-select v-model="userForm.roleIds" multiple :placeholder="$t('users.roles')" style="width:100%">
            <t-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </t-select>
        </t-form-item>
      </t-form>
      <template #footer>
        <t-button @click="userDialogVisible = false">{{ $t('common.cancel') }}</t-button>
        <t-button theme="primary" :loading="saving" @click="handleSaveUser">{{ $t('common.save') }}</t-button>
      </template>
    </t-dialog>

    <t-dialog v-model:visible="roleDialogVisible" :header="$t('users.rolesTitle')" width="600px" destroy-on-close :footer="false">
      <template v-if="roles.length">
        <div v-for="r in roles" :key="r.id" class="role-item">
          <div class="role-head">
            <span class="role-name">{{ r.name }}</span>
            <span class="role-desc">{{ r.description ?? '' }}</span>
          </div>
          <div class="role-perms">
            <t-tag v-for="p in r.permissions" :key="p" size="small" class="perm-tag">{{ p }}</t-tag>
          </div>
        </div>
      </template>
      <t-empty v-else :description="$t('users.noRoles')" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { UserIcon, AddIcon, ViewListIcon } from 'tdesign-icons-vue-next'
import { get, post, put, del, patch } from '@/api/client'
import { useI18n } from 'vue-i18n'
import type { UserResponse, RoleResponse, CreateUserRequest, PageDTO } from '@/api/types'
import type { FormInstanceFunctions, FormRules } from 'tdesign-vue-next'
import { formatDateTime } from '@/composables/useFormat'
import SkeletonTable from '@/components/skeleton/SkeletonTable.vue'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const users = ref<UserResponse[]>([])
const roles = ref<RoleResponse[]>([])

const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const editingId = ref<number | null>(null)
const userFormRef = ref<FormInstanceFunctions>()

const userForm = reactive<CreateUserRequest & { displayName?: string }>({
  username: '', password: '', email: '', displayName: '', roleIds: [],
})

const userFormRules: FormRules = {
  username: [{ required: true, message: t('users.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('users.passwordRequired'), trigger: 'blur' }, { min: 6, message: t('users.passwordMinLen'), trigger: 'blur' }],
}

const userColumns = computed(() => [
  { colKey: 'username', title: t('users.username'), width: 140 },
  { colKey: 'email', title: t('users.email'), minWidth: 180 },
  { colKey: 'displayName', title: t('users.displayName'), width: 160 },
  { colKey: 'roles', title: t('users.roles'), width: 200 },
  { colKey: 'enabled', title: t('common.status'), width: 100 },
  { colKey: 'createdAt', title: t('users.created'), width: 170 },
  { colKey: 'actions', title: t('common.actions'), width: 140, fixed: 'right' as const },
])

async function fetchUsers() {
  loading.value = true
  try {
    const res = await get<PageDTO<UserResponse>>('/users')
    if (res.code === 200) users.value = res.data.content
  } finally { loading.value = false }
}

async function fetchRoles() {
  try {
    const res = await get<RoleResponse[]>('/roles')
    if (res.code === 200) roles.value = res.data
  } catch { /* auxiliary */ }
}

async function toggleEnabled(row: UserResponse, val: boolean) {
  try {
    const res = await patch(`/users/${row.id}/${val ? 'enable' : 'disable'}`)
    if (res.code === 200) row.enabled = val
  } catch { /* handled */ }
}

function resetUserForm() {
  userForm.username = ''
  userForm.password = ''
  userForm.email = ''
  userForm.displayName = ''
  userForm.roleIds = []
  editingId.value = null
}

function openCreate() { resetUserForm(); userDialogVisible.value = true }
function openEdit(row: UserResponse) {
  editingId.value = row.id
  userForm.username = row.username
  userForm.password = ''
  userForm.email = row.email ?? ''
  userForm.displayName = row.displayName ?? ''
  userForm.roleIds = row.roles.map(rr => roles.value.find(r => r.name === rr)?.id).filter(Boolean) as number[]
  userDialogVisible.value = true
}

async function handleSaveUser() {
  const result = await userFormRef.value?.validate()
  if (result !== true) return
  saving.value = true
  try {
    const payload: Record<string, any> = { username: userForm.username }
    if (userForm.password) payload.password = userForm.password
    if (userForm.email) payload.email = userForm.email
    if (userForm.displayName) payload.displayName = userForm.displayName
    if (userForm.roleIds?.length) payload.roleIds = userForm.roleIds

    if (editingId.value) {
      await put(`/users/${editingId.value}`, payload)
    } else {
      await post('/users', payload)
    }
    userDialogVisible.value = false
    fetchUsers()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try { await del(`/users/${id}`); fetchUsers() } catch { /* handled */ }
}

function openRoleDialog() { fetchRoles(); roleDialogVisible.value = true }

onMounted(() => { fetchUsers(); fetchRoles() })
</script>

<style scoped>
.users-page { padding: 0; }

.header-icon-success { color: var(--color-success); background: var(--color-success-light); }

.card-header-icon-success { color: var(--color-success); }
</style>
