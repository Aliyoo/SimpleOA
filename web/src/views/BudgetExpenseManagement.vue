<template>
  <div class="expense-container">
    <h1>预算支出管理</h1>
    
    <div class="operation-bar">
      <el-button type="primary" @click="handleCreateExpense">新建支出</el-button>
      <el-select 
        v-model="selectedBudget" 
        placeholder="选择预算" 
        style="width: 300px; margin-left: 10px"
        @change="handleBudgetChange"
        clearable
      >
        <el-option
          v-for="budget in budgets"
          :key="budget.id"
          :label="budget.name"
          :value="budget.id"
        />
      </el-select>
      <el-input
        v-model="expenseSearch"
        placeholder="搜索支出记录"
        style="width: 300px; margin-left: 10px"
        clearable
      />
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="margin-left: 10px"
        @change="handleDateRangeChange"
      />
    </div>

    <el-table :data="filteredExpenses" style="width: 100%; margin-top: 20px">
      <el-table-column prop="expenseDate" label="支出日期" width="120">
        <template #default="scope">
          {{ formatDate(scope.row.expenseDate) }}
        </template>
      </el-table-column>
      <el-table-column label="所属预算" width="180">
        <template #default="scope">
          {{ scope.row.budget?.name || 'N/A' }}
        </template>
      </el-table-column>
      <el-table-column label="预算项目" width="150">
        <template #default="scope">
          {{ scope.row.budgetItem?.name || 'N/A' }}
        </template>
      </el-table-column>
      <el-table-column prop="expenseType" label="支出类型" width="120" />
      <el-table-column prop="amount" label="支出金额" width="120">
        <template #default="scope">
          {{ formatCurrency(scope.row.amount) }}
        </template>
      </el-table-column>
      <el-table-column prop="referenceNumber" label="参考编号" width="150" />
      <el-table-column label="记录人" width="120">
        <template #default="scope">
          {{ scope.row.recordedBy?.realName || 'N/A' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getExpenseStatusType(scope.row.status)">
            {{ scope.row.status || '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleViewExpense(scope.row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEditExpense(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDeleteExpense(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: right"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <!-- 支出表单对话框 -->
    <el-dialog 
      :title="expenseForm.id ? '编辑支出' : '新建支出'" 
      v-model="expenseDialogVisible"
      width="800px"
      @closed="resetExpenseForm"
    >
      <el-form 
        :model="expenseForm" 
        :rules="expenseRules" 
        ref="expenseFormRef" 
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属预算" prop="budgetId">
              <el-select 
                v-model="expenseForm.budgetId" 
                placeholder="选择预算" 
                style="width: 100%"
                @change="handleExpenseBudgetChange"
              >
                <el-option
                  v-for="budget in budgets"
                  :key="budget.id"
                  :label="budget.name"
                  :value="budget.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算项目" prop="budgetItemId">
              <el-select 
                v-model="expenseForm.budgetItemId" 
                placeholder="选择预算项目（可选）" 
                style="width: 100%"
                clearable
              >
                <el-option
                  v-for="item in budgetItems"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="支出金额" prop="amount">
              <el-input-number
                v-model="expenseForm.amount"
                :min="0"
                :precision="2"
                :step="100"
                style="width: 100%"
                placeholder="请输入支出金额"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支出日期" prop="expenseDate">
              <el-date-picker
                v-model="expenseForm.expenseDate"
                type="date"
                placeholder="选择支出日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="支出类型" prop="expenseType">
              <el-select v-model="expenseForm.expenseType" placeholder="选择支出类型" style="width: 100%">
                <el-option label="人工费用" value="人工费用" />
                <el-option label="设备费用" value="设备费用" />
                <el-option label="材料费用" value="材料费用" />
                <el-option label="差旅费用" value="差旅费用" />
                <el-option label="管理费用" value="管理费用" />
                <el-option label="市场费用" value="市场费用" />
                <el-option label="培训费用" value="培训费用" />
                <el-option label="其他费用" value="其他费用" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参考编号" prop="referenceNumber">
              <el-input v-model="expenseForm.referenceNumber" placeholder="请输入参考编号（可选）" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="expenseForm.status" placeholder="选择状态" style="width: 100%">
                <el-option label="正常" value="正常" />
                <el-option label="待审核" value="待审核" />
                <el-option label="已驳回" value="已驳回" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录人" prop="recordedBy">
              <el-select v-model="expenseForm.recordedBy" placeholder="选择记录人" style="width: 100%">
                <el-option
                  v-for="user in users"
                  :key="user.id"
                  :label="user.realName"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="支出描述" prop="description">
          <el-input
            v-model="expenseForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入支出描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="expenseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExpenseForm" :loading="submittingExpense">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios.js'

// 响应式数据
const expenses = ref([])
const budgets = ref([])
const budgetItems = ref([])
const users = ref([])
const expenseSearch = ref('')
const selectedBudget = ref(null)
const dateRange = ref([])
const expenseDialogVisible = ref(false)
const submittingExpense = ref(false)
const expenseFormRef = ref()

// 分页相关
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 表单数据
const initialExpenseFormState = () => ({
  id: null,
  budgetId: null,
  budgetItemId: null,
  amount: null,
  expenseDate: '',
  expenseType: '',
  referenceNumber: '',
  status: '正常',
  description: '',
  recordedBy: null
})

const expenseForm = reactive(initialExpenseFormState())

// 表单验证规则
const expenseRules = {
  budgetId: [{ required: true, message: '请选择所属预算', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入支出金额', trigger: 'blur' },
    { type: 'number', min: 0, message: '支出金额不能为负', trigger: 'blur' }
  ],
  expenseDate: [{ required: true, message: '请选择支出日期', trigger: 'change' }],
  expenseType: [{ required: true, message: '请选择支出类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 计算属性
const filteredExpenses = computed(() => {
  let filtered = expenses.value

  // 根据预算筛选
  if (selectedBudget.value) {
    filtered = filtered.filter(expense => expense.budget?.id === selectedBudget.value)
  }

  // 根据搜索词筛选
  if (expenseSearch.value) {
    const searchLower = expenseSearch.value.toLowerCase()
    filtered = filtered.filter(expense => 
      expense.expenseType?.toLowerCase().includes(searchLower) ||
      expense.description?.toLowerCase().includes(searchLower) ||
      expense.referenceNumber?.toLowerCase().includes(searchLower)
    )
  }

  // 根据日期范围筛选
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    filtered = filtered.filter(expense => {
      if (!expense.expenseDate) return false
      const expenseDate = new Date(expense.expenseDate)
      return expenseDate >= startDate && expenseDate <= endDate
    })
  }

  return filtered
})

// 方法
const fetchExpenses = async () => {
  try {
    const response = await api.get('/api/budgets/expenses')
    expenses.value = response.data
    total.value = response.data.length
  } catch (error) {
    ElMessage.error('获取支出记录失败: ' + error.message)
  }
}

const fetchBudgets = async () => {
  try {
    const response = await api.get('/api/budgets')
    budgets.value = response.data
  } catch (error) {
    ElMessage.error('获取预算列表失败: ' + error.message)
  }
}

const fetchBudgetItems = async (budgetId) => {
  if (!budgetId) {
    budgetItems.value = []
    return
  }
  
  try {
    const response = await api.get(`/api/budgets/${budgetId}/items`)
    budgetItems.value = response.data
  } catch (error) {
    ElMessage.error('获取预算项目失败: ' + error.message)
  }
}

const fetchUsers = async () => {
  try {
    const response = await api.get('/api/users')
    users.value = response.data
  } catch (error) {
    ElMessage.error('获取用户列表失败: ' + error.message)
  }
}

const fetchProjects = async () => {
  try {
    const response = await api.get('/api/projects')
    projects.value = response.data
  } catch (error) {
    ElMessage.error('获取项目列表失败: ' + error.message)
  }
}

const handleBudgetChange = (budgetId) => {
  if (budgetId) {
    fetchBudgetItems(budgetId)
    const selected = budgets.value.find(budget => budget.id === budgetId)
    if (selected && selected.project) {
      projects.value = [selected.project]
    }
  }
}

const handleDateRangeChange = (dates) => {
  // 日期范围改变时的处理逻辑已在计算属性中实现
}

const handleExpenseBudgetChange = (budgetId) => {
  fetchBudgetItems(budgetId)
  // 清除之前选择的预算项目
  expenseForm.budgetItemId = null
}

const resetExpenseForm = () => {
  Object.assign(expenseForm, initialExpenseFormState())
  budgetItems.value = []
  expenseFormRef.value?.clearValidate()
}

const handleCreateExpense = () => {
  resetExpenseForm()
  expenseDialogVisible.value = true
}

const handleEditExpense = (expense) => {
  Object.assign(expenseForm, {
    ...expense,
    budgetId: expense.budget?.id,
    budgetItemId: expense.budgetItem?.id,
    recordedBy: expense.recordedBy?.id,
    expenseDate: expense.expenseDate ? new Date(expense.expenseDate).toISOString().slice(0, 10) : ''
  })
  
  // 加载该预算的预算项目
  if (expense.budget?.id) {
    fetchBudgetItems(expense.budget.id)
  }
  
  expenseDialogVisible.value = true
}

const handleViewExpense = (expense) => {
  ElMessage.info('查看支出详情功能待实现')
}

const handleDeleteExpense = async (expense) => {
  try {
    await ElMessageBox.confirm(
      `确定删除这条支出记录吗？金额：${formatCurrency(expense.amount)}`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.delete(`/api/budgets/expenses/${expense.id}`)
    ElMessage.success('删除成功')
    fetchExpenses()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error('删除失败: ' + error.message)
  }
}

const submitExpenseForm = async () => {
  if (!expenseFormRef.value) return
  
  await expenseFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingExpense.value = true
      try {
        const payload = {
          ...expenseForm,
          // 处理日期格式
          expenseDate: expenseForm.expenseDate ? new Date(expenseForm.expenseDate) : new Date(),
          // 设置记录时间为当前时间
          recordTime: new Date().toISOString(),
          // 设置创建时间和更新时间
          createTime: expenseForm.id ? expenseForm.createTime : new Date().toISOString(),
          lastUpdateTime: new Date().toISOString()
        }

        if (payload.id) {
          // 更新现有支出记录
          await api.put(`/api/budgets/expenses/${payload.id}`, payload)
          ElMessage.success('支出记录更新成功')
        } else {
          // 创建新的支出记录
          await api.post('/api/budgets/expenses', payload)
          ElMessage.success('支出记录创建成功')
        }

        expenseDialogVisible.value = false
        fetchExpenses()
        } catch (error) {
        ElMessage.error('操作失败: ' + error.message)
      } finally {
        submittingExpense.value = false
      }
    }
  })
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchExpenses()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchExpenses()
}

// 工具方法
const formatDate = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('zh-CN', { 
    style: 'currency', 
    currency: 'CNY' 
  }).format(value || 0)
}

const getExpenseStatusType = (status) => {
  const statusMap = {
    '正常': 'success',
    '待审核': 'warning',
    '已驳回': 'danger'
  }
  return statusMap[status] || 'success'
}

const projects = ref([])

// 生命周期
onMounted(() => {
  fetchExpenses()
  fetchBudgets()
  fetchUsers()
})
</script>

<style scoped>
.expense-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.el-table {
  margin-top: 20px;
}

.el-pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
