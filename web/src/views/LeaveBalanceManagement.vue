<template>
  <div class="balance-management">
    <el-card>
      <div class="header">
        <h2>假期余额管理</h2>
        <div class="actions">
          <el-button type="primary" @click="showBatchInitDialog">
            <el-icon><Calendar /></el-icon> 批量初始化
          </el-button>
          <el-button type="success" @click="showAdjustDialog">
            <el-icon><EditPen /></el-icon> 手动调整
          </el-button>
          <el-button @click="exportData">
            <el-icon><Download /></el-icon> 导出数据
          </el-button>
          <el-button @click="refreshData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </div>

      <!-- 状态统计 -->
      <div class="status-summary" v-if="statusReport">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic
              title="总用户数"
              :value="statusReport.totalUsers"
              :precision="0"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="已初始化"
              :value="statusReport.initializedUsers"
              :precision="0"
              :value-style="{ color: '#67c23a' }"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="未初始化"
              :value="statusReport.uninitializedUsers"
              :precision="0"
              :value-style="{ color: '#f56c6c' }"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic
              title="初始化率"
              :value="initializationRate"
              suffix="%"
              :precision="2"
              :value-style="{ color: '#409eff' }"
            />
          </el-col>
        </el-row>
      </div>

      <!-- 查询条件 -->
      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="年份">
          <el-select v-model="queryForm.year" placeholder="选择年份" style="width: 120px">
            <el-option
              v-for="year in availableYears"
              :key="year"
              :label="year"
              :value="year"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="部门">
          <el-select
            v-model="queryForm.departmentId"
            placeholder="选择部门"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="假期类型">
          <el-select
            v-model="queryForm.leaveType"
            placeholder="选择类型"
            clearable
            style="width: 150px"
          >
            <el-option label="年假" value="ANNUAL_LEAVE" />
            <el-option label="病假" value="SICK_LEAVE" />
            <el-option label="事假" value="PERSONAL_LEAVE" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="queryForm.status"
            placeholder="选择状态"
            clearable
            style="width: 120px"
          >
            <el-option label="正常" value="normal" />
            <el-option label="警告" value="warning" />
            <el-option label="不足" value="insufficient" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="queryData">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetQuery">
            <el-icon><RefreshRight /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 快捷操作 -->
      <div class="quick-actions" v-if="uninitializedCount > 0">
        <el-alert
          :title="`发现${uninitializedCount}名用户假期余额未初始化`"
          type="warning"
          show-icon
          :closable="false"
        >
          <template #default>
            <el-button
              type="primary"
              size="small"
              @click="showBatchInitDialog"
            >
              立即初始化
            </el-button>
          </template>
        </el-alert>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="balanceData"
        v-loading="loading"
        stripe
        border
        height="600"
        class="balance-table">

        <el-table-column type="selection" width="55" />

        <el-table-column prop="userName" label="员工姓名" width="100" sortable>
          <template #default="{ row }">
            <el-link type="primary" @click="showUserDetail(row)">
              {{ row.userName }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column prop="departmentName" label="部门" width="120" sortable />

        <el-table-column prop="year" label="年份" width="80" sortable />

        <el-table-column prop="leaveType" label="假期类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getLeaveTypeColor(row.leaveType)">
              {{ getLeaveTypeName(row.leaveType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalDays" label="总天数" width="80" sortable>
          <template #default="{ row }">
            <span class="total-days">{{ row.totalDays }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="usedDays" label="已使用" width="80" sortable>
          <template #default="{ row }">
            <span class="used-days">{{ row.usedDays }}</span>
          </template>
        </el-table-column>

        <el-table-column label="剩余天数" width="100">
          <template #default="{ row }">
            <span
              :class="{
                'remaining-days': true,
                'low-balance': row.remainingDays <= 1,
                'warning-balance': row.remainingDays <= 3 && row.remainingDays > 1
              }"
            >
              {{ row.remainingDays }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="seniorityYears" label="工龄(年)" width="90" sortable>
          <template #default="{ row }">
            {{ row.seniorityYears }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="adjustBalance(row)"
              title="调整余额"
            >
              <el-icon><Tools /></el-icon>
            </el-button>
            <el-button
              size="small"
              @click="showDetail(row)"
              title="查看详情"
            >
              <el-icon><View /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 批量初始化对话框 -->
    <el-dialog
      title="批量初始化假期余额"
      v-model="batchInitDialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="batch-init-content">
        <p>确定要初始化 <strong>{{ currentYear }}</strong> 年所有员工的假期余额吗？</p>
        <p class="warning">
          <el-icon><Warning /></el-icon>
          此操作将覆盖现有的假期余额数据，请谨慎操作！
        </p>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="batchInitDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="executeBatchInitialization"
            :loading="batchInitLoading"
          >
            确认初始化
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 调整余额对话框 -->
    <el-dialog
      title="调整假期余额"
      v-model="adjustDialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="adjustFormRef"
        :model="adjustForm"
        :rules="adjustRules"
        label-width="100px"
        @submit.prevent="executeAdjustment">

        <el-form-item label="员工">
          <el-input
            v-model="adjustForm.userName"
            placeholder="请选择员工"
            readonly
          />
        </el-form-item>

        <el-form-item label="年份" prop="year">
          <el-input-number v-model="adjustForm.year" :min="2020" :max="2030" />
        </el-form-item>

        <el-form-item label="假期类型" prop="leaveType">
          <el-select v-model="adjustForm.leaveType" placeholder="选择假期类型">
            <el-option label="年假" value="ANNUAL_LEAVE" />
            <el-option label="病假" value="SICK_LEAVE" />
            <el-option label="事假" value="PERSONAL_LEAVE" />
          </el-select>
        </el-form-item>

        <el-form-item label="调整类型" prop="adjustmentType">
          <el-radio-group v-model="adjustForm.adjustmentType">
            <el-radio label="INCREASE">增加</el-radio>
            <el-radio label="DECREASE">减少</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="调整天数" prop="adjustmentDays">
          <el-input-number
            v-model="adjustForm.adjustmentDays"
            :min="0.5"
            :max="30"
            :step="0.5"
            :precision="1"
          />
        </el-form-item>

        <el-form-item label="调整原因" prop="reason">
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因"
            maxlength="200"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="executeAdjustment"
            :loading="adjustLoading"
          >
            确认调整
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      title="假期余额详情"
      v-model="detailDialogVisible"
      width="600px"
    >
      <div v-if="selectedBalance" class="balance-detail">
        <el-descriptions border>
          <el-descriptions-item label="员工姓名">
            {{ selectedBalance.userName }}
          </el-descriptions-item>
          <el-descriptions-item label="部门">
            {{ selectedBalance.departmentName }}
          </el-descriptions-item>
          <el-descriptions-item label="年份">
            {{ selectedBalance.year }}
          </el-descriptions-item>
          <el-descriptions-item label="假期类型">
            <el-tag :type="getLeaveTypeColor(selectedBalance.leaveType)">
              {{ getLeaveTypeName(selectedBalance.leaveType) }}
            </el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="总天数">
            <span class="detail-value">{{ selectedBalance.totalDays }}天</span>
          </el-descriptions-item>
          <el-descriptions-item label="已使用">
            <span class="detail-value used-days">{{ selectedBalance.usedDays }}天</span>
          </el-descriptions-item>
          <el-descriptions-item label="剩余天数">
            <span
              :class="{
                'detail-value': true,
                'low-balance': selectedBalance.remainingDays <= 1,
                'warning-balance': selectedBalance.remainingDays <= 3
              }"
            >
              {{ selectedBalance.remainingDays }}天
            </span>
          </el-descriptions-item>

          <el-descriptions-item label="工龄" v-if="selectedBalance.seniorityYears">
            {{ selectedBalance.seniorityYears }}年
          </el-descriptions-item>

          <el-descriptions-item label="应用规则" v-if="selectedBalance.appliedRule">
            {{ selectedBalance.appliedRule.description }}
          </el-descriptions-item>

          <el-descriptions-item label="创建时间">
            {{ formatDateTime(selectedBalance.createdAt) }}
          </el-descriptions-item>

          <el-descriptions-item label="更新时间" v-if="selectedBalance.updatedAt">
            {{ formatDateTime(selectedBalance.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Calendar, EditPen, Tools, View, Download, Refresh, Search, RefreshRight, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios'

// 响应式数据
const balanceData = ref([])
const loading = ref(false)
const statusReport = ref({})
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 查询表单
const queryForm = reactive({
  year: new Date().getFullYear(),
  departmentId: null,
  leaveType: null,
  status: null
})

// 可用的年份列表
const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  return [currentYear, currentYear - 1, currentYear - 2, currentYear + 1]
})

const currentYear = computed(() => new Date().getFullYear())

// 部门列表（示例）
const departments = ref([
  { id: 1, name: '技术部' },
  { id: 2, name: '人力资源部' },
  { id: 3, name: '财务部' },
  { id: 4, name: '市场部' }
])

// 对话框状态
const batchInitDialogVisible = ref(false)
const adjustDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const batchInitLoading = ref(false)
const adjustLoading = ref(false)
const selectedBalance = ref(null)

// 调整表单
const adjustFormRef = ref()
const adjustForm = reactive({
  userId: null,
  userName: '',
  year: currentYear.value,
  leaveType: 'ANNUAL_LEAVE',
  adjustmentType: 'INCREASE',
  adjustmentDays: null,
  reason: ''
})

const adjustRules = {
  year: [
    { required: true, message: '请选择年份', trigger: 'change' }
  ],
  leaveType: [
    { required: true, message: '请选择假期类型', trigger: 'change' }
  ],
  adjustmentType: [
    { required: true, message: '请选择调整类型', trigger: 'change' }
  ],
  adjustmentDays: [
    { required: true, message: '请输入调整天数', trigger: 'blur' },
    { type: 'number', required: true, min: 0.5, max: 30, message: '调整天数应在0.5-30之间', trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入调整原因', trigger: 'blur' },
    { min: 5, max: 200, message: '调整原因长度应在5-200字符之间', trigger: 'blur' }
  ]
}

// 计算属性
const uninitializedCount = computed(() => {
  return statusReport.value.uninitializedUsers || 0
})

const initializationRate = computed(() => {
  if (!statusReport.value.totalUsers || statusReport.value.totalUsers === 0) {
    return 0
  }
  const rate = (statusReport.value.initializedUsers / statusReport.value.totalUsers) * 100
  return rate.toFixed(2)
})

// 方法
const fetchBalanceData = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/leave/balance', {
      params: {
        ...queryForm,
        page: pagination.page - 1,
        size: pagination.size
      }
    })

    if (response.data.content) {
      balanceData.value = response.data.content
      pagination.total = response.data.totalElements
    } else {
      balanceData.value = response.data
      pagination.total = response.data.length
    }

    // 过滤数据
    if (queryForm.status) {
      balanceData.value = balanceData.value.filter(item => {
        const remaining = item.remainingDays
        if (queryForm.status === 'warning') {
          return remaining > 1 && remaining <= 3
        } else if (queryForm.status === 'insufficient') {
          return remaining <= 1
        } else {
          return remaining > 3
        }
      })
    }

  } catch (error) {
    ElMessage.error('获取假期余额失败：' + (error.response?.data?.message || error.message))
    balanceData.value = []
  } finally {
    loading.value = false
  }
}

const fetchStatusReport = async () => {
  try {
    const year = queryForm.year || new Date().getFullYear()
    const response = await api.get(`/api/leave/balance/initialization-status/${year}`)
    statusReport.value = response.data
  } catch (error) {
    console.error('获取初始化状态失败', error)
  }
}

const showBatchInitDialog = () => {
  batchInitDialogVisible.value = true
}

const executeBatchInitialization = async () => {
  try {
    batchInitLoading.value = true
    const year = currentYear.value

    const response = await api.post(`/api/leave/balance/initialize/${year}`)
    
    const hasPartialFailure = response.data.hasPartialFailure || false
    const errors = response.data.errors || []
    const errorCount = response.data.errorCount || 0
    
    batchInitDialogVisible.value = false
    
    if (hasPartialFailure && errors.length > 0) {
      // 显示部分失败的详细信息
      const errorList = errors.map((err, index) => `${index + 1}. ${err}`).join('\n')
      ElMessageBox.alert(
        `初始化过程中有 ${errorCount} 个用户失败:\n\n${errorList}`,
        '批量初始化部分失败',
        {
          confirmButtonText: '我知道了',
          type: 'warning',
          dangerouslyUseHTMLString: false
        }
      )
    } else {
      ElMessage.success(response.data.message || '批量初始化成功完成')
    }

    // 刷新数据
    setTimeout(() => {
      fetchBalanceData()
      fetchStatusReport()
    }, 1000)

  } catch (error) {
    const errorMessage = error.response?.data?.message || error.message
    const errors = error.response?.data?.errors || []
    
    if (errors.length > 0) {
      const errorList = errors.map((err, index) => `${index + 1}. ${err}`).join('\n')
      ElMessageBox.alert(
        `${errorMessage}\n\n错误详情:\n${errorList}`,
        '批量初始化失败',
        {
          confirmButtonText: '我知道了',
          type: 'error',
          dangerouslyUseHTMLString: false
        }
      )
    } else {
      ElMessage.error('批量初始化失败:' + errorMessage)
    }
  } finally {
    batchInitLoading.value = false
  }
}

const showAdjustDialog = () => {
  adjustForm.userId = null
  adjustForm.userName = ''
  adjustForm.year = currentYear.value
  adjustForm.adjustmentDays = null
  adjustForm.reason = ''
  adjustDialogVisible.value = true
}

const adjustBalance = (row) => {
  Object.assign(adjustForm, {
    userId: row.userId,
    userName: row.userName,
    year: row.year,
    leaveType: row.leaveType
  })
  adjustDialogVisible.value = true
}

const executeAdjustment = async () => {
  if (!adjustFormRef.value) return

  try {
    await adjustFormRef.value.validate()
    adjustLoading.value = true

    const payload = {
      userId: adjustForm.userId,
      year: adjustForm.year,
      leaveType: adjustForm.leaveType,
      adjustmentType: adjustForm.adjustmentType,
      adjustmentDays: adjustForm.adjustmentDays,
      reason: adjustForm.reason
    }

    await api.post('/api/leave/balance/adjust', payload)
    ElMessage.success('余额调整成功')

    adjustDialogVisible.value = false
    fetchBalanceData()

  } catch (error) {
    ElMessage.error('调整失败：' + (error.response?.data?.message || error.message))
  } finally {
    adjustLoading.value = false
  }
}

const showDetail = (row) => {
  selectedBalance.value = row
  detailDialogVisible.value = true
}

const showUserDetail = async (row) => {
  try {
    // 获取用户年假计算详情
    const response = await api.get(`/api/leave/annual-rules/calculate/current`, {
      params: { userId: row.userId }
    })

    if (response.data) {
      selectedBalance.value = {
        ...row,
        appliedRule: response.data.appliedRule,
        seniorityYears: response.data.seniorityYears
      }
    }

    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败：' + (error.response?.data?.message || error.message))
  }
}

const queryData = () => {
  pagination.page = 1
  fetchBalanceData()
  fetchStatusReport()
}

const resetQuery = () => {
  Object.assign(queryForm, {
    year: currentYear.value,
    departmentId: null,
    leaveType: null,
    status: null
  })
  queryData()
}

const refreshData = () => {
  fetchBalanceData()
  fetchStatusReport()
}

const exportData = () => {
  // TODO: 实现导出功能
  ElMessage.info('导出功能待实现')
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchBalanceData()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchBalanceData()
}

// 辅助方法
const getLeaveTypeName = (leaveType) => {
  const typeMap = {
    'ANNUAL_LEAVE': '年假',
    'SICK_LEAVE': '病假',
    'PERSONAL_LEAVE': '事假',
    'MARRIAGE_LEAVE': '婚假',
    'MATERNITY_LEAVE': '产假',
    'PATERNITY_LEAVE': '陪产假',
    'BEREAVEMENT_LEAVE': '丧假',
    'OTHER_LEAVE': '其他'
  }
  return typeMap[leaveType] || leaveType
}

const getLeaveTypeColor = (leaveType) => {
  const colorMap = {
    'ANNUAL_LEAVE': 'success',
    'SICK_LEAVE': 'warning',
    'PERSONAL_LEAVE': 'info',
    'MARRIAGE_LEAVE': 'primary',
    'MATERNITY_LEAVE': 'danger',
    'PATERNITY_LEAVE': 'warning',
    'BEREAVEMENT_LEAVE': 'info',
    'OTHER_LEAVE': ''
  }
  return colorMap[leaveType] || ''
}

const formatDateTime = (dateTime) => {
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  fetchBalanceData()
  fetchStatusReport()
})
</script>

<style scoped>
.balance-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.actions {
  display: flex;
  gap: 10px;
}

.status-summary {
  margin-bottom: 20px;
}

.query-form {
  margin-bottom: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.quick-actions {
  margin-bottom: 20px;
}

.balance-table {
  margin-bottom: 20px;
}

.total-days {
  font-weight: bold;
  color: #67c23a;
}

.used-days {
  font-weight: bold;
  color: #e6a23c;
}

.remaining-days {
  font-weight: bold;
}

.low-balance {
  color: #f56c6c;
}

.warning-balance {
  color: #e6a23c;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.batch-init-content {
  text-align: center;
  padding: 20px 0;
}

.batch-init-content p {
  margin: 10px 0;
}

.warning {
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-footer {
  text-align: right;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.balance-detail {
  padding: 20px 0;
}

.detail-value {
  font-weight: bold;
  font-size: 16px;
}

.used-days.detail-value {
  color: #e6a23c;
}

.low-balance.detail-value {
  color: #f56c6c;
}

.warning-balance.detail-value {
  color: #e6a23c;
}
</style>