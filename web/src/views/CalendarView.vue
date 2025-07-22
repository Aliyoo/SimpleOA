<template>
  <div class="calendar-container">
    <el-card class="box-card" shadow="always">
      <template #header>
        <div class="card-header">
          <el-row justify="space-between" align="middle">
            <el-col>
              <h3>日历管理</h3>
            </el-col>
            <el-col class="button-group" style="text-align: right;">
              <el-button type="primary" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                添加事件
              </el-button>
            </el-col>
          </el-row>
        </div>
      </template>
      
      <FullCalendar 
        :options="calendarOptions" 
        ref="fullCalendar"
      />
    </el-card>

    <!-- 添加/编辑事件对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑事件' : '添加事件'"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="name">
          <el-input v-model="form.name" placeholder="请输入事件标题" />
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker 
            v-model="form.date" 
            type="date" 
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择事件类型" style="width: 100%">
            <el-option label="节假日" value="HOLIDAY" />
            <el-option label="工作日" value="WORKDAY" />
            <el-option label="调休" value="ADJUSTED" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import timeGridPlugin from '@fullcalendar/timegrid'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '@/utils/axios'

// 响应式数据
const fullCalendar = ref(null)
const dialogVisible = ref(false)
const loading = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  date: '',
  type: 'HOLIDAY'
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入事件标题', trigger: 'blur' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  type: [{ required: true, message: '请选择事件类型', trigger: 'change' }]
}

// FullCalendar 配置
const calendarOptions = reactive({
  plugins: [dayGridPlugin, interactionPlugin, timeGridPlugin],
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  initialView: 'dayGridMonth',
  locale: 'zh-cn',
  firstDayOfWeek: 1,
  events: fetchEvents,
  eventClick: handleEventClick,
  dateClick: handleDateClick,
  eventColor: '#409EFF'
})

// 获取事件数据
function fetchEvents(fetchInfo, successCallback, failureCallback) {
  const promises = [
    axios.get('/api/holidays'),
    axios.get('/api/paydays')
  ]
  
  Promise.all(promises)
    .then(responses => {
      const [holidaysResponse, paydaysResponse] = responses
      const events = []
      
      // 添加节假日事件
      holidaysResponse.data.forEach(holiday => {
        events.push({
          id: `holiday-${holiday.id}`,
          title: holiday.name,
          start: holiday.date,
          backgroundColor: '#f56c6c',
          borderColor: '#f56c6c',
          extendedProps: {
            type: holiday.type || 'HOLIDAY',
            originalType: 'holiday',
            originalId: holiday.id
          }
        })
      })
      
      // 添加发薪日事件
      paydaysResponse.data.forEach(payday => {
        events.push({
          id: `payday-${payday.id}`,
          title: payday.description || '发薪日',
          start: payday.date,
          backgroundColor: '#67c23a',
          borderColor: '#67c23a',
          extendedProps: {
            type: 'PAYDAY',
            originalType: 'payday',
            originalId: payday.id
          }
        })
      })
      
      successCallback(events)
    })
    .catch(error => {
      console.error('获取日历事件失败:', error)
      ElMessage.error('获取日历事件失败')
      failureCallback(error)
    })
}

// 处理事件点击
function handleEventClick(info) {
  const event = info.event
  const props = event.extendedProps
  
  ElMessageBox.confirm(
    `事件: ${event.title}\n类型: ${getTypeLabel(props.type)}\n日期: ${event.startStr}\n\n是否要删除此事件？`,
    '事件详情',
    {
      confirmButtonText: '删除',
      cancelButtonText: '关闭',
      type: 'warning',
      distinguishCancelAndClose: true
    }
  ).then(() => {
    deleteEvent(props.originalType, props.originalId)
  }).catch(() => {
    // 用户取消操作
  })
}

// 处理日期点击
function handleDateClick(info) {
  form.date = info.dateStr
  showAddDialog()
}

// 显示添加对话框
function showAddDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 重置表单
function resetForm() {
  form.id = null
  form.name = ''
  form.date = ''
  form.type = 'HOLIDAY'
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 提交表单
function handleSubmit() {
  if (!formRef.value) return
  
  formRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      
      const apiCall = form.type === 'PAYDAY' 
        ? axios.post('/api/paydays', {
            date: form.date,
            description: form.name
          })
        : axios.post('/api/holidays', {
            name: form.name,
            date: form.date,
            type: form.type
          })
      
      apiCall
        .then(() => {
          ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
          dialogVisible.value = false
          refreshCalendar()
        })
        .catch(error => {
          console.error('提交失败:', error)
          ElMessage.error('操作失败')
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}

// 删除事件
function deleteEvent(type, id) {
  const apiCall = type === 'payday' 
    ? axios.delete(`/api/paydays/${id}`)
    : axios.delete(`/api/holidays/${id}`)
  
  apiCall
    .then(() => {
      ElMessage.success('删除成功')
      refreshCalendar()
    })
    .catch(error => {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    })
}

// 刷新日历
function refreshCalendar() {
  if (fullCalendar.value) {
    fullCalendar.value.getApi().refetchEvents()
  }
}

// 获取类型标签
function getTypeLabel(type) {
  const typeMap = {
    'HOLIDAY': '节假日',
    'WORKDAY': '工作日',
    'ADJUSTED': '调休',
    'PAYDAY': '发薪日',
    'OTHER': '其他'
  }
  return typeMap[type] || type
}

// 组件挂载时刷新日历
onMounted(() => {
  refreshCalendar()
})
</script>

<style scoped>
.calendar-container {
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

:deep(.fc) {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

:deep(.fc-toolbar-title) {
  font-size: 18px;
  font-weight: bold;
}

:deep(.fc-button) {
  background-color: #409eff;
  border-color: #409eff;
}

:deep(.fc-button:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

:deep(.fc-event) {
  cursor: pointer;
}

:deep(.fc-daygrid-day:hover) {
  background-color: #f5f7fa;
}
</style>

