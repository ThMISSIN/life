import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 - Omni-Life 全能生活', public: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册 - Omni-Life 全能生活', public: true },
  },
  {
    path: '/forgot',
    name: 'forgot',
    component: () => import('@/views/ForgotPasswordView.vue'),
    meta: { title: '忘记密码 - Omni-Life 全能生活', public: true },
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页 - 全能生活权益汇' },
  },
  {
    path: '/mall',
    name: 'mall',
    component: () => import('@/views/MallView.vue'),
    meta: { title: '商城 - 全能生活权益汇' },
  },
  {
    path: '/mall/goods/:id',
    name: 'goods-detail',
    component: () => import('@/views/GoodsDetailView.vue'),
    meta: { title: '商品详情 - 全能生活权益汇' },
  },
  {
    path: '/benefit-center',
    name: 'benefit-center',
    component: () => import('@/views/BenefitCenterView.vue'),
    meta: { title: '权益中心 - 全能生活权益汇' },
  },
  {
    path: '/benefit/:id',
    name: 'benefit-detail',
    component: () => import('@/views/BenefitDetailView.vue'),
    meta: { title: '权益详情 - 全能生活权益汇' },
  },
  {
    path: '/my-benefits',
    name: 'my-benefits',
    component: () => import('@/views/MyBenefitsView.vue'),
    meta: { title: '我的权益 - 全能生活权益汇' },
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/views/CartView.vue'),
    meta: { title: '购物车 - 全能生活权益汇' },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/OrdersView.vue'),
    meta: { title: '我的订单 - 全能生活权益汇' },
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/views/MessagesView.vue'),
    meta: { title: '消息通知 - 全能生活权益汇' },
  },
  {
    path: '/address',
    name: 'address',
    component: () => import('@/views/AddressView.vue'),
    meta: { title: '收货地址 - 全能生活权益汇' },
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: '设置 - 全能生活权益汇' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人中心 - 全能生活权益汇' },
  },
  {
    path: '/u/:userId',
    name: 'user-profile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { title: '用户主页 - 全能生活权益汇' },
  },
  {
    path: '/follow',
    name: 'follow',
    component: () => import('@/views/FollowView.vue'),
    meta: { title: '关注/粉丝 - 全能生活权益汇' },
  },
  {
    path: '/assistant',
    name: 'assistant',
    component: () => import('@/views/AssistantView.vue'),
    meta: { title: 'AI 助手 - 全能生活权益汇' },
  },
  // 兜底：重定向到首页
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫：未登录访问业务页 -> 跳登录页（带回跳地址）；已登录访问认证页 -> 回首页
router.beforeEach((to) => {
  const hasToken = Boolean(getToken())
  if (!hasToken && !to.meta.public) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (hasToken && ['login', 'register', 'forgot'].includes(to.name)) {
    return { path: '/' }
  }
})

router.afterEach((to) => {
  if (to.meta.title) document.title = to.meta.title
})

export default router
