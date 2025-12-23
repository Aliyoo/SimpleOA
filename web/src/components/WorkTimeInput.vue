<template>
  <div class="worktime-input-container" :class="containerClass">
    <el-input-number
      v-model="localValue"
      :min="min"
      :max="max"
      :step="step"
      :precision="precision"
      :controls="showControls"
      :controls-position="controlsPosition"
      :size="size"
      :disabled="disabled"
      :placeholder="placeholder"
      class="hours-input"
      @change="handleChange"
      @blur="handleBlur"
      @focus="handleFocus"
    />

    <!-- 错误提示 -->
    <el-tooltip
      v-if="errorMessage"
      :content="errorMessage"
      placement="top"
      effect="dark"
    >
      <el-icon class="error-icon">
        <Warning />
      </el-icon>
    </el-tooltip>

    <!-- 警告提示 -->
    <el-tooltip
      v-if="warningMessage"
      :content="warningMessage"
      placement="top"
      effect="dark"
    >
      <el-icon class="warning-icon">
        <Warning />
      </el-icon>
    </el-tooltip>

    <!-- 成功标识 -->
    <el-icon v-if="showSuccess" class="success-icon">
      <CircleCheck />
    </el-icon>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, CircleCheck } from '@element-plus/icons-vue'

// ==================== Props ====================
const props = defineProps({
  // 绑定的工时值
  modelValue: {
    type: Number,
    default: 0
  },

  // 项目ID（用于跨项目校验）
  projectId: {
    type: Number,
    required: true
  },

  // 用户ID（用于跨项目校验）
  userId: {
    type: Number,
    required: true
  },

  // 日期（用于跨项目校验）
  date: {
    type: String,
    required: true
  },

  // 是否为工作日
  isWorkday: {
    type: Boolean,
    default: true
  },

  // 最小值
  min: {
    type: Number,
    default: 0
  },

  // 最大值
  max: {
    type: Number,
    default: 8
  },

  // 步长
  step: {
    type: Number,
    default: 1
  },

  // 精度
  precision: {
    type: Number,
    default: 0
  },

  // 是否显示控制按钮
  showControls: {
    type: Boolean,
    default: false
  },

  // 控制按钮位置
  controlsPosition: {
    type: String,
    default: 'right' // right | 'both'
  },

  // 大小
  size: {
    type: String,
    default: 'small' // large | default | small
  },

  // 是否禁用
  disabled: {
    type: Boolean,
    default: false
  },

  // 占位符
  placeholder: {
    type: String,
    default: '0'
  },

  // 是否显示成功标识
  showSuccessIndicator: {
    type: Boolean,
    default: false
  },

  // 是否启用跨项目校验
  enableCrossProjectValidation: {
    type: Boolean,
    default: true
  }
})

// ==================== Emits ====================
const emit = defineEmits([
  'update:modelValue', // Value emitted when hours change
  'change',           // Value + validation info
  'blur',
  'focus'
])

// ==================== Reactive State ====================
const localValue = ref(props.modelValue)
const errorMessage = ref('')
const warningMessage = ref('')
const showSuccess = ref(false)
const isValidating = ref(false)

// ==================== Computed ====================
const containerClass = computed(() => ({
  'has-error': errorMessage.value,
  'has-warning': warningMessage.value,
  'is-disabled': props.disabled,
  'is-non-workday': !props.isWorkday
}))

// ==================== Watchers ====================
watch(() => props.modelValue, (newVal) => {
  if (newVal !== localValue.value) {
    localValue.value = newVal
  }
})

watch(localValue, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    emit('update:modelValue', newVal)
    validateHours(newVal)

    // Show success indicator
    if (props.showSuccessIndicator && newVal !== oldVal) {
      showSuccess.value = true
      setTimeout(() => {
        showSuccess.value = false
      }, 1000)
    }
  }
})

// ==================== Validation Functions ====================

/**
 * Validate hours value
 * Check ranges, workday status, and cross-project totals
 */
async function validateHours(value) {
  if (isValidating.value) return

  isValidating.value = true
  errorMessage.value = ''
  warningMessage.value = ''

  try {
    // 1. Validate range
    if (value < props.min) {
      errorMessage.value = `工时不能小于${props.min}小时`
      return false
    }

    if (value > props.max) {
      errorMessage.value = `工时不能大于${props.max}小时`
      return false
    }

    // 2. Validate workday
    if (!props.isWorkday && value > 0) {
      warningMessage.value = '非工作日`date`不允许填写工时'
    }

    // 3. Cross-project validation (if enabled)
    if (props.enableCrossProjectValidation && value > 0) {
      await validateCrossProjectTotal(value)
    }

    return errorMessage.value === ''
  } catch (error) {
    console.error('验证失败:', error)
    return false
  } finally {
    isValidating.value = false
  }
}

/**
 * Validate cross-project total hours
 * Check if user exceeds daily limit across projects
 */
async function validateCrossProjectTotal(currentValue) {
  try {
    // Get total hours for this user on this date
    // This is passed in as a prop or computed from parent
    const totalHours = await getDailyTotalHours()

    if (totalHours > props.max) {
      const projectHours = totalHours - currentValue
      const maxAllowed = Math.max(0, props.max - projectHours)

      if (currentValue > maxAllowed) {
        errorMessage.value = `跨项目总工时${totalHours}h，超过${props.max}h限制`
        return false
      }
    }

    return true
  } catch (error) {
    console.error('跨项目验证失败:', error)
    return true
    // Don't block on validation errors
  }
}

/**
 * Get daily total hours for user (replaced with actual logic from parent)
 * This is a placeholder - in real usage, parent should provide this
 */
async function getDailyTotalHours() {
  // Log for debugging
  console.log(`Validating cross-project: userId=${props.userId}, date=${props.date}`)

  // Default: return current value (simple validation)
  // In parent component, provide actual cross-project total
  return new Promise((resolve) => {
    resolve(localValue.value)
  })
}

// ==================== Event Handlers ====================

/**
 * Handle value change
 */
function handleChange(value) {
  emit('change', {
    value,
    projectId: props.projectId,
    userId: props.userId,
    date: props.date,
    isValid: errorMessage.value === ''
  })
}

/**
 * Handle blur event
 */
function handleBlur() {
  emit('blur', {
    value: localValue.value,
    projectId: props.projectId,
    userId: props.userId,
    date: props.date
  })
}

/**
 * Handle focus event
 */
function handleFocus() {
  emit('focus', {
    projectId: props.projectId,
    userId: props.userId,
    date: props.date
  })
}

/**
 * Set value programmatically
 */
function setValue(value) {
  localValue.value = value
}

/**
 * Get current validation state
 */
function getValidationState() {
  return {
    isValid: errorMessage.value === '' && warningMessage.value === '',
    error: errorMessage.value,
    warning: warningMessage.value,
    value: localValue.value
  }
}

/**
 * Reset validation state
 */
function resetValidation() {
  errorMessage.value = ''
  warningMessage.value = ''
}

// Expose methods for parent component
defineExpose({
  validateHours,
  setValue,
  getValidationState,
  resetValidation
})
</script>

<style scoped>
.worktime-input-container {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hours-input {
  width: 100%;
}

/* Styles for different states */
.has-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset;
}

.has-error :deep(.el-input__wrapper):hover {
  box-shadow: 0 0 0 1px #f56c6c inset;
}

.has-warning :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e6a23c inset;
}

.is-disabled :deep(.el-input__wrapper) {
  background-color: #f5f7fa;
  color: #c0c4cc;
}

/* Error/Warning/Success icons */
.error-icon,
.warning-icon,
.success-icon {
  position: absolute;
  right: 4px;
  font-size: 14px;
  z-index: 10;
  pointer-events: none;
}

.error-icon {
  color: #f56c6c;
}

.warning-icon {
  color: #e6a23c;
}

.success-icon {
  color: #67c23a;
  animation: fadeInOut 1.5s ease-in-out;
}

@keyframes fadeInOut {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

/* Non-workday styling */
.is-non-workday :deep(.el-input__wrapper) {
  background-color: #fafafa;
}

/* Size specific styles */
:deep(.el-input-number--small) {
  width: 70px;
}

:deep(.el-input-number--default) {
  width: 80px;
}

:deep(.el-input-number--large) {
  width: 90px;
}

/* Control button styles */
:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  width: 18px;
  font-size: 12px;
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  color: #409eff;
}
</style>
