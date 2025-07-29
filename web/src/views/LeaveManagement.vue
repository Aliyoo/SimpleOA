<template>
  <div class="leave-management-container">
    <h1>请假管理</h1>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="请假申请" name="apply">
        <el-form ref="leaveFormRef" :model="leaveForm" :rules="leaveFormRules" label-width="100px" class="apply-form">
          <el-form-item label="请假类型" prop="leaveType">
            <el-select v-model="leaveForm.leaveType" placeholder="请选择请假类型" @change="onLeaveTypeChange">
              <el-option
                v-for="type in leaveTypes"
                :key="type.value"
                :label="`${type.label} (剩余: ${getLeaveBalance(type.value)}天)`"
                :value="type.value"
              />
            </el-select>
          </el-form-item>

          <!-- 显示当前选择类型的余额信息 -->
          <el-form-item v-if="leaveForm.leaveType && currentBalance" label="余额信息">
            <el-tag type="info">
              {{ getCurrentLeaveTypeLabel() }}: 总共{{ currentBalance.totalDays }}天，已用{{
                currentBalance.usedDays
              }}天，剩余{{ currentBalance.remainingDays }}天
            </el-tag>
          </el-form-item>

          <el-form-item label="开始时间" prop="startDate">
            <el-date-picker
              v-model="leaveForm.startDate"
              type="datetime"
              placeholder="选择开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
          </el-form-item>

          <el-form-item label="结束时间" prop="endDate">
            <el-date-picker
              v-model="leaveForm.endDate"
              type="datetime"
              placeholder="选择结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
          </el-form-item>

          <el-form-item label="请假原因" prop="reason">
            <el-input v-model="leaveForm.reason" type="textarea" :rows="4" placeholder="请输入请假原因" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitLeave">提交申请</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="请假列表" name="myApplications">
        <el-table :data="myApplicationsList" style="width: 100%">
          <el-table-column prop="leaveType" label="请假类型" width="120">
            <template #default="scope">
              {{ getLeaveTypeLabel(scope.row.leaveType) }}
            </template>
          </el-table-column>
          <el-table-column prop="startDate" label="开始时间" width="180" />
          <el-table-column prop="endDate" label="结束时间" width="180" />
          <el-table-column prop="reason" label="原因" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applyDate" label="申请时间" width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="请假余额" name="balance">
        <div class="balance-container">
          <h3>{{ new Date().getFullYear() }}年请假余额</h3>
          <el-row :gutter="20">
            <el-col v-for="balance in leaveBalances" :key="balance.leaveType" :span="6">
              <el-card class="balance-card">
                <template #header>
                  <span>{{ getLeaveTypeLabel(balance.leaveType) }}</span>
                </template>
                <div class="balance-info">
                  <div class="balance-item">
                    <span class="label">总额:</span>
                    <span class="value">{{ balance.totalDays }}天</span>
                  </div>
                  <div class="balance-item">
                    <span class="label">已用:</span>
                    <span class="value used">{{ balance.usedDays }}天</span>
                  </div>
                  <div class="balance-item">
                    <span class="label">剩余:</span>
                    <span class="value remaining" :class="{ low: balance.remainingDays < 3 }"
                      >{{ balance.remainingDays }}天</span
                    >
                  </div>
                  <el-progress
                    :percentage="Math.round((balance.usedDays / balance.totalDays) * 100)"
                    :status="balance.remainingDays < 3 ? 'exception' : 'success'"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="统计报表" name="statistics">
        <div class="statistics-container">
          <div class="filter-container">
            <el-date-picker
              v-model="statisticsDateRange"
              type="monthrange"
              range-separator="至"
              start-placeholder="开始月份"
              end-placeholder="结束月份"
              format="YYYY-MM"
              value-format="YYYY-MM"
              @change="fetchStatisticsData"
            />
            <el-button type="primary" style="margin-left: 10px" @click="fetchStatisticsData">查询</el-button>
          </div>

          <!-- 汇总信息卡片 -->
          <div class="summary-cards">
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">总请假天数</div>
                <div class="card-value">{{ statisticsSummary.totalDays || 0 }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">请假次数</div>
                <div class="card-value">{{ statisticsSummary.totalCount || 0 }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">通过率</div>
                <div class="card-value">{{ statisticsSummary.approvalRate || '0%' }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">最常用类型</div>
                <div class="card-value">{{ getLeaveTypeLabel(statisticsSummary.mostUsedType) || '-' }}</div>
              </div>
            </el-card>
          </div>

          <el-table :data="statisticsData" style="width: 100%; margin-top: 20px">
            <el-table-column prop="type" label="请假类型">
              <template #default="scope">
                {{ getLeaveTypeLabel(scope.row.type) }}
              </template>
            </el-table-column>
            <el-table-column prop="totalDays" label="总天数" />
            <el-table-column prop="totalCount" label="请假次数" />
            <el-table-column prop="percentage" label="占比" />
          </el-table>

          <div class="chart-container">
            <el-empty v-if="!statisticsData.length" description="暂无数据" />
            <div v-else>
              <div id="leaveStatisticsChart" style="width: 100%; height: 400px"></div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { APP_CONFIG } from '../utils/config.js'

const activeTab = ref('apply')

const leaveForm = reactive({
  leaveType: '',
  startDate: '',
  endDate: '',
  reason: ''
})

const leaveTypes = ref([
  { value: 'ANNUAL_LEAVE', label: '年假' },
  { value: 'SICK_LEAVE', label: '病假' },
  { value: 'PERSONAL_LEAVE', label: '事假' },
  { value: 'MARRIAGE_LEAVE', label: '婚假' },
  { value: 'MATERNITY_LEAVE', label: '产假' },
  { value: 'PATERNITY_LEAVE', label: '陪产假' },
  { value: 'BEREAVEMENT_LEAVE', label: '丧假' },
  { value: 'OTHER', label: '其他' }
])

const myApplicationsList = ref([])
const statisticsData = ref([])
const statisticsSummary = ref({})
const statisticsDateRange = ref([])
const leaveFormRef = ref()
const leaveBalances = ref([])
const currentBalance = ref(null)

// 表单验证规则
const leaveFormRules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }]
}

const fetchMyApplicationsList = async () => {
  try {
    const response = await api.get('/api/leave/my-applications')
    myApplicationsList.value = response.data
  } catch (error) {
    ElMessage.error('获取我的申请列表失败: ' + error.message)
  }
}

const fetchStatisticsData = async () => {
  if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
    return
  }

  try {
    const response = await api.get('/api/leave/statistics', {
      params: {
        startDate: statisticsDateRange.value[0],
        endDate: statisticsDateRange.value[1]
      }
    })
    statisticsData.value = response.data.details || []
    statisticsSummary.value = response.data.summary || {}
    renderChart()
  } catch (error) {
    ElMessage.error('获取统计数据失败: ' + error.message)
  }
}

const renderChart = () => {
  const chartDom = document.getElementById('leaveStatisticsChart')
  if (!chartDom) return

  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '请假分布',
        type: 'pie',
        radius: '50%',
        data: statisticsData.value.map((item) => ({
          value: item.totalDays,
          name: getLeaveTypeLabel(item.type)
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

  myChart.setOption(option)
}

const submitLeave = async () => {
  if (!leaveFormRef.value) return

  try {
    await leaveFormRef.value.validate()

    // 调试：打印发送的数据
    console.log('发送的请假申请数据:', leaveForm)

    await api.post('/api/leave/apply', leaveForm)
    ElMessage.success('请假申请提交成功')
    // 重置表单
    Object.assign(leaveForm, {
      leaveType: '',
      startDate: '',
      endDate: '',
      reason: ''
    })
    fetchMyApplicationsList()
  } catch (error) {
    console.error('请假申请错误:', error)
    console.error('响应数据:', error.response?.data)
    const errorMsg = error.response?.data?.error || error.message
    ElMessage.error('提交失败: ' + errorMsg)
  }
}

const getStatusTagType = (status) => {
  switch (status) {
    case '已通过':
      return 'success'
    case '已拒绝':
      return 'danger'
    case '待审批':
      return 'warning'
    default:
      return 'info'
  }
}

// 将英文请假类型转换为中文显示
const getLeaveTypeLabel = (value) => {
  const type = leaveTypes.value.find((item) => item.value === value)
  return type ? type.label : value
}

// 获取请假余额
const fetchLeaveBalances = async () => {
  try {
    const response = await api.get('/api/leave/balance', {
      params: { year: new Date().getFullYear() }
    })
    leaveBalances.value = response.data
  } catch (error) {
    console.error('获取请假余额失败:', error)
  }
}

// 获取指定类型的余额
const getLeaveBalance = (leaveType) => {
  const balance = leaveBalances.value.find((b) => b.leaveType === leaveType)
  return balance ? balance.remainingDays : 0
}

// 当请假类型改变时
const onLeaveTypeChange = () => {
  if (leaveForm.leaveType) {
    currentBalance.value = leaveBalances.value.find((b) => b.leaveType === leaveForm.leaveType)
  } else {
    currentBalance.value = null
  }
}

// 获取当前选择的请假类型标签
const getCurrentLeaveTypeLabel = () => {
  return getLeaveTypeLabel(leaveForm.leaveType)
}

onMounted(async () => {
  fetchMyApplicationsList()
  fetchLeaveBalances()

  // 从全局配置获取默认日期范围
  statisticsDateRange.value = APP_CONFIG.DEFAULT_DATE_RANGE.getRange()
  fetchStatisticsData()
})
</script>

<style scoped>
.leave-management-container {
  padding: 20px;
}

.balance-container {
  padding: 20px 0;
}

.balance-card {
  margin-bottom: 20px;
  height: 200px;
}

.balance-info {
  padding: 10px 0;
}

.balance-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.balance-item .label {
  color: #666;
  font-size: 14px;
}

.balance-item .value {
  font-weight: bold;
  font-size: 14px;
}

.balance-item .value.used {
  color: #e6a23c;
}

.balance-item .value.remaining {
  color: #67c23a;
}

.balance-item .value.remaining.low {
  color: #f56c6c;
}

.apply-form {
  max-width: 800px;
  margin-top: 20px;
}

.statistics-container {
  margin-top: 20px;
}

.filter-container {
  margin-bottom: 20px;
}

.summary-cards {
  display: flex;
  gap: 20px;
  margin: 20px 0;
}

.summary-card {
  flex: 1;
  text-align: center;
}

.card-content {
  padding: 20px;
}

.card-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.chart-container {
  margin-top: 30px;
}
</style>
