<template>
  <div class="reimbursement-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="报销申请" name="apply">
        <h1>报销申请</h1>
        <el-form :model="reimbursementForm" :rules="rules" ref="reimbursementForm" label-width="120px">
          <el-form-item label="报销类型" prop="type">
            <el-select v-model="reimbursementForm.type" placeholder="请选择报销类型">
              <el-option label="差旅费" value="travel"></el-option>
              <el-option label="办公用品" value="office"></el-option>
              <el-option label="招待费" value="entertainment"></el-option>
              <el-option label="其他" value="other"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="报销金额" prop="amount">
            <el-input-number v-model="reimbursementForm.amount" :min="0" :precision="2"></el-input-number>
          </el-form-item>
          <el-form-item label="报销日期" prop="date">
            <el-date-picker v-model="reimbursementForm.date" type="date" placeholder="选择日期"></el-date-picker>
          </el-form-item>
          <el-form-item label="发票附件" prop="attachments">
            <el-upload
              action="/api/upload"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :file-list="reimbursementForm.attachments"
              multiple
              :limit="5">
              <el-button type="primary">上传发票</el-button>
              <template #tip>
                <div class="el-upload__tip">
                  请上传清晰的发票照片，最多5张
                </div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item label="报销说明" prop="description">
            <el-input type="textarea" v-model="reimbursementForm.description" rows="4"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitForm">提交申请</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="报销记录" name="records">
        <h2>报销记录</h2>
        <el-form :inline="true" :model="queryForm" ref="queryForm" class="query-form">
          <el-form-item label="状态">
            <el-select v-model="queryForm.status" placeholder="请选择状态">
              <el-option label="全部" value=""></el-option>
              <el-option label="待审批" value="PENDING"></el-option>
              <el-option label="已通过" value="APPROVED"></el-option>
              <el-option label="已驳回" value="REJECTED"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker v-model="queryForm.startDate" type="date" placeholder="选择开始日期"></el-date-picker>
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker v-model="queryForm.endDate" type="date" placeholder="选择结束日期"></el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="queryRecords">查询</el-button>
            <el-button @click="resetQueryForm">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="records" style="width: 100%" border v-loading="loading">
          <el-table-column prop="id" label="ID" width="80"></el-table-column>
          <el-table-column prop="type" label="类型" width="120">
            <template #default="scope">
              {{ getTypeLabel(scope.row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="scope">
              ¥{{ scope.row.amount }}
            </template>
          </el-table-column>
          <el-table-column prop="date" label="日期" width="120"></el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" width="200"></el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button v-if="scope.row.status === 'PENDING'" type="primary" size="small" @click="approveRecord(scope.row.id, 'APPROVED')">通过</el-button>
              <el-button v-if="scope.row.status === 'PENDING'" type="danger" size="small" @click="approveRecord(scope.row.id, 'REJECTED')">驳回</el-button>
              <el-button type="text" size="small" @click="viewDetails(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="totalRecords"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          style="margin-top: 20px;"
        ></el-pagination>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('apply')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)
const records = ref([])

const reimbursementForm = ref({
  type: '',
  amount: 0,
  date: '',
  attachments: [],
  description: ''
})

const queryForm = reactive({
  status: '',
  startDate: '',
  endDate: ''
})

const rules = {
  type: [{ required: true, message: '请选择报销类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入报销金额', trigger: 'blur' }],
  date: [{ required: true, message: '请选择报销日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入报销说明', trigger: 'blur' }]
}

// 获取报销记录列表
const fetchRecords = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/oa/reimbursement/records', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        ...queryForm
      }
    })
    records.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('获取报销记录失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 查询记录
const queryRecords = () => {
  currentPage.value = 1
  fetchRecords()
}

// 重置查询表单
const resetQueryForm = () => {
  queryForm.status = ''
  queryForm.startDate = ''
  queryForm.endDate = ''
  queryRecords()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchRecords()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchRecords()
}

// 获取类型标签
const getTypeLabel = (type) => {
  const typeMap = {
    'travel': '差旅费',
    'office': '办公用品',
    'entertainment': '招待费',
    'other': '其他'
  }
  return typeMap[type] || type
}

// 获取状态标签
const getStatusLabel = (status) => {
  const statusMap = {
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  }
  return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return typeMap[status] || ''
}

// 审批记录
const approveRecord = async (id, status) => {
  try {
    await ElMessageBox.confirm(
      `确定要${status === 'APPROVED' ? '通过' : '驳回'}该报销申请吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await api.post(`/api/oa/reimbursement/approve/${id}`, { status })
    ElMessage.success('操作成功')
    fetchRecords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败：' + error.message)
    }
  }
}

// 查看详情
const viewDetails = (row) => {
  // TODO: 实现查看详情功能
  console.log('查看详情:', row)
}

const handleUploadSuccess = (response, file, fileList) => {
  reimbursementForm.value.attachments = fileList
  ElMessage.success('上传成功')
}

const handleUploadError = (error) => {
  ElMessage.error('上传失败：' + error.message)
}

const submitForm = async () => {
  try {
    await api.post('/api/oa/reimbursement/apply', reimbursementForm.value)
    ElMessage.success('报销申请提交成功')
    resetForm()
    // 切换到记录标签页
    activeTab.value = 'records'
    // 刷新记录列表
    fetchRecords()
  } catch (error) {
    ElMessage.error('提交失败：' + error.message)
  }
}

const resetForm = () => {
  reimbursementForm.value = {
    type: '',
    amount: 0,
    date: '',
    attachments: [],
    description: ''
  }
}

// 页面加载时获取记录列表
onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.reimbursement-container {
  padding: 20px;
}

.query-form {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>