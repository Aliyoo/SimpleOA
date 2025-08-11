/**
 * 优化的API请求工具
 * 集成性能监控、缓存管理、错误处理等功能
 */

import workTimeCache from './workTimeCache.js'
import performanceMonitor from './performanceMonitor.js'
import api from './axios.js'

class OptimizedWorkTimeApi {
  constructor() {
    this.cache = workTimeCache
    this.monitor = performanceMonitor
    
    // 请求拦截器 - 添加性能监控
    api.interceptors.request.use(
      (config) => {
        // 开始性能监控
        config.performanceTracker = this.monitor.startApiRequest(config.url, config.method?.toUpperCase())
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )
    
    // 响应拦截器 - 结束性能监控
    api.interceptors.response.use(
      (response) => {
        // 结束性能监控
        if (response.config.performanceTracker) {
          const responseSize = JSON.stringify(response.data).length
          response.config.performanceTracker.end(true, responseSize, response.status)
        }
        return response
      },
      (error) => {
        // 记录错误
        if (error.config && error.config.performanceTracker) {
          error.config.performanceTracker.end(false, 0, error.response?.status || 0)
        }
        
        // 记录到性能监控
        this.monitor.recordError({
          type: 'api',
          url: error.config?.url,
          method: error.config?.method,
          status: error.response?.status,
          message: error.message,
          timestamp: Date.now()
        })
        
        return Promise.reject(error)
      }
    )
  }
  
  /**
   * 批量获取用户统计数据
   */
  async getUserStats(userIds, startDate, endDate) {
    return this.cache.getBatchUserStats(userIds, startDate, endDate, api)
  }
  
  /**
   * 批量获取项目统计数据
   */
  async getProjectStats(projectIds, startDate, endDate) {
    return this.cache.getBatchProjectStats(projectIds, startDate, endDate, api)
  }
  
  /**
   * 获取批量项目工时数据
   */
  async getBatchWorkTimeByProjects(projectIds, startDate, endDate) {
    return this.cache.getBatchWorkTimeByProjects(projectIds, startDate, endDate, api)
  }
  
  /**
   * 获取时间序列统计数据
   */
  async getTimeSeriesStats(startDate, endDate, period = 'day') {
    return this.cache.getTimeSeriesStats(startDate, endDate, period, api)
  }
  
  /**
   * 获取工作类型统计
   */
  async getWorkTypeStats(startDate, endDate, projectId = null, userId = null) {
    return this.cache.getWorkTypeStats(startDate, endDate, projectId, userId, api)
  }
  
  /**
   * 获取项目-成员工时统计（批量）
   */
  async getProjectUserStatsBatch(projectIds, startDate, endDate) {
    const cacheKey = this.cache.generateCacheKey('projectUserStatsBatch', { projectIds: projectIds?.join(',') || '', startDate, endDate })
    const cached = this.cache.getFromCache(cacheKey)
    if (cached) return cached
    const response = await api.post('/api/worktime/projects/users/stats', { projectIds, startDate, endDate })
    this.cache.setCache(cacheKey, response.data)
    return response.data
  }

  /**
   * 获取工时排行榜
   */
  async getWorkTimeRanking(startDate, endDate, limit = 10) {
    const cacheKey = this.cache.generateCacheKey('ranking', { startDate, endDate, limit })
    
    const cachedData = this.cache.getFromCache(cacheKey)
    if (cachedData) {
      return cachedData
    }
    
    try {
      const response = await api.get('/api/worktime/ranking', {
        params: { startDate, endDate, limit }
      })
      
      const result = response.data
      this.cache.setCache(cacheKey, result)
      return result
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 获取工时分布统计
   */
  async getHoursDistribution(startDate, endDate) {
    const cacheKey = this.cache.generateCacheKey('distribution', { startDate, endDate })
    
    const cachedData = this.cache.getFromCache(cacheKey)
    if (cachedData) {
      return cachedData
    }
    
    try {
      const response = await api.get('/api/worktime/distribution', {
        params: { startDate, endDate }
      })
      
      const result = response.data
      this.cache.setCache(cacheKey, result)
      return result
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 高性能分页查询
   */
  async getCursorPagedRecords(startDate, endDate, lastId = null, size = 20) {
    return this.cache.getCursorPagedRecords(startDate, endDate, lastId, size, api)
  }
  
  /**
   * 提交工时记录
   */
  async submitWorkTime(workTimeRecord) {
    try {
      const response = await api.post('/api/worktime', workTimeRecord)
      
      // 清除相关缓存
      this.cache.clearCache('userStats')
      this.cache.clearCache('projectStats')
      this.cache.clearCache('batchWorkTime')
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 批量提交工时记录
   */
  async submitBatchWorkTime(workTimeRecords) {
    try {
      const response = await api.post('/api/worktime/batch', workTimeRecords)
      
      // 清除相关缓存
      this.cache.clearCache('userStats')
      this.cache.clearCache('projectStats')
      this.cache.clearCache('batchWorkTime')
      this.cache.clearCache('timeSeriesStats')
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 获取用户工时记录（支持筛选和分页）
   */
  async getUserWorkTimeRecords(userId, startDate, endDate, options = {}) {
    const {
      projectId = null,
      approved = null,
      page = 0,
      size = 10
    } = options
    
    try {
      const response = await api.get(`/api/worktime/user/${userId}/range`, {
        params: {
          startDate,
          endDate,
          projectId,
          approved,
          page,
          size
        }
      })
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 获取项目工时记录（支持筛选和分页）
   */
  async getProjectWorkTimeRecords(projectId, startDate, endDate, options = {}) {
    const {
      approved = null,
      userId = null,
      workType = null,
      page = 0,
      size = 10
    } = options
    
    try {
      const response = await api.get('/api/worktime/project/range', {
        params: {
          projectId,
          startDate,
          endDate,
          approved,
          userId,
          workType,
          page,
          size
        }
      })
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 批量审批工时记录
   */
  async batchApproveWorkTime(recordIds) {
    try {
      const response = await api.put('/api/worktime/batch/approve', { recordIds })
      
      // 清除相关缓存
      this.cache.clearCache('userStats')
      this.cache.clearCache('projectStats')
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 批量驳回工时记录
   */
  async batchRejectWorkTime(recordIds, reason) {
    try {
      const response = await api.put('/api/worktime/batch/reject', { recordIds, reason })
      
      // 清除相关缓存
      this.cache.clearCache('userStats')
      this.cache.clearCache('projectStats')
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 导出工时数据
   */
  async exportWorkTimeData(startDate, endDate, options = {}) {
    const {
      projectIds = null,
      userIds = null
    } = options
    
    try {
      const response = await api.post('/api/worktime/export', {
        startDate,
        endDate,
        projectIds,
        userIds
      })
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 检查数据完整性
   */
  async checkDataIntegrity(userIds, startDate, endDate) {
    try {
      const response = await api.post('/api/worktime/integrity/check', {
        userIds,
        startDate,
        endDate
      })
      
      return response.data
    } catch (error) {
      throw error
    }
  }
  
  /**
   * 预热缓存
   */
  async warmUpCache(config) {
    return this.cache.warmUpCache(config, api)
  }
  
  /**
   * 获取性能统计
   */
  getPerformanceStats() {
    return {
      cache: this.cache.getStats(),
      monitor: this.monitor.getPerformanceReport()
    }
  }
  
  /**
   * 清除所有缓存
   */
  clearAllCache() {
    this.cache.clearCache()
  }
  
  /**
   * 重置性能监控
   */
  resetPerformanceMonitor() {
    this.monitor.reset()
  }
}

// 创建单例实例
const optimizedWorkTimeApi = new OptimizedWorkTimeApi()

export default optimizedWorkTimeApi

// 导出各个方法供直接使用
export const {
  getUserStats,
  getProjectStats,
  getBatchWorkTimeByProjects,
  getTimeSeriesStats,
  getWorkTypeStats,
  getWorkTimeRanking,
  getHoursDistribution,
  getCursorPagedRecords,
  submitWorkTime,
  submitBatchWorkTime,
  getUserWorkTimeRecords,
  getProjectWorkTimeRecords,
  batchApproveWorkTime,
  batchRejectWorkTime,
  exportWorkTimeData,
  checkDataIntegrity,
  warmUpCache,
  getPerformanceStats,
  clearAllCache,
  getProjectUserStatsBatch,
  resetPerformanceMonitor
} = optimizedWorkTimeApi