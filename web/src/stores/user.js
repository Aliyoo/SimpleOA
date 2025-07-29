import { defineStore } from 'pinia'
import api from '../utils/axios'
import { ElMessage } from 'element-plus'
import { getItem, setItem, removeItem, clearUserData } from '../utils/storage'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: getItem('user', null),
    token: getItem('token', null), // 保留token作为认证状态标识
    permissions: getItem('permissions', []),
    menus: getItem('menus', []),
    isLoading: false, // 添加加载状态
    isUserDataLoaded: false // 标记用户数据是否已完全加载
  }),
  getters: {
    isAuthenticated: (state) => !!state.user || !!state.token,
    isAdmin: (state) => {
      return (
        state.user &&
        (state.user.username === 'admin' ||
          (state.user.roles && state.user.roles.some((role) => role.name === 'ADMIN')))
      )
    },
    hasPermission: (state) => (permission) => {
      // admin用户拥有所有权限
      if (
        state.user &&
        (state.user.username === 'admin' ||
          (state.user.roles && state.user.roles.some((role) => role.name === 'ADMIN')))
      ) {
        return true
      }
      return state.permissions.includes(permission)
    },
    hasAnyPermission: (state) => (permissions) => {
      // admin用户拥有所有权限
      if (
        state.user &&
        (state.user.username === 'admin' ||
          (state.user.roles && state.user.roles.some((role) => role.name === 'ADMIN')))
      ) {
        return true
      }
      return permissions.some((permission) => state.permissions.includes(permission))
    },
    hasAllPermissions: (state) => (permissions) => {
      // admin用户拥有所有权限
      if (
        state.user &&
        (state.user.username === 'admin' ||
          (state.user.roles && state.user.roles.some((role) => role.name === 'ADMIN')))
      ) {
        return true
      }
      return permissions.every((permission) => state.permissions.includes(permission))
    }
  },
  actions: {
    async login(loginForm) {
      try {
        const response = await api.post('/api/auth/login', loginForm)
        if (response.data && response.data.user && response.data.token) {
          this.user = response.data.user
          this.token = response.data.token // 实际 token 由后端通过 HttpOnly Cookie 设置，这里仅作为标识
          // 保存用户信息到localStorage
          setItem('user', this.user)
          setItem('token', this.token)
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
        // 清除localStorage中的所有用户相关数据
        clearUserData()
        ElMessage.success('登出成功')
      } catch (error) {
        console.error('登出操作失败:', error)
        this.user = null
        this.token = null // 清除 token 标识
        this.permissions = []
        this.menus = []
        // 即使出错也要清除localStorage
        clearUserData()
      }
    },
    async fetchUser() {
      // 由于使用 HttpOnly Cookie，无法直接检查 token 存在性，依赖后端验证
      try {
        const response = await api.get('/api/auth/me', {
          skipGlobalErrorHandler: true // 跳过全局错误处理
        })

        let userData = response.data

        // 如果响应数据是字符串，尝试解析为JSON
        if (typeof userData === 'string') {
          try {
            userData = JSON.parse(userData)
          } catch (parseError) {
            console.error('解析用户数据失败:', parseError)
            throw new Error('用户数据格式错误')
          }
        }

        if (userData && userData.id) {
          this.user = userData
          this.token = 'authenticated' // 设置认证标识
          // 保存用户信息到localStorage
          setItem('user', this.user)
          setItem('token', this.token)

          // 获取用户权限和菜单（不阻塞主流程）
          try {
            await this.fetchUserPermissions()
            await this.fetchUserMenus()
          } catch (permError) {
            console.warn('获取用户权限或菜单失败:', permError)
            // 不抛出错误，允许继续使用
          }
        } else {
          throw new Error('无效的用户数据')
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)

        // 尝试从响应中提取用户数据（针对500错误但有数据的情况）
        if (error.response?.status === 500 && error.response?.data) {
          let userData = error.response.data
          if (typeof userData === 'string') {
            try {
              userData = JSON.parse(userData)
              if (userData && userData.id) {
                console.log('从500错误中恢复用户数据:', userData)
                this.user = userData
                this.token = 'authenticated'
                // 保存用户信息到localStorage
                setItem('user', this.user)
                setItem('token', this.token)

                // 尝试获取权限和菜单
                try {
                  await this.fetchUserPermissions()
                  await this.fetchUserMenus()
                } catch (permError) {
                  console.warn('获取用户权限或菜单失败:', permError)
                }
                return // 成功恢复，不抛出错误
              }
            } catch (parseError) {
              console.error('从500错误中解析用户数据失败:', parseError)
            }
          }
        }

        // 认证失败时清除用户信息
        this.user = null
        this.token = null
        this.permissions = []
        this.menus = []
        clearUserData()
        throw error // 重新抛出错误以便调用者处理
      }
    },

    async fetchUserPermissions() {
      try {
        const response = await api.get('/api/auth/permissions')
        if (response.data) {
          this.permissions = response.data
          setItem('permissions', this.permissions)
          this.token = 'authenticated' // 设置一个标识表示已认证
        }
      } catch (error) {
        console.error('获取用户权限失败:', error)
      }
    },

    async fetchUserMenus() {
      try {
        const response = await api.get('/api/menus/user/tree')
        if (response.data) {
          this.menus = response.data
          setItem('menus', this.menus)
          if (!this.token) {
            this.token = 'authenticated' // 设置一个标识表示已认证
          }
        }
      } catch (error) {
        console.error('获取用户菜单失败:', error)
      }
    }
  }
})
