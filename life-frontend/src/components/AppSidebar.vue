<template>
  <header class="sidebar">
    <div class="sidebar__top">
      <!-- 品牌 Logo -->
      <router-link to="/" class="sidebar__logo" title="全能生活权益汇">
        <span class="ms-icon ms-icon--fill sidebar__logo-icon">all_inclusive</span>
      </router-link>

      <!-- 主导航 -->
      <nav class="sidebar__nav">
        <router-link
          v-for="item in navItems"
          :key="item.route"
          :to="item.route"
          class="nav-item"
          active-class="nav-item--active"
        >
          <span class="ms-icon nav-item__icon" :class="{ 'ms-icon--fill': isActive(item) }">
            {{ item.icon }}
          </span>
          <span class="nav-item__label">{{ item.label }}</span>
        </router-link>

        <!-- 更多（下拉菜单） -->
        <el-dropdown trigger="click" @command="handleMoreCommand">
          <a class="nav-item" :class="{ 'nav-item--active': false }" @click.prevent>
            <span class="ms-icon nav-item__icon">more_horiz</span>
            <span class="nav-item__label">更多</span>
          </a>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="notifications">
                <el-badge :value="unread" :hidden="!unread" :max="99">
                  <el-icon><Bell /></el-icon>
                </el-badge>
                消息通知
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>设置
              </el-dropdown-item>
              <el-dropdown-item command="help">
                <el-icon><QuestionFilled /></el-icon>帮助中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <!-- 发布按钮 -->
      <el-button class="publish-btn" round @click="publishDialogVisible = true">
        <span class="ms-icon ms-icon--fill publish-btn__icon--collapse">add</span>
        <span class="publish-btn__text">发布</span>
      </el-button>
    </div>

    <!-- 底部用户信息（点击弹出菜单，对应原型 Suki Pro + ··· 交互） -->
    <el-dropdown
      trigger="click"
      placement="top-start"
      popper-class="sidebar-user-menu"
      @command="handleUserCommand"
    >
      <div class="sidebar__user">
        <el-avatar :size="40" :src="user.avatar" />
        <div class="sidebar__user-meta">
          <span class="sidebar__user-name">{{ user.nickname }}</span>
          <span class="sidebar__user-id">{{ user.username }}</span>
        </div>
        <el-icon class="sidebar__user-more"><MoreFilled /></el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">
            <el-icon><User /></el-icon>查看资料
          </el-dropdown-item>
          <el-dropdown-item command="follow">
            <el-icon><UserFilled /></el-icon>关注 / 粉丝
          </el-dropdown-item>
          <el-dropdown-item command="logout" divided>
            <el-icon><SwitchButton /></el-icon>退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 发布弹窗（Teleport 到 body, 不受侧边栏层叠上下文影响） -->
    <PublishDialog v-model="publishDialogVisible" @published="onPublished" />
  </header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PublishDialog from '@/components/PublishDialog.vue'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/notice'

const route = useRoute()
const router = useRouter()
const { user, loadUser, logout } = useUserStore()

// 进入应用后拉取当前登录用户信息（覆盖侧边栏昵称/头像等）
loadUser()

// 消息未读角标：登录后拉取 + 60s 轮询（后端 /notice/unread/count）
const unread = ref(0)
let unreadTimer = null
async function refreshUnread() {
  if (!user.id) {
    unread.value = 0
    return
  }
  try {
    unread.value = (await getUnreadCount()) || 0
  } catch {
    /* 角标失败静默 */
  }
}
onMounted(() => {
  refreshUnread()
  unreadTimer = setInterval(refreshUnread, 60000)
})
onUnmounted(() => unreadTimer && clearInterval(unreadTimer))
watch(() => user.id, refreshUnread)

const navItems = [
  { route: '/', label: '首页', icon: 'home' },
  { route: '/mall', label: '商城', icon: 'storefront' },
  { route: '/cart', label: '购物车', icon: 'shopping_cart' },
  { route: '/orders', label: '订单', icon: 'receipt_long' },
  { route: '/benefit-center', label: '权益中心', icon: 'workspace_premium' },
  { route: '/my-benefits', label: '卡包', icon: 'confirmation_number' },
  { route: '/assistant', label: 'AI 助手', icon: 'smart_toy' },
  { route: '/profile', label: '个人中心', icon: 'person' },
]

// 首页精确匹配，其余支持子路径（如 /mall/goods/xxx 仍高亮「商城」）
const isActive = (item) =>
  item.route === '/' ? route.path === '/' : route.path.startsWith(item.route)

const publishDialogVisible = ref(false)

// 弹窗内发布成功后提示并刷新用户信息
function onPublished() {
  loadUser(true)
}

const loggingOut = ref(false)

async function doLogout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  } finally {
    loggingOut.value = false
  }
}

// 左下角头像弹出菜单
function handleUserCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'follow') {
    router.push({ path: '/follow', query: { tab: 'following' } })
  } else if (command === 'logout') {
    doLogout()
  }
}

async function handleMoreCommand(command) {
  if (command === 'logout') {
    doLogout()
    return
  }
  if (command === 'settings') {
    router.push('/settings')
    return
  }
  if (command === 'notifications') {
    router.push('/messages')
    return
  }
  const tips = {
    help: '帮助中心（待接入后端）',
  }
  ElMessage.info(tips[command] || '功能开发中')
}

// 面包屑占位，避免 computed 未使用告警
const _routeName = computed(() => route.name)
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: sticky;
  top: 0;
  height: 100vh;
  width: 88px;
  flex-shrink: 0;
  padding: 8px;
  border-right: 1px solid var(--color-border);
}

.sidebar__top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.sidebar__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 8px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.sidebar__logo:hover {
  background: var(--color-surface-low);
}

.sidebar__logo-icon {
  font-size: 32px;
  color: var(--color-text);
}

.sidebar__nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100%;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px;
  border-radius: 9999px;
  cursor: pointer;
  transition: background-color 0.2s;
  width: max-content;
}

.nav-item:hover {
  background: rgba(15, 20, 25, 0.06);
}

.nav-item__icon {
  font-size: 26px;
  color: var(--color-text);
}

.nav-item__label {
  display: none;
  font-size: 20px;
  font-weight: 400;
  color: var(--color-text);
}

.nav-item--active .nav-item__label {
  font-weight: 700;
}

.publish-btn {
  width: 48px;
  height: 48px;
  margin-top: 16px;
  background: var(--color-primary);
  border: none;
  color: #fff;
  font-weight: 700;
  font-size: 17px;
  box-shadow: 0 2px 6px rgba(29, 155, 240, 0.3);
}

.publish-btn:hover,
.publish-btn:focus {
  background: var(--color-primary-hover);
  color: #fff;
}

.publish-btn__icon--collapse {
  font-size: 22px;
}

.publish-btn__text {
  display: none;
}

.sidebar__user {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 12px;
  border-radius: 9999px;
  transition: background-color 0.2s;
  cursor: pointer;
  outline: none;
  color: inherit;
  width: 100%;
}

.sidebar__user:hover {
  background: var(--color-surface-low);
  text-decoration: none;
}

.sidebar__user-meta {
  display: none;
  flex-direction: column;
  flex: 1;
  text-align: left;
  line-height: 1.2;
}

.sidebar__user-name {
  font-weight: 700;
  font-size: 15px;
  color: var(--color-text);
}

.sidebar__user-id {
  font-size: 15px;
  color: var(--color-text-secondary);
}

.sidebar__user-more {
  display: none;
  color: var(--color-text);
}

/* 宽屏（≥1280px）：展开为完整侧边栏 */
@media (min-width: 1280px) {
  .sidebar {
    width: 275px;
    padding: 8px 16px;
  }

  .sidebar__top,
  .sidebar__nav {
    align-items: flex-start;
  }

  .sidebar__logo {
    margin-left: 8px;
  }

  .nav-item {
    padding: 12px 16px;
  }

  .nav-item__label,
  .publish-btn__text,
  .sidebar__user-meta,
  .sidebar__user-more {
    display: block;
  }

  .sidebar__user {
    justify-content: flex-start;
  }

  .sidebar__user-more {
    display: inline-flex;
  }

  .publish-btn {
    width: 100%;
    height: 56px;
    border-radius: 9999px;
  }

  .publish-btn__icon--collapse {
    display: none;
  }
}
</style>
