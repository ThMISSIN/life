import request from './request'

/**
 * 图片上传统一封装（后端 CommonController: POST /admin/common/upload, 需登录）
 * 流程: 选图 → multipart 提交 → 后端 UUID+后缀 推阿里云 OSS → 返回访问 URL
 * 图片本身不进数据库, 库里只存 URL; 调用方拿到 URL 后随业务表单提交
 */

// 供 el-upload action 属性使用（开发环境走 vite 代理）
export const UPLOAD_URL = `${import.meta.env.VITE_API_BASE_URL || '/api'}/admin/common/upload`

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
const MAX_SIZE = 5 * 1024 * 1024 // 5MB

/**
 * 校验图片类型与大小
 * @returns {string|null} 错误信息, null 表示通过
 */
export function validateImage(file) {
  if (!ALLOWED_TYPES.includes(file.type)) {
    return '仅支持 JPG / PNG / GIF / WebP 格式图片'
  }
  if (file.size > MAX_SIZE) {
    return '图片大小不能超过 5MB'
  }
  return null
}

/**
 * 上传单张图片
 * @param {File} file 图片文件
 * @returns {Promise<string>} OSS 访问 URL
 */
export async function uploadImage(file) {
  const err = validateImage(file)
  if (err) throw new Error(err)

  const formData = new FormData()
  formData.append('file', file)

  // request 响应拦截器已剥离 Result 包装, 直接拿到 URL 字符串
  const url = await request.post('/admin/common/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
  if (!url) throw new Error('上传失败：未获取到图片地址')
  return url
}
