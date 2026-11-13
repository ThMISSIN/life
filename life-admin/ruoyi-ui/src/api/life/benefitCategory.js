import request from '@/utils/request'

// 查询权益分类全量列表(管理端含禁用)
export function listBenefitCategory(query) {
  return request({
    url: '/life/benefitCategory/list',
    method: 'get',
    params: query
  })
}

// 查询权益分类详细
export function getBenefitCategory(id) {
  return request({
    url: '/life/benefitCategory/' + id,
    method: 'get'
  })
}

// 新增权益分类
export function addBenefitCategory(data) {
  return request({
    url: '/life/benefitCategory',
    method: 'post',
    data: data
  })
}

// 修改权益分类
export function updateBenefitCategory(data) {
  return request({
    url: '/life/benefitCategory',
    method: 'put',
    data: data
  })
}

// 删除权益分类(存在子分类或挂载权益时禁止)
export function delBenefitCategory(id) {
  return request({
    url: '/life/benefitCategory/' + id,
    method: 'delete'
  })
}
