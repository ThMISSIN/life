import request from '@/utils/request'

// 查询权益发放记录列表(含券码; 1.7增强: 权益名/用户关键词/获得时间区间筛选)
export function listUserBenefit(query) {
  return request({
    url: '/life/userBenefit/list',
    method: 'get',
    params: query
  })
}

// 作废发放记录(券作废+退积分+回库存减销量)
export function voidUserBenefit(id) {
  return request({
    url: '/life/userBenefit/void/' + id,
    method: 'put'
  })
}

// 商家按券码核销(1.7): 校验通过后置已使用, 返回发放记录供回执展示
export function verifyUserBenefitByCode(code) {
  return request({
    url: '/life/userBenefit/verify/' + encodeURIComponent(code),
    method: 'put'
  })
}
