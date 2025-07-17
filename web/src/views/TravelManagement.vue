<template>
  <div class="travel-management-container">
    <h1>出差管理</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="出差申请" name="apply">
        <el-form :model="travelForm" :rules="formRules" ref="travelFormRef" label-width="100px" class="apply-form">
          <el-form-item label="出差地点" prop="destination">
            <el-input v-model="travelForm.destination" placeholder="请输入出差地点" />
          </el-form-item>
          
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker 
              v-model="travelForm.startTime" 
              type="datetime" 
              placeholder="选择开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
          </el-form-item>
          
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker 
              v-model="travelForm.endTime" 
              type="datetime" 
              placeholder="选择结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
          </el-form-item>
          
          <el-form-item label="出差天数" prop="days">
            <el-input-number v-model="travelForm.days" :min="1" :max="30" :step="1" />
          </el-form-item>
          
          <el-form-item label="出差事由" prop="reason">
            <el-input 
              v-model="travelForm.reason" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入出差事由"
            />
          </el-form-item>
          
          <el-form-item label="预算金额" prop="budget">
            <el-input-number v-model="travelForm.budget" :min="0" :precision="2" />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitTravel">提交申请</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="出差列表" name="list">
        <el-table :data="travelList" style="width: 100%">
          <el-table-column prop="destination" label="出差地点" width="180" />
          <el-table-column prop="startTime" label="开始时间" width="180" />
          <el-table-column prop="endTime" label="结束时间" width="180" />
          <el-table-column prop="days" label="天数" width="100" />
          <el-table-column prop="budget" label="预算" width="120" />
          <el-table-column prop="reason" label="事由" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <el-button 
                size="small" 
                type="primary" 
                @click="editTravel(scope.row)"
              >
                编辑
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="deleteTravel(scope.row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="统计报表" name="statistics">
        <div class="statistics-container">
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
          
          <!-- 汇总信息卡片 -->
          <div class="summary-cards">
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">总出差天数</div>
                <div class="card-value">{{ statisticsSummary.totalDays || 0 }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">出差次数</div>
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
                <div class="card-title">最常去地点</div>
                <div class="card-value">{{ statisticsSummary.mostUsedDestination || '-' }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">总预算</div>
                <div class="card-value">¥{{ statisticsSummary.totalBudget || 0 }}</div>
              </div>
            </el-card>
          </div>
          
          <el-table :data="statisticsData" style="width: 100%; margin-top: 20px">
            <el-table-column prop="destination" label="出差地点" />
            <el-table-column prop="totalDays" label="总天数" />
            <el-table-column prop="totalBudget" label="总预算" />
          </el-table>
          
          <div class="chart-container">
            <el-empty description="暂无数据" v-if="!statisticsData.length" />
            <div v-else>
              <div id="travelStatisticsChart" style="width: 100%; height: 400px"></div>
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

const travelForm = reactive({
  destination: '',
  startTime: '',
  endTime: '',
  days: 1,
  reason: '',
  budget: 0
})

const travelList = ref([])
const statisticsData = ref([])
const statisticsSummary = ref({})
const statisticsDateRange = ref([])
const travelFormRef = ref(null)

// 表单验证规则
const formRules = {
  destination: [
    { required: true, message: '请输入出差地点', trigger: 'blur' },
    { min: 2, max: 100, message: '出差地点长度在2到100个字符', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' }
  ],
  reason: [
    { required: true, message: '请输入出差事由', trigger: 'blur' },
    { min: 10, max: 500, message: '出差事由长度在10到500个字符', trigger: 'blur' }
  ],
  budget: [
    { required: true, message: '请输入预算金额', trigger: 'blur' },
    { type: 'number', min: 0, message: '预算金额不能为负数', trigger: 'blur' }
  ]
}

const fetchTravelList = async () => {
  try {
    const response = await api.get('/api/travel/list')
    travelList.value = response.data
  } catch (error) {
    ElMessage.error('获取出差列表失败: ' + error.message)
  }
}

const fetchStatisticsData = async () => {
  if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
    return
  }
  
  try {
    const response = await api.get('/api/travel/statistics', {
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
  const chartDom = document.getElementById('travelStatisticsChart')
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
        name: '出差预算分布',
        type: 'pie',
        radius: '50%',
        data: statisticsData.value.map(item => ({
          value: item.totalBudget,
          name: item.destination
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

const submitTravel = async () => {
  // 表单验证
  if (!travelFormRef.value) return
  
  try {
    await travelFormRef.value.validate()
    
    // 额外的时间验证
    if (travelForm.startTime && travelForm.endTime) {
      const startTime = new Date(travelForm.startTime)
      const endTime = new Date(travelForm.endTime)
      if (startTime >= endTime) {
        ElMessage.error('开始时间不能晚于或等于结束时间')
        return
      }
    }
    
    await api.post('/api/travel/apply', travelForm)
    ElMessage.success('出差申请提交成功')
    
    // 清空表单
    Object.assign(travelForm, {
      destination: '',
      startTime: '',
      endTime: '',
      days: 1,
      reason: '',
      budget: 0
    })
    
    // 重置表单验证
    travelFormRef.value.resetFields()
    
    fetchTravelList()
  } catch (error) {
    if (error.name === 'ValidationError') {
      ElMessage.error('请正确填写表单信息')
    } else {
      ElMessage.error('提交失败: ' + error.message)
    }
  }
}

const editTravel = async (row) => {
  // 编辑功能待实现
  ElMessage.info('编辑功能待实现')
}

const deleteTravel = async (row) => {
  try {
    await api.delete(`/api/travel/${row.id}`)
    ElMessage.success('删除成功')
    fetchTravelList()
  } catch (error) {
    ElMessage.error('删除失败: ' + error.message)
  }
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    case 'PENDING': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已拒绝'
    case 'PENDING': return '待审批'
    default: return '未知'
  }
}

onMounted(async () => {
  fetchTravelList()
  
  // 从全局配置获取默认日期范围
  if (APP_CONFIG && APP_CONFIG.DEFAULT_DATE_RANGE) {
    statisticsDateRange.value = APP_CONFIG.DEFAULT_DATE_RANGE.getRange()
  }
  fetchStatisticsData()
})
</script setup>

<style scoped>
.travel-management-container {
  padding: 20px;
}

.apply-form {
  max-width: 800px;
  margin-top: 20px;
}

.statistics-container {
  margin-top: 20px;
}

.chart-container {
  margin-top: 30px;
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
</style>