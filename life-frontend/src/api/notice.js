import request from './request'

/**
 * 消息通知接口（后端 NoticeController, 均需登录）
 * 类型 type: 1-系统 2-订单 3-活动 4-权益 5-积分
 */

// 通知分页: params { pageNum, pageSize, isRead?(0未读1已读), type? } -> { total, records }
export function getNoticePage(params = {}) {
  return request.get('/notice/page', { params: { pageNum: 1, pageSize: 20, ...params } })
}

// 未读数(角标轮询)
export function getUnreadCount() {
  return request.get('/notice/unread/count')
}

// 标记单条已读(幂等)
export function markNoticeRead(id) {
  return request.put(`/notice/${id}/read`)
}

// 全部已读
export function markAllRead() {
  return request.put('/notice/read/all')
}

export const NOTICE_TYPE_MAP = {
  1: { label: '系统', icon: 'campaign' },
  2: { label: '订单', icon: 'receipt_long' },
  3: { label: '活动', icon: 'celebration' },
  4: { label: '权益', icon: 'workspace_premium' },
  5: { label: '积分', icon: 'stars' },
}
