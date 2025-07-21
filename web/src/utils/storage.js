/**
 * 安全的localStorage工具类
 */

/**
 * 安全地从localStorage读取并解析JSON
 * @param {string} key - localStorage的键名
 * @param {*} defaultValue - 如果读取失败时的默认值
 * @returns {*} 解析后的值或默认值
 */
export function getItem(key, defaultValue = null) {
  try {
    const item = localStorage.getItem(key)
    if (item === null) {
      return defaultValue
    }
    return JSON.parse(item)
  } catch (error) {
    console.warn(`Failed to parse localStorage item "${key}":`, error)
    // 如果解析失败，移除损坏的数据
    localStorage.removeItem(key)
    return defaultValue
  }
}

/**
 * 安全地设置localStorage项
 * @param {string} key - localStorage的键名
 * @param {*} value - 要存储的值
 * @returns {boolean} 是否设置成功
 */
export function setItem(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
    return true
  } catch (error) {
    console.warn(`Failed to set localStorage item "${key}":`, error)
    return false
  }
}

/**
 * 移除localStorage项
 * @param {string} key - 要移除的键名
 */
export function removeItem(key) {
  try {
    localStorage.removeItem(key)
  } catch (error) {
    console.warn(`Failed to remove localStorage item "${key}":`, error)
  }
}

/**
 * 清空所有用户相关的localStorage数据
 */
export function clearUserData() {
  const userKeys = ['user', 'token', 'permissions', 'menus']
  userKeys.forEach(key => removeItem(key))
}
