<template>
  <div class="reimbursement-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="报销申请" name="apply">
        <h1>报销申请</h1>
        <!-- 调试信息 -->
        <div style="background: #f0f0f0; padding: 10px; margin: 10px 0; font-size: 12px;">
          <strong>调试信息:</strong>
          类型: {{ reimbursementForm.type || '未选择' }} |
          金额: {{ reimbursementForm.amount || '未填写' }} |
          日期: {{ reimbursementForm.date || '未选择' }}
        </div>
        <el-form :model="reimbursementForm" :rules="rules" ref="reimbursementFormRef" label-width="120px">
          <el-form-item label="报销类型" prop="type">
            <el-select
              v-model="reimbursementForm.type"
              placeholder="请选择报销类型"
              clearable>
              <el-option label="差旅费" value="travel"></el-option>
              <el-option label="办公用品" value="office"></el-option>
              <el-option label="招待费" value="entertainment"></el-option>
              <el-option label="其他" value="other"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="报销金额" prop="amount">
            <el-input-number
              v-model="reimbursementForm.amount"
              :min="0"
              :precision="2"
              placeholder="请输入金额">
            </el-input-number>
          </el-form-item>
          <el-form-item label="报销日期" prop="date">
            <el-date-picker
              v-model="reimbursementForm.date"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD">
            </el-date-picker>
          </el-form-item>
          <el-form-item label="发票附件" prop="attachments">
            <el-upload
              action="/api/upload"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :file-list="fileList"
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
            <el-button type="info" @click="testFormData">测试数据</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="报销记录" name="records">
        <h2>报销记录</h2>
        <!-- 调试信息 -->
        <div style="background: #f0f0f0; padding: 10px; margin: 10px 0; font-size: 12px;">
          <strong>查询条件:</strong>
          状态: {{ queryForm.status || '全部' }} |
          开始日期: {{ queryForm.startDate || '未选择' }} |
          结束日期: {{ queryForm.endDate || '未选择' }} |
          记录数: {{ totalRecords }}
        </div>
        <!-- 记录详情调试 -->
        <div v-if="records.length > 0" style="background: #e8f4fd; padding: 10px; margin: 10px 0; font-size: 12px;">
          <strong>第一条记录详情:</strong> {{ JSON.stringify(records[0], null, 2) }}
        </div>
        <el-form :inline="true" :model="queryForm" ref="queryFormRef" class="query-form">
          <el-form-item label="状态">
            <el-select
              v-model="queryForm.status"
              placeholder="请选择状态"
              clearable>
              <el-option label="全部" value=""></el-option>
              <el-option label="待审批" value="PENDING"></el-option>
              <el-option label="已通过" value="APPROVED"></el-option>
              <el-option label="已驳回" value="REJECTED"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker
              v-model="queryForm.startDate"
              type="date"
              placeholder="选择开始日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD">
            </el-date-picker>
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker
              v-model="queryForm.endDate"
              type="date"
              placeholder="选择结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD">
            </el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="queryRecords">查询</el-button>
            <el-button @click="resetQueryForm">重置</el-button>
            <el-button type="warning" @click="testSimpleQuery">测试简单查询</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="records" style="width: 100%" border v-loading="loading">
          <el-table-column prop="id" label="ID" width="80"></el-table-column>
          <el-table-column prop="type" label="类型" width="120">
            <template #default="scope">
              {{ getTypeLabel(scope.row.type || scope.row.expenseType) }}
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="scope">
              ¥{{ scope.row.amount }}
            </template>
          </el-table-column>
          <el-table-column prop="expenseDate" label="日期" width="120">
            <template #default="scope">
              {{ scope.row.expenseDate || scope.row.date || '未设置' }}
            </template>
          </el-table-column>
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
import { ref, reactive, onMounted, watch } from 'vue'
import api from '../utils/axios.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('apply')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)
const records = ref([])
const fileList = ref([])
const reimbursementFormRef = ref(null)
const queryFormRef = ref(null)

const reimbursementForm = reactive({
  type: '',
  amount: null,
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
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...queryForm
    }
    console.log('查询参数:', params)

    const response = await api.get('/api/oa/reimbursement/records', { params })
    console.log('查询响应:', response.data)

    // 处理统一响应格式
    if (response.data.code === 200) {
      const data = response.data.data
      console.log('查询结果数据:', data)
      records.value = data.content || []
      totalRecords.value = data.totalElements || 0

      if (data.content && data.content.length === 0) {
        ElMessage.info('暂无报销记录')
      }
    } else {
      ElMessage.error(response.data.message || '获取报销记录失败')
    }
  } catch (error) {
    console.error('查询错误:', error)
    ElMessage.error('获取报销记录失败：' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

// 查询记录
const queryRecords = () => {
  console.log('查询条件:', queryForm)
  currentPage.value = 1
  fetchRecords()
}

// 重置查询表单
const resetQueryForm = () => {
  Object.assign(queryForm, {
    status: '',
    startDate: '',
    endDate: ''
  })
  // 重置表单验证状态
  if (queryFormRef.value) {
    queryFormRef.value.resetFields()
  }
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

    const response = await api.post(`/api/oa/reimbursement/approve/${id}`, { status })

    if (response.data.code === 200) {
      ElMessage.success(response.data.message || '操作成功')
      fetchRecords()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败：' + (error.response?.data?.message || error.message))
    }
  }
}

// 查看详情
const viewDetails = (row) => {
  // TODO: 实现查看详情功能
  console.log('查看详情:', row)
}

const handleUploadSuccess = (response, file, uploadFileList) => {
  console.log('上传成功:', response, file, uploadFileList)
  // 更新文件列表
  fileList.value = uploadFileList
  // 更新表单中的附件URL列表
  reimbursementForm.attachments = uploadFileList.map(item => {
    if (item.response && item.response.data && item.response.data.url) {
      return item.response.data.url
    }
    return item.url || ''
  }).filter(url => url)
  ElMessage.success('上传成功')
}

const handleUploadError = (error) => {
  ElMessage.error('上传失败：' + (error.message || '未知错误'))
}

const submitForm = async () => {
  if (!reimbursementFormRef.value) return

  try {
    // 验证表单
    await reimbursementFormRef.value.validate()

    console.log('提交表单数据:', reimbursementForm)
    const response = await api.post('/api/oa/reimbursement/apply', reimbursementForm)

    if (response.data.code === 200) {
      ElMessage.success(response.data.message || '报销申请提交成功')
      resetForm()
      // 切换到记录标签页
      activeTab.value = 'records'
      // 刷新记录列表
      fetchRecords()
    } else {
      ElMessage.error(response.data.message || '提交失败')
    }
  } catch (error) {
    if (error.response) {
      ElMessage.error('提交失败：' + (error.response?.data?.message || error.message))
    } else {
      // 表单验证失败
      ElMessage.error('请检查表单填写是否完整')
    }
  }
}

const resetForm = () => {
  Object.assign(reimbursementForm, {
    type: '',
    amount: null,
    date: '',
    attachments: [],
    description: ''
  })
  fileList.value = []
  // 重置表单验证状态
  if (reimbursementFormRef.value) {
    reimbursementFormRef.value.resetFields()
  }
}

// 测试表单数据
const testFormData = () => {
  console.log('当前表单数据:', reimbursementForm)
  console.log('当前查询数据:', queryForm)
  ElMessage.info('请查看控制台输出')
}

// 测试简单查询
const testSimpleQuery = async () => {
  try {
    console.log('开始测试简单查询...')
    const response = await api.get('/api/oa/reimbursement/all')
    console.log('简单查询响应:', response.data)

    if (response.data.code === 200) {
      const data = response.data.data
      console.log('简单查询结果:', data)
      ElMessage.success(`查询到 ${data.length} 条记录`)

      // 临时显示在记录列表中
      records.value = data
      totalRecords.value = data.length
    } else {
      ElMessage.error('简单查询失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('简单查询错误:', error)
    ElMessage.error('简单查询失败: ' + (error.response?.data?.message || error.message))
  }
}

// 监听表单数据变化
watch(reimbursementForm, (newVal) => {
  console.log('报销表单数据变化:', newVal)
}, { deep: true })

watch(queryForm, (newVal) => {
  console.log('查询表单数据变化:', newVal)
}, { deep: true })

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

/* 确保表单元素有足够的空间 */
.el-form-item {
  margin-bottom: 18px;
}

.el-select,
.el-input-number,
.el-date-picker {
  width: 100%;
}

/* 调试样式 - 可以临时添加边框来检查元素是否正确渲染 */
.el-select:focus-within,
.el-input-number:focus-within,
.el-date-picker:focus-within {
  border: 2px solid #409eff;
}
</style>