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
          <div class="header-left"></div>
          <div class="header-right">
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
}

.el-aside {
  background-color: #304156;
  transition: width 0.3s;
}

.el-menu-vertical:not(.el-menu--collapse) {
  width: 200px;
  min-height: 400px;
}

.el-menu {
  border-right: none;
}

.el-menu-item.is-active {
  background-color: #2d8cf0 !important;
}

.el-sub-menu__title:hover,
.el-menu-item:hover {
  background-color: #263445 !important;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  height: 50px;
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
}

.toggle-sidebar {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #303133;
}

.user-dropdown .el-icon {
  margin-left: 5px;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
  height: calc(100vh - 50px);
}
</style>
