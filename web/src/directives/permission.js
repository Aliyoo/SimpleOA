import { useUserStore } from '@/stores/user'

/**
 * 权限指令
 * 用法：v-permission="'permission:view'"
 * 或者：v-permission="['permission:view', 'permission:edit']"
 */
export const permission = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const { value } = binding

    // 如果是admin用户，拥有所有权限
    if (userStore.isAdmin) {
      return // 保留元素，不做任何处理
    }

    // 获取用户权限
    const permissions = userStore.permissions || []

    // 判断是否有权限
    const hasPermission = (permission) => {
      return permissions.includes(permission)
    }

    // 判断是否有权限
    const checkPermission = () => {
      if (Array.isArray(value)) {
        // 如果是数组，只要有一个权限就可以
        return value.some(permission => hasPermission(permission))
      } else {
        // 如果是字符串，必须有这个权限
        return hasPermission(value)
      }
    }

    // 如果没有权限，则移除元素
    if (!checkPermission()) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

/**
 * 注册权限指令
 */
export function setupPermissionDirective(app) {
  app.directive('permission', permission)
}

export default permission
