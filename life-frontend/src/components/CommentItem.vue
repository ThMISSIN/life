<template>
  <div class="citem">
    <el-avatar
      :size="34"
      :src="comment.author?.avatar"
      class="citem__avatar"
      :class="{ 'is-link': comment.author?.userId }"
      @click.stop="goAuthorProfile"
    />

    <div class="citem__main">
      <div class="citem__head">
        <span
          class="citem__name"
          :class="{ 'is-link': comment.author?.userId }"
          @click.stop="goAuthorProfile"
        >
          {{ comment.author?.nickname || comment.author?.username || '用户' }}
        </span>
        <span class="citem__time">{{ comment.createTime }}</span>
      </div>

      <p class="citem__content">{{ comment.content }}</p>

      <div class="citem__ops">
        <button
          class="citem__op"
          :class="{ 'is-liked': comment.liked }"
          @click.stop="toggleLike"
        >
          <span class="ms-icon" :class="{ 'ms-icon--fill': comment.liked }">favorite</span>
          {{ comment.likeCount || '' }}
        </button>
        <button class="citem__op" @click.stop="onReply">
          <span class="ms-icon">chat_bubble</span>
          回复
        </button>
        <button
          v-if="canDelete"
          class="citem__op citem__op--danger"
          @click.stop="onDelete"
        >
          <span class="ms-icon">delete</span>
          删除
        </button>
      </div>

      <!-- 二级回复 -->
      <div v-if="children.length" class="citem__children">
        <CommentItem
          v-for="child in children"
          :key="child.id"
          :comment="child"
          :post-author-id="postAuthorId"
          :root-id="comment.id"
          @reply="(payload) => emit('reply', payload)"
          @deleted="(info) => emit('deleted', info)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 单条评论（含递归二级回复）
 * - 点赞走 LikeController（bizType=COMMENT），乐观更新失败回滚
 * - 删除权限：本人评论或帖子作者可见入口（后端最终校验）
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { toggleLike as toggleLikeApi } from '@/api/like'
import { deleteComment } from '@/api/comment'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  comment: { type: Object, required: true },
  // 帖子作者 id（其也可删除评论下入口）；顶级评论不需要 rootId
  postAuthorId: { type: [Number, String], default: null },
  rootId: { type: [Number, String], default: null },
})

const emit = defineEmits(['reply', 'deleted'])

const router = useRouter()
const { user } = useUserStore()

const children = computed(() => props.comment.children || [])

// 顶级评论的 rootId 是自身; 二级评论继承顶级评论 id, 回复时挂在同一根上
const effectiveRootId = computed(() => props.rootId ?? props.comment.id)

const canDelete = computed(() => {
  const mine = String(props.comment.author?.userId) === String(user.id)
  const postMine = String(props.postAuthorId || '') === String(user.id)
  return mine || postMine
})

async function toggleLike() {
  const next = !props.comment.liked
  props.comment.liked = next
  props.comment.likeCount = Math.max(0, (props.comment.likeCount || 0) + (next ? 1 : -1))
  try {
    await toggleLikeApi(props.comment.id, 'COMMENT', next)
  } catch (e) {
    props.comment.liked = !next
    props.comment.likeCount = Math.max(0, (props.comment.likeCount || 0) + (next ? -1 : 1))
    ElMessage.error(e.message || '操作失败')
  }
}

function onReply() {
  emit('reply', { comment: props.comment, rootId: effectiveRootId.value })
}

async function onDelete() {
  try {
    await ElMessageBox.confirm('删除这条评论吗？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteComment(props.comment.id)
    ElMessage.success('已删除')
    emit('deleted', { id: props.comment.id, parentId: props.rootId || null })
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

function goAuthorProfile() {
  const uid = props.comment.author?.userId
  if (!uid) return
  router.push(`/u/${uid}`)
}
</script>

<style scoped>
.citem {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.citem__avatar.is-link,
.citem__name.is-link {
  cursor: pointer;
}

.citem__avatar.is-link:hover,
.citem__name.is-link:hover {
  opacity: 0.85;
}

.citem__main {
  flex: 1;
  min-width: 0;
}

.citem__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.citem__name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
}

.citem__time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.citem__reply-to {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-primary);
}

.citem__content {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.citem__ops {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 6px;
}

.citem__op {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}

.citem__op .ms-icon {
  font-size: 14px;
}

.citem__op:hover {
  color: var(--color-primary);
}

.citem__op.is-liked {
  color: var(--color-like);
}

.citem__op--danger:hover {
  color: var(--color-danger);
}

.citem__children {
  margin-top: 8px;
  padding: 4px 0 4px 12px;
  border-left: 2px solid var(--color-border);
}

.citem__children .citem {
  padding: 8px 0;
  border-bottom: none;
}

.citem__children .citem + .citem {
  border-top: 1px solid var(--color-border);
}
</style>
