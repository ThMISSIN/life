/**
 * life业务公共工具
 * 图片存于C端后端本地存储, 库中保存相对路径(/uploads/...), 由C端(或管理端)静态映射对外提供
 */

// C端后端地址(开发环境 http://localhost:8080), 生产环境请在 .env.production 配置
const LIFE_API_BASE = process.env.VUE_APP_LIFE_API || 'http://localhost:8080'

/**
 * life图片地址补全: /uploads/... -> C端后端可访问地址; 完整URL原样返回
 */
export function lifeImgUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  if (path.indexOf('/uploads/') === 0) return LIFE_API_BASE + path
  return path
}

/**
 * 分 -> 元 显示(保留两位, 空值返回'-')
 */
export function fenToYuan(fen) {
  if (fen === null || fen === undefined) return '-'
  return (fen / 100).toFixed(2)
}

/**
 * 元 -> 分(表单提交用)
 */
export function yuanToFen(yuan) {
  if (yuan === null || yuan === undefined || yuan === '') return 0
  return Math.round(parseFloat(yuan) * 100)
}

/**
 * 积分价格展示: 0 表示不可积分兑换
 */
export function pointsText(points) {
  if (!points) return '不可兑换'
  return points + ' 积分'
}
