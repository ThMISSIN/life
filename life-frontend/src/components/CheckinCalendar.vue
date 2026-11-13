<template>
  <div class="checkin-card">
    <div class="checkin-card__header">
      <h2 class="widget-title">签到月历</h2>
      <span class="checkin-card__month">{{ year }}年{{ month }}月</span>
    </div>

    <!-- 星期表头 -->
    <div class="checkin-card__weekdays">
      <span v-for="d in weekdays" :key="d">{{ d }}</span>
    </div>

    <!-- 日期网格 -->
    <div class="checkin-card__grid">
      <span
        v-for="(cell, i) in cells"
        :key="i"
        class="day"
        :class="{
          'day--muted': !cell.current,
          'day--checked': cell.checked,
          'day--today': cell.isToday,
          'day--future': cell.current && cell.date > todayDate,
        }"
      >
        {{ cell.label }}
        <i v-if="cell.checked && !cell.isToday" class="day__dot" />
      </span>
    </div>

    <el-button class="checkin-card__btn" round :disabled="checkedToday" @click="handleCheckin">
      {{ checkedToday ? '今日已打卡' : '今日打卡' }}
    </el-button>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { doCheckin } from '@/api/user'

const props = defineProps({
  // 已签到日期列表（YYYY-MM-DD）
  checkedDates: { type: Array, default: () => [] },
  // 连续签到天数
  consecutiveDays: { type: Number, default: 0 },
  // 今日是否已签到
  checkedToday: { type: Boolean, default: false },
})

const emit = defineEmits(['checked'])

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const now = new Date()
const year = now.getFullYear()
const month = now.getMonth() + 1

const pad = (n) => String(n).padStart(2, '0')
const fmt = (y, m, d) => `${y}-${pad(m)}-${pad(d)}`

const todayDate = fmt(year, month, now.getDate())

// 签到信息
const checkinInfo = ref({
  consecutiveDays: props.consecutiveDays,
  todayPoints: 10,
  checkedToday: props.checkedToday,
})

const checked = ref([...props.checkedDates])
const checkedToday = computed(() => props.checkedToday || checkinInfo.value.checkedToday || checked.value.includes(todayDate))

// 父页面异步加载签到记录后同步进组件（内部 ref 初始化时记录尚未返回）
watch(
  () => props.checkedDates,
  (v) => {
    checked.value = [...v]
  },
  { deep: true },
)
watch(
  () => props.checkedToday,
  (v) => {
    if (v) checkinInfo.value.checkedToday = true
  },
)

// 组装日历单元格：前置补齐上月日期
const cells = computed(() => {
  const firstDay = new Date(year, month - 1, 1).getDay()
  const daysInMonth = new Date(year, month, 0).getDate()
  const daysInPrevMonth = new Date(year, month - 1, 0).getDate()

  const list = []
  for (let i = firstDay - 1; i >= 0; i--) {
    list.push({ label: daysInPrevMonth - i, current: false })
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const date = fmt(year, month, d)
    list.push({
      label: d,
      current: true,
      date,
      isToday: date === todayDate,
      checked: checked.value.includes(date),
    })
  }
  return list
})

async function handleCheckin() {
  try {
    const res = await doCheckin()
    if (!checkedToday.value) {
      checked.value.push(todayDate)
      checkinInfo.value.consecutiveDays += 1
      checkinInfo.value.checkedToday = true
    }
    emit('checked', res)
    ElMessage.success(res.message || '签到成功')
  } catch (e) {
    ElMessage.error(e.message || '签到失败，请稍后重试')
  }
}
</script>

<style scoped>
.checkin-card {
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
}

.checkin-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.checkin-card__month {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  padding: 4px 10px;
  border-radius: 9999px;
}

.checkin-card__weekdays,
.checkin-card__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  text-align: center;
}

.checkin-card__weekdays {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.checkin-card__weekdays span {
  line-height: 32px;
}

.day {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1;
  font-size: 14px;
  color: var(--color-text);
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.2s;
}

.day--muted {
  color: var(--color-text-secondary);
  cursor: default;
}

.day--future {
  color: var(--color-text);
}

.day:hover {
  background: var(--color-surface-hover);
}

.day--checked {
  background: var(--color-green-soft);
  color: var(--color-green);
  font-weight: 700;
}

.day--today {
  background: var(--color-primary);
  color: #fff;
  font-weight: 700;
}

.day__dot {
  position: absolute;
  bottom: 5px;
  right: 7px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--color-green);
}

.day--today .day__dot {
  background: #fff;
}

.checkin-card__btn {
  width: 100%;
  margin-top: 16px;
  height: 42px;
  font-size: 15px;
  font-weight: 700;
  background: var(--color-text);
  border: none;
  color: var(--color-surface);
}

.checkin-card__btn:hover,
.checkin-card__btn:focus {
  background: #2e303a;
  color: var(--color-surface);
}
</style>
