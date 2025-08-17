import { ref, watch, onMounted, onUnmounted, readonly } from 'vue'

const THEME_KEY = 'oa-theme'
const THEMES = {
  LIGHT: 'theme-light',
  DARK: 'theme-dark',
  AUTO: 'theme-auto'
}

// 响应式主题状态
const currentTheme = ref(THEMES.LIGHT)
const isDarkMode = ref(false)

/**
 * 主题切换组合式函数
 */
export function useTheme() {
  // 检测系统主题偏好
  const getSystemTheme = () => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia('(prefers-color-scheme: dark)').matches 
        ? THEMES.DARK 
        : THEMES.LIGHT
    }
    return THEMES.LIGHT
  }

  // 应用主题到DOM
  const applyTheme = (theme) => {
    if (typeof document === 'undefined') return

    const root = document.documentElement
    
    // 移除所有主题类
    Object.values(THEMES).forEach(themeClass => {
      root.classList.remove(themeClass)
    })

    // 根据主题设置类名和暗黑模式状态
    let actualTheme = theme
    if (theme === THEMES.AUTO) {
      actualTheme = getSystemTheme()
    }

    root.classList.add(actualTheme)
    isDarkMode.value = actualTheme === THEMES.DARK

    // 设置data属性，方便CSS选择器使用
    root.setAttribute('data-theme', actualTheme.replace('theme-', ''))
    
    // 存储到localStorage
    localStorage.setItem(THEME_KEY, theme)
    currentTheme.value = theme
  }

  // 切换到指定主题
  const setTheme = (theme) => {
    if (Object.values(THEMES).includes(theme)) {
      applyTheme(theme)
    }
  }

  // 切换亮/暗主题
  const toggleTheme = () => {
    const newTheme = isDarkMode.value ? THEMES.LIGHT : THEMES.DARK
    setTheme(newTheme)
  }

  // 设置自动主题
  const setAutoTheme = () => {
    setTheme(THEMES.AUTO)
  }

  // 监听系统主题变化
  const watchSystemTheme = () => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      
      const handler = (e) => {
        if (currentTheme.value === THEMES.AUTO) {
          applyTheme(THEMES.AUTO)
        }
      }

      mediaQuery.addEventListener('change', handler)
      
      // 返回清理函数
      return () => mediaQuery.removeEventListener('change', handler)
    }
    return () => {}
  }

  // 初始化主题
  const initTheme = () => {
    if (typeof window === 'undefined') return

    // 从localStorage读取保存的主题
    const savedTheme = localStorage.getItem(THEME_KEY)
    
    if (savedTheme && Object.values(THEMES).includes(savedTheme)) {
      applyTheme(savedTheme)
    } else {
      // 如果没有保存的主题，使用系统主题
      applyTheme(THEMES.AUTO)
    }

    // 开始监听系统主题变化
    return watchSystemTheme()
  }

  // 获取当前主题信息
  const getThemeInfo = () => {
    return {
      current: currentTheme.value,
      isDark: isDarkMode.value,
      isLight: !isDarkMode.value,
      isAuto: currentTheme.value === THEMES.AUTO,
      system: getSystemTheme()
    }
  }

  // 获取主题选项列表
  const getThemeOptions = () => {
    return [
      {
        value: THEMES.LIGHT,
        label: '浅色主题',
        icon: 'Sunny'
      },
      {
        value: THEMES.DARK,
        label: '深色主题',
        icon: 'Moon'
      },
      {
        value: THEMES.AUTO,
        label: '跟随系统',
        icon: 'Monitor'
      }
    ]
  }

  return {
    // 状态
    currentTheme: readonly(currentTheme),
    isDarkMode: readonly(isDarkMode),
    
    // 方法
    setTheme,
    toggleTheme,
    setAutoTheme,
    initTheme,
    getThemeInfo,
    getThemeOptions,
    
    // 常量
    THEMES
  }
}

// 全局主题状态管理
let themeCleanup = null

// 在组件挂载时初始化主题
export function setupTheme() {
  onMounted(() => {
    const { initTheme } = useTheme()
    themeCleanup = initTheme()
  })

  // 在组件卸载时清理
  onUnmounted(() => {
    if (themeCleanup) {
      themeCleanup()
      themeCleanup = null
    }
  })
}

// 导出主题常量供模板使用
export { THEMES }

// 只读状态，供外部组件访问
export const themeState = {
  get current() { return currentTheme.value },
  get isDark() { return isDarkMode.value },
  get isLight() { return !isDarkMode.value }
}