<template>
  <div class="sdn-page">
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon header-icon-primary"><Monitor /></el-icon>
        <h2>{{ $t('sdn.title') }}</h2>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchHealth">
        {{ $t('common.refreshStatus') }}
      </el-button>
    </div>

    <el-row :gutter="24" class="stat-row">
      <template v-if="loading">
        <el-col v-for="n in 3" :key="'sk' + n" :span="8"><SkeletonCard /></el-col>
      </template>
      <template v-else>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper" :class="health.status === 'healthy' ? 'success-bg' : 'danger-bg'">
                <el-icon><CircleCheckFilled v-if="health.status === 'healthy'" /><CircleCloseFilled v-else /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('sdn.controllerStatus') }}</div>
                <div class="stat-value" :class="health.status === 'healthy' ? 'success-text' : 'danger-text'">{{ health.status?.toUpperCase() ?? $t('common.unknown') }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper primary-bg"><el-icon><Connection /></el-icon></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('sdn.connectedSwitches') }}</div>
                <div class="stat-value primary-text">{{ health.details?.switches ?? 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper warning-bg"><el-icon><Key /></el-icon></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('sdn.activeFlows') }}</div>
                <div class="stat-value warning-text">{{ health.details?.activeFlows ?? 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </template>
    </el-row>

    <el-row :gutter="24" class="action-row">
      <el-col :span="12">
        <el-card shadow="hover" class="action-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon-danger"><Lock /></el-icon> {{ $t('sdn.blockFlow') }}</span>
            </div>
          </template>
          <el-form :model="blockForm" label-width="120px" size="default">
            <el-form-item :label="$t('sdn.sourceIp')">
              <el-input v-model="blockForm.srcIp" placeholder="192.168.1.100" class="mono" />
            </el-form-item>
            <el-form-item :label="$t('sdn.destIp')">
              <el-input v-model="blockForm.dstIp" placeholder="192.168.2.200" class="mono" />
            </el-form-item>
            <el-form-item :label="$t('sdn.protocol')">
              <el-select v-model="blockForm.protocol" style="width:100%">
                <el-option :label="$t('protocol.tcp')" value="tcp" />
                <el-option :label="$t('protocol.udp')" value="udp" />
                <el-option :label="$t('protocol.modbus')" value="modbus" />
                <el-option :label="$t('protocol.dnp3')" value="dnp3" />
                <el-option :label="$t('protocol.bacnet')" value="bacnet" />
                <el-option :label="$t('protocol.s7comm')" value="s7comm" />
                <el-option :label="$t('protocol.ethernetIp')" value="ethernet_ip" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('sdn.reason')">
              <el-input v-model="blockForm.reason" type="textarea" :rows="2" :placeholder="$t('sdn.reasonPlaceholder')" />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" :loading="blocking" @click="handleBlock">{{ $t('sdn.blockBtn') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover" class="action-card">
          <template #header>
            <div class="card-header">
              <span><el-icon class="card-header-icon-primary"><Upload /></el-icon> {{ $t('sdn.applyRules') }}</span>
            </div>
          </template>
          <div class="rules-apply">
            <p class="desc">{{ $t('sdn.rulesDesc') }}</p>
            <el-button type="primary" :loading="applying" @click="handleApplyRules">
              <el-icon><Upload /></el-icon> {{ $t('sdn.applyBtn') }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="lastOp" shadow="hover" class="result-card">
      <template #header>
        <div class="card-header">
          <span><el-icon class="card-header-icon-success"><List /></el-icon> {{ $t('sdn.lastOperation') }}</span>
          <el-tag :type="lastOp.status === 'success' ? 'success' : 'danger'" effect="plain">
            {{ lastOp.status?.toUpperCase() }}
          </el-tag>
        </div>
      </template>
      <div class="result-body">
        <div class="result-row">
          <span class="lbl">{{ $t('sdn.operation') }}</span>
          <span class="tech-font mono">{{ lastOp.operation }}</span>
        </div>
        <div v-if="lastOp.details" class="result-details">
          <div v-for="(v, k) in lastOp.details" :key="k" class="result-row">
            <span class="lbl">{{ k }}</span>
            <span class="tech-font mono">{{ v }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Monitor, Refresh, CircleCheckFilled, CircleCloseFilled, Connection, Key, Lock, Upload, List } from '@element-plus/icons-vue'
import { get, post } from '@/api/client'
import type { FlowBlockRequest, SdnOperationResponse } from '@/api/types'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'

const loading = ref(false)
const blocking = ref(false)
const applying = ref(false)

const health = reactive({ status: '', details: null as Record<string, any> | null })
const lastOp = ref<SdnOperationResponse | null>(null)

const blockForm = reactive<FlowBlockRequest>({ srcIp: '', dstIp: '', protocol: '', reason: '' })

async function fetchHealth() {
  loading.value = true
  try {
    const res = await get<{ status: string; details: Record<string, any> | null }>('/sdn/health')
    if (res.code === 200) Object.assign(health, res.data)
  } finally { loading.value = false }
}

async function handleBlock() {
  if (!blockForm.srcIp || !blockForm.dstIp || !blockForm.protocol) return
  blocking.value = true
  try {
    const res = await post<SdnOperationResponse>('/sdn/flows/block', blockForm)
    if (res.code === 200) lastOp.value = res.data
    blockForm.srcIp = ''; blockForm.dstIp = ''; blockForm.protocol = ''; blockForm.reason = ''
  } finally { blocking.value = false }
}

async function handleApplyRules() {
  applying.value = true
  try {
    const res = await post<SdnOperationResponse>('/sdn/rules/apply')
    if (res.code === 200) lastOp.value = res.data
  } finally { applying.value = false }
}

onMounted(fetchHealth)
</script>

<style scoped>
.sdn-page { padding: 0; }
.action-row { margin-bottom: var(--space-xl); }

.header-icon-primary { color: var(--color-primary); background: var(--color-primary-light); }

.card-header-icon-danger { color: var(--color-danger); font-size: 18px; }
.card-header-icon-primary { color: var(--color-primary); font-size: 18px; }
.card-header-icon-success { color: var(--color-success); font-size: 18px; }
</style>
