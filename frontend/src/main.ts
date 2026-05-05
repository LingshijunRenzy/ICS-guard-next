import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import './styles/common.css'
import App from './App.vue'
import router from './router'
import { i18n } from './locales'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(TDesign)
app.use(i18n)
app.mount('#app')
