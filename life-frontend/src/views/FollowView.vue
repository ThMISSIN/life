<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="follow-main">
      <!-- 吸顶头部 -->
      <div class="follow-header">
        <button class="follow-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <div class="follow-header__title">
          <h2>{{ isSelf ? '我的' : 'TA的' }}社交关系</h2>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="follow-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="follow-tabs__item"
          :class="{ 'is-active': activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}<span v-if="counts[tab.value] !== null" class="follow-tabs__num">{{ counts[tab.value] }}</span>
        </div>
      </div>

      <!-- 列表 -->
      <div v-loading="loading" class="follow-list">
        <div
          v-for="item in list"
          :key="item.author.userId"
          class="follow-item"
          @click="goProfile(item.author.userId)"
        >
          <el-avatar :size="44" :src="item.author.avatar" />
          <div class="follow-item__info">
            <div class="follow-item__name">
              <span>{{ item.author.nickname || item.author.username }}</span>
            </div>
            <div class="follow-item__meta">
              <span v-if="item.author.username">@{{ item.author.username }}</span>
              <span v-if="item.followTime"> · {{ relativeTime(item.followTime) }}</span>
            </div>
          </div>
          <button
            v-if="!isSelfUser(item.author.userId)"
            class="follow-item__btn"
            :class="{ 'is-followed': item.author.followed }"
            :disabled="pendingIds.has(item.author.userId)"
            @click.stop="toggleFollow(item)"
          >
            {{ item.author.followed ? '正在关注' : '关注' }}
          </button>
          <span v-else class="follow-item__self">我</span>
        </div>
        <el-empty v-if="!loading && list.length === 0" :description="emptyText" />
      </div>

      <el-pagination
        v-if="total > PAGE_SIZE"
        class="follow-pagination"
        layout="prev, pager, next"
        :total="total"
        :page-size="PAGE_SIZE"
        :current-page="pageNum"
        background
        @current-change="onPageChange"
      />
    </main>

    <aside class="follow-aside">
      <div class="follow-aside__sticky">
        <SearchBar />
        <SideFooter />
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import SearchBar from '@/components/SearchBar.vue'
import SideFooter from '@/components/SideFooter.vue'
import { followUser, getFollowersList, getFollowingList, unfollowUser } from '@/api/follow'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { user, loadUser } = useUserStore()

const PAGE_SIZE = 20

const tabs = [
  { label: '正在关注', value: 'following' },
  { label: '粉丝', value: 'followers' },
]

const activeTab = ref(route.query.tab === 'followers' ? 'followers' : 'following')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const pendingIds = ref(new Set())
const counts = reactive({ following: null, followers: null })

// 列表归属用户：不传 userId 默认本人
const ownerUserId = computed(() => (route.query.userId ? Number(route.query.userId) : user.id))
const isSelf = computed(() => String(ownerUserId.value) === String(user.id))

const emptyText = computed(() =>
  activeTab.value === 'following' ? '还没有关注任何人' : '还没有粉丝',
)

function relativeTime(value) {
  if (!value) return ''
  const t = new Date(value).getTime()
  if (Number.isNaN(t)) return String(value)
  const diff = Date.now() - t
  const day = 24 * 60 * 60 * 1000
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}小时内`
  if (diff < day) return `${Math.floor(diff / (60 * 60 * 1000))}天前`
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function isSelfUser(userId) {
  return String(userId) === String(user.id)
}

async function fetchList() {
  loading.value = true
  try {
    const fetcher = activeTab.value === 'following' ? getFollowingList : getFollowersList
    const res = await fetcher({ userId: ownerUserId.value, pageNum: pageNum.value, pageSize: PAGE_SIZE })
    list.value = res?.records || []
    total.value = Number(res?.total || 0)
    counts[activeTab.value] = total.value
  } catch (e) {
    ElMessage.error(e.message || '加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function loadOtherCount() {
  // 另一个 Tab 的计数（仅展示用，小请求 pageSize=1）
  const other = activeTab.value === 'following' ? 'followers' : 'following'
  if (counts[other] !== null) return
  try {
    const fetcher = other === 'following' ? getFollowingList : getFollowersList
    const res = await fetcher({ userId: ownerUserId.value, pageNum: 1, pageSize: 1 })
    counts[other] = Number(res?.total || 0)
  } catch {
    counts[other] = 0
  }
}

function switchTab(value) {
  if (activeTab.value === value) return
  activeTab.value = value
  pageNum.value = 1
  fetchList()
  loadOtherCount()
}

function onPageChange(p) {
  pageNum.value = p
  fetchList()
}

async function toggleFollow(item) {
  const next = !item.author.followed
  pendingIds.value = new Set([...pendingIds.value, item.author.userId])
  try {
    const res = next ? await followUser(item.author.userId) : await unfollowUser(item.author.userId)
    item.author.followed = res?.followed ?? next
    if (typeof res?.followerCount === 'number') counts.followers = res.followerCount
    if (typeof res?.followingCount === 'number') counts.following = res.followingCount
    // 关注列表内取关：本人视角的列表需刷新（对方不再被关注）
    if (isSelf.value && activeTab.value === 'following' && !item.author.followed) {
      fetchList()
    }
    ElMessage.success(next ? '已关注' : '已取消关注')
  } catch (e) {
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    const ids = new Set(pendingIds.value)
    ids.delete(item.author.userId)
    pendingIds.value = ids
  }
}

function goBack() {
  router.back()
}

// 点击列表项进入用户主页
function goProfile(userId) {
  if (userId) router.push(`/u/${userId}`)
}

watch(
  () => user.id,
  (id) => {
    if (id) fetchList()
  },
)

onMounted(async () => {
  await loadUser(true)
  fetchList()
  loadOtherCount()
})
</script>

<style scoped>
.follow-main {
  width: 100%;
  max-width: 760px;
  min-height: 100vh;
  border-right: 1px solid var(--color-border);
  padding-bottom: 80px;
}

.follow-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 16px;
  background: rgba(250, 248, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.follow-header__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text);
  cursor: pointer;
}

.follow-header__back:hover {
  background: var(--color-surface-hover);
}

.follow-header__title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.follow-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
}

.follow-tabs__item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color 0.2s;
}

.follow-tabs__item:hover {
  background: var(--color-surface-hover);
}

.follow-tabs__item.is-active {
  font-weight: 700;
  color: var(--color-text);
  box-shadow: inset 0 -4px 0 var(--color-primary);
}

.follow-tabs__num {
  font-size: 13px;
  color: var(--color-primary);
}

.follow-list {
  min-height: 240px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background-color 0.2s;
}

.follow-item:hover {
  background: var(--color-surface-low);
}

.follow-item__info {
  flex: 1;
  min-width: 0;
}

.follow-item__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-item__meta {
  margin-top: 2px;
  font-size: 13px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-item__btn {
  flex-shrink: 0;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  color: #fff;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.follow-item__btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.follow-item__btn.is-followed {
  color: var(--color-text-secondary);
  background: transparent;
  border-color: var(--color-border-strong);
}

.follow-item__btn.is-followed:hover:not(:disabled) {
  color: var(--color-like);
  border-color: var(--color-like);
}

.follow-item__btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.follow-item__self {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.follow-pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.follow-aside {
  display: none;
  width: 350px;
  flex-shrink: 0;
}

.follow-aside__sticky {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 100vh;
  overflow-y: auto;
  padding: 8px 0 16px 24px;
}

@media (min-width: 1024px) {
  .follow-aside {
    display: block;
  }
}
</style>
