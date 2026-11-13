import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 实例
 * - 开发环境通过 vite proxy 将 /api 转发到后端（见 vite.config.js，转发时剥离 /api 前缀）
 * - 统一注入 token、统一处理后端 { code, msg, data } 包装结构与错误
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// 设备指纹(2.10 风控采集层): 首次生成UUID持久化到localStorage, 随所有请求头上送;
// 后端风控命中时落 life_risk_record.device_fingerprint, 用于识别同设备多账号刷分
function getDeviceFingerprint() {
  let fp = localStorage.getItem('device-fingerprint')
  if (!fp) {
    fp = (crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`).replace(/-/g, '')
    localStorage.setItem('device-fingerprint', fp)
  }
  return fp
}

// 请求拦截器：附加登录态（后端约定的请求头名为 authentication，裸 token，无 Bearer 前缀）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.authentication = token
  config.headers['X-Device-Fingerprint'] = getDeviceFingerprint()
  return config
})

// 响应拦截器：统一剥离包装层、处理错误码
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code, msg, data }：code = 1 成功，0 失败
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 1) return res.data
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    // 登录态失效（token 过期 / 已登出被拉黑）：清除本地态并回登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      if (!location.pathname.startsWith('/login')) {
        ElMessage.error('登录已过期，请重新登录')
        setTimeout(() => {
          const redirect = encodeURIComponent(location.pathname + location.search)
          location.href = `/login?redirect=${redirect}`
        }, 600)
      }
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // 后端业务异常以 HTTP 200 + code=0 返回，已在上方处理；
    // 这里处理网络错误 / 网关错误等
    const msg =
      error.response?.data?.msg ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请检查网络连接')
    return Promise.reject(new Error(msg))
  },
)

export default request
