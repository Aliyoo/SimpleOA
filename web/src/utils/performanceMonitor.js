/**
 * 前端性能监控工具
 * 监控API请求性能、组件渲染性能、缓存命中率等
 */

class PerformanceMonitor {
  constructor() {
    this.metrics = {
      apiRequests: new Map(),
      componentRenders: new Map(),
      cacheMetrics: new Map(),
      pageLoads: [],
      errors: []
    }
    
    this.thresholds = {
      slowApiRequest: 2000, // 2秒
      slowRender: 100, // 100ms
      memoryWarning: 50 * 1024 * 1024, // 50MB
      errorRate: 0.05 // 5%
    }
    
    this.isMonitoring = false
    this.startTime = Date.now()
    
    this.init()
  }
  
  init() {
    if (typeof window !== 'undefined') {
      // 监控页面加载性能
      this.monitorPageLoad()
      
      // 监控内存使用情况
      this.monitorMemoryUsage()
      
      // 监控错误
      this.monitorErrors()
      
      this.isMonitoring = true
    }
  }
  
  /**
   * 监控API请求性能
   */
  startApiRequest(url, method = 'GET') {
    const requestId = `${method}:${url}:${Date.now()}`
    const startTime = performance.now()
    
    return {
      requestId,
      end: (success = true, responseSize = 0, statusCode = 200) => {
        const endTime = performance.now()
        const duration = endTime - startTime
        
        const requestData = {
          url,
          method,
          duration,
          success,
          responseSize,
          statusCode,
          timestamp: Date.now()
        }
        
        this.metrics.apiRequests.set(requestId, requestData)
        
        // 检查慢请求
        if (duration > this.thresholds.slowApiRequest) {
          console.warn(`慢API请求检测: ${method} ${url} 用时 ${duration.toFixed(2)}ms`)
        }
        
        return requestData
      }
    }
  }
  
  /**
   * 监控组件渲染性能
   */
  startRender(componentName) {
    const renderId = `${componentName}:${Date.now()}`
    const startTime = performance.now()
    
    return {
      renderId,
      end: () => {
        const endTime = performance.now()
        const duration = endTime - startTime
        
        const renderData = {
          component: componentName,
          duration,
          timestamp: Date.now()
        }
        
        this.metrics.componentRenders.set(renderId, renderData)
        
        // 检查慢渲染
        if (duration > this.thresholds.slowRender) {
          console.warn(`慢组件渲染检测: ${componentName} 用时 ${duration.toFixed(2)}ms`)
        }
        
        return renderData
      }
    }
  }
  
  /**
   * 记录缓存性能
   */
  recordCacheMetric(operation, hit, duration = 0) {
    const cacheData = {
      operation,
      hit,
      duration,
      timestamp: Date.now()
    }
    
    const key = `cache:${operation}:${Date.now()}`
    this.metrics.cacheMetrics.set(key, cacheData)
  }
  
  /**
   * 监控页面加载性能
   */
  monitorPageLoad() {
    if (typeof window.addEventListener === 'function') {
      window.addEventListener('load', () => {
        setTimeout(() => {
          const navigation = performance.getEntriesByType('navigation')[0]
          if (navigation) {
            const pageLoadData = {
              loadTime: navigation.loadEventEnd - navigation.fetchStart,
              domContentLoaded: navigation.domContentLoadedEventEnd - navigation.fetchStart,
              firstContentfulPaint: this.getFirstContentfulPaint(),
              timestamp: Date.now()
            }
            
            this.metrics.pageLoads.push(pageLoadData)
            
            console.log('页面加载性能:', pageLoadData)
          }
        }, 0)
      })
    }
  }
  
  /**
   * 获取首次内容绘制时间
   */
  getFirstContentfulPaint() {
    try {
      const fcpEntry = performance.getEntriesByName('first-contentful-paint')[0]
      return fcpEntry ? fcpEntry.startTime : null
    } catch (error) {
      return null
    }
  }
  
  /**
   * 监控内存使用情况
   */
  monitorMemoryUsage() {
    if (typeof window.performance.memory !== 'undefined') {
      setInterval(() => {
        const memory = window.performance.memory
        
        if (memory.usedJSHeapSize > this.thresholds.memoryWarning) {
          console.warn(`内存使用警告: ${(memory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`)
        }
      }, 30000) // 每30秒检查一次
    }
  }
  
  /**
   * 监控错误
   */
  monitorErrors() {
    if (typeof window.addEventListener === 'function') {
      // JavaScript错误
      window.addEventListener('error', (event) => {
        this.recordError({
          type: 'javascript',
          message: event.message,
          filename: event.filename,
          lineno: event.lineno,
          colno: event.colno,
          stack: event.error ? event.error.stack : null,
          timestamp: Date.now()
        })
      })
      
      // Promise错误
      window.addEventListener('unhandledrejection', (event) => {
        this.recordError({
          type: 'promise',
          message: event.reason ? event.reason.toString() : 'Unhandled Promise Rejection',
          stack: event.reason ? event.reason.stack : null,
          timestamp: Date.now()
        })
      })
    }
  }
  
  /**
   * 记录错误
   */
  recordError(errorData) {
    this.metrics.errors.push(errorData)
    
    // 保持错误日志在合理大小
    if (this.metrics.errors.length > 100) {
      this.metrics.errors.shift()
    }
    
    console.error('性能监控捕获错误:', errorData)
  }
  
  /**
   * 获取API请求统计
   */
  getApiRequestStats() {
    const requests = Array.from(this.metrics.apiRequests.values())
    
    if (requests.length === 0) {
      return {
        totalRequests: 0,
        averageResponseTime: 0,
        successRate: 0,
        slowRequests: 0
      }
    }
    
    const successful = requests.filter(r => r.success)
    const slowRequests = requests.filter(r => r.duration > this.thresholds.slowApiRequest)
    const totalDuration = requests.reduce((sum, r) => sum + r.duration, 0)
    
    return {
      totalRequests: requests.length,
      averageResponseTime: Math.round(totalDuration / requests.length),
      successRate: Math.round((successful.length / requests.length) * 100),
      slowRequests: slowRequests.length,
      errorRate: Math.round(((requests.length - successful.length) / requests.length) * 100)
    }
  }
  
  /**
   * 获取组件渲染统计
   */
  getComponentRenderStats() {
    const renders = Array.from(this.metrics.componentRenders.values())
    
    if (renders.length === 0) {
      return {
        totalRenders: 0,
        averageRenderTime: 0,
        slowRenders: 0
      }
    }
    
    const slowRenders = renders.filter(r => r.duration > this.thresholds.slowRender)
    const totalDuration = renders.reduce((sum, r) => sum + r.duration, 0)
    
    return {
      totalRenders: renders.length,
      averageRenderTime: Math.round(totalDuration / renders.length * 100) / 100,
      slowRenders: slowRenders.length
    }
  }
  
  /**
   * 获取缓存统计
   */
  getCacheStats() {
    const cacheMetrics = Array.from(this.metrics.cacheMetrics.values())
    
    if (cacheMetrics.length === 0) {
      return {
        totalOperations: 0,
        hitRate: 0,
        averageDuration: 0
      }
    }
    
    const hits = cacheMetrics.filter(c => c.hit)
    const totalDuration = cacheMetrics.reduce((sum, c) => sum + c.duration, 0)
    
    return {
      totalOperations: cacheMetrics.length,
      hitRate: Math.round((hits.length / cacheMetrics.length) * 100),
      averageDuration: Math.round(totalDuration / cacheMetrics.length * 100) / 100
    }
  }
  
  /**
   * 获取完整性能报告
   */
  getPerformanceReport() {
    const report = {
      overview: {
        monitoringDuration: Date.now() - this.startTime,
        isMonitoring: this.isMonitoring,
        timestamp: new Date().toISOString()
      },
      apiRequests: this.getApiRequestStats(),
      componentRenders: this.getComponentRenderStats(),
      cachePerformance: this.getCacheStats(),
      pageLoads: this.metrics.pageLoads.slice(-5), // 最近5次页面加载
      errors: this.metrics.errors.slice(-10), // 最近10个错误
      memory: this.getMemoryInfo(),
      recommendations: this.generateRecommendations()
    }
    
    return report
  }
  
  /**
   * 获取内存信息
   */
  getMemoryInfo() {
    if (typeof window.performance.memory !== 'undefined') {
      const memory = window.performance.memory
      return {
        used: Math.round(memory.usedJSHeapSize / 1024 / 1024 * 100) / 100,
        total: Math.round(memory.totalJSHeapSize / 1024 / 1024 * 100) / 100,
        limit: Math.round(memory.jsHeapSizeLimit / 1024 / 1024 * 100) / 100
      }
    }
    
    return null
  }
  
  /**
   * 生成性能优化建议
   */
  generateRecommendations() {
    const recommendations = []
    
    // API请求建议
    const apiStats = this.getApiRequestStats()
    if (apiStats.averageResponseTime > this.thresholds.slowApiRequest) {
      recommendations.push({
        type: 'api',
        priority: 'high',
        message: `API响应时间较慢 (平均${apiStats.averageResponseTime}ms)，建议优化后端查询或增加缓存`
      })
    }
    
    if (apiStats.errorRate > this.thresholds.errorRate * 100) {
      recommendations.push({
        type: 'api',
        priority: 'high',
        message: `API错误率较高 (${apiStats.errorRate}%)，建议检查错误处理和重试机制`
      })
    }
    
    // 组件渲染建议
    const renderStats = this.getComponentRenderStats()
    if (renderStats.slowRenders > renderStats.totalRenders * 0.1) {
      recommendations.push({
        type: 'rendering',
        priority: 'medium',
        message: '存在较多慢渲染组件，建议使用虚拟滚动或组件懒加载'
      })
    }
    
    // 缓存建议
    const cacheStats = this.getCacheStats()
    if (cacheStats.hitRate < 70) {
      recommendations.push({
        type: 'cache',
        priority: 'medium',
        message: `缓存命中率较低 (${cacheStats.hitRate}%)，建议优化缓存策略`
      })
    }
    
    // 内存建议
    const memoryInfo = this.getMemoryInfo()
    if (memoryInfo && memoryInfo.used > 50) {
      recommendations.push({
        type: 'memory',
        priority: 'high',
        message: `内存使用量较高 (${memoryInfo.used}MB)，建议检查内存泄漏`
      })
    }
    
    return recommendations
  }
  
  /**
   * 清理旧数据
   */
  cleanup() {
    const now = Date.now()
    const maxAge = 10 * 60 * 1000 // 10分钟
    
    // 清理API请求数据
    for (const [key, value] of this.metrics.apiRequests) {
      if (now - value.timestamp > maxAge) {
        this.metrics.apiRequests.delete(key)
      }
    }
    
    // 清理组件渲染数据
    for (const [key, value] of this.metrics.componentRenders) {
      if (now - value.timestamp > maxAge) {
        this.metrics.componentRenders.delete(key)
      }
    }
    
    // 清理缓存指标数据
    for (const [key, value] of this.metrics.cacheMetrics) {
      if (now - value.timestamp > maxAge) {
        this.metrics.cacheMetrics.delete(key)
      }
    }
    
    // 保持页面加载记录在合理数量
    if (this.metrics.pageLoads.length > 20) {
      this.metrics.pageLoads.splice(0, this.metrics.pageLoads.length - 20)
    }
  }
  
  /**
   * 重置所有指标
   */
  reset() {
    this.metrics = {
      apiRequests: new Map(),
      componentRenders: new Map(),
      cacheMetrics: new Map(),
      pageLoads: [],
      errors: []
    }
    this.startTime = Date.now()
  }
}

// 创建单例实例
const performanceMonitor = new PerformanceMonitor()

// 定期清理数据
setInterval(() => {
  performanceMonitor.cleanup()
}, 5 * 60 * 1000) // 每5分钟清理一次

export default performanceMonitor