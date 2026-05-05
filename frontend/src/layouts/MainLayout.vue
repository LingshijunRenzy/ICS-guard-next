<template>
  <t-layout class="app-container">
    <t-aside class="sidebar" width="232px">
      <div class="logo">
        <span class="logo-icon" />
        <span class="logo-text">{{ $t('common.appName') }}</span>
      </div>
      <t-menu :value="route.path" theme="light"
        class="custom-menu"
@change="handleMenuChange"
      >
        <t-menu-item value="/">
          <template #icon>
            <DashboardIcon />
          </template>
          {{ $t('layout.dashboard') }}
        </t-menu-item>
        <t-menu-item value="/alerts">
          <template #icon>
            <NotificationIcon />
          </template>
          {{ $t('layout.alerts') }}
        </t-menu-item>
        <t-menu-item value="/rules">
          <template #icon>
            <SettingIcon />
          </template>
          {{ $t('layout.rules') }}
        </t-menu-item>
        <t-menu-item value="/topology">
          <template #icon>
            <ShareIcon />
          </template>
          {{ $t('layout.topology') }}
        </t-menu-item>
        <t-menu-item value="/traffic">
          <template #icon>
            <LinkIcon />
          </template>
          {{ $t('layout.traffic') }}
        </t-menu-item>
        <t-menu-item value="/audit-logs">
          <template #icon>
            <FileIcon />
          </template>
          {{ $t('layout.auditLogs') }}
        </t-menu-item>
        <t-menu-item value="/users">
          <template #icon>
            <UserIcon />
          </template>
          {{ $t('layout.users') }}
        </t-menu-item>
        <t-menu-item value="/sdn">
          <template #icon>
            <DesktopIcon />
          </template>
          {{ $t('layout.sdnControl') }}
        </t-menu-item>
      </t-menu>
    </t-aside>

    <t-layout class="right-layout">
      <t-header class="header">
        <div class="header-left">
          <span class="campus-title">{{ $t('common.campusTitle') }}</span>
        </div>
        <div class="header-right">
          <t-button variant="text" class="lang-btn" @click="toggleLang">{{ $t('langSwitch') }}</t-button>
          <t-divider layout="vertical" />
          <t-avatar size="32px" class="user-avatar">{{ auth.username?.charAt(0).toUpperCase() }}</t-avatar>
          <span class="greeting">{{ auth.username }}</span>
          <t-divider layout="vertical" />
          <t-button variant="text" theme="primary" @click="handleLogout">{{ $t('layout.logout') }}</t-button>
        </div>
      </t-header>
      <t-content class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </t-content>
    </t-layout>
  </t-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { toggleLocale, currentLocale } from '@/locales'
import { patch } from '@/api/client'
import {
  DashboardIcon,
  NotificationIcon,
  SettingIcon,
  ShareIcon,
  LinkIcon,
  FileIcon,
  UserIcon,
  DesktopIcon,
} from 'tdesign-icons-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

function toggleLang() {
  toggleLocale()
  if (auth.isAuthenticated) {
    patch('/auth/profile', { language: currentLocale() }).catch(() => {})
  }
}

function handleMenuChange(value: string) {
  router.push(value)
}

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  overflow: hidden;
    display: flex;
}

.sidebar {
  background: var(--td-bg-color-secondarycontainer, #242424);
  box-shadow: var(--shadow-sidebar);
  overflow-y: auto;
  z-index: 10;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
    height: 100vh;
  }
  
  .right-layout {
    display: flex;
    flex-direction: column;
    flex: 1;
    height: 100vh;
    overflow: hidden;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 var(--space-xl);
  color: var(--td-text-color-primary);
    border-bottom: 1px solid var(--td-component-border);
    flex-shrink: 0;
}

.logo-icon {
  width: 12px;
  height: 12px;
  background: var(--color-primary);
  border-radius: 50%;
  margin-right: var(--space-md);
  box-shadow: 0 0 10px var(--color-primary), 0 0 20px var(--color-primary);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
      box-shadow: 0 0 0 0 rgba(0, 82, 217, 0.7);
    }
  
    70% {
      box-shadow: 0 0 0 6px rgba(0, 82, 217, 0);
    }
  
    100% {
      box-shadow: 0 0 0 0 rgba(0, 82, 217, 0);
    }
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
    color: var(--td-text-color-primary);
}
.custom-menu {
  border-right: none;
  flex: 1;
  background: transparent !important;
}

.custom-menu :deep(.t-menu__item) {
  margin: 4px 8px;
  border-radius: var(--td-radius-default);
}


.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
  padding: 0 var(--space-xl);
  height: 64px;
  flex-shrink: 0;
  z-index: 5;
}

.content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--space-xl);
  background-color: var(--bg-page);
}
.header-left .campus-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-heading);
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.lang-btn {
  color: var(--color-primary);
  font-weight: 500;
  font-size: 13px;
}

.user-avatar {
  background-color: var(--color-primary);
  color: #fff;
  font-weight: bold;
}

.greeting {
  color: var(--text-regular);
  font-weight: 500;
}

.content {
  background: var(--bg-page);
  padding: var(--space-xl);
  min-height: 0;
  box-sizing: border-box;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all var(--transition-fast);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
