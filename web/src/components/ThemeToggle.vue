<template>
  <div class="theme-toggle">
    <!-- 简单切换按钮 -->
    <el-button 
      v-if="mode === 'button'"
      :icon="themeIcon"
      circle
      size="small"
      @click="toggleTheme"
      :title="themeTooltip"
      class="theme-toggle-btn"
    />
    
    <!-- 下拉选择器 -->
    <el-dropdown v-else-if="mode === 'dropdown'" @command="setTheme" trigger="click">
      <el-button :icon="themeIcon" circle size="small" :title="themeTooltip" />
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item 
            v-for="option in themeOptions" 
            :key="option.value"
            :command="option.value"
            :class="{ 'is-active': currentTheme === option.value }"
          >
            <el-icon><component :is="option.icon" /></el-icon>
            <span>{{ option.label }}</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 单选按钮组 -->
    <el-radio-group 
      v-else-if="mode === 'radio'"
      v-model="currentTheme" 
      @change="setTheme"
      size="small"
      class="theme-radio-group"
    >
      <el-radio-button 
        v-for="option in themeOptions" 
        :key="option.value"
        :value="option.value"
        :title="option.label"
      >
        <el-icon><component :is="option.icon" /></el-icon>
        <span v-if="showLabels">{{ option.label }}</span>
      </el-radio-button>
    </el-radio-group>

    <!-- 开关样式 -->
    <div v-else-if="mode === 'switch'" class="theme-switch">
      <el-icon class="theme-switch-icon"><Sunny /></el-icon>
      <el-switch
        v-model="isDarkMode"
        @change="toggleTheme"
        :active-icon="Moon"
        :inactive-icon="Sunny"
        inline-prompt
        size="large"
      />
      <el-icon class="theme-switch-icon"><Moon /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'

const props = defineProps({
  // 组件模式：button | dropdown | radio | switch
  mode: {
    type: String,
    default: 'button',
    validator: (value) => ['button', 'dropdown', 'radio', 'switch'].includes(value)
  },
  // 是否显示文字标签（仅radio模式有效）
  showLabels: {
    type: Boolean,
    default: false
  }
})

const { 
  currentTheme, 
  isDarkMode, 
  setTheme, 
  toggleTheme, 
  getThemeOptions,
  getThemeInfo 
} = useTheme()

// 主题选项
const themeOptions = computed(() => getThemeOptions())

// 当前主题图标
const themeIcon = computed(() => {
  const themeInfo = getThemeInfo()
  if (themeInfo.current === 'theme-auto') {
    return Monitor
  }
  return themeInfo.isDark ? Moon : Sunny
})

// 主题提示文字
const themeTooltip = computed(() => {
  const themeInfo = getThemeInfo()
  if (themeInfo.current === 'theme-auto') {
    return '跟随系统主题'
  }
  return themeInfo.isDark ? '切换到浅色主题' : '切换到深色主题'
})
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
}

.theme-toggle-btn {
  transition: all var(--oa-transition-fast);
}

.theme-toggle-btn:hover {
  transform: rotate(180deg);
}

.theme-radio-group {
  background-color: var(--oa-bg-secondary);
  border-radius: var(--oa-border-radius-base);
  padding: var(--oa-spacing-xs);
}

.theme-radio-group .el-radio-button__inner {
  border: none;
  background-color: transparent;
  color: var(--oa-text-regular);
  transition: all var(--oa-transition-fast);
  display: flex;
  align-items: center;
  gap: var(--oa-spacing-xs);
}

.theme-radio-group .el-radio-button.is-active .el-radio-button__inner {
  background-color: var(--oa-primary-color);
  color: var(--oa-bg-white);
  box-shadow: var(--oa-shadow-light);
}

.theme-switch {
  display: flex;
  align-items: center;
  gap: var(--oa-spacing-sm);
}

.theme-switch-icon {
  color: var(--oa-text-secondary);
  transition: color var(--oa-transition-fast);
}

.theme-switch-icon:first-child {
  color: var(--oa-warning-color);
}

.theme-switch-icon:last-child {
  color: var(--oa-info-color);
}

/* 下拉菜单项样式 */
.el-dropdown-menu__item.is-active {
  background-color: var(--oa-primary-light-9);
  color: var(--oa-primary-color);
}

.el-dropdown-menu__item.is-active .el-icon {
  color: var(--oa-primary-color);
}

.el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: var(--oa-spacing-sm);
}

/* 主题切换动画 */
.theme-toggle {
  position: relative;
}

.theme-toggle::before {
  content: '';
  position: absolute;
  inset: -4px;
  background: conic-gradient(
    from 0deg,
    transparent,
    var(--oa-primary-color),
    transparent,
    var(--oa-primary-color),
    transparent
  );
  border-radius: inherit;
  opacity: 0;
  transition: opacity var(--oa-transition-fast);
  z-index: -1;
  animation: rotate 2s linear infinite;
}

.theme-toggle:hover::before {
  opacity: 0.1;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .theme-radio-group span {
    display: none;
  }
  
  .theme-switch {
    gap: var(--oa-spacing-xs);
  }
  
  .theme-switch-icon {
    font-size: var(--oa-font-size-sm);
  }
}
</style>