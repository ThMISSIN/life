import request from '@/utils/request'

// 查询积分流水列表
export function listPointsRecord(query) {
  return request({
    url: '/life/pointsRecord/list',
    method: 'get',
    params: query
  })
}
