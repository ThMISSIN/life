import request from '@/utils/request'

// 查询通知模板列表(status/type不传查全部)
export function listNoticeTemplate(query) {
  return request({
    url: '/life/noticeTemplate/list',
    method: 'get',
    params: query
  })
}

// 查询通知模板详细
export function getNoticeTemplate(id) {
  return request({
    url: '/life/noticeTemplate/' + id,
    method: 'get'
  })
}

// 新增通知模板
export function addNoticeTemplate(data) {
  return request({
    url: '/life/noticeTemplate',
    method: 'post',
    data: data
  })
}

// 修改通知模板
export function updateNoticeTemplate(data) {
  return request({
    url: '/life/noticeTemplate',
    method: 'put',
    data: data
  })
}

// 模板启用/禁用
export function changeNoticeTemplateStatus(id, status) {
  return request({
    url: '/life/noticeTemplate/changeStatus',
    method: 'put',
    params: { id, status }
  })
}

// 删除通知模板(软删除)
export function delNoticeTemplate(id) {
  return request({
    url: '/life/noticeTemplate/' + id,
    method: 'delete'
  })
}
