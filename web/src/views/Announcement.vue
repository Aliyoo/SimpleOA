<template>
  <div class="announcement-container">
    <h1>系统公告</h1>
    
    <div class="header">
      <el-button type="primary" @click="showDialog('create')">发布公告</el-button>
    </div>
    
    <el-table :data="announcementList" border style="width: 100%">
      <el-table-column prop="title" label="标题" width="180" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="showDialog('edit', scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteAnnouncement(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="50%">
      <el-form :model="announcementForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="announcementForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="announcementForm.content" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'

export default {
  setup() {
    const announcementList = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const isEdit = ref(false)
    const currentId = ref('')
    
    const announcementForm = ref({
      title: '',
      content: ''
    })
    
    const fetchAnnouncements = async () => {
      try {
        const response = await api.get('/api/announcements')
        announcementList.value = response.data
      } catch (error) {
        ElMessage.error('获取公告列表失败: ' + error.message)
      }
    }
    
    const showDialog = (type, row) => {
      if (type === 'edit') {
        dialogTitle.value = '编辑公告'
        isEdit.value = true
        currentId.value = row.id
        announcementForm.value = {
          title: row.title,
          content: row.content
        }
      } else {
        dialogTitle.value = '发布公告'
        isEdit.value = false
        announcementForm.value = {
          title: '',
          content: ''
        }
      }
      dialogVisible.value = true
    }
    
    const submitForm = async () => {
      try {
        if (isEdit.value) {
          await api.put(`/api/announcements/${currentId.value}`, announcementForm.value)
        } else {
          await api.post('/api/announcements', announcementForm.value)
        }
        ElMessage.success('操作成功')
        dialogVisible.value = false
        fetchAnnouncements()
      } catch (error) {
        ElMessage.error('操作失败: ' + error.message)
      }
    }
    
    const deleteAnnouncement = async (id) => {
      try {
        await api.delete(`/api/announcements/${id}`)
        ElMessage.success('删除成功')
        fetchAnnouncements()
      } catch (error) {
        ElMessage.error('删除失败: ' + error.message)
      }
    }
    
    onMounted(() => {
      fetchAnnouncements()
    })
    
    return {
      announcementList,
      announcementForm,
      dialogVisible,
      dialogTitle,
      showDialog,
      submitForm,
      deleteAnnouncement
    }
  }
}
</script>

<style scoped>
.announcement-container {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}
</style>