<template>
  <div class="img-uploader">
    <!-- 已选图片网格: 预览 / 上传中 / 删除 -->
    <div v-if="modelValue.length || uploadingCount > 0" class="img-uploader__grid">
      <div v-for="(item, i) in displayItems" :key="item.key" class="img-uploader__cell">
        <el-image :src="item.url" fit="cover" class="img-uploader__img">
          <template #placeholder>
            <div class="img-uploader__loading"><span class="ms-icon">downloading</span></div>
          </template>
          <template #error>
            <span class="ms-icon img-uploader__broken">image</span>
          </template>
        </el-image>
        <div v-if="item.uploading" class="img-uploader__mask">
          <span class="ms-icon img-uploader__spinner">progress_activity</span>
        </div>
        <button
          v-else
          class="img-uploader__remove"
          title="移除图片"
          @click.stop="removeAt(i)"
        >
          <span class="ms-icon">close</span>
        </button>
      </div>

      <!-- 追加按钮（未达上限时显示） -->
      <button
        v-if="modelValue.length + uploadingCount < limit"
        class="img-uploader__add"
        @click="open"
      >
        <span class="ms-icon">add_photo_alternate</span>
      </button>
    </div>

    <!-- 空态: 图片按钮 + 提示（hideEmpty 时由父级工具栏触发, 本组件只保留隐藏 input） -->
    <div v-if="!hideEmpty" class="img-uploader__empty">
      <button class="img-uploader__trigger" @click="open">
        <span class="ms-icon">add_photo_alternate</span>
        添加图片
      </button>
      <span class="img-uploader__hint">最多 {{ limit }} 张，单张 ≤5MB</span>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      multiple
      hidden
      @change="onFileChange"
    />
  </div>
</template>

<script setup>
/**
 * 图片上传组件（发布动态等场景共用）
 * 流程: 选图 → 逐张 POST /admin/common/upload → OSS URL 填入 modelValue
 * v-model 为 URL 数组; 上传期间通过 emit('uploading', bool) 通知父级禁用发布
 */
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/upload'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  limit: { type: Number, default: 9 },
  // 空态时隐藏"添加图片"行（由父级工具栏触发 open）, 隐藏 input 常驻保证可唤起
  hideEmpty: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'uploading'])

const fileInputRef = ref(null)
// 上传中的临时项（含 key 与 url 占位）
const pendingItems = ref([])

const uploadingCount = computed(() => pendingItems.value.length)
const emitUploading = () => emit('uploading', uploadingCount.value > 0)

// 展示列表: 已完成 URL + 上传中占位
const displayItems = computed(() => [
  ...props.modelValue.map((url) => ({ key: url, url, uploading: false })),
  ...pendingItems.value,
])

function open() {
  fileInputRef.value?.click()
}

async function onFileChange(e) {
  const files = [...(e.target.files || [])]
  e.target.value = '' // 允许重复选择同一文件
  if (!files.length) return

  const remain = props.limit - props.modelValue.length - pendingItems.value.length
  if (remain <= 0) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片`)
    return
  }
  if (files.length > remain) {
    ElMessage.warning(`最多还能选择 ${remain} 张图片`)
    files.length = remain
  }

  const jobs = files.map(async (file) => {
    const key = `pending-${Date.now()}-${Math.random().toString(36).slice(2)}`
    pendingItems.value.push({ key, url: '', uploading: true })
    emitUploading()
    try {
      const url = await uploadImage(file)
      emit('update:modelValue', [...props.modelValue, url])
    } catch (err) {
      ElMessage.error(err.message || '图片上传失败')
    } finally {
      pendingItems.value = pendingItems.value.filter((p) => p.key !== key)
      emitUploading()
    }
  })
  await Promise.all(jobs)
}

function removeAt(index) {
  const next = [...props.modelValue]
  next.splice(index, 1)
  emit('update:modelValue', next)
}

defineExpose({ open })
</script>

<style scoped>
.img-uploader__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 72px);
  gap: 8px;
}

.img-uploader__cell {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--color-surface-low);
}

.img-uploader__img {
  width: 100%;
  height: 100%;
  display: block;
}

.img-uploader__loading,
.img-uploader__broken {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--color-text-muted);
}

.img-uploader__mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.65);
}

.img-uploader__spinner {
  font-size: 22px;
  color: var(--color-primary);
  animation: img-uploader-spin 1s linear infinite;
}

@keyframes img-uploader-spin {
  to {
    transform: rotate(360deg);
  }
}

.img-uploader__remove {
  position: absolute;
  top: 4px;
  right: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: rgba(15, 20, 25, 0.6);
  color: #fff;
  cursor: pointer;
}

.img-uploader__remove .ms-icon {
  font-size: 13px;
}

.img-uploader__remove:hover {
  background: var(--color-danger);
}

.img-uploader__add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 1px dashed var(--color-border-strong);
  border-radius: 10px;
  background: var(--color-surface-low);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.img-uploader__add:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.img-uploader__add .ms-icon {
  font-size: 24px;
}

.img-uploader__empty {
  display: flex;
  align-items: center;
  gap: 10px;
}

.img-uploader__trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  font-size: 13px;
  color: var(--color-text);
  background: var(--color-surface-low);
  border: 1px solid var(--color-border-strong);
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.img-uploader__trigger:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.img-uploader__trigger .ms-icon {
  font-size: 18px;
}

.img-uploader__hint {
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
