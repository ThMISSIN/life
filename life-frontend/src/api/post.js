import request from './request'

/**
 * 动态（帖子）相关接口
 *
 * 已接入后端 PostController（/post/**）：
 *   GET    /post/page       分页列表（type 1-推荐 2-关注 / userId 指定用户 / keyword / category）
 *   POST   /post/publish    发布（成功后端自动清理草稿），返回 PostVO
 *   GET    /post/draft      拉取草稿（Redis，无草稿 data 为 null）
 *   PUT    /post/draft      保存草稿（一人一份，覆盖式）
 *   DELETE /post/draft      放弃草稿（幂等）
 *   DELETE /post/{id}       删除自己的帖子
 *
 * PostVO → 前端卡片结构由 transformPost 统一映射；带 remote: true 的帖子
 * 可参与真实点赞/删除。
 */

// 获取首页信息流: type 'recommend'-为你推荐 / 'following'-正在关注; keyword 可选(标题/内容模糊搜索)
export async function getFeedPosts(type = 'recommend', { pageNum = 1, pageSize = 20, keyword } = {}) {
  const params = { type: type === 'following' ? 2 : 1, pageNum, pageSize }
  if (keyword) params.keyword = keyword
  const res = await request.get('/post/page', { params })
  return (res?.records || []).map(transformPost)
}

// 获取指定用户的动态列表
export async function getUserPosts(userId, { pageNum = 1, pageSize = 20 } = {}) {
  const res = await request.get('/post/page', {
    params: { userId: Number(userId), pageNum, pageSize },
  })
  return (res?.records || []).map(transformPost)
}

// 相对时间展示（刚发布/xx小时/昨天/日期）
function relativeTime(value) {
  if (!value) return ''
  const t = new Date(value).getTime()
  if (Number.isNaN(t)) return String(value)
  const diff = Date.now() - t
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)}分钟`
  if (diff < day) return `${Math.floor(diff / hour)}小时`
  if (diff < 2 * day) return '昨天'
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// 浏览量格式化：4200 → 4.2K
function formatViews(n) {
  const v = Number(n || 0)
  if (v >= 10000) return `${(v / 10000).toFixed(1).replace(/\.0$/, '')}万`
  if (v >= 1000) return `${(v / 1000).toFixed(1).replace(/\.0$/, '')}K`
  return String(v)
}

// 后端 PostVO → 前端帖子卡片结构
export function transformPost(vo) {
  return {
    id: vo.id,
    remote: true,
    author: {
      userId: vo.author?.userId,
      nickname: vo.author?.nickname || vo.author?.username || '',
      username: vo.author?.username ? `@${vo.author.username}` : '',
      avatar: vo.author?.avatar || '',
      followed: vo.author?.followed ?? false,
    },
    time: relativeTime(vo.createTime),
    content: vo.content || '',
    images: vo.images || [],
    hashtags: vo.category ? [vo.category] : [],
    actions: {
      comments: vo.commentCount || 0,
      reposts: vo.repostCount || 0,
      likes: vo.likeCount || 0,
      views: formatViews(vo.viewCount),
    },
    liked: Boolean(vo.liked),
    favorited: Boolean(vo.favorited),
    repostSource: vo.repostSource || null,
  }
}

// 发布动态: { content, images?, title?, category? }，返回转换后的帖子
export async function publishPost({ title, content, images = [], category }) {
  const vo = await request.post('/post/publish', { title, content, images, category })
  return transformPost(vo)
}

// 转发动态(一期: 计数+转发帖生成): content 为附言(可空)
// 后端 POST /post/{id}/repost, 返回生成的转发帖 PostVO
export async function repostPost(id, content = '') {
  const vo = await request.post(`/post/${id}/repost`, { content })
  return transformPost(vo)
}

// 阅读上报(2.11 热度"阅"权重): 打开帖子(评论抽屉)时触发, 会话级去重避免反复开合多报;
// 后端另有 用户+帖+日 去重与布隆过滤, 统计软态——失败静默不影响任何业务
const viewReported = new Set()
export function reportPostView(id) {
  if (!id || viewReported.has(id)) return
  viewReported.add(id)
  request.post(`/post/${id}/view`).catch(() => viewReported.delete(id))
}

// 拉取上次未发布的草稿（Redis 存储，仅内容回填，无草稿 id），无草稿返回 null
export async function getDraft() {
  const vo = await request.get('/post/draft')
  if (!vo) return null
  return {
    title: vo.title || '',
    content: vo.content || '',
    images: vo.images || [],
    category: vo.category || '',
  }
}

// 保存草稿（一人一份，覆盖式；content 为空时后端视为清除草稿）
export async function saveDraft({ title, content, images = [], category }) {
  return request.put('/post/draft', { title, content, images, category })
}

// 放弃草稿（幂等）
export function clearDraft() {
  return request.delete('/post/draft')
}

// 删除自己的帖子
export function deletePost(id) {
  return request.delete(`/post/${id}`)
}
