import request from './request'
import { submitOrder } from './order'

/**
 * 权益模块接口（权益中心：VIP 会员用积分兑换优惠券、小商品）
 * 后端 Result 包装已在请求层统一剥离, 此处直接拿到业务数据
 */

// 权益分类树: [{ id, name, icon, children: [...] }]
export function getBenefitCategories() {
  return request.get('/benefit/category')
}

// 权益分页列表: params { pageNum, pageSize, categoryId, keyword, sortType }
// 返回 { total, records }
export function getBenefits(params) {
  return request.get('/benefit/page', { params })
}

// 权益详情(含当前用户已兑次数与可兑换标识)
export function getBenefitDetail(id) {
  return request.get(`/benefit/${id}`)
}

/**
 * 积分兑换权益（后端契约: 兑换 = 提交权益订单, 纯积分即时结算并发放券码）
 * 对应 OrderController POST /order, items 中 itemType=2 表示权益
 */
export function exchangeBenefit(id, quantity = 1) {
  return submitOrder({ items: [{ itemType: 2, itemId: id, quantity }] })
}

// —— 我的权益（卡包）: 后端 LifeUserBenefitController /benefit/mine/** ——

// 我的权益分页: params { pageNum, pageSize, status? }
// status: 0待生效 / 1未使用 / 2已使用 / 3已过期 / 4已作废
export function getMyBenefits(params = {}) {
  return request.get('/benefit/mine/page', { params })
}

// 我的权益详情（含券码 code）
export function getMyBenefitDetail(id) {
  return request.get(`/benefit/mine/${id}`)
}

// 核销/使用权益
export function verifyMyBenefit(id) {
  return request.put(`/benefit/mine/${id}/verify`)
}

export const MY_BENEFIT_STATUS_MAP = {
  0: { label: '待生效', type: 'info' },
  1: { label: '未使用', type: 'success' },
  2: { label: '已使用', type: 'info' },
  3: { label: '已过期', type: 'danger' },
  4: { label: '已作废', type: 'danger' },
}

// 权益中心落地页 Hero —— 真实内容(标题/底图)来自后端 GET /banner?position=benefit;
// tag/引导语/buttonText 为本页固定 UI 文案(非业务数据)。后端无数据时标题给默认引导语。
export async function getBenefitBanner() {
  const rows = await request.get('/banner', { params: { position: 'benefit' } })
  const b = Array.isArray(rows) && rows.length ? rows[0] : null
  return {
    tag: 'VIP 专享',
    title: b?.title || '会员专属权益',
    subtitle: 'VIP 会员可用积分兑换优惠券、实物小商品等专属好礼',
    buttonText: '探索所有权益',
    image: b?.image || '',
    url: b?.url || '',
  }
}

/**
 * 积分兑券列表（领券中心）: 券码类权益(benefitType=1)销量前三转换为优惠券卡片结构,
 * 点击卡片跳权益详情走完整兑换流程
 */
export async function getCoupons() {
  const res = await request.get('/benefit/page', {
    params: { pageNum: 1, pageSize: 3, sortType: 2, benefitType: 1 },
  })
  const themes = ['orange', 'primary', 'green']
  return (res?.records || []).map((b, i) => ({
    id: b.id,
    icon: 'confirmation_number',
    value: '专属券码',
    theme: themes[i % themes.length],
    title: b.name,
    desc: b.description?.slice(0, 20) || '会员专属权益',
    expireText:
      b.validType === 1
        ? `兑换后 ${b.validDays || 0} 天内有效`
        : b.fixedEndTime
          ? `有效期至 ${String(b.fixedEndTime).slice(0, 10)}`
          : '长期有效',
    points: b.pointsPrice,
  }))
}

/**
 * 兑换记录（权益中心右侧栏）: 我的权益最近 3 条,
 * 并按订单号关联积分流水(type=6 积分兑换)还原消耗积分
 */
export async function getRedemptionHistory() {
  const [mineRes, pointsRes] = await Promise.all([
    request.get('/benefit/mine/page', { params: { pageNum: 1, pageSize: 3 } }).catch(() => null),
    request
      .get('/points/record/page', { params: { pageNum: 1, pageSize: 20 } })
      .catch(() => null),
  ])
  const pointsByOrderNo = new Map()
  ;(pointsRes?.records || []).forEach((r) => {
    if (r.orderNo && !pointsByOrderNo.has(r.orderNo)) pointsByOrderNo.set(r.orderNo, r.points)
  })
  return (mineRes?.records || []).map((b) => ({
    id: b.id,
    icon: 'card_giftcard',
    title: b.benefitName,
    date: b.createTime ? String(b.createTime).slice(0, 10) : '',
    points: pointsByOrderNo.get(b.orderNo),
  }))
}
