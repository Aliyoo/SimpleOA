<template>
  <div class="log-management-container">
    <h1>系统日志</h1>
    
    <el-table :data="logList" style="width: 100%">
      <el-table-column prop="timestamp" label="时间" width="180" />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="action" label="操作" />
      <el-table-column prop="details" label="详情" />
      <el-table-column prop="ip" label="IP地址" width="150" />
    </el-table>
    
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total">
    </el-pagination>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'

const logList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchLogs = async () => {
  try {
    const response = await api.get('/api/logs', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    logList.value = response.data.items
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('获取日志失败: ' + error.message)
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchLogs()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchLogs()
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.log-management-container {
  padding: 20px;
}
</style>