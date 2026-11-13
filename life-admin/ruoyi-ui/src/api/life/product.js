import request from '@/utils/request'

// 查询商品列表(管理端口径: status不传查全部含下架)
export function listProduct(query) {
  return request({
    url: '/life/product/list',
    method: 'get',
    params: query
  })
}

// 查询商品详细
export function getProduct(id) {
  return request({
    url: '/life/product/' + id,
    method: 'get'
  })
}

// 新增商品
export function addProduct(data) {
  return request({
    url: '/life/product',
    method: 'post',
    data: data
  })
}

// 修改商品
export function updateProduct(data) {
  return request({
    url: '/life/product',
    method: 'put',
    data: data
  })
}

// 商品上下架
export function changeProductStatus(id, status) {
  return request({
    url: '/life/product/changeStatus',
    method: 'put',
    params: { id, status }
  })
}

// 删除商品(软删除, 上架中禁止)
export function delProduct(id) {
  return request({
    url: '/life/product/' + id,
    method: 'delete'
  })
}
