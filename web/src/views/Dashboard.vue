<template>
  <div class="dashboard-container">
    <h1>系统仪表盘</h1>
    
    <div class="stat-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">用户总数</div>
            <div class="stat-value">{{ stats.userCount }}</div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">未读通知</div>
            <div class="stat-value">{{ stats.unreadNotificationCount }}</div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">活跃用户</div>
            <div class="stat-value">{{ stats.activeUserCount }}</div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">今日通知</div>
            <div class="stat-value">{{ stats.todayNotificationCount }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <div class="charts">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <div id="userChart" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card shadow="hover">
            <div id="notificationChart" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import api from '../utils/axios.js'
import * as echarts from 'echarts'

export default {
  setup() {
    const stats = ref({
      userCount: 0,
      unreadNotificationCount: 0,
      activeUserCount: 0,
      todayNotificationCount: 0
    })
    
    const fetchStats = async () => {
      try {
        const response = await api.get('/api/dashboard/stats')
        stats.value = response.data
        
        // 初始化图表
        initCharts()
      } catch (error) {
        console.error('获取仪表盘数据失败:', error)
      }
    }
    
    const initCharts = () => {
      // 用户增长图表
      const userChart = echarts.init(document.getElementById('userChart'))
      userChart.setOption({
        title: {
          text: '用户增长趋势'
        },
        tooltip: {},
        xAxis: {
          data: ['1月', '2月', '3月', '4月', '5月', '6月']
        },
        yAxis: {},
        series: [
          {
            name: '用户数',
            type: 'line',
            data: [120, 132, 101, 134, 90, 230]
          }
        ]
      })
      
      // 通知统计图表
      const notificationChart = echarts.init(document.getElementById('notificationChart'))
      notificationChart.setOption({
        title: {
          text: '通知统计'
        },
        tooltip: {},
        legend: {
          data: ['已读', '未读']
        },
        xAxis: {
          data: ['1月', '2月', '3月', '4月', '5月', '6月']
        },
        yAxis: {},
        series: [
          {
            name: '已读',
            type: 'bar',
            data: [50, 60, 70, 80, 90, 100]
          },
          {
            name: '未读',
            type: 'bar',
            data: [10, 15, 20, 25, 30, 35]
          }
        ]
      })
    }
    
    onMounted(() => {
      fetchStats()
    })
    
    return {
      stats
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-title {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-top: 10px;
}

.charts {
  margin-top: 20px;
}
</style>