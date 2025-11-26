import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('../layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('../views/OrderListView.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'accounts',
        name: 'accounts',
        component: () => import('../views/AccountListView.vue'),
        meta: { title: '账号管理' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/UserListView.vue'),
        meta: { title: '用户管理', requiresAdmin: true },
      },
      {
        path: 'test-pay',
        name: 'test-pay',
        component: () => import('../views/TestPayView.vue'),
        meta: { title: '测试支付' },
      },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // 需要认证的页面
  if (to.meta.requiresAuth !== false) {
    if (!authStore.isAuthenticated) {
      return next({ name: 'login', query: { redirect: to.fullPath } })
    }

    // 初始化用户信息
    if (!authStore.user) {
      try {
        await authStore.initialize()
      } catch {
        return next({ name: 'login' })
      }
    }

    // 管理员页面检查
    if (to.meta.requiresAdmin && !authStore.isAdmin) {
      return next({ name: 'dashboard' })
    }
  }

  // 已登录用户访问登录页，重定向到首页
  if (to.name === 'login' && authStore.isAuthenticated) {
    return next({ name: 'dashboard' })
  }

  next()
})
