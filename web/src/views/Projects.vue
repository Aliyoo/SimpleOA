<template>
  <div class="projects-container">
    <div class="projects-header">
      <h1>项目管理</h1>
      <el-button type="primary" @click="handleCreate">新建项目</el-button>
    </div>
    
    <router-view v-if="$route.path !== '/projects'" @projectCreated="fetchProjects" />
    
    <el-table v-else :data="projects" style="width: 100%">
      <el-table-column prop="name" label="项目名称" width="180" />
      <el-table-column prop="status" label="状态" width="180" :formatter="formatStatus" />
      <el-table-column prop="startDate" label="开始日期" :formatter="formatDate" />
      <el-table-column prop="endDate" label="结束日期" :formatter="formatDate" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const projects = ref([])

const fetchProjects = async () => {
  try {
    const response = await api.get('/api/projects')
    projects.value = response.data
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

const handleCreate = () => {
  router.push('/projects/new')
}

const handleEdit = (project) => {
  router.push(`/projects/edit/${project.id}`)
}

const handleDelete = async (project) => {
  try {
    await api.delete(`/api/projects/${project.id}`)
    await fetchProjects()
  } catch (error) {
    console.error('删除项目失败:', error)
  }
}

const formatStatus = (row, col, status) => {
  const statusMap = {
    PLANNING: '规划中',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REQUIREMENT: '需求阶段',
    DEVELOPMENT: '开发阶段',
    DESIGN: '设计阶段',
    ACCEPTANCE: '终验阶段'
  }
  return statusMap[status] || status;
}

const formatDate = (row, col, date) => {
  if (!date) return '';

  // 打印调试信息
  console.log('原始日期:', date);

  // 尝试将日期字符串转换为 Date 对象
  const d = new Date(date);
  
  // 检查日期是否有效
  if (isNaN(d.getTime())) {
    console.error('无效日期:', date);
    return '无效日期';
  }

  // 格式化为可读的日期格式，例如 'YYYY-MM-DD'
  return d.toISOString().split('T')[0]; // 只返回日期部分
}

onMounted(() => {
  fetchProjects()
})
</script setup>

<style scoped>
.projects-container {
  padding: 20px;
}

.projects-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>