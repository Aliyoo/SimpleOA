<template>
  <div class="notification-container">
    <h1>系统通知</h1>
    
    <div class="header">
      <el-button type="primary" @click="showDialog('create')">发送通知</el-button>
    </div>
    
    <el-table :data="notificationList" border style="width: 100%">
      <el-table-column prop="title" label="标题" width="180" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="createTime" label="发送时间" width="180" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'read' ? 'success' : 'warning'">
            {{ scope.row.status === 'read' ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="markAsRead(scope.row.id)" v-if="scope.row.status === 'unread'">标记已读</el-button>
          <el-button size="small" type="danger" @click="deleteNotification(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="50%">
      <el-form :model="notificationForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="notificationForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="notificationForm.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="接收人">
          <el-select v-model="notificationForm.receiverIds" multiple placeholder="请选择接收人">
            <el-option 
              v-for="user in userList" 
              :key="user.id" 
              :label="user.realName" 
              :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'

const notificationList = ref([])
const userList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentId = ref('')

const notificationForm = ref({
  title: '',
  content: '',
  receiverIds: []
})

const fetchNotifications = async () => {
  try {
    const response = await api.get('/api/notifications')
    notificationList.value = response.data
  } catch (error) {
    ElMessage.error('获取通知列表失败: ' + error.message)
  }
}

const fetchUsers = async () => {
  try {
    const response = await api.get('/api/users')
    userList.value = response.data
  } catch (error) {
    ElMessage.error('获取用户列表失败: ' + error.message)
  }
}

const showDialog = (type) => {
  if (type === 'edit') {
    dialogTitle.value = '编辑通知'
    isEdit.value = true
  } else {
    dialogTitle.value = '发送通知'
    isEdit.value = false
    notificationForm.value = {
      title: '',
      content: '',
      receiverIds: []
    }
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    if (isEdit.value) {
      await api.put(`/api/notifications/${currentId.value}`, notificationForm.value)
    } else {
      await api.post('/api/notifications', notificationForm.value)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchNotifications()
  } catch (error) {
    ElMessage.error('操作失败: ' + error.message)
  }
}

const markAsRead = async (id) => {
  try {
    await api.patch(`/api/notifications/${id}/read`)
    ElMessage.success('标记已读成功')
    fetchNotifications()
  } catch (error) {
    ElMessage.error('标记已读失败: ' + error.message)
  }
}

const deleteNotification = async (id) => {
  try {
    await api.delete(`/api/notifications/${id}`)
    ElMessage.success('删除成功')
    fetchNotifications()
  } catch (error) {
    ElMessage.error('删除失败: ' + error.message)
  }
}

onMounted(() => {
  fetchNotifications()
  fetchUsers()
})
</script>

<style scoped>
.notification-container {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}
</style>