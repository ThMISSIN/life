<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="mine-main">
      <!-- 吸顶标题栏 -->
      <div class="mine-header">
        <button class="mine-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <div class="mine-header__title-wrap">
          <h2 class="mine-header__title">我的权益</h2>
          <p class="mine-header__sub">积分兑换的券码都存放在这里</p>
        </div>
        <el-button round class="mine-header__go" @click="goBenefitCenter">去兑换</el-button>
      </div>

      <!-- 状态 Tab -->
      <div class="mine-tabs">
        <button
          v-for="t in statusTabs"
          :key="String(t.value)"
          class="mine-tabs__item"
          :class="{ 'is-active': status === t.value }"
          @click="switchStatus(t.value)"
        >
          {{ t.label }}
        </button>
      </div>

      <div v-loading="loading" class="mine-body">
        <template v-if="records.length">
          <div v-for="b in records" :key="b.id" class="benefit-item" @click="openDetail(b)">
            <el-image :src="b.coverImage" fit="cover" class="benefit-item__img">
              <template #error>
                <span class="ms-icon benefit-item__placeholder">card_giftcard</span>
              </template>
            </el-image>
            <div class="benefit-item__info">
              <p class="benefit-item__name">{{ b.benefitName }}</p>
              <p class="benefit-item__time">
                {{ b.effectiveTime || b.createTime }} ~ {{ b.expireTime || '长期有效' }}
              </p>
            </div>
            <div class="benefit-item__side">
              <el-tag size="small" :type="statusMeta(b.status).type" effect="plain">
                {{ statusMeta(b.status).label }}
              </el-tag>
              <span v-if="b.status === 1" class="benefit-item__view">查看券码</span>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="暂无权益，去权益中心兑换吧">
          <el-button type="primary" @click="goBenefitCenter">去权益中心</el-button>
        </el-empty>

        <el-pagination
          v-if="total > PAGE_SIZE"
          class="mine-pagination"
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

    <!-- 券码详情弹窗 -->
    <el-dialog v-model="codeVisible" title="券码详情" width="420px" destroy-on-close>
      <div v-loading="codeLoading" class="code">
        <template v-if="codeDetail">
          <h3 class="code__name">{{ codeDetail.benefitName }}</h3>
          <p class="code__time">
            {{ codeDetail.effectiveTime || codeDetail.createTime }} ~
            {{ codeDetail.expireTime || '长期有效' }}
          </p>
          <div class="code__box" :class="{ 'is-used': codeDetail.status !== 1 }">
            <span class="code__label">{{ codeDetail.status === 1 ? '专属券码' : statusMeta(codeDetail.status).label }}</span>
            <span class="code__value">{{ codeDetail.code || '————' }}</span>
          </div>
          <p v-if="codeDetail.status === 2 && codeDetail.verifyTime" class="code__verify">
            核销时间：{{ codeDetail.verifyTime }}
          </p>
          <p class="code__tip">出示券码给商家核销即可使用</p>
        </template>
      </div>
      <template #footer>
        <el-button @click="codeVisible = false">关闭</el-button>
        <el-button
          v-if="codeDetail?.status === 1 && codeDetail?.code"
          type="primary"
          :loading="verifying"
          @click="onVerify"
        >
          复制券码
        </el-button>
        <el-button
          v-if="codeDetail?.status === 1"
          type="warning"
          plain
          :loading="verifying"
          @click="onVerify(true)"
        >
          核销使用
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 我的权益（卡包）页（后端 LifeUserBenefitController /benefit/mine/**）
 * - 列表接口不下发券码, 点击卡片拉详情获取券码
 * - 「核销使用」对应 PUT /benefit/mine/{id}/verify
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import {
  getMyBenefits,
  getMyBenefitDetail,
  verifyMyBenefit,
  MY_BENEFIT_STATUS_MAP,
} from '@/api/benefit'

const router = useRouter()
const PAGE_SIZE = 10

const statusTabs = [
  { label: '全部', value: null },
  { label: '未使用', value: 1 },
  { label: '待生效', value: 0 },
  { label: '已使用', value: 2 },
  { label: '已过期', value: 3 },
  { label: '已作废', value: 4 },
]

const loading = ref(false)
const records = ref([])
const total = ref(0)
const page = ref(1)
const status = ref(null)

const codeVisible = ref(false)
const codeLoading = ref(false)
const codeDetail = ref(null)
const verifying = ref(false)

function statusMeta(s) {
  return MY_BENEFIT_STATUS_MAP[s] || { label: `状态${s}`, type: 'info' }
}

async function load() {
  loading.value = true
  try {
    const params = { pageNum: page.value, pageSize: PAGE_SIZE }
    if (status.value !== null) params.status = status.value
    const res = await getMyBenefits(params)
    records.value = res?.records || []
    total.value = Number(res?.total || 0)
  } catch (e) {
    ElMessage.error(e.message || '卡包加载失败')
  } finally {
    loading.value = false
  }
}

function switchStatus(v) {
  status.value = v
  page.value = 1
  load()
}

function onPageChange(p) {
  page.value = p
  load()
}

async function openDetail(b) {
  codeVisible.value = true
  codeLoading.value = true
  codeDetail.value = null
  try {
    codeDetail.value = await getMyBenefitDetail(b.id)
  } catch (e) {
    ElMessage.error(e.message || '券码加载失败')
  } finally {
    codeLoading.value = false
  }
}

async function onVerify(asVerify = false) {
  const d = codeDetail.value
  if (!d) return
  if (asVerify === true) {
    try {
      await ElMessageBox.confirm('核销后权益将标记为已使用，确定核销吗？', '核销确认', {
        type: 'warning',
        confirmButtonText: '确认核销',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
    verifying.value = true
    try {
      await verifyMyBenefit(d.id)
      ElMessage.success('已核销')
      codeVisible.value = false
      load()
    } catch (e) {
      ElMessage.error(e.message || '核销失败')
    } finally {
      verifying.value = false
    }
    return
  }
  // 复制券码
  try {
    await navigator.clipboard.writeText(d.code || '')
    ElMessage.success('券码已复制')
  } catch {
    ElMessage.warning(`复制失败，请手动复制：${d.code}`)
  }
}

function goBack() {
  router.back()
}
function goBenefitCenter() {
  router.push('/benefit-center')
}

onMounted(load)
</script>

<style scoped>
.mine-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  min-height: 100vh;
}

.mine-header {
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

.mine-header__back {
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

.mine-header__back:hover {
  background: var(--color-surface-hover);
}

.mine-header__title-wrap {
  flex: 1;
}

.mine-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.mine-header__sub {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.mine-header__go.el-button {
  font-weight: 700;
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.mine-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  background: rgba(250, 248, 255, 0.6);
  overflow-x: auto;
}

.mine-tabs__item {
  flex: 1;
  min-width: 64px;
  padding: 12px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.mine-tabs__item:hover {
  color: var(--color-primary);
}

.mine-tabs__item.is-active {
  font-weight: 700;
  color: var(--color-text);
  box-shadow: inset 0 -3px 0 var(--color-primary);
}

.mine-body {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-strong);
  border-radius: 14px;
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s;
}

.benefit-item:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(15, 20, 25, 0.06);
}

.benefit-item__img {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  background: var(--color-surface-low);
  flex-shrink: 0;
}

.benefit-item__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 28px;
  color: var(--color-text-muted);
}

.benefit-item__info {
  flex: 1;
  min-width: 0;
}

.benefit-item__name {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.benefit-item__time {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.benefit-item__side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.benefit-item__view {
  font-size: 12px;
  color: var(--color-primary);
}

.mine-pagination {
  justify-content: center;
  padding-bottom: 60px;
}

/* 券码弹窗 */
.code__name {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
}

.code__time {
  margin: 0 0 16px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.code__box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(29, 155, 240, 0.08), rgba(255, 152, 0, 0.08));
  border: 1px dashed rgba(29, 155, 240, 0.4);
  border-radius: 12px;
}

.code__box.is-used {
  filter: grayscale(1);
  opacity: 0.7;
}

.code__label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.code__value {
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 2px;
  color: var(--color-primary);
  word-break: break-all;
  text-align: center;
}

.code__verify {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  text-align: center;
}

.code__tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
}
</style>
