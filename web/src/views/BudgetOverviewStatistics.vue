<template>
  <div class="budget-overview-statistics">
    <h2>预算与报销统计总览</h2>
    
    <!-- 时间范围筛选 -->
    <div class="time-filter-bar">
      <el-radio-group v-model="timeRangeType" @change="handleTimeRangeChange">
        <el-radio-button label="month">本月</el-radio-button>
        <el-radio-button label="quarter">本季度</el-radio-button>
        <el-radio-button label="year">本年度</el-radio-button>
        <el-radio-button label="custom">自定义</el-radio-button>
      </el-radio-group>
      
      <el-date-picker
        v-if="timeRangeType === 'custom'"
        v-model="customDateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="handleCustomDateChange"
        style="margin-left: 20px"
      />
      
      <el-button 
        type="primary" 
        :icon="Refresh" 
        @click="refreshAllData"
        style="margin-left: 20px"
      >
        刷新数据
      </el-button>
      
      <el-dropdown @command="handleExportCommand" style="margin-left: 10px">
        <el-button type="success" :icon="Download">
          导出数据<el-icon class="el-icon--right"><arrow-down /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="overview">导出总览数据</el-dropdown-item>
            <el-dropdown-item command="project">导出项目统计</el-dropdown-item>
            <el-dropdown-item command="category">导出类别统计</el-dropdown-item>
            <el-dropdown-item command="reimbursement">导出报销统计</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- Tab页布局 -->
    <el-tabs v-model="activeTab" class="statistics-tabs">
      <!-- 总览Tab -->
      <el-tab-pane label="总览" name="overview">
        <div class="overview-content" v-loading="overviewLoading">
          <!-- 关键指标卡片 -->
          <el-row :gutter="20" class="metrics-row">
            <el-col :span="6">
              <el-card class="metric-card">
                <div class="metric-icon" style="background-color: #409eff20;">
                  <el-icon :size="32" color="#409eff"><Money /></el-icon>
                </div>
                <div class="metric-content">
                  <div class="metric-label">预算总额</div>
                  <div class="metric-value">{{ formatCurrency(overviewData.totalBudgetAmount) }}</div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="metric-card">
                <div class="metric-icon" style="background-color: #67c23a20;">
                  <el-icon :size="32" color="#67c23a"><Wallet /></el-icon>
                </div>
                <div class="metric-content">
                  <div class="metric-label">已使用金额</div>
                  <div class="metric-value">{{ formatCurrency(overviewData.totalUsedAmount) }}</div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="metric-card">
                <div class="metric-icon" style="background-color: #e6a23c20;">
                  <el-icon :size="32" color="#e6a23c"><CreditCard /></el-icon>
                </div>
                <div class="metric-content">
                  <div class="metric-label">剩余金额</div>
                  <div class="metric-value">{{ formatCurrency(overviewData.totalRemainingAmount) }}</div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="metric-card">
                <div class="metric-icon" style="background-color: #f56c6c20;">
                  <el-icon :size="32" color="#f56c6c"><TrendCharts /></el-icon>
                </div>
                <div class="metric-content">
                  <div class="metric-label">整体使用率</div>
                  <div class="metric-value">{{ overviewData.overallUsageRate?.toFixed(2) || 0 }}%</div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 预算状态统计 -->
          <el-row :gutter="20" class="status-row">
            <el-col :span="8">
              <el-card class="status-card">
                <div class="status-item">
                  <el-icon :size="24" color="#67c23a"><CircleCheck /></el-icon>
                  <div class="status-content">
                    <div class="status-label">活跃预算</div>
                    <div class="status-count">{{ overviewData.activeBudgetCount || 0 }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="8">
              <el-card class="status-card">
                <div class="status-item">
                  <el-icon :size="24" color="#e6a23c"><Warning /></el-icon>
                  <div class="status-content">
                    <div class="status-label">预警预算</div>
                    <div class="status-count">{{ overviewData.warningBudgetCount || 0 }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="8">
              <el-card class="status-card">
                <div class="status-item">
                  <el-icon :size="24" color="#909399"><Lock /></el-icon>
                  <div class="status-content">
                    <div class="status-label">已关闭预算</div>
                    <div class="status-count">{{ overviewData.closedBudgetCount || 0 }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 报销与执行率 -->
          <el-row :gutter="20" class="execution-row">
            <el-col :span="12">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <span>报销总金额</span>
                  </div>
                </template>
                <div class="execution-value">{{ formatCurrency(overviewData.totalReimbursementAmount) }}</div>
              </el-card>
            </el-col>
            
            <el-col :span="12">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <span>预算执行率</span>
                  </div>
                </template>
                <div class="execution-value">
                  <el-progress 
                    :percentage="overviewData.budgetExecutionRate || 0" 
                    :color="getExecutionRateColor(overviewData.budgetExecutionRate)"
                    :stroke-width="20"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 项目统计Tab -->
      <el-tab-pane label="项目统计" name="project">
        <div class="project-statistics-content" v-loading="projectLoading">
          <!-- 项目预算分布柱状图 -->
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>项目预算分布</span>
                <el-button type="primary" size="small" @click="exportChart('project')">导出</el-button>
              </div>
            </template>
            <div ref="projectBudgetChartRef" class="chart-container"></div>
          </el-card>

          <!-- 费用类别占比饼图 -->
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>费用类别占比</span>
                <el-button type="primary" size="small" @click="exportChart('category')">导出</el-button>
              </div>
            </template>
            <div ref="categoryPieChartRef" class="chart-container"></div>
          </el-card>

          <!-- 预算使用趋势折线图 -->
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>预算使用趋势</span>
                <el-button type="primary" size="small" @click="exportChart('trend')">导出</el-button>
              </div>
            </template>
            <div ref="trendLineChartRef" class="chart-container"></div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 报销统计Tab -->
      <el-tab-pane label="报销统计" name="reimbursement">
        <div class="reimbursement-statistics-content" v-loading="reimbursementLoading">
          <!-- 报销汇总卡片 -->
          <el-row :gutter="20" class="reimbursement-summary-row">
            <el-col :span="6">
              <el-card class="summary-card">
                <div class="summary-item">
                  <div class="summary-label">报销总金额</div>
                  <div class="summary-value">{{ formatCurrency(reimbursementData.totalAmount) }}</div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="summary-card">
                <div class="summary-item">
                  <div class="summary-label">报销申请总数</div>
                  <div class="summary-value">{{ reimbursementData.totalCount || 0 }}</div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="summary-card">
                <div class="summary-item">
                  <div class="summary-label">审批通过率</div>
                  <div class="summary-value">{{ (reimbursementData.approvalRate || 0).toFixed(2) }}%</div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="summary-card">
                <div class="summary-item">
                  <div class="summary-label">平均审批时长</div>
                  <div class="summary-value">{{ (reimbursementData.avgApprovalDays || 0).toFixed(1) }}天</div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 状态分布图表 -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">
                    <span>报销状态分布</span>
                  </div>
                </template>
                <div ref="statusDistributionChartRef" class="chart-container"></div>
              </el-card>
            </el-col>
            
            <el-col :span="12">
              <el-card class="chart-card">
                <template #header>
                  <div class="card-header">
                    <span>费用类别分布</span>
                  </div>
                </template>
                <div ref="reimbursementCategoryChartRef" class="chart-container"></div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 项目排名列表 -->
          <el-card class="ranking-card">
            <template #header>
              <div class="card-header">
                <span>项目报销金额排名</span>
              </div>
            </template>
            <el-table :data="reimbursementData.projectRanking || []" style="width: 100%">
              <el-table-column prop="rank" label="排名" width="80" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.rank === 1" type="danger" effect="dark">{{ scope.row.rank }}</el-tag>
                  <el-tag v-else-if="scope.row.rank === 2" type="warning" effect="dark">{{ scope.row.rank }}</el-tag>
                  <el-tag v-else-if="scope.row.rank === 3" type="success" effect="dark">{{ scope.row.rank }}</el-tag>
                  <span v-else>{{ scope.row.rank }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="projectName" label="项目名称" />
              <el-table-column prop="reimbursementAmount" label="报销金额" align="right">
                <template #default="scope">
                  {{ formatCurrency(scope.row.reimbursementAmount) }}
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 预算监控Tab -->
      <el-tab-pane label="预算监控" name="monitoring">
        <div class="monitoring-content" v-loading="monitoringLoading">
          <!-- 预警级别筛选 -->
          <div class="monitoring-filter-bar">
            <el-radio-group v-model="alertLevelFilter" @change="handleAlertLevelChange">
              <el-radio-button label="ALL">全部</el-radio-button>
              <el-radio-button label="NORMAL">正常</el-radio-button>
              <el-radio-button label="WARNING">预警</el-radio-button>
              <el-radio-button label="DANGER">危险</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 预算执行进度表格 -->
          <el-table 
            :data="monitoringData" 
            style="width: 100%; margin-top: 20px"
            :default-sort="{ prop: 'usageRate', order: 'descending' }"
          >
            <el-table-column prop="budgetName" label="预算名称" width="200" />
            <el-table-column prop="projectName" label="所属项目" width="180" />
            <el-table-column prop="budgetType" label="预算类型" width="150" />
            <el-table-column prop="totalAmount" label="总预算" width="120" align="right">
              <template #default="scope">
                {{ formatCurrency(scope.row.totalAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="usedAmount" label="已使用" width="120" align="right">
              <template #default="scope">
                {{ formatCurrency(scope.row.usedAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="remainingAmount" label="剩余金额" width="120" align="right">
              <template #default="scope">
                {{ formatCurrency(scope.row.remainingAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="usageRate" label="使用率" width="180" sortable>
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.usageRate" 
                  :status="getProgressStatus(scope.row.usageRate)"
                  :stroke-width="16"
                />
              </template>
            </el-table-column>
            <el-table-column prop="alertLevel" label="预警级别" width="120" align="center">
              <template #default="scope">
                <el-tag :type="getAlertLevelType(scope.row.alertLevel)" effect="dark">
                  {{ getAlertLevelText(scope.row.alertLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="预算周期" width="200">
              <template #default="scope">
                {{ formatDate(scope.row.startDate) }} ~ {{ formatDate(scope.row.endDate) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Refresh, Money, Wallet, CreditCard, TrendCharts,
  CircleCheck, Warning, Lock, Download, ArrowDown
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../utils/axios.js'

// Tab状态
const activeTab = ref('overview')

// 时间范围筛选
const timeRangeType = ref('year') // month, quarter, year, custom
const customDateRange = ref(null)
const currentDateRange = reactive({
  startDate: null,
  endDate: null
})

// 总览数据
const overviewLoading = ref(false)
const overviewData = reactive({
  totalBudgetAmount: 0,
  totalUsedAmount: 0,
  totalRemainingAmount: 0,
  overallUsageRate: 0,
  activeBudgetCount: 0,
  warningBudgetCount: 0,
  closedBudgetCount: 0,
  totalReimbursementAmount: 0,
  budgetExecutionRate: 0
})

// 项目统计数据
const projectLoading = ref(false)
const projectStatData = ref([])
const categoryStatData = ref([])
const timeSeriesData = ref([])

// 图表引用
const projectBudgetChartRef = ref(null)
const categoryPieChartRef = ref(null)
const trendLineChartRef = ref(null)
const statusDistributionChartRef = ref(null)
const reimbursementCategoryChartRef = ref(null)

// 图表实例
let projectBudgetChart = null
let categoryPieChart = null
let trendLineChart = null
let statusDistributionChart = null
let reimbursementCategoryChart = null

// 报销统计数据
const reimbursementLoading = ref(false)
const reimbursementData = reactive({
  totalAmount: 0,
  totalCount: 0,
  approvedCount: 0,
  pendingCount: 0,
  rejectedCount: 0,
  draftCount: 0,
  approvalRate: 0,
  avgApprovalDays: 0,
  statusDistribution: [],
  categoryDistribution: [],
  projectRanking: []
})

// 预算监控数据
const monitoringLoading = ref(false)
const monitoringData = ref([])
const alertLevelFilter = ref('ALL')

// 计算预设时间范围
const calculatePresetDateRange = (type) => {
  const now = new Date()
  let startDate, endDate

  switch (type) {
    case 'month':
      // 本月第一天到今天
      startDate = new Date(now.getFullYear(), now.getMonth(), 1)
      endDate = now
      break
    case 'quarter':
      // 本季度第一天到今天
      const quarter = Math.floor(now.getMonth() / 3)
      startDate = new Date(now.getFullYear(), quarter * 3, 1)
      endDate = now
      break
    case 'year':
      // 本年度第一天到今天
      startDate = new Date(now.getFullYear(), 0, 1)
      endDate = now
      break
    default:
      return { startDate: null, endDate: null }
  }

  return {
    startDate: startDate.toISOString().split('T')[0],
    endDate: endDate.toISOString().split('T')[0]
  }
}

// 处理时间范围变更
const handleTimeRangeChange = (type) => {
  if (type !== 'custom') {
    const range = calculatePresetDateRange(type)
    currentDateRange.startDate = range.startDate
    currentDateRange.endDate = range.endDate
    refreshAllData()
  }
}

// 处理自定义日期变更
const handleCustomDateChange = (dates) => {
  if (dates && dates.length === 2) {
    currentDateRange.startDate = dates[0]
    currentDateRange.endDate = dates[1]
    refreshAllData()
  }
}

// 获取总览数据
const fetchOverviewData = async () => {
  overviewLoading.value = true
  try {
    const params = {
      startDate: currentDateRange.startDate,
      endDate: currentDateRange.endDate
    }
    const response = await api.get('/api/budgets/statistics/overview', { params })
    Object.assign(overviewData, response.data)
  } catch (error) {
    console.error('获取总览数据失败:', error)
    ElMessage.error('获取总览数据失败: ' + (error.response?.data?.message || error.message))
  } finally {
    overviewLoading.value = false
  }
}

// 获取项目统计数据
const fetchProjectStatistics = async () => {
  projectLoading.value = true
  try {
    const params = {
      startDate: currentDateRange.startDate,
      endDate: currentDateRange.endDate
    }
    
    // 获取项目统计
    const projectResponse = await api.get('/api/budgets/statistics/by-project', { params })
    projectStatData.value = projectResponse.data || []
    
    // 获取类别统计
    const categoryResponse = await api.get('/api/budgets/statistics/by-category', { params })
    categoryStatData.value = categoryResponse.data || []
    
    // 获取时间序列统计
    const timeParams = { ...params, granularity: 'MONTH' }
    const timeResponse = await api.get('/api/budgets/statistics/by-time', { params: timeParams })
    timeSeriesData.value = timeResponse.data || []
    
    // 渲染图表
    await nextTick()
    renderProjectBudgetChart()
    renderCategoryPieChart()
    renderTrendLineChart()
  } catch (error) {
    console.error('获取项目统计数据失败:', error)
    ElMessage.error('获取项目统计数据失败: ' + (error.response?.data?.message || error.message))
  } finally {
    projectLoading.value = false
  }
}

// 渲染项目预算分布柱状图
const renderProjectBudgetChart = () => {
  if (!projectBudgetChartRef.value) return
  
  if (!projectBudgetChart) {
    projectBudgetChart = echarts.init(projectBudgetChartRef.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach(item => {
          result += `${item.marker} ${item.seriesName}: ${formatCurrency(item.value)}<br/>`
        })
        return result
      }
    },
    legend: {
      data: ['总预算', '已使用', '剩余金额']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: projectStatData.value.map(item => item.projectName),
      axisLabel: {
        rotate: 45,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value) => {
          return value >= 10000 ? (value / 10000).toFixed(1) + '万' : value
        }
      }
    },
    series: [
      {
        name: '总预算',
        type: 'bar',
        data: projectStatData.value.map(item => item.totalBudget),
        itemStyle: { color: '#409eff' }
      },
      {
        name: '已使用',
        type: 'bar',
        data: projectStatData.value.map(item => item.usedAmount),
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '剩余金额',
        type: 'bar',
        data: projectStatData.value.map(item => item.remainingAmount),
        itemStyle: { color: '#e6a23c' }
      }
    ]
  }
  
  projectBudgetChart.setOption(option)
}

// 渲染费用类别占比饼图
const renderCategoryPieChart = () => {
  if (!categoryPieChartRef.value) return
  
  if (!categoryPieChart) {
    categoryPieChart = echarts.init(categoryPieChartRef.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.marker} ${params.name}<br/>金额: ${formatCurrency(params.value)}<br/>占比: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        name: '费用类别',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: categoryStatData.value.map(item => ({
          name: item.category,
          value: item.totalAmount
        }))
      }
    ]
  }
  
  categoryPieChart.setOption(option)
}

// 渲染预算使用趋势折线图
const renderTrendLineChart = () => {
  if (!trendLineChartRef.value) return
  
  if (!trendLineChart) {
    trendLineChart = echarts.init(trendLineChartRef.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        let result = params[0].axisValue + '<br/>'
        params.forEach(item => {
          result += `${item.marker} ${item.seriesName}: ${formatCurrency(item.value)}<br/>`
        })
        return result
      }
    },
    legend: {
      data: ['预算金额', '已使用金额']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeSeriesData.value.map(item => item.period)
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value) => {
          return value >= 10000 ? (value / 10000).toFixed(1) + '万' : value
        }
      }
    },
    series: [
      {
        name: '预算金额',
        type: 'line',
        data: timeSeriesData.value.map(item => item.budgetAmount),
        smooth: true,
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ])
        }
      },
      {
        name: '已使用金额',
        type: 'line',
        data: timeSeriesData.value.map(item => item.usedAmount),
        smooth: true,
        itemStyle: { color: '#67c23a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
          ])
        }
      }
    ]
  }
  
  trendLineChart.setOption(option)
}

// 导出图表
const exportChart = (type) => {
  let chart = null
  let filename = ''
  
  switch (type) {
    case 'project':
      chart = projectBudgetChart
      filename = '项目预算分布'
      break
    case 'category':
      chart = categoryPieChart
      filename = '费用类别占比'
      break
    case 'trend':
      chart = trendLineChart
      filename = '预算使用趋势'
      break
  }
  
  if (chart) {
    const url = chart.getDataURL({
      type: 'png',
      pixelRatio: 2,
      backgroundColor: '#fff'
    })
    
    const link = document.createElement('a')
    link.href = url
    link.download = `${filename}.png`
    link.click()
    
    ElMessage.success('图表导出成功')
  }
}

// 刷新所有数据
const refreshAllData = () => {
  if (activeTab.value === 'overview') {
    fetchOverviewData()
  } else if (activeTab.value === 'project') {
    fetchProjectStatistics()
  } else if (activeTab.value === 'reimbursement') {
    fetchReimbursementStatistics()
  } else if (activeTab.value === 'monitoring') {
    fetchMonitoringData()
  }
}

// 获取报销统计数据
const fetchReimbursementStatistics = async () => {
  reimbursementLoading.value = true
  try {
    const params = {
      startDate: currentDateRange.startDate,
      endDate: currentDateRange.endDate
    }
    
    const response = await api.get('/api/budgets/statistics/reimbursement', { params })
    Object.assign(reimbursementData, response.data)
    
    // 渲染图表
    await nextTick()
    renderStatusDistributionChart()
    renderReimbursementCategoryChart()
  } catch (error) {
    console.error('获取报销统计数据失败:', error)
    ElMessage.error('获取报销统计数据失败: ' + (error.response?.data?.message || error.message))
  } finally {
    reimbursementLoading.value = false
  }
}

// 渲染报销状态分布图表
const renderStatusDistributionChart = () => {
  if (!statusDistributionChartRef.value) return
  
  if (!statusDistributionChart) {
    statusDistributionChart = echarts.init(statusDistributionChartRef.value)
  }
  
  const statusColors = {
    '草稿': '#909399',
    '待审批': '#409eff',
    '已通过': '#67c23a',
    '已驳回': '#f56c6c'
  }
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.marker} ${params.name}<br/>数量: ${params.data.count}<br/>金额: ${formatCurrency(params.data.amount)}<br/>占比: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center'
    },
    series: [
      {
        name: '报销状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {c}笔'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: (reimbursementData.statusDistribution || []).map(item => ({
          name: item.statusLabel,
          value: item.count,
          count: item.count,
          amount: item.amount,
          itemStyle: {
            color: statusColors[item.statusLabel] || '#409eff'
          }
        }))
      }
    ]
  }
  
  statusDistributionChart.setOption(option)
}

// 渲染报销费用类别分布图表
const renderReimbursementCategoryChart = () => {
  if (!reimbursementCategoryChartRef.value) return
  
  if (!reimbursementCategoryChart) {
    reimbursementCategoryChart = echarts.init(reimbursementCategoryChartRef.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.marker} ${params.name}<br/>金额: ${formatCurrency(params.value)}<br/>占比: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      type: 'scroll'
    },
    series: [
      {
        name: '费用类别',
        type: 'pie',
        radius: '60%',
        center: ['40%', '50%'],
        data: (reimbursementData.categoryDistribution || []).map(item => ({
          name: item.category,
          value: item.amount
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  reimbursementCategoryChart.setOption(option)
}

// 获取预算监控数据
const fetchMonitoringData = async () => {
  monitoringLoading.value = true
  try {
    const params = {
      alertLevel: alertLevelFilter.value
    }
    
    const response = await api.get('/api/budgets/statistics/monitoring', { params })
    monitoringData.value = response.data || []
  } catch (error) {
    console.error('获取预算监控数据失败:', error)
    ElMessage.error('获取预算监控数据失败: ' + (error.response?.data?.message || error.message))
  } finally {
    monitoringLoading.value = false
  }
}

// 处理预警级别筛选变更
const handleAlertLevelChange = () => {
  fetchMonitoringData()
}

// 获取进度条状态
const getProgressStatus = (rate) => {
  if (rate >= 95) return 'exception'
  if (rate >= 80) return 'warning'
  return 'success'
}

// 获取预警级别标签类型
const getAlertLevelType = (level) => {
  const typeMap = {
    'NORMAL': 'success',
    'WARNING': 'warning',
    'DANGER': 'danger'
  }
  return typeMap[level] || 'info'
}

// 获取预警级别文本
const getAlertLevelText = (level) => {
  const textMap = {
    'NORMAL': '正常',
    'WARNING': '预警',
    'DANGER': '危险'
  }
  return textMap[level] || level
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

// 处理导出命令
const handleExportCommand = async (command) => {
  try {
    const params = {
      exportType: command.toUpperCase(),
      startDate: currentDateRange.startDate,
      endDate: currentDateRange.endDate
    }
    
    const response = await api.get('/api/budgets/statistics/export', {
      params,
      responseType: 'blob'
    })
    
    // 创建下载链接
    const blob = new Blob([response.data], { 
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 设置文件名
    const fileNameMap = {
      'overview': '预算总览统计',
      'project': '项目预算统计',
      'category': '类别预算统计',
      'reimbursement': '报销统计'
    }
    const fileName = `${fileNameMap[command]}_${new Date().toISOString().split('T')[0]}.xlsx`
    link.download = fileName
    
    // 触发下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('数据导出成功')
  } catch (error) {
    console.error('导出数据失败:', error)
    ElMessage.error('导出数据失败: ' + (error.response?.data?.message || error.message))
  }
}

// 监听Tab切换
watch(activeTab, (newTab) => {
  if (newTab === 'project' && projectStatData.value.length === 0) {
    fetchProjectStatistics()
  } else if (newTab === 'reimbursement' && reimbursementData.totalCount === 0) {
    fetchReimbursementStatistics()
  } else if (newTab === 'monitoring' && monitoringData.value.length === 0) {
    fetchMonitoringData()
  }
})

// 格式化货币
const formatCurrency = (value) => {
  return new Intl.NumberFormat('zh-CN', { 
    style: 'currency', 
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value || 0)
}

// 获取执行率颜色
const getExecutionRateColor = (rate) => {
  if (rate >= 95) return '#f56c6c'
  if (rate >= 80) return '#e6a23c'
  return '#67c23a'
}

// 初始化
onMounted(() => {
  // 默认加载本年度数据
  const range = calculatePresetDateRange('year')
  currentDateRange.startDate = range.startDate
  currentDateRange.endDate = range.endDate
  
  // 加载总览数据
  fetchOverviewData()
})
</script>

<style scoped>
.budget-overview-statistics {
  padding: 20px;
}

.budget-overview-statistics h2 {
  margin-bottom: 20px;
  color: #303133;
}

.time-filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.statistics-tabs {
  margin-top: 20px;
}

.overview-content,
.project-statistics-content,
.reimbursement-statistics-content,
.monitoring-content {
  min-height: 400px;
  padding: 20px 0;
}

/* 总览样式 */
.metrics-row {
  margin-bottom: 20px;
}

.metric-card {
  cursor: default;
}

.metric-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 20px;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.metric-content {
  flex: 1;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.status-row {
  margin-bottom: 20px;
}

.status-card :deep(.el-card__body) {
  padding: 20px;
}

.status-item {
  display: flex;
  align-items: center;
}

.status-content {
  margin-left: 15px;
  flex: 1;
}

.status-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.status-count {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.execution-row {
  margin-bottom: 20px;
}

.card-header {
  font-weight: 600;
  color: #303133;
}

.execution-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
  padding: 20px 0;
}

/* 图表样式 */
.chart-card {
  margin-bottom: 20px;
}

.chart-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 400px;
}

/* 报销统计样式 */
.reimbursement-summary-row {
  margin-bottom: 20px;
}

.summary-card :deep(.el-card__body) {
  padding: 20px;
}

.summary-item {
  text-align: center;
}

.summary-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.summary-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.ranking-card {
  margin-top: 20px;
}

/* 预算监控样式 */
.monitoring-filter-bar {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
