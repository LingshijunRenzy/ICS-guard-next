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
          meta: { title: 'Dashboard', permission: 'dashboard:read' },
        },
        {
          path: 'alerts',
          name: 'Alerts',
          component: () => import('@/views/AlertsView.vue'),
          meta: { title: 'Alerts', permission: 'alerts:read' },
        },
        {
          path: 'rules',
          name: 'Rules',
          component: () => import('@/views/RulesView.vue'),
          meta: { title: 'Rules', permission: 'rules:read' },
        },
        {
          path: 'topology',
          name: 'Topology',
          component: () => import('@/views/TopologyView.vue'),
          meta: { title: 'Topology', permission: 'topology:read' },
        },
        {
          path: 'traffic',
          name: 'Traffic',
          component: () => import('@/views/TrafficView.vue'),
          meta: { title: 'Traffic', permission: 'metrics:read' },
        },
        {
          path: 'audit-logs',
          name: 'AuditLogs',
          component: () => import('@/views/AuditLogsView.vue'),
          meta: { title: 'Audit Logs', permission: 'audit:read' },
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/UsersView.vue'),
          meta: { title: 'Users', permission: 'users:read' },
        },
        {
          path: 'roles',
          name: 'Roles',
          component: () => import('@/views/RolesView.vue'),
          meta: { title: 'Roles', permission: 'roles:read' },
        },
        {
          path: 'permissions',
          name: 'Permissions',
          component: () => import('@/views/PermissionsView.vue'),
          meta: { title: 'Permissions', permission: 'permissions:read' },
        },
        {
          path: 'sdn',
          name: 'Sdn',
          component: () => import('@/views/SdnView.vue'),
          meta: { title: 'SDN Control', permission: 'sdn:read' },
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

  const perm = to.meta.permission as string | undefined
  if (perm && !auth.hasPermission(perm)) {
    return '/403'
  }

  return true
})

export default router
