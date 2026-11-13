<template>
  <article class="post">
    <el-avatar
      :size="40"
      :src="post.author.avatar"
      class="post__avatar"
      :class="{ 'is-link': post.author?.userId }"
      @click.stop="goAuthorProfile"
    />

    <div class="post__main">
      <!-- 头部：作者信息 -->
      <div class="post__header">
        <div class="post__author">
          <span
            class="post__name"
            :class="{ 'is-link': post.author?.userId }"
            @click.stop="goAuthorProfile"
          >{{ post.author.nickname }}</span>
          <span
            v-if="post.author.verified"
            class="ms-icon ms-icon--fill post__verified"
          >
            verified
          </span>
          <span v-if="post.author.username" class="post__meta">{{ post.author.username }}</span>
          <span v-if="post.time" class="post__meta">·</span>
          <span v-if="post.time" class="post__meta post__time">{{ post.time }}</span>
        </div>
        <div class="post__header-actions">
          <button
            v-if="showFollowBtn"
            class="post__follow"
            :class="{ 'is-followed': post.author.followed }"
            :disabled="followPending"
            @click.stop="toggleFollow"
          >
            {{ post.author.followed ? '正在关注' : '关注' }}
          </button>
          <el-dropdown v-if="!isMine" trigger="click" @command="onCommand">
            <button class="post__more" @click.stop>
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="mute">不感兴趣</el-dropdown-item>
                <el-dropdown-item command="report" divided>举报</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown v-else trigger="click" @command="onCommand">
            <button class="post__more" @click.stop>
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="delete">删除动态</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 正文 -->
      <p class="post__content">{{ post.content }}</p>

      <!-- 话题标签 -->
      <p v-if="post.hashtags?.length" class="post__tags">
        <a v-for="tag in post.hashtags" :key="tag" href="javascript:;" @click.prevent>
          #{{ tag }}
        </a>
      </p>

      <!-- 图片（可点击预览） -->
      <div v-if="post.images?.length" class="post__images" :class="`post__images--${post.images.length}`">
        <el-image
          v-for="(img, i) in post.images"
          :key="i"
          :src="img"
          :preview-src-list="post.images"
          :initial-index="i"
          fit="cover"
          preview-teleported
          loading="lazy"
        />
      </div>

      <!-- 内嵌权益卡片 -->
      <div v-if="post.benefit" class="post__benefit">
        <span class="ms-icon post__benefit-icon">{{ post.benefit.icon }}</span>
        <div class="post__benefit-info">
          <div class="post__benefit-title">{{ post.benefit.title }}</div>
          <div class="post__benefit-condition">{{ post.benefit.condition }}</div>
        </div>
        <span class="post__benefit-status">
          {{ post.benefit.status === 'claimed' ? '已领取' : '可领取' }}
        </span>
      </div>

      <!-- 互动栏 -->
      <div class="post__actions">
        <button class="action action--comment" @click.stop="openComments">
          <span class="action__btn"><span class="ms-icon action__icon">chat_bubble</span></span>
          <span class="action__count">{{ post.actions.comments }}</span>
        </button>
        <button class="action action--repost" :class="{ 'is-on': reposted }" @click.stop="toggleRepost">
          <span class="action__btn"><span class="ms-icon action__icon">repeat</span></span>
          <span class="action__count">{{ post.actions.reposts }}</span>
        </button>
        <button class="action action--like" :class="{ 'is-on': liked }" @click.stop="toggleLike">
          <span class="action__btn">
            <span class="ms-icon action__icon" :class="{ 'ms-icon--fill': liked }">favorite</span>
          </span>
          <span class="action__count">{{ post.actions.likes }}</span>
        </button>
        <button class="action action--comment">
          <span class="action__btn"><span class="ms-icon action__icon">bar_chart</span></span>
          <span class="action__count">{{ post.actions.views }}</span>
        </button>
        <div class="post__actions-extra">
          <button class="action" @click.stop="bookmarkIt">
            <span class="action__btn"><span class="ms-icon action__icon" :class="{ 'ms-icon--fill': favorited, 'is-faved': favorited }">bookmark</span></span>
          </button>
          <button class="action" @click.stop="shareIt">
            <span class="action__btn"><span class="ms-icon action__icon">ios_share</span></span>
          </button>
        </div>
      </div>
    </div>

    <!-- 评论抽屉 -->
    <CommentDrawer v-model="commentVisible" :post="post" @count-change="onCommentCountChange" />
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommentDrawer from './CommentDrawer.vue'
import { toggleLike as toggleLikeApi } from '@/api/like'
import { followUser, unfollowUser } from '@/api/follow'
import { deletePost, repostPost } from '@/api/post'
import { toggleFavorite } from '@/api/favorite'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const props = defineProps({
  post: { type: Object, required: true },
})

const emit = defineEmits(['deleted', 'unfollowed'])

const { user } = useUserStore()

const liked = ref(props.post.liked || false)
const reposted = ref(false)
const favorited = ref(props.post.favorited || false)
const likePending = ref(false)
const favPending = ref(false)
const repostPending = ref(false)
const followPending = ref(false)
const commentVisible = ref(false)

// 评论抽屉: 后端真实帖子可拉取评论
function openComments() {
  if (!isRemote.value) {
    ElMessage.info('演示数据不支持评论')
    return
  }
  commentVisible.value = true
}

// 抽屉内发布/删除评论后同步卡片计数
function onCommentCountChange(total) {
  props.post.actions.comments = total
}

// 后端真实帖子带 remote 标记，且 id 为数字，可参与点赞/删除
const isRemote = computed(() => props.post.remote === true && Number.isFinite(Number(props.post.id)))
// 关注按钮：远程帖、能拿到作者 userId、且不是本人
const showFollowBtn = computed(
  () => isRemote.value && props.post.author?.userId && String(props.post.author.userId) !== String(user.id),
)
// 删除按钮：远程帖且是本人作者
const isMine = computed(() => isRemote.value && String(props.post.author?.userId) === String(user.id))

async function toggleLike() {
  const next = !liked.value
  // 乐观更新
  liked.value = next
  adjustLikeCount(next ? 1 : -1)
  if (!isRemote.value) return
  if (likePending.value) return
  likePending.value = true
  try {
    await toggleLikeApi(props.post.id, 'POST', next)
  } catch (e) {
    // 回滚
    liked.value = !next
    adjustLikeCount(next ? -1 : 1)
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    likePending.value = false
  }
}

function adjustLikeCount(delta) {
  const cur = Number(props.post.actions.likes) || 0
  props.post.actions.likes = Math.max(0, cur + delta)
}

async function toggleFollow() {
  const targetId = props.post.author.userId
  const next = !props.post.author.followed
  if (followPending.value) return
  followPending.value = true
  try {
    const res = next ? await followUser(targetId) : await unfollowUser(targetId)
    props.post.author.followed = res?.followed ?? next
    if (!props.post.author.followed) emit('unfollowed', targetId)
  } catch (e) {
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    followPending.value = false
  }
}

async function onCommand(command) {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('删除后不可恢复，确定删除这条动态吗？', '删除动态', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
    try {
      await deletePost(props.post.id)
      ElMessage.success('已删除')
      emit('deleted', props.post.id)
    } catch (e) {
      ElMessage.error(e.message || '删除失败，请稍后重试')
    }
  } else if (command === 'report') {
    ElMessage.info('举报（待接入后端）')
  } else if (command === 'mute') {
    ElMessage.success('已减少此类内容（演示）')
  }
}

async function toggleRepost() {
  // 演示卡片保持本地假动作
  if (!isRemote.value) {
    reposted.value = !reposted.value
    return
  }
  if (repostPending.value) return
  // 一期: 附言可空的轻转发(生成转发帖+源帖计数+1)
  let remark = ''
  try {
    const r = await ElMessageBox.prompt('转发附言（可留空）', '转发动态', {
      confirmButtonText: '转发',
      cancelButtonText: '取消',
      inputPlaceholder: '说点什么…',
    })
    remark = r.value || ''
  } catch {
    return
  }
  repostPending.value = true
  try {
    await repostPost(props.post.id, remark)
    reposted.value = true
    adjustRepostCount(1)
    ElMessage.success('已转发')
  } catch (e) {
    ElMessage.error(e.message || '转发失败，请稍后重试')
  } finally {
    repostPending.value = false
  }
}

function adjustRepostCount(delta) {
  const cur = Number(props.post.actions.reposts) || 0
  props.post.actions.reposts = Math.max(0, cur + delta)
}

// 跳转作者主页(仅后端真实帖子有 userId)
function goAuthorProfile() {
  const uid = props.post.author?.userId
  if (!uid) return
  router.push(`/u/${uid}`)
}

async function bookmarkIt() {
  if (!isRemote.value) {
    ElMessage.success('已收藏（演示）')
    return
  }
  if (favPending.value) return
  const next = !favorited.value
  // 乐观更新, 失败回滚
  favorited.value = next
  favPending.value = true
  try {
    const favoritedNow = await toggleFavorite(1, props.post.id)
    favorited.value = Boolean(favoritedNow)
    ElMessage.success(favorited.value ? '已收藏' : '已取消收藏')
  } catch (e) {
    favorited.value = !next
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    favPending.value = false
  }
}

// 分享: 复制正文+作者主页链接(剪贴板 API, http 环境降级 execCommand)
async function shareIt() {
  const text = `${props.post.content || ''}\n${location.origin}/u/${props.post.author?.userId || ''}`
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    ElMessage.success('内容已复制，去分享吧')
  } catch {
    ElMessage.error('复制失败，请手动选择内容复制')
  }
}
</script>

<style scoped>
.post {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background-color 0.2s;
}

.post:hover {
  background: rgba(242, 243, 255, 0.3);
}

.post__avatar {
  flex-shrink: 0;
}

.post__avatar.is-link,
.post__name.is-link {
  cursor: pointer;
}

.post__main {
  flex: 1;
  min-width: 0;
}

.post__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.post__author {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  min-width: 0;
}

.post__name {
  font-weight: 700;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post__name:hover {
  text-decoration: underline;
}

.post__verified {
  font-size: 16px;
  color: var(--color-primary);
  flex-shrink: 0;
}

.post__meta {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.post__time:hover {
  text-decoration: underline;
}

.post__more {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition:
    color 0.2s,
    background-color 0.2s;
}

.post__more:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.post__header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.post__follow {
  padding: 4px 14px;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  color: var(--color-primary);
  background: transparent;
  border: 1px solid var(--color-primary);
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.post__follow:hover:not(:disabled) {
  background: var(--color-primary-soft);
}

.post__follow.is-followed {
  color: var(--color-text-secondary);
  border-color: var(--color-border-strong);
}

.post__follow.is-followed:hover:not(:disabled) {
  color: var(--color-like);
  border-color: var(--color-like);
  background: rgba(249, 24, 128, 0.06);
}

.post__follow:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.post__content {
  margin: 4px 0 12px;
  font-size: 15px;
  line-height: 1.5;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.post__tags {
  margin: -6px 0 12px;
  display: flex;
  gap: 12px;
}

.post__tags a {
  color: var(--color-primary);
}

.post__images {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 12px;
}

.post__images .el-image {
  width: 100%;
  height: 192px;
  display: block;
}

.post__images--1 {
  grid-template-columns: 1fr;
}

.post__images--1 .el-image {
  height: auto;
  max-height: 340px;
}

.post__benefit {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  margin-bottom: 4px;
  transition: background-color 0.2s;
}

.post__benefit:hover {
  background: var(--color-surface-low);
}

.post__benefit-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  font-size: 24px;
  color: var(--color-orange);
  background: rgba(254, 107, 0, 0.1);
  border-radius: 8px;
  flex-shrink: 0;
}

.post__benefit-info {
  flex: 1;
  min-width: 0;
}

.post__benefit-title {
  font-weight: 700;
  font-size: 15px;
  color: var(--color-text);
}

.post__benefit-condition {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.post__benefit-status {
  font-weight: 700;
  font-size: 13px;
  color: var(--color-orange);
  padding-right: 12px;
  flex-shrink: 0;
}

.post__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 425px;
  margin-top: 10px;
  color: var(--color-text-secondary);
}

.post__actions-extra {
  display: flex;
}

.action {
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  color: inherit;
}

.action__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  transition:
    background-color 0.2s,
    color 0.2s;
}

.action__icon {
  font-size: 18px;
}

.action__count {
  font-size: 13px;
  padding-right: 6px;
}

.action--comment:hover,
.action--comment:hover .action__icon {
  color: var(--color-primary);
}

.action--comment:hover .action__btn {
  background: var(--color-primary-soft);
}

.action--repost:hover,
.action--repost:hover .action__icon,
.action--repost.is-on {
  color: var(--color-repost);
}

.action--repost:hover .action__btn,
.action--repost.is-on .action__btn {
  background: rgba(0, 186, 124, 0.1);
}

.action--like:hover,
.action--like:hover .action__icon,
.action--like.is-on {
  color: var(--color-like);
}

.action--like:hover .action__btn,
.action--like.is-on .action__btn {
  background: rgba(249, 24, 128, 0.1);
}
</style>
