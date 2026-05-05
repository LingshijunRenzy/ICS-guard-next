import { ref } from 'vue'

const STORAGE_KEY = 'ics-theme'

export type ThemeMode = 'light' | 'dark'

const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode
const defaultMode: ThemeMode = saved ? saved : (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')

export const currentTheme = ref<ThemeMode>(defaultMode)

export function applyTheme(mode: ThemeMode) {
  if (mode !== 'light' && mode !== 'dark') return
  currentTheme.value = mode
  localStorage.setItem(STORAGE_KEY, mode)
  document.documentElement.setAttribute('theme-mode', mode)
}

export function toggleTheme() {
  const next: ThemeMode = currentTheme.value === 'light' ? 'dark' : 'light'
  applyTheme(next)
}

export function applyServerTheme(profileTheme: string | null | undefined) {
  if (!profileTheme) return
  if (profileTheme === 'dark' || profileTheme === 'light') {
    applyTheme(profileTheme)
  }
}

// Initial apply
applyTheme(defaultMode)
