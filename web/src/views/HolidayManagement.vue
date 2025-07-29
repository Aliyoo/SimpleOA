<template>
  <div class="holiday-container">
    <el-card class="box-card" shadow="always">
      <template #header>
        <div class="card-header">
          <el-row justify="space-between" align="middle">
            <el-col>
              <h3>节假日管理</h3>
              <p style="color: #666; margin: 5px 0">管理法定节假日、公司节假日等</p>
            </el-col>
            <el-col class="button-group" style="text-align: right">
              <el-button type="success" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                添加节假日
              </el-button>
            </el-col>
          </el-row>
        </div>
      </template>

      <!-- 查询条件 -->
      <el-row style="margin-bottom: 20px">
        <el-col :span="24">
          <el-form :inline="true" :model="queryForm" class="query-form">
            <el-form-item label="年份">
              <el-date-picker
                v-model="queryForm.year"
                type="year"
                placeholder="选择年份"
                style="width: 120px"
                @change="fetchHolidays"
              />
            </el-form-item>
            <el-form-item label="月份">
              <el-select v-model="queryForm.month" placeholder="选择月份" style="width: 120px" @change="fetchHolidays">
                <el-option label="全部" :value="null" />
                <el-option v-for="month in 12" :key="month" :label="`${month}月`" :value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="queryForm.type" placeholder="选择类型" style="width: 150px" @change="fetchHolidays">
                <el-option label="全部" :value="null" />
                <el-option label="法定节假日" value="PUBLIC" />
                <el-option label="公司节假日" value="COMPANY" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="名称">
              <el-input
                v-model="queryForm.name"
                placeholder="输入节假日名称"
                style="width: 150px"
                @keyup.enter="searchHolidays"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchHolidays">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>

      <el-table v-loading="loading" :data="holidays" style="width: 100%">
        <el-table-column prop="date" label="日期" width="150">
          <template #default="{ row }">
            {{ formatDate(row.date) }}
          </template>
        </el-table-column>
        <el-table-column label="星期" width="100">
          <template #default="{ row }">
            {{ getWeekday(row.date) }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="节假日名称" width="150" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getHolidayTypeTag(row.type)">
              {{ getHolidayTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editHoliday(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteHoliday(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加节假日对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加节假日" width="500px">
      <el-form ref="addFormRef" :model="addForm" :rules="formRules" label-width="80px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="addForm.date" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入节假日名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="addForm.type" placeholder="选择类型" style="width: 100%">
            <el-option label="法定节假日" value="PUBLIC" />
            <el-option label="公司节假日" value="COMPANY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="addForm.description" placeholder="请输入描述" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleAddHoliday">添加</el-button>
      </template>
    </el-dialog>

    <!-- 编辑节假日对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑节假日" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="formRules" label-width="80px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="editForm.date" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入节假日名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="editForm.type" placeholder="选择类型" style="width: 100%">
            <el-option label="法定节假日" value="PUBLIC" />
            <el-option label="公司节假日" value="COMPANY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" placeholder="请输入描述" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleUpdateHoliday">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '@/utils/axios'

const loading = ref(false)
const holidays = ref([])
const addDialogVisible = ref(false)
const editDialogVisible = ref(false)
const addFormRef = ref()
const editFormRef = ref()

const queryForm = reactive({
  year: new Date().getFullYear(),
  month: null,
  type: null,
  name: ''
})

const addForm = reactive({
  date: '',
  name: '',
  type: 'PUBLIC',
  description: ''
})

const editForm = reactive({
  id: null,
  date: '',
  name: '',
  type: 'PUBLIC',
  description: ''
})

const formRules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  name: [{ required: true, message: '请输入节假日名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// 获取节假日信息
function fetchHolidays() {
  loading.value = true
  let url = '/api/holidays'
  const params = new URLSearchParams()

  if (queryForm.year && queryForm.month) {
    url = '/api/holidays/by-month'
    params.append('year', queryForm.year)
    params.append('month', queryForm.month)
  } else if (queryForm.year) {
    url = '/api/holidays/by-year'
    params.append('year', queryForm.year)
  } else if (queryForm.type) {
    url = '/api/holidays/by-type'
    params.append('type', queryForm.type)
  }

  const finalUrl = params.toString() ? `${url}?${params.toString()}` : url

  axios
    .get(finalUrl)
    .then((response) => {
      holidays.value = response.data
    })
    .catch((error) => {
      console.error('获取节假日失败:', error)
      ElMessage.error('获取节假日失败')
    })
    .finally(() => {
      loading.value = false
    })
}

// 搜索节假日
function searchHolidays() {
  if (queryForm.name) {
    loading.value = true
    axios
      .get(`/api/holidays/search?name=${queryForm.name}`)
      .then((response) => {
        holidays.value = response.data
      })
      .catch((error) => {
        console.error('搜索节假日失败:', error)
        ElMessage.error('搜索节假日失败')
      })
      .finally(() => {
        loading.value = false
      })
  } else {
    fetchHolidays()
  }
}

// 显示添加对话框
function showAddDialog() {
  resetAddForm()
  addDialogVisible.value = true
}

// 重置添加表单
function resetAddForm() {
  addForm.date = ''
  addForm.name = ''
  addForm.type = 'PUBLIC'
  addForm.description = ''
}

// 重置查询
function resetQuery() {
  queryForm.year = new Date().getFullYear()
  queryForm.month = null
  queryForm.type = null
  queryForm.name = ''
  fetchHolidays()
}

// 处理添加节假日
function handleAddHoliday() {
  addFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true
      axios
        .post('/api/holidays', {
          date: addForm.date,
          name: addForm.name,
          type: addForm.type,
          description: addForm.description
        })
        .then(() => {
          ElMessage.success('添加成功')
          addDialogVisible.value = false
          fetchHolidays()
        })
        .catch((error) => {
          console.error('添加失败:', error)
          ElMessage.error('添加失败')
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}

// 编辑节假日
function editHoliday(holiday) {
  editForm.id = holiday.id
  editForm.date = new Date(holiday.date)
  editForm.name = holiday.name
  editForm.type = holiday.type
  editForm.description = holiday.description || ''
  editDialogVisible.value = true
}

// 处理更新节假日
function handleUpdateHoliday() {
  editFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true
      axios
        .put(`/api/holidays/${editForm.id}`, {
          date: editForm.date,
          name: editForm.name,
          type: editForm.type,
          description: editForm.description
        })
        .then(() => {
          ElMessage.success('更新成功')
          editDialogVisible.value = false
          fetchHolidays()
        })
        .catch((error) => {
          console.error('更新失败:', error)
          ElMessage.error('更新失败')
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}

// 删除节假日
function deleteHoliday(id) {
  ElMessageBox.confirm('此操作将永久删除该节假日, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      loading.value = true
      axios
        .delete(`/api/holidays/${id}`)
        .then(() => {
          ElMessage.success('删除成功')
          fetchHolidays()
        })
        .catch((error) => {
          console.error('删除失败:', error)
          ElMessage.error('删除失败')
        })
        .finally(() => {
          loading.value = false
        })
    })
    .catch(() => {
      // 用户取消
    })
}

// 格式化日期
function formatDate(date) {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 获取星期
function getWeekday(date) {
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  const day = new Date(date).getDay()
  return `星期${weekdays[day]}`
}

// 获取节假日类型标签样式
function getHolidayTypeTag(type) {
  const tagMap = {
    PUBLIC: 'danger',
    COMPANY: 'warning',
    OTHER: ''
  }
  return tagMap[type] || ''
}

// 获取节假日类型文本
function getHolidayTypeText(type) {
  const textMap = {
    PUBLIC: '法定节假日',
    COMPANY: '公司节假日',
    OTHER: '其他'
  }
  return textMap[type] || type
}

// 加载节假日列表
onMounted(fetchHolidays)
</script>

<style scoped>
.holiday-container {
  padding: 20px;
}

.box-card {
  min-height: 600px;
}

.card-header {
  padding: 10px 0;
}

.button-group {
  flex: none;
}

.query-form {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
}
</style>
