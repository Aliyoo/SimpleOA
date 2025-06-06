import { defineStore } from 'pinia'
import api from '../utils/axios'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: null, // 不再从 localStorage 获取 token，因为使用 HttpOnly Cookie
    permissions: JSON.parse(localStorage.getItem('permissions')) || [],
    menus: JSON.parse(localStorage.getItem('menus')) || []
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => {
      return state.user && (
        state.user.username === 'admin' ||
        (state.user.roles && state.user.roles.some(role => role.name === 'ADMIN'))
      )
    },
    hasPermission: (state) => (permission) => {
      // admin用户拥有所有权限
      if (state.user && (
        state.user.username === 'admin' ||
        (state.user.roles && state.user.roles.some(role => role.name === 'ADMIN'))
      )) {
        return true
      }
      return state.permissions.includes(permission)
    },
    hasAnyPermission: (state) => (permissions) => {
      // admin用户拥有所有权限
      if (state.user && (
        state.user.username === 'admin' ||
        (state.user.roles && state.user.roles.some(role => role.name === 'ADMIN'))
      )) {
        return true
      }
      return permissions.some(permission => state.permissions.includes(permission))
    },
    hasAllPermissions: (state) => (permissions) => {
      // admin用户拥有所有权限
      if (state.user && (
        state.user.username === 'admin' ||
        (state.user.roles && state.user.roles.some(role => role.name === 'ADMIN'))
      )) {
        return true
      }
      return permissions.every(permission => state.permissions.includes(permission))
    }
  },
  actions: {
    async login(loginForm) {
      try {
        const response = await api.post('/api/auth/login', loginForm)
        if (response.data && response.data.user && response.data.token) {
            this.user = response.data.user
            this.token = response.data.token // 实际 token 由后端通过 HttpOnly Cookie 设置，这里仅作为标识
            // 在登录成功后立即获取用户权限和菜单
            await this.fetchUserPermissions()
            await this.fetchUserMenus()
            ElMessage.success('登录成功')
            return Promise.resolve(response.data)
        } else {
            ElMessage.error('登录响应格式错误')
            return Promise.reject('Invalid login response')
        }
      } catch (error) {
        const message = error.response?.data?.message || '登录失败，请检查用户名和密码'
        ElMessage.error(message)
        return Promise.reject(error)
      }
    },
    async logout() {
      try {
        this.user = null
        this.token = null // 清除 token 标识，实际 HttpOnly Cookie 由后端清除
        this.permissions = []
        this.menus = []
        localStorage.removeItem('permissions')
        localStorage.removeItem('menus')
        ElMessage.success('登出成功')
      } catch (error) {
        console.error('登出操作失败:', error)
        this.user = null
        this.token = null // 清除 token 标识
        this.permissions = []
        this.menus = []
        localStorage.removeItem('permissions')
        localStorage.removeItem('menus')
      }
    },
    async fetchUser() {
        // 由于使用 HttpOnly Cookie，无法直接检查 token 存在性，依赖后端验证
        try {
            const response = await api.get('/api/auth/me');
            if (response.data) {
                this.user = response.data;
                // 获取用户权限
                await this.fetchUserPermissions();
                // 获取用户菜单
                await this.fetchUserMenus();
            }
        } catch (error) {
            console.error("获取用户信息失败:", error);
            // Token might be invalid or API issue, but don't logout immediately.
            // The navigation guard will still protect routes based on token existence.
            // If API calls requiring authentication start failing, that's a better indicator
            // that the token is truly invalid and logout is needed (e.g., via an Axios interceptor).
            // this.logout(); // <-- 注释掉这一行
        }
    },

    async fetchUserPermissions() {
        try {
            const response = await api.get('/api/auth/permissions');
            if (response.data) {
                this.permissions = response.data;
                localStorage.setItem('permissions', JSON.stringify(this.permissions));
                this.token = 'authenticated'; // 设置一个标识表示已认证
            }
        } catch (error) {
            console.error("获取用户权限失败:", error);
        }
    },

    async fetchUserMenus() {
        try {
            const response = await api.get('/api/menus/user/tree');
            if (response.data) {
                this.menus = response.data;
                localStorage.setItem('menus', JSON.stringify(this.menus));
                if (!this.token) {
                    this.token = 'authenticated'; // 设置一个标识表示已认证
                }
            }
        } catch (error) {
            console.error("获取用户菜单失败:", error);
        }
    }
  }
})