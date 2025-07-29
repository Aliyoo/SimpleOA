<template>
  <div class="projects-container">
    <div class="projects-header">
      <h1>项目管理</h1>
      <el-button v-if="$route.path === '/projects'" type="primary" @click="handleCreate">新建项目</el-button>
    </div>
    
    <router-view v-if="$route.path !== '/projects'" @project-created="fetchProjects" />
    
    <div v-else>
      <!-- 查询区域 -->
      <div class="search-section">
        <el-form :model="searchForm" :inline="true" label-width="80px">
          <el-form-item label="项目名称">
            <el-input v-model="searchForm.name" placeholder="请输入项目名称" clearable></el-input>
          </el-form-item>
          <el-form-item label="项目状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="已取消" value="CANCELLED"></el-option>
              <el-option label="需求阶段" value="REQUIREMENT"></el-option>
              <el-option label="设计阶段" value="DESIGN"></el-option>
              <el-option label="开发阶段" value="DEVELOPMENT"></el-option>
              <el-option label="终验阶段" value="ACCEPTANCE"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="项目经理">
            <el-select v-model="searchForm.managerId" placeholder="请选择项目经理" clearable>
              <el-option
                v-for="manager in managerOptions"
                :key="manager.id"
                :label="manager.name"
                :value="manager.id">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 项目列表 -->
      <el-table :data="filteredProjects" style="width: 100%">
        <el-table-column prop="name" label="项目名称" width="180" />
        <el-table-column prop="status" label="状态" width="120" :formatter="formatStatus" />
        <el-table-column prop="manager.realName" label="项目经理" width="120" />
        <el-table-column prop="startDate" label="开始日期" width="120" :formatter="formatDate" />
        <el-table-column prop="endDate" label="结束日期" width="120" :formatter="formatDate" />
        <el-table-column prop="description" label="项目描述" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button 
              v-if="userStore.isAdmin" 
              size="small" 
              type="danger" 
              @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../utils/axios'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const projects = ref([])
const managerOptions = ref([])

// 搜索表单
const searchForm = ref({
  name: '',
  status: '',
  managerId: null
})

// 根据搜索条件过滤项目
const filteredProjects = computed(() => {
  let filtered = projects.value

  if (searchForm.value.name) {
    filtered = filtered.filter(project => 
      project.name.toLowerCase().includes(searchForm.value.name.toLowerCase())
    )
  }

  if (searchForm.value.status) {
    filtered = filtered.filter(project => project.status === searchForm.value.status)
  }

  if (searchForm.value.managerId) {
    filtered = filtered.filter(project => 
      project.manager && project.manager.id === searchForm.value.managerId
    )
  }

  return filtered
})

const fetchProjects = async () => {
  try {
    const response = await api.get('/api/projects')
    projects.value = response.data
  } catch (error) {
    console.error('获取项目列表失败:', error)
    ElMessage.error('获取项目列表失败')
  }
}

const fetchManagers = async () => {
  try {
    const response = await api.get('/api/users/selectable')
    managerOptions.value = response.data.map(user => ({
      id: user.id,
      name: user.realName || user.username
    }))
  } catch (error) {
    console.error('获取项目经理列表失败:', error)
  }
}

const handleCreate = () => {
  router.push('/projects/new')
}

const handleEdit = (project) => {
  router.push(`/projects/edit/${project.id}`)
}

const handleDelete = async (project) => {
  // 双重验证：前端验证超管权限
  if (!userStore.isAdmin) {
    ElMessage.error('只有超级管理员才能删除项目')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除项目 "${project.name}" 吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    await api.delete(`/api/projects/${project.id}`)
    await fetchProjects()
    ElMessage.success('项目删除成功')
  } catch (error) {
    if (error === 'cancel') {
      // 用户取消删除
      return
    }
    console.error('删除项目失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '删除项目失败'
    ElMessage.error(errorMsg)
  }
}

// 搜索功能
const handleSearch = () => {
  // 由于使用了 computed 属性，搜索会自动执行
}

// 重置搜索条件
const handleReset = () => {
  searchForm.value = {
    name: '',
    status: '',
    managerId: null
  }
}

const formatStatus = (row, col, status) => {
  const statusMap = {
    PLANNING: '规划中',
    IN_PROGRESS: '进行中',
    REQUIREMENT: '需求阶段',
    DEVELOPMENT: '开发阶段',
    DESIGN: '设计阶段',
    ACCEPTANCE: '终验阶段',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return statusMap[status] || status;
}

const formatDate = (row, col, date) => {
  if (!date) return '';

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
  fetchManagers()
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

.search-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.search-section .el-form-item {
  margin-bottom: 10px;
}

.search-section .el-input,
.search-section .el-select {
  width: 200px;
}
</style>
