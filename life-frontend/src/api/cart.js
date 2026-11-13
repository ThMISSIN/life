import request from './request'

/**
 * 购物车相关接口（后端 CartController：/cart/**，均需登录）
 *
 *   POST    /cart        加入购物车（同款合并数量）
 *   GET     /cart/list   购物车列表（实时校验库存/上架，失效行 valid=false）
 *   PUT     /cart/{id}   修改数量（query: quantity）
 *   DELETE  /cart/{id}   删除单行
 *   DELETE  /cart        清空购物车
 *
 * CartVO: { id(行ID), itemType(1商品/2权益), itemId, itemName, itemImage,
 *           price(现金分), pointsPrice(积分), quantity, stock(-1不限),
 *           onSale, valid(上架且有库存), createTime }
 */

// 加入购物车: itemType 1-实物商品 2-虚拟权益
export function addCart({ itemType, itemId, quantity = 1 }) {
  return request.post('/cart', { itemType, itemId, quantity })
}

// 购物车列表
export function getCartList() {
  return request.get('/cart/list')
}

// 修改数量
export function updateCartQuantity(id, quantity) {
  return request.put(`/cart/${id}`, null, { params: { quantity } })
}

// 删除单行
export function removeCart(id) {
  return request.delete(`/cart/${id}`)
}

// 清空购物车
export function clearCart() {
  return request.delete('/cart')
}
