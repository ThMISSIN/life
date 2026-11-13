import request from './request'

/**
 * 订单相关接口（后端 OrderController：/order/**，均需登录）
 *
 *   POST  /order               提交订单 → OrderVO
 *   GET   /order/page          我的订单分页 → PageDTO{ total, pages, list }（list 为订单头，不含明细）
 *   GET   /order/{id}          订单详情（含 details 明细）
 *   PUT   /order/{id}/cancel   取消（仅待支付；权益单不支持取消）
 *   PUT   /order/{id}/pay      支付（余额通道，仅待支付）
 *   PUT   /order/{id}/confirm  确认收货（仅已发货）
 *
 * 订单状态 status：0待支付 / 1已支付 / 2已发货 / 3已完成 / 4已取消 / 5已退款
 * 订单类型 orderType：1商品订单（现金，待支付→支付） / 2权益订单（纯积分，提交即成交）
 */

// 签发下单幂等令牌（后端 GET /order/token，一次性，TTL 30 分钟）
export function getOrderToken() {
  return request.get('/order/token')
}

// 提交订单
// items: [{ itemType: 1商品/2权益, itemId, quantity }]；商品单必传 addressId；购物车结算传 cartIds
// pointsDeduct: 商品单可选积分抵扣(积分, 需为100整数倍且不超现金50%), 见 CheckoutDialog 抵扣面板
// orderToken: 幂等令牌, 未显式传入则提交前自动签发(一次性调用方如权益直兑);
//             结算对话框场景应在打开时签发并传入, 使同一结算会话的重复提交被后端拒绝
export async function submitOrder({ items, addressId, remark, cartIds, pointsDeduct, orderToken }) {
  const payload = { items }
  if (addressId) payload.addressId = addressId
  if (remark?.trim()) payload.remark = remark.trim()
  if (cartIds?.length) payload.cartIds = cartIds
  if (pointsDeduct) payload.pointsDeduct = pointsDeduct
  payload.orderToken = orderToken || (await getOrderToken())
  return request.post('/order', payload)
}

// 我的订单分页: params { pageNum, pageSize, status?, orderType? }，返回 { total, pages, list }
export function getOrderPage(params = {}) {
  return request.get('/order/page', { params: { pageNum: 1, pageSize: 10, ...params } })
}

// 订单详情（含明细 details）
export function getOrderDetail(id) {
  return request.get(`/order/${id}`)
}

// 取消订单（仅待支付）
export function cancelOrder(id) {
  return request.put(`/order/${id}/cancel`)
}

// 支付订单（一期为余额支付，仅待支付）
export function payOrder(id) {
  return request.put(`/order/${id}/pay`)
}

// 确认收货（仅已发货）
export function confirmOrder(id) {
  return request.put(`/order/${id}/confirm`)
}

/**
 * 订单状态 → 前端展示
 */
export const ORDER_STATUS_MAP = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'primary' },
  2: { label: '已发货', type: 'primary' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'info' },
  5: { label: '已退款', type: 'info' },
}

export function orderStatusText(status) {
  return ORDER_STATUS_MAP[status]?.label || `状态${status}`
}

// 分 → 元字符串（保留两位小数，去除末尾多余的 0）
export function fenToYuan(fen) {
  const v = Number(fen || 0) / 100
  return v.toFixed(2).replace(/\.?0+$/, '')
}
