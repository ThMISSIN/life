<template>
  <div class="container-shell">
    <AppSidebar />

    <main v-loading="loading" class="detail-main">
      <button class="detail__back" @click="goBack">
        <span class="ms-icon">arrow_back</span>
        返回商城
      </button>

      <template v-if="goods">
        <!-- 主信息区 -->
        <div class="detail-card">
          <!-- 左：主图与缩略图 -->
          <div class="detail-card__gallery">
            <div class="detail-card__cover">
              <img :src="currentImage" :alt="goods.name" />
            </div>
            <div v-if="goods.detailImages.length > 1" class="detail-card__thumbs">
              <img
                v-for="(img, i) in goods.detailImages"
                :key="i"
                :src="img"
                :alt="`${goods.name} 图 ${i + 1}`"
                :class="{ 'is-active': img === currentImage }"
                @click="currentImage = img"
              />
            </div>
          </div>

          <!-- 右：购买信息 -->
          <div class="detail-card__info">
            <h1 class="detail-card__title">{{ goods.name }}</h1>

            <div class="detail-card__promo">
              <div class="detail-card__price-row">
                <div v-if="goods.price > 0" class="detail-card__price">
                  <span class="detail-card__symbol">¥</span>
                  <span class="detail-card__num">{{ goods.price }}</span>
                  <span
                    v-if="goods.originalPrice > goods.price"
                    class="detail-card__original"
                  >
                    ¥{{ goods.originalPrice }}
                  </span>
                </div>
                <div
                  v-if="goods.pointsPrice > 0"
                  class="detail-card__price detail-card__price--points"
                >
                  <span class="ms-icon detail-card__points-icon">redeem</span>
                  <span class="detail-card__num">{{ goods.pointsPrice }}</span>
                  <span class="detail-card__symbol">积分</span>
                </div>
                <span class="detail-card__sales">已售 {{ salesText }}</span>
              </div>
              <p
                v-if="goods.price > 0 && goods.originalPrice > goods.price"
                class="detail-card__promo-tip"
              >
                <span class="ms-icon">local_offer</span>
                限时直降，下单立省 ¥{{ goods.originalPrice - goods.price }}
              </p>
              <p class="detail-card__stock">
                <span class="ms-icon">{{ goods.inStock ? 'inventory_2' : 'block' }}</span>
                <span>{{ stockText }}</span>
                <span v-if="goods.perUserLimit > 0" class="detail-card__limit">
                  · 每人限兑 {{ goods.perUserLimit }} 次
                </span>
                <span v-if="goods.minLevel > 0" class="detail-card__limit">
                  · 需会员 Lv{{ goods.minLevel }} 以上
                </span>
              </p>
            </div>

            <ul class="detail-card__services">
              <li v-for="s in goods.services" :key="s.label">
                <span class="ms-icon">{{ s.icon }}</span>
                {{ s.label }}
              </li>
            </ul>

            <div class="detail-card__meta">
              <span>物流：{{ goods.location }} 发货 · 免运费</span>
              <span>店铺：{{ goods.shop }}</span>
            </div>

            <div class="detail-card__actions">
              <div class="detail-card__qty">
                <span class="detail-card__qty-label">数量</span>
                <el-input-number v-model="quantity" :min="1" :max="maxQty" :disabled="!goods.inStock" />
              </div>
              <div class="detail-card__btns">
                <el-button class="detail-card__cart-btn" :disabled="!goods.inStock" @click="onAddCart">
                  <span class="ms-icon">add_shopping_cart</span>
                  加入购物车
                </el-button>
                <el-button
                  class="detail-card__buy-btn"
                  type="primary"
                  :disabled="!goods.inStock"
                  @click="onBuy"
                >
                  {{ goods.inStock ? (goods.price > 0 ? '立即购买' : '立即兑换') : '暂时缺货' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 详情区 -->
        <div class="detail-desc">
          <h2 class="detail-desc__title">商品详情</h2>
          <p class="detail-desc__text">{{ goods.description }}</p>
          <div class="detail-desc__images">
            <img
              v-for="(img, i) in goods.detailImages"
              :key="i"
              :src="img"
              :alt="`${goods.name} 详情图 ${i + 1}`"
              loading="lazy"
            />
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="商品不存在或已下架">
        <el-button @click="goBack">返回商城</el-button>
      </el-empty>

      <!-- 立即购买结算 -->
      <CheckoutDialog
        v-model="checkoutVisible"
        :items="checkoutItems"
        title="确认购买"
        @success="onOrderSuccess"
      />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import CheckoutDialog from '@/components/CheckoutDialog.vue'
import { getGoodsDetail, addCart } from '@/api/mall'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const goods = ref(null)
const currentImage = ref('')
const quantity = ref(1)
const acting = ref(false)
const checkoutVisible = ref(false)

const salesText = computed(() => {
  const s = Number(goods.value?.sales || 0)
  if (s >= 10000) return `${(s / 10000).toFixed(1).replace(/\.0$/, '')}万+`
  return `${s}+`
})

// 数量上限: stock=-1 不限购(封顶99), 否则不超过库存
const maxQty = computed(() => {
  const stock = Number(goods.value?.stock ?? 0)
  if (stock === -1) return 99
  return Math.min(99, Math.max(1, stock))
})

const stockText = computed(() => {
  const g = goods.value
  if (!g) return ''
  if (!g.inStock) return '暂时缺货'
  if (g.stock === -1) return '库存充足'
  if (g.stock <= 10) return `仅剩 ${g.stock} 件`
  return '库存充足'
})

// 结算对话框所需的商品条目（itemType 1-实物商品）
const checkoutItems = computed(() =>
  goods.value
    ? [
        {
          itemType: 1,
          itemId: goods.value.id,
          name: goods.value.name,
          image: goods.value.image,
          price: Math.round(Number(goods.value.price || 0) * 100),
          points: goods.value.pointsPrice,
          quantity: quantity.value,
        },
      ]
    : [],
)

async function loadDetail() {
  loading.value = true
  try {
    goods.value = await getGoodsDetail(route.params.id)
    currentImage.value = goods.value?.image || ''
  } catch (e) {
    ElMessage.error(e.message || '加载商品详情失败')
  } finally {
    loading.value = false
  }
}

async function onAddCart() {
  if (acting.value) return
  acting.value = true
  try {
    const res = await addCart(goods.value.id, quantity.value)
    ElMessage.success(res.message || '已加入购物车')
  } catch (e) {
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    acting.value = false
  }
}

function onBuy() {
  checkoutVisible.value = true
}

// 下单成功: 待支付订单引导去订单页支付（余额通道）
async function onOrderSuccess(order) {
  try {
    await ElMessageBox.confirm(
      order?.payAmount === 0
        ? '订单已提交，¥0 订单也需在订单页点击「支付」完成流程。是否立即前往？'
        : '订单提交成功！是否立即前往订单页完成支付？',
      '下单成功',
      { confirmButtonText: '去订单页', cancelButtonText: '稍后再说', type: 'success' },
    )
    router.push('/orders')
  } catch {
    // 用户选择稍后处理
  }
}

function goBack() {
  router.push('/mall')
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
  height: 300px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-surface-low);
}

.detail-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.2s;
}

.detail-card__thumbs img.is-active {
  border-color: var(--color-primary);
}

.detail-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.detail-card__title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.01em;
  color: var(--color-text);
}

/* 促销价格块 */
.detail-card__promo {
  padding: 16px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(254, 107, 0, 0.06), rgba(254, 107, 0, 0.1));
  border-radius: 12px;
}

.detail-card__price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.detail-card__price {
  display: flex;
  align-items: baseline;
  color: var(--color-orange);
}

.detail-card__symbol {
  font-size: 16px;
  font-weight: 700;
}

.detail-card__num {
  font-size: 34px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1;
}

.detail-card__original {
  font-size: 14px;
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.detail-card__price--points {
  color: var(--color-primary);
}

.detail-card__price--points .detail-card__num {
  font-size: 28px;
}

.detail-card__points-icon {
  font-size: 22px;
  margin-right: 2px;
  align-self: center;
}

.detail-card__stock {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.detail-card__stock .ms-icon {
  font-size: 16px;
  color: var(--color-green);
}

.detail-card__limit {
  color: var(--color-text-muted);
}

.detail-card__sales {
  margin-left: auto;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.detail-card__promo-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 10px 0 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-orange);
}

.detail-card__promo-tip .ms-icon {
  font-size: 16px;
}

/* 服务保障 */
.detail-card__services {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
}

.detail-card__services li {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.detail-card__services .ms-icon {
  font-size: 16px;
  color: var(--color-green);
}

.detail-card__meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 购买操作 */
.detail-card__actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card__qty {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-card__qty-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.detail-card__btns {
  display: flex;
  gap: 12px;
}

.detail-card__cart-btn.el-button {
  flex: 1;
  padding: 14px 24px;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-orange);
  background: rgba(254, 107, 0, 0.08);
  border: 1px solid rgba(254, 107, 0, 0.4);
  border-radius: 10px;
}

.detail-card__cart-btn.el-button:hover {
  color: #fff;
  background: var(--color-orange);
  border-color: var(--color-orange);
}

.detail-card__buy-btn.el-button {
  flex: 1;
  padding: 14px 24px;
  font-size: 15px;
  font-weight: 700;
  background: var(--color-orange);
  border: none;
  border-radius: 10px;
}

.detail-card__buy-btn.el-button:hover {
  background: #e05f00;
}

/* 详情区 */
.detail-desc {
  margin-top: 24px;
  padding: 32px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 16px;
}

.detail-desc__title {
  margin: 0 0 16px;
  padding-left: 12px;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  border-left: 4px solid var(--color-primary);
  line-height: 1.2;
}

.detail-desc__text {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  white-space: pre-line;
}

.detail-desc__images {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-desc__images img {
  width: 100%;
  max-width: 720px;
  border-radius: 12px;
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
