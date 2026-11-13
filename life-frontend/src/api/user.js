import request from './request'

/**
 * 积分类型映射（后端 type → 前端显示）
 */
const POINTS_TYPE_MAP = {
  1: { icon: 'check_circle', type: '每日签到' },
  2: { icon: 'shopping_bag', type: '消费' },
  3: { icon: 'card_giftcard', type: '活动奖励' },
  4: { icon: 'person_add', type: '邀请好友' },
  5: { icon: 'rate_review', type: '评价奖励' },
  6: { icon: 'redeem', type: '积分兑换' },
}

/**
 * 将后端积分记录转换为前端显示格式
 */
function transformPointsRecord(record) {
  const typeInfo = POINTS_TYPE_MAP[record.type] || { icon: 'help', type: '其他' }
  const createTime = record.createTime ? new Date(record.createTime) : new Date()
  const timeStr = `${createTime.getFullYear()}-${String(createTime.getMonth() + 1).padStart(2, '0')}-${String(createTime.getDate()).padStart(2, '0')} ${String(createTime.getHours()).padStart(2, '0')}:${String(createTime.getMinutes()).padStart(2, '0')}`

  return {
    id: record.id,
    icon: typeInfo.icon,
    type: typeInfo.type,
    time: timeStr,
    change: record.points,
    // 后端流水可能不写备注, 按 类型 给默认说明避免"说明"列空白
    remark: record.remark || typeInfo.type,
  }
}

/**
 * 用户与积分相关接口
 */

// 获取当前登录用户信息（后端返回：id, username, phone, nickname, avatar, email, userType, points, level, status）
export function getCurrentUser() {
  return request.get('/user/user/info')
}

// 修改当前用户资料（后端 UserUpdateDTO 仅支持：nickname / avatar / email / phone）
export function updateCurrentUser({ nickname, avatar, email, phone }) {
  return request.put('/user/user/update', { nickname, avatar, email, phone })
}

// 指定用户的公开资料（用户主页：昵称/头像/等级/关注粉丝计数/关注状态）
export function getUserDetail(userId) {
  return request.get(`/user/user/${userId}`)
}

// 获取会员等级信息（按积分实时推导等级区间与进度）
// 说明: 后端 level 字段暂无自动升级逻辑, 展示层以积分区间为准避免"高积分低等级"的矛盾
export function getMembership(userPoints = 0) {
  const levels = [
    { level: 1, name: '普通会员', minPoints: 0 },
    { level: 2, name: '银卡会员', minPoints: 5000 },
    { level: 3, name: '金卡会员', minPoints: 15000 },
    { level: 4, name: '钻石会员', minPoints: 50000 },
  ]

  // 积分所在区间: 最后一个 minPoints <= 积分的等级
  let currentLevelInfo = levels[0]
  for (const l of levels) {
    if (userPoints >= l.minPoints) currentLevelInfo = l
  }
  const nextLevelInfo = levels.find((l) => l.minPoints > currentLevelInfo.minPoints)

  if (!nextLevelInfo) {
    return {
      currentLevel: currentLevelInfo.name,
      nextLevel: '已满级',
      nextLevelPoints: currentLevelInfo.minPoints,
      progress: 100,
    }
  }

  const currentProgress = userPoints - currentLevelInfo.minPoints
  const needed = nextLevelInfo.minPoints - currentLevelInfo.minPoints
  const progress = Math.min(100, Math.round((currentProgress / needed) * 100))

  return {
    currentLevel: currentLevelInfo.name,
    nextLevel: nextLevelInfo.name,
    nextLevelPoints: nextLevelInfo.minPoints,
    progress,
  }
}

// 获取积分总览（余额 + 本月获得/本月消耗，后端 GET /points/summary；失败兜底全 0 不炸页面）
export function getPointsSummary() {
  return request.get('/points/summary').catch(() => ({ total: 0, monthEarned: 0, monthSpent: 0 }))
}

// 获取积分明细（右侧栏最近 3 条）
export function getPointsRecords() {
  return request.get('/points/record/page', { params: { pageNum: 1, pageSize: 3 } })
    .then((res) => {
      const records = res?.records || []
      return records.map(transformPointsRecord)
    })
    .catch(() => [])
}

// 获取积分流水分页列表（个人中心「积分」Tab）
export function getPointsRecordsFull(pageNum = 1, pageSize = 20) {
  return request.get('/points/record/page', { params: { pageNum, pageSize } })
    .then((res) => {
      const records = res?.records || []
      return records.map(transformPointsRecord)
    })
    .catch(() => [])
}

// 获取积分榜 TopN（后端 GET /points/rank，口径=本月获得积分聚合，自然月自动重榜；
// pointsText 为展示装饰字段前端格式化，verified 后端不提供恒 false）
function formatPointsText(n) {
  const v = Number(n || 0)
  if (v < 1000) return String(v)
  const k = v / 1000
  return (k >= 10 ? Math.round(k) : Math.round(k * 10) / 10) + 'k'
}

export async function getLeaderboard(limit = 3) {
  try {
    const rows = await request.get('/points/rank', { params: { pageNum: 1, pageSize: limit } })
    return (rows || []).map((item) => ({
      rank: item.rank,
      userId: item.userId,
      nickname: item.nickname,
      username: item.username,
      avatar: item.avatar,
      points: item.points,
      pointsText: formatPointsText(item.points),
      verified: false,
    }))
  } catch {
    return []
  }
}

// 完整榜单分页（GET /points/rank）：后端返回 List（无 total），配合"加载更多"逐页追加；
// 返回不足 pageSize 条即视为到底。异常向上抛给调用方提示
export async function getLeaderboardPage(pageNum = 1, pageSize = 10) {
  const rows = await request.get('/points/rank', { params: { pageNum, pageSize } })
  return (rows || []).map((item) => ({
    rank: item.rank,
    userId: item.userId,
    nickname: item.nickname,
    username: item.username,
    avatar: item.avatar,
    points: item.points,
    pointsText: formatPointsText(item.points),
  }))
}

// 我的本月名次与本月获得积分（GET /points/rank/me；rank=0 表示未上榜）
export function getMyRank() {
  return request.get('/points/rank/me').catch(() => null)
}

// 获取用户设置（后端 GET /user/settings，字段归属 life_user_detail；失败兜默认全开）
export function getSettings() {
  return request.get('/user/settings').catch(() => ({ notifyPoints: 1, recommendFeed: 1 }))
}

// 保存用户设置（后端 PUT /user/settings，部分更新仅覆盖传值字段）
export function saveSettings(data) {
  return request.put('/user/settings', data)
}

// 每日签到
export function doCheckin() {
  return request.post('/signRecord').then((res) => ({
    success: true,
    points: (res?.signPoints || 1) + (res?.rewardPoints || 0),
    signDays: res?.signDays || 1,
    message: `签到成功，+${(res?.signPoints || 1) + (res?.rewardPoints || 0)} 积分`,
  }))
}

// 查询本月签到记录
export function getSignRecords() {
  return request.get('/signRecord').catch(() => [])
}

// —— 账户与安全（后端 UserController）—— 

// 余额充值: amountYuan 单位元, 后端 RechargeDTO.amount 单位为分
export function recharge(amountYuan) {
  return request.post('/user/user/recharge', { amount: Math.round(amountYuan * 100) })
}

// 修改密码
export function updatePassword({ oldPassword, newPassword }) {
  return request.put('/user/user/update/password', { oldPassword, newPassword })
}

// 图片上传接口（后端 CommonController, 需登录, 表单字段 file, 返回图片 URL）
// 供 el-upload 的 action 使用: 开发环境走 vite 代理
export const UPLOAD_URL = `${import.meta.env.VITE_API_BASE_URL || '/api'}/admin/common/upload`

