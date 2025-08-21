<template>
  <div class="login-container">
    <!-- 主题切换按钮 -->
    <div class="theme-toggle-wrapper">
      <ThemeToggle mode="button" />
    </div>
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
      auto-complete="on"
      label-position="left"
    >
      <h3 class="title">SimpleOA 登录</h3>
      <p class="subtitle">SuperOA By AI Coding</p>
      <el-form-item prop="username">
        <el-input
          ref="usernameInput"
          v-model="loginForm.username"
          name="username"
          type="text"
          placeholder="用户名"
          :disabled="loading"
          @keyup.enter="handleLogin"
        ></el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          name="password"
          type="password"
          placeholder="密码"
          show-password
          :disabled="loading"
          @keyup.enter="handleLogin"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button :loading="loading" type="primary" class="login-button" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTheme } from '@/composables/useTheme'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 初始化主题
const { initTheme } = useTheme()

const loginForm = ref({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loading = ref(false)
const loginFormRef = ref()
const usernameInput = ref()

const handleLogin = () => {
  if (loading.value) return

  loginFormRef.value?.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm.value)
        // 获取跳转地址，如果没有则跳转到仪表盘页面
        const redirect = route.query.redirect || '/dashboard'
        router.push(redirect)
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  // 初始化主题
  initTheme()
  
  // 自动聚焦用户名输入框
  usernameInput.value?.focus()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
  position: relative;
  transition: all var(--oa-transition-base);
}

.theme-dark .login-container {
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
}

.theme-toggle-wrapper {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
}

.login-form {
  position: relative;
  width: 400px;
  max-width: 100%;
  padding: 40px 35px;
  margin: 0;
  overflow: hidden;
  background: var(--oa-bg-card);
  box-shadow: var(--oa-shadow-dark);
  border-radius: var(--oa-border-radius-lg);
  border: 1px solid var(--oa-border-light);
  transition: all var(--oa-transition-base);
  backdrop-filter: blur(10px);
}

.title {
  font-size: 26px;
  color: var(--oa-text-primary);
  margin: 0 auto 10px auto;
  text-align: center;
  font-weight: var(--oa-font-weight-bold);
  transition: color var(--oa-transition-base);
}

.subtitle {
  font-size: 12px;
  color: var(--oa-text-secondary);
  margin: 0 auto 30px auto;
  text-align: center;
  font-weight: var(--oa-font-weight-normal);
  transition: color var(--oa-transition-base);
}

.login-button {
  width: 100%;
  background-color: var(--oa-primary-color);
  border-radius: var(--oa-border-radius-base);
  color: var(--oa-bg-white);
  border: none;
  font-weight: var(--oa-font-weight-medium);
  height: 40px;
  font-size: var(--oa-font-size-base);
  transition: all var(--oa-transition-fast);
}

.login-button:hover {
  background-color: var(--oa-primary-light-1);
  transform: translateY(-1px);
  box-shadow: var(--oa-shadow-light);
}

/* 登录表单内的输入框样式 */
.login-form :deep(.el-input__wrapper) {
  background-color: var(--oa-bg-white);
  border: 1px solid var(--oa-border-base);
  transition: all var(--oa-transition-fast);
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--oa-primary-light-3);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--oa-primary-color);
  box-shadow: 0 0 0 2px var(--oa-primary-light-9);
}

.login-form :deep(.el-input__inner) {
  color: var(--oa-text-primary);
  background-color: transparent;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: var(--oa-text-placeholder);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-form {
    width: 90%;
    padding: 30px 25px;
  }
  
  .theme-toggle-wrapper {
    top: 15px;
    right: 15px;
  }
  
  .title {
    font-size: 22px;
  }
}
</style>
