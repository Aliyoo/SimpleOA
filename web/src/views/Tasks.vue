<template>
  <div class="tasks-container">
    <h1>任务管理</h1>
    <el-button type="primary" @click="handleCreate">新建任务</el-button>
    <el-table :data="tasks" style="width: 100%">
      <el-table-column prop="name" label="任务名称" width="180" />
      <el-table-column prop="status" label="状态" width="180" />
      <el-table-column prop="priority" label="优先级" />
      <el-table-column prop="dueDate" label="截止日期" />
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
import { useRouter } from 'vue-router'
import api from '../utils/axios'

const router = useRouter()
const tasks = ref([])

const fetchTasks = async () => {
  try {
    const response = await api.get('/api/tasks')
    tasks.value = response.data
  } catch (error) {
    console.error('获取任务列表失败:', error)
  }
}

const handleCreate = () => {
  router.push('/tasks/new')
}

const handleEdit = (task) => {
  router.push(`/tasks/edit/${task.id}`)
}

const handleDelete = async (task) => {
  try {
    await api.delete(`/api/tasks/${task.id}`)
    await fetchTasks()
  } catch (error) {
    console.error('删除任务失败:', error)
  }
}

onMounted(() => {
  fetchTasks()
})
</script>

<style scoped>
.tasks-container {
  padding: 20px;
}
</style>
