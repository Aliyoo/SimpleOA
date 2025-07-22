<template>
  <div class="login-container">
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
        <el-button
          :loading="loading"
          type="primary"
          class="login-button"
          @click="handleLogin"
        >
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

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginForm = ref({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
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
  // 自动聚焦用户名输入框
  usernameInput.value?.focus()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background-color: #2d3a4b;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.login-form {
  position: relative;
  width: 400px;
  max-width: 100%;
  padding: 40px 35px;
  margin: 0;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  border-radius: 12px;
}

.title {
  font-size: 26px;
  color: #333;
  margin: 0 auto 10px auto;
  text-align: center;
  font-weight: bold;
}

.subtitle {
  font-size: 12px;
  color: #999;
  margin: 0 auto 30px auto;
  text-align: center;
  font-weight: normal;
}

.login-button {
  width: 100%;
  background-color: #409eff;
  border-radius: 6px;
  color: #fff;
  border: none;
  font-weight: 500;
}
</style>