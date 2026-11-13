<template>
  <div class="container-shell">
    <AppSidebar />

    <!-- 中间信息流 -->
    <main class="home-main">
      <!-- 顶部 Tab -->
      <div class="home-tabs">
        <div
          v-for="tab in feedTabs"
          :key="tab.value"
          class="home-tabs__item"
          :class="{ 'is-active': activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- 发布器 -->
      <PublishBox @published="onPublished" />

      <!-- 信息流 -->
      <div v-loading="loading" class="home-feed">
        <div v-if="keyword" class="home-feed__search-tip">
          <span class="ms-icon">search</span>
          搜索「{{ keyword }}」的结果
          <a href="javascript:;" class="home-feed__search-clear" @click="clearSearch">清除 ×</a>
        </div>
        <PostCard v-for="post in posts" :key="post.id" :post="post" @deleted="onDeleted" />
        <el-empty v-if="!loading && posts.length === 0" :description="keyword ? '没有找到相关动态' : '暂无动态，快来发布第一条吧'" />
      </div>
    </main>

    <!-- 右侧小组件 -->
    <aside class="home-aside">
      <div class="home-aside__sticky">
        <SearchBar @search="onSearch" />

        <!-- 我的积分 -->
        <div class="card widget points-widget">
          <h2 class="widget-title">我的积分</h2>
          <div class="points-widget__value">
            <span class="points-widget__num">{{ formatNumber(pointsSummary.total) }}</span>
            <span class="points-widget__unit">分</span>
          </div>
          <a
            class="points-widget__link"
            href="javascript:;"
            @click="goPointsDetail"
          >
            查看积分明细及历史记录
          </a>
        </div>

        <!-- 积分榜前三 -->
        <div class="card leaderboard">
          <h2 class="widget-title leaderboard__title">积分榜前三</h2>
          <div
            v-for="item in leaderboard"
            :key="item.rank"
            class="leaderboard__item"
          >
            <div class="leaderboard__user">
              <span class="leaderboard__rank" :class="`leaderboard__rank--${item.rank}`">
                {{ item.rank }}
              </span>
              <el-avatar :size="40" :src="item.avatar" />
              <div class="leaderboard__info">
                <div class="leaderboard__name">
                  <span>{{ item.nickname }}</span>
                  <span
                    v-if="item.verified"
                    class="ms-icon ms-icon--fill leaderboard__verified"
                    :class="`is-${item.verifiedColor}`"
                  >
                    verified
                  </span>
                </div>
                <span class="leaderboard__username">{{ item.username }}</span>
              </div>
            </div>
            <div class="leaderboard__points">
              <span class="leaderboard__points-num">{{ item.pointsText }}</span>
              <span class="leaderboard__points-unit">分</span>
            </div>
          </div>
          <button class="leaderboard__more" @click="viewFullBoard">查看完整榜单</button>
        </div>

        <SideFooter />
      </div>
    </aside>
  </div>

  <!-- 积分完整榜单弹窗(1.6: GET /points/rank 分页 + /points/rank/me) -->
  <el-dialog v-model="boardVisible" width="440px" align-center append-to-body>
    <template #header>
      <div class="board-head">
        <span class="ms-icon ms-icon--fill board-head__trophy">emoji_events</span>
        <div>
          <h3 class="board-head__title">积分完整榜单</h3>
          <p class="board-head__sub">按本月获得积分排序，自然月切换自动重榜</p>
        </div>
      </div>
    </template>

    <div v-if="myRank" class="board-me">
      <span class="board-me__rank" :class="{ 'is-on': myRank.rank > 0 }">
        {{ myRank.rank > 0 ? `#${myRank.rank}` : '未上榜' }}
      </span>
      <el-avatar :size="34" :src="myRank.avatar" />
      <div class="board-me__info">
        <span class="board-me__label">我的本月积分</span>
        <span class="board-me__name">{{ myRank.nickname }}</span>
      </div>
      <div class="board-me__points">{{ myRank.points || 0 }} 分</div>
    </div>

    <div v-loading="boardLoading" class="board-list">
      <div
        v-for="item in boardList"
        :key="item.rank"
        class="board-row"
        :class="{ 'is-me': user && item.userId === user.id }"
      >
        <span class="board-row__rank" :class="`board-row__rank--${item.rank <= 3 ? item.rank : 'n'}`">
          {{ item.rank }}
        </span>
        <el-avatar :size="36" :src="item.avatar" />
        <div class="board-row__info">
          <span class="board-row__name">{{ item.nickname }}</span>
          <span class="board-row__username">@{{ item.username }}</span>
        </div>
        <div class="board-row__points">
          <b>{{ item.pointsText }}</b><i>分</i>
        </div>
      </div>
      <el-empty
        v-if="!boardLoading && !boardList.length"
        description="本月还没有人获得积分，去签到抢首榜！"
        :image-size="72"
      />
    </div>

    <template #footer>
      <div class="board-foot">
        <el-button
          v-if="!boardFinished && boardList.length"
          text
          :loading="boardLoading"
          @click="loadMoreBoard"
        >
          加载更多
        </el-button>
        <span v-else-if="boardList.length" class="board-foot__end">— 已到榜单末尾 —</span>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import PublishBox from '@/components/PublishBox.vue'
import PostCard from '@/components/PostCard.vue'
import SearchBar from '@/components/SearchBar.vue'
import SideFooter from '@/components/SideFooter.vue'
import { getFeedPosts } from '@/api/post'
import { getLeaderboard, getLeaderboardPage, getMyRank } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { user, loadUser } = useUserStore()

const feedTabs = [
  { label: '为你推荐', value: 'recommend' },
  { label: '正在关注', value: 'following' },
]

const activeTab = ref('recommend')
const posts = ref([])
const loading = ref(false)
const leaderboard = ref([])

const formatNumber = (n) => Number(n || 0).toLocaleString('en-US')

// 积分总览直接取自登录用户信息（后端 UserVO.points）
const pointsSummary = computed(() => ({ total: user.points || 0 }))

async function loadFeed() {
  loading.value = true
  try {
    posts.value = await getFeedPosts(activeTab.value, { keyword: keyword.value || undefined })
  } catch (e) {
    posts.value = []
    ElMessage.error(e.message || '加载动态失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 搜索: 后端 /post/page?keyword 命中帖子标题/内容; 空关键词回普通信息流
const keyword = ref('')
function onSearch(kw) {
  keyword.value = kw || ''
  loadFeed()
}
function clearSearch() {
  keyword.value = ''
  loadFeed()
}

function switchTab(value) {
  if (activeTab.value === value) return
  activeTab.value = value
  loadFeed()
}

// 发布成功后插入到信息流顶部
function onPublished(post) {
  posts.value.unshift(post)
}

// 删除动态后从信息流移除
function onDeleted(id) {
  posts.value = posts.value.filter((p) => p.id !== id)
}

function goPointsDetail() {
  router.push({ path: '/profile', query: { tab: 'points' } })
}

// ---- 积分完整榜单弹窗 ----
const boardVisible = ref(false)
const boardList = ref([])
const boardPage = ref(1)
const boardLoading = ref(false)
const boardFinished = ref(false)
const myRank = ref(null)
const BOARD_SIZE = 10

async function viewFullBoard() {
  boardVisible.value = true
  boardList.value = []
  boardPage.value = 1
  boardFinished.value = false
  myRank.value = null
  await Promise.all([loadBoardPage(), loadMyRank()])
}

async function loadBoardPage() {
  if (boardLoading.value || boardFinished.value) return
  boardLoading.value = true
  try {
    const rows = await getLeaderboardPage(boardPage.value, BOARD_SIZE)
    if (!rows.length) {
      boardFinished.value = true
    } else {
      boardList.value.push(...rows)
      if (rows.length < BOARD_SIZE) boardFinished.value = true
      else boardPage.value += 1
    }
  } catch (e) {
    ElMessage.error(e.message || '榜单加载失败')
    boardFinished.value = true
  } finally {
    boardLoading.value = false
  }
}

async function loadMyRank() {
  const r = await getMyRank()
  if (r) myRank.value = r
}

onMounted(async () => {
  loadUser()
  loadFeed()
  leaderboard.value = await getLeaderboard()
})
</script>

<style scoped>
.home-main {
  width: 100%;
  max-width: 760px;
  min-height: 100vh;
  border-right: 1px solid var(--color-border);
  padding-bottom: 80px;
}

.home-tabs {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  height: 53px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.home-tabs__item {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color 0.2s;
}

.home-tabs__item:hover {
  background: var(--color-surface-low);
}

.home-tabs__item.is-active {
  font-weight: 700;
  color: var(--color-text);
}

.home-tabs__item.is-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 56px;
  height: 4px;
  border-radius: 9999px;
  background: var(--color-primary);
}

.home-feed {
  min-height: 200px;
}

.home-feed__search-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--color-text-secondary, #64748b);
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
}
.home-feed__search-tip .ms-icon {
  font-size: 16px;
  color: var(--color-primary, #1d9bf0);
}
.home-feed__search-clear {
  margin-left: auto;
  color: var(--color-primary, #1d9bf0);
  text-decoration: none;
}

.home-aside {
  display: none;
  width: 350px;
  flex-shrink: 0;
  padding: 8px 0 0 24px;
}

.home-aside__sticky {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 100vh;
  overflow-y: auto;
  padding-bottom: 16px;
}

/* 我的积分 */
.points-widget {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.points-widget__value {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-top: 4px;
}

.points-widget__num {
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
  color: var(--color-text);
}

.points-widget__unit {
  font-weight: 500;
  color: var(--color-text-secondary);
  padding-bottom: 2px;
}

.points-widget__link {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-primary);
  margin-top: 4px;
}

/* 积分榜 */
.leaderboard {
  display: flex;
  flex-direction: column;
  padding-top: 12px;
  background: var(--color-surface-hover);
}

.leaderboard__title {
  padding: 0 16px;
  margin-bottom: 8px;
}

.leaderboard__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.leaderboard__item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.leaderboard__user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.leaderboard__rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.leaderboard__rank--1 {
  background: var(--color-gold);
}

.leaderboard__rank--2 {
  background: var(--color-silver);
}

.leaderboard__rank--3 {
  background: var(--color-bronze);
}

.leaderboard__info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.leaderboard__name {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.leaderboard__verified {
  font-size: 16px;
}

.leaderboard__verified.is-gold {
  color: var(--color-gold);
}

.leaderboard__verified.is-primary {
  color: var(--color-primary);
}

.leaderboard__username {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.leaderboard__points {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.leaderboard__points-num {
  font-weight: 700;
  color: var(--color-primary);
}

.leaderboard__points-unit {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.leaderboard__more {
  padding: 16px;
  border: none;
  background: transparent;
  text-align: left;
  font-size: 15px;
  font-family: inherit;
  color: var(--color-primary);
  cursor: pointer;
  border-radius: 0 0 16px 16px;
  transition: background-color 0.2s;
}

.leaderboard__more:hover {
  background: rgba(0, 0, 0, 0.04);
}

/* ---- 积分完整榜单弹窗 ---- */
.board-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.board-head__trophy {
  font-size: 28px;
  color: #f59e0b;
}
.board-head__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1f2937;
}
.board-head__sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: #9ca3af;
}
.board-me {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff7ed, #fef3c7);
  border: 1px solid #fde68a;
}
.board-me__rank {
  min-width: 46px;
  text-align: center;
  font-size: 14px;
  font-weight: 800;
  color: #92400e;
}
.board-me__rank:not(.is-on) {
  font-size: 12px;
  color: #b45309;
}
.board-me__info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.board-me__label {
  font-size: 11px;
  color: #b45309;
}
.board-me__name {
  font-size: 14px;
  font-weight: 600;
  color: #78350f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.board-me__points {
  font-size: 14px;
  font-weight: 700;
  color: #92400e;
}
.board-list {
  max-height: 340px;
  min-height: 120px;
  overflow-y: auto;
}
.board-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  border-radius: 10px;
}
.board-row.is-me {
  background: #eff6ff;
  outline: 1px solid #bfdbfe;
}
.board-row__rank {
  width: 26px;
  text-align: center;
  font-size: 14px;
  font-weight: 800;
  font-style: italic;
  color: #9ca3af;
}
.board-row__rank--1 { color: #f59e0b; }
.board-row__rank--2 { color: #94a3b8; }
.board-row__rank--3 { color: #b45309; }
.board-row__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.board-row__name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.board-row__username {
  font-size: 12px;
  color: #9ca3af;
}
.board-row__points b {
  font-size: 15px;
  font-weight: 800;
  color: #f97316;
}
.board-row__points i {
  font-size: 11px;
  font-style: normal;
  color: #9ca3af;
  margin-left: 2px;
}
.board-foot {
  text-align: center;
  min-height: 24px;
}
.board-foot__end {
  font-size: 12px;
  color: #c4c9d2;
}

@media (min-width: 1024px) {
  .home-aside {
    display: block;
  }
}
</style>
