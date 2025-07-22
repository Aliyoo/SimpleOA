<template>
  <div class="leave-management-container">
    <h1>请假管理</h1>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="请假申请" name="apply">
        <el-form :model="leaveForm" :rules="leaveFormRules" ref="leaveFormRef" label-width="100px" class="apply-form">
          <el-form-item label="请假类型" prop="leaveType">
            <el-select v-model="leaveForm.leaveType" placeholder="请选择请假类型">
              <el-option
                  v-for="type in leaveTypes"
                  :key="type.value"
                  :label="type.label"
                  :value="type.value"
              />
            </el-select>
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
            <el-input
                v-model="leaveForm.reason"
                type="textarea"
                :rows="4"
                placeholder="请输入请假原因"
            />
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
          <el-table-column prop="startDate" label="开始时间" width="180"/>
          <el-table-column prop="endDate" label="结束时间" width="180"/>
          <el-table-column prop="reason" label="原因"/>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applyDate" label="申请时间" width="180"/>
        </el-table>
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
            <el-button type="primary" @click="fetchStatisticsData" style="margin-left: 10px">查询</el-button>
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
            <el-table-column prop="totalDays" label="总天数"/>
            <el-table-column prop="totalCount" label="请假次数"/>
            <el-table-column prop="percentage" label="占比"/>
          </el-table>

          <div class="chart-container">
            <el-empty description="暂无数据" v-if="!statisticsData.length"/>
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
import {ref, reactive, onMounted} from 'vue'
import api from '../utils/axios.js'
import {ElMessage} from 'element-plus'
import * as echarts from 'echarts'
import {APP_CONFIG} from '../utils/config.js'

const activeTab = ref('apply')

const leaveForm = reactive({
  leaveType: '',
  startDate: '',
  endDate: '',
  reason: ''
})

const leaveTypes = ref([
  {value: 'annual', label: '年假'},
  {value: 'sick', label: '病假'},
  {value: 'personal', label: '事假'},
  {value: 'marriage', label: '婚假'},
  {value: 'maternity', label: '产假'},
  {value: 'paternity', label: '陪产假'},
  {value: 'other', label: '其他'}
])

const myApplicationsList = ref([])
const statisticsData = ref([])
const statisticsSummary = ref({})
const statisticsDateRange = ref([])
const leaveFormRef = ref()

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
        data: statisticsData.value.map(item => ({
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
    if (error.message) {
      ElMessage.error('提交失败: ' + error.message)
    } else {
      console.log('表单验证失败')
    }
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
  const type = leaveTypes.value.find(item => item.value === value)
  return type ? type.label : value
}


onMounted(async () => {
  fetchMyApplicationsList()

  // 从全局配置获取默认日期范围
  statisticsDateRange.value = APP_CONFIG.DEFAULT_DATE_RANGE.getRange();
  fetchStatisticsData()
})
</script>

<style scoped>
.leave-management-container {
  padding: 20px;
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
