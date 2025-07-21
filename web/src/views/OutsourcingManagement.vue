<template>
  <div class="outsourcing-management-container">
    <h1>外包管理</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="外包申请" name="apply">
        <el-form :model="outsourcingForm" label-width="100px" class="apply-form">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="outsourcingForm.projectName" placeholder="请输入项目名称" />
          </el-form-item>
          
          <el-form-item label="外包公司" prop="company">
            <el-input v-model="outsourcingForm.company" placeholder="请输入外包公司名称" />
          </el-form-item>
          
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker 
              v-model="outsourcingForm.startTime" 
              type="date" 
              placeholder="选择开始时间"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker 
              v-model="outsourcingForm.endTime" 
              type="date" 
              placeholder="选择结束时间"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          
          <el-form-item label="预算金额" prop="budget">
            <el-input-number v-model="outsourcingForm.budget" :min="0" :precision="2" />
          </el-form-item>
          
          <el-form-item label="项目描述" prop="description">
            <el-input 
              v-model="outsourcingForm.description" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入项目描述"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitOutsourcing">提交申请</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="外包列表" name="approval">
        <el-table :data="approvalList" style="width: 100%">
          <el-table-column prop="projectName" label="项目名称" width="180" />
          <el-table-column prop="company" label="外包公司" width="180" />
          <el-table-column prop="startTime" label="开始时间" width="120" />
          <el-table-column prop="endTime" label="结束时间" width="120" />
          <el-table-column prop="budget" label="预算" width="120" />
          <el-table-column prop="description" label="项目描述" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" v-if="isApprover">
            <template #default="scope">
              <el-button 
                size="small" 
                type="success" 
                @click="approveOutsourcing(scope.row)"
                v-if="scope.row.status === '待审批'"
              >
                通过
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="rejectOutsourcing(scope.row)"
                v-if="scope.row.status === '待审批'"
              >
                拒绝
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
          
          <el-table :data="statisticsData" style="width: 100%; margin-top: 20px">
            <el-table-column prop="company" label="外包公司" />
            <el-table-column prop="totalBudget" label="总预算" />
            <el-table-column prop="projectCount" label="项目数量" />
          </el-table>
          
          <div class="chart-container">
            <el-empty description="暂无数据" v-if="!statisticsData.length" />
            <div v-else>
              <div id="outsourcingStatisticsChart" style="width: 100%; height: 400px"></div>
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

const activeTab = ref('apply')

const outsourcingForm = reactive({
  projectName: '',
  company: '',
  startTime: '',
  endTime: '',
  budget: 0,
  description: ''
})

const approvalList = ref([])
const statisticsData = ref([])
const statisticsDateRange = ref([])
const isApprover = ref(false)

const fetchApprovalList = async () => {
  try {
    const response = await api.get('/api/outsourcing/approval-list')
    approvalList.value = response.data
  } catch (error) {
    ElMessage.error('获取审批列表失败: ' + error.message)
  }
}

const fetchStatisticsData = async () => {
  if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
    return
  }
  
  try {
    const response = await api.get('/api/outsourcing/statistics', {
      params: {
        startDate: statisticsDateRange.value[0],
        endDate: statisticsDateRange.value[1]
      }
    })
    statisticsData.value = response.data
    renderChart()
  } catch (error) {
    ElMessage.error('获取统计数据失败: ' + error.message)
  }
}

const renderChart = () => {
  const chartDom = document.getElementById('outsourcingStatisticsChart')
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
        name: '外包预算分布',
        type: 'pie',
        radius: '50%',
        data: statisticsData.value.map(item => ({
          value: item.totalBudget,
          name: item.company
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

const submitOutsourcing = async () => {
  try {
    await api.post('/api/outsourcing/apply', outsourcingForm)
    ElMessage.success('外包申请提交成功')
    outsourcingForm.description = ''
    fetchApprovalList()
  } catch (error) {
    ElMessage.error('提交失败: ' + error.message)
  }
}

const approveOutsourcing = async (row) => {
  try {
    await api.post(`/api/outsourcing/approve/${row.id}`)
    ElMessage.success('已通过审批')
    fetchApprovalList()
  } catch (error) {
    ElMessage.error('操作失败: ' + error.message)
  }
}

const rejectOutsourcing = async (row) => {
  try {
    await api.post(`/api/outsourcing/reject/${row.id}`)
    ElMessage.success('已拒绝审批')
    fetchApprovalList()
  } catch (error) {
    ElMessage.error('操作失败: ' + error.message)
  }
}

const getStatusTagType = (status) => {
  switch (status) {
    case '已通过': return 'success'
    case '已拒绝': return 'danger'
    case '待审批': return 'warning'
    default: return 'info'
  }
}

const checkApproverRole = async () => {
  try {
    const response = await api.get('/api/user/is-approver')
    isApprover.value = response.data
  } catch (error) {
    console.error('检查审批权限失败:', error)
  }
}

onMounted(() => {
  fetchApprovalList()
  checkApproverRole()
  
  // 设置默认统计日期范围为当前月
  const now = new Date()
  const year = now.getFullYear()
  const month = (now.getMonth() + 1).toString().padStart(2, '0')
  statisticsDateRange.value = [`${year}-${month}`, `${year}-${month}`]
  fetchStatisticsData()
})
</script>

<style scoped>
.outsourcing-management-container {
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
</style>