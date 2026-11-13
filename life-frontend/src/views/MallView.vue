<template>
  <div class="container-shell">
    <AppSidebar />

    <div class="mall-wrapper">
      <main v-loading="loading" class="mall-main">
        <!-- 顶部搜索栏 -->
        <div class="mall-topbar">
          <h1 class="mall-topbar__title">商城</h1>
          <el-input
            v-model="keyword"
            class="mall-topbar__search"
            placeholder="搜索商品，例如：咖啡 / 耳机"
            clearable
            @keyup.enter="onSearch"
            @clear="onSearch"
          >
            <template #prefix>
              <span class="ms-icon mall-topbar__search-icon">search</span>
            </template>
          </el-input>
          <button class="mall-topbar__cart" @click="goCart">
            <span class="ms-icon">shopping_cart</span>
            <span class="mall-topbar__cart-text">购物车</span>
          </button>
        </div>

        <!-- 轮播 Banner -->
        <section class="banner">
          <el-carousel height="280px" :interval="5000" arrow="hover" indicator-position="none">
            <el-carousel-item v-for="b in banners" :key="b.id">
              <div class="banner__item" :style="{ backgroundImage: `url(${b.image})` }">
                <div class="banner__mask" />
                <div class="banner__content">
                  <span class="banner__tag">{{ b.tag }}</span>
                  <h2 class="banner__title">{{ b.title }}</h2>
                  <p class="banner__subtitle">{{ b.subtitle }}</p>
                  <el-button class="banner__btn" round @click="goCategory(null)">
                    {{ b.buttonText }}
                  </el-button>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </section>

        <!-- 分类快捷入口 -->
        <section class="section section--cats">
          <button
            v-for="c in categories"
            :key="c.name"
            class="cat-item"
            :class="{ 'cat-item--active': activeCategory === c.key }"
            @click="goCategory(c.key)"
          >
            <span class="cat-item__icon">
              <span class="ms-icon ms-icon--fill">{{ c.icon }}</span>
            </span>
            <span class="cat-item__name">{{ c.name }}</span>
          </button>
        </section>

        <!-- 精选好物 -->
        <section class="section">
          <div class="section__header">
            <h2 class="section__title">{{ sectionTitle }}</h2>
            <!-- 排序 -->
            <div class="sort-bar">
              <button
                v-for="s in sortOptions"
                :key="s.value"
                class="sort-bar__item"
                :class="{ 'sort-bar__item--active': sortType === s.value }"
                @click="onSortChange(s.value)"
              >
                {{ s.label }}
                <span v-if="s.value > 2" class="ms-icon sort-bar__arrow">
                  {{ sortType === s.value ? 'expand_less' : 'expand_more' }}
                </span>
              </button>
            </div>
          </div>

          <div v-loading="listLoading" class="goods-grid">
            <GoodsCard v-for="g in goods" :key="g.id" :goods="g" @select="goDetail(g.id)" />
            <el-empty
              v-if="!listLoading && !goods.length"
              class="goods-grid__empty"
              description="没有找到相关商品，换个关键词试试"
            />
          </div>
          <el-pagination
            class="goods-pagination"
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
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import GoodsCard from '@/components/GoodsCard.vue'
import { getMallBanners, getMallCategories, getGoodsList } from '@/api/mall'

const router = useRouter()
const PAGE_SIZE = 8

const loading = ref(false)
const banners = ref([])
const categories = ref([])
const goods = ref([])
const total = ref(0)
const page = ref(1)
const activeCategory = ref(null)
const keyword = ref('')
const sortType = ref(1)
const listLoading = ref(false)

const sortOptions = [
  { value: 1, label: '综合推荐' },
  { value: 2, label: '销量优先' },
  { value: 3, label: '价格从低到高' },
  { value: 4, label: '价格从高到低' },
]

const sectionTitle = computed(() => {
  if (keyword.value.trim()) return `“${keyword.value.trim()}” 的搜索结果`
  const cat = categories.value.find((c) => c.key === activeCategory.value)
  return cat && cat.key ? `${cat.name} · 精选好物` : '精选好物'
})

async function fetchList() {
  listLoading.value = true
  try {
    const res = await getGoodsList({
      pageNum: page.value,
      pageSize: PAGE_SIZE,
      categoryId: activeCategory.value ?? undefined,
      keyword: keyword.value.trim() || undefined,
      sortType: sortType.value,
    })
    goods.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) {
    ElMessage.error(e.message || '加载商品列表失败')
  } finally {
    listLoading.value = false
  }
}

function goCategory(key) {
  if (activeCategory.value === key) return
  activeCategory.value = key
  page.value = 1
  fetchList()
}

function onSearch() {
  page.value = 1
  fetchList()
}

function onSortChange(type) {
  if (sortType.value === type) return
  sortType.value = type
  page.value = 1
  fetchList()
}

function onPageChange(p) {
  page.value = p
  fetchList()
}

function goDetail(id) {
  router.push(`/mall/goods/${id}`)
}

function goCart() {
  router.push('/cart')
}

onMounted(async () => {
  loading.value = true
  try {
    const [b, c] = await Promise.all([getMallBanners(), getMallCategories()])
    banners.value = b
    categories.value = c
  } catch (e) {
    ElMessage.error(e.message || '页面数据加载失败')
  } finally {
    loading.value = false
  }
  fetchList()
})
</script>

<style scoped>
.mall-wrapper {
  display: flex;
  flex-grow: 1;
  width: 100%;
  min-height: 100vh;
}

.mall-main {
  flex: 1;
  min-width: 0;
  padding: 0 16px 40px;
  border-right: 1px solid var(--color-border);
}

/* 顶部搜索栏 */
.mall-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  margin: 0 -24px;
  padding-left: 24px;
  padding-right: 24px;
  background: rgba(250, 248, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.mall-topbar__title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--color-text);
  flex-shrink: 0;
}

.mall-topbar__search {
  max-width: 480px;
}

.mall-topbar__search :deep(.el-input__wrapper) {
  border-radius: 9999px;
  background: var(--color-search-bg);
  box-shadow: none;
}

.mall-topbar__search-icon {
  font-size: 18px;
  color: var(--color-text-muted);
}

.mall-topbar__cart {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.mall-topbar__cart:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.mall-topbar__cart .ms-icon {
  font-size: 20px;
}

/* 轮播 Banner */
.banner {
  margin-top: 24px;
}

.banner :deep(.el-carousel__container) {
  border-radius: 16px;
  overflow: hidden;
}

.banner__item {
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 40px 48px;
  background-size: cover;
  background-position: center;
}

.banner__mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(15, 20, 25, 0.55), rgba(15, 20, 25, 0.05) 60%);
}

.banner__content {
  position: relative;
  z-index: 1;
  max-width: 520px;
}

.banner__tag {
  display: inline-block;
  margin-bottom: 12px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-orange);
  background: rgba(254, 107, 0, 0.15);
  border-radius: 9999px;
}

.banner__title {
  margin: 0 0 8px;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.2;
  color: #fff;
}

.banner__subtitle {
  margin: 0 0 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.banner__btn.el-button {
  background: var(--color-orange);
  border: none;
  color: #fff;
  font-weight: 700;
  padding: 12px 28px;
}

.banner__btn.el-button:hover {
  background: #e05f00;
  color: #fff;
}

/* 区块通用 */
.section {
  margin-top: 28px;
}

.section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.section__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

/* 分类快捷入口 */
.section--cats {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

@media (max-width: 1023px) {
  .section--cats {
    grid-template-columns: repeat(4, 1fr);
  }
}

.cat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 4px;
  background: transparent;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.cat-item:hover {
  background: var(--color-surface-low);
}

.cat-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 24px;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-radius: 16px;
}

.cat-item--active .cat-item__icon {
  color: #fff;
  background: var(--color-primary);
}

.cat-item__name {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.cat-item--active .cat-item__name {
  color: var(--color-primary);
  font-weight: 700;
}

/* 排序条 */
.sort-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 9999px;
}

.sort-bar__item {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.sort-bar__item:hover {
  color: var(--color-primary);
}

.sort-bar__item--active {
  color: #fff;
  font-weight: 700;
  background: var(--color-primary);
}

.sort-bar__arrow {
  font-size: 16px;
}

/* 商品网格 */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  min-height: 200px;
}

@media (min-width: 768px) {
  .goods-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1200px) {
  .goods-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.goods-grid__empty {
  grid-column: 1 / -1;
}

.goods-pagination {
  margin-top: 24px;
  justify-content: center;
}
</style>
