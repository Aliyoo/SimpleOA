<template>
  <div class="dashboard-container">
    <!-- 头部欢迎信息 -->
    <div class="dashboard-header">
      <h1>系统仪表盘</h1>
      <div class="welcome-info">
        <span>欢迎回来，{{ currentUser.realName || currentUser.username }}!</span>
        <span class="last-login">上次登录: {{ formatDate(currentUser.lastLogin) }}</span>
      </div>
    </div>

    <!-- 快速操作区域 -->
    <div class="quick-actions">
      <div class="action-buttons">
        <div class="action-item">
          <router-link to="/time-management">
            <el-button type="primary" size="default">
              <template #icon>
                <el-icon><Timer /></el-icon>
              </template>
              填报工时
            </el-button>
          </router-link>
        </div>
        <div class="action-item">
          <router-link to="/leave-management">
            <el-button type="success" size="default">
              <template #icon>
                <el-icon><DocumentAdd /></el-icon>
              </template>
              申请请假
            </el-button>
          </router-link>
        </div>
        <div class="action-item">
          <router-link to="/reimbursement">
            <el-button type="warning" size="default">
              <template #icon>
                <el-icon><CreditCard /></el-icon>
              </template>
              申请报销
            </el-button>
          </router-link>
        </div>
        <div class="action-item">
          <router-link to="/projects">
            <el-button type="info" size="default">
              <template #icon>
                <el-icon><Briefcase /></el-icon>
              </template>
              项目管理
            </el-button>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stat-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card user-stat">
            <div class="stat-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">用户总数</div>
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-trend">活跃用户: {{ stats.activeUserCount }}</div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card shadow="hover" class="stat-card project-stat">
            <div class="stat-icon">
              <el-icon><FolderOpened /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">项目总数</div>
              <div class="stat-value">{{ stats.projectCount }}</div>
              <div class="stat-trend">开发中: {{ stats.activeProjectCount }}</div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card shadow="hover" class="stat-card approval-stat">
            <div class="stat-icon">
              <el-icon><DocumentChecked /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">待审批</div>
              <div class="stat-value">{{ stats.pendingApprovalCount }}</div>
              <div class="stat-trend">今日: {{ stats.todayApprovalCount }}</div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card
            v-if="userStore.canViewBudget"
            shadow="hover"
            class="stat-card budget-stat"
          >
            <div class="stat-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">预算使用率</div>
              <div class="stat-value">{{ stats.budgetUtilization }}%</div>
              <div class="stat-trend">总预算: {{ formatCurrency(stats.totalBudget) }}</div>
            </div>
          </el-card>
          <!-- 普通用户显示任务统计 -->
          <el-card v-else shadow="hover" class="stat-card task-stat">
            <div class="stat-icon">
              <el-icon><DocumentChecked /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">我的任务</div>
              <div class="stat-value">{{ stats.myTaskCount || 0 }}</div>
              <div class="stat-trend">未完成: {{ stats.pendingTaskCount || 0 }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 工时统计区域 -->
    <div class="worktime-section">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="worktime-card">
            <template #header>
              <div class="card-header">
                <span>今日工时</span>
                <el-icon><Clock /></el-icon>
              </div>
            </template>
            <div class="worktime-display">
              <div class="worktime-value">{{ stats.todayWorkHours }}h</div>
              <div class="worktime-status">{{ getWorkTimeStatus(stats.todayWorkHours) }}</div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="worktime-card">
            <template #header>
              <div class="card-header">
                <span>本周工时</span>
                <el-icon><Calendar /></el-icon>
              </div>
            </template>
            <div class="worktime-display">
              <div class="worktime-value">{{ stats.weekWorkHours }}h</div>
              <div class="worktime-average">平均: {{ (stats.weekWorkHours / 5).toFixed(1) }}h/天</div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="worktime-card">
            <template #header>
              <div class="card-header">
                <span>本月工时</span>
                <el-icon><Calendar /></el-icon>
              </div>
            </template>
            <div class="worktime-display">
              <div class="worktime-value">{{ stats.monthWorkHours }}h</div>
              <div class="worktime-target">目标: {{ getMonthlyTarget() }}h</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>工时趋势</span>
                <el-button-group size="small">
                  <el-button :type="trendPeriod === 7 ? 'primary' : ''" @click="changeTrendPeriod(7)">7天</el-button>
                  <el-button :type="trendPeriod === 30 ? 'primary' : ''" @click="changeTrendPeriod(30)">30天</el-button>
                </el-button-group>
              </div>
            </template>
            <div id="worktimeChart" style="width: 100%; height: 300px"></div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>项目工时分布</span>
                <el-button size="small" @click="refreshProjectStats">刷新</el-button>
              </div>
            </template>
            <div id="projectChart" style="width: 100%; height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 审批流程和财务概览 -->
    <div class="bottom-section">
      <el-row :gutter="20">
        <el-col :span="userStore.canViewBudget ? 12 : 24">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>审批流程统计</span>
                <router-link to="/approvals">
                  <el-button type="text" size="small">查看详情</el-button>
                </router-link>
              </div>
            </template>
            <div id="approvalChart" style="width: 100%; height: 250px"></div>
          </el-card>
        </el-col>

        <el-col v-if="userStore.canViewBudget" :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>财务概览</span>
                <router-link to="/budget-management">
                  <el-button type="text" size="small">查看详情</el-button>
                </router-link>
              </div>
            </template>
            <div class="financial-overview">
              <div class="financial-item">
                <span class="financial-label">总预算</span>
                <span class="financial-value">{{ formatCurrency(stats.totalBudget) }}</span>
              </div>
              <div class="financial-item">
                <span class="financial-label">已支出</span>
                <span class="financial-value expense">{{ formatCurrency(stats.totalExpenses) }}</span>
              </div>
              <div class="financial-item">
                <span class="financial-label">剩余预算</span>
                <span class="financial-value remaining">{{
                  formatCurrency(stats.totalBudget - stats.totalExpenses)
                }}</span>
              </div>
              <div class="financial-item">
                <span class="financial-label">本月报销</span>
                <span class="financial-value">{{ formatCurrency(stats.monthReimbursementAmount) }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useUserStore } from '../stores/user'
import { useTheme } from '@/composables/useTheme'
import api from '../utils/axios.js'
import * as echarts from 'echarts'
import { User, FolderOpened, DocumentChecked, Money, Clock, Calendar, 
         Timer, DocumentAdd, CreditCard, Briefcase, 
         Files, Operation, Refresh } from '@element-plus/icons-vue'

const userStore = useUserStore()
const currentUser = ref(userStore.userInfo || {})
const { isDarkMode, getThemeInfo } = useTheme()

const stats = ref({
  userCount: 0,
  activeUserCount: 0,
  projectCount: 0,
  activeProjectCount: 0,
  pendingApprovalCount: 0,
  todayApprovalCount: 0,
  budgetUtilization: 0,
  totalBudget: 0,
  totalExpenses: 0,
  todayWorkHours: 0,
  weekWorkHours: 0,
  monthWorkHours: 0,
  monthLeaveCount: 0,
  monthReimbursementCount: 0,
  monthReimbursementAmount: 0,
  myTaskCount: 0,
  pendingTaskCount: 0
})

const trendPeriod = ref(30)
const worktimeTrends = ref([])
const projectStats = ref([])
const approvalStats = ref([])

// 获取基础统计数据
const fetchStats = async () => {
  try {
    const response = await api.get('/api/dashboard/stats')
    stats.value = response.data

    // 初始化图表
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
    // 使用模拟数据
    stats.value = {
      userCount: 156,
      activeUserCount: 89,
      projectCount: 23,
      activeProjectCount: 16,
      pendingApprovalCount: 12,
      todayApprovalCount: 5,
      budgetUtilization: 75.2,
      totalBudget: 1200000,
      totalExpenses: 902400,
      todayWorkHours: 7.5,
      weekWorkHours: 36.5,
      monthWorkHours: 152.5,
      monthLeaveCount: 8,
      monthReimbursementCount: 25,
      monthReimbursementAmount: 45600,
      myTaskCount: 12,
      pendingTaskCount: 5
    }
    await nextTick()
    initCharts()
  }
}

// 获取工时趋势数据
const fetchWorktimeTrends = async (days = 30) => {
  try {
    const response = await api.get(`/api/dashboard/worktime-trends?days=${days}`)
    worktimeTrends.value = response.data.dailyTrends
    updateWorktimeChart()
  } catch (error) {
    console.error('获取工时趋势数据失败:', error)
    // 使用模拟数据
    worktimeTrends.value = generateMockWorktimeTrends(days)
    updateWorktimeChart()
  }
}

// 获取项目统计数据
const fetchProjectStats = async () => {
  try {
    const response = await api.get('/api/dashboard/project-stats')
    projectStats.value = response.data.projectWorkHours
    updateProjectChart()
  } catch (error) {
    console.error('获取项目统计数据失败:', error)
    // 使用模拟数据
    projectStats.value = [
      { name: '电商平台开发', value: 120.5 },
      { name: '移动应用项目', value: 89.2 },
      { name: '数据分析系统', value: 76.8 },
      { name: '企业管理平台', value: 65.3 },
      { name: '其他项目', value: 48.7 }
    ]
    updateProjectChart()
  }
}

// 获取审批统计数据
const fetchApprovalStats = async () => {
  try {
    const response = await api.get('/api/dashboard/approval-stats')
    approvalStats.value = response.data.approvalTypeDistribution
    updateApprovalChart()
  } catch (error) {
    console.error('获取审批统计数据失败:', error)
    // 使用模拟数据
    approvalStats.value = [
      { name: '请假申请', value: 15 },
      { name: '报销申请', value: 23 },
      { name: '出差申请', value: 8 },
      { name: '工时审批', value: 45 },
      { name: '其他审批', value: 12 }
    ]
    updateApprovalChart()
  }
}

// 初始化所有图表
const initCharts = () => {
  fetchWorktimeTrends(trendPeriod.value)
  fetchProjectStats()
  fetchApprovalStats()
}

// 获取图表主题颜色
const getChartTheme = () => {
  const themeInfo = getThemeInfo()
  return {
    backgroundColor: themeInfo.isDark ? getComputedStyle(document.documentElement).getPropertyValue('--oa-chart-bg').trim() : '#FFFFFF',
    textColor: themeInfo.isDark ? getComputedStyle(document.documentElement).getPropertyValue('--oa-chart-text').trim() : '#303133',
    lineColor: themeInfo.isDark ? getComputedStyle(document.documentElement).getPropertyValue('--oa-chart-line').trim() : '#E4E7ED',
    gridColor: themeInfo.isDark ? getComputedStyle(document.documentElement).getPropertyValue('--oa-chart-grid').trim() : '#F5F5F5'
  }
}

// 更新工时趋势图表
const updateWorktimeChart = () => {
  const chartElement = document.getElementById('worktimeChart')
  if (!chartElement) return

  const chart = echarts.init(chartElement)
  const theme = getChartTheme()
  
  chart.setOption({
    backgroundColor: 'transparent',
    title: {
      text: '工时趋势',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
        color: theme.textColor
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>{a}: {c}小时',
      backgroundColor: theme.backgroundColor,
      textStyle: {
        color: theme.textColor
      },
      borderColor: theme.lineColor
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: worktimeTrends.value.map((item) => item.date),
      axisLine: {
        lineStyle: {
          color: theme.lineColor
        }
      },
      axisLabel: {
        color: theme.textColor
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: theme.lineColor
        }
      },
      axisLabel: {
        color: theme.textColor
      },
      splitLine: {
        lineStyle: {
          color: theme.gridColor
        }
      }
    },
    series: [
      {
        name: '工时',
        type: 'line',
        data: worktimeTrends.value.map((item) => item.hours),
        smooth: true,
        lineStyle: {
          color: '#409EFF'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
            ]
          }
        }
      }
    ]
  })
}

// 更新项目工时分布图表
const updateProjectChart = () => {
  const chartElement = document.getElementById('projectChart')
  if (!chartElement) return

  const chart = echarts.init(chartElement)
  const theme = getChartTheme()
  
  chart.setOption({
    backgroundColor: 'transparent',
    title: {
      text: '项目工时分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
        color: theme.textColor
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}小时 ({d}%)',
      backgroundColor: theme.backgroundColor,
      textStyle: {
        color: theme.textColor
      },
      borderColor: theme.lineColor
    },
    legend: {
      textStyle: {
        color: theme.textColor
      }
    },
    series: [
      {
        name: '项目工时',
        type: 'pie',
        radius: '60%',
        center: ['50%', '60%'],
        data: projectStats.value,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: theme.textColor === '#E8E8E8' ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          color: theme.textColor
        }
      }
    ]
  })
}

// 更新审批统计图表
const updateApprovalChart = () => {
  const chartElement = document.getElementById('approvalChart')
  if (!chartElement) return

  const chart = echarts.init(chartElement)
  const theme = getChartTheme()
  
  chart.setOption({
    backgroundColor: 'transparent',
    title: {
      text: '审批类型分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
        color: theme.textColor
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}件 ({d}%)',
      backgroundColor: theme.backgroundColor,
      textStyle: {
        color: theme.textColor
      },
      borderColor: theme.lineColor
    },
    legend: {
      textStyle: {
        color: theme.textColor
      }
    },
    series: [
      {
        name: '审批类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '60%'],
        data: approvalStats.value,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: theme.textColor === '#E8E8E8' ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          color: theme.textColor
        }
      }
    ]
  })
}

// 切换工时趋势时间段
const changeTrendPeriod = (days) => {
  trendPeriod.value = days
  fetchWorktimeTrends(days)
}

// 刷新项目统计
const refreshProjectStats = () => {
  fetchProjectStats()
}

// 工具函数
const formatDate = (date) => {
  if (!date) return '未知'
  return new Date(date).toLocaleString('zh-CN')
}

const formatCurrency = (amount) => {
  if (!amount) return '¥0'
  return `¥${amount.toLocaleString()}`
}

const getWorkTimeStatus = (hours) => {
  if (hours >= 8) return '正常'
  if (hours >= 6) return '偏少'
  return '不足'
}

const getMonthlyTarget = () => {
  const workDays = 22 // 每月工作日
  return workDays * 8
}

// 生成模拟工时趋势数据
const generateMockWorktimeTrends = (days) => {
  const trends = []
  const today = new Date()

  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)

    // 周末工时较少
    const isWeekend = date.getDay() === 0 || date.getDay() === 6
    const baseHours = isWeekend ? 0 : 8
    const randomVariation = (Math.random() - 0.5) * 2
    const hours = Math.max(0, baseHours + randomVariation)

    trends.push({
      date: `${date.getMonth() + 1}/${date.getDate()}`,
      hours: parseFloat(hours.toFixed(1))
    })
  }

  return trends
}

// 监听主题变化，重新渲染图表
watch(isDarkMode, () => {
  nextTick(() => {
    updateWorktimeChart()
    updateProjectChart()
    updateApprovalChart()
  })
}, { flush: 'post' })

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: var(--oa-bg-primary);
  min-height: 100vh;
  transition: background-color var(--oa-transition-base);
}

/* 头部样式 */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background: var(--oa-bg-card);
  border-radius: var(--oa-border-radius-md);
  box-shadow: var(--oa-shadow-light);
  transition: all var(--oa-transition-base);
}

.dashboard-header h1 {
  margin: 0;
  color: var(--oa-text-primary);
  font-size: var(--oa-font-size-2xl);
  transition: color var(--oa-transition-base);
}

.welcome-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.welcome-info span {
  color: var(--oa-text-regular);
  font-size: var(--oa-font-size-base);
  transition: color var(--oa-transition-base);
}

.last-login {
  color: var(--oa-text-secondary) !important;
  font-size: var(--oa-font-size-sm) !important;
}

/* 统计卡片样式 */
.stat-cards {
  margin-bottom: 30px;
}

.stat-card {
  border-radius: var(--oa-border-radius-md);
  transition: all var(--oa-transition-fast);
  height: auto;
  min-height: 120px;
  overflow: hidden;
  background: var(--oa-bg-card);
  border: 1px solid var(--oa-border-light);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--oa-shadow-base);
}

/* 重置Element卡片内部结构 - 图标和内容在一行 */
.stat-card .el-card__body {
  padding: 20px !important;
  display: flex !important;
  align-items: center !important;
  height: 100%;
  gap: 16px !important;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--oa-border-radius-round);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  flex-shrink: 0;
  box-shadow: var(--oa-shadow-light);
}

.user-stat .stat-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.project-stat .stat-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.approval-stat .stat-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.budget-stat .stat-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.task-stat .stat-icon {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
}

.stat-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.stat-title {
  font-size: var(--oa-font-size-sm);
  color: var(--oa-text-secondary);
  margin: 0;
  font-weight: var(--oa-font-weight-normal);
  line-height: var(--oa-line-height-sm);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.stat-value {
  font-size: 32px;
  font-weight: var(--oa-font-weight-bold);
  color: var(--oa-text-primary);
  margin: 0;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.stat-trend {
  font-size: var(--oa-font-size-xs);
  color: var(--oa-success-color);
  margin: 0;
  line-height: var(--oa-line-height-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

/* 工时统计区域 */
.worktime-section {
  margin-bottom: 30px;
}

.worktime-card {
  border-radius: var(--oa-border-radius-md);
  border: 1px solid var(--oa-border-light);
  background: var(--oa-bg-card);
  transition: all var(--oa-transition-fast);
}

.worktime-card:hover {
  box-shadow: var(--oa-shadow-base);
}

.worktime-card .el-card__header {
  padding: 16px 20px !important;
  border-bottom: 1px solid var(--oa-border-light);
  background: var(--oa-bg-secondary);
}

.worktime-card .el-card__body {
  padding: 20px !important;
}

.worktime-display {
  text-align: center;
}

.worktime-value {
  font-size: 36px;
  font-weight: var(--oa-font-weight-bold);
  color: var(--oa-primary-color);
  margin-bottom: 8px;
  line-height: 1.2;
}

.worktime-status {
  font-size: var(--oa-font-size-base);
  color: var(--oa-success-color);
  font-weight: var(--oa-font-weight-medium);
}

.worktime-average,
.worktime-target {
  font-size: var(--oa-font-size-xs);
  color: var(--oa-text-secondary);
  margin-top: 4px;
}

/* 图表区域 */
.charts-section {
  margin-bottom: 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: var(--oa-font-weight-medium);
  color: var(--oa-text-primary);
}

/* 图表卡片样式增强 */
.charts-section .el-card,
.bottom-section .el-card {
  border-radius: var(--oa-border-radius-md);
  border: 1px solid var(--oa-border-light);
  background: var(--oa-bg-card);
  transition: all var(--oa-transition-fast);
}

.charts-section .el-card:hover,
.bottom-section .el-card:hover {
  box-shadow: var(--oa-shadow-base);
}

.charts-section .el-card__header,
.bottom-section .el-card__header {
  padding: 16px 20px !important;
  border-bottom: 1px solid var(--oa-border-light);
  background: var(--oa-bg-secondary);
}

/* 底部区域 */
.bottom-section {
  margin-bottom: 30px;
}

.financial-overview {
  padding: 20px;
}

.financial-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.financial-item:last-child {
  border-bottom: none;
}

.financial-label {
  font-size: 14px;
  color: #606266;
}

.financial-value {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.financial-value.expense {
  color: #f56c6c;
}

.financial-value.remaining {
  color: #67c23a;
}

/* 快速操作区域 */
.quick-actions {
  margin-bottom: 25px;
  padding: 15px 20px;
  background: var(--oa-bg-card);
  border-radius: var(--oa-border-radius-md);
  box-shadow: var(--oa-shadow-light);
  transition: all var(--oa-transition-base);
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
  align-items: center;
}

.action-item {
  text-decoration: none;
}

.action-item a {
  text-decoration: none;
}

.action-buttons .el-button {
  min-width: 100px;
  padding: 12px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.action-buttons .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-container {
    padding: 12px;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 16px;
  }

  .welcome-info {
    align-items: flex-start;
  }

  .stat-card .el-card__body {
    flex-direction: column !important;
    justify-content: center !important;
    text-align: center;
    gap: 16px !important;
    padding: 20px !important;
  }

  .stat-content {
    gap: 6px;
  }

  .stat-content .stat-title,
  .stat-content .stat-value,
  .stat-content .stat-trend {
    text-align: center !important;
  }

  .stat-value {
    font-size: 28px;
  }

  .action-buttons {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
