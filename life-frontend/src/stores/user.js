import { reactive, toRef } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

/**
 * 轻量全局用户状态（未引入 pinia，保持依赖精简）
 *
 * 登录态说明：
 * - token 存于 localStorage（key: token），请求层自动附加到 authentication 请求头
 * - 后端 UserVO 返回：id, username, phone, nickname, avatar, email, userType, points, level, status
 */

// 默认用户状态（未登录时的初始值）
const defaultUser = {
  id: null,
  nickname: '',
  username: '',
  avatar: '',
  level: 1,
  points: 0,
  money: 0,
  phone: '',
  email: '',
  userType: 2,
  status: 1,
}

export const userStore = reactive({
  user: { ...defaultUser },
  loaded: false,
  logging: false,
})

const TOKEN_KEY = 'token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

function mergeUserInfo(data) {
  if (!data) return
  // 合并后端返回的用户信息
  for (const [key, value] of Object.entries(data)) {
    if (value !== null && value !== undefined) {
      userStore.user[key] = value
    }
  }
}

export function useUserStore() {
  /**
   * 登录：成功后保存 token 并拉取用户信息
   */
  async function login(account, password) {
    userStore.logging = true
    try {
      const data = await loginApi(account, password)
      if (!data?.token) throw new Error('登录响应缺少凭证')
      setToken(data.token)
      await loadUser(true)
      return { ok: true }
    } catch (e) {
      return { ok: false, message: e.message || '登录失败，请稍后重试' }
    } finally {
      userStore.logging = false
    }
  }

  /**
   * 退出登录：通知后端拉黑 token（失败不阻塞本地登出），清理本地登录态
   */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // token 已失效等场景后端会报错，本地照常登出
    }
    clearToken()
    userStore.loaded = false
    userStore.user = { ...defaultUser }
  }

  /**
   * 拉取当前登录用户信息（force = true 时忽略缓存）
   * 后端返回：id, username, phone, nickname, avatar, email, userType, points, level, status
   */
  async function loadUser(force = false) {
    if (userStore.loaded && !force) return
    if (!getToken()) return
    try {
      const data = await getUserInfo()
      mergeUserInfo(data)
      userStore.loaded = true
    } catch {
      // 401 已由请求层统一处理（清 token 回登录页）；其余错误静默
    }
  }

  return {
    user: userStore.user,
    login,
    logout,
    loadUser,
    logging: toRef(userStore, 'logging'),
  }
}
