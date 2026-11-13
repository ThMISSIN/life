import request from '@/utils/request'

// 查询商品分类全量列表(管理端含禁用)
export function listProductCategory(query) {
  return request({
    url: '/life/productCategory/list',
    method: 'get',
    params: query
  })
}

// 查询商品分类详细
export function getProductCategory(id) {
  return request({
    url: '/life/productCategory/' + id,
    method: 'get'
  })
}

// 新增商品分类
export function addProductCategory(data) {
  return request({
    url: '/life/productCategory',
    method: 'post',
    data: data
  })
}

// 修改商品分类
export function updateProductCategory(data) {
  return request({
    url: '/life/productCategory',
    method: 'put',
    data: data
  })
}

// 删除商品分类(存在子分类或挂载商品时禁止)
export function delProductCategory(id) {
  return request({
    url: '/life/productCategory/' + id,
    method: 'delete'
  })
}
