import request from '@/utils/request'

// 查询订单列表(含下单人昵称与明细)
export function listOrder(query) {
  return request({
    url: '/life/order/list',
    method: 'get',
    params: query
  })
}

// 查询订单详细(含明细)
export function getOrder(id) {
  return request({
    url: '/life/order/' + id,
    method: 'get'
  })
}

// 订单发货(仅已支付订单)
export function deliverOrder(id) {
  return request({
    url: '/life/order/deliver/' + id,
    method: 'put'
  })
}
