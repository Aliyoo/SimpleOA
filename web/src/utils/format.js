/**
 * 统一格式化工具函数
 * 用于保证项目中金额、日期、状态显示的一致性
 */

/**
 * 格式化金额
 * @param {number|string} amount - 金额
 * @param {number} precision - 精度，默认2位小数
 * @param {string} currency - 货币符号，默认为'¥'
 * @returns {string} 格式化后的金额字符串
 */
export const formatMoney = (amount, precision = 2, currency = '¥') => {
  if (amount === null || amount === undefined || amount === '') {
    return `${currency}0.00`
  }

  const numAmount = Number(amount)
  if (isNaN(numAmount)) {
    return `${currency}0.00`
  }

  return `${currency}${numAmount.toFixed(precision)}`
}

/**
 * 格式化日期
 * @param {string|Date} date - 日期
 * @param {string} format - 格式类型: 'date'(仅日期) | 'datetime'(日期时间) | 'time'(仅时间)
 * @returns {string} 格式化后的日期字符串
 */
export const formatDate = (date, format = 'date') => {
  if (!date) return '-'

  try {
    const d = new Date(date)
    if (isNaN(d.getTime())) return '-'

    switch (format) {
      case 'date':
        return d.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        })
      case 'datetime':
        return d.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      case 'time':
        return d.toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      default:
        return d.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        })
    }
  } catch (error) {
    console.error('日期格式化错误:', error)
    return '-'
  }
}

/**
 * 格式化报销状态
 * @param {string} status - 状态值
 * @returns {string} 格式化后的状态文本
 */
export const formatReimbursementStatus = (status) => {
  const statusMap = {
    DRAFT: '草稿',
    PENDING_MANAGER_APPROVAL: '待项目经理审批',
    PENDING_FINANCE_APPROVAL: '待财务审批',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return statusMap[status] || status
}

/**
 * 获取报销状态对应的标签类型
 * @param {string} status - 状态值
 * @returns {string} ElementPlus标签类型
 */
export const getReimbursementStatusTagType = (status) => {
  const tagMap = {
    DRAFT: 'info',
    PENDING_MANAGER_APPROVAL: 'warning',
    PENDING_FINANCE_APPROVAL: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return tagMap[status] || 'primary'
}

/**
 * 格式化审批状态
 * @param {string} status - 状态值
 * @returns {string} 格式化后的状态文本
 */
export const formatApprovalStatus = (status) => {
  const statusMap = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return statusMap[status] || status
}

/**
 * 获取审批状态对应的标签类型
 * @param {string} status - 状态值
 * @returns {string} ElementPlus标签类型
 */
export const getApprovalStatusTagType = (status) => {
  const tagMap = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return tagMap[status] || 'primary'
}

/**
 * 格式化请假状态
 * @param {string} status - 状态值
 * @returns {string} 格式化后的状态文本
 */
export const formatLeaveStatus = (status) => {
  const statusMap = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return statusMap[status] || status
}

/**
 * 获取请假状态对应的标签类型
 * @param {string} status - 状态值
 * @returns {string} ElementPlus标签类型
 */
export const getLeaveStatusTagType = (status) => {
  const tagMap = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return tagMap[status] || 'primary'
}

/**
 * 格式化工时状态
 * @param {boolean} approved - 是否已审批
 * @returns {string} 格式化后的状态文本
 */
export const formatWorkTimeStatus = (approved) => {
  return approved ? '已审批' : '待审批'
}

/**
 * 获取工时状态对应的标签类型
 * @param {boolean} approved - 是否已审批
 * @returns {string} ElementPlus标签类型
 */
export const getWorkTimeStatusTagType = (approved) => {
  return approved ? 'success' : 'warning'
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的文件大小
 */
export const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 格式化百分比
 * @param {number} value - 数值
 * @param {number} precision - 精度，默认1位小数
 * @returns {string} 格式化后的百分比
 */
export const formatPercentage = (value, precision = 1) => {
  if (value === null || value === undefined || isNaN(value)) {
    return '0%'
  }
  return `${Number(value).toFixed(precision)}%`
}

/**
 * 格式化数字（添加千分位分隔符）
 * @param {number|string} num - 数字
 * @returns {string} 格式化后的数字
 */
export const formatNumber = (num) => {
  if (num === null || num === undefined || num === '') {
    return '0'
  }

  const numValue = Number(num)
  if (isNaN(numValue)) {
    return '0'
  }

  return numValue.toLocaleString('zh-CN')
}
