import request from './request'

/**
 * 收藏接口（后端 FavoriteController, 均需登录）
 * bizType: 1-帖子, 2-权益, 3-商品
 */

// 收藏/取消 toggle: 返回 true=已收藏, false=已取消
export function toggleFavorite(bizType, bizId) {
  return request.post('/favorite', { bizType, bizId })
}

// 收藏分页: params { pageNum, pageSize, bizType? }
// 返回 { total, records }, record 含 name/image/price/pointsPrice/valid
export function getFavorites(params = {}) {
  return request.get('/favorite/page', { params })
}

// 批量收藏状态(列表页角标): bizType + ids 数组 -> 已收藏的 bizId 列表
export function getFavoriteStatus(bizType, ids = []) {
  if (!ids.length) return Promise.resolve([])
  return request.get('/favorite/list', { params: { bizType, ids: ids.join(',') } })
}
