<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="order-main">
      <!-- 吸顶标题栏 + 状态筛选 -->
      <div class="order-header">
        <button class="order-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <div class="order-header__title-wrap">
          <h2 class="order-header__title">我的订单</h2>
        </div>
        <el-select v-model="orderType" class="order-header__type" @change="reload">
          <el-option :value="null" label="全部类型" />
          <el-option :value="1" label="商品订单" />
          <el-option :value="2" label="权益订单" />
        </el-select>
      </div>

      <!-- 状态 Tab -->
      <div class="order-tabs">
        <button
          v-for="t in statusTabs"
          :key="String(t.value)"
          class="order-tabs__item"
          :class="{ 'is-active': status === t.value }"
          @click="switchStatus(t.value)"
        >
          {{ t.label }}
        </button>
      </div>

      <div v-loading="loading" class="order-body">
        <template v-if="orders.length">
          <div v-for="o in orders" :key="o.id" class="order-card">
            <div class="order-card__head" @click="toggleDetail(o)">
              <div class="order-card__meta">
                <span class="order-card__no">{{ o.orderNo }}</span>
                <el-tag size="small" :type="statusMeta(o.status).type" effect="plain">
                  {{ statusMeta(o.status).label }}
                </el-tag>
                <el-tag v-if="o.orderType === 2" size="small" type="warning" effect="plain">
                  权益
                </el-tag>
              </div>
              <span class="order-card__time">{{ o.createTime }}</span>
            </div>

            <div class="order-card__summary" @click="toggleDetail(o)">
              <span class="order-card__amount">
                <template v-if="o.orderType === 2">
                  <span class="ms-icon order-card__points-icon">stars</span>
                  {{ o.pointsUsed }} 积分
                </template>
                <template v-else>¥{{ fenToYuan(o.payAmount) }}</template>
              </span>
              <span class="order-card__toggle">
                {{ expanded[o.id] ? '收起明细' : '查看明细' }}
                <span class="ms-icon" :class="{ 'is-open': expanded[o.id] }">expand_more</span>
              </span>
            </div>

            <!-- 明细（按需拉取详情接口） -->
            <div v-if="expanded[o.id]" class="order-card__details" v-loading="detailLoading[o.id]">
              <div v-for="d in details[o.id] || []" :key="d.id" class="order-detail">
                <el-image :src="d.itemImage" fit="cover" class="order-detail__img">
                  <template #error>
                    <span class="ms-icon order-detail__placeholder">image</span>
                  </template>
                </el-image>
                <div class="order-detail__info">
                  <p class="order-detail__name">{{ d.itemName }}</p>
                  <p class="order-detail__spec">
                    {{ d.itemType === 2 ? '虚拟权益' : '实物商品' }}
                    <template v-if="d.itemType === 1 && d.price > 0">
                      · ¥{{ fenToYuan(d.price) }} × {{ d.quantity }}
                    </template>
                    <template v-else> · {{ d.pointsPrice }} 积分 × {{ d.quantity }}</template>
                  </p>
                </div>
                <span class="order-detail__total">
                  {{ d.itemType === 2 ? `${d.pointsPrice * d.quantity} 积分` : `¥${fenToYuan(d.totalAmount)}` }}
                </span>
              </div>
              <p v-if="o.deliveryAddress" class="order-card__address">
                <span class="ms-icon">location_on</span>
                {{ addressText(o.deliveryAddress) }}
              </p>
              <p v-if="o.remark" class="order-card__remark">备注：{{ o.remark }}</p>
            </div>

            <!-- 操作按钮 -->
            <div class="order-card__actions">
              <template v-if="o.status === 0">
                <el-button size="small" @click="onCancel(o)">取消订单</el-button>
                <el-button size="small" type="primary" @click="onPay(o)">
                  支付 ¥{{ fenToYuan(o.payAmount) }}
                </el-button>
              </template>
              <el-button v-else-if="o.status === 2" size="small" type="primary" @click="onConfirm(o)">
                确认收货
              </el-button>
              <span v-else-if="o.status === 1 && o.orderType === 1" class="order-card__waiting">
                等待商家发货
              </span>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="暂无订单">
          <div class="order-empty-actions">
            <el-button @click="goMall">去商城</el-button>
            <el-button type="primary" @click="goBenefit">去权益中心</el-button>
          </div>
        </el-empty>

        <el-pagination
          v-if="total > PAGE_SIZE"
          class="order-pagination"
          layout="prev, pager, next"
          :total="total"
          :page-size="PAGE_SIZE"
          :current-page="page"
          background
          hide-on-single-page
          @current-change="onPageChange"
        />
      </div>
    </main>

    <!-- 余额展示条（支付通道为账户余额） -->
    <div class="order-balance-tip">当前余额 ¥{{ fenToYuan(user.money) }} · 订单支付使用账户余额</div>
  </div>
</template>

<script setup>
/**
 * 我的订单页（后端 OrderController）
 * - 列表接口仅返回订单头, 点击「查看明细」按需拉取详情（含 details）
 * - 待支付: 余额支付 / 取消; 已发货: 确认收货; 权益单提交即成交, 无支付环节
 * - 支付成功后刷新用户余额
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import {
  getOrderPage,
  getOrderDetail,
  cancelOrder,
  payOrder,
  confirmOrder,
  ORDER_STATUS_MAP,
  fenToYuan,
} from '@/api/order'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { user, loadUser } = useUserStore()
const PAGE_SIZE = 8

const statusTabs = [
  { label: '全部', value: null },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已发货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
]

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const status = ref(null)
const orderType = ref(null)
const expanded = reactive({})
const details = reactive({})
const detailLoading = reactive({})

function statusMeta(s) {
  return ORDER_STATUS_MAP[s] || { label: `状态${s}`, type: 'info' }
}

async function load() {
  loading.value = true
  try {
    const params = { pageNum: page.value, pageSize: PAGE_SIZE }
    if (status.value !== null) params.status = status.value
    if (orderType.value !== null) params.orderType = orderType.value
    const res = await getOrderPage(params)
    orders.value = res?.list || []
    total.value = Number(res?.total || 0)
  } catch (e) {
    ElMessage.error(e.message || '订单加载失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function switchStatus(v) {
  status.value = v
  reload()
}

function onPageChange(p) {
  page.value = p
  load()
}

// 展开/收起明细, 首次展开拉取详情
async function toggleDetail(o) {
  if (expanded[o.id]) {
    expanded[o.id] = false
    return
  }
  expanded[o.id] = true
  if (details[o.id]) return
  detailLoading[o.id] = true
  try {
    const d = await getOrderDetail(o.id)
    details[o.id] = d?.details || []
  } catch (e) {
    ElMessage.error(e.message || '订单详情加载失败')
  } finally {
    detailLoading[o.id] = false
  }
}

async function onPay(o) {
  try {
    await ElMessageBox.confirm(
      `将使用账户余额支付 ¥${fenToYuan(o.payAmount)}（当前余额 ¥${fenToYuan(user.money)}），确认支付？`,
      '余额支付',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  try {
    await payOrder(o.id)
    ElMessage.success('支付成功')
    loadUser(true) // 刷新余额
    load()
  } catch (e) {
    ElMessage.error(e.message || '支付失败')
  }
}

async function onCancel(o) {
  try {
    await ElMessageBox.confirm(`确定取消订单 ${o.orderNo} 吗？`, '取消订单', {
      type: 'warning',
      confirmButtonText: '取消订单',
      cancelButtonText: '再想想',
    })
  } catch {
    return
  }
  try {
    await cancelOrder(o.id)
    ElMessage.success('订单已取消')
    load()
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

async function onConfirm(o) {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '确认收货', {
      type: 'info',
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await confirmOrder(o.id)
    ElMessage.success('已确认收货')
    load()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// deliveryAddress 为地址 JSON 快照
function addressText(json) {
  try {
    const a = JSON.parse(json)
    return `${a.province || ''}${a.city || ''}${a.district || ''} ${a.address || ''}（${a.receiverName || ''} ${a.receiverPhone || ''}）`
  } catch {
    return json
  }
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

onMounted(async () => {
  await loadUser(true)
  load()
})
</script>

<style scoped>
.order-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  min-height: 100vh;
  position: relative;
}

.order-header {
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

.order-header__back {
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

.order-header__back:hover {
  background: var(--color-surface-hover);
}

.order-header__title-wrap {
  flex: 1;
}

.order-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.order-header__type {
  width: 130px;
}

.order-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  background: rgba(250, 248, 255, 0.6);
}

.order-tabs__item {
  flex: 1;
  padding: 12px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.order-tabs__item:hover {
  color: var(--color-primary);
}

.order-tabs__item.is-active {
  font-weight: 700;
  color: var(--color-text);
  box-shadow: inset 0 -3px 0 var(--color-primary);
}

.order-body {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border: 1px solid var(--color-border-strong);
  border-radius: 14px;
  background: var(--color-surface);
  overflow: hidden;
}

.order-card__head,
.order-card__summary {
  cursor: pointer;
}

.order-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px dashed var(--color-border);
}

.order-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.order-card__no {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
}

.order-card__time {
  font-size: 12px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.order-card__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}

.order-card__amount {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 18px;
  font-weight: 900;
  color: var(--color-orange);
}

.order-card__points-icon {
  font-size: 18px;
  color: var(--color-orange);
}

.order-card__toggle {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--color-primary);
}

.order-card__toggle .ms-icon {
  transition: transform 0.2s;
}

.order-card__toggle .is-open {
  transform: rotate(180deg);
}

.order-card__details {
  padding: 0 16px 8px;
  border-top: 1px dashed var(--color-border);
}

.order-detail {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
}

.order-detail__img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: var(--color-surface-low);
  flex-shrink: 0;
}

.order-detail__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--color-text-muted);
}

.order-detail__info {
  flex: 1;
  min-width: 0;
}

.order-detail__name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.order-detail__spec {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.order-detail__total {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
  flex-shrink: 0;
}

.order-card__address {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.order-card__address .ms-icon {
  font-size: 16px;
  color: var(--color-primary);
}

.order-card__remark {
  margin: 6px 0 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.order-card__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid var(--color-border);
}

.order-card__waiting {
  font-size: 12px;
  color: var(--color-text-muted);
}

.order-pagination {
  justify-content: center;
  padding-bottom: 60px;
}

.order-empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 余额提示条 */
.order-balance-tip {
  position: fixed;
  right: 16px;
  bottom: 16px;
  z-index: 40;
  padding: 8px 14px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 9999px;
  box-shadow: 0 4px 12px rgba(15, 20, 25, 0.08);
}
</style>
