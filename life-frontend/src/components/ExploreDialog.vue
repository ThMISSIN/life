<template>
  <Teleport to="body">
    <Transition name="explore-dialog">
      <div v-if="modelValue" class="explore-overlay" @click.self="close">
        <div class="explore-dialog" role="dialog" aria-label="选择权益类型">
          <div class="explore-dialog__head">
            <h3 class="explore-dialog__title">探索权益</h3>
            <button class="explore-dialog__close" title="关闭" @click="close">
              <span class="ms-icon">close</span>
            </button>
          </div>
          <p class="explore-dialog__sub">请选择你想浏览的权益类型</p>

          <div class="explore-dialog__options">
            <button class="explore-option" @click="choose('coupon')">
              <span class="explore-option__icon is-orange">
                <span class="ms-icon">confirmation_number</span>
              </span>
              <span class="explore-option__title">优惠券</span>
              <span class="explore-option__desc">餐饮、出行、娱乐等专属券码</span>
              <span class="explore-option__go">前往领券中心<span class="ms-icon">chevron_right</span></span>
            </button>

            <button class="explore-option" @click="choose('goods')">
              <span class="explore-option__icon is-blue">
                <span class="ms-icon">redeem</span>
              </span>
              <span class="explore-option__title">兑换商品</span>
              <span class="explore-option__desc">直充卡与实物小商品，支持筛选</span>
              <span class="explore-option__go">进入商品兑换<span class="ms-icon">chevron_right</span></span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
/**
 * 探索权益选择弹窗：用户选择浏览 优惠券 / 商品
 * （Teleport 到 body, 与发布弹窗同套自制弹窗风格）
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'select'])

function close() {
  emit('update:modelValue', false)
}

function choose(type) {
  emit('select', type)
  close()
}
</script>

<style scoped>
.explore-overlay {
  position: fixed;
  inset: 0;
  z-index: 2500;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(15, 20, 25, 0.5);
}

.explore-dialog {
  width: 100%;
  max-width: 460px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(15, 20, 25, 0.2);
  padding: 20px;
}

.explore-dialog__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.explore-dialog__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
}

.explore-dialog__close {
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

.explore-dialog__close:hover {
  background: var(--color-surface-hover);
  color: var(--color-text);
}

.explore-dialog__sub {
  margin: 6px 0 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.explore-dialog__options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.explore-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 16px;
  text-align: left;
  background: var(--color-surface-low);
  border: 1px solid var(--color-border-strong);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.explore-option:hover {
  border-color: var(--color-primary);
  background: rgba(29, 155, 240, 0.05);
  transform: translateY(-2px);
}

.explore-option__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #fff;
  margin-bottom: 4px;
}

.explore-option__icon .ms-icon {
  font-size: 24px;
}

.explore-option__icon.is-orange {
  background: linear-gradient(135deg, #ffb74d, #f57c00);
}

.explore-option__icon.is-blue {
  background: linear-gradient(135deg, #64b5f6, #1976d2);
}

.explore-option__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.explore-option__desc {
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.explore-option__go {
  display: flex;
  align-items: center;
  margin-top: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
}

.explore-option__go .ms-icon {
  font-size: 16px;
}

/* 过渡动画 */
.explore-dialog-enter-active,
.explore-dialog-leave-active {
  transition: opacity 0.2s ease;
}

.explore-dialog-enter-active .explore-dialog,
.explore-dialog-leave-active .explore-dialog {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.explore-dialog-enter-from,
.explore-dialog-leave-to {
  opacity: 0;
}

.explore-dialog-enter-from .explore-dialog,
.explore-dialog-leave-to .explore-dialog {
  transform: scale(0.96);
  opacity: 0;
}
</style>
