import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const api = axios.create({
  baseURL: 'http://localhost:8989',
  timeout: 5000,
  withCredentials: true
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 由于使用 HttpOnly Cookie，token 由浏览器自动发送，无需手动设置
    // config.headers.Authorization = `Bearer ${token}`
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status
    const message = error.response?.data?.message || '服务器错误'
    console.error(`请求失败，状态码: ${status}, 错误信息: ${message}`, error.response)
    ElMessage.error(message)
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
