import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import './styles/common.css'
import './styles/charts.css'

// ── ECharts global registration (once, before any chart renders) ──
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
use([PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

import App from './App.vue'
import router from './router'
import { i18n } from './locales'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(TDesign)
app.use(i18n)
app.mount('#app')
