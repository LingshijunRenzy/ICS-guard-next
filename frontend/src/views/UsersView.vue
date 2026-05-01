<template>
  <div class="users-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon header-icon-success"><User /></el-icon>
        <h2>{{ $t('users.title') }}</h2>
      </div>
      <div class="header-actions">
        <el-button @click="openRoleDialog">{{ $t('users.manageRoles') }}</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">{{ $t('users.newUser') }}</el-button>
      </div>
    </div>

    <SkeletonTable v-if="loading && !users.length" :rows="5" :cols="5" />
    <el-card v-else shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon-success"><List /></el-icon> {{ $t('users.users') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ users.length }}</span>
        </div>
      </template>
      <template v-if="users.length">
        <el-table v-loading="loading" :data="users" stripe size="default" class="tech-table">
          <el-table-column :label="$t('users.username')" width="140">
            <template #default="{ row }"><span class="user-name">{{ row.username }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('users.email')" min-width="180">
            <template #default="{ row }"><span class="tech-font">{{ row.email ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('users.displayName')" width="160">
            <template #default="{ row }"><span class="tech-font">{{ row.displayName ?? $t('common.dash') }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('users.roles')" width="200">
            <template #default="{ row }">
              <el-tag v-for="r in row.roles" :key="r" size="small" effect="plain" style="margin-right:4px">{{ r }}</el-tag>
              <span v-if="!row.roles?.length" class="tech-font">{{ $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.status')" width="100">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                active-color="var(--color-success)"
                inactive-color="var(--color-danger)"
                @change="(val: boolean) => toggleEnabled(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('users.created')" width="170">
            <template #default="{ row }"><span class="tech-font">{{ formatDateTime(row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('common.actions')" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
              <el-popconfirm :title="$t('users.deleteConfirm')" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" link type="danger">{{ $t('common.delete') }}</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else :description="$t('users.noUsers')" :image-size="100" />
    </el-card>

    <el-dialog v-model="userDialogVisible" :title="editingId ? $t('users.editUser') : $t('users.createUser')" width="500px" destroy-on-close>
      <el-form ref="userFormRef" :model="userForm" :rules="userFormRules" label-width="110px">
        <el-form-item :label="$t('users.username')" prop="username">
          <el-input v-model="userForm.username" :disabled="!!editingId" :placeholder="$t('users.username')" />
        </el-form-item>
        <el-form-item :label="$t('users.password')" :prop="editingId ? undefined : 'password'">
          <el-input v-model="userForm.password" type="password" show-password :placeholder="$t('users.passwordHint')" />
        </el-form-item>
        <el-form-item :label="$t('users.displayName')">
          <el-input v-model="userForm.displayName" :placeholder="$t('users.displayName')" />
        </el-form-item>
        <el-form-item :label="$t('users.email')">
          <el-input v-model="userForm.email" :placeholder="$t('users.email')" />
        </el-form-item>
        <el-form-item :label="$t('users.roles')">
          <el-select v-model="userForm.roleIds" multiple :placeholder="$t('users.roles')" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveUser">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="$t('users.rolesTitle')" width="600px" destroy-on-close>
      <template v-if="roles.length">
        <div v-for="r in roles" :key="r.id" class="role-item">
          <div class="role-head">
            <span class="role-name">{{ r.name }}</span>
            <span class="role-desc">{{ r.description ?? '' }}</span>
          </div>
          <div class="role-perms">
            <el-tag v-for="p in r.permissions" :key="p" size="small" class="perm-tag">{{ p }}</el-tag>
          </div>
        </div>
      </template>
      <el-empty v-else :description="$t('users.noRoles')" :image-size="80" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { User, Plus, List } from '@element-plus/icons-vue'
import { get, post, put, del, patch } from '@/api/client'
import { useI18n } from 'vue-i18n'
import type { UserResponse, RoleResponse, CreateUserRequest } from '@/api/types'
import type { FormInstance, FormRules } from 'element-plus'
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
const userFormRef = ref<FormInstance>()

const userForm = reactive<CreateUserRequest & { displayName?: string }>({
  username: '', password: '', email: '', displayName: '', roleIds: [],
})

const userFormRules: FormRules = {
  username: [{ required: true, message: t('users.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('users.passwordRequired'), trigger: 'blur' }, { min: 6, message: t('users.passwordMinLen'), trigger: 'blur' }],
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await get<UserResponse[]>('/users')
    if (res.code === 200) users.value = res.data
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
  userForm.roleIds = row.roles.map(r => roles.value.find(rr => rr.name === r)?.id).filter(Boolean) as number[]
  userDialogVisible.value = true
}

async function handleSaveUser() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return
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

.card-header-icon-success { color: var(--color-success); font-size: 18px; }
</style>
