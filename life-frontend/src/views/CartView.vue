<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="cart-main">
      <!-- 吸顶标题栏 -->
      <div class="cart-header">
        <button class="cart-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <div class="cart-header__title-wrap">
          <h2 class="cart-header__title">购物车</h2>
          <p class="cart-header__count" v-if="rows.length">共 {{ rows.length }} 种商品</p>
        </div>
        <el-button
          v-if="rows.length"
          class="cart-header__clear"
          text
          type="danger"
          @click="onClear"
        >
          清空购物车
        </el-button>
      </div>

      <div v-loading="loading" class="cart-body">
        <template v-if="rows.length">
          <div
            v-for="row in rows"
            :key="row.id"
            class="cart-row"
            :class="{ 'is-invalid': !row.valid }"
          >
            <el-checkbox
              v-model="row.checked"
              :disabled="!row.valid"
              class="cart-row__check"
              @change="recalcSummary"
            />
            <el-image :src="row.itemImage" fit="cover" class="cart-row__img">
              <template #error>
                <span class="ms-icon cart-row__img-placeholder">image</span>
              </template>
            </el-image>

            <div class="cart-row__info">
              <p class="cart-row__name">
                <el-tag v-if="row.itemType === 2" size="small" type="warning" effect="plain" class="cart-row__type">
                  权益
                </el-tag>
                {{ row.itemName }}
              </p>
              <p class="cart-row__price">
                <template v-if="row.itemType === 1 && row.price > 0">
                  ¥{{ fenToYuan(row.price) }}
                </template>
                <template v-else>
                  <span class="ms-icon cart-row__points-icon">stars</span>
                  {{ row.pointsPrice }} 积分
                </template>
                <el-tag
                  v-if="row.priceChanged"
                  size="small"
                  type="danger"
                  effect="plain"
                  class="cart-row__price-changed"
                >
                  价格已变动
                </el-tag>
              </p>
              <p v-if="!row.valid" class="cart-row__invalid-tip">
                {{ row.onSale ? '库存不足，暂时无法结算' : '已下架，暂时无法结算' }}
              </p>
            </div>

            <div class="cart-row__ops">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="maxQtyOf(row)"
                size="small"
                :disabled="!row.valid"
                @change="(q) => onQuantityChange(row, q)"
              />
              <div class="cart-row__subtotal">
                <template v-if="row.itemType === 1 && row.price > 0">
                  ¥{{ fenToYuan(row.price * row.quantity) }}
                </template>
                <template v-else>{{ row.pointsPrice * row.quantity }} 积分</template>
              </div>
              <el-button text type="danger" size="small" @click="onRemove(row)">删除</el-button>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="购物车还是空的">
          <div class="cart-empty-actions">
            <el-button @click="goMall">去商城逛逛</el-button>
            <el-button type="primary" @click="goBenefit">去权益中心</el-button>
          </div>
        </el-empty>
      </div>

      <!-- 底部结算栏 -->
      <div v-if="rows.length" class="cart-footer">
        <el-checkbox v-model="allChecked" :indeterminate="indeterminate" @change="onCheckAll">
          全选
        </el-checkbox>
        <div class="cart-footer__summary">
          <span v-if="selectedCount">已选 {{ selectedCount }} 件</span>
          <template v-if="selectedCash > 0">
            <span class="cart-footer__total-label">合计</span>
            <strong class="cart-footer__total">¥{{ fenToYuan(selectedCash) }}</strong>
          </template>
          <template v-else-if="selectedPoints > 0">
            <span class="cart-footer__total-label">消耗</span>
            <strong class="cart-footer__total">{{ selectedPoints }} 积分</strong>
          </template>
        </div>
        <el-button
          type="primary"
          round
          class="cart-footer__btn"
          :disabled="!selectedCount"
          @click="onCheckout"
        >
          结算（{{ selectedCount }}）
        </el-button>
      </div>
    </main>

    <!-- 结算对话框 -->
    <CheckoutDialog
      v-model="checkoutVisible"
      :items="checkoutItems"
      :cart-ids="selectedCartIds"
      :title="checkoutTitle"
      @success="onOrderSuccess"
    />
  </div>
</template>

<script setup>
/**
 * 购物车页（后端 CartController）
 * - 行级勾选, 失效行（下架/无库存）置灰且不可勾选
 * - 后端限制实物商品与权益不能混合下单, 勾选混合时结算前提示分开结算
 * - 结算走 CheckoutDialog（商品单需地址; 纯权益单为积分即时兑换）
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import CheckoutDialog from '@/components/CheckoutDialog.vue'
import {
  getCartList,
  updateCartQuantity,
  removeCart,
  clearCart,
} from '@/api/cart'
import { fenToYuan } from '@/api/order'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const checkoutVisible = ref(false)

// 购物车行 → 勾选/展示结构（valid=false 的行禁止勾选）
async function load() {
  loading.value = true
  try {
    const list = await getCartList()
    rows.value = (list || []).map((r) => ({ ...r, checked: false }))
  } catch (e) {
    ElMessage.error(e.message || '购物车加载失败')
  } finally {
    loading.value = false
  }
}

function maxQtyOf(row) {
  if (row.stock === -1 || row.stock == null) return 99
  return Math.max(1, row.stock)
}

async function onQuantityChange(row, q) {
  if (!q || q < 1) return
  try {
    await updateCartQuantity(row.id, q)
  } catch (e) {
    ElMessage.error(e.message || '数量修改失败')
    await load()
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(`从购物车移除「${row.itemName}」？`, '移除商品', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await removeCart(row.id)
    rows.value = rows.value.filter((r) => r.id !== row.id)
    ElMessage.success('已移除')
  } catch (e) {
    ElMessage.error(e.message || '移除失败')
  }
}

async function onClear() {
  try {
    await ElMessageBox.confirm('将清空购物车中的全部商品，确定吗？', '清空购物车', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await clearCart()
    rows.value = []
    ElMessage.success('购物车已清空')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

const validRows = computed(() => rows.value.filter((r) => r.valid))
const selectedRows = computed(() => validRows.value.filter((r) => r.checked))
const selectedCount = computed(() => selectedRows.value.length)
const selectedCash = computed(() =>
  selectedRows.value.reduce(
    (sum, r) => sum + (r.itemType === 1 ? (r.price || 0) * r.quantity : 0),
    0,
  ),
)
const selectedPoints = computed(() =>
  selectedRows.value.reduce(
    (sum, r) => sum + (r.itemType === 2 ? (r.pointsPrice || 0) * r.quantity : 0),
    0,
  ),
)
const selectedCartIds = computed(() => selectedRows.value.map((r) => r.id))

const allChecked = computed({
  get: () => validRows.value.length > 0 && validRows.value.every((r) => r.checked),
  set: (v) => validRows.value.forEach((r) => (r.checked = v)),
})
const indeterminate = computed(() => {
  const c = validRows.value.filter((r) => r.checked).length
  return c > 0 && c < validRows.value.length
})

function onCheckAll(v) {
  validRows.value.forEach((r) => (r.checked = v))
}

// 后端拦截混合下单: 实物与权益需分开结算
const checkoutItems = computed(() =>
  selectedRows.value.map((r) => ({
    itemType: r.itemType,
    itemId: r.itemId,
    name: r.itemName,
    image: r.itemImage,
    price: r.price || 0,
    points: r.pointsPrice || 0,
    quantity: r.quantity,
  })),
)
const checkoutTitle = computed(() =>
  checkoutItems.value[0]?.itemType === 2 ? '确认兑换' : '确认订单',
)

function recalcSummary() {
  // 勾选状态直接落在行对象上, 合计均为 computed, 此处无需额外处理
}

async function onCheckout() {
  const types = new Set(selectedRows.value.map((r) => r.itemType))
  if (types.size > 1) {
    ElMessage.warning('实物商品与权益不能合并结算，请分别勾选后再结算')
    return
  }
  checkoutVisible.value = true
}

async function onOrderSuccess() {
  ElMessage.success('下单成功')
  await load()
  router.push('/orders')
}

function goBack() {
  router.back()
}
function goMall() {
  router.push('/mall')
}
function goBenefit() {
  router.push('/benefit-center')
}

onMounted(load)
</script>

<style scoped>
.cart-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  min-height: 100vh;
}

.cart-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  background: rgba(250, 248, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.cart-header__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text);
  cursor: pointer;
  transition: background-color 0.2s;
}

.cart-header__back:hover {
  background: var(--color-surface-hover);
}

.cart-header__title-wrap {
  flex: 1;
}

.cart-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.cart-header__count {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.cart-body {
  flex: 1;
  padding: 8px 0 120px;
}

.cart-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
}

.cart-row.is-invalid {
  opacity: 0.55;
}

.cart-row__img {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  background: var(--color-surface-low);
  flex-shrink: 0;
}

.cart-row__img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--color-text-muted);
}

.cart-row__info {
  flex: 1;
  min-width: 0;
}

.cart-row__name {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-row__type {
  flex-shrink: 0;
}

.cart-row__price {
  margin: 6px 0 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-orange);
  display: flex;
  align-items: center;
  gap: 2px;
}

.cart-row__points-icon {
  font-size: 15px;
  color: var(--color-orange);
}

.cart-row__invalid-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--color-danger);
}

.cart-row__ops {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.cart-row__subtotal {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
}

.cart-empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 底部结算栏 */
.cart-footer {
  position: sticky;
  bottom: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: rgba(250, 248, 255, 0.92);
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--color-border-strong);
}

.cart-footer__summary {
  flex: 1;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.cart-footer__total {
  font-size: 22px;
  font-weight: 900;
  color: var(--color-orange);
}

.cart-footer__btn {
  min-width: 140px;
  font-weight: 700;
}
</style>
