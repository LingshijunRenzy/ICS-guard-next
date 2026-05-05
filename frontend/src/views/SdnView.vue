<template>
  <div class="sdn-page">
    <div class="page-header">
      <div class="header-title">
        <span class="header-icon header-icon-primary"><DesktopIcon size="22px" /></span>
        <h2>{{ $t('sdn.title') }}</h2>
      </div>
      <t-button theme="primary" :loading="loading" @click="fetchHealth">
        <template #icon><RefreshIcon /></template>
        {{ $t('common.refreshStatus') }}
      </t-button>
    </div>

    <t-row :gutter="[16, 16]" class="stat-row">
      <template v-if="loading">
        <t-col v-for="n in 3" :key="'sk' + n" :span="4"><SkeletonCard /></t-col>
      </template>
      <template v-else>
        <t-col :span="4">
          <t-card :bordered="false" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper" :class="health.status === 'healthy' ? 'success-bg' : 'danger-bg'">
                <CheckCircleFilledIcon v-if="health.status === 'healthy'" size="28px" />
                <CloseCircleFilledIcon v-else size="28px" />
              </div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('sdn.controllerStatus') }}</div>
                <div class="stat-value" :class="health.status === 'healthy' ? 'success-text' : 'danger-text'">{{ health.status?.toUpperCase() ?? $t('common.unknown') }}</div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="4">
          <t-card :bordered="false" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper primary-bg"><LinkIcon size="28px" /></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('sdn.connectedSwitches') }}</div>
                <div class="stat-value primary-text">{{ health.details?.switches ?? 0 }}</div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="4">
          <t-card :bordered="false" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper warning-bg"><SecuredIcon size="28px" /></div>
              <div class="stat-info">
                <div class="stat-title">{{ $t('sdn.activeFlows') }}</div>
                <div class="stat-value warning-text">{{ health.details?.activeFlows ?? 0 }}</div>
              </div>
            </div>
          </t-card>
        </t-col>
      </template>
    </t-row>

    <t-row :gutter="[16, 16]" class="action-row">
      <t-col :span="12">
        <t-card :bordered="false" class="action-card">
          <template #header>
            <div class="card-header">
              <span><LockOnIcon size="18px" class="card-header-icon-danger" /> {{ $t('sdn.blockFlow') }}</span>
            </div>
          </template>
          <t-form :data="blockForm" label-width="120px">
            <t-form-item :label="$t('sdn.sourceIp')">
              <t-input v-model="blockForm.srcIp" placeholder="192.168.1.100" class="mono" />
            </t-form-item>
            <t-form-item :label="$t('sdn.destIp')">
              <t-input v-model="blockForm.dstIp" placeholder="192.168.2.200" class="mono" />
            </t-form-item>
            <t-form-item :label="$t('sdn.protocol')">
              <t-select v-model="blockForm.protocol" style="width:100%">
                <t-option :label="$t('protocol.tcp')" value="tcp" />
                <t-option :label="$t('protocol.udp')" value="udp" />
                <t-option :label="$t('protocol.modbus')" value="modbus" />
                <t-option :label="$t('protocol.dnp3')" value="dnp3" />
                <t-option :label="$t('protocol.bacnet')" value="bacnet" />
                <t-option :label="$t('protocol.s7comm')" value="s7comm" />
                <t-option :label="$t('protocol.ethernetIp')" value="ethernet_ip" />
              </t-select>
            </t-form-item>
            <t-form-item :label="$t('sdn.reason')">
              <t-textarea v-model="blockForm.reason" :rows="2" :placeholder="$t('sdn.reasonPlaceholder')" />
            </t-form-item>
            <t-form-item>
              <t-button v-if="auth.hasPermission('sdn:manage')" theme="danger" :loading="blocking" @click="handleBlock">{{ $t('sdn.blockBtn') }}</t-button>
            </t-form-item>
          </t-form>
        </t-card>
      </t-col>

      <t-col :span="12">
        <t-card :bordered="false" class="action-card">
          <template #header>
            <div class="card-header">
              <span><UploadIcon size="18px" class="card-header-icon-primary" /> {{ $t('sdn.applyRules') }}</span>
            </div>
          </template>
          <div class="rules-apply">
            <p class="desc">{{ $t('sdn.rulesDesc') }}</p>
            <t-button v-if="auth.hasPermission('sdn:manage')" theme="primary" :loading="applying" @click="handleApplyRules">
              <template #icon><UploadIcon /></template>
              {{ $t('sdn.applyBtn') }}
            </t-button>
          </div>
        </t-card>
      </t-col>
    </t-row>

    <t-card v-if="lastOp" :bordered="false" class="result-card">
      <template #header>
        <div class="card-header">
          <span><ViewListIcon size="18px" class="card-header-icon-success" /> {{ $t('sdn.lastOperation') }}</span>
          <t-tag :theme="lastOp.status === 'success' ? 'success' : 'danger'" variant="light">
            {{ lastOp.status?.toUpperCase() }}
          </t-tag>
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
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  DesktopIcon, RefreshIcon, CheckCircleFilledIcon, CloseCircleFilledIcon,
  LinkIcon, SecuredIcon, LockOnIcon, UploadIcon, ViewListIcon,
} from 'tdesign-icons-vue-next'
import { get, post } from '@/api/client'
import type { FlowBlockRequest, SdnOperationResponse } from '@/api/types'
import SkeletonCard from '@/components/skeleton/SkeletonCard.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
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

.card-header-icon-danger { color: var(--color-danger); }
.card-header-icon-primary { color: var(--color-primary); }
.card-header-icon-success { color: var(--color-success); }
</style>
