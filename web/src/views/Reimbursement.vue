<template>
  <div class="reimbursement-container">
    <div class="page-header">
      <h1>报销管理</h1>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="报销申请" name="apply">
        <div class="tab-content">
          <!-- 基本信息区域 -->
          <div class="form-section">
            <div class="section-header">
              <h3>基本信息</h3>
            </div>
            <div class="form-content">
              <el-form ref="reimbursementFormRef" :model="reimbursementForm" :rules="formRules" label-width="120px">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="报销标题" prop="title">
                      <el-input v-model="reimbursementForm.title" placeholder="请输入报销标题" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="关联项目" prop="projectId">
                      <el-select
                        v-model="reimbursementForm.projectId"
                        placeholder="请选择关联项目"
                        clearable
                        style="width: 100%"
                      >
                        <el-option
                          v-for="project in projectList"
                          :key="project.id"
                          :label="project.name"
                          :value="project.id"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </div>
          </div>

          <!-- 费用明细区域 -->
          <div class="form-section">
            <div class="section-header">
              <h3>费用明细</h3>
            </div>
            <div class="form-content">
              <el-table :data="reimbursementForm.items" size="small" style="margin-bottom: 20px">
                <el-table-column label="费用日期" width="150" align="left">
                  <template #default="{ row, $index }">
                    <el-date-picker
                      v-model="row.expenseDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="选择费用日期"
                      size="small"
                      style="width: 100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="费用类别" width="120" align="left">
                  <template #default="{ row, $index }">
                    <el-select v-model="row.itemCategory" placeholder="选择费用类别" size="small" style="width: 100%">
                      <el-option label="劳务费" value="劳务费" />
                      <el-option label="房屋费" value="房屋费" />
                      <el-option label="差旅费" value="差旅费" />
                      <el-option label="交通费" value="交通费" />
                      <el-option label="办公费" value="办公费" />
                      <el-option label="通信费" value="通信费" />
                      <el-option label="车辆费" value="车辆费" />
                      <el-option label="货运费" value="货运费" />
                      <el-option label="物料消耗费" value="物料消耗费" />
                      <el-option label="评审验收费" value="评审验收费" />
                      <el-option label="加班餐费" value="加班餐费" />
                      <el-option label="质保维护费(不含人工)" value="质保维护费(不含人工)" />
                      <el-option label="业务招待费" value="业务招待费" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="费用说明" align="left">
                  <template #default="{ row, $index }">
                    <el-input v-model="row.description" placeholder="请输入费用说明" size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="预算来源" width="180" align="left">
                  <template #default="{ row, $index }">
                    <el-select
                      v-model="row.budgetId"
                      placeholder="选择预算"
                      size="small"
                      style="width: 100%"
                      clearable
                      @change="onBudgetChange(row, $index)"
                    >
                      <el-option
                        v-for="budget in availableBudgets"
                        :key="budget.id"
                        :label="`${budget.name} (余额: ¥${budget.remainingAmount})`"
                        :value="budget.id"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="预算明细" width="180" align="left">
                  <template #default="{ row, $index }">
                    <el-select
                      v-model="row.budgetItemId"
                      placeholder="选择预算明细"
                      size="small"
                      style="width: 100%"
                      clearable
                      :disabled="!row.budgetId"
                    >
                      <el-option
                        v-for="item in getBudgetItems(row.budgetId)"
                        :key="item.id"
                        :label="`${item.category} (余额: ¥${item.remainingAmount})`"
                        :value="item.id"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="金额" width="120" align="left">
                  <template #default="{ row, $index }">
                    <el-input-number
                      v-model="row.amount"
                      :precision="2"
                      :min="0"
                      size="small"
                      style="width: 100%"
                      @change="validateBudgetAmount(row)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="left">
                  <template #default="{ $index }">
                    <el-button type="danger" size="small" @click="removeItem($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="table-actions">
                <el-button type="primary" plain @click="addItem">添加明细</el-button>
              </div>

              <el-form :model="reimbursementForm" label-width="120px" style="margin-top: 20px">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="总金额">
                      <el-input-number v-model="totalAmount" :precision="2" disabled style="width: 100%" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </div>
          </div>

          <!-- 凭证上传区域 -->
          <div class="form-section">
            <div class="section-header">
              <h3>凭证上传</h3>
            </div>
            <div class="form-content">
              <el-upload
                v-model:file-list="fileList"
                action="/api/files/upload"
                multiple
                :on-success="handleUploadSuccess"
                list-type="picture-card"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </div>
          </div>

          <!-- 操作按钮区域 -->
          <div class="form-section">
            <div class="form-actions">
              <el-button size="large" @click="saveDraftReimbursement">保存草稿</el-button>
              <el-button type="primary" size="large" @click="submitReimbursement">提交审批</el-button>
              <el-button size="large" @click="resetForm">重置表单</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="报销列表" name="list">
        <div class="tab-content">
          <!-- 查询操作区域 -->
          <div class="operation-section">
            <div class="filter-container">
              <el-row :gutter="16" align="middle">
                <el-col :span="4">
                  <el-select
                    v-model="listFilters.status"
                    placeholder="选择状态"
                    clearable
                    style="width: 100%"
                    @change="fetchReimbursementList"
                  >
                    <el-option label="全部状态" :value="null" />
                    <el-option
                      v-for="status in statusOptions"
                      :key="status.value"
                      :label="status.label"
                      :value="status.value"
                    />
                  </el-select>
                </el-col>
                <el-col :span="8">
                  <el-date-picker
                    v-model="listFilters.dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    clearable
                    style="width: 100%"
                    @change="onDateRangeChange"
                  />
                </el-col>
                <el-col :span="6">
                  <el-input
                    v-model="listFilters.keyword"
                    placeholder="搜索标题关键字"
                    clearable
                    @clear="fetchReimbursementList"
                    @keyup.enter="fetchReimbursementList"
                  >
                    <template #append>
                      <el-button @click="fetchReimbursementList">
                        <el-icon><Search /></el-icon>
                      </el-button>
                    </template>
                  </el-input>
                </el-col>
                <el-col :span="6">
                  <el-button @click="resetFilters">重置</el-button>
                  <el-button type="primary" @click="fetchReimbursementList">查询</el-button>
                </el-col>
              </el-row>
            </div>
          </div>

          <div class="table-section">
            <div class="section-header">
              <h3>报销记录</h3>
            </div>
            <el-table v-loading="listLoading" :data="reimbursementList" style="width: 100%">
              <el-table-column prop="title" label="报销标题" width="200" show-overflow-tooltip align="left" />
              <el-table-column label="关联项目" width="150" show-overflow-tooltip align="left">
                <template #default="scope">
                  {{ scope.row.project?.name || '无关联项目' }}
                </template>
              </el-table-column>
              <el-table-column prop="totalAmount" label="总金额" width="120" align="left">
                <template #default="scope">
                  {{ formatMoney(scope.row.totalAmount) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="150" align="left">
                <template #default="scope">
                  <el-tag :type="getReimbursementStatusTagType(scope.row.status)">
                    {{ formatReimbursementStatus(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="申请时间" width="180" align="left">
                <template #default="scope">
                  {{ formatDate(scope.row.createTime, 'datetime') }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="320" align="left" fixed="right">
                <template #default="scope">
                  <el-button size="small" @click="openDetail(scope.row)">查看详情</el-button>
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="scope.row.status !== 'DRAFT'"
                    @click="editReimbursement(scope.row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="scope.row.status === 'DRAFT'"
                    size="small"
                    type="success"
                    @click="submitReimbursementForApproval(scope.row)"
                  >
                    提交审批
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    :disabled="scope.row.status !== 'DRAFT'"
                    @click="deleteReimbursement(scope.row)"
                  >
                    删除
                  </el-button>
                  <el-tooltip
                    v-if="canViewApproval(scope.row)"
                    content="查看审批状态请前往审批管理模块"
                    placement="top"
                  >
                    <el-button size="small" type="info" @click="goToApprovalManagement(scope.row)">
                      查看审批
                    </el-button>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="统计报表" name="statistics">
        <div class="tab-content">
          <div class="statistics-section">
            <div class="section-header">
              <h3>统计查询</h3>
            </div>
            <el-date-picker
              v-model="statisticsDateRange"
              type="monthrange"
              range-separator="至"
              start-placeholder="开始月份"
              end-placeholder="结束月份"
              format="YYYY-MM"
              value-format="YYYY-MM"
              @change="fetchStatisticsData"
            />
            <el-button type="primary" style="margin-left: 10px" @click="fetchStatisticsData">查询</el-button>
          </div>

          <div class="summary-section">
            <div class="section-header">
              <h3>数据汇总</h3>
            </div>
            <div class="summary-cards">
              <el-card class="summary-card">
                <div class="card-content">
                  <div class="card-title">报销总额</div>
                  <div class="card-value">{{ formatMoney(statisticsSummary.totalAmount || 0) }}</div>
                </div>
              </el-card>
              <el-card class="summary-card">
                <div class="card-content">
                  <div class="card-title">报销次数</div>
                  <div class="card-value">{{ statisticsSummary.totalCount || 0 }}</div>
                </div>
              </el-card>
              <el-card class="summary-card">
                <div class="card-content">
                  <div class="card-title">通过率</div>
                  <div class="card-value">{{ statisticsSummary.approvalRate || '0%' }}</div>
                </div>
              </el-card>
              <el-card class="summary-card">
                <div class="card-content">
                  <div class="card-title">最常类别</div>
                  <div class="card-value">{{ statisticsSummary.mostUsedCategory || '-' }}</div>
                </div>
              </el-card>
              <el-card class="summary-card">
                <div class="card-content">
                  <div class="card-title">平均金额</div>
                  <div class="card-value">{{ formatMoney(statisticsSummary.avgAmount || 0) }}</div>
                </div>
              </el-card>
            </div>
          </div>

          <div class="table-section">
            <div class="section-header">
              <h3>明细统计</h3>
            </div>
            <el-table :data="statisticsData" style="width: 100%">
              <el-table-column prop="category" label="费用类别" />
              <el-table-column prop="totalAmount" label="总金额" />
              <el-table-column prop="count" label="次数" />
              <el-table-column prop="avgAmount" label="平均金额" />
            </el-table>
          </div>

          <div class="chart-section">
            <div class="section-header">
              <h3>图表分析</h3>
            </div>
            <div class="chart-container">
              <el-empty v-if="!statisticsData.length" description="暂无数据" />
              <div v-else>
                <div id="reimbursementStatisticsChart" style="width: 100%; height: 400px"></div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Form Dialog -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑报销申请' : '新建报销申请'" width="60%">
      <el-form :model="formData" label-width="120px">
        <el-form-item label="报销标题" prop="title">
          <el-input v-model="formData.title"></el-input>
        </el-form-item>

        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="formData.projectId" placeholder="请选择关联项目" clearable style="width: 100%">
            <el-option v-for="project in projectList" :key="project.id" :label="project.name" :value="project.id" />
          </el-select>
        </el-form-item>

        <el-divider>费用明细</el-divider>
        <el-table :data="formData.items" size="small">
          <el-table-column label="费用日期">
            <template #default="{ row }">
              <el-date-picker v-model="row.expenseDate" type="date" value-format="YYYY-MM-DD"></el-date-picker>
            </template>
          </el-table-column>
          <el-table-column label="费用类别">
            <template #default="{ row }">
              <el-input v-model="row.itemCategory"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="描述">
            <template #default="{ row }">
              <el-input v-model="row.description"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="金额">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :precision="2"></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ $index }">
              <el-button type="danger" size="small" @click="removeItemForDialog($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button style="margin-top: 10px" @click="addItemForDialog">添加明细</el-button>

        <el-divider>凭证上传</el-divider>
        <el-upload
          v-model:file-list="fileList"
          action="/api/files/upload"
          multiple
          :on-success="handleUploadSuccess"
          list-type="picture-card"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <ReimbursementDetail
      v-if="detailDialogVisible"
      :visible="detailDialogVisible"
      :reimbursement="currentReimbursement"
      @close="closeDetail"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive, watch, defineAsyncComponent } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import api from '../utils/axios.js'
import { Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'
import { APP_CONFIG } from '../utils/config.js'
import { formatMoney, formatDate, formatReimbursementStatus, getReimbursementStatusTagType } from '../utils/format.js'

const userStore = useUserStore()
const router = useRouter()
const currentUser = computed(() => userStore.user)

// 详情对话框相关状态
const detailDialogVisible = ref(false)
const currentReimbursement = ref(null)

// 懒加载组件
const ReimbursementDetail = defineAsyncComponent(() => import('./ReimbursementDetail.vue'))

// 打开详情
const openDetail = async (row) => {
  try {
    console.log('openDetail called with row:', row)
    detailDialogVisible.value = true

    // 如果传入的是完整的报销对象（包含items），直接使用
    if (row.items && Array.isArray(row.items)) {
      console.log('Using row data directly (has items):', row.items.length)
      currentReimbursement.value = row
    } else {
      console.log('Fetching data from API for ID:', row.id)
      // 否则通过ID获取完整的报销详情
      const response = await api.get(`/api/oa/reimbursement/${row.id}`)
      console.log('API response:', response.data)
      currentReimbursement.value = response.data.data || response.data
    }
    console.log('Final currentReimbursement:', currentReimbursement.value)
  } catch (error) {
    console.error('获取报销详情失败:', error)
    ElMessage.error('获取报销详情失败')
    detailDialogVisible.value = false
  }
}

// 关闭详情
const closeDetail = () => {
  detailDialogVisible.value = false
  currentReimbursement.value = null
}

// Tab控制
const activeTab = ref('apply')

// 报销申请表单
const reimbursementForm = reactive({
  id: null,
  title: '',
  projectId: null,
  items: [],
  attachments: []
})

// 表单验证规则
const formRules = {
  title: [
    { required: true, message: '请输入报销标题', trigger: 'blur' },
    { min: 2, max: 100, message: '报销标题长度在2到100个字符', trigger: 'blur' }
  ]
}

const reimbursementFormRef = ref(null)
const reimbursementList = ref([])
const projectList = ref([])
const statisticsData = ref([])
const statisticsSummary = ref({})
const statisticsDateRange = ref([])
const fileList = ref([])
const availableBudgets = ref([])
const budgetItems = ref([])

// 筛选相关变量
const listFilters = reactive({
  status: null,
  dateRange: null,
  keyword: ''
})
const listLoading = ref(false)
const statusOptions = ref([])

// 计算总金额
const totalAmount = computed(() => {
  return reimbursementForm.items.reduce((sum, item) => sum + (item.amount || 0), 0)
})

// 原有的弹窗相关变量
const formDialogVisible = ref(false)
const isEdit = ref(false)

const initialFormData = () => ({
  id: null,
  title: '',
  projectId: null,
  items: [],
  attachments: []
})
const formData = ref(initialFormData())

// Fetch data - 使用统一的API调用
const fetchProjects = async () => {
  if (!currentUser.value || !currentUser.value.id) {
    console.log('用户信息不存在，跳过项目列表加载')
    projectList.value = [] // 设置为空数组
    return
  }
  try {
    const response = await api.get(`/api/projects/user/${currentUser.value.id}`)
    // API 返回格式: { code: 200, message: "操作成功", data: [...] }
    projectList.value = response.data.data || response.data || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
    // 不显示错误消息，因为项目列表不是必需的
    projectList.value = [] // Ensure it's an empty array on error
  }
}

// 获取可用预算
const fetchAvailableBudgets = async (projectId) => {
  if (!projectId) {
    availableBudgets.value = []
    budgetItems.value = []
    return
  }

  try {
    const response = await api.get(`/api/budgets/project/${projectId}/available-budgets`)
    availableBudgets.value = response.data.data || response.data || []

    // 获取预算明细
    const itemsResponse = await api.get(`/api/budgets/project/${projectId}/budget-items`)
    budgetItems.value = itemsResponse.data.data || itemsResponse.data || []
  } catch (error) {
    console.error('获取预算信息失败:', error)
    availableBudgets.value = []
    budgetItems.value = []
  }
}

// 监听项目变化
watch(
  () => reimbursementForm.projectId,
  (newProjectId) => {
    if (newProjectId) {
      fetchAvailableBudgets(newProjectId)
    } else {
      availableBudgets.value = []
      budgetItems.value = []
      // 清空所有明细项的预算选择
      reimbursementForm.items.forEach((item) => {
        item.budgetId = null
        item.budgetItemId = null
      })
    }
  }
)

onMounted(async () => {
  // 首先尝试获取用户信息，但不阻塞其他功能
  if (!userStore.user) {
    try {
      await userStore.fetchUser()
      console.log('用户信息获取成功:', userStore.user)
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 不显示错误消息，因为路由守卫已经处理了认证
    }
  }

  // 初始化状态选项
  initializeStatusOptions()

  // 继续加载其他数据
  fetchReimbursementList()
  fetchProjects()

  // 从全局配置获取默认月份范围
  if (APP_CONFIG && APP_CONFIG.DEFAULT_DATE_RANGE) {
    statisticsDateRange.value = APP_CONFIG.DEFAULT_DATE_RANGE.getMonthRange()
  }
  fetchStatisticsData()
})

// Form Dialog Logic
const openFormDialog = (row) => {
  isEdit.value = !!row
  if (row) {
    formData.value = JSON.parse(JSON.stringify(row))
    // Ensure projectId is set, default to null if not present in row
    formData.value.projectId = row.projectId || null
    fileList.value = (formData.value.attachments || []).map((path) => {
      // Assuming path is a string URL, if it's an object, adjust accordingly
      const name = typeof path === 'string' ? path.substring(path.lastIndexOf('/') + 1) : 'attachment'
      return { name: name, url: path }
    })
  } else {
    formData.value = initialFormData()
    fileList.value = []
  }
  formDialogVisible.value = true
}

// 原有的弹窗相关函数（保留用于弹窗）
const addItemForDialog = () => {
  formData.value.items.push({ expenseDate: '', itemCategory: '', description: '', amount: 0 })
}

const removeItemForDialog = (index) => {
  formData.value.items.splice(index, 1)
}

const handleUploadSuccessForDialog = (response, file) => {
  formData.value.attachments.push(response.data?.url || response.url)
  fileList.value.push({ name: file.name, url: response.data?.url || response.url })
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      await api.put(`/api/oa/reimbursement/${formData.value.id}`, formData.value)
      ElMessage.success('更新成功')
    } else {
      await api.post('/api/oa/reimbursement', formData.value)
      ElMessage.success('创建成功')
    }
    formDialogVisible.value = false
    fetchReimbursementList()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await api.delete(`/api/oa/reimbursement/${id}`)
      ElMessage.success('删除成功')
      fetchReimbursementList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  })
}

// Navigation to Approval Management
const goToApprovalManagement = (row) => {
  router.push({
    path: '/approvals',
    query: {
      type: 'REIMBURSEMENT',
      requestId: row.id
    }
  })
}

// Helpers
const canViewApproval = (row) => {
  return (
    row.status === 'PENDING_MANAGER_APPROVAL' ||
    row.status === 'PENDING_FINANCE_APPROVAL' ||
    row.status === 'APPROVED' ||
    row.status === 'REJECTED'
  )
}

// 状态和格式化函数现在从统一的格式化工具导入

// 初始化状态选项
const initializeStatusOptions = () => {
  statusOptions.value = [
    { label: '草稿', value: 'DRAFT' },
    { label: '待项目经理审批', value: 'PENDING_MANAGER_APPROVAL' },
    { label: '待财务审批', value: 'PENDING_FINANCE_APPROVAL' },
    { label: '已通过', value: 'APPROVED' },
    { label: '已驳回', value: 'REJECTED' }
  ]
}

// 日期范围变化处理
const onDateRangeChange = (dateRange) => {
  listFilters.dateRange = dateRange
  fetchReimbursementList()
}

// 筛选功能的列表获取函数
const fetchReimbursementList = async () => {
  listLoading.value = true
  try {
    const params = {}

    // 添加筛选参数
    if (listFilters.status) {
      params.status = listFilters.status
    }
    if (listFilters.dateRange && listFilters.dateRange.length === 2) {
      params.startDate = listFilters.dateRange[0]
      params.endDate = listFilters.dateRange[1]
    }
    if (listFilters.keyword && listFilters.keyword.trim()) {
      params.keyword = listFilters.keyword.trim()
    }

    const response = await api.get('/api/oa/reimbursement', { params })
    // API 返回格式: { code: 200, message: "操作成功", data: Page<ReimbursementRequest> }
    // Page 格式: { content: [...], totalElements: 10, ... }
    const pageData = response.data.data || response.data
    reimbursementList.value = pageData.content || pageData
  } catch (error) {
    console.error('获取报销列表失败:', error)
    ElMessage.error('获取报销列表失败: ' + (error.response?.data?.message || error.message))
  } finally {
    listLoading.value = false
  }
}

// 重置筛选条件
const resetFilters = () => {
  Object.assign(listFilters, {
    status: null,
    dateRange: null,
    keyword: ''
  })
  fetchReimbursementList()
}

const fetchStatisticsData = async () => {
  if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
    console.log('日期范围未设置，跳过统计数据获取')
    return
  }

  try {
    // 添加详细的调试信息
    console.log('=== 开始调试日期格式问题 ===')
    console.log('statisticsDateRange.value:', statisticsDateRange.value)
    console.log('statisticsDateRange.value 类型:', typeof statisticsDateRange.value)
    console.log('第一个元素:', statisticsDateRange.value[0], '类型:', typeof statisticsDateRange.value[0])
    console.log('第二个元素:', statisticsDateRange.value[1], '类型:', typeof statisticsDateRange.value[1])

    // 将月份范围转换为日期范围
    let startMonth = statisticsDateRange.value[0] // 可能是 Date 对象或字符串
    let endMonth = statisticsDateRange.value[1] // 可能是 Date 对象或字符串

    console.log('原始 startMonth:', startMonth, 'instanceof Date:', startMonth instanceof Date)
    console.log('原始 endMonth:', endMonth, 'instanceof Date:', endMonth instanceof Date)

    // 确保转换为 YYYY-MM 格式的字符串
    if (startMonth instanceof Date) {
      startMonth = startMonth.getFullYear() + '-' + String(startMonth.getMonth() + 1).padStart(2, '0')
    }
    if (endMonth instanceof Date) {
      endMonth = endMonth.getFullYear() + '-' + String(endMonth.getMonth() + 1).padStart(2, '0')
    }

    console.log('转换后 startMonth:', startMonth)
    console.log('转换后 endMonth:', endMonth)

    // 开始日期: 月份的第一天
    const startDate = `${startMonth}-01`

    // 结束日期: 月份的最后一天
    const [endYear, endMonthNum] = endMonth.split('-')
    const endDate = new Date(parseInt(endYear), parseInt(endMonthNum), 0).getDate()
    const endDateString = `${endMonth}-${String(endDate).padStart(2, '0')}`

    console.log('最终 startDate:', startDate)
    console.log('最终 endDateString:', endDateString)
    console.log('准备发送的参数:', { startDate, endDate: endDateString })
    console.log('=== 调试信息结束 ===')

    const response = await api.get('/api/oa/reimbursement/statistics', {
      params: {
        startDate: startDate,
        endDate: endDateString
      }
    })
    // API 返回格式: { code: 200, message: "操作成功", data: { details: [...], summary: {...} } }
    const statisticsResponse = response.data.data || response.data
    statisticsData.value = statisticsResponse.details || []
    statisticsSummary.value = statisticsResponse.summary || {}
    renderChart()
  } catch (error) {
    console.error('获取统计数据失败:', error)
    console.error('错误响应:', error.response)
    // 显示详细错误以便调试
    ElMessage.error('获取统计数据失败: ' + (error.response?.data?.message || error.message))
    statisticsData.value = []
    statisticsSummary.value = {}
  }
}

const renderChart = () => {
  const chartDom = document.getElementById('reimbursementStatisticsChart')
  if (!chartDom) return

  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '报销费用分布',
        type: 'pie',
        radius: '50%',
        data: statisticsData.value.map((item) => ({
          value: item.totalAmount,
          name: item.category
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  myChart.setOption(option)
}

// 保存草稿
const saveDraftReimbursement = async () => {
  // 表单验证
  if (!reimbursementFormRef.value) return

  try {
    await reimbursementFormRef.value.validate()

    // 检查是否有费用明细
    if (!reimbursementForm.items.length) {
      ElMessage.error('请至少添加一项费用明细')
      return
    }

    // 准备提交数据
    const submitData = {
      title: reimbursementForm.title,
      projectId: reimbursementForm.projectId,
      items: reimbursementForm.items,
      attachments: reimbursementForm.attachments
    }

    let response
    if (reimbursementForm.id) {
      // 更新草稿
      response = await api.put(`/api/oa/reimbursement/${reimbursementForm.id}`, submitData)
      ElMessage.success('报销草稿保存成功')
    } else {
      // 创建草稿
      response = await api.post('/api/oa/reimbursement', submitData)
      ElMessage.success('报销草稿创建成功')
      reimbursementForm.id = response.data.data.id
    }

    // 刷新列表
    fetchReimbursementList()
  } catch (error) {
    if (error.name === 'ValidationError') {
      ElMessage.error('请正确填写表单信息')
    } else {
      ElMessage.error('保存失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

// 提交审批
const submitReimbursement = async () => {
  try {
    // 首先保存草稿（如果需要）
    if (!reimbursementForm.id) {
      await saveDraftReimbursement()
      if (!reimbursementForm.id) {
        return // 如果保存草稿失败，不继续提交
      }
    }

    // 预算验证
    const budgetValid = await validateReimbursementBudget()
    if (!budgetValid) {
      ElMessage.error('预算不足，无法提交审批')
      return
    }

    // 提交审批
    await api.post(`/api/oa/reimbursement/${reimbursementForm.id}/submit`)
    ElMessage.success('报销申请已提交审批')

    // 重置表单
    resetForm()

    // 刷新列表
    fetchReimbursementList()

    // 切换到列表页面
    activeTab.value = 'list'
  } catch (error) {
    ElMessage.error('提交审批失败: ' + (error.response?.data?.message || error.message))
  }
}

const resetForm = () => {
  // 重置表单数据
  Object.assign(reimbursementForm, {
    id: null,
    title: '',
    projectId: null,
    items: [],
    attachments: []
  })

  // 重置文件列表
  fileList.value = []

  // 重置表单验证
  if (reimbursementFormRef.value) {
    reimbursementFormRef.value.resetFields()
  }
}

// 添加明细项
const addReimbursementItem = () => {
  reimbursementForm.items.push({
    expenseDate: '',
    itemCategory: '',
    description: '',
    amount: 0,
    budgetId: null,
    budgetItemId: null
  })
}

// 移除明细项
const removeReimbursementItem = (index) => {
  reimbursementForm.items.splice(index, 1)
}

// 编辑报销申请
const editReimbursement = (row) => {
  // 将数据复制到申请表单
  Object.assign(reimbursementForm, {
    id: row.id,
    title: row.title,
    projectId: row.projectId || row.project?.id,
    items: JSON.parse(JSON.stringify(row.items || [])),
    attachments: row.attachments || []
  })

  // 设置文件列表
  fileList.value = (row.attachments || []).map((path) => {
    const name = typeof path === 'string' ? path.substring(path.lastIndexOf('/') + 1) : 'attachment'
    return { name: name, url: path }
  })

  // 切换到申请页面
  activeTab.value = 'apply'
}

// 删除报销申请
const deleteReimbursement = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个报销申请吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await api.delete(`/api/oa/reimbursement/${row.id}`)
    ElMessage.success('删除成功')
    fetchReimbursementList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// 处理文件上传成功
const handleReimbursementUploadSuccess = (response, file) => {
  reimbursementForm.attachments.push(response.data?.url || response.url)
  fileList.value.push({ name: file.name, url: response.data?.url || response.url })
}

// 修正原有函数中的一些问题
const addItem = () => {
  if (activeTab.value === 'apply') {
    addReimbursementItem()
  } else {
    // 为原有的弹窗添加项目
    formData.value.items.push({ expenseDate: '', itemCategory: '', description: '', amount: 0 })
  }
}

const removeItem = (index) => {
  if (activeTab.value === 'apply') {
    removeReimbursementItem(index)
  } else {
    // 为原有的弹窗移除项目
    formData.value.items.splice(index, 1)
  }
}

const handleUploadSuccess = (response, file) => {
  if (activeTab.value === 'apply') {
    handleReimbursementUploadSuccess(response, file)
  } else {
    // 原有的弹窗上传处理
    formData.value.attachments.push(response.data?.url || response.url)
    fileList.value.push({ name: file.name, url: response.data?.url || response.url })
  }
}

// 预算相关方法
const getBudgetItems = (budgetId) => {
  if (!budgetId) return []
  return budgetItems.value.filter((item) => item.budget && item.budget.id === budgetId)
}

const onBudgetChange = (row, index) => {
  // 清空预算明细选择
  row.budgetItemId = null
  // 可以在这里添加预算可用性检查
  validateBudgetAmount(row)
}

const validateBudgetAmount = async (row) => {
  if (!row.amount || row.amount <= 0) return

  if (row.budgetId) {
    const budget = availableBudgets.value.find((b) => b.id === row.budgetId)
    if (budget && row.amount > budget.remainingAmount) {
      ElMessage.warning(`金额超过预算余额 ¥${budget.remainingAmount}`)
    }
  }

  if (row.budgetItemId) {
    const budgetItem = budgetItems.value.find((item) => item.id === row.budgetItemId)
    if (budgetItem && row.amount > budgetItem.remainingAmount) {
      ElMessage.warning(`金额超过预算明细余额 ¥${budgetItem.remainingAmount}`)
    }
  }
}

// 验证整个报销申请的预算
const validateReimbursementBudget = async () => {
  if (!reimbursementForm.projectId || !reimbursementForm.items.length) {
    return true
  }

  try {
    const response = await api.post('/api/oa/reimbursement/validate-budget', reimbursementForm)
    return response.data.data
  } catch (error) {
    console.error('预算验证失败:', error)
    return false
  }
}

// 从列表中提交审批
const submitReimbursementForApproval = async (row) => {
  try {
    await ElMessageBox.confirm('确定要提交这个报销申请到审批流程吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await api.post(`/api/oa/reimbursement/${row.id}/submit`)
    ElMessage.success('报销申请已提交审批')
    fetchReimbursementList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提交审批失败: ' + (error.response?.data?.message || error.message))
    }
  }
}
</script>

<style scoped>
.reimbursement-container {
  padding: 20px;
  max-width: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
  font-weight: 600;
}

.tab-content {
  padding: 20px;
}

.form-section,
.operation-section,
.table-section,
.statistics-section,
.summary-section,
.chart-section {
  margin-bottom: 24px;
}

.section-header {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0f2f5;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.form-content {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.table-actions {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-start;
}

.upload-section {
  margin-bottom: 24px;
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-start;
  gap: 12px;
}

.filter-container {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.table-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}

.table-section .el-table {
  border: none;
}

.statistics-section,
.summary-section,
.chart-section {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.chart-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin: 0;
}

.summary-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.summary-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.card-content {
  padding: 20px;
  text-align: center;
}

.card-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
  font-weight: 500;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
  line-height: 1.2;
}

/* 通用样式优化 */
.el-table {
  --el-table-border-color: #e4e7ed;
  --el-table-header-bg-color: #f5f7fa;
}

.el-table th {
  background-color: var(--el-table-header-bg-color);
  font-weight: 600;
  color: #303133;
}

.el-form-item {
  margin-bottom: 18px;
}

.el-divider {
  margin: 20px 0;
}

.el-divider__text {
  font-weight: 600;
  color: #303133;
  background-color: #fafafa;
  padding: 0 16px;
}

/* 响应式布局优化 */
@media (max-width: 768px) {
  .tab-content {
    padding: 16px;
  }

  .summary-cards {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .form-actions .el-button {
    margin-bottom: 8px;
  }
}

/* 交互效果增强 */
.form-section:hover,
.operation-section:hover,
.table-section:hover,
.statistics-section:hover,
.summary-section:hover,
.chart-section:hover {
  border-color: #c6e2ff;
  transition: border-color 0.3s ease;
}

/* 审批对话框样式 */
.approval-content {
  max-height: 600px;
  overflow-y: auto;
}

.approval-section {
  margin-bottom: 24px;
}

.approval-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}

.amount-highlight {
  font-size: 18px;
  font-weight: 600;
  color: #e6a23c;
}

.approval-section .el-descriptions {
  margin-bottom: 16px;
}

.approval-section .el-table {
  margin-bottom: 16px;
}
</style>
