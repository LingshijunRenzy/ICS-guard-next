import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import { get, post, put, patch, del } from '@/api/client'

vi.mock('axios', () => {
  const mockAxios = {
    create: vi.fn(() => mockAxios),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  }
  return { default: mockAxios }
})

const mockAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>
  post: ReturnType<typeof vi.fn>
  put: ReturnType<typeof vi.fn>
  patch: ReturnType<typeof vi.fn>
  delete: ReturnType<typeof vi.fn>
}

describe('API client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('get', () => {
    it('returns response data on success', async () => {
      mockAxios.get.mockResolvedValueOnce({ data: { code: 200, data: { id: 1 } } })
      const res = await get('/test')
      expect(res).toEqual({ code: 200, data: { id: 1 } })
    })

    it('passes query params', async () => {
      mockAxios.get.mockResolvedValueOnce({ data: { code: 200, data: [] } })
      await get('/test', { page: 0, size: 10 })
      expect(mockAxios.get).toHaveBeenCalledWith('/test', { params: { page: 0, size: 10 } })
    })

    it('propagates axios errors', async () => {
      const err = new Error('Network Error')
      mockAxios.get.mockRejectedValueOnce(err)
      await expect(get('/test')).rejects.toThrow('Network Error')
    })
  })

  describe('post', () => {
    it('sends body and returns data', async () => {
      mockAxios.post.mockResolvedValueOnce({ data: { code: 201, data: { id: 2 } } })
      const res = await post('/test', { name: 'foo' })
      expect(res.code).toBe(201)
      expect(mockAxios.post).toHaveBeenCalledWith('/test', { name: 'foo' })
    })
  })

  describe('put', () => {
    it('sends body and returns data', async () => {
      mockAxios.put.mockResolvedValueOnce({ data: { code: 200, data: { id: 3 } } })
      const res = await put('/test/3', { name: 'bar' })
      expect(res.code).toBe(200)
      expect(mockAxios.put).toHaveBeenCalledWith('/test/3', { name: 'bar' })
    })
  })

  describe('patch', () => {
    it('sends partial body', async () => {
      mockAxios.patch.mockResolvedValueOnce({ data: { code: 200, data: null } })
      await patch('/test/3', { enabled: true })
      expect(mockAxios.patch).toHaveBeenCalledWith('/test/3', { enabled: true })
    })
  })

  describe('del', () => {
    it('calls delete', async () => {
      mockAxios.delete.mockResolvedValueOnce({ data: { code: 204, data: null } })
      await del('/test/3')
      expect(mockAxios.delete).toHaveBeenCalledWith('/test/3')
    })
  })
})
