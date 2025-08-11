/**
 * 工时数据缓存和性能优化工具类
 * 提供数据缓存、批量请求、防抖等性能优化功能
 */

class WorkTimeCache {
  constructor() {
    this.cache = new Map()
    this.requestQueue = new Map()
    this.debounceTimers = new Map()
    this.pendingRequests = new Map()
    
    // 配置项
    this.config = {
      cacheTimeout: 5 * 60 * 1000, // 5分钟缓存
      batchSize: 10, // 批量请求大小
      debounceDelay: 300, // 防抖延迟
      maxConcurrentRequests: 5, // 最大并发请求数
      retryAttempts: 3, // 重试次数
      retryDelay: 1000 // 重试延迟
    }
    
    this.stats = {
      cacheHits: 0,
      cacheMisses: 0,
      totalRequests: 0,
      failedRequests: 0
    }
  }
  
  /**
   * 生成缓存键
   */
  generateCacheKey(type, params) {
    const sortedParams = Object.keys(params)
      .sort()
      .reduce((result, key) => {
        result[key] = params[key]
        return result
      }, {})
    return `${type}:${JSON.stringify(sortedParams)}`
  }
  
  /**
   * 检查缓存是否有效
   */
  isCacheValid(cacheItem) {
    if (!cacheItem) return false
    const now = Date.now()
    return (now - cacheItem.timestamp) < this.config.cacheTimeout
  }
  
  /**
   * 从缓存获取数据
   */
  getFromCache(key) {
    const cacheItem = this.cache.get(key)
    if (this.isCacheValid(cacheItem)) {
      this.stats.cacheHits++
      return cacheItem.data
    }
    
    // 清理过期缓存
    if (cacheItem) {
      this.cache.delete(key)
    }
    
    this.stats.cacheMisses++
    return null
  }
  
  /**
   * 设置缓存数据
   */
  setCache(key, data) {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    })
  }
  
  /**
   * 防抖装饰器
   */
  debounce(key, fn, delay = this.config.debounceDelay) {
    return (...args) => {
      clearTimeout(this.debounceTimers.get(key))
      
      return new Promise((resolve, reject) => {
        const timer = setTimeout(async () => {
          try {
            const result = await fn(...args)
            resolve(result)
          } catch (error) {
            reject(error)
          } finally {
            this.debounceTimers.delete(key)
          }
        }, delay)
        
        this.debounceTimers.set(key, timer)
      })
    }
  }
  
  /**
   * 批量获取用户统计数据
   */
  async getBatchUserStats(userIds, startDate, endDate, api) {
    const cacheKey = this.generateCacheKey('userStats', { userIds: userIds.sort(), startDate, endDate })
    
    // 检查缓存
    const cachedData = this.getFromCache(cacheKey)
    if (cachedData) {
      return cachedData
    }
    
    // 检查是否有正在进行的相同请求
    const pendingKey = `userStats:${cacheKey}`
    if (this.pendingRequests.has(pendingKey)) {
      return await this.pendingRequests.get(pendingKey)
    }
    
    // 创建新的请求
    const requestPromise = this.executeWithRetry(async () => {
      this.stats.totalRequests++
      const response = await api.post('/api/worktime/users/stats', {
        userIds,
        startDate,
        endDate
      })
      return response.data
    })
    
    // 缓存正在进行的请求
    this.pendingRequests.set(pendingKey, requestPromise)
    
    try {
      const result = await requestPromise
      // 缓存结果
      this.setCache(cacheKey, result)
      return result
    } catch (error) {
      this.stats.failedRequests++
      throw error
    } finally {
      // 清理正在进行的请求
      this.pendingRequests.delete(pendingKey)
    }
  }
  
  /**
   * 批量获取项目统计数据
   */
  async getBatchProjectStats(projectIds, startDate, endDate, api) {
    const cacheKey = this.generateCacheKey('projectStats', { projectIds: projectIds.sort(), startDate, endDate })
    
    const cachedData = this.getFromCache(cacheKey)
    if (cachedData) {
      return cachedData
    }
    
    const pendingKey = `projectStats:${cacheKey}`
    if (this.pendingRequests.has(pendingKey)) {
      return await this.pendingRequests.get(pendingKey)
    }
    
    const requestPromise = this.executeWithRetry(async () => {
      this.stats.totalRequests++
      const response = await api.post('/api/worktime/projects/stats', {
        projectIds,
        startDate,
        endDate
      })
      return response.data
    })
    
    this.pendingRequests.set(pendingKey, requestPromise)
    
    try {
      const result = await requestPromise
      this.setCache(cacheKey, result)
      return result
    } catch (error) {
      this.stats.failedRequests++
      throw error
    } finally {
      this.pendingRequests.delete(pendingKey)
    }
  }
  
  /**
   * 获取批量项目工时数据
   */
  async getBatchWorkTimeByProjects(projectIds, startDate, endDate, api) {
    const cacheKey = this.generateCacheKey('batchWorkTime', { projectIds: projectIds.sort(), startDate, endDate })
    
    const cachedData = this.getFromCache(cacheKey)
    if (cachedData) {
      return cachedData
    }
    
    const pendingKey = `batchWorkTime:${cacheKey}`
    if (this.pendingRequests.has(pendingKey)) {
      return await this.pendingRequests.get(pendingKey)
    }
    
    const requestPromise = this.executeWithRetry(async () => {
      this.stats.totalRequests++
      const response = await api.post('/api/worktime/projects/batch', {
        projectIds,
        startDate,
        endDate
      })
      return response.data
    })
    
    this.pendingRequests.set(pendingKey, requestPromise)
    
    try {
      const result = await requestPromise
      // 由于批量填写数据会被修改，缓存时间设置较短
      this.setCache(cacheKey, result, this.config.cacheTimeout / 10)
      return result
    } catch (error) {
      this.stats.failedRequests++
      throw error
    } finally {
      this.pendingRequests.delete(pendingKey)
    }
  }
  
  /**
   * 获取时间序列统计数据
   */
  async getTimeSeriesStats(startDate, endDate, period, api) {
    const cacheKey = this.generateCacheKey('timeSeriesStats', { startDate, endDate, period })
    
    const cachedData = this.getFromCache(cacheKey)
    if (cachedData) {
      return cachedData
    }
    
    try {
      this.stats.totalRequests++
      const response = await api.get('/api/worktime/timeseries/stats', {
        params: { startDate, endDate, period }
      })
      
      const result = response.data
      this.setCache(cacheKey, result)
      return result
    } catch (error) {
      this.stats.failedRequests++
      throw error
    }
  }
  
  /**
   * 获取工作类型统计
   */
  async getWorkTypeStats(startDate, endDate, projectId, userId, api) {
    const cacheKey = this.generateCacheKey('workTypeStats', { startDate, endDate, projectId, userId })
    
    const cachedData = this.getFromCache(cacheKey)
    if (cachedData) {
      return cachedData
    }
    
    try {
      this.stats.totalRequests++
      const response = await api.get('/api/worktime/worktype/stats', {
        params: { startDate, endDate, projectId, userId }
      })
      
      const result = response.data
      this.setCache(cacheKey, result)
      return result
    } catch (error) {
      this.stats.failedRequests++
      throw error
    }
  }
  
  /**
   * 高性能分页查询（使用游标分页）
   */
  async getCursorPagedRecords(startDate, endDate, lastId, size, api) {
    const cacheKey = this.generateCacheKey('cursorPaged', { startDate, endDate, lastId, size })
    
    // 分页数据通常不缓存，因为会频繁变化
    try {
      this.stats.totalRequests++
      const response = await api.get('/api/worktime/cursor', {
        params: { startDate, endDate, lastId, size }
      })
      
      return response.data
    } catch (error) {
      this.stats.failedRequests++
      throw error
    }
  }
  
  /**
   * 带重试机制的请求执行
   */
  async executeWithRetry(fn, attempts = this.config.retryAttempts) {
    let lastError
    
    for (let i = 0; i < attempts; i++) {
      try {
        return await fn()
      } catch (error) {
        lastError = error
        
        // 如果是客户端错误（4xx），不重试
        if (error.response && error.response.status >= 400 && error.response.status < 500) {
          throw error
        }
        
        // 最后一次尝试，抛出错误
        if (i === attempts - 1) {
          throw error
        }
        
        // 等待后重试
        await this.sleep(this.config.retryDelay * (i + 1))
      }
    }
    
    throw lastError
  }
  
  /**
   * 睡眠函数
   */
  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }
  
  /**
   * 清除指定类型的缓存
   */
  clearCache(type) {
    if (type) {
      // 清除特定类型的缓存
      for (const [key] of this.cache) {
        if (key.startsWith(`${type}:`)) {
          this.cache.delete(key)
        }
      }
    } else {
      // 清除所有缓存
      this.cache.clear()
    }
  }
  
  /**
   * 清除过期缓存
   */
  clearExpiredCache() {
    const now = Date.now()
    for (const [key, item] of this.cache) {
      if ((now - item.timestamp) >= this.config.cacheTimeout) {
        this.cache.delete(key)
      }
    }
  }
  
  /**
   * 获取缓存统计信息
   */
  getStats() {
    return {
      ...this.stats,
      cacheSize: this.cache.size,
      hitRate: this.stats.totalRequests > 0 ? 
        ((this.stats.cacheHits / (this.stats.cacheHits + this.stats.cacheMisses)) * 100).toFixed(2) + '%' : 
        '0%',
      failureRate: this.stats.totalRequests > 0 ? 
        ((this.stats.failedRequests / this.stats.totalRequests) * 100).toFixed(2) + '%' : 
        '0%'
    }
  }
  
  /**
   * 预热缓存
   */
  async warmUpCache(config, api) {
    const { userIds, projectIds, startDate, endDate } = config
    
    try {
      // 并行预加载常用数据
      const promises = []
      
      if (userIds && userIds.length > 0) {
        promises.push(this.getBatchUserStats(userIds, startDate, endDate, api))
      }
      
      if (projectIds && projectIds.length > 0) {
        promises.push(this.getBatchProjectStats(projectIds, startDate, endDate, api))
        promises.push(this.getBatchWorkTimeByProjects(projectIds, startDate, endDate, api))
      }
      
      // 预加载时间序列数据
      promises.push(this.getTimeSeriesStats(startDate, endDate, 'day', api))
      
      await Promise.allSettled(promises)
      console.log('Cache warm-up completed')
    } catch (error) {
      console.error('Cache warm-up failed:', error)
    }
  }
  
  /**
   * 重置统计信息
   */
  resetStats() {
    this.stats = {
      cacheHits: 0,
      cacheMisses: 0,
      totalRequests: 0,
      failedRequests: 0
    }
  }
}

// 创建单例实例
const workTimeCache = new WorkTimeCache()

// 定期清理过期缓存
setInterval(() => {
  workTimeCache.clearExpiredCache()
}, 60 * 1000) // 每分钟清理一次

export default workTimeCache