import request from './request'

/**
 * 关注相关接口（后端 LifeFollowController：/follow/**）
 *
 *   POST   /follow/{followUserId}          关注（幂等）→ FollowResultVO{ followed, followerCount, followingCount }
 *   DELETE /follow/{followUserId}          取关 → FollowResultVO
 *   GET    /follow/following?userId=       关注列表（userId 不传=本人），分页，total 即关注数
 *   GET    /follow/followers?userId=       粉丝列表（同上，total 即粉丝数）
 *   GET    /follow/{followUserId}/status   当前登录用户是否已关注 TA → boolean
 *
 * 列表 records 为 FollowUserVO{ author: { userId, username, nickname, avatar, followed }, followTime }
 */

// 关注
export function followUser(followUserId) {
  return request.post(`/follow/${followUserId}`)
}

// 取关
export function unfollowUser(followUserId) {
  return request.delete(`/follow/${followUserId}`)
}

// 关注列表: params { userId?, pageNum, pageSize }，返回 { total, records }
export function getFollowingList(params = {}) {
  return request.get('/follow/following', { params: { pageNum: 1, pageSize: 20, ...params } })
}

// 粉丝列表: params { userId?, pageNum, pageSize }，返回 { total, records }
export function getFollowersList(params = {}) {
  return request.get('/follow/followers', { params: { pageNum: 1, pageSize: 20, ...params } })
}

// 查询是否已关注某用户
export function getFollowStatus(followUserId) {
  return request.get(`/follow/${followUserId}/status`)
}
