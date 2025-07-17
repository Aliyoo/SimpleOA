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
              <div class="stat-trend">进行中: {{ stats.activeProjectCount }}</div>
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
          <el-card shadow="hover" class="stat-card budget-stat">
            <div class="stat-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">预算使用率</div>
              <div class="stat-value">{{ stats.budgetUtilization }}%</div>
              <div class="stat-trend">总预算: {{ formatCurrency(stats.totalBudget) }}</div>
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
            <div id="worktimeChart" style="width: 100%; height: 300px;"></div>
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
            <div id="projectChart" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <!-- 审批流程和财务概览 -->
    <div class="bottom-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>审批流程统计</span>
                <router-link to="/approval-management">
                  <el-button type="text" size="small">查看详情</el-button>
                </router-link>
              </div>
            </template>
            <div id="approvalChart" style="width: 100%; height: 250px;"></div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
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
                <span class="financial-value remaining">{{ formatCurrency(stats.totalBudget - stats.totalExpenses) }}</span>
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
    
    <!-- 快速操作区域 -->
    <div class="quick-actions">
      <el-card shadow="hover">
        <template #header>
          <span>快速操作</span>
        </template>
        <div class="action-buttons">
          <router-link to="/time-management">
            <el-button type="primary" icon="Clock">填报工时</el-button>
          </router-link>
          <router-link to="/leave-application">
            <el-button type="success" icon="Calendar">申请请假</el-button>
          </router-link>
          <router-link to="/reimbursement">
            <el-button type="warning" icon="Money">申请报销</el-button>
          </router-link>
          <router-link to="/project-management">
            <el-button type="info" icon="FolderOpened">项目管理</el-button>
          </router-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useUserStore } from '../stores/user'
import api from '../utils/axios.js'
import * as echarts from 'echarts'
import { 
  User, 
  FolderOpened, 
  DocumentChecked, 
  Money, 
  Clock, 
  Calendar 
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const currentUser = ref(userStore.userInfo || {})

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
  monthReimbursementAmount: 0
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
      monthReimbursementAmount: 45600
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

// 更新工时趋势图表
const updateWorktimeChart = () => {
  const chartElement = document.getElementById('worktimeChart')
  if (!chartElement) return
  
  const chart = echarts.init(chartElement)
  chart.setOption({
    title: {
      text: '工时趋势',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>{a}: {c}小时'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: worktimeTrends.value.map(item => item.date),
      axisLine: {
        lineStyle: {
          color: '#E4E7ED'
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: '#E4E7ED'
        }
      }
    },
    series: [
      {
        name: '工时',
        type: 'line',
        data: worktimeTrends.value.map(item => item.hours),
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
  chart.setOption({
    title: {
      text: '项目工时分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}小时 ({d}%)'
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
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
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
  chart.setOption({
    title: {
      text: '审批类型分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}件 ({d}%)'
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
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
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

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f5f5f5;
}

/* 头部样式 */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-header h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.welcome-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.welcome-info span {
  color: #606266;
  font-size: 14px;
}

.last-login {
  color: #909399 !important;
  font-size: 12px !important;
}

/* 统计卡片样式 */
.stat-cards {
  margin-bottom: 30px;
}

.stat-card {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  border-radius: 8px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
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

.stat-content {
  flex: 1;
  text-align: left;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-trend {
  font-size: 12px;
  color: #67C23A;
}

/* 工时统计区域 */
.worktime-section {
  margin-bottom: 30px;
}

.worktime-card {
  border-radius: 8px;
}

.worktime-display {
  text-align: center;
  padding: 20px;
}

.worktime-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.worktime-status {
  font-size: 14px;
  color: #67C23A;
}

.worktime-average, .worktime-target {
  font-size: 12px;
  color: #909399;
}

/* 图表区域 */
.charts-section {
  margin-bottom: 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
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
  border-bottom: 1px solid #EBEEF5;
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
  color: #F56C6C;
}

.financial-value.remaining {
  color: #67C23A;
}

/* 快速操作区域 */
.quick-actions {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  padding: 10px 0;
}

.action-buttons a {
  text-decoration: none;
}

.action-buttons .el-button {
  padding: 12px 20px;
  border-radius: 6px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .welcome-info {
    align-items: flex-start;
  }
  
  .stat-card {
    flex-direction: column;
    text-align: center;
  }
  
  .stat-content {
    text-align: center;
  }
  
  .action-buttons {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
