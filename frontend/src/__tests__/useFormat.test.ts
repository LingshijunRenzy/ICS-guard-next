import { describe, it, expect } from 'vitest'
import { formatBytes, formatDateTime, calcPercent, calcBarPercent, hasEntries } from '@/composables/useFormat'

describe('formatBytes', () => {
  it('returns bytes for values under 1000', () => {
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(500)).toBe('500 B')
  })

  it('formats KB', () => {
    expect(formatBytes(1500)).toBe('1.5 KB')
    expect(formatBytes(100000)).toBe('100.0 KB')
  })

  it('formats MB', () => {
    expect(formatBytes(5_000_000)).toBe('5.0 MB')
  })

  it('formats GB', () => {
    expect(formatBytes(3_000_000_000)).toBe('3.0 GB')
  })
})

describe('formatDateTime', () => {
  it('returns empty string for falsy input', () => {
    expect(formatDateTime('')).toBe('')
  })

  it('returns locale string for ISO date', () => {
    const result = formatDateTime('2026-04-30T12:00:00Z')
    expect(result).toContain('2026')
    expect(result).toContain('04')
    expect(result).toContain('30')
  })
})

describe('calcPercent', () => {
  it('returns 0 for zero total', () => {
    expect(calcPercent(5, 0)).toBe(0)
  })

  it('calculates percentage correctly', () => {
    expect(calcPercent(25, 100)).toBe(25)
    expect(calcPercent(3, 10)).toBe(30)
  })

  it('caps at 100', () => {
    expect(calcPercent(200, 100)).toBe(100)
  })
})

describe('calcBarPercent', () => {
  it('returns 0 for zero maxBytes', () => {
    expect(calcBarPercent(100, 0)).toBe(0)
  })

  it('calculates bar percentage with minimum 2', () => {
    expect(calcBarPercent(1, 100)).toBe(2)
    expect(calcBarPercent(50, 100)).toBe(50)
  })

  it('caps at 100', () => {
    expect(calcBarPercent(200, 100)).toBe(100)
  })
})

describe('hasEntries', () => {
  it('returns false for undefined', () => {
    expect(hasEntries(undefined)).toBe(false)
  })

  it('returns false for empty object', () => {
    expect(hasEntries({})).toBe(false)
  })

  it('returns true for non-empty object', () => {
    expect(hasEntries({ a: 1 })).toBe(true)
  })
})
