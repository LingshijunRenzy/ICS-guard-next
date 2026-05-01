import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { post, get } from '@/api/client'
import type { LoginRequest, UserResponse } from '@/api/types'
import { applyServerLanguage } from '@/locales'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserResponse | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => user.value !== null)
  const username = computed(() => user.value?.username ?? '')
  const roles = computed(() => user.value?.roles ?? [])

  async function login(credentials: LoginRequest) {
    loading.value = true
    try {
      const res = await post<UserResponse>('/auth/login', credentials)
      if (res.code === 200) {
        user.value = res.data
        applyServerLanguage(res.data.profile?.language)
      }
      return res
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    await post('/auth/logout')
    user.value = null
  }

  async function fetchMe() {
    try {
      const res = await get<UserResponse>('/auth/me')
      if (res.code === 200) {
        user.value = res.data
        applyServerLanguage(res.data.profile?.language)
      }
    } catch {
      user.value = null
    }
  }

  return { user, loading, isAuthenticated, username, roles, login, logout, fetchMe }
})
