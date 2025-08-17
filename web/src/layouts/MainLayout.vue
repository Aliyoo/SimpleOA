<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <el-menu
        :default-active="route.path"
        class="el-menu-vertical"
        :router="true"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <!-- Admin用户显示所有菜单 -->
        <template v-if="userStore.isAdmin">
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>

          <!-- OA办公模块 -->
          <el-sub-menu index="oa">
            <template #title>
              <el-icon><OfficeBuilding /></el-icon>
              <span>OA办公</span>
            </template>
            <el-menu-item index="/time-management">
              <el-icon><Clock /></el-icon>
              <span>工时管理</span>
            </el-menu-item>
            <el-menu-item index="/project-manager-time">
              <el-icon><Clock /></el-icon>
              <span>项目工时</span>
            </el-menu-item>
            <el-menu-item index="/leave-management">
              <el-icon><Calendar /></el-icon>
              <span>请假管理</span>
            </el-menu-item>
            <el-menu-item index="/travel-management">
              <el-icon><Position /></el-icon>
              <span>出差管理</span>
            </el-menu-item>
            <el-menu-item index="/reimbursement">
              <el-icon><Wallet /></el-icon>
              <span>报销管理</span>
            </el-menu-item>
            <el-menu-item index="/approvals">
              <el-icon><Document /></el-icon>
              <span>审批管理</span>
            </el-menu-item>
            <el-menu-item index="/workday-management">
              <el-icon><Calendar /></el-icon>
              <span>工作日管理</span>
            </el-menu-item>
            <el-menu-item index="/holiday-management">
              <el-icon><Calendar /></el-icon>
              <span>节假日管理</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 项目管理模块 -->
          <el-sub-menu index="pm">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>项目管理</span>
            </template>
            <el-menu-item index="/projects">
              <el-icon><Tickets /></el-icon>
              <span>项目信息</span>
            </el-menu-item>
            <el-menu-item index="/tasks">
              <el-icon><List /></el-icon>
              <span>任务管理</span>
            </el-menu-item>
            <el-menu-item index="/outsourcing-management">
              <el-icon><Connection /></el-icon>
              <span>外包管理</span>
            </el-menu-item>
            <el-menu-item index="/payment-management">
              <el-icon><Money /></el-icon>
              <span>付款管理</span>
            </el-menu-item>
            <el-menu-item index="/budget-management">
              <el-icon><Coin /></el-icon>
              <span>预算管理</span>
            </el-menu-item>
            <el-menu-item index="/budget-expense-management">
              <el-icon><Coin /></el-icon>
              <span>预算支出管理</span>
            </el-menu-item>
            <el-menu-item index="/performance-management">
              <el-icon><DataAnalysis /></el-icon>
              <span>绩效管理</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 系统管理模块 -->
          <el-sub-menu index="sys">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/user-management">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item index="/role-management">
              <el-icon><Lock /></el-icon>
              <span>角色管理</span>
            </el-menu-item>
            <el-menu-item index="/permission-management">
              <el-icon><Key /></el-icon>
              <span>权限管理</span>
            </el-menu-item>
            <el-menu-item index="/log-management">
              <el-icon><DocumentCopy /></el-icon>
              <span>日志管理</span>
            </el-menu-item>
            <el-menu-item index="/notification">
              <el-icon><Bell /></el-icon>
              <span>通知管理</span>
            </el-menu-item>
            <el-menu-item index="/announcement">
              <el-icon><Notification /></el-icon>
              <span>公告管理</span>
            </el-menu-item>
            <el-menu-item index="/system-config">
              <el-icon><Tools /></el-icon>
              <span>系统配置</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </el-menu-item>
        </template>

        <!-- 非Admin用户动态生成菜单 -->
        <template v-else-if="userStore.menus && userStore.menus.length > 0">
          <!-- 顶级菜单 -->
          <template v-for="menu in userStore.menus" :key="`menu-outer-${menu.id}`">
            <!-- 如果没有子菜单，直接显示菜单项 -->
            <el-menu-item
              v-if="!menu.children || menu.children.length === 0"
              :key="`menu-${menu.id}`"
              :index="menu.path"
            >
              <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
              <span>{{ menu.name }}</span>
            </el-menu-item>

            <!-- 如果有子菜单，显示子菜单 -->
            <el-sub-menu v-else :key="`submenu-${menu.id}`" :index="menu.path || String(menu.id)">
              <template #title>
                <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
                <span>{{ menu.name }}</span>
              </template>

              <!-- 递归渲染子菜单 -->
              <el-menu-item
                v-for="child in getVisibleChildren(menu.children)"
                :key="`child-${child.id}`"
                :index="child.path"
              >
                <el-icon><component :is="getIconComponent(child.icon)" /></el-icon>
                <span>{{ child.name }}</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </template>

        <!-- 如果没有菜单数据，显示默认菜单 -->
        <template v-else>
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>

          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-container">
          <div class="header-left">
            <h1 class="header-title">SimpleOA 办公系统</h1>
          </div>
          <div class="header-right">
            <ThemeToggle mode="dropdown" />
            <el-dropdown>
              <span class="user-dropdown">
                <el-avatar
                  size="small"
                  :src="userStore.user?.avatar || defaultAvatar"
                  style="margin-right: 8px"
                ></el-avatar>
                {{ userStore.user?.realName || userStore.user?.name || userStore.user?.username || '用户' }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goToProfile">个人资料</el-dropdown-item>
                  <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import defaultAvatar from '@/assets/default-avatar.png'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { ArrowDown } from '@element-plus/icons-vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

// 导入所有需要的图标组件
const {
  Odometer,
  OfficeBuilding,
  Clock,
  Calendar,
  Position,
  Wallet,
  Document,
  Folder,
  Tickets,
  List,
  Connection,
  Money,
  Coin,
  DataAnalysis,
  Setting,
  User,
  Lock,
  Key,
  DocumentCopy,
  Bell,
  Notification,
  Tools
} = ElementPlusIconsVue

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 获取图标组件
const getIconComponent = (iconName) => {
  if (!iconName) return ElementPlusIconsVue.Document // 默认图标

  // 如果图标名称存在于ElementPlusIconsVue中，则返回对应的组件
  if (ElementPlusIconsVue[iconName]) {
    return ElementPlusIconsVue[iconName]
  }

  // 根据路径或名称映射图标
  const iconMap = {
    dashboard: ElementPlusIconsVue.Odometer,
    'time-management': ElementPlusIconsVue.Clock,
    'project-manager-time': ElementPlusIconsVue.Clock,
    'leave-management': ElementPlusIconsVue.Calendar,
    'travel-management': ElementPlusIconsVue.Position,
    reimbursement: ElementPlusIconsVue.Wallet,
    approvals: ElementPlusIconsVue.Document,
    projects: ElementPlusIconsVue.Tickets,
    tasks: ElementPlusIconsVue.List,
    'outsourcing-management': ElementPlusIconsVue.Connection,
    'payment-management': ElementPlusIconsVue.Money,
    'budget-management': ElementPlusIconsVue.Coin,
    'budget-expense-management': ElementPlusIconsVue.Coin,
    'performance-management': ElementPlusIconsVue.DataAnalysis,
    'user-management': ElementPlusIconsVue.User,
    'role-management': ElementPlusIconsVue.Lock,
    'permission-management': ElementPlusIconsVue.Key,
    'log-management': ElementPlusIconsVue.DocumentCopy,
    notification: ElementPlusIconsVue.Bell,
    announcement: ElementPlusIconsVue.Notification,
    'system-config': ElementPlusIconsVue.Tools,
    calendar: ElementPlusIconsVue.Calendar,
    profile: ElementPlusIconsVue.User,
    oa: ElementPlusIconsVue.OfficeBuilding,
    pm: ElementPlusIconsVue.Folder,
    sys: ElementPlusIconsVue.Setting
  }

  return iconMap[iconName] || ElementPlusIconsVue.Document
}

// 获取可见的子菜单
const getVisibleChildren = (children) => {
  if (!children || !Array.isArray(children)) return []
  return children.filter((child) => !child.isHidden)
}

const handleLogout = async () => {
  try {
    await userStore.logout()
    router.push('/login')
  } catch (error) {
    console.error('Logout failed:', error)
  }
}

const goToProfile = () => {
  router.push('/profile')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
  background-color: var(--oa-bg-primary);
}

.el-aside {
  background-color: var(--oa-sidebar-bg);
  transition: width var(--oa-transition-base);
  width: var(--oa-sidebar-width);
  box-shadow: var(--oa-shadow-light);
}

.el-menu-vertical:not(.el-menu--collapse) {
  width: var(--oa-sidebar-width);
  min-height: 100vh;
}

.el-menu {
  border-right: none;
  background-color: transparent;
}

.el-menu-item {
  color: var(--oa-sidebar-text);
  transition: all var(--oa-transition-fast);
  border-radius: var(--oa-border-radius-base);
  margin: var(--oa-spacing-xs) var(--oa-spacing-sm);
}

.el-menu-item.is-active {
  background-color: var(--oa-sidebar-active-bg) !important;
  color: var(--oa-bg-white) !important;
  box-shadow: var(--oa-shadow-light);
}

.el-sub-menu__title {
  color: var(--oa-sidebar-text);
  transition: all var(--oa-transition-fast);
  border-radius: var(--oa-border-radius-base);
  margin: var(--oa-spacing-xs) var(--oa-spacing-sm);
}

.el-sub-menu__title:hover,
.el-menu-item:hover {
  background-color: var(--oa-sidebar-hover-bg) !important;
  color: var(--oa-sidebar-active-text) !important;
  transform: translateX(2px);
}

.el-header {
  background-color: var(--oa-bg-white);
  border-bottom: 1px solid var(--oa-border-light);
  padding: 0 var(--oa-spacing-lg);
  height: var(--oa-header-height);
  box-shadow: var(--oa-shadow-light);
  z-index: var(--oa-z-index-top);
}

.header-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--oa-spacing-base);
}

.header-title {
  font-size: var(--oa-font-size-lg);
  font-weight: var(--oa-font-weight-medium);
  color: var(--oa-text-primary);
  margin: 0;
}

.toggle-sidebar {
  font-size: var(--oa-font-size-xl);
  cursor: pointer;
  color: var(--oa-text-regular);
  transition: color var(--oa-transition-fast);
  padding: var(--oa-spacing-sm);
  border-radius: var(--oa-border-radius-base);
}

.toggle-sidebar:hover {
  color: var(--oa-primary-color);
  background-color: var(--oa-primary-light-9);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--oa-spacing-base);
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: var(--oa-text-primary);
  padding: var(--oa-spacing-sm) var(--oa-spacing-base);
  border-radius: var(--oa-border-radius-base);
  transition: all var(--oa-transition-fast);
  font-weight: var(--oa-font-weight-medium);
}

.user-dropdown:hover {
  background-color: var(--oa-primary-light-9);
  color: var(--oa-primary-color);
}

.user-dropdown .el-icon {
  margin-left: var(--oa-spacing-xs);
  transition: transform var(--oa-transition-fast);
}

.user-dropdown:hover .el-icon {
  transform: rotate(180deg);
}

.el-main {
  background-color: var(--oa-bg-primary);
  padding: var(--oa-content-padding);
  overflow-y: auto;
  height: calc(100vh - var(--oa-header-height));
  min-width: var(--oa-content-min-width);
}

/* 侧边栏子菜单样式优化 */
.el-sub-menu .el-menu-item {
  background-color: transparent;
  color: var(--oa-sidebar-text);
  padding-left: 48px !important;
}

.el-sub-menu .el-menu-item.is-active {
  background-color: var(--oa-primary-color) !important;
  color: var(--oa-bg-white) !important;
}

.el-sub-menu .el-menu-item:hover {
  background-color: var(--oa-sidebar-hover-bg) !important;
  color: var(--oa-sidebar-active-text) !important;
}

/* 头像样式优化 */
.el-avatar {
  border: 2px solid var(--oa-border-light);
  transition: border-color var(--oa-transition-fast);
}

.user-dropdown:hover .el-avatar {
  border-color: var(--oa-primary-color);
}

/* 菜单图标优化 */
.el-menu-item .el-icon,
.el-sub-menu__title .el-icon {
  margin-right: var(--oa-spacing-sm);
  font-size: var(--oa-font-size-md);
  color: inherit;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .el-aside {
    width: var(--oa-sidebar-collapsed-width);
  }
  
  .el-menu-vertical:not(.el-menu--collapse) {
    width: var(--oa-sidebar-collapsed-width);
  }
  
  .el-main {
    padding: var(--oa-spacing-base);
    min-width: auto;
  }
  
  .header-container {
    padding: 0 var(--oa-spacing-base);
  }
  
  .header-title {
    display: none;
  }
}

/* 布局收缩动画 */
.layout-container.collapsed .el-aside {
  width: var(--oa-sidebar-collapsed-width);
}

.layout-container.collapsed .el-menu-item span,
.layout-container.collapsed .el-sub-menu__title span {
  display: none;
}

/* 加载状态 */
.layout-container.loading {
  pointer-events: none;
}

.layout-container.loading::after {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--oa-bg-overlay);
  z-index: var(--oa-z-index-modal);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
