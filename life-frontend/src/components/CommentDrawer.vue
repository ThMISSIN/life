<template>
  <el-drawer
    :model-value="modelValue"
    title="评论"
    size="440px"
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
    @open="onOpen"
  >
    <div class="cdrawer">
      <!-- 帖子摘要 -->
      <div class="cdrawer__post">
        <span class="cdrawer__post-author">{{ post.author?.nickname || '' }}</span>
        <p class="cdrawer__post-content">{{ post.content }}</p>
      </div>

      <!-- 评论列表 -->
      <div v-loading="loading" class="cdrawer__list">
        <template v-if="comments.length">
          <CommentItem
            v-for="c in comments"
            :key="c.id"
            :comment="c"
            :post-author-id="post.author?.userId"
            @reply="startReply"
            @deleted="onCommentDeleted"
          />
          <el-button
            v-if="hasMore"
            class="cdrawer__more"
            text
            :loading="loadingMore"
            @click="loadMore"
          >
            加载更多评论
          </el-button>
        </template>
        <el-empty v-else-if="!loading" description="还没有评论，来抢沙发" :image-size="70" />
      </div>

      <!-- 回复提示条 -->
      <div v-if="replyTarget" class="cdrawer__replying">
        <span class="cdrawer__replying-text">
          回复 @{{ replyTarget.comment.author?.nickname || 'TA' }}
        </span>
        <button class="cdrawer__replying-cancel" @click="cancelReply">
          <span class="ms-icon">close</span>
        </button>
      </div>

      <!-- 发表评论 -->
      <div class="cdrawer__composer">
        <el-avatar :size="32" :src="user.avatar" class="cdrawer__composer-avatar" />
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          maxlength="200"
          resize="none"
          :placeholder="replyTarget ? `回复 @${replyTarget.comment.author?.nickname || 'TA'}：` : '发表评论…'"
          @keydown.enter.exact.prevent="onEnter"
        />
        <el-button
          type="primary"
          round
          class="cdrawer__send"
          :disabled="!draft.trim()"
          :loading="sending"
          @click="onSend"
        >
          发送
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
/**
 * 帖子评论抽屉（后端 CommentController /replies，无 Result 包装的裸接口）
 * - 顶级评论分页 + 内嵌二级回复（children 由后端随顶级评论一并返回）
 * - 回复二级评论时 parentId 仍挂其顶级评论, replyUserId 指向被回复人
 * - 删除权限（本人或帖子作者）由后端校验, 前端按可见性展示入口
 */
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CommentItem from './CommentItem.vue'
import { getCommentPage, publishComment } from '@/api/comment'
import { reportPostView } from '@/api/post'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  post: { type: Object, required: true },
})

const emit = defineEmits(['update:modelValue', 'count-change'])

const { user } = useUserStore()

const loading = ref(false)
const loadingMore = ref(false)
const comments = ref([])
const total = ref(0)
const page = ref(1)
const PAGE_SIZE = 10

const draft = ref('')
const sending = ref(false)
const replyTarget = ref(null) // { comment, rootId }

const hasMore = computed(() => comments.value.length < total.value)

async function loadPage(p) {
  const res = await getCommentPage(props.post.id, { pageNum: p, pageSize: PAGE_SIZE })
  const list = res?.list || []
  total.value = Number(res?.total || 0)
  if (p === 1) {
    comments.value = list
  } else {
    comments.value = comments.value.concat(list)
  }
}

function onOpen() {
  // 阅读上报(2.11 热度"阅"): 打开帖子即视为阅读, 会话内同帖去重, 静默失败
  reportPostView(props.post?.id)
  comments.value = []
  total.value = 0
  draft.value = ''
  replyTarget.value = null
  loading.value = true
  loadPage(1)
    .catch((e) => ElMessage.error(e.message || '评论加载失败'))
    .finally(() => {
      loading.value = false
    })
}

async function loadMore() {
  loadingMore.value = true
  try {
    await loadPage(page.value + 1)
    page.value += 1
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loadingMore.value = false
  }
}

// 回复入口: rootId 为该回复挂载的顶级评论 id（顶级评论本身则为自身 id）
function startReply({ comment, rootId }) {
  replyTarget.value = { comment, rootId }
}

function cancelReply() {
  replyTarget.value = null
}

function onEnter() {
  // Ctrl/Shift+Enter 换行由 prevent 之外的默认行为处理受限, 这里 Enter 直接发送
  if (draft.value.trim()) onSend()
}

async function onSend() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    const target = replyTarget.value
    await publishComment({
      postId: props.post.id,
      content,
      parentId: target ? target.rootId : 0,
      replyUserId: target ? target.comment.author?.userId : undefined,
    })
    draft.value = ''
    replyTarget.value = null
    ElMessage.success('评论成功')
    // 重新拉第一页, 把新评论（或新回复）刷出来
    await loadPage(1)
    emit('count-change', total.value)
  } catch (e) {
    ElMessage.error(e.message || '评论失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

function onCommentDeleted(deletedInfo) {
  // 从列表中移除（顶级或子级）; 后端为物理删除, 本地同步计数
  const { id, parentId } = deletedInfo
  if (parentId) {
    const parent = comments.value.find((c) => c.id === parentId)
    if (parent) parent.children = (parent.children || []).filter((c) => c.id !== id)
    total.value = Math.max(0, total.value - 1)
  } else {
    const removed = comments.value.find((c) => c.id === id)
    const childCount = removed?.children?.length || 0
    comments.value = comments.value.filter((c) => c.id !== id)
    total.value = Math.max(0, total.value - 1 - childCount)
  }
  emit('count-change', total.value)
}
</script>

<style scoped>
.cdrawer {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.cdrawer__post {
  padding: 12px;
  margin-bottom: 12px;
  background: var(--color-surface-low);
  border-radius: 10px;
}

.cdrawer__post-author {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
}

.cdrawer__post-content {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cdrawer__list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.cdrawer__more {
  display: flex;
  margin: 8px auto;
}

.cdrawer__replying {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(29, 155, 240, 0.06);
  border-radius: 8px;
}

.cdrawer__replying-cancel {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.cdrawer__replying-cancel:hover {
  background: var(--color-surface-hover);
}

.cdrawer__composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
  margin-top: 8px;
}

.cdrawer__composer-avatar {
  flex-shrink: 0;
}

.cdrawer__send {
  flex-shrink: 0;
}
</style>
