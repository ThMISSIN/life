<template>
  <div class="container-shell">
    <AppSidebar />

    <div class="benefit-wrapper">
      <!-- 中间主内容（8 列） -->
      <main v-loading="loading" class="benefit-main">
        <!-- Banner: 仅落地页展示, 点击"探索所有权益"进入兑换模式后隐藏 -->
        <section v-if="!entered" class="hero">
          <div class="hero__bg" :style="{ backgroundImage: `url(${banner.image})` }" />
          <div class="hero__content">
            <span class="hero__tag">
              <span class="ms-icon ms-icon--fill hero__tag-icon">workspace_premium</span>
              {{ banner.tag }}
            </span>
            <h1 class="hero__title">{{ banner.title }}</h1>
            <p class="hero__subtitle">{{ banner.subtitle }}</p>
            <el-button class="hero__btn" @click="showFab('COUPON')">{{ banner.buttonText }}</el-button>
          </div>
        </section>

        <!-- 会员提示条: 仅落地页展示 -->
        <div v-if="!entered" class="vip-strip">
          <span class="ms-icon ms-icon--fill vip-strip__icon">diamond</span>
          <p class="vip-strip__text">
            权益中心为 <strong>VIP 会员</strong>专享，使用积分即可兑换优惠券、实物小商品等好礼
          </p>
          <button class="vip-strip__link" @click="goMembership">
            {{ membership.currentLevel }} · 查看会员
          </button>
        </div>

        <!-- 领券中心: 仅落地页展示, 券码类权益销量前三 -->
        <section v-if="!entered" class="section">
          <div class="section__header">
            <h2 class="section__title">领券中心</h2>
            <a class="section__link" href="javascript:;" @click.prevent="showFab('COUPON')">查看全部</a>
          </div>
          <div class="coupon-grid">
            <CouponCard
              v-for="coupon in coupons"
              :key="coupon.id"
              :coupon="coupon"
              @select="goDetail"
            />
          </div>
        </section>

        <!-- 兑换列表区（模式驱动）: 落地页各展示 3 张券卡（无筛选）; 兑换模式显示完整列表+筛选 -->
        <section ref="benefitSection" class="section">
          <div class="section__header">
            <h2 class="section__title">
              {{ listMode === 'COUPON' ? '优惠券兑换' : '商品兑换' }}
            </h2>
            <a v-if="!entered" class="section__link" href="javascript:;" @click.prevent="showFab('GOODS')">查看全部</a>
          </div>

          <!-- 分类筛选/搜索/排序: 仅兑换模式的商品模式展示（落地页隐藏, 点进去才展示） -->
          <div v-show="entered && listMode === 'GOODS'" class="filter-bar">
            <div class="cat-row">
              <button
                class="cat-chip"
                :class="{ 'cat-chip--active': activeRoot === null }"
                @click="selectRoot(null)"
              >
                全部
              </button>
              <button
                v-for="c in categories"
                :key="c.id"
                class="cat-chip"
                :class="{ 'cat-chip--active': activeRoot === c.id }"
                @click="selectRoot(c.id)"
              >
                {{ c.name }}
              </button>
            </div>
            <div v-if="activeChildren.length" class="cat-row cat-row--sub">
              <button
                class="cat-chip cat-chip--sm"
                :class="{ 'cat-chip--active': activeChild === null }"
                @click="selectChild(null)"
              >
                全部
              </button>
              <button
                v-for="c in activeChildren"
                :key="c.id"
                class="cat-chip cat-chip--sm"
                :class="{ 'cat-chip--active': activeChild === c.id }"
                @click="selectChild(c.id)"
              >
                {{ c.name }}
              </button>
            </div>

            <!-- 搜索与排序 -->
            <div class="filter-controls">
              <el-input
                v-model="keyword"
                class="filter-controls__search"
                placeholder="搜索权益名称"
                clearable
                @keyup.enter="onSearch"
                @clear="onSearch"
              >
                <template #append>
                  <el-button @click="onSearch">
                    <span class="ms-icon filter-controls__search-icon">search</span>
                  </el-button>
                </template>
              </el-input>
              <el-select v-model="sortType" class="filter-controls__sort" @change="onSortChange">
                <el-option :value="1" label="综合排序" />
                <el-option :value="2" label="销量优先" />
                <el-option :value="3" label="积分 从低到高" />
                <el-option :value="4" label="积分 从高到低" />
              </el-select>
            </div>
          </div>

          <div v-loading="listLoading" class="product-grid">
            <!-- 落地页: 商品兑换与领券中心风格统一, 只展示 3 张券卡样式 -->
            <!-- 兑换模式: COUPON-券卡罗列 / GOODS-带图商品卡片 -->
            <template v-if="!entered || listMode === 'COUPON'">
              <CouponCard
                v-for="(c, i) in landingCouponCards"
                :key="c.id"
                :coupon="c"
                @select="goDetail"
              />
            </template>
            <template v-else>
              <BenefitCard
                v-for="b in benefits"
                :key="b.id"
                :benefit="b"
                @select="goDetail(b.id)"
              />
            </template>
            <el-empty
              v-if="!listLoading && !benefits.length"
              class="product-grid__empty"
              :description="listMode === 'COUPON' ? '暂无符合条件的优惠券' : '暂无符合条件的商品'"
            />
          </div>
          <el-pagination
            v-if="entered"
            class="product-pagination"
            layout="prev, pager, next"
            :total="total"
            :page-size="PAGE_SIZE"
            :current-page="page"
            background
            hide-on-single-page
            @current-change="onPageChange"
          />
        </section>
      </main>

      <!-- 右侧小组件（4 列）: 积分余额实时, 兑换记录来自我的权益 -->
      <aside class="benefit-aside">
        <!-- 我的积分余额 -->
        <div class="card balance-card">
          <div class="balance-card__deco" />
          <h3 class="balance-card__title">我的积分余额</h3>
          <div class="balance-card__value">
            <span class="balance-card__num">{{ formatNumber(summary.total) }}</span>
            <span class="balance-card__unit">pts</span>
          </div>
          <div class="balance-card__progress">
            <div class="balance-card__levels">
              <span>{{ membership.currentLevel }}</span>
              <span>{{ membership.nextLevel }} ({{ formatNumber(membership.nextLevelPoints) }} 积分)</span>
            </div>
            <el-progress
              :percentage="membership.progress"
              :stroke-width="8"
              :show-text="false"
              class="balance-card__bar"
            />
          </div>
          <el-button class="balance-card__earn" @click="earnMore">赚取更多积分</el-button>
        </div>

        <!-- 兑换记录 -->
        <div class="card history-card">
          <div class="history-card__header">
            <h3 class="history-card__title">兑换记录</h3>
            <button class="history-card__icon-btn" @click="viewAllHistory">
              <span class="ms-icon">history</span>
            </button>
          </div>
          <div class="history-card__list">
            <div v-for="item in history" :key="item.id" class="history-item">
              <div class="history-item__icon">
                <span class="ms-icon">{{ item.icon }}</span>
              </div>
              <div class="history-item__info">
                <p class="history-item__name">{{ item.title }}</p>
                <p class="history-item__date">{{ item.date }}</p>
              </div>
              <span class="history-item__points">{{ item.points }}</span>
            </div>
          </div>
          <a class="history-card__more" href="javascript:;" @click.prevent="viewAllHistory">
            查看完整历史
          </a>
        </div>
      </aside>
    </div>

    <!-- 悬浮 Fab 兑换快捷菜单（进入兑换模式后显示; hover 展开, 模式切换 + 分类筛选） -->
    <FabExchangeMenu
      v-show="entered"
      :categories="categoryNames"
      @switch-mode="onSwitchMode"
      @select-category="onFabSelectCategory"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import CouponCard from '@/components/CouponCard.vue'
import BenefitCard from '@/components/BenefitCard.vue'
import FabExchangeMenu from '@/components/FabExchangeMenu.vue'
import { getBenefitCategories, getBenefits, getBenefitBanner, getCoupons, getRedemptionHistory } from '@/api/benefit'
import { getMembership } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const PAGE_SIZE = 8
const { user, loadUser } = useUserStore()

// ---- 页面基础数据(banner/领券中心/侧栏兑换记录) ----
const loading = ref(false)
// hero 固定文案兜底(真实标题/底图由 /banner?position=benefit 覆盖), 避免加载前后空白闪烁
const banner = ref({ tag: 'VIP 专享', title: '会员专属权益', subtitle: 'VIP 会员可用积分兑换优惠券、实物小商品等专属好礼', buttonText: '探索所有权益', image: '' })
const coupons = ref([])
const history = ref([])
const membership = ref({ currentLevel: '', nextLevel: '', nextLevelPoints: 0, progress: 0 })

// 悬浮 Fab: 点击"探索所有权益"后显示
const fabVisible = ref(false)

// 从用户信息计算积分余额
const summary = computed(() => ({
  total: user.points || 0,
}))

// 监听用户积分变化，更新会员信息
watch(
  () => [user.points, user.level],
  () => {
    membership.value = getMembership(user.points || 0)
  },
  { immediate: true },
)

// ---- 权益列表(真实接口) ----
const listLoading = ref(false)
const categories = ref([])
const benefits = ref([])
const total = ref(0)
const page = ref(1)
const activeRoot = ref(null) // 一级分类ID, null=全部
const activeChild = ref(null) // 二级分类ID, null=该一级下全部
const keyword = ref('') // 输入框
const sortType = ref(1)

// Fab 驱动的兑换模式: 落地页为商品兑换(GOODS), 点"探索所有权益"进入优惠券兑换(COUPON), 经 Fab 互相切换
const listMode = ref('GOODS')

const activeChildren = computed(
  () => categories.value.find((c) => c.id === activeRoot.value)?.children || [],
)

// Fab 分类下拉的分类名列表
const categoryNames = computed(() => categories.value.map((c) => c.name))

const formatNumber = (n) => Number(n || 0).toLocaleString('en-US')

// COUPON 模式: LifeBenefit 实体 → 无图券卡结构（原优惠券卡片样式）
const COUPON_THEMES = ['orange', 'primary', 'green']

// 券卡映射: badge 角标文案区分 券码/好物
function toCouponCard(b, i, badge) {
  return {
    id: b.id,
    icon: badge === '专属好物' ? 'redeem' : 'confirmation_number',
    value: badge,
    theme: COUPON_THEMES[i % COUPON_THEMES.length],
    title: b.name,
    desc: b.description?.slice(0, 24) || '会员专属权益',
    expireText:
      b.validType === 1
        ? `兑换后 ${b.validDays || 0} 天内有效`
        : b.fixedEndTime
          ? `有效期至 ${String(b.fixedEndTime).slice(0, 10)}`
          : '长期有效',
    points: b.pointsPrice,
  }
}

// 兑换模式券列表卡片（专属券码）
const couponCards = computed(() => benefits.value.map((b, i) => toCouponCard(b, i, '专属券码')))

// 落地页商品兑换卡片（专属好物, 与领券中心风格统一, 只展示 3 个）
const landingCouponCards = computed(() => benefits.value.slice(0, 3).map((b, i) => toCouponCard(b, i, '专属好物')))

// 页面两态: 落地页(Banner+探索按钮) / 兑换模式(直接列表, 无 Banner, Fab 常驻)
const entered = ref(false)

// 点击"探索所有权益"/"查看全部": 进入兑换模式（隐藏 Banner/VIP条/领券中心）
// mode: COUPON-优惠券列表 / GOODS-商品列表（带筛选）, 并显示右下角悬浮 Fab
function showFab(mode = 'COUPON') {
  entered.value = true
  listMode.value = mode
  activeRoot.value = null
  activeChild.value = null
  keyword.value = ''
  sortType.value = 1
  page.value = 1
  fetchList()
  nextTick(() => {
    benefitSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

// 请求序号: Fab 模式切换与分类筛选连续触发时, 丢弃过期响应避免旧结果覆盖新筛选
let fetchSeq = 0

async function fetchList() {
  const seq = ++fetchSeq
  listLoading.value = true
  try {
    const res = await getBenefits({
      pageNum: page.value,
      // 落地页只展示 3 个, 兑换模式完整分页列表
      pageSize: entered.value ? PAGE_SIZE : 3,
      // 按模式分流: 优惠券模式取券类, 商品模式取直充类小商品
      benefitType: listMode.value === 'COUPON' ? 1 : 2,
      // 二级优先; 只选了一级时传一级ID
      categoryId: activeChild.value ?? activeRoot.value ?? undefined,
      keyword: keyword.value.trim() || undefined,
      sortType: sortType.value,
    })
    if (seq !== fetchSeq) return
    benefits.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) {
    if (seq !== fetchSeq) return
    ElMessage.error(e.message || '加载列表失败')
  } finally {
    if (seq === fetchSeq) listLoading.value = false
  }
}

// Fab: 切换兑换模式（重置筛选与分页）
function onSwitchMode(mode) {
  if (listMode.value === mode) return
  listMode.value = mode
  activeRoot.value = null
  activeChild.value = null
  keyword.value = ''
  sortType.value = 1
  page.value = 1
  fetchList()
}

// Fab: 分类下拉选中（分类名 → 分类ID）
function onFabSelectCategory(name) {
  activeRoot.value = name === '全部' ? null : categories.value.find((c) => c.name === name)?.id ?? null
  activeChild.value = null
  page.value = 1
  fetchList()
}

function selectRoot(id) {
  if (activeRoot.value === id) return
  activeRoot.value = id
  activeChild.value = null
  page.value = 1
  fetchList()
}

function selectChild(id) {
  if (activeChild.value === id) return
  activeChild.value = id
  page.value = 1
  fetchList()
}

function onSearch() {
  page.value = 1
  fetchList()
}

function onSortChange() {
  page.value = 1
  fetchList()
}

function onPageChange(p) {
  page.value = p
  fetchList()
}

function goDetail(id) {
  router.push(`/benefit/${id}`)
}

function goMembership() {
  router.push('/profile')
}

const benefitSection = ref(null)

function earnMore() {
  ElMessage.info('可每日签到、发布动态、消费返积分赚取积分')
}

function viewAllHistory() {
  router.push('/my-benefits')
}

onMounted(async () => {
  loading.value = true
  try {
    // 并行加载用户信息和其他数据
    await loadUser(true)
    banner.value = await getBenefitBanner()
    coupons.value = await getCoupons()
    history.value = await getRedemptionHistory()
    categories.value = await getBenefitCategories()
    membership.value = getMembership(user.points || 0)
  } catch (e) {
    // 单项失败不阻塞权益列表加载
    ElMessage.error(e.message || '页面数据加载失败')
  } finally {
    loading.value = false
  }
  fetchList()
})
</script>

<style scoped>
.benefit-wrapper {
  display: flex;
  flex-grow: 1;
  width: 100%;
  min-height: 100vh;
}

.benefit-main {
  width: 100%;
  max-width: 760px;
  min-width: 0;
  padding: 20px 16px 40px;
  border-right: 1px solid var(--color-border);
}

/* Banner */
.hero {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 300px;
  border: 1px solid var(--color-border-strong);
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-surface-low);
}

.hero__bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0.4;
}

.hero__content {
  position: relative;
  z-index: 1;
  padding: 40px;
  max-width: 640px;
}

.hero__tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 16px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 700;
  color: #8a5800;
  background: rgba(255, 215, 0, 0.22);
  border-radius: 9999px;
}

.hero__tag-icon {
  font-size: 14px;
}

.hero__title {
  margin: 0 0 12px;
  font-size: 40px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
  color: var(--color-text);
}

.hero__subtitle {
  margin: 0 0 24px;
  font-size: 16px;
  color: var(--color-text-secondary);
}

.hero__btn.el-button {
  background: var(--color-primary);
  border: none;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  padding: 12px 24px;
  border-radius: 8px;
}

.hero__btn.el-button:hover {
  background: var(--color-primary-hover);
  color: #fff;
}

/* 会员提示条 */
.vip-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 12px 16px;
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.12), rgba(254, 107, 0, 0.08));
  border: 1px solid rgba(255, 215, 0, 0.35);
  border-radius: 12px;
}

.vip-strip__icon {
  font-size: 20px;
  color: var(--color-gold);
  flex-shrink: 0;
}

.vip-strip__text {
  margin: 0;
  flex: 1;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.vip-strip__text strong {
  color: var(--color-orange);
}

.vip-strip__link {
  flex-shrink: 0;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 700;
  color: #7a4f00;
  background: linear-gradient(90deg, #ffd700, #ffb300);
  border: none;
  border-radius: 9999px;
  cursor: pointer;
  transition: filter 0.2s;
}

.vip-strip__link:hover {
  filter: brightness(1.05);
}

/* 区块通用 */
.section {
  margin-top: 32px;
}

.section__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.section__link {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
}

/* 筛选栏 */
.filter-bar {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

.cat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cat-row--sub {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-border);
}

.cat-chip {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-surface-low);
  border: 1px solid transparent;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.cat-chip:hover {
  color: var(--color-primary);
  background: rgba(29, 155, 240, 0.06);
}

.cat-chip--active {
  color: #fff;
  background: var(--color-primary);
  font-weight: 600;
}

.cat-chip--sm {
  padding: 4px 12px;
  font-size: 12px;
}

.cat-chip--sm.cat-chip--active {
  background: rgba(29, 155, 240, 0.12);
  color: var(--color-primary);
  border-color: rgba(29, 155, 240, 0.4);
}

.filter-controls {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.filter-controls__search {
  max-width: 280px;
}

.filter-controls__search-icon {
  font-size: 16px;
}

.filter-controls__sort {
  width: 160px;
}

/* 优惠券网格 */
.coupon-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

@media (min-width: 768px) {
  .coupon-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 权益网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  min-height: 200px;
}

@media (min-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.product-grid__empty {
  grid-column: 1 / -1;
}

.product-pagination {
  margin-top: 24px;
  justify-content: center;
}

/* 右侧栏 */
.benefit-aside {
  display: none;
  width: 350px;
  flex-shrink: 0;
  padding: 8px 0 0 24px;
}

.benefit-aside .card + .card {
  margin-top: 24px;
}

/* 积分余额 */
.balance-card {
  position: relative;
  padding: 24px;
  overflow: hidden;
}

.balance-card__deco {
  position: absolute;
  right: -32px;
  top: -32px;
  width: 128px;
  height: 128px;
  border-radius: 50%;
  background: rgba(29, 155, 240, 0.05);
}

.balance-card__title {
  position: relative;
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.balance-card__value {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 20px;
}

.balance-card__num {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1;
  color: var(--color-primary);
}

.balance-card__unit {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  padding-bottom: 2px;
}

.balance-card__progress {
  position: relative;
  margin-bottom: 8px;
}

.balance-card__levels {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.balance-card__bar :deep(.el-progress-bar__outer) {
  background: var(--color-surface-hover);
}

.balance-card__bar :deep(.el-progress-bar__inner) {
  background: linear-gradient(to right, var(--color-primary), var(--color-orange));
}

.balance-card__earn.el-button {
  width: 100%;
  margin-top: 16px;
  background: var(--color-surface-low);
  border: 1px solid rgba(29, 155, 240, 0.2);
  color: var(--color-primary);
  font-weight: 600;
  font-size: 13px;
  border-radius: 8px;
}

.balance-card__earn.el-button:hover {
  background: rgba(29, 155, 240, 0.06);
  border-color: rgba(29, 155, 240, 0.4);
  color: var(--color-primary);
}

/* 兑换记录 */
.history-card {
  padding: 24px;
}

.history-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.history-card__title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
}

.history-card__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color 0.2s;
}

.history-card__icon-btn:hover {
  background: var(--color-surface-low);
}

.history-card__icon-btn .ms-icon {
  font-size: 18px;
}

.history-card__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--color-surface-low);
  color: var(--color-primary);
  flex-shrink: 0;
}

.history-item__info {
  flex: 1;
  min-width: 0;
}

.history-item__name {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.history-item__date {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--color-text-muted);
}

.history-item__points {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-danger);
}

.history-card__more {
  display: block;
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
}

@media (min-width: 1024px) {
  .benefit-aside {
    display: block;
  }
}
</style>
