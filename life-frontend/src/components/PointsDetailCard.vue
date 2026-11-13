<template>
  <div class="points-detail-card">
    <div class="points-detail-card__header">
      <h2 class="widget-title">积分明细</h2>
      <a href="javascript:;" class="points-detail-card__all" @click.prevent="viewAll">全部</a>
    </div>

    <div class="points-detail-card__list">
      <div v-for="record in records" :key="record.id" class="record">
        <div class="record__icon">
          <span class="ms-icon">{{ record.icon }}</span>
        </div>
        <div class="record__info">
          <p class="record__title">{{ record.type }}</p>
          <p class="record__time">{{ record.time }}</p>
        </div>
        <span class="record__change" :class="record.change > 0 ? 'is-earn' : 'is-spend'">
          {{ record.change > 0 ? `+${record.change}` : record.change }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPointsRecords } from '@/api/user'

const router = useRouter()
const records = ref([])

// 组件挂载时由父页面调用 load()
async function load() {
  records.value = await getPointsRecords()
}

defineExpose({ load })

function viewAll() {
  router.push('/profile?tab=points')
}
</script>

<style scoped>
.points-detail-card {
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
}

.points-detail-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.points-detail-card__all {
  font-size: 14px;
  color: var(--color-primary);
}

.points-detail-card__list {
  display: flex;
  flex-direction: column;
}

.record {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  margin: 0 -8px;
  border-bottom: 1px solid var(--color-border);
  border-radius: 8px;
  transition: background-color 0.2s;
}

.record:last-child {
  border-bottom: none;
}

.record:hover {
  background: rgba(225, 226, 238, 0.2);
}

.record__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-surface-low);
  color: var(--color-primary);
  flex-shrink: 0;
}

.record__icon .ms-icon {
  font-size: 20px;
}

.record__info {
  flex: 1;
  min-width: 0;
}

.record__title {
  margin: 0;
  font-weight: 700;
  font-size: 15px;
  color: var(--color-text);
  line-height: 1.4;
}

.record__time {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.record__change {
  font-weight: 700;
  font-size: 15px;
}

.record__change.is-earn {
  color: var(--color-green);
}

.record__change.is-spend {
  color: var(--color-text);
}
</style>
