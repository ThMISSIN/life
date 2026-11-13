import request from '@/utils/request'

// 查询余额流水列表
export function listBalanceRecord(query) {
  return request({
    url: '/life/balanceRecord/list',
    method: 'get',
    params: query
  })
}
