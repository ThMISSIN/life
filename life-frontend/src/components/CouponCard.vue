<template>
  <div class="coupon" :class="`coupon--${coupon.theme}`">
    <div class="coupon__glow" />
    <div class="coupon__top">
      <span class="ms-icon ms-icon--fill coupon__icon">{{ coupon.icon }}</span>
      <span class="coupon__value">{{ coupon.value }}</span>
    </div>
    <h3 class="coupon__title">{{ coupon.title }}</h3>
    <p class="coupon__desc">{{ coupon.desc }}</p>

    <!-- 虚线分割 + 打孔效果 -->
    <div class="coupon__divider" />
    <div class="coupon__footer">
      <span class="coupon__expire">{{ coupon.claimed ? '已放入卡包' : coupon.expireText }}</span>
      <el-button
        v-if="coupon.points != null"
        class="coupon__btn coupon__btn--points"
        size="small"
        :disabled="coupon.claimed"
        @click="handleClaim"
      >
        <span class="ms-icon ms-icon--fill coupon__btn-icon">stars</span>
        {{ coupon.points }} 兑换
      </el-button>
      <el-button v-else class="coupon__btn" size="small" :disabled="coupon.claimed" @click="handleClaim">
        {{ coupon.claimed ? '已领取' : '领取' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  coupon: { type: Object, required: true },
})

const emit = defineEmits(['select'])

// 点击「积分兑换」跳转权益详情, 完整兑换流程在详情页完成
function handleClaim() {
  if (props.coupon.claimed) return
  emit('select', props.coupon.id)
}
</script>

<style scoped>
.coupon {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 12px;
  overflow: hidden;
  transition:
    border-color 0.2s,
    transform 0.2s,
    box-shadow 0.2s;
}

.coupon:hover {
  border-color: rgba(29, 155, 240, 0.5);
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(15, 20, 25, 0.06);
}

/* 右上角装饰光晕 */
.coupon__glow {
  position: absolute;
  right: -16px;
  top: -16px;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  filter: blur(24px);
  background: rgba(29, 155, 240, 0.1);
  transition: background-color 0.2s;
}

.coupon--orange .coupon__glow {
  background: rgba(254, 107, 0, 0.12);
}

.coupon--green .coupon__glow {
  background: rgba(0, 129, 51, 0.12);
}

.coupon__top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.coupon__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  font-size: 22px;
  color: var(--color-primary);
  background: var(--color-surface-low);
  border-radius: 8px;
}

.coupon__value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}

.coupon--orange .coupon__value {
  color: var(--color-orange);
}

.coupon--green .coupon__value {
  color: var(--color-green);
}

.coupon__title {
  position: relative;
  z-index: 1;
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
}

.coupon__desc {
  position: relative;
  z-index: 1;
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--color-text-secondary);
  flex-grow: 1;
}

/* 打孔虚线 */
.coupon__divider {
  position: relative;
  border-top: 1px dashed rgba(194, 198, 217, 0.6);
  padding-top: 16px;
}

.coupon__divider::before,
.coupon__divider::after {
  content: '';
  position: absolute;
  top: 0;
  width: 16px;
  height: 16px;
  background: #f8f9fa;
  border-radius: 50%;
  transform: translateY(-50%);
}

.coupon__divider::before {
  left: 0;
  transform: translate(-50%, -50%);
}

.coupon__divider::after {
  right: 0;
  transform: translate(50%, -50%);
}

.coupon__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.coupon__expire {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-muted);
}

.coupon__btn.el-button {
  background: var(--color-orange);
  border: none;
  color: #fff;
  font-weight: 600;
  padding: 8px 18px;
}

/* 积分兑换按钮 */
.coupon__btn--points.el-button {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  background: var(--color-primary);
}

.coupon__btn--points.el-button:not(:disabled):hover {
  background: var(--color-primary-hover);
  color: #fff;
}

.coupon__btn-icon {
  font-size: 14px;
}

.coupon__btn.el-button:not(:disabled):hover {
  background: #e05f00;
  color: #fff;
}

.coupon__btn.el-button.is-disabled {
  background: var(--color-search-bg);
  color: var(--color-text-muted);
}
</style>
