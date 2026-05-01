<template>
  <div class="topology-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon"><Share /></el-icon>
        <h2>{{ $t('topology.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchAll">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Cpu /></el-icon> {{ $t('topology.connectedDevices') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ devices.length }}</span>
        </div>
      </template>
      <template v-if="devices.length">
        <el-row :gutter="16">
          <el-col v-for="d in devices" :key="d.deviceId" :span="6">
            <el-card shadow="hover" class="device-card" @click="selectDevice(d.deviceId)">
              <div class="device-head">
                <div class="device-type-tag" :class="deviceStatusClass(d.status)">{{ $t(`deviceStatus.${d.status}`, d.status.toUpperCase()) }}</div>
              </div>
              <div class="device-name">{{ d.deviceName }}</div>
              <div class="device-info">
                <div class="info-row"><span class="lbl">{{ $t('topology.deviceType') }}</span><span class="tech-font mono">{{ d.deviceType }}</span></div>
                <div class="info-row" v-if="d.ipAddress"><span class="lbl">{{ $t('topology.ipAddress') }}</span><span class="tech-font mono">{{ d.ipAddress }}</span></div>
                <div class="info-row" v-if="d.macAddress"><span class="lbl">{{ $t('topology.macAddress') }}</span><span class="tech-font mono">{{ d.macAddress }}</span></div>
                <div class="info-row" v-if="d.port"><span class="lbl">{{ $t('topology.port') }}</span><span class="tech-font mono">{{ d.port }}</span></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
      <el-empty v-else :description="$t('topology.noDevices')" :image-size="80" />
    </el-card>

    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> {{ $t('topology.topologyEvents') }}</span>
          <span class="header-count">{{ $t('common.total') }}: {{ page.totalElements }}</span>
        </div>
      </template>
      <template v-if="events.length">
        <el-table :data="events" stripe size="default" class="tech-table">
          <el-table-column :label="$t('topology.eventType')" width="140">
            <template #default="{ row }">
              <el-tag :type="eventTag(row.eventType)" size="small" effect="dark">{{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.deviceName')" min-width="140">
            <template #default="{ row }">
              <span class="tech-font">{{ row.deviceName }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.deviceType')" width="100">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.deviceType }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.ipAddress')" width="160">
            <template #default="{ row }">
              <span class="tech-font mono" v-if="row.ipAddress">{{ row.ipAddress }}</span>
              <span v-else class="tech-font">{{ $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.macAddress')" width="160">
            <template #default="{ row }">
              <span class="tech-font mono" v-if="row.macAddress">{{ row.macAddress }}</span>
              <span v-else class="tech-font">{{ $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.port')" width="80">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.port ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="plain">{{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.occurredAt')" width="180">
            <template #default="{ row }">
              <span class="tech-font">{{ fmt(row.occurredAt) }}</span>
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
            @size-change="fetchEvents(1)"
            @current-change="fetchEvents"
          />
        </div>
      </template>
      <el-empty v-else :description="$t('topology.noEvents')" :image-size="100" />
    </el-card>

    <el-dialog v-model="historyVisible" :title="$t('topology.deviceHistory', { id: historyId })" width="800px" destroy-on-close>
      <template v-if="deviceHistory.length">
        <el-table :data="deviceHistory" stripe size="small" max-height="400">
          <el-table-column :label="$t('topology.eventType')" width="120">
            <template #default="{ row }">
              <el-tag :type="eventTag(row.eventType)" size="small" effect="dark">{{ $t(`topoEvent.${row.eventType}`, row.eventType.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.status')" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="plain">{{ $t(`deviceStatus.${row.status}`, row.status.toUpperCase()) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.ipAddress')" width="150">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.ipAddress ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.macAddress')" width="160">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.macAddress ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.port')" width="80">
            <template #default="{ row }">
              <span class="tech-font mono">{{ row.port ?? $t('common.dash') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('topology.occurredAt')" width="170">
            <template #default="{ row }">
              <span class="tech-font">{{ fmt(row.occurredAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else :description="$t('topology.noHistory')" :image-size="80" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Share, Refresh, Cpu, List } from '@element-plus/icons-vue'
import { get } from '@/api/client'
import type { TopologyEventResponse, PageDTO } from '@/api/types'

const loading = ref(false)
const devices = ref<TopologyEventResponse[]>([])
const events = ref<TopologyEventResponse[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const page = reactive<PageDTO<TopologyEventResponse>>({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 })

const historyVisible = ref(false)
const historyId = ref('')
const deviceHistory = ref<TopologyEventResponse[]>([])

async function fetchDevices() {
  try {
    const res = await get<TopologyEventResponse[]>('/topology/devices')
    if (res.code === 200) devices.value = res.data
  } catch { /* auxiliary */ }
}

async function fetchEvents(p: number) {
  loading.value = true
  try {
    const res = await get<PageDTO<TopologyEventResponse>>('/topology/events', { page: p - 1, size: pageSize.value })
    if (res.code === 200) {
      Object.assign(page, res.data)
      events.value = res.data.content
      pageNum.value = p
    }
  } finally {
    loading.value = false
  }
}

async function selectDevice(deviceId: string) {
  historyId.value = deviceId
  historyVisible.value = true
  try {
    const res = await get<TopologyEventResponse[]>(`/topology/devices/${deviceId}/history`)
    if (res.code === 200) deviceHistory.value = res.data
  } catch { deviceHistory.value = [] }
}

function fetchAll() {
  fetchDevices()
  fetchEvents(pageNum.value)
}

function eventTag(et: string): 'success' | 'danger' | 'warning' | 'info' {
  if (et === 'device_discovered' || et === 'device_online') return 'success'
  if (et === 'device_offline') return 'danger'
  return 'info'
}

function statusTag(st: string): 'success' | 'warning' | 'danger' | 'info' | '' {
  if (st === 'online') return 'success'
  if (st === 'offline') return 'danger'
  return 'info'
}

function deviceStatusClass(st: string): string {
  if (st === 'online') return 'status-online'
  if (st === 'offline') return 'status-offline'
  return 'status-unknown'
}

function fmt(iso: string): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-CA', { hour12: false })
}

onMounted(fetchAll)
</script>

<style scoped>
.topology-page { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
}
.header-title { display: flex; align-items: center; gap: 12px; }
.header-title h2 { margin: 0; font-size: 22px; font-weight: 600; color: #2c3e50; letter-spacing: 0.5px; }
.header-icon {
  font-size: 28px; color: #67c23a; background: rgba(103, 194, 58, 0.1); padding: 8px; border-radius: 8px;
}

.section-card { border-radius: 12px; border: none; margin-bottom: 24px; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; font-size: 16px; color: #303133; }
.card-header > :first-child { display: flex; align-items: center; gap: 8px; }
.card-header .el-icon { color: #67c23a; font-size: 18px; }
.header-count { font-size: 13px; color: #909399; font-weight: 400; }

.device-card {
  border-radius: 10px; border: none; cursor: pointer; margin-bottom: 16px; transition: all 0.3s;
}
.device-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.device-head { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.device-type-tag {
  font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px;
}
.status-online { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.status-offline { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.status-unknown { background: rgba(144, 147, 153, 0.1); color: #909399; }

.device-name { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.device-info { display: flex; flex-direction: column; gap: 6px; }
.info-row { display: flex; justify-content: space-between; align-items: center; }
.info-row .lbl { font-size: 12px; color: #c0c4cc; text-transform: uppercase; }

.tech-table { border-radius: 8px; overflow: hidden; }
.tech-font { font-weight: 500; color: #606266; font-size: 13px; }
.mono { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; color: #409eff; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
