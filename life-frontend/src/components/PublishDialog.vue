<template>
  <Teleport to="body">
    <Transition name="publish-dialog">
      <div v-if="modelValue" class="pub-dialog-overlay" @click.self="close">
        <div class="pub-dialog" role="dialog" aria-label="发布动态">
          <!-- 头部 -->
          <div class="pub-dialog__head">
            <h3 class="pub-dialog__title">发布动态</h3>
            <button class="pub-dialog__close" title="关闭" @click="close">
              <span class="ms-icon">close</span>
            </button>
          </div>

          <!-- 正文 -->
          <div class="pub-dialog__body">
            <div class="pub-dialog__author">
              <el-avatar :size="38" :src="user.avatar" />
              <div class="pub-dialog__author-info">
                <span class="pub-dialog__author-name">{{ user.nickname || user.username }}</span>
                <span class="pub-dialog__author-sub">@{{ user.username }}</span>
              </div>
            </div>

            <el-input
              v-model="content"
              type="textarea"
              class="pub-dialog__input"
              placeholder="有什么新鲜事？！"
              :autosize="{ minRows: 4, maxRows: 10 }"
              maxlength="280"
              show-word-limit
              resize="none"
            />

            <ImageUploader v-model="images" :limit="9" @uploading="uploading = $event" />
          </div>

          <!-- 底部 -->
          <div class="pub-dialog__foot">
            <span class="pub-dialog__count">{{ content.length }} / 280</span>
            <div class="pub-dialog__actions">
              <el-button round @click="close">取消</el-button>
              <el-button
                type="primary"
                round
                :disabled="!content.trim() || uploading"
                :loading="pending"
                @click="handlePublish"
              >
                发布
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
/**
 * 发布动态弹窗（Teleport 到 body, 脱离侧边栏层叠上下文）
 * 样式与项目卡片语言统一: 白底圆角卡片 + 遮罩淡入
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ImageUploader from '@/components/ImageUploader.vue'
import { publishPost } from '@/api/post'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'published'])

const { user } = useUserStore()

const content = ref('')
const images = ref([])
const uploading = ref(false)
const pending = ref(false)

// 打开时重置内容
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      content.value = ''
      images.value = []
      uploading.value = false
      // Esc 关闭
      document.addEventListener('keydown', onKeydown)
    } else {
      document.removeEventListener('keydown', onKeydown)
    }
  },
)

function onKeydown(e) {
  if (e.key === 'Escape') close()
}

function close() {
  if (pending.value) return
  emit('update:modelValue', false)
}

async function handlePublish() {
  const text = content.value.trim()
  if (!text || pending.value || uploading.value) return
  pending.value = true
  try {
    const post = await publishPost({ content: text, images: images.value })
    ElMessage.success('发布成功')
    emit('update:modelValue', false)
    emit('published', post)
  } catch (e) {
    ElMessage.error(e.message || '发布失败，请稍后重试')
  } finally {
    pending.value = false
  }
}
</script>

<style scoped>
.pub-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 2500;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 8vh 16px 16px;
  background: rgba(15, 20, 25, 0.5);
  overflow-y: auto;
}

.pub-dialog {
  width: 100%;
  max-width: 540px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(15, 20, 25, 0.2);
  overflow: hidden;
}

/* 头部 */
.pub-dialog__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.pub-dialog__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
}

.pub-dialog__close {
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
  transition: all 0.2s;
}

.pub-dialog__close:hover {
  background: var(--color-surface-hover);
  color: var(--color-text);
}

.pub-dialog__close .ms-icon {
  font-size: 20px;
}

/* 正文 */
.pub-dialog__body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pub-dialog__author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pub-dialog__author-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.pub-dialog__author-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
}

.pub-dialog__author-sub {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.pub-dialog__input :deep(.el-textarea__inner) {
  background: var(--color-surface-low);
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 15px;
  line-height: 1.6;
}

/* 底部 */
.pub-dialog__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px 16px;
  border-top: 1px solid var(--color-border);
}

.pub-dialog__count {
  font-size: 12px;
  color: var(--color-text-muted);
}

.pub-dialog__actions {
  display: flex;
  gap: 10px;
}

/* 过渡动画 */
.publish-dialog-enter-active,
.publish-dialog-leave-active {
  transition: opacity 0.2s ease;
}

.publish-dialog-enter-active .pub-dialog,
.publish-dialog-leave-active .pub-dialog {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.publish-dialog-enter-from,
.publish-dialog-leave-to {
  opacity: 0;
}

.publish-dialog-enter-from .pub-dialog,
.publish-dialog-leave-to .pub-dialog {
  transform: translateY(-16px);
  opacity: 0;
}
</style>
