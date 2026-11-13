import request from './request'

/**
 * 认证相关接口（对应后端 UserController：/user/user/**）
 * 登录、注册、忘记密码、登出为公开接口，其余需携带 authentication 请求头
 */

// 用户登录：account 支持 用户名 / 手机号 / 邮箱
export function login(account, password) {
  return request.post('/user/user/login', { account, password })
}

// 注册（用户名 + 手机号必填，邮箱选填）
export function register(data) {
  return request.post('/user/user/register', data)
}

// 忘记密码（重置密码）
export function forgetPassword({ account, password, confirmPassword }) {
  return request.put('/user/user/forget', { account, password, confirmPassword })
}

// 退出登录（服务端将 token 加入 Redis 黑名单）
export function logout() {
  return request.post('/user/user/logout')
}

// 获取当前登录用户信息
export function getUserInfo() {
  return request.get('/user/user/info')
}
