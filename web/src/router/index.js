import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/dashboard'
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'projects',
          name: 'Projects',
          component: () => import('@/views/Projects.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: 'new',
              name: 'ProjectCreate',
              component: () => import('@/views/ProjectForm.vue')
            },
            {
              path: 'edit/:id',
              name: 'ProjectEdit',
              component: () => import('@/views/ProjectForm.vue')
            },
            {
              path: ':id',
              name: 'ProjectDetail',
              component: () => import('@/views/ProjectDetail.vue')
            }
          ]
        },
        {
          path: 'tasks',
          name: 'Tasks',
          component: () => import('@/views/Tasks.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'outsourcing-management',
          name: 'OutsourcingManagement',
          component: () => import('@/views/OutsourcingManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'payment-management',
          name: 'PaymentManagement',
          component: () => import('@/views/PaymentManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'budget-management',
          name: 'BudgetManagement',
          component: () => import('@/views/BudgetManagement.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: 'new',
              name: 'BudgetCreate',
              component: () => import('@/views/BudgetForm.vue')
            },
            {
              path: ':id',
              name: 'BudgetDetail',
              component: () => import('@/views/BudgetDetail.vue')
            },
            {
              path: 'edit/:id',
              name: 'BudgetEdit',
              component: () => import('@/views/BudgetForm.vue')
            }
          ]
        },
        {
          path: 'performance-management',
          name: 'PerformanceManagement',
          component: () => import('@/views/PerformanceManagement.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: 'new',
              name: 'PerformanceCreate',
              component: () => import('@/views/PerformanceForm.vue')
            },
            {
              path: ':id',
              name: 'PerformanceDetail',
              component: () => import('@/views/PerformanceDetail.vue')
            },
            {
              path: 'edit/:id',
              name: 'PerformanceEdit',
              component: () => import('@/views/PerformanceForm.vue')
            }
          ]
        },
        {
          path: 'time-management',
          name: 'TimeManagement',
          component: () => import('@/views/TimeManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'project-manager-time',
          name: 'ProjectManagerTime',
          component: () => import('@/views/ProjectManagerTimeManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'leave-management',
          name: 'LeaveManagement',
          component: () => import('@/views/LeaveManagement.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: 'new',
              name: 'LeaveCreate',
              component: () => import('@/views/LeaveForm.vue')
            },
            {
              path: ':id',
              name: 'LeaveDetail',
              component: () => import('@/views/LeaveDetail.vue')
            },
            {
              path: 'edit/:id',
              name: 'LeaveEdit',
              component: () => import('@/views/LeaveForm.vue')
            }
          ]
        },
        {
          path: 'travel-management',
          name: 'TravelManagement',
          component: () => import('@/views/TravelManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'reimbursement',
          name: 'Reimbursement',
          component: () => import('@/views/Reimbursement.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: 'new',
              name: 'ReimbursementCreate',
              component: () => import('@/views/ReimbursementForm.vue')
            },
            {
              path: ':id',
              name: 'ReimbursementDetail',
              component: () => import('@/views/ReimbursementDetail.vue')
            },
            {
              path: 'edit/:id',
              name: 'ReimbursementEdit',
              component: () => import('@/views/ReimbursementForm.vue')
            }
          ]
        },
        {
          path: 'approvals',
          name: 'Approvals',
          component: () => import('@/views/Approvals.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'user-management',
          name: 'UserManagement',
          component: () => import('@/views/UserManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'role-management',
          name: 'RoleManagement',
          component: () => import('@/views/RoleManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'permission-management',
          name: 'PermissionManagement',
          component: () => import('@/views/PermissionManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'log-management',
          name: 'LogManagement',
          component: () => import('@/views/LogManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'notification',
          name: 'Notification',
          component: () => import('@/views/Notification.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: 'new',
              name: 'NotificationCreate',
              component: () => import('@/views/NotificationForm.vue')
            },
            {
              path: ':id',
              name: 'NotificationDetail',
              component: () => import('@/views/NotificationDetail.vue')
            },
            {
              path: 'edit/:id',
              name: 'NotificationEdit',
              component: () => import('@/views/NotificationForm.vue')
            }
          ]
        },
        {
          path: 'announcement',
          name: 'Announcement',
          component: () => import('@/views/Announcement.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: 'new',
              name: 'AnnouncementCreate',
              component: () => import('@/views/AnnouncementForm.vue')
            },
            {
              path: ':id',
              name: 'AnnouncementDetail',
              component: () => import('@/views/AnnouncementDetail.vue')
            },
            {
              path: 'edit/:id',
              name: 'AnnouncementEdit',
              component: () => import('@/views/AnnouncementForm.vue')
            }
          ]
        },
        {
          path: 'system-config',
          name: 'SystemConfig',
          component: () => import('@/views/SystemConfig.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/Profile.vue'),
          meta: { requiresAuth: true }
        }
      ]
    }
  ]
})

// 路由与权限的映射关系
const routePermissionMap = {
  'Dashboard': 'dashboard:view',
  'Projects': 'project:view',
  'Tasks': 'task:view',
  'OutsourcingManagement': 'outsourcing:view',
  'PaymentManagement': 'payment:view',
  'BudgetManagement': 'budget:view',
  'PerformanceManagement': 'performance:view',
  'TimeManagement': 'time:view',
  'ProjectManagerTime': 'manager-time:view',
  'LeaveManagement': 'leave:view',
  'TravelManagement': 'travel:view',
  'Reimbursement': 'reimbursement:view',
  'Approvals': 'approval:view',
  'UserManagement': 'user:view',
  'RoleManagement': 'role:view',
  'PermissionManagement': 'permission:view',
  'LogManagement': 'log:view',
  'Notification': 'notification:view',
  'Announcement': 'announcement:view',
  'SystemConfig': 'system:view',
  'Profile': 'profile:view'
};

router.beforeEach((to, from, next) => {
  const userStore = useUserStore();

  // 检查是否需要登录
  if (to.matched.some(record => record.meta.requiresAuth) && !userStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }

  // 如果是admin用户，允许访问所有路由
  if (userStore.isAdmin) {
    next();
    return;
  }

  // 检查是否有权限访问该路由
  if (to.name && routePermissionMap[to.name]) {
    const requiredPermission = routePermissionMap[to.name];
    if (userStore.hasPermission(requiredPermission)) {
      next();
    } else {
      // 如果没有权限，跳转到仪表盘或者有权限的第一个页面
      if (userStore.hasPermission('dashboard:view')) {
        next({ name: 'Dashboard' });
      } else if (userStore.hasPermission('profile:view')) {
        next({ name: 'Profile' });
      } else {
        // 如果连基本权限都没有，跳转到登录页
        next({ name: 'Login' });
      }
    }
  } else {
    // 如果路由没有配置权限要求，允许访问
    next();
  }
});

export default router