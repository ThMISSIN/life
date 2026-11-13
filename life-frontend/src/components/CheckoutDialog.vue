<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="560px"
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
    @open="onOpen"
  >
    <div v-loading="loading" class="checkout">
      <!-- 收货地址（仅含实物商品的订单需要） -->
      <template v-if="needAddress">
        <div class="checkout__section-title">
          收货地址
          <a href="javascript:;" class="checkout__link" @click.prevent="goManageAddress">管理地址</a>
        </div>
        <el-empty
          v-if="!loading && !addresses.length"
          description="还没有收货地址"
          :image-size="60"
        >
          <el-button type="primary" size="small" @click="goManageAddress">去新增地址</el-button>
        </el-empty>
        <div v-else class="checkout__addresses">
          <div
            v-for="a in addresses"
            :key="a.id"
            class="addr"
            :class="{ 'is-selected': selectedAddressId === a.id }"
            @click="selectedAddressId = a.id"
          >
            <div class="addr__line1">
              <span class="addr__name">{{ a.receiverName }}</span>
              <span class="addr__phone">{{ a.receiverPhone }}</span>
              <span v-if="a.isDefault === 1" class="addr__default">默认</span>
            </div>
            <p class="addr__detail">
              {{ a.province }}{{ a.city }}{{ a.district }} {{ a.address }}
            </p>
          </div>
        </div>
      </template>

      <!-- 权益订单：积分结算提示 -->
      <template v-else>
        <div class="checkout__section-title">兑换确认</div>
        <div class="checkout__points-tip">
          <span class="ms-icon">stars</span>
          本单为权益兑换，提交后将使用
          <strong>{{ pointsTotal }}</strong> 积分即时兑换并发放券码
        </div>
      </template>

      <!-- 商品清单 -->
      <div class="checkout__section-title">商品清单</div>
      <ul class="checkout__items">
        <li v-for="(it, i) in displayItems" :key="i" class="checkout__item">
          <el-image :src="it.image" fit="cover" class="checkout__item-img">
            <template #error>
              <span class="ms-icon checkout__item-placeholder">image</span>
            </template>
          </el-image>
          <div class="checkout__item-info">
            <p class="checkout__item-name">{{ it.name }}</p>
            <p class="checkout__item-spec">
              {{ it.itemType === 2 ? '虚拟权益' : '实物商品' }} · x{{ it.quantity }}
            </p>
          </div>
          <span class="checkout__item-price">{{ itemPriceText(it) }}</span>
        </li>
      </ul>

      <!-- 积分抵扣(2.5, 仅现金商品单): 100积分=1元, 上限为现金的50%, 与后端配置口径一致 -->
      <div v-if="deductAvailable" class="checkout__deduct">
        <div class="checkout__deduct-row">
          <span class="checkout__deduct-label">
            <span class="ms-icon">stars</span>
            积分抵扣（可用 {{ userPoints }} 分）
          </span>
          <el-switch v-model="deductOn" @change="onDeductToggle" />
        </div>
        <div v-if="deductOn" class="checkout__deduct-slider">
          <el-slider v-model="deductPoints" :min="0" :max="deductMax" :step="DEDUCT_RATE" />
          <p class="checkout__deduct-tip">
            本单最多可抵 ¥{{ fenToYuan(deductMax) }}（{{ deductMax }} 积分）
          </p>
        </div>
      </div>

      <!-- 备注 -->
      <el-input
        v-model="remark"
        type="textarea"
        :rows="2"
        maxlength="100"
        show-word-limit
        placeholder="订单备注（选填）"
        class="checkout__remark"
      />

      <!-- 合计 -->
      <div class="checkout__summary">
        <template v-if="needAddress">
          <span>共 {{ quantityTotal }} 件 · 合计</span>
          <strong class="checkout__summary-price">¥{{ fenToYuan(cashTotal) }}</strong>
          <template v-if="deductFen > 0">
            <span class="checkout__summary-deduct">-¥{{ fenToYuan(deductFen) }}</span>
            <span class="checkout__summary-pay">应付</span>
            <strong class="checkout__summary-price">¥{{ fenToYuan(cashTotal - deductFen) }}</strong>
          </template>
        </template>
        <template v-else>
          <span>共 {{ quantityTotal }} 份 · 消耗积分</span>
          <strong class="checkout__summary-price">{{ pointsTotal }}</strong>
        </template>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="onSubmit">
        {{ needAddress ? '提交订单' : `确认兑换（${pointsTotal} 积分）` }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 结算/兑换确认对话框（商品详情直购、购物车结算共用）
 * - items: [{ itemType: 1实物/2权益, itemId, name, image, price(分), points, quantity }]
 * - cartIds: 购物车结算时传入, 下单成功后端同事务清行
 * - 含实物商品必须选择收货地址; 纯权益订单走积分即时兑换, 无需地址
 * - 后端限制实物与权益不能混合下单, 页面在打开前保证 items 单一类型
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAddressList } from '@/api/address'
import { submitOrder, getOrderToken, fenToYuan } from '@/api/order'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  cartIds: { type: Array, default: null },
  title: { type: String, default: '确认订单' },
})

const emit = defineEmits(['update:modelValue', 'success'])
const router = useRouter()
const { user } = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const remark = ref('')
// 下单幂等令牌: 弹窗打开时签发一次, 提交即被后端 Lua 原子消费;
// 同一弹窗内双击/重试携带同一令牌 → 后端判"请勿重复提交"; 业务失败后换新令牌方可再试
const orderToken = ref('')

const displayItems = computed(() =>
  props.items.filter((it) => it && Number(it.quantity) > 0),
)
const needAddress = computed(() => displayItems.value.some((it) => it.itemType === 1))
const cashTotal = computed(() =>
  displayItems.value.reduce((sum, it) => sum + (it.itemType === 1 ? (it.price || 0) * it.quantity : 0), 0),
)
const pointsTotal = computed(() =>
  displayItems.value.reduce((sum, it) => sum + (it.itemType === 2 ? it.points * it.quantity : 0), 0),
)
const quantityTotal = computed(() =>
  displayItems.value.reduce((sum, it) => sum + Number(it.quantity || 0), 0),
)
const canSubmit = computed(() => !needAddress.value || Boolean(selectedAddressId.value))

// ---- 积分抵扣(2.5): 常量与后端 application.yml life.points.deduct 保持一致 ----
const DEDUCT_RATE = 100 // 100积分=1元(=100分), 即 1积分=1分
const DEDUCT_MAX_RATIO = 50 // 抵扣上限占现金金额百分比
const deductOn = ref(false)
const deductPoints = ref(0)
const userPoints = computed(() => Number(user.points) || 0)
// 可抵积分上限: min(用户积分, 现金*maxRatio), 取 rate 整数倍
const deductMax = computed(() => {
  const byAmount = Math.floor((cashTotal.value * DEDUCT_MAX_RATIO) / 100)
  return Math.floor(Math.min(byAmount, userPoints.value) / DEDUCT_RATE) * DEDUCT_RATE
})
const deductAvailable = computed(
  () => needAddress.value && cashTotal.value > 0 && deductMax.value >= DEDUCT_RATE,
)
const deductFen = computed(() =>
  deductOn.value ? Math.floor(deductPoints.value / DEDUCT_RATE) * 100 : 0,
)
function onDeductToggle(on) {
  deductPoints.value = on ? deductMax.value : 0
}

function itemPriceText(it) {
  const qty = it.quantity
  if (it.itemType === 2) return `${(it.points || 0) * qty} 积分`
  return `¥${fenToYuan((it.price || 0) * qty)}`
}

async function onOpen() {
  remark.value = ''
  selectedAddressId.value = null
  deductOn.value = false
  deductPoints.value = 0
  // 结算会话开始: 签发一次性下单令牌(失败留空, 提交前 onSubmit 会兜底重取)
  orderToken.value = ''
  getOrderToken().then((t) => (orderToken.value = t)).catch(() => {})
  if (!needAddress.value) return
  loading.value = true
  try {
    addresses.value = await getAddressList()
    const def = addresses.value.find((a) => a.isDefault === 1)
    if (def) selectedAddressId.value = def.id
  } catch (e) {
    ElMessage.error(e.message || '地址加载失败')
  } finally {
    loading.value = false
  }
}

function goManageAddress() {
  emit('update:modelValue', false)
  router.push('/address')
}

async function onSubmit() {
  if (submitting.value) return
  if (needAddress.value && !selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const order = await submitOrder({
      items: displayItems.value.map((it) => ({
        itemType: it.itemType,
        itemId: it.itemId,
        quantity: it.quantity,
      })),
      addressId: needAddress.value ? selectedAddressId.value : undefined,
      remark: remark.value,
      cartIds: props.cartIds || undefined,
      orderToken: orderToken.value || undefined,
      pointsDeduct: deductAvailable.value && deductOn.value && deductPoints.value >= DEDUCT_RATE
        ? Math.floor(deductPoints.value / DEDUCT_RATE) * DEDUCT_RATE
        : undefined,
    })
    emit('update:modelValue', false)
    emit('success', order)
  } catch (e) {
    ElMessage.error(e.message || '下单失败，请稍后重试')
    // 令牌已被后端消费(无论走到哪一步失败), 换新令牌保证用户可直接重试
    orderToken.value = ''
    getOrderToken().then((t) => (orderToken.value = t)).catch(() => {})
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
}

.checkout__link {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
}

.checkout__addresses {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  max-height: 180px;
  overflow-y: auto;
}

.addr {
  padding: 10px 12px;
  border: 1px solid var(--color-border-strong);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.addr:hover {
  border-color: var(--color-primary);
}

.addr.is-selected {
  border-color: var(--color-primary);
  background: rgba(29, 155, 240, 0.05);
}

.addr__line1 {
  display: flex;
  align-items: center;
  gap: 8px;
}

.addr__name {
  font-weight: 700;
  font-size: 14px;
  color: var(--color-text);
}

.addr__phone {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.addr__default {
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-orange);
  border: 1px solid var(--color-orange);
  border-radius: 4px;
}

.addr__detail {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.checkout__points-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px;
  margin-bottom: 20px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-surface-low);
  border-radius: 10px;
}

.checkout__points-tip strong {
  color: var(--color-primary);
}

.checkout__points-tip .ms-icon {
  color: var(--color-orange);
}

.checkout__items {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.checkout__item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.checkout__item-img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: var(--color-surface-low);
  flex-shrink: 0;
}

.checkout__item-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--color-text-muted);
}

.checkout__item-info {
  flex: 1;
  min-width: 0;
}

.checkout__item-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checkout__item-spec {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.checkout__item-price {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-orange);
  flex-shrink: 0;
}

.checkout__remark {
  margin-bottom: 16px;
}

.checkout__summary {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.checkout__summary-price {
  font-size: 20px;
  font-weight: 900;
  color: var(--color-orange);
}

.checkout__summary-deduct {
  color: var(--color-orange);
  font-weight: 600;
}
.checkout__summary-pay {
  color: var(--color-text-secondary);
}

.checkout__deduct {
  margin: 0 0 14px;
  padding: 10px 12px;
  background: var(--color-surface-low);
  border-radius: 10px;
}
.checkout__deduct-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.checkout__deduct-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}
.checkout__deduct-label .ms-icon {
  color: var(--color-orange);
}
.checkout__deduct-slider {
  padding: 0 6px;
}
.checkout__deduct-tip {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
