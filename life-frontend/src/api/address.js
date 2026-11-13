import request from './request'

/**
 * 收货地址相关接口（后端 UserAddressController：/user/address/**，均需登录）
 *
 *   GET    /user/address              地址列表（默认地址排前）
 *   POST   /user/address              新增（不传 id）
 *   PUT    /user/address              修改（id 必传）
 *   DELETE /user/address/{id}         删除
 *   PUT    /user/address/{id}/default 设为默认
 */

// 地址列表
export function getAddressList() {
  return request.get('/user/address/list')
}

// 新增地址: { receiverName, receiverPhone, province, city, district, address, isDefault }
export function addAddress(data) {
  return request.post('/user/address', data)
}

// 修改地址（data.id 必传）
export function updateAddress(data) {
  return request.put('/user/address', data)
}

// 删除地址
export function deleteAddress(id) {
  return request.delete(`/user/address/${id}`)
}

// 设为默认地址
export function setDefaultAddress(id) {
  return request.put(`/user/address/${id}/default`)
}
