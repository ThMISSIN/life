import request from '@/utils/request'

// 查询公告列表(status不传查全部含草稿)
export function listAnnouncement(query) {
  return request({
    url: '/life/announcement/list',
    method: 'get',
    params: query
  })
}

// 查询公告详细
export function getAnnouncement(id) {
  return request({
    url: '/life/announcement/' + id,
    method: 'get'
  })
}

// 新增公告(默认草稿, 表单可选直接发布)
export function addAnnouncement(data) {
  return request({
    url: '/life/announcement',
    method: 'post',
    data: data
  })
}

// 修改公告(已发布需先撤回)
export function updateAnnouncement(data) {
  return request({
    url: '/life/announcement',
    method: 'put',
    data: data
  })
}

// 发布公告(触发C端收件箱fan-out与实时推送)
export function publishAnnouncement(id) {
  return request({
    url: '/life/announcement/publish/' + id,
    method: 'put'
  })
}

// 下架(撤回)公告
export function revokeAnnouncement(id) {
  return request({
    url: '/life/announcement/revoke/' + id,
    method: 'put'
  })
}

// 删除公告(软删除, 已发布禁止)
export function delAnnouncement(id) {
  return request({
    url: '/life/announcement/' + id,
    method: 'delete'
  })
}
