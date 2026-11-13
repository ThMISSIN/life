<template>
  <div class="publish-box">
    <el-avatar :size="40" :src="user.avatar" />
    <div class="publish-box__main">
      <el-input
        v-model="content"
        type="textarea"
        class="publish-box__input"
        placeholder="有什么新鲜事？！"
        :autosize="{ minRows: 1, maxRows: 6 }"
        resize="none"
        @keydown.ctrl.enter="handlePublish"
      />

      <!-- 图片上传与预览（空态时由工具栏图片按钮触发选图） -->
      <ImageUploader
        v-if="images.length || uploading"
        ref="uploaderRef"
        v-model="images"
        :limit="9"
        @uploading="uploading = $event"
      />
      <!-- 空态常驻隐藏 input, 供工具栏唤起 -->
      <ImageUploader
        v-else
        ref="uploaderRef"
        v-model="images"
        :limit="9"
        hide-empty
        @uploading="uploading = $event"
      />

      <div class="publish-box__toolbar">
        <div class="publish-box__tools">
          <span
            v-for="tool in tools"
            :key="tool.icon"
            :title="tool.label"
            class="ms-icon publish-box__tool"
            @click="onToolClick(tool)"
          >
            {{ tool.icon }}
          </span>
        </div>
        <div class="publish-box__actions">
          <el-button
            class="publish-box__draft"
            text
            :disabled="!content.trim() || pending"
            @click="handleSaveDraft"
          >
            存草稿
          </el-button>
          <el-button
            class="publish-box__submit"
            round
            :disabled="!content.trim() || pending || uploading"
            :loading="pending"
            @click="handlePublish"
          >
            发布
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ImageUploader from '@/components/ImageUploader.vue'
import { publishPost, getDraft, saveDraft } from '@/api/post'
import { useUserStore } from '@/stores/user'

const emit = defineEmits(['published'])

const { user } = useUserStore()

const tools = [
  { icon: 'image', label: '图片', action: 'image' },
  { icon: 'gif_box', label: 'GIF' },
  { icon: 'ballot', label: '投票' },
  { icon: 'mood', label: '表情' },
  { icon: 'event', label: '日程' },
  { icon: 'location_on', label: '位置' },
]

const content = ref('')
const pending = ref(false)
const images = ref([])
const uploading = ref(false)
const uploaderRef = ref(null)

// 覆盖 el-input 默认内边距，贴近设计稿的大号输入框
// 多行 textarea: 长文本自动换行, 行高 32px, autosize 控制在 1~6 行
const inputStyle = { fontSize: '20px', lineHeight: '32px', padding: '6px 0' }

// 打开页面时恢复上次未发布的草稿（Redis 存储，仅回填编辑器，无草稿 id）
onMounted(async () => {
  try {
    const draft = await getDraft()
    if (draft?.content) {
      content.value = draft.content
      ElMessage.info('已恢复上次未发布的草稿')
    }
  } catch {
    // 无 token / 服务异常时静默，不影响正常发布
  }
})

// 发布动态（成功后后端自动清理草稿）
async function handlePublish() {
  const text = content.value.trim()
  if (!text || pending.value || uploading.value) return
  if (uploading.value) {
    ElMessage.warning('图片正在上传，请稍候')
    return
  }
  pending.value = true
  try {
    const post = await publishPost({ content: text, images: images.value })
    emit('published', post)
    content.value = ''
    images.value = []
    ElMessage.success('发布成功')
  } catch (e) {
    ElMessage.error(e.message || '发布失败，请稍后重试')
  } finally {
    pending.value = false
  }
}

// 保存草稿（一人一份，覆盖式；内容为空时后端视为清除草稿）
async function handleSaveDraft() {
  const text = content.value.trim()
  if (!text || pending.value) return
  pending.value = true
  try {
    await saveDraft({ content: text })
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.error(e.message || '草稿保存失败，请稍后重试')
  } finally {
    pending.value = false
  }
}

function notifyTool(label) {
  ElMessage.info(`${label}功能（待接入后端）`)
}

// 工具栏: 图片按钮触发选图上传, 其余功能提示待接入
function onToolClick(tool) {
  if (tool.action === 'image') {
    if (images.value.length >= 9 || uploading.value) return
    uploaderRef.value?.open()
    return
  }
  notifyTool(tool.label)
}
</script>

<style scoped>
.publish-box {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
}

.publish-box__main {
  flex: 1;
  min-width: 0;
}

.publish-box__input :deep(.el-textarea__inner) {
  box-shadow: none !important;
  background: transparent;
  font-weight: 400;
  resize: none;
}

.publish-box__input :deep(.el-textarea__inner::placeholder) {
  color: var(--color-text-secondary);
}

.publish-box__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(239, 243, 244, 0.6);
}

.publish-box__tools {
  display: flex;
  align-items: center;
  min-width: 0;
}

.publish-box__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.publish-box__draft.el-button {
  color: var(--color-text-secondary);
  font-weight: 600;
}

.publish-box__draft.el-button:not(:disabled):hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.publish-box__tool {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 20px;
  color: var(--color-primary);
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.2s;
}

.publish-box__tool:hover {
  background: var(--color-primary-soft);
}

.publish-box__submit.el-button {
  background: var(--color-primary);
  border: none;
  color: #fff;
  font-weight: 700;
  /* 左右加宽, 避免「发布」二字贴住胶囊圆角边缘 */
  padding: 8px 24px;
}

.publish-box__submit.el-button:not(:disabled):hover {
  background: var(--color-primary-hover);
  color: #fff;
}

.publish-box__submit.el-button.is-disabled {
  background: var(--color-primary);
  color: rgba(255, 255, 255, 0.6);
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
