export function formatBytes(bytes: number): string {
  if (bytes >= 1e9) return (bytes / 1e9).toFixed(1) + ' GB'
  if (bytes >= 1e6) return (bytes / 1e6).toFixed(1) + ' MB'
  if (bytes >= 1e3) return (bytes / 1e3).toFixed(1) + ' KB'
  return bytes + ' B'
}

export function formatDateTime(iso: string): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-CA', { hour12: false })
}

export function calcPercent(count: number, total: number): number {
  if (!total) return 0
  return Math.min(100, Math.round((count / total) * 100))
}

export function calcBarPercent(bytes: number, maxBytes: number): number {
  if (!maxBytes) return 0
  return Math.min(100, Math.max(2, Math.round((bytes / maxBytes) * 100)))
}

export function hasEntries(obj: Record<string, number> | undefined): boolean {
  return obj != null && Object.keys(obj).length > 0
}
