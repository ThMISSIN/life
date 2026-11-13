import request from './request'

/**
 * 帖子评论相关接口（后端 CommentController：/replies）
 *
 * 注意后端契约特殊（无 Result 包装）：
 *   POST   /replies          发表评论 → void（HTTP 200 即成功）
 *   GET    /replies/page     评论分页 → 裸 PageDTO{ total, pages, list }
 *   DELETE /replies/{id}     删除评论（本人或帖子作者可删）→ void
 *
 * CommentVO: { id, postId, author: { userId, username, nickname, avatar, followed },
 *              parentId, replyUserId, content, likeCount, liked,
 *              children: List<CommentVO>（仅两级，children 内不再嵌套）, createTime }
 */

// 评论分页: parentId 不传/0 查顶级评论，否则查该父评论的子回复
export function getCommentPage(postId, { pageNum = 1, pageSize = 10, parentId } = {}) {
  const params = { postId, pageNum, pageSize }
  if (parentId) params.parentId = parentId
  return request.get('/replies/page', { params })
}

// 发表评论: parentId 0/不传为顶级评论；replyUserId 为被回复的二级评论作者
export function publishComment({ postId, content, parentId = 0, replyUserId }) {
  const payload = { postId, content, parentId }
  if (replyUserId) payload.replyUserId = replyUserId
  return request.post('/replies', payload)
}

// 删除评论（本人或帖子作者可删，权限由后端校验）
export function deleteComment(id) {
  return request.delete(`/replies/${id}`)
}
