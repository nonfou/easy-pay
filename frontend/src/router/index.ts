import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'payment-demo',
    component: () => import('../views/PaymentDemoView.vue'),
    meta: { title: '支付演示' },
  },
  {
    path: '/result',
    name: 'payment-result',
    component: () => import('../views/PaymentResultView.vue'),
    meta: { title: '支付结果' },
  },
  {
    path: '/refund',
    name: 'refund-demo',
    component: () => import('../views/RefundDemoView.vue'),
    meta: { title: '退款测试' },
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})
