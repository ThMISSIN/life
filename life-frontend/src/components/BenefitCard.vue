<template>
  <div class="benefit" @click="$emit('select')">
    <div class="benefit__img-wrap">
      <img v-if="benefit.coverImage" class="benefit__img" :src="benefit.coverImage" :alt="benefit.name" loading="lazy" />
      <div v-else class="benefit__placeholder">
        <span class="ms-icon">card_giftcard</span>
      </div>
      <span v-if="soldOut" class="benefit__badge">已抢完</span>
    </div>
    <div class="benefit__info">
      <h4 class="benefit__name">{{ benefit.name }}</h4>
      <p v-if="benefit.sales > 0" class="benefit__sales">已兑 {{ formatNumber(benefit.sales) }} 份</p>
      <div class="benefit__price">
        <div class="benefit__points">
          <span class="ms-icon ms-icon--fill benefit__points-icon">stars</span>
          <span class="benefit__points-num">{{ formatNumber(benefit.pointsPrice) }}</span>
        </div>
        <span v-if="originalYuan" class="benefit__original">¥{{ originalYuan }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  benefit: { type: Object, required: true },
})

defineEmits(['select'])

// stock: 0-售罄, -1-不限
const soldOut = computed(() => props.benefit.stock === 0)

// 原价单位为分, 展示为元(整数)
const originalYuan = computed(() => {
  const p = Number(props.benefit.originalPrice || 0)
  return p > 0 ? Math.round(p / 100) : 0
})

const formatNumber = (n) => Number(n || 0).toLocaleString('en-US')
</script>

<style scoped>
.benefit {
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.benefit:hover {
  border-color: rgba(29, 155, 240, 0.5);
  box-shadow: 0 8px 20px rgba(15, 20, 25, 0.06);
}

.benefit__img-wrap {
  position: relative;
  height: 128px;
  overflow: hidden;
  background: var(--color-surface-low);
}

.benefit__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.benefit:hover .benefit__img {
  transform: scale(1.05);
}

.benefit__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(29, 155, 240, 0.08), rgba(255, 152, 0, 0.08));
  color: var(--color-primary);
}

.benefit__placeholder .ms-icon {
  font-size: 40px;
  opacity: 0.5;
}

.benefit__badge {
  position: absolute;
  right: 8px;
  top: 8px;
  padding: 2px 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: rgba(15, 20, 25, 0.6);
  border-radius: 9999px;
  backdrop-filter: blur(2px);
}

.benefit__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  flex-grow: 1;
}

.benefit__name {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.benefit__sales {
  margin: 0;
  font-size: 11px;
  color: var(--color-text-muted);
}

.benefit__price {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 4px;
}

.benefit__points {
  display: flex;
  align-items: center;
  gap: 4px;
}

.benefit__points-icon {
  font-size: 16px;
  color: var(--color-primary);
}

.benefit__points-num {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-primary);
}

.benefit__original {
  font-size: 11px;
  color: var(--color-text-muted);
  text-decoration: line-through;
}
</style>
