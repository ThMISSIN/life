<template>
  <div class="goods" @click="$emit('select')">
    <div class="goods__img-wrap">
      <img class="goods__img" :src="goods.image" :alt="goods.name" loading="lazy" />
    </div>
    <div class="goods__info">
      <h4 class="goods__name">{{ goods.name }}</h4>
      <div class="goods__tags">
        <span v-for="tag in goods.tags" :key="tag" class="goods__tag">{{ tag }}</span>
      </div>
      <div class="goods__price-row">
        <div class="goods__price">
          <span class="goods__symbol">¥</span>
          <span class="goods__price-num">{{ goods.price }}</span>
          <span v-if="goods.originalPrice" class="goods__original">¥{{ goods.originalPrice }}</span>
        </div>
        <span class="goods__sales">已售 {{ salesText }}</span>
      </div>
      <div class="goods__meta">
        <span class="goods__shop">{{ goods.shop }}</span>
        <span class="goods__location">{{ goods.location }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  goods: { type: Object, required: true },
})

defineEmits(['select'])

// 已售数量格式化：1.2万
const salesText = computed(() => {
  const s = Number(props.goods.sales || 0)
  if (s >= 10000) return `${(s / 10000).toFixed(1).replace(/\.0$/, '')}万+`
  return `${s}+`
})
</script>

<style scoped>
.goods {
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.goods:hover {
  border-color: rgba(29, 155, 240, 0.5);
  box-shadow: 0 8px 20px rgba(15, 20, 25, 0.08);
  transform: translateY(-2px);
}

.goods__img-wrap {
  height: 190px;
  overflow: hidden;
  background: var(--color-surface-low);
}

.goods__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.goods:hover .goods__img {
  transform: scale(1.05);
}

.goods__info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  flex-grow: 1;
}

.goods__name {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  color: var(--color-text);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  min-height: 36px;
}

.goods__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.goods__tag {
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 600;
  color: var(--color-primary);
  background: rgba(29, 155, 240, 0.08);
  border: 1px solid rgba(29, 155, 240, 0.25);
  border-radius: 4px;
}

.goods__price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
}

.goods__price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  color: var(--color-orange);
}

.goods__symbol {
  font-size: 12px;
  font-weight: 700;
}

.goods__price-num {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.goods__original {
  margin-left: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.goods__sales {
  font-size: 11px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.goods__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: var(--color-text-secondary);
}

.goods__shop {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods__location {
  flex-shrink: 0;
  color: var(--color-text-muted);
}
</style>
