<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="profile-main">
      <!-- 吸顶头部 -->
      <div class="profile-header">
        <button class="profile-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <div>
          <h2 class="profile-header__name">{{ profile?.nickname || '用户主页' }}</h2>
          <p v-if="posts.length" class="profile-header__count">{{ formatNumber(posts.length) }} 条动态</p>
        </div>
      </div>

      <!-- 资料区 -->
      <div v-if="profile" class="profile-info">
        <div class="profile-info__cover" />
        <div class="profile-info__body">
          <div class="profile-info__top">
            <el-avatar :size="134" :src="profile.avatar" class="profile-info__avatar" />
            <el-button
              v-if="!isSelf"
              class="profile-info__edit"
              round
              :class="{ 'is-followed': profile.followed }"
              :loading="followPending"
              @click="toggleFollow"
            >
              {{ profile.followed ? '正在关注' : '关注' }}
            </el-button>
          </div>

          <div class="profile-info__name-row">
            <h1 class="profile-info__name">{{ profile.nickname }}</h1>
            <span v-if="profile.level > 1" class="profile-info__pro">
              <span class="ms-icon">star</span>
              Lv{{ profile.level }}
            </span>
          </div>
          <p class="profile-info__username">@{{ profile.username }}</p>

          <div class="profile-info__social">
            <span class="social-stat" @click="goFollow('following')">
              <strong>{{ formatNumber(profile.followingCount) }}</strong> 关注
            </span>
            <span class="social-stat" @click="goFollow('followers')">
              <strong>{{ formatNumber(profile.followerCount) }}</strong> 粉丝
            </span>
          </div>
        </div>
      </div>

      <!-- 动态列表 -->
      <div v-loading="loading" class="profile-panel">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          @deleted="onPostDeleted"
        />
        <el-empty v-if="!loading && posts.length === 0" description="TA还没有发布动态" />
      </div>
    </main>

    <!-- 右侧小组件 -->
    <aside class="profile-aside">
      <div class="profile-aside__sticky">
        <SearchBar />
        <SideFooter />
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import PostCard from '@/components/PostCard.vue'
import SearchBar from '@/components/SearchBar.vue'
import SideFooter from '@/components/SideFooter.vue'
import { getUserPosts } from '@/api/post'
import { getUserDetail } from '@/api/user'
import { followUser, unfollowUser } from '@/api/follow'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { user, loadUser } = useUserStore()

const profile = ref(null)
const posts = ref([])
const loading = ref(false)
const followPending = ref(false)

const targetUserId = computed(() => Number(route.params.userId))
const isSelf = computed(() => String(targetUserId.value) === String(user.id))

const formatNumber = (n) => Number(n || 0).toLocaleString('en-US')

async function loadPage() {
  if (!targetUserId.value) return
  loading.value = true
  try {
    const [detail, feed] = await Promise.all([
      getUserDetail(targetUserId.value),
      getUserPosts(targetUserId.value),
    ])
    profile.value = detail
    posts.value = feed
  } catch (e) {
    ElMessage.error(e.message || '加载用户主页失败')
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (followPending.value || !profile.value) return
  const next = !profile.value.followed
  followPending.value = true
  try {
    const res = next ? await followUser(profile.value.id) : await unfollowUser(profile.value.id)
    profile.value.followed = res?.followed ?? next
    if (typeof res?.followerCount === 'number') profile.value.followerCount = res.followerCount
    // 同步该用户动态卡片上的关注状态
    posts.value.forEach((p) => {
      if (p.author?.userId === profile.value.id) p.author.followed = profile.value.followed
    })
    ElMessage.success(next ? '已关注' : '已取消关注')
  } catch (e) {
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    followPending.value = false
  }
}

function onPostDeleted(id) {
  posts.value = posts.value.filter((p) => p.id !== id)
}

function goFollow(tab) {
  router.push({ path: '/follow', query: { tab, userId: targetUserId.value } })
}

function goBack() {
  router.back()
}

watch(
  () => route.params.userId,
  async () => {
    await loadUser()
    loadPage()
  },
  { immediate: true },
)
</script>

<style scoped>
.profile-main {
  width: 100%;
  max-width: 760px;
  min-height: 100vh;
  border-right: 1px solid var(--color-border);
  padding-bottom: 80px;
}

/* 吸顶头部 */
.profile-header {
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

.profile-header__back {
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
  transition: background-color 0.2s;
}

.profile-header__back:hover {
  background: var(--color-surface-hover);
}

.profile-header__name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.profile-header__count {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 资料区 */
.profile-info {
  border-bottom: 1px solid var(--color-border);
}

.profile-info__cover {
  height: 200px;
  background: linear-gradient(
    to right,
    rgba(29, 155, 240, 0.3),
    rgba(254, 107, 0, 0.2)
  );
}

.profile-info__body {
  padding: 0 16px 16px;
}

.profile-info__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.profile-info__avatar {
  margin-top: -67px;
  border: 4px solid var(--color-surface);
  background: var(--color-surface);
}

.profile-info__edit.el-button {
  margin-top: 12px;
  font-weight: 700;
  color: var(--color-text);
  border-color: var(--color-border-strong);
}

.profile-info__edit.el-button:hover {
  background: var(--color-surface-hover);
  border-color: var(--color-border-strong);
  color: var(--color-text);
}

.profile-info__edit.el-button.is-followed {
  color: #fff;
  background: var(--color-text);
  border-color: var(--color-text);
}

.profile-info__edit.el-button.is-followed:hover {
  color: var(--color-like);
  background: var(--color-surface);
  border-color: var(--color-like);
}

.profile-info__name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.profile-info__name {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  color: var(--color-text);
  line-height: 1.2;
}

.profile-info__pro {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 700;
  color: var(--color-green);
  background: var(--color-green-soft);
  border-radius: 4px;
}

.profile-info__pro .ms-icon {
  font-size: 10px;
}

.profile-info__username {
  margin: 0;
  font-size: 15px;
  color: var(--color-text-secondary);
}

.profile-info__social {
  display: flex;
  gap: 20px;
  margin-top: 12px;
}

.social-stat {
  font-size: 15px;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.social-stat:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.social-stat strong {
  font-weight: 900;
  color: var(--color-text);
}

/* 动态列表 */
.profile-panel {
  min-height: 200px;
}

/* 右侧栏 */
.profile-aside {
  display: none;
  width: 350px;
  flex-shrink: 0;
}

.profile-aside__sticky {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0 80px 24px;
}

@media (min-width: 1024px) {
  .profile-aside {
    display: block;
  }
}
</style>
