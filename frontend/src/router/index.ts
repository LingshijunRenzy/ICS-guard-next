import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: 'Dashboard' },
        },
        {
          path: 'alerts',
          name: 'Alerts',
          component: () => import('@/views/AlertsView.vue'),
          meta: { title: 'Alerts' },
        },
        {
          path: 'rules',
          name: 'Rules',
          component: () => import('@/views/RulesView.vue'),
          meta: { title: 'Rules' },
        },
        {
          path: 'topology',
          name: 'Topology',
          component: () => import('@/views/TopologyView.vue'),
          meta: { title: 'Topology' },
        },
        {
          path: 'traffic',
          name: 'Traffic',
          component: () => import('@/views/TrafficView.vue'),
          meta: { title: 'Traffic' },
        },
        {
          path: 'audit-logs',
          name: 'AuditLogs',
          component: () => import('@/views/AuditLogsView.vue'),
          meta: { title: 'Audit Logs' },
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/UsersView.vue'),
          meta: { title: 'Users' },
        },
        {
          path: 'sdn',
          name: 'Sdn',
          component: () => import('@/views/SdnView.vue'),
          meta: { title: 'SDN Control' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.isAuthenticated) {
    await auth.fetchMe()
  }

  if (to.meta.guest) {
    if (auth.isAuthenticated) return '/'
    return true
  }

  if (!auth.isAuthenticated) {
    return '/login'
  }

  return true
})

export default router
