import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
// 在开发环境中使用相对路径，生产环境中使用环境变量或默认值
/**
 * 获取API基础URL
 * 开发环境：
 *   - 本地开发（localhost:3000）使用 VITE_API_BASE_URL 或默认 '/api'
 *   - 外部IP访问时，使用当前host的8989端口
 * 生产环境：使用 VITE_API_BASE_URL 或默认 '/api'
 */
const getBaseURL = () => {
  // 开发环境且通过外部IP访问
  if (import.meta.env.DEV && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
    return `http://${window.location.hostname}:8989`
  }
  // 开发环境本地访问或生产环境
  return import.meta.env.VITE_API_BASE_URL || '/api'
}

// 从环境变量获取超时时间（毫秒）
const API_TIMEOUT = parseInt(import.meta.env.VITE_API_TIMEOUT || '5000', 10)

// 创建axios实例
const api = axios.create({
  baseURL: getBaseURL(),
  timeout: API_TIMEOUT,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'X-Requested-With': 'XMLHttpRequest'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 由于使用 HttpOnly Cookie，token 由浏览器自动发送，无需手动设置
    // config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 检查响应状态码
    if (response.status >= 200 && response.status < 300) {
      return response
    }
    return Promise.reject(new Error(`HTTP ${response.status}: ${response.statusText}`))
  },
  (error) => {
    const status = error.response?.status
    let message = '服务器错误'

    // 尝试解析错误信息
    if (error.response?.data) {
      // 如果响应数据是字符串，尝试解析为JSON
      if (typeof error.response.data === 'string') {
        try {
          const parsed = JSON.parse(error.response.data)
          message = parsed.message || message
        } catch (e) {
          // 如果解析失败，使用原始字符串
          message = error.response.data
        }
      } else if (error.response.data.message) {
        message = error.response.data.message
      }
    }

    console.error(`请求失败，状态码: ${status}, 错误信息: ${message}`, error.response)

    // 只有在非组件调用时才显示错误消息
    if (!error.config?.skipGlobalErrorHandler) {
      ElMessage.error(message)
    }

    if (status === 401 || status === 403) {
      console.error('认证错误 (401/403)，但不立即清除认证状态，可能是临时问题:', error.response)
      // 不要立即登出，可能是临时问题
      // const userStore = require('../stores/user').useUserStore()
      // userStore.logout()
      // window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
