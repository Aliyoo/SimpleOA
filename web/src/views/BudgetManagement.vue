<template>
  <div class="budget-container">
    <h1>预算管理</h1>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="预算列表" name="budgetList">
        <div class="operation-bar">
          <el-button
            type="primary"
            :disabled="!canCreateBudget"
            :title="canCreateBudget ? '新建预算' : '您没有可管理的项目，无法创建预算'"
            @click="handleCreateBudget"
          >
            新建预算
          </el-button>
          <el-button 
            type="info" 
            :icon="searchFormVisible ? 'ArrowUp' : 'ArrowDown'" 
            @click="toggleSearchForm"
            style="margin-left: 10px"
          >
            {{ searchFormVisible ? '收起搜索' : '展开搜索' }}
          </el-button>
        </div>

        <!-- 高级搜索表单 -->
        <el-collapse-transition>
          <div v-show="searchFormVisible" class="search-form">
            <el-form :model="searchForm" label-width="100px" :inline="true">
              <el-form-item label="关键词">
                <el-input v-model="searchForm.keyword" placeholder="搜索预算名称" clearable style="width: 200px" />
              </el-form-item>
              <el-form-item label="所属项目">
                <el-select v-model="searchForm.projectId" placeholder="选择项目" clearable style="width: 200px">
                  <el-option v-for="project in projectOptions" :key="project.id" :label="project.name" :value="project.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 150px">
                  <el-option label="活跃" value="ACTIVE" />
                  <el-option label="待定" value="PENDING" />
                  <el-option label="关闭" value="CLOSED" />
                </el-select>
              </el-form-item>
              <el-form-item label="开始日期">
                <el-date-picker
                  v-model="searchForm.startDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 240px"
                />
              </el-form-item>
              <el-form-item label="结束日期">
                <el-date-picker
                  v-model="searchForm.endDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 240px"
                />
              </el-form-item>
              <el-form-item label="总预算">
                <el-input-number v-model="searchForm.totalAmountFrom" placeholder="最小金额" :min="0" :precision="2" style="width: 120px" />
                <span style="margin: 0 10px">-</span>
                <el-input-number v-model="searchForm.totalAmountTo" placeholder="最大金额" :min="0" :precision="2" style="width: 120px" />
              </el-form-item>
              <el-form-item label="已使用">
                <el-input-number v-model="searchForm.usedAmountFrom" placeholder="最小金额" :min="0" :precision="2" style="width: 120px" />
                <span style="margin: 0 10px">-</span>
                <el-input-number v-model="searchForm.usedAmountTo" placeholder="最大金额" :min="0" :precision="2" style="width: 120px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSearch" :loading="loading">搜索</el-button>
                <el-button @click="handleResetSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-collapse-transition>

        <el-table :data="budgets" v-loading="loading" style="width: 100%; margin-top: 20px">
          <el-table-column prop="name" label="预算名称" width="180" />
          <el-table-column label="所属项目" width="180">
            <template #default="scope">
              {{ scope.row.project?.name || 'N/A' }}
            </template>
          </el-table-column>
          <el-table-column prop="startDate" label="开始日期" width="120">
            <template #default="scope">
              {{ formatDate(scope.row.startDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="endDate" label="结束日期" width="120">
            <template #default="scope">
              {{ formatDate(scope.row.endDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="总预算" width="120">
            <template #default="scope">
              {{ formatCurrency(scope.row.totalAmount) }}
            </template>
          </el-table-column>
          <el-table-column prop="usedAmount" label="已使用" width="120">
            <template #default="scope">
              {{ formatCurrency(scope.row.usedAmount || 0) }}
            </template>
          </el-table-column>
          <el-table-column prop="remainingAmount" label="剩余金额" width="120">
            <template #default="scope">
              {{
                formatCurrency(
                  scope.row.remainingAmount != null
                    ? scope.row.remainingAmount
                    : scope.row.totalAmount - (scope.row.usedAmount || 0)
                )
              }}
            </template>
          </el-table-column>
          <el-table-column label="使用率" width="180">
            <template #default="scope">
              <el-progress :percentage="calculateUsagePercentage(scope.row)" :status="getProgressStatus(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getBudgetStatusType(scope.row.status)">
                {{ getBudgetStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="scope">
              <div class="operation-buttons">
                <el-button size="small" @click="handleViewBudget(scope.row)">查看</el-button>
                <el-button size="small" type="primary" @click="handleEditBudget(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDeleteBudget(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页组件 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="预算统计" name="budgetStatistics">
        <div class="statistics-container">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>总预算金额</span>
                  </div>
                </template>
                <div class="statistics-value">{{ formatCurrency(totalBudgetAmount) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>已使用金额</span>
                  </div>
                </template>
                <div class="statistics-value">{{ formatCurrency(totalUsedAmount) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>剩余金额</span>
                  </div>
                </template>
                <div class="statistics-value">{{ formatCurrency(totalRemainingAmount) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>使用率</span>
                  </div>
                </template>
                <div class="statistics-value">{{ totalUsagePercentage }}%</div>
              </el-card>
            </el-col>
          </el-row>

          <div class="charts-container">
            <div class="chart-wrapper">
              <h3>预算分布</h3>
              <!-- 这里可以使用 ECharts 等图表库展示预算分布 -->
              <div class="chart-placeholder">预算分布图表</div>
            </div>
            <div class="chart-wrapper">
              <h3>预算使用趋势</h3>
              <!-- 这里可以使用 ECharts 等图表库展示预算使用趋势 -->
              <div class="chart-placeholder">预算使用趋势图表</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="预算预警" name="budgetAlerts">
        <div class="alerts-container">
          <el-table :data="budgetAlerts" style="width: 100%">
            <el-table-column label="预算名称" width="180">
              <template #default="scope">
                {{ scope.row.budget?.name || 'N/A' }}
              </template>
            </el-table-column>
            <el-table-column label="预算项目" width="180">
              <template #default="scope">
                {{ scope.row.budgetItem?.name || 'N/A' }}
              </template>
            </el-table-column>
            <el-table-column prop="alertType" label="预警类型" width="120">
              <template #default="scope">
                <el-tag :type="getAlertTypeTag(scope.row.alertType)">
                  {{ getAlertTypeText(scope.row.alertType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="threshold" label="预警阈值" width="120">
              <template #default="scope">
                {{
                  scope.row.alertType === 'PERCENTAGE' ? scope.row.threshold + '%' : formatCurrency(scope.row.threshold)
                }}
              </template>
            </el-table-column>
            <el-table-column prop="message" label="预警消息" />

            <el-table-column prop="alertDate" label="预警日期" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.alertDate) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getAlertStatusType(scope.row.status)">
                  {{ getAlertStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="resolvedBy" label="解决人" width="120">
              <template #default="scope">
                {{ scope.row.resolvedBy?.username || 'N/A' }}
              </template>
            </el-table-column>
            <el-table-column prop="resolvedDate" label="解决日期" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.resolvedDate) }}
              </template>
            </el-table-column>
            <el-table-column prop="resolution" label="解决方案" />
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button size="small" type="primary" @click="handleViewAlert(scope.row)">查看</el-button>
                <el-button
                  v-if="scope.row.status === '未解决'"
                  size="small"
                  type="success"
                  @click="handleResolveAlert(scope.row)"
                  >解决</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Budget Form Dialog -->
    <el-dialog v-model="budgetDialogVisible" :title="budgetForm.id ? '编辑预算' : '新建预算'" @closed="resetBudgetForm">
      <el-form ref="budgetFormRef" :model="budgetForm" :rules="budgetRules" label-width="100px">
        <el-form-item label="预算名称" prop="name">
          <el-input v-model="budgetForm.name" />
        </el-form-item>
        <el-form-item label="所属项目" prop="projectId">
          <!-- Assuming projects are fetched into a ref named 'projectOptions' -->
          <el-select v-model="budgetForm.projectId" placeholder="选择项目" style="width: 100%">
            <el-option v-for="project in projectOptions" :key="project.id" :label="project.name" :value="project.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总预算" prop="totalAmount">
          <el-input-number
            v-model="budgetForm.totalAmount"
            :precision="2"
            :step="1000"
            :min="0"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="budgetForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="budgetForm.status" placeholder="选择状态" style="width: 100%">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="待定" value="PENDING" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="budgetForm.startDate"
            type="date"
            placeholder="选择开始日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="budgetForm.endDate"
            type="date"
            placeholder="选择结束日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="budgetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingBudget" @click="submitBudgetForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- Placeholder for Alert View Dialog -->
    <!-- <el-dialog title="查看预警详情" v-model="alertViewDialogVisible"> ... </el-dialog> -->

    <!-- Resolve Alert Dialog -->
    <el-dialog v-model="resolveAlertDialogVisible" title="解决预警" @closed="handleResolveAlertDialogClosed">
      <el-form ref="resolveAlertFormRef" :model="resolveAlertForm" label-width="100px">
        <el-form-item label="预警消息">
          <el-input type="textarea" :value="selectedAlert?.message" :rows="3" readonly />
        </el-form-item>
        <el-form-item
          label="解决方案"
          prop="resolution"
          :rules="[{ required: true, message: '请输入解决方案', trigger: 'blur' }]"
        >
          <el-input v-model="resolveAlertForm.resolution" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveAlertDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResolveAlertForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios.js'
import { useUserStore } from '@/stores/user'

// --- Refs and Reactive Variables ---

const userStore = useUserStore()
const activeTab = ref('budgetList')

// Budget List
const budgets = ref([])
const loading = ref(false)
const searchFormVisible = ref(false)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  projectId: null,
  status: '',
  startDateRange: null,
  endDateRange: null,
  totalAmountFrom: null,
  totalAmountTo: null,
  usedAmountFrom: null,
  usedAmountTo: null
})

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0,
  totalPages: 0
})
const budgetDialogVisible = ref(false)
const submittingBudget = ref(false)
const budgetFormRef = ref(null)
const projectOptions = ref([]) // For project selection

// Budget Form
const initialBudgetFormState = () => ({
  id: null,
  name: '',
  projectId: null,
  totalAmount: 0,
  description: '',
  status: 'ACTIVE',
  startDate: null,
  endDate: null
})
const budgetForm = reactive(initialBudgetFormState())

// Budget Alerts
const budgetAlerts = ref([])
const selectedAlert = ref(null)
const resolveAlertDialogVisible = ref(false)
const resolveAlertForm = reactive({
  alertId: null,
  resolution: '',
  resolvedById: 1 // Assuming a default user ID for now, or fetch current user ID
})
const resolveAlertFormRef = ref(null)

// --- Computed Properties ---

// 移除filteredBudgets计算属性，直接使用budgets

// 统计数据
const statistics = ref({
  totalBudgetAmount: 0,
  totalUsedAmount: 0,
  totalRemainingAmount: 0,
  totalUsagePercentage: 0
})

const totalBudgetAmount = computed(() => statistics.value.totalBudgetAmount)
const totalUsedAmount = computed(() => statistics.value.totalUsedAmount)
const totalRemainingAmount = computed(() => statistics.value.totalRemainingAmount)
const totalUsagePercentage = computed(() => statistics.value.totalUsagePercentage)

// 判断用户是否可以创建预算
const canCreateBudget = computed(() => {
  if (!userStore.user) {
    return false
  }

  const userInfo = userStore.user
  const roles = userInfo.roles || []

  // 管理员和财务总是可以创建预算
  const isAdmin = roles.some((role) => role.name === 'ROLE_ADMIN')
  const isFinance = roles.some((role) => role.name === 'ROLE_FINANCE')

  if (isAdmin || isFinance) {
    return projectOptions.value.length > 0
  }

  // 项目经理只有在有管理项目时才能创建预算
  const isProjectManager = roles.some((role) => role.name === 'ROLE_MANAGER')
  if (isProjectManager) {
    return projectOptions.value.length > 0
  }

  // 其他用户不能创建预算
  return false
})

// --- Validation Rules ---
const budgetRules = {
  name: [{ required: true, message: '请输入预算名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  totalAmount: [
    { required: true, message: '请输入总预算金额', trigger: ['blur', 'change'] },
    { type: 'number', min: 0, message: '预算金额不能为负', trigger: ['blur', 'change'] }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (budgetForm.startDate && value && new Date(value) < new Date(budgetForm.startDate)) {
          callback(new Error('结束日期不能早于开始日期'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// --- Methods ---

// 搜索相关方法
const toggleSearchForm = () => {
  searchFormVisible.value = !searchFormVisible.value
}

const handleSearch = () => {
  pagination.page = 1
  fetchBudgets()
}

const handleResetSearch = () => {
  Object.assign(searchForm, {
    keyword: '',
    projectId: null,
    status: '',
    startDateRange: null,
    endDateRange: null,
    totalAmountFrom: null,
    totalAmountTo: null,
    usedAmountFrom: null,
    usedAmountTo: null
  })
  pagination.page = 1
  fetchBudgets()
}

// 分页相关方法
const handleSizeChange = (newSize) => {
  pagination.size = newSize
  pagination.page = 1
  fetchBudgets()
}

const handleCurrentChange = (newPage) => {
  pagination.page = newPage
  fetchBudgets()
}

// Data Fetching
const fetchBudgets = async () => {
  loading.value = true
  try {
    // 构建搜索参数
    const searchParams = {
      page: pagination.page - 1, // 后端从0开始
      size: pagination.size,
      keyword: searchForm.keyword || null,
      projectId: searchForm.projectId || null,
      status: searchForm.status || null,
      startDateFrom: searchForm.startDateRange?.[0] || null,
      startDateTo: searchForm.startDateRange?.[1] || null,
      endDateFrom: searchForm.endDateRange?.[0] || null,
      endDateTo: searchForm.endDateRange?.[1] || null,
      totalAmountFrom: searchForm.totalAmountFrom || null,
      totalAmountTo: searchForm.totalAmountTo || null,
      usedAmountFrom: searchForm.usedAmountFrom || null,
      usedAmountTo: searchForm.usedAmountTo || null,
      sortBy: 'createTime',
      sortDirection: 'desc'
    }

    const response = await api.get('/api/budgets/search', { params: searchParams })
    const data = response.data
    
    budgets.value = data.content || []
    pagination.total = data.totalElements || 0
    pagination.totalPages = data.totalPages || 0
    
    // 更新统计数据
    updateStatistics()
  } catch (error) {
    console.error('获取预算列表失败:', error)
    ElMessage.error('获取预算列表失败: ' + (error.response?.data?.message || error.message))
    budgets.value = []
    pagination.total = 0
    pagination.totalPages = 0
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStatistics = () => {
  const totalAmount = budgets.value.reduce((sum, budget) => sum + (budget.totalAmount || 0), 0)
  const usedAmount = budgets.value.reduce((sum, budget) => sum + (budget.usedAmount || 0), 0)
  
  statistics.value = {
    totalBudgetAmount: totalAmount,
    totalUsedAmount: usedAmount,
    totalRemainingAmount: totalAmount - usedAmount,
    totalUsagePercentage: totalAmount > 0 ? Math.round((usedAmount / totalAmount) * 100) : 0
  }
}

const fetchBudgetAlerts = async () => {
  try {
    // Assuming you have a way to get the current project ID, e.g., from a user store or route params
    // For now, let's assume a dummy project ID or fetch all alerts
    const projectId = 1 // Replace with actual project ID
    const response = await api.get(`/api/budgets/project/${projectId}/alerts`)
    budgetAlerts.value = response.data
  } catch (error) {
    ElMessage.error('获取预算预警失败: ' + error.message)
  }
}

const fetchProjectsForSelect = async () => {
  try {
    // 确保用户信息已加载
    if (!userStore.user) {
      try {
        await userStore.fetchUser()
      } catch (error) {
        console.error('获取用户信息失败:', error)
        ElMessage.error('用户信息获取失败，请重新登录')
        projectOptions.value = []
        return
      }
    }

    // 根据用户权限获取不同的项目列表
    let response
    const userInfo = userStore.user
    console.log('用户信息:', userInfo)

    const roles = userInfo.roles || []
    console.log('用户角色:', roles)

    // 检查用户角色
    const isAdmin = roles.some((role) => role.name === 'ROLE_ADMIN')
    const isFinance = roles.some((role) => role.name === 'ROLE_FINANCE')
    const isProjectManager = roles.some((role) => role.name === 'ROLE_MANAGER')

    console.log('角色判断结果:', { isAdmin, isFinance, isProjectManager })

    if (isAdmin || isFinance) {
      // 管理员和财务可以看到所有项目
      response = await api.get('/api/projects')
      console.log('管理员/财务用户，获取所有项目')
    } else if (isProjectManager && userInfo.id) {
      // 项目经理只能看到自己管理的项目
      try {
        response = await api.get(`/api/projects/manager/${userInfo.id}`)
        console.log('项目经理用户，获取管理的项目:', response.data?.length || 0, '个')

        // 如果项目经理没有管理的项目，显示提示信息
        if (!response.data || response.data.length === 0) {
          ElMessage.warning('您当前没有管理任何项目，无法创建预算')
        }
      } catch (managerError) {
        console.error('获取项目经理管理的项目失败:', managerError)
        ElMessage.error('获取管理项目失败，请联系管理员')
        projectOptions.value = []
        return
      }
    } else if (userInfo.id) {
      // 其他用户看到参与的项目
      try {
        response = await api.get(`/api/projects/user/${userInfo.id}`)
        console.log('普通用户，获取参与的项目:', response.data?.length || 0, '个')
      } catch (userError) {
        console.error('获取用户参与项目失败:', userError)
        ElMessage.error('获取项目列表失败')
        projectOptions.value = []
        return
      }
    } else {
      // 如果没有用户ID，显示错误
      console.error('用户信息缺失')
      ElMessage.error('用户信息缺失，请重新登录')
      projectOptions.value = []
      return
    }

    projectOptions.value = response.data || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
    ElMessage.error('获取项目列表失败: ' + (error.response?.data?.message || error.message))
    projectOptions.value = []
  }
}

// Budget Dialog Methods
const resetBudgetForm = () => {
  Object.assign(budgetForm, initialBudgetFormState())
  budgetFormRef.value?.clearValidate()
}

const handleCreateBudget = () => {
  if (!canCreateBudget.value) {
    ElMessage.warning('您没有可管理的项目，无法创建预算')
    return
  }
  resetBudgetForm()
  budgetDialogVisible.value = true
}

const handleEditBudget = (item) => {
  // Ensure projectId is correctly assigned
  const formData = {
    ...item,
    projectId: item.project?.id, // Handle if project object is nested
    startDate: item.startDate ? new Date(item.startDate).toISOString().slice(0, 10) : null,
    endDate: item.endDate ? new Date(item.endDate).toISOString().slice(0, 10) : null
  }
  Object.assign(budgetForm, formData)
  budgetDialogVisible.value = true
  // nextTick(() => budgetFormRef.value?.clearValidate());
}

const submitBudgetForm = async () => {
  if (!budgetFormRef.value) return
  await budgetFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingBudget.value = true
      try {
        const payload = { ...budgetForm }
        if (payload.id) {
          await api.put(`/api/budgets/${payload.id}`, payload)
          ElMessage.success('预算更新成功')
        } else {
          await api.post('/api/budgets', payload)
          ElMessage.success('预算创建成功')
        }
        budgetDialogVisible.value = false
        fetchBudgets() // Use the new paginated fetchBudgets method
      } catch (error) {
        ElMessage.error('操作失败: ' + error.message)
      } finally {
        submittingBudget.value = false
      }
    }
  })
}

const handleDeleteBudget = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除预算 "${item.name}"?`, '提示', { type: 'warning' })
    await api.delete(`/api/budgets/${item.id}`)
    ElMessage.success('删除成功')
    fetchBudgets() // Use the new paginated fetchBudgets method
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// Placeholder for view budget detail (could open a new page or dialog)
const handleViewBudget = () => {
  ElMessage.info('查看预算详情功能待实现')
  // Example: router.push(`/budget/detail/${item.id}`);
}

// Alert Methods
const handleViewAlert = () => {
  ElMessage.info('查看预警详情功能待实现')
  // Example:
  // selectedAlert.value = alert;
  // alertViewDialogVisible.value = true;
}

const handleResolveAlert = (alert) => {
  selectedAlert.value = alert
  resolveAlertForm.alertId = alert.id
  resolveAlertForm.resolution = '' // Clear previous resolution
  resolveAlertDialogVisible.value = true
}

const handleResolveAlertDialogClosed = () => {
  selectedAlert.value = null
  resolveAlertForm.resolution = ''
  resolveAlertFormRef.value?.clearValidate()
}

const submitResolveAlertForm = async () => {
  if (!resolveAlertFormRef.value) return
  await resolveAlertFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await api.post(`/api/budgets/alerts/${resolveAlertForm.alertId}/resolve`, null, {
          params: {
            resolution: resolveAlertForm.resolution,
            resolvedById: resolveAlertForm.resolvedById
          }
        })
        ElMessage.success('预警已解决')
        resolveAlertDialogVisible.value = false
        fetchBudgetAlerts() // Refresh alerts list
      } catch (error) {
        ElMessage.error('解决预警失败: ' + error.message)
      }
    }
  })
}

// Helper functions for display
const formatCurrency = (value) => {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value || 0)
}

const calculateUsagePercentage = (budget) => {
  if (!budget || budget.totalAmount === 0 || budget.totalAmount == null) return 0
  // 直接使用 (usedAmount / totalAmount) * 100
  return Math.max(0, Math.min(100, Math.round(((budget.usedAmount || 0) / budget.totalAmount) * 100)))
}

const getProgressStatus = (budget) => {
  const percentage = calculateUsagePercentage(budget)
  if (percentage >= 90) return 'exception'
  if (percentage >= 75) return 'warning' // Adjusted threshold
  return 'success'
}

const getBudgetStatusType = (status) => {
  const statusMap = { ACTIVE: 'success', PENDING: 'info', CLOSED: 'info', OVERBUDGET: 'danger' }
  return statusMap[status] || 'info'
}

const getBudgetStatusText = (status) => {
  const statusMap = { ACTIVE: '活跃', PENDING: '待定', CLOSED: '关闭', OVERBUDGET: '超预算' }
  return statusMap[status] || status
}

const getAlertTypeTag = (type) => {
  return type === 'PERCENTAGE' ? 'warning' : 'danger'
}

const getAlertTypeText = (type) => {
  return type === 'PERCENTAGE' ? '百分比预警' : '金额预警'
}

const getAlertStatusType = (status) => {
  const statusMap = { 未解决: 'danger', 已解决: 'success' }
  return statusMap[status] || 'info'
}

const getAlertStatusText = (status) => {
  const statusMap = { 未解决: '未解决', 已解决: '已解决' }
  return statusMap[status] || status
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

// --- Lifecycle Hook ---
onMounted(() => {
  fetchProjectsForSelect() // Fetch projects for the dropdown first
  fetchBudgets() // Then fetch budgets with search/pagination
  fetchBudgetAlerts()
})
</script>

<style scoped>
.budget-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.statistics-container {
  margin-top: 20px;
}

.statistics-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.statistics-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
  padding: 10px 0;
}

.charts-container {
  margin-top: 30px;
  display: flex;
  justify-content: space-between;
}

.chart-wrapper {
  width: 48%;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 20px;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
  color: #909399;
}

.alerts-container {
  margin-top: 20px;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 48px;
}

.search-form {
  background-color: #f8f9fa;
  padding: 20px;
  margin: 20px 0;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.search-form .el-form-item {
  margin-bottom: 15px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
