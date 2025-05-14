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
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
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
    const message = error.response?.data?.message || '服务器错误'
    ElMessage.error(message)
    if (error.response?.status === 401 || error.response?.status === 403) {
      // 未登录或 Token 过期，暂时不清除 token 或跳转，方便调试
      console.error('认证错误 (401/403)，暂时不跳转登录页:', error.response)
    }
    return Promise.reject(error)
  }
)

export default api
