<template>
  <div class="permissions-container">
    <t-card :bordered="false" class="permissions-card">
      <template #header>
        <div class="header-title">
          <h2>权限管理</h2>
          <p class="subtitle">管理系统中的权限点及其分类。您可以增删改权限信息，以便进行更细粒度的访问控制。</p>
        </div>
      </template>

      <t-layout class="permissions-layout">
        <t-aside class="permissions-aside">
          <t-menu :value="activeTypeId" @change="handleTypeChange" theme="light" class="permissions-menu">
            <t-menu-item value="all">
              <template #icon><app-icon /></template>
              全部权限
            </t-menu-item>
            <t-menu-item v-for="type in permissionTypes" :key="type.id" :value="String(type.id)">
              <template #icon><view-module-icon /></template>
              {{ type.name }}
            </t-menu-item>
          </t-menu>
        </t-aside>

        <t-content class="permissions-content">
          <div class="content-header">
            <div class="header-actions">
              <t-button theme="primary" @click="handleAddPermission">
                <template #icon><add-icon /></template>
                添加权限
              </t-button>
              <t-button variant="outline" @click="fetchData">
                <template #icon><refresh-icon /></template>
                刷新
              </t-button>
            </div>
            <div class="search-bar">
              <t-input v-model="searchQuery" shadow placeholder="搜索权限名称或描述..." cleanable>
                <template #prefix-icon><search-icon /></template>
              </t-input>
            </div>
          </div>

          <div class="table-container">
            <t-table
              row-key="id"
              :data="filteredData"
              :columns="columns"
              :loading="loading"
              hover
              stripe
              vertical-align="middle"
            >
              <template #name="{ row }">
                <div class="perm-title">{{ row.name }}</div>
              </template>
              <template #typeName="{ row }">
                <t-tag v-if="row.typeName" theme="primary" variant="light-outline">{{ row.typeName }}</t-tag>
                <span v-else class="text-muted">未分类</span>
              </template>
              <template #operation="{ row }">
                <t-space>
                  <t-link theme="primary" @click="handleEditPermission(row)">编辑</t-link>
                  <t-popconfirm content="确认删除该权限吗？" @confirm="handleDeletePermission(row)">
                    <t-link theme="danger">删除</t-link>
                  </t-popconfirm>
                </t-space>
              </template>
            </t-table>
          </div>
        </t-content>
      </t-layout>
    </t-card>

    <!-- 这里可以后续添加添加/编辑的 Dialog -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { 
  SearchIcon, 
  AddIcon, 
  RefreshIcon, 
  AppIcon, 
  ViewModuleIcon 
} from 'tdesign-icons-vue-next'
import { permissionApi } from '@/api/permissions'
import type { PermissionResponse, PermissionTypeResponse } from '@/api/types'

const activeTypeId = ref('all')
const searchQuery = ref('')
const loading = ref(false)
const permissions = ref<PermissionResponse[]>([])
const permissionTypes = ref<PermissionTypeResponse[]>([])

const columns = [
  { colKey: 'name', title: '权限标识', width: 250 },
  { colKey: 'description', title: '描述', width: 350 },
  { colKey: 'typeName', title: '所属类型', width: 150 },
  { colKey: 'operation', title: '操作', width: 150, fixed: 'right' },
]

const handleTypeChange = (value: any) => {
  activeTypeId.value = value as string
}

const fetchData = async () => {
  loading.value = true
  try {
    const [permRes, typeRes] = await Promise.all([
      permissionApi.list(),
      permissionApi.listTypes()
    ])
    
    if (permRes.code === 200) {
      permissions.value = permRes.data
    }
    if (typeRes.code === 200) {
      permissionTypes.value = typeRes.data
    }
  } catch (error) {
    MessagePlugin.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

const filteredData = computed(() => {
  let data = permissions.value

  if (activeTypeId.value !== 'all') {
    data = data.filter((item) => String(item.typeId) === activeTypeId.value)
  }

  if (searchQuery.value) {
    const lowerQuery = searchQuery.value.toLowerCase()
    data = data.filter(
      (item) =>
        item.name.toLowerCase().includes(lowerQuery) ||
        (item.description && item.description.toLowerCase().includes(lowerQuery)),
    )
  }

  return data
})

const handleAddPermission = () => {
  MessagePlugin.info('添加权限功能待实现')
}

const handleEditPermission = (row: PermissionResponse) => {
  MessagePlugin.info(`编辑权限: ${row.name}`)
}

const handleDeletePermission = async (row: PermissionResponse) => {
  try {
    const res = await permissionApi.delete(row.id)
    if (res.code === 200) {
      MessagePlugin.success('删除成功')
      fetchData()
    }
  } catch (error) {
    MessagePlugin.error('删除失败')
  }
}
</script>

<style scoped>
.permissions-container {
  padding: 16px;
  height: calc(100vh - 120px);
}

.permissions-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header-title h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

.header-title .subtitle {
  margin: 0;
  color: var(--td-text-color-secondary);
  font-size: 14px;
}

.permissions-layout {
  border: 1px solid var(--td-component-border);
  border-radius: var(--td-radius-default);
  margin-top: 16px;
  background-color: var(--td-bg-color-container);
  flex: 1;
}

.permissions-aside {
  width: 220px;
  border-right: 1px solid var(--td-component-border);
  background-color: var(--td-bg-color-container);
}

.permissions-menu {
  height: 100%;
}

.permissions-content {
  display: flex;
  flex-direction: column;
  padding: 24px;
  background-color: var(--td-bg-color-container);
  overflow: hidden;
}

.content-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-bar {
  width: 300px;
}

.table-container {
  flex: 1;
  overflow: auto;
}

.perm-title {
  font-weight: 500;
  font-family: monospace;
  color: var(--td-text-color-primary);
}

.text-muted {
  color: var(--td-text-color-placeholder);
}

:deep(.t-layout__sider) {
  background-color: transparent;
}
</style>
