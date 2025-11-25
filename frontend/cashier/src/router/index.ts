import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/pay/:orderId',
      name: 'pay',
      component: () => import('../views/PayView.vue'),
      props: true,
    },
    {
      path: '/result/:orderId',
      name: 'result',
      component: () => import('../views/ResultView.vue'),
      props: true,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

export default router
