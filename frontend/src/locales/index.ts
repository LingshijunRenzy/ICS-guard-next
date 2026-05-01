import { createI18n } from 'vue-i18n'
import en from './en'
import zh from './zh'

const saved = localStorage.getItem('ics-lang')
const locale = saved === 'zh' ? 'zh' : 'en'

export const i18n = createI18n({
  legacy: false,
  locale,
  fallbackLocale: 'en',
  messages: { en, zh },
})

export function toggleLocale() {
  const next = i18n.global.locale.value === 'zh' ? 'en' : 'zh'
  setLocale(next)
}

export function setLocale(lang: string) {
  if (lang !== 'zh' && lang !== 'en') return
  i18n.global.locale.value = lang
  localStorage.setItem('ics-lang', lang)
}

export function applyServerLanguage(profileLang: string | null | undefined) {
  if (!profileLang || (profileLang !== 'zh' && profileLang !== 'en')) return
  if (profileLang !== i18n.global.locale.value) {
    setLocale(profileLang)
  }
}

export const currentLocale = () => i18n.global.locale.value
