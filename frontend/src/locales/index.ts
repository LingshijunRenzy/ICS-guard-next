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
  i18n.global.locale.value = next
  localStorage.setItem('ics-lang', next)
}

export const currentLocale = () => i18n.global.locale.value
