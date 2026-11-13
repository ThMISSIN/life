import request from './request'

/**
 * 商城（实物商品）相关接口 —— 数据全部来自后端，无 mock
 *
 *   GET  /banner?position=mall    轮播 Banner（life_banner 表）
 *   GET  /product/category   分类树 [{ id, name, icon, children }]
 *   GET  /product/page       分页，records 为 LifeProduct 实体（价格为分、images 为 JSON 串、tags 为逗号串）
 *   GET  /product/{id}       详情，返回 ProductDetailVO（images/tags 已解析为数组，含 inStock）
 *   POST /cart               加入购物车（见下方 addCart）
 *   POST /order              下单/结算（见 api/order.js 的 submitOrder）
 */

// 商品通用服务条（纯前端静态展示配置，非业务数据，后端无此模型）
const goodsServices = [
  { icon: 'verified_user', label: '正品保障' },
  { icon: 'local_shipping', label: '全场包邮' },
  { icon: 'assignment_turned_in', label: '七天无理由' },
  { icon: 'currency_exchange', label: '极速退款' },
  { icon: 'support_agent', label: '专属客服' },
]

// 分 → 元（去除多余的 .0 / 末尾 0）
function fenToYuan(fen) {
  if (fen === null || fen === undefined || fen === 0) return 0
  return parseFloat(((fen / 100).toFixed(2)).replace(/\.?0+$/, ''))
}

// 兼容实体(JSON字符串)与VO(数组)两种 images 形态
function parseImages(images, coverImage) {
  let list = []
  if (Array.isArray(images)) {
    list = images
  } else if (typeof images === 'string' && images.trim()) {
    try {
      list = JSON.parse(images)
    } catch {
      list = []
    }
  }
  if (!list.length && coverImage) list = [coverImage]
  return list
}

// 兼容实体(逗号分隔串)与VO(数组)两种 tags 形态
function parseTags(tags) {
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string' && tags.trim()) {
    return tags
      .split(',')
      .map((t) => t.trim())
      .filter(Boolean)
  }
  return []
}

// 后端商品（实体/VO）→ 前端卡片/详情展示结构
function transformProduct(raw) {
  return {
    id: raw.id,
    name: raw.name,
    price: fenToYuan(raw.price),
    originalPrice: fenToYuan(raw.originalPrice),
    pointsPrice: raw.pointsPrice || 0,
    sales: raw.sales || 0,
    shop: raw.shopName,
    location: raw.location,
    image: raw.coverImage,
    detailImages: parseImages(raw.images, raw.coverImage),
    categoryKey: raw.categoryId,
    tags: parseTags(raw.tags),
    description: raw.description || '',
    stock: raw.stock,
    inStock: raw.inStock ?? raw.stock !== 0,
    perUserLimit: raw.perUserLimit || 0,
    minLevel: raw.minLevel || 0,
  }
}

// 获取商城轮播 Banner（后端 GET /banner?position=mall, 上架+生效期内按 sort 升序）
// 空数组=运营未配置轮播, 前端如实不展示（不再回落 mock）
export async function getMallBanners() {
  const rows = await request.get('/banner', { params: { position: 'mall' } })
  return (Array.isArray(rows) ? rows : []).map((b) => ({
    id: b.id,
    tag: '',
    title: b.title,
    subtitle: '',
    buttonText: '立即抢购',
    image: b.image,
    url: b.url,
  }))
}

// 获取商城分类快捷入口（后端返回一级分类树，转换为卡片所需 { key, name, icon }）
export async function getMallCategories() {
  const tree = await request.get('/product/category')
  return [
    { key: null, name: '全部', icon: 'apps' },
    ...(tree || []).map((c) => ({
      key: c.id,
      name: c.name,
      icon: c.icon || 'category',
    })),
  ]
}

// 商品分页列表: params { pageNum, pageSize, categoryId, keyword, sortType }
// sortType: 1-综合 2-销量优先 3-价格升序 4-价格降序
// 返回 { total, records }，records 已转换为前端展示结构
export async function getGoodsList({ pageNum = 1, pageSize = 8, categoryId, keyword, sortType = 1 } = {}) {
  const params = { pageNum, pageSize, sortType }
  if (categoryId !== undefined && categoryId !== null) params.categoryId = categoryId
  if (keyword?.trim()) params.keyword = keyword.trim()
  const res = await request.get('/product/page', { params })
  return {
    total: Number(res?.total || 0),
    records: (res?.records || []).map(transformProduct),
  }
}

// 商品详情（服务保障为前端静态文案，不入库）
export async function getGoodsDetail(id) {
  const raw = await request.get(`/product/${id}`)
  if (!raw) throw new Error('商品不存在或已下架')
  return { ...transformProduct(raw), services: goodsServices }
}

// 加入购物车（后端 CartController：POST /cart，itemType 1-实物商品；同款自动合并数量）
export async function addCart(goodsId, quantity = 1) {
  await request.post('/cart', { itemType: 1, itemId: goodsId, quantity })
  return { success: true, message: '已加入购物车，可前往购物车结算' }
}
