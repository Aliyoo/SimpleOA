<template>
  <div class="travel-management-container">
    <h1>出差管理</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="出差申请" name="apply">
        <el-form ref="travelFormRef" :model="travelForm" :rules="formRules" label-width="100px" class="apply-form">
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
          
          <el-form-item label="关联项目" prop="projectId">
            <el-select 
              v-model="travelForm.projectId" 
              placeholder="请选择关联项目"
              clearable
              style="width: 100%"
              :loading="projects.length === 0"
            >
              <el-option 
                v-for="project in projects" 
                :key="project.id" 
                :label="project.name" 
                :value="project.id"
              />
              <template #empty>
                <p class="select-empty-text">{{ projects.length === 0 ? '加载项目列表中...' : '没有匹配的项目' }}</p>
              </template>
            </el-select>
          </el-form-item>

          <el-form-item label="出差事由" prop="reason">
            <el-input 
              v-model="travelForm.reason" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入出差事由"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitTravel">提交申请</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="出差列表" name="list">
        <!-- 查询条件栏 -->
        <div class="filter-container">
          <el-form :inline="true" :model="queryForm" class="filter-form">
            <!-- 出差地点筛选 -->
            <el-form-item label="出差地点">
              <el-input
                v-model="queryForm.destination"
                placeholder="请输入出差地点"
                clearable
                style="width: 200px"
              />
            </el-form-item>

            <!-- 状态筛选 -->
            <el-form-item label="审批状态">
              <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 150px">
                <el-option label="待审批" value="PENDING" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已拒绝" value="REJECTED" />
              </el-select>
            </el-form-item>

            <!-- 日期范围筛选 -->
            <el-form-item label="出差日期">
              <el-date-picker
                v-model="queryForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :shortcuts="dateShortcuts"
                style="width: 240px"
              />
            </el-form-item>

            <!-- 搜索按钮 -->
            <el-form-item>
              <el-button type="primary" @click="handleQuery">搜索</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-table :data="filteredTravelList" style="width: 100%">
          <el-table-column prop="destination" label="出差地点" width="120" />
          <el-table-column prop="project" label="关联项目" width="120">
            <template #default="scope">
              {{ scope.row.project ? scope.row.project.name : '无' }}
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="160" />
          <el-table-column prop="endTime" label="结束时间" width="160" />
          <el-table-column prop="days" label="天数" width="80" />
          <el-table-column prop="reason" label="事由">
            <template #default="scope">
              {{ scope.row.reason || scope.row.purpose || '' }}
            </template>
          </el-table-column>
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

        <!-- 分页组件 -->
        <div v-if="filteredTravelList.length > 0" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalCount"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
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
          <el-button type="primary" style="margin-left: 10px" @click="fetchStatisticsData">查询</el-button>
          
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
          </div>
          
          <el-table :data="statisticsData" style="width: 100%; margin-top: 20px">
            <el-table-column prop="destination" label="出差地点" />
            <el-table-column prop="totalDays" label="总天数" />
          </el-table>
          
          <div class="chart-container">
            <el-empty v-if="!statisticsData.length" description="暂无数据" />
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
import { ref, reactive, computed, onMounted } from 'vue'
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
  projectId: null // 添加项目ID字段
})

const travelList = ref([])
const statisticsData = ref([])
const statisticsSummary = ref({})
const statisticsDateRange = ref([])
const travelFormRef = ref(null)

// 查询条件
const queryForm = reactive({
  destination: '',
  status: '',
  dateRange: []
})

// 分页数据
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)

// 项目列表
const projects = ref([])

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近三个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

// 过滤后的出差列表
const filteredTravelList = computed(() => {
  let result = [...travelList.value]

  // 按出差地点筛选
  if (queryForm.destination) {
    result = result.filter(item => 
      item.destination && item.destination.toLowerCase().includes(queryForm.destination.toLowerCase())
    )
  }

  // 按状态筛选
  if (queryForm.status) {
    result = result.filter(item => item.status === queryForm.status)
  }

  // 按日期范围筛选
  if (queryForm.dateRange && queryForm.dateRange.length === 2) {
    const startDate = new Date(queryForm.dateRange[0])
    const endDate = new Date(queryForm.dateRange[1])
    endDate.setHours(23, 59, 59, 999) // 设置为当天结束时间

    result = result.filter(item => {
      // 使用startTime或startDate进行筛选
      const itemDate = item.startTime ? new Date(item.startTime) : (item.startDate ? new Date(item.startDate) : null)
      if (!itemDate) return false
      return itemDate >= startDate && itemDate <= endDate
    })
  }

  // 更新总数
  totalCount.value = result.length

  // 分页处理
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value
  return result.slice(startIndex, endIndex)
})

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
  projectId: [
    { required: true, message: '请选择关联项目', trigger: 'change' }
  ],
  reason: [
    { required: true, message: '请输入出差事由', trigger: 'blur' },
    { min: 10, max: 500, message: '出差事由长度在10到500个字符', trigger: 'blur' }
  ],
}

const fetchTravelList = async () => {
  try {
    const response = await api.get('/api/travel/list')
    travelList.value = response.data
  } catch (error) {
    ElMessage.error('获取出差列表失败: ' + error.message)
  }
}

const fetchProjects = async () => {
  try {
    const response = await api.get('/api/projects')
    projects.value = response.data
    console.log('成功获取项目列表:', projects.value.length, '个项目')
  } catch (error) {
    console.error('获取项目列表失败:', error)
    ElMessage.error('获取项目列表失败: ' + error.message)
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
        name: '出差天数分布',
        type: 'pie',
        radius: '50%',
        data: statisticsData.value.map(item => ({
          value: item.totalDays,
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
      projectId: null
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
    case 'PENDING_MANAGER_APPROVAL': return 'warning'
    case 'PENDING_FINANCE_APPROVAL': return 'info'
    case 'DRAFT': return ''
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已拒绝'
    case 'PENDING': return '待审批'
    case 'PENDING_MANAGER_APPROVAL': return '待项目经理审批'
    case 'PENDING_FINANCE_APPROVAL': return '待财务审批'
    case 'DRAFT': return '草稿'
    default: return '未知'
  }
}

// 查询处理
const handleQuery = () => {
  currentPage.value = 1 // 重置到第一页
}

// 重置查询条件
const resetQuery = () => {
  Object.assign(queryForm, {
    destination: '',
    status: '',
    dateRange: []
  })
  currentPage.value = 1 // 重置到第一页
}

// 分页大小变更
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1 // 重置到第一页
}

// 当前页变更
const handleCurrentChange = (val) => {
  currentPage.value = val
}

onMounted(async () => {
  fetchTravelList()
  fetchProjects() // 获取项目列表
  
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
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.filter-form .el-form-item {
  margin-right: 15px;
  margin-bottom: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
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
