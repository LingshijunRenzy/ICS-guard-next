<template>
  <el-container class="app-container">
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <span class="logo-icon"></span>
        <span class="logo-text">{{ $t('common.appName') }}</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="custom-menu"
        background-color="transparent"
        text-color="#adb5bd"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>{{ $t('layout.dashboard') }}</span>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Bell /></el-icon>
          <span>{{ $t('layout.alerts') }}</span>
        </el-menu-item>
        <el-menu-item index="/rules">
          <el-icon><Setting /></el-icon>
          <span>{{ $t('layout.rules') }}</span>
        </el-menu-item>
        <el-menu-item index="/topology">
          <el-icon><Share /></el-icon>
          <span>{{ $t('layout.topology') }}</span>
        </el-menu-item>
        <el-menu-item index="/traffic">
          <el-icon><Connection /></el-icon>
          <span>{{ $t('layout.traffic') }}</span>
        </el-menu-item>
        <el-menu-item index="/audit-logs">
          <el-icon><Document /></el-icon>
          <span>{{ $t('layout.auditLogs') }}</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>{{ $t('layout.users') }}</span>
        </el-menu-item>
        <el-menu-item index="/sdn">
          <el-icon><Monitor /></el-icon>
          <span>{{ $t('layout.sdnControl') }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="campus-title">{{ $t('common.campusTitle') }}</span>
        </div>
        <div class="header-right">
          <el-button link @click="toggleLang" class="lang-btn">{{ $t('langSwitch') }}</el-button>
          <el-divider direction="vertical" />
          <el-avatar :size="32" class="user-avatar">{{ auth.username?.charAt(0).toUpperCase() }}</el-avatar>
          <span class="greeting">{{ auth.username }}</span>
          <el-divider direction="vertical" />
          <el-button link type="primary" @click="handleLogout">{{ $t('layout.logout') }}</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { toggleLocale } from '@/locales'
import { Odometer, Bell, Setting, Share, Connection, Document, User, Monitor } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

function toggleLang() {
  toggleLocale()
}

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}
.sidebar {
  background: linear-gradient(180deg, #051328 0%, #0b1a37 100%);
  box-shadow: 2px 0 8px 0 rgba(0, 0, 0, 0.15);
  overflow-y: auto;
  z-index: 10;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
.logo-icon {
  width: 12px;
  height: 12px;
  background: #409eff;
  border-radius: 50%;
  margin-right: 12px;
  box-shadow: 0 0 10px #409eff, 0 0 20px #409eff;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.7); }
  70% { box-shadow: 0 0 0 6px rgba(64, 158, 255, 0); }
  100% { box-shadow: 0 0 0 0 rgba(64, 158, 255, 0); }
}
.logo-text {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1.5px;
  background: linear-gradient(90deg, #fff, #409eff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.custom-menu {
  border-right: none;
  flex: 1;
  margin-top: 16px;
}
.custom-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 12px;
  border-radius: 6px;
}
.custom-menu .el-menu-item.is-active {
  background: rgba(64, 158, 255, 0.15) !important;
  color: #409eff !important;
  font-weight: 500;
}
.custom-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.05) !important;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 24px;
  height: 64px;
  z-index: 5;
}
.header-left .campus-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2f3d;
  letter-spacing: 0.5px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.lang-btn {
  color: #409eff;
  font-weight: 500;
  font-size: 13px;
}
.user-avatar {
  background-color: #409eff;
  color: #fff;
  font-weight: bold;
}
.greeting {
  color: #606266;
  font-weight: 500;
}
.content {
  background: #f0f2f5;
  padding: 24px;
  min-height: 0;
  box-sizing: border-box;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
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
