<template>
  <div class="container-shell">
    <AppSidebar />

    <!-- 中间主内容 -->
    <main class="profile-main">
      <!-- 吸顶头部 -->
      <div class="profile-header">
        <button class="profile-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <div>
          <h2 class="profile-header__name">{{ user.nickname }}</h2>
          <p class="profile-header__count">{{ formatNumber(myPosts.length) }} 动态</p>
        </div>
      </div>

      <!-- 资料区 -->
      <div class="profile-info">
        <!-- 封面 -->
        <div class="profile-info__cover" />
        <div class="profile-info__body">
          <div class="profile-info__top">
            <el-avatar :size="134" :src="user.avatar" class="profile-info__avatar" />
            <el-button class="profile-info__edit" round @click="openEdit">编辑资料</el-button>
          </div>

          <div class="profile-info__name-row">
            <h1 class="profile-info__name">{{ user.nickname }}</h1>
            <span class="profile-info__pro">
              <span class="ms-icon">star</span>
              {{ membership.currentLevel }}
            </span>
          </div>
          <p class="profile-info__username">{{ user.username }}</p>
          <p v-if="user.email || user.phone" class="profile-info__bio">
            {{ user.email || user.phone }}
          </p>

          <div v-if="user.lastLoginTime" class="profile-info__joined">
            <span class="ms-icon">calendar_month</span>
            <span>最近登录 {{ user.lastLoginTime }}</span>
          </div>

          <!-- 关注 / 粉丝 统计 -->
          <div class="profile-info__social">
            <span class="social-stat" @click="goFollow('following')">
              <strong>{{ formatNumber(social.following) }}</strong> 关注
            </span>
            <span class="social-stat" @click="goFollow('followers')">
              <strong>{{ formatNumber(social.followers) }}</strong> 粉丝
            </span>
          </div>

          <!-- 等级进度（依据积分实时推导） -->
          <div class="profile-level">
            <div class="profile-level__header">
              <span class="profile-level__label">{{ membership.currentLevel }}</span>
              <span class="profile-level__points">
                {{ formatNumber(user.points) }} / {{ formatNumber(membership.nextLevelPoints) }} 积分
              </span>
            </div>
            <div class="profile-level__bar">
              <div class="profile-level__bar-inner" :style="{ width: `${membership.progress}%` }" />
            </div>
            <div class="profile-level__badges">
              <span v-for="badge in badges" :key="badge.label" class="profile-badge">
                <span class="ms-icon" :class="`is-${badge.type}`">{{ badge.icon }}</span>
                {{ badge.label }}
              </span>
            </div>
          </div>
        </div>

        <!-- Tab 导航 -->
        <div class="profile-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.value"
            class="profile-tabs__item"
            :class="{ 'is-active': activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </div>
        </div>
      </div>

      <!-- Tab 面板：我的动态 -->
      <div v-show="activeTab === 'posts'" v-loading="loading" class="profile-panel">
        <PostCard v-for="post in myPosts" :key="post.id" :post="post" @deleted="onPostDeleted" />
        <el-empty v-if="!loading && myPosts.length === 0" description="还没有发布过动态" />
      </div>

      <!-- Tab 面板：签到 -->
      <div v-show="activeTab === 'checkin'" class="profile-panel profile-panel--card">
        <div class="checkin-summary">
          <div class="checkin-summary__main">
            <span class="checkin-summary__num">{{ checkinInfo.consecutiveDays }}</span>
            <span class="checkin-summary__label">连续签到（天）</span>
          </div>
          <el-divider />
          <ul class="checkin-summary__rules">
            <li><span class="ms-icon">check_circle</span>每日签到 +1 积分</li>
            <li><span class="ms-icon">local_fire_department</span>连续签到 7 / 14 / 28 天，额外 +10 / +20 / +40 积分</li>
          </ul>
          <p class="checkin-summary__tip">右侧月历可进行今日打卡</p>
        </div>
      </div>

      <!-- Tab 面板：积分 -->
      <div v-show="activeTab === 'points'" class="profile-panel profile-panel--card">
        <el-table :data="recordsFull" style="width: 100%">
          <el-table-column label="类型" width="170">
            <template #default="{ row }">
              <div class="record-type">
                <span class="ms-icon record-type__icon">{{ row.icon }}</span>
                {{ row.type }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column prop="remark" label="说明" min-width="160" />
          <el-table-column label="变动" width="110" align="right">
            <template #default="{ row }">
              <span class="record-change" :class="row.change > 0 ? 'is-earn' : 'is-spend'">
                {{ row.change > 0 ? `+${row.change}` : row.change }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </main>

    <!-- 右侧小组件 -->
    <aside class="profile-aside">
      <div class="profile-aside__sticky">
        <SearchBar />
        <CheckinCalendar
          :checked-dates="checkedDates"
          :checked-today="checkinInfo.checkedToday"
          @checked="onChecked"
        />
        <PointsDetailCard ref="pointsDetailRef" />
        <SideFooter />
      </div>
    </aside>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="460px">
      <el-form label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-edit">
            <el-avatar :size="64" :src="editForm.avatar" class="avatar-edit__preview" />
            <el-upload
              class="avatar-edit__upload"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/gif,image/webp"
              :http-request="onAvatarUpload"
            >
              <el-button size="small" :loading="avatarUploading">
                {{ avatarUploading ? '上传中…' : '上传头像' }}
              </el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="editForm.avatar" maxlength="255" placeholder="也可直接粘贴图片链接" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import PostCard from '@/components/PostCard.vue'
import SearchBar from '@/components/SearchBar.vue'
import SideFooter from '@/components/SideFooter.vue'
import CheckinCalendar from '@/components/CheckinCalendar.vue'
import PointsDetailCard from '@/components/PointsDetailCard.vue'
import { getUserPosts } from '@/api/post'
import {
  getMembership,
  getPointsRecordsFull,
  getSignRecords,
  updateCurrentUser,
} from '@/api/user'
import { uploadImage } from '@/api/upload'
import { getFollowersList, getFollowingList } from '@/api/follow'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { user, loadUser } = useUserStore()

const tabs = [
  { label: '我的动态', value: 'posts' },
  { label: '签到', value: 'checkin' },
  { label: '积分', value: 'points' },
]

const activeTab = ref('posts')
const loading = ref(false)
const myPosts = ref([])
const recordsFull = ref([])
const checkedDates = ref([])
const checkinInfo = ref({ consecutiveDays: 0, todayPoints: 10, checkedToday: false })
const social = ref({ following: 0, followers: 0 })
const pointsDetailRef = ref(null)

// 关注/粉丝计数（列表接口的 total 即数量，pageSize=1 省流量）
async function loadSocialCounts() {
  try {
    const [f, r] = await Promise.all([
      getFollowingList({ pageNum: 1, pageSize: 1 }),
      getFollowersList({ pageNum: 1, pageSize: 1 }),
    ])
    social.value = { following: Number(f?.total || 0), followers: Number(r?.total || 0) }
  } catch {
    // 未登录或服务异常时保持 0
  }
}

function goFollow(tab) {
  router.push({ path: '/follow', query: { tab } })
}

const formatNumber = (n) => Number(n || 0).toLocaleString('en-US')

// 会员等级信息（依据积分/等级实时计算，后端无独立接口）
const membership = computed(() => getMembership(user.points || 0))

// 资料区徽章（由真实账户数据推导）
const badges = computed(() => {
  const list = [
    { icon: 'stars', label: `${formatNumber(user.points)} 积分`, type: 'orange' },
    { icon: 'workspace_premium', label: membership.value.currentLevel, type: 'green' },
  ]
  if (checkedDates.value.length) {
    list.push({ icon: 'event_available', label: `本月签到 ${checkedDates.value.length} 天`, type: 'orange' })
  }
  return list
})

// 编辑资料（后端 PUT /user/user/update 支持 nickname/avatar/email/phone）
const editVisible = ref(false)
const savingProfile = ref(false)
const editForm = reactive({ nickname: '', avatar: '', phone: '', email: '' })

function openEdit() {
  editForm.nickname = user.nickname || ''
  editForm.avatar = user.avatar || ''
  editForm.phone = user.phone || ''
  editForm.email = user.email || ''
  editVisible.value = true
}

async function saveProfile() {
  if (savingProfile.value) return
  // 空字段置为 undefined，MyBatis-Plus 更新时跳过 null，避免误清空
  const payload = {
    nickname: editForm.nickname.trim() || undefined,
    avatar: editForm.avatar.trim() || undefined,
    phone: editForm.phone.trim() || undefined,
    email: editForm.email.trim() || undefined,
  }
  if (!payload.nickname && !payload.avatar && !payload.phone && !payload.email) {
    ElMessage.warning('没有需要保存的修改')
    return
  }
  savingProfile.value = true
  try {
    await updateCurrentUser(payload)
    Object.entries(payload).forEach(([key, value]) => {
      if (value !== undefined) user[key] = value
    })
    editVisible.value = false
    ElMessage.success('资料已更新')
  } catch (e) {
    ElMessage.error(e.message || '保存失败，请稍后重试')
  } finally {
    savingProfile.value = false
  }
}

// —— 头像上传（统一走 api/upload.js, 带 types/size 校验, 成功返回 OSS URL）——
const avatarUploading = ref(false)

async function onAvatarUpload({ file }) {
  avatarUploading.value = true
  try {
    const url = await uploadImage(file)
    editForm.avatar = url
    ElMessage.success('头像已上传，记得保存资料')
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败，请稍后重试')
  } finally {
    avatarUploading.value = false
  }
}

// 删除动态后从列表移除
function onPostDeleted(id) {
  myPosts.value = myPosts.value.filter((p) => p.id !== id)
}

// 签到成功：同步积分与明细
function onChecked(res) {
  if (res?.points) {
    user.points = (user.points || 0) + res.points
  }
  if (res?.signDays) {
    checkinInfo.value.consecutiveDays = res.signDays
  }
  pointsDetailRef.value?.load()
}

function goBack() {
  router.back()
}

// 支持从首页「查看积分明细」跳转定位到积分 Tab
watch(
  () => route.query.tab,
  (tab) => {
    if (tab && tabs.some((t) => t.value === tab)) {
      activeTab.value = tab
    }
  },
  { immediate: true },
)

// 加载签到记录
async function loadSignRecords() {
  try {
    const records = await getSignRecords()
    if (Array.isArray(records)) {
      const now = new Date()
      const year = now.getFullYear()
      const month = now.getMonth() + 1
      const daysInMonth = new Date(year, month, 0).getDate()
      // 后端契约: 数组索引+1 为日期, 元素值(0/1)为当天是否签到
      const marks = records.map((v) => Number(v) > 0)
      checkedDates.value = marks
        .map((on, i) => (on && i + 1 <= daysInMonth ? i + 1 : null))
        .filter(Boolean)
        .map((d) => `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`)

      // 连续签到天数: 今天未签则从昨天起往前数连续已签天数
      let idx = marks.length - 1
      if (!marks[idx]) idx -= 1
      let consecutive = 0
      while (idx >= 0 && marks[idx]) {
        consecutive += 1
        idx -= 1
      }
      checkinInfo.value.consecutiveDays = consecutive
      checkinInfo.value.checkedToday = checkedDates.value.includes(
        `${year}-${String(month).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`,
      )
    }
  } catch {
    // 签到记录加载失败静默处理
  }
}

onMounted(async () => {
  await loadUser(true)
  loading.value = true
  try {
    myPosts.value = await getUserPosts(user.id)
    recordsFull.value = await getPointsRecordsFull()
    await loadSignRecords()
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
  loadSocialCounts()
  pointsDetailRef.value?.load()
})
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

.profile-info__bio {
  margin: 12px 0 0;
  font-size: 15px;
  color: var(--color-text);
}

.profile-info__joined {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  font-size: 15px;
  color: var(--color-text-secondary);
}

.profile-info__joined .ms-icon {
  font-size: 18px;
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

/* 等级进度 */
.profile-level {
  margin-top: 16px;
  padding-top: 12px;
}

.profile-level__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 4px;
}

.profile-level__label {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.profile-level__points {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.profile-level__bar {
  height: 4px;
  background: var(--color-search-bg);
  border-radius: 9999px;
  overflow: hidden;
}

.profile-level__bar-inner {
  height: 100%;
  background: var(--color-primary);
  border-radius: 9999px;
}

.profile-level__badges {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.profile-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
  border: 1px solid rgba(194, 198, 217, 0.35);
  border-radius: 9999px;
}

.profile-badge .ms-icon {
  font-size: 16px;
}

.profile-badge .is-orange {
  color: var(--color-orange);
}

.profile-badge .is-green {
  color: var(--color-green);
}

/* Tabs */
.profile-tabs {
  display: flex;
  margin-top: 8px;
}

.profile-tabs__item {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 16px 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition:
    background-color 0.2s,
    color 0.2s;
}

.profile-tabs__item:hover {
  background: var(--color-surface-hover);
  color: var(--color-text);
}

.profile-tabs__item.is-active {
  font-weight: 700;
  color: var(--color-text);
  box-shadow: inset 0 -4px 0 var(--color-primary);
}

/* Tab 面板 */
.profile-panel {
  min-height: 200px;
}

.profile-panel--card {
  padding: 24px;
}

/* 头像编辑 */
.avatar-edit {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-edit__preview {
  background: var(--color-surface-low);
  flex-shrink: 0;
}

/* 签到 Tab */
.checkin-summary {
  padding: 24px;
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
  background: var(--color-surface);
}

.checkin-summary__main {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.checkin-summary__num {
  font-size: 48px;
  font-weight: 900;
  line-height: 1;
  color: var(--color-orange);
}

.checkin-summary__label {
  font-size: 15px;
  color: var(--color-text-secondary);
}

.checkin-summary__rules {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkin-summary__rules li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: var(--color-text);
}

.checkin-summary__rules .ms-icon {
  color: var(--color-green);
  font-size: 20px;
}

.checkin-summary__tip {
  margin: 16px 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.checkin-summary :deep(.el-divider--horizontal) {
  margin: 20px 0;
}

/* 积分 Tab 表格 */
.record-type {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text);
}

.record-type__icon {
  font-size: 18px;
  color: var(--color-primary);
}

.record-change {
  font-weight: 700;
}

.record-change.is-earn {
  color: var(--color-green);
}

.record-change.is-spend {
  color: var(--color-text);
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
