import request from '@/utils/request'

// 查询权益列表(管理端口径: status不传查全部含下架)
export function listBenefit(query) {
  return request({
    url: '/life/benefit/list',
    method: 'get',
    params: query
  })
}

// 查询权益详细
export function getBenefit(id) {
  return request({
    url: '/life/benefit/' + id,
    method: 'get'
  })
}

// 新增权益
export function addBenefit(data) {
  return request({
    url: '/life/benefit',
    method: 'post',
    data: data
  })
}

// 修改权益
export function updateBenefit(data) {
  return request({
    url: '/life/benefit',
    method: 'put',
    data: data
  })
}

// 权益上下架
export function changeBenefitStatus(id, status) {
  return request({
    url: '/life/benefit/changeStatus',
    method: 'put',
    params: { id, status }
  })
}

// 删除权益(存在未使用发放记录时禁止)
export function delBenefit(id) {
  return request({
    url: '/life/benefit/' + id,
    method: 'delete'
  })
}
