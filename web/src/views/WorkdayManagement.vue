<template>
  <div class="workday-container">
    <el-card class="box-card" shadow="always">
      <template #header>
        <div class="card-header">
          <el-row justify="space-between" align="middle">
            <el-col>
              <h3>工作日管理</h3>
              <p style="color: #666; margin: 5px 0">管理特殊工作日，如调休补班、加班日等</p>
            </el-col>
            <el-col class="button-group" style="text-align: right">
              <el-button type="success" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                添加工作日
              </el-button>
              <el-button type="primary" @click="showGenerateDialog">
                <el-icon><Calendar /></el-icon>
                批量生成
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
                value-format="YYYY"
                @change="fetchWorkdays"
              />
            </el-form-item>
            <el-form-item label="月份">
              <el-select v-model="queryForm.month" placeholder="选择月份" style="width: 120px" @change="fetchWorkdays">
                <el-option label="全部" :value="null" />
                <el-option v-for="month in 12" :key="month" :label="`${month}月`" :value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="类型">
              <el-select
                v-model="queryForm.workType"
                placeholder="选择类型"
                style="width: 150px"
                @change="fetchWorkdays"
              >
                <el-option label="全部" :value="null" />
                <el-option label="正常工作日" value="NORMAL" />
                <el-option label="调休补班" value="MAKEUP" />
                <el-option label="加班日" value="OVERTIME" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchWorkdays">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>

      <el-table v-loading="loading" :data="workdays" style="width: 100%">
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
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getWorkTypeTag(row.workType)">
              {{ getWorkTypeText(row.workType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editWorkday(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteWorkday(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加工作日对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加工作日" width="500px">
      <el-form ref="addFormRef" :model="addForm" :rules="formRules" label-width="80px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="addForm.date" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型" prop="workType">
          <el-select v-model="addForm.workType" placeholder="选择类型" style="width: 100%">
            <el-option label="正常工作日" value="NORMAL" />
            <el-option label="调休补班" value="MAKEUP" />
            <el-option label="加班日" value="OVERTIME" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="addForm.description" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleAddWorkday">添加</el-button>
      </template>
    </el-dialog>

    <!-- 批量生成工作日对话框 -->
    <el-dialog v-model="generateDialogVisible" title="批量生成工作日" width="600px">
      <el-form :model="generateForm" label-width="80px">
        <el-form-item label="年份" prop="year">
          <el-input v-model.number="generateForm.year" placeholder="请输入年份" />
        </el-form-item>
        <el-form-item label="月份" prop="month">
          <el-select v-model="generateForm.month" placeholder="请选择月份">
            <el-option v-for="month in 12" :key="month" :label="`${month}月`" :value="month" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="generateForm.description" placeholder="请输入描述" />
        </el-form-item>
        <el-alert
          title="注意：批量生成将创建该月份的所有工作日（排除节假日，包含周末调休补班日）"
          type="info"
          style="margin-bottom: 20px"
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleGenerateWorkdays">生成</el-button>
      </template>
    </el-dialog>

    <!-- 编辑工作日对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑工作日" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="formRules" label-width="80px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="editForm.date" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型" prop="workType">
          <el-select v-model="editForm.workType" placeholder="选择类型" style="width: 100%">
            <el-option label="正常工作日" value="NORMAL" />
            <el-option label="调休补班" value="MAKEUP" />
            <el-option label="加班日" value="OVERTIME" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleUpdateWorkday">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Calendar } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '@/utils/axios'

const loading = ref(false)
const workdays = ref([])
const addDialogVisible = ref(false)
const generateDialogVisible = ref(false)
const editDialogVisible = ref(false)
const addFormRef = ref()
const editFormRef = ref()

const queryForm = reactive({
  year: new Date().getFullYear().toString(),
  month: null,
  workType: null
})

const addForm = reactive({
  date: '',
  workType: 'NORMAL',
  description: ''
})

const generateForm = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  description: ''
})

const editForm = reactive({
  id: null,
  date: '',
  workType: 'NORMAL',
  description: ''
})

const formRules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  workType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// 获取工作日信息
function fetchWorkdays() {
  loading.value = true
  let url = '/api/workdays'
  const params = new URLSearchParams()

  // 处理年份参数 - 确保年份是数字
  const yearParam = queryForm.year ? parseInt(queryForm.year) : null

  if (yearParam && queryForm.month) {
    url = '/api/workdays/by-month'
    params.append('year', yearParam.toString())
    params.append('month', queryForm.month.toString())
  } else if (queryForm.workType) {
    url = '/api/workdays/by-type'
    params.append('workType', queryForm.workType)
  } else if (yearParam) {
    // 如果只有年份没有月份，获取该年份所有工作日
    url = '/api/workdays/by-year'
    params.append('year', yearParam.toString())
  }

  const finalUrl = params.toString() ? `${url}?${params.toString()}` : url

  axios
    .get(finalUrl)
    .then((response) => {
      workdays.value = response.data
    })
    .catch((error) => {
      console.error('获取工作日失败:', error)
      ElMessage.error('获取工作日失败')
    })
    .finally(() => {
      loading.value = false
    })
}

// 显示添加对话框
function showAddDialog() {
  resetAddForm()
  addDialogVisible.value = true
}

// 显示生成工作日对话框
function showGenerateDialog() {
  resetGenerateForm()
  generateDialogVisible.value = true
}

// 重置添加表单
function resetAddForm() {
  addForm.date = ''
  addForm.workType = 'NORMAL'
  addForm.description = ''
}

// 重置生成工作日表单
function resetGenerateForm() {
  generateForm.year = new Date().getFullYear()
  generateForm.month = new Date().getMonth() + 1
  generateForm.description = ''
}

// 重置查询
function resetQuery() {
  queryForm.year = new Date().getFullYear().toString()
  queryForm.month = null
  queryForm.workType = null
  fetchWorkdays()
}

// 处理添加工作日
function handleAddWorkday() {
  addFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true
      axios
        .post('/api/workdays', {
          date: addForm.date,
          workType: addForm.workType,
          description: addForm.description
        })
        .then(() => {
          ElMessage.success('添加成功')
          addDialogVisible.value = false
          fetchWorkdays()
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

// 处理生成工作日
function handleGenerateWorkdays() {
  loading.value = true
  axios
    .post('/api/workdays/generate', {
      year: generateForm.year,
      month: generateForm.month,
      description: generateForm.description
    })
    .then(() => {
      ElMessage.success('生成成功')
      generateDialogVisible.value = false
      fetchWorkdays()
    })
    .catch((error) => {
      console.error('生成失败:', error)
      const errorMsg = error.response?.data || '生成失败'
      ElMessage.error(errorMsg)
    })
    .finally(() => {
      loading.value = false
    })
}

// 编辑工作日
function editWorkday(workday) {
  editForm.id = workday.id
  editForm.date = new Date(workday.date)
  editForm.workType = workday.workType
  editForm.description = workday.description
  editDialogVisible.value = true
}

// 处理更新工作日
function handleUpdateWorkday() {
  editFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true
      axios
        .put(`/api/workdays/${editForm.id}`, {
          date: editForm.date,
          workType: editForm.workType,
          description: editForm.description
        })
        .then(() => {
          ElMessage.success('更新成功')
          editDialogVisible.value = false
          fetchWorkdays()
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

// 删除工作日
function deleteWorkday(id) {
  ElMessageBox.confirm('此操作将永久删除该工作日, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      loading.value = true
      axios
        .delete(`/api/workdays/${id}`)
        .then(() => {
          ElMessage.success('删除成功')
          fetchWorkdays()
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

// 获取工作类型标签样式
function getWorkTypeTag(workType) {
  const tagMap = {
    NORMAL: '',
    MAKEUP: 'warning',
    OVERTIME: 'success'
  }
  return tagMap[workType] || ''
}

// 获取工作类型文本
function getWorkTypeText(workType) {
  const textMap = {
    NORMAL: '正常工作日',
    MAKEUP: '调休补班',
    OVERTIME: '加班日'
  }
  return textMap[workType] || workType
}

// 加载工作日列表
onMounted(fetchWorkdays)
</script>

<style scoped>
.workday-container {
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
