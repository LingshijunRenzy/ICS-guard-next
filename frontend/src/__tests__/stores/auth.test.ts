import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/client', () => ({
  post: vi.fn(),
  get: vi.fn(),
}))

vi.mock('@/locales', () => ({
  applyServerLanguage: vi.fn(),
  setLocale: vi.fn(),
  currentLocale: vi.fn(() => 'en'),
}))

import { post, get } from '@/api/client'

const mockPost = post as ReturnType<typeof vi.fn>
const mockGet = get as ReturnType<typeof vi.fn>

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('sets username and roles on successful login', async () => {
      mockPost.mockResolvedValueOnce({
        code: 200,
        data: { username: 'admin', roles: ['ADMIN'] },
      })

      const store = useAuthStore()
      const res = await store.login({ username: 'admin', password: 'admin123' })

      expect(res.code).toBe(200)
      expect(store.username).toBe('admin')
      expect(store.roles).toEqual(['ADMIN'])
      expect(store.isAuthenticated).toBe(true)
    })

    it('does not set state on failed login', async () => {
      mockPost.mockResolvedValueOnce({
        code: 401,
        msg: 'Invalid credentials',
      })

      const store = useAuthStore()
      const res = await store.login({ username: 'bad', password: 'wrong' })

      expect(res.code).toBe(401)
      expect(store.username).toBe('')
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('fetchMe', () => {
    it('sets user from /auth/me response', async () => {
      mockGet.mockResolvedValueOnce({
        code: 200,
        data: { username: 'operator', roles: ['OPERATOR'] },
      })

      const store = useAuthStore()
      await store.fetchMe()

      expect(store.username).toBe('operator')
      expect(store.roles).toEqual(['OPERATOR'])
      expect(store.isAuthenticated).toBe(true)
    })

    it('clears state on fetch failure', async () => {
      mockGet.mockRejectedValueOnce(new Error('Network error'))

      const store = useAuthStore()
      // pre-set state to ensure it's cleared
      store.user = { id: 1, username: 'admin', email: null, displayName: null, enabled: true, roles: ['ADMIN'], createdAt: '', updatedAt: '', profile: null }

      await store.fetchMe()

      expect(store.username).toBe('')
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('logout', () => {
    it('clears state and calls post', async () => {
      mockPost.mockResolvedValueOnce({ code: 200 })

      const store = useAuthStore()
      store.user = { id: 1, username: 'admin', email: null, displayName: null, enabled: true, roles: ['ADMIN'], createdAt: '', updatedAt: '', profile: null }

      await store.logout()

      expect(store.username).toBe('')
      expect(store.roles).toEqual([])
      expect(store.isAuthenticated).toBe(false)
      expect(mockPost).toHaveBeenCalledWith('/auth/logout')
    })
  })
})
