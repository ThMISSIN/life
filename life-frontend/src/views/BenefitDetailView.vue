<template>
  <div class="container-shell">
    <AppSidebar />

    <main v-loading="loading" class="detail-main">
      <button class="detail__back" @click="goBack">
        <span class="ms-icon">arrow_back</span>
        返回权益列表
      </button>

      <template v-if="detail">
        <div class="detail-card">
          <!-- 左: 封面与详情图 -->
          <div class="detail-card__gallery">
            <div class="detail-card__cover">
              <img v-if="detail.coverImage" :src="detail.coverImage" :alt="detail.name" />
              <div v-else class="detail-card__cover-placeholder">
                <span class="ms-icon">card_giftcard</span>
              </div>
            </div>
            <div v-if="detail.images && detail.images.length" class="detail-card__thumbs">
              <img
                v-for="(img, i) in detail.images"
                :key="i"
                :src="img"
                :alt="`${detail.name} 详情图 ${i + 1}`"
                loading="lazy"
              />
            </div>
          </div>

          <!-- 右: 信息与兑换 -->
          <div class="detail-card__info">
            <h1 class="detail-card__title">{{ detail.name }}</h1>

            <div class="detail-card__price-row">
              <div class="detail-card__points">
                <span class="ms-icon ms-icon--fill detail-card__points-icon">stars</span>
                <span>{{ formatNumber(detail.pointsPrice) }}</span>
                <span class="detail-card__points-unit">积分</span>
              </div>
              <span v-if="originalYuan" class="detail-card__original">价值 ¥{{ originalYuan }}</span>
            </div>

            <ul class="detail-card__rules">
              <li>
                <span class="detail-card__rule-label">有效期</span>
                <template v-if="detail.validType === 1">兑换后 {{ detail.validDays }} 天内有效</template>
                <template v-else>有效期至 {{ detail.fixedEndTime }}</template>
              </li>
              <li>
                <span class="detail-card__rule-label">限兑</span>
                <template v-if="detail.perUserLimit > 0">
                  每人限兑 {{ detail.perUserLimit }} 次（已兑 {{ detail.myExchangedCount }} 次）
                </template>
                <template v-else>不限次数</template>
              </li>
              <li>
                <span class="detail-card__rule-label">库存</span>
                {{ stockText }}
              </li>
              <li v-if="detail.minLevel > 0">
                <span class="detail-card__rule-label">等级</span>
                需要 LV{{ detail.minLevel }} 及以上会员
              </li>
              <li v-if="detail.sales > 0">
                <span class="detail-card__rule-label">热度</span>
                已累计兑换 {{ formatNumber(detail.sales) }} 份
              </li>
            </ul>

            <p class="detail-card__desc">{{ detail.description || '暂无介绍' }}</p>

            <div class="detail-card__actions">
              <el-button
                class="detail-card__btn"
                type="primary"
                :disabled="!detail.canExchange || exchanging"
                :loading="exchanging"
                @click="onExchange"
              >
                {{ exchangeLabel }}
              </el-button>
            </div>
            <p class="detail-card__hint">
              兑换成功后将生成专属券码，
              <a href="javascript:;" class="detail-card__hint-link" @click.prevent="goMyBenefits">
                可在「我的权益」中查看使用
              </a>
            </p>
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="权益不存在或已下架">
        <el-button @click="goBack">返回权益列表</el-button>
      </el-empty>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import { getBenefitDetail, exchangeBenefit } from '@/api/benefit'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { loadUser } = useUserStore()

const loading = ref(false)
const exchanging = ref(false)
const detail = ref(null)

const formatNumber = (n) => Number(n || 0).toLocaleString('en-US')

const originalYuan = computed(() => {
  const p = Number(detail.value?.originalPrice || 0)
  return p > 0 ? Math.round(p / 100) : 0
})

const stockText = computed(() => {
  const stock = detail.value?.stock
  if (stock === -1) return '不限量'
  return stock > 0 ? `剩余 ${formatNumber(stock)} 份` : '已抢完'
})

const limitReached = computed(() => {
  const d = detail.value
  return d?.perUserLimit > 0 && d?.myExchangedCount >= d?.perUserLimit
})

const exchangeLabel = computed(() => {
  if (!detail.value) return ''
  if (detail.value.stock === 0) return '已抢完'
  if (limitReached.value) return '已达限兑次数'
  return `立即兑换（${formatNumber(detail.value.pointsPrice)} 积分）`
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getBenefitDetail(route.params.id)
  } catch (e) {
    // 401 由请求层统一处理; 业务错误(如权益已下架)展示后保留空态
    ElMessage.error(e.message || '加载权益详情失败')
  } finally {
    loading.value = false
  }
}

async function onExchange() {
  const d = detail.value
  try {
    await ElMessageBox.confirm(
      `确认使用 ${formatNumber(d.pointsPrice)} 积分兑换「${d.name}」？`,
      '积分兑换确认',
      { confirmButtonText: '确认兑换', cancelButtonText: '再想想', type: 'info' },
    )
  } catch {
    return // 用户取消
  }

  exchanging.value = true
  try {
    // 兑换 = 提交权益订单（纯积分即时结算, 券码即时发放）
    await exchangeBenefit(d.id)
    ElMessage.success('兑换成功，券码已放入卡包')
    // 刷新积分余额与详情（已兑次数/可兑换标识）
    loadUser(true)
    await loadDetail()
  } catch (e) {
    ElMessage.error(e.message || '兑换失败，请稍后重试')
  } finally {
    exchanging.value = false
  }
}

function goBack() {
  router.push('/benefit-center')
}

// 前往卡包查看已兑换的券码
function goMyBenefits() {
  router.push('/my-benefits')
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-main {
  width: 100%;
  max-width: 760px;
  min-width: 0;
  padding: 20px 16px 60px;
  border-right: 1px solid var(--color-border);
  min-height: 100vh;
}

.detail__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.detail__back:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: rgba(29, 155, 240, 0.04);
}

.detail__back .ms-icon {
  font-size: 16px;
}

.detail-card {
  display: flex;
  gap: 24px;
  padding: 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
}

.detail-card__gallery {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-card__cover {
  height: 260px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-surface-low);
}

.detail-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-card__cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(29, 155, 240, 0.08), rgba(255, 152, 0, 0.08));
  color: var(--color-primary);
}

.detail-card__cover-placeholder .ms-icon {
  font-size: 56px;
  opacity: 0.5;
}

.detail-card__thumbs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.detail-card__thumbs img {
  width: 100%;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.detail-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.detail-card__title {
  margin: 0 0 16px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--color-text);
}

.detail-card__price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 16px;
  margin-bottom: 16px;
  background: var(--color-surface-low);
  border-radius: 12px;
}

.detail-card__points {
  display: flex;
  align-items: baseline;
  gap: 4px;
  color: var(--color-primary);
  font-weight: 700;
  font-size: 26px;
}

.detail-card__points-icon {
  font-size: 20px;
  align-self: center;
}

.detail-card__points-unit {
  font-size: 12px;
  font-weight: 500;
}

.detail-card__original {
  font-size: 13px;
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.detail-card__rules {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-card__rules li {
  display: flex;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.detail-card__rule-label {
  flex-shrink: 0;
  width: 48px;
  color: var(--color-text-muted);
}

.detail-card__desc {
  margin: 0 0 24px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-secondary);
  white-space: pre-line;
}

.detail-card__actions {
  margin-top: auto;
}

.detail-card__btn.el-button {
  width: 100%;
  padding: 14px 24px;
  font-size: 15px;
  font-weight: 700;
  border-radius: 10px;
}

.detail-card__hint {
  margin: 12px 0 0;
  font-size: 11px;
  color: var(--color-text-muted);
  text-align: center;
}

.detail-card__hint-link {
  color: var(--color-primary);
}

@media (max-width: 900px) {
  .detail-card {
    flex-direction: column;
  }

  .detail-card__gallery {
    width: 100%;
  }
}
</style>
