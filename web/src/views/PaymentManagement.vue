<template>
  <div class="payment-management-container">
    <h1>回款管理</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="回款记录" name="record">
        <el-form :model="paymentForm" label-width="100px" class="record-form">
          <el-form-item label="项目名称" prop="project">
            <el-select v-model="paymentForm.project" placeholder="请选择项目">
              <el-option 
                v-for="project in projectList" 
                :key="project.id" 
                :label="project.name" 
                :value="project.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="回款金额" prop="amount">
            <el-input-number v-model="paymentForm.amount" :min="0" :precision="2" />
          </el-form-item>
          
          <el-form-item label="回款日期" prop="date">
            <el-date-picker 
              v-model="paymentForm.date" 
              type="date" 
              placeholder="选择回款日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          
          <el-form-item label="回款方式" prop="method">
            <el-select v-model="paymentForm.method" placeholder="请选择回款方式">
              <el-option 
                v-for="method in paymentMethods" 
                :key="method.value" 
                :label="method.label" 
                :value="method.value"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="备注" prop="remark">
            <el-input 
              v-model="paymentForm.remark" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入备注信息"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitPayment">提交记录</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="回款统计" name="statistics">
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
            <el-table-column prop="projectName" label="项目" />
            <el-table-column prop="totalAmount" label="回款总额" />
            <el-table-column prop="paymentCount" label="回款次数" />
          </el-table>
          
          <div class="chart-container">
            <el-empty description="暂无数据" v-if="!statisticsData.length" />
            <div v-else>
              <div id="paymentStatisticsChart" style="width: 100%; height: 400px"></div>
            </div>
          </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="回款提醒" name="reminder">
        <el-table :data="reminderList" style="width: 100%">
          <el-table-column prop="projectName" label="项目" width="180" />
          <el-table-column prop="expectedAmount" label="预期金额" width="120" />
          <el-table-column prop="expectedDate" label="预期日期" width="120" />
          <el-table-column prop="daysLeft" label="剩余天数" width="100" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <el-button 
                size="small" 
                type="primary" 
                @click="markAsPaid(scope.row)"
                v-if="scope.row.status === '待回款'"
              >
                标记为已回款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

export default {
  setup() {
    const activeTab = ref('record')
    
    const paymentForm = reactive({
      project: '',
      amount: 0,
      date: '',
      method: '',
      remark: ''
    })
    
    const paymentMethods = ref([
      { value: 'bank', label: '银行转账' },
      { value: 'cash', label: '现金' },
      { value: 'check', label: '支票' },
      { value: 'other', label: '其他' }
    ])
    
    const projectList = ref([])
    const statisticsData = ref([])
    const statisticsDateRange = ref([])
    const reminderList = ref([])
    
    const fetchProjects = async () => {
      try {
        const response = await api.get('/api/projects')
        projectList.value = response.data
      } catch (error) {
        ElMessage.error('获取项目列表失败: ' + error.message)
      }
    }
    
    const fetchStatisticsData = async () => {
      if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
        return
      }
      
      try {
        const response = await api.get('/api/payment/statistics', {
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
      const chartDom = document.getElementById('paymentStatisticsChart')
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
            name: '回款分布',
            type: 'pie',
            radius: '50%',
            data: statisticsData.value.map(item => ({
              value: item.totalAmount,
              name: item.projectName
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
    
    const fetchReminderList = async () => {
      try {
        const response = await api.get('/api/payment/reminder-list')
        reminderList.value = response.data
      } catch (error) {
        ElMessage.error('获取提醒列表失败: ' + error.message)
      }
    }
    
    const submitPayment = async () => {
      try {
        await api.post('/api/payment/record', paymentForm)
        ElMessage.success('回款记录提交成功')
        paymentForm.remark = ''
        fetchStatisticsData()
        fetchReminderList()
      } catch (error) {
        ElMessage.error('提交失败: ' + error.message)
      }
    }
    
    const markAsPaid = async (row) => {
      try {
        await api.post(`/api/payment/mark-paid/${row.id}`)
        ElMessage.success('已标记为已回款')
        fetchReminderList()
      } catch (error) {
        ElMessage.error('操作失败: ' + error.message)
      }
    }
    
    const getStatusTagType = (status) => {
      switch (status) {
        case '已回款': return 'success'
        case '待回款': return 'warning'
        case '逾期': return 'danger'
        default: return 'info'
      }
    }
    
    onMounted(() => {
      fetchProjects()
      fetchReminderList()
      
      // 设置默认统计日期范围为当前月
      const now = new Date()
      const year = now.getFullYear()
      const month = (now.getMonth() + 1).toString().padStart(2, '0')
      statisticsDateRange.value = [`${year}-${month}`, `${year}-${month}`]
      fetchStatisticsData()
    })
    
    return {
      activeTab,
      paymentForm,
      paymentMethods,
      projectList,
      statisticsData,
      statisticsDateRange,
      reminderList,
      submitPayment,
      markAsPaid,
      getStatusTagType,
      fetchStatisticsData
    }
  }
}
</script>

<style scoped>
.payment-management-container {
  padding: 20px;
}

.record-form {
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