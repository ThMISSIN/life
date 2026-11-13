<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="messages-main">
      <div class="messages-header">
        <h2 class="messages-header__title">消息通知</h2>
        <el-button v-if="unread > 0" text type="primary" @click="onReadAll">
          全部标为已读（{{ unread }}）
        </el-button>
      </div>

      <div class="messages-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value ?? 'all'"
          class="messages-tabs__item"
          :class="{ 'is-active': activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-loading="loading" class="messages-body card">
        <div
          v-for="n in list"
          :key="n.id"
          class="msg"
          :class="{ 'msg--unread': n.isRead === 0 }"
          @click="openNotice(n)"
        >
          <span class="ms-icon msg__icon" :class="{ 'is-new': n.isRead === 0 }">
            {{ typeMeta(n.type).icon }}
          </span>
          <div class="msg__main">
            <div class="msg__top">
              <span class="msg__title">{{ n.title }}</span>
              <span class="msg__type">{{ typeMeta(n.type).label }}</span>
            </div>
            <p class="msg__content">{{ n.content }}</p>
            <span class="msg__time">{{ formatTime(n.createTime) }}</span>
          </div>
          <span v-if="n.isRead === 0" class="msg__dot" />
        </div>

        <el-empty v-if="!loading && !list.length" description="暂无消息" :image-size="80" />

        <div v-if="list.length" class="messages-foot">
          <el-button v-if="hasMore" text :loading="loading" @click="loadMore">加载更多</el-button>
          <span v-else class="messages-foot__end">— 到底啦 —</span>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
/**
 * 消息中心(1.3 C端读接口对接): GET /notice/page + /notice/unread/count + PUT 已读
 * 后端为唯一事实源, 推送(WebSocket)仅作在线加速, 断线消息在此照常可见
 */
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import { getNoticePage, getUnreadCount, markNoticeRead, markAllRead, NOTICE_TYPE_MAP } from '@/api/notice'

const router = useRouter()
const tabs = [
  { label: '全部', value: null },
  { label: '未读', value: 0 },
  { label: '已读', value: 1 },
]
const activeTab = ref(null)
const list = ref([])
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const unread = ref(0)
let timer = null

const hasMore = () => list.value.length < total.value
const typeMeta = (t) => NOTICE_TYPE_MAP[t] || { label: '通知', icon: 'notifications' }

function formatTime(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return t
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

async function fetchPage(reset) {
  if (loading.value) return
  loading.value = true
  try {
    const params = { pageNum: reset ? 1 : pageNum.value + 1, pageSize }
    if (activeTab.value !== null) params.isRead = activeTab.value
    const res = await getNoticePage(params)
    const records = res?.records || []
    list.value = reset ? records : [...list.value, ...records]
    total.value = res?.total || 0
    pageNum.value = params.pageNum
  } catch (e) {
    ElMessage.error(e.message || '消息加载失败')
  } finally {
    loading.value = false
  }
}

function loadMore() {
  fetchPage(false)
}

function switchTab(v) {
  if (activeTab.value === v) return
  activeTab.value = v
  fetchPage(true)
}

async function openNotice(n) {
  if (n.isRead === 0) {
    try {
      await markNoticeRead(n.id)
      n.isRead = 1
      refreshUnread()
    } catch {
      /* 已读失败不阻断跳转 */
    }
  }
  if (n.url) {
    try {
      router.push(n.url)
    } catch {
      /* 无匹配路由的运营位链接静默忽略 */
    }
  }
}

async function onReadAll() {
  try {
    await markAllRead()
    list.value.forEach((n) => (n.isRead = 1))
    refreshUnread()
    ElMessage.success('已全部标为已读')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function refreshUnread() {
  try {
    unread.value = (await getUnreadCount()) || 0
  } catch {
    /* 角标失败静默 */
  }
}

onMounted(() => {
  fetchPage(true)
  refreshUnread()
  // 轻量轮询角标(60s); WebSocket 实时推送为后续增强(后端通道已就绪)
  timer = setInterval(refreshUnread, 60000)
})
onUnmounted(() => timer && clearInterval(timer))
</script>

<style scoped>
.messages-main {
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  padding: 20px 16px 60px;
}
.messages-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.messages-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
}
.messages-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.messages-tabs__item {
  padding: 6px 16px;
  border: none;
  border-radius: 9999px;
  background: var(--color-surface-low, #f1f5f9);
  color: var(--color-text-secondary, #64748b);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.messages-tabs__item.is-active {
  background: var(--color-primary, #1d9bf0);
  color: #fff;
  font-weight: 700;
}
.card {
  background: #fff;
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 16px;
  min-height: 240px;
}
.messages-body {
  overflow: hidden;
}
.msg {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border, #eef2f7);
  cursor: pointer;
  transition: background 0.15s;
}
.msg:hover {
  background: var(--color-surface-low, #f8fafc);
}
.msg:last-of-type {
  border-bottom: none;
}
.msg__icon {
  font-size: 22px;
  color: var(--color-text-muted, #94a3b8);
  flex-shrink: 0;
  padding-top: 2px;
}
.msg__icon.is-new {
  color: var(--color-primary, #1d9bf0);
}
.msg__main {
  flex: 1;
  min-width: 0;
}
.msg__top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.msg__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text, #1f2937);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg--unread .msg__title::after {
  content: '未读';
  margin-left: 6px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--color-orange, #f97316);
  border-radius: 4px;
  padding: 1px 4px;
  vertical-align: middle;
}
.msg__type {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--color-text-secondary, #64748b);
  background: var(--color-surface-low, #f1f5f9);
  border-radius: 4px;
  padding: 1px 6px;
}
.msg__content {
  margin: 4px 0 6px;
  font-size: 13px;
  color: var(--color-text-secondary, #64748b);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.msg__time {
  font-size: 11px;
  color: var(--color-text-muted, #94a3b8);
}
.msg__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-orange, #f97316);
  align-self: center;
  flex-shrink: 0;
}
.messages-foot {
  display: flex;
  justify-content: center;
  padding: 10px 0 14px;
}
.messages-foot__end {
  font-size: 12px;
  color: #c4c9d2;
}
</style>
