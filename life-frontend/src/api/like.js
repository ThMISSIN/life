import request from './request'

/**
 * 点赞相关接口（后端 LikeController：/like/**，Redis SET 实时计数）
 *
 * 注意后端契约特殊：
 *   POST /like        参数以 query/form 绑定 LikeRecordDTO{ bizId, bizType, liked }，
 *                     返回 void（无 Result 包装，axios 拿到空字符串）
 *   GET  /like/list   ?bizType=POST&bizIds=1,2,3 返回当前用户已点的 id 数组（无 Result 包装）
 *
 * 业务类型见后端 BizTypeConstant：POST / COMMENT（BENEFIT、GOODS 暂不支持）
 */

// 点赞 / 取消点赞: liked true-点赞 false-取消
export function toggleLike(bizId, bizType = 'POST', liked = true) {
  return request.post('/like', null, { params: { bizId, bizType, liked } })
}

// 批量查询当前用户在 bizIds 中已点赞的 id 集合，返回数组（失败返回空数组）
export async function getLikedBizIds(bizType = 'POST', bizIds = []) {
  if (!bizIds.length) return []
  try {
    const ids = await request.get('/like/list', {
      params: { bizType, bizIds: bizIds.join(',') },
    })
    return Array.isArray(ids) ? ids.map(Number) : []
  } catch {
    return []
  }
}
