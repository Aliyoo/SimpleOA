<template>
  <div class="budget-detail-container">
    <div class="detail-header">
      <div class="header-left">
        <el-button @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>{{ budget.name || '预算详情' }}</h2>
      </div>
      <div class="header-right">
        <el-button @click="handleEdit">编辑预算</el-button>
        <el-button type="danger" @click="handleDelete">删除预算</el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="budget.id" class="detail-content">
      <!-- 基本信息 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag :type="getBudgetStatusType(budget.status)">
              {{ getBudgetStatusText(budget.status) }}
            </el-tag>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="8">
            <div class="info-item">
              <label>预算名称：</label>
              <span>{{ budget.name }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <label>所属项目：</label>
              <span>{{ budget.project?.name || 'N/A' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <label>创建人：</label>
              <span>{{ budget.createdBy?.name || 'N/A' }}</span>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <div class="info-item">
              <label>开始日期：</label>
              <span>{{ formatDate(budget.startDate) }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <label>结束日期：</label>
              <span>{{ formatDate(budget.endDate) }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <label>创建时间：</label>
              <span>{{ formatDateTime(budget.createTime) }}</span>
            </div>
          </el-col>
        </el-row>

        <el-row v-if="budget.description">
          <el-col :span="24">
            <div class="info-item">
              <label>预算描述：</label>
              <p class="description">{{ budget.description }}</p>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 预算统计 -->
      <el-card class="stats-card">
        <template #header>
          <span>预算统计</span>
        </template>

        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ formatCurrency(budget.totalAmount) }}</div>
              <div class="stat-label">总预算</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ formatCurrency(budget.usedAmount || 0) }}</div>
              <div class="stat-label">已使用</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ formatCurrency(budget.remainingAmount || budget.totalAmount) }}</div>
              <div class="stat-label">剩余金额</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ calculateUsagePercentage(budget) }}%</div>
              <div class="stat-label">使用率</div>
            </div>
          </el-col>
        </el-row>

        <div class="usage-progress">
          <el-progress
            :percentage="calculateUsagePercentage(budget)"
            :status="getProgressStatus(budget)"
            :stroke-width="20"
          />
        </div>
      </el-card>

      <!-- 预算项目 -->
      <el-card class="items-card">
        <template #header>
          <div class="card-header">
            <span>预算项目</span>
            <el-button type="primary" size="small" @click="handleAddItem">
              添加项目
            </el-button>
          </div>
        </template>

        <el-table :data="budgetItems" style="width: 100%">
          <el-table-column prop="name" label="项目名称" width="180" />
          <el-table-column prop="category" label="类别" width="120" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="scope">
              {{ formatCurrency(scope.row.amount) }}
            </template>
          </el-table-column>
          <el-table-column prop="usedAmount" label="已使用" width="120">
            <template #default="scope">
              {{ formatCurrency(scope.row.usedAmount || 0) }}
            </template>
          </el-table-column>
          <el-table-column prop="remainingAmount" label="剩余" width="120">
            <template #default="scope">
              {{ formatCurrency(scope.row.remainingAmount || scope.row.amount) }}
            </template>
          </el-table-column>
          <el-table-column label="使用率" width="120">
            <template #default="scope">
              <el-progress
                :percentage="calculateItemUsagePercentage(scope.row)"
                :status="getItemProgressStatus(scope.row)"
                :show-text="false"
              />
              <div class="progress-text">{{ calculateItemUsagePercentage(scope.row) }}%</div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getItemStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="handleItemDetail(scope.row)">详情</el-button>
              <el-button size="small" type="primary" @click="handleEditItem(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteItem(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 支出记录 -->
      <el-card class="expenses-card">
        <template #header>
          <div class="card-header">
            <span>支出记录</span>
            <el-button type="primary" size="small" @click="handleAddExpense">
              添加支出
            </el-button>
          </div>
        </template>

        <el-table :data="budgetExpenses" style="width: 100%">
          <el-table-column prop="expenseDate" label="支出日期" width="120">
            <template #default="scope">
              {{ formatDate(scope.row.expenseDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="expenseType" label="支出类型" width="120" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="scope">
              {{ formatCurrency(scope.row.amount) }}
            </template>
          </el-table-column>
          <el-table-column prop="budgetItem.name" label="对应项目" width="150" />
          <el-table-column prop="referenceNumber" label="参考编号" width="150" />
          <el-table-column prop="recordedBy.name" label="记录人" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getExpenseStatusType(scope.row.status)">
                {{ scope.row.status || '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="handleExpenseDetail(scope.row)">详情</el-button>
              <el-button size="small" type="primary" @click="handleEditExpense(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteExpense(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 预警记录 -->
      <el-card v-if="budgetAlerts.length > 0" class="alerts-card">
        <template #header>
          <span>预警记录</span>
        </template>

        <el-table :data="budgetAlerts" style="width: 100%">
          <el-table-column prop="alertDate" label="预警时间" width="160">
            <template #default="scope">
              {{ formatDateTime(scope.row.alertDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="alertType" label="预警类型" width="120" />
          <el-table-column prop="alertLevel" label="预警级别" width="100">
            <template #default="scope">
              <el-tag :type="getAlertLevelType(scope.row.alertLevel)">
                {{ scope.row.alertLevel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="预警信息" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getAlertStatusType(scope.row.status)">
                {{ scope.row.status || '未处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                v-if="scope.row.status !== '已解决'"
                size="small"
                type="success"
                @click="handleResolveAlert(scope.row)"
              >
                标记解决
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <div v-else class="no-data">
      <el-empty description="预算不存在或已被删除" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 响应式数据
const loading = ref(true)
const budget = ref({})
const budgetItems = ref([])
const budgetExpenses = ref([])
const budgetAlerts = ref([])

// 计算属性
const budgetId = computed(() => route.params.id)

// 方法
const loadBudgetDetail = async () => {
  loading.value = true
  try {
    // 并行加载所有相关数据
    const [
      budgetResponse,
      itemsResponse,
      expensesResponse,
      alertsResponse
    ] = await Promise.all([
      axios.get(`/api/budgets/${budgetId.value}`),
      axios.get(`/api/budgets/${budgetId.value}/items`),
      axios.get(`/api/budgets/${budgetId.value}/expenses`),
      axios.get(`/api/budgets/${budgetId.value}/alerts`)
    ])

    budget.value = budgetResponse.data
    budgetItems.value = itemsResponse.data
    budgetExpenses.value = expensesResponse.data
    budgetAlerts.value = alertsResponse.data
  } catch (error) {
    ElMessage.error('加载预算详情失败')
    console.error('Error loading budget detail:', error)
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/budget-management')
}

const handleEdit = () => {
  router.push(`/budget-form/${budgetId.value}`)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除预算 "${budget.value.name}" 吗？删除后无法恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await axios.delete(`/api/budgets/${budgetId.value}`)
    ElMessage.success('预算删除成功')
    router.push('/budget-management')
  } catch (error) {
    if (error === 'cancel') return
    
    ElMessage.error('删除预算失败')
    console.error('Error deleting budget:', error)
  }
}

const handleAddItem = () => {
  // TODO: 跳转到添加预算项目页面
  ElMessage.info('添加项目功能待实现')
}

const handleItemDetail = (item) => {
  // TODO: 显示项目详情
  ElMessage.info('项目详情功能待实现')
}

const handleEditItem = (item) => {
  // TODO: 编辑预算项目
  ElMessage.info('编辑项目功能待实现')
}

const handleDeleteItem = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除预算项目 "${item.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await axios.delete(`/api/budgets/items/${item.id}`)
    ElMessage.success('删除成功')
    
    // 重新加载数据
    await loadBudgetDetail()
  } catch (error) {
    if (error === 'cancel') return
    
    ElMessage.error('删除失败')
    console.error('Error deleting budget item:', error)
  }
}

const handleAddExpense = () => {
  // TODO: 添加支出记录
  ElMessage.info('添加支出功能待实现')
}

const handleExpenseDetail = (expense) => {
  // TODO: 支出详情
  ElMessage.info('支出详情功能待实现')
}

const handleEditExpense = (expense) => {
  // TODO: 编辑支出
  ElMessage.info('编辑支出功能待实现')
}

const handleDeleteExpense = async (expense) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条支出记录吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await axios.delete(`/api/budgets/expenses/${expense.id}`)
    ElMessage.success('删除成功')
    
    // 重新加载数据
    await loadBudgetDetail()
  } catch (error) {
    if (error === 'cancel') return
    
    ElMessage.error('删除失败')
    console.error('Error deleting budget expense:', error)
  }
}

const handleResolveAlert = async (alert) => {
  try {
    await ElMessageBox.prompt(
      '请输入解决方案',
      '标记预警为已解决',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValidator: (value) => {
          if (!value) {
            return '请输入解决方案'
          }
          return true
        }
      }
    )

    // TODO: 获取当前用户ID
    const currentUserId = 1 // 临时硬编码，实际应从用户状态获取

    await axios.post(`/api/budgets/alerts/${alert.id}/resolve`, null, {
      params: {
        resolution: value,
        resolvedById: currentUserId
      }
    })
    
    ElMessage.success('预警已标记为解决')
    
    // 重新加载数据
    await loadBudgetDetail()
  } catch (error) {
    if (error === 'cancel') return
    
    ElMessage.error('操作失败')
    console.error('Error resolving alert:', error)
  }
}

// 工具方法
const formatDate = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return 'N/A'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const formatCurrency = (value) => {
  return value ? `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '¥0.00'
}

const calculateUsagePercentage = (budget) => {
  if (!budget.totalAmount || budget.totalAmount === 0) return 0
  const usedAmount = budget.usedAmount || 0
  return Math.round((usedAmount / budget.totalAmount) * 100)
}

const calculateItemUsagePercentage = (item) => {
  if (!item.amount || item.amount === 0) return 0
  const usedAmount = item.usedAmount || 0
  return Math.round((usedAmount / item.amount) * 100)
}

const getBudgetStatusType = (status) => {
  const statusMap = {
    '活跃': 'success',
    '暂停': 'warning',
    '已完成': 'info',
    '已取消': 'danger'
  }
  return statusMap[status] || 'default'
}

const getBudgetStatusText = (status) => {
  return status || '未知状态'
}

const getProgressStatus = (budget) => {
  const percentage = calculateUsagePercentage(budget)
  if (percentage >= 90) return 'exception'
  if (percentage >= 75) return 'warning'
  return 'success'
}

const getItemProgressStatus = (item) => {
  const percentage = calculateItemUsagePercentage(item)
  if (percentage >= 90) return 'exception'
  if (percentage >= 75) return 'warning'
  return 'success'
}

const getItemStatusType = (status) => {
  const statusMap = {
    '活跃': 'success',
    '暂停': 'warning',
    '已完成': 'info'
  }
  return statusMap[status] || 'default'
}

const getExpenseStatusType = (status) => {
  const statusMap = {
    '正常': 'success',
    '待审核': 'warning',
    '已驳回': 'danger'
  }
  return statusMap[status] || 'success'
}

const getAlertLevelType = (level) => {
  const levelMap = {
    '低': 'info',
    '中': 'warning',
    '高': 'danger'
  }
  return levelMap[level] || 'default'
}

const getAlertStatusType = (status) => {
  const statusMap = {
    '未处理': 'warning',
    '处理中': 'primary',
    '已解决': 'success'
  }
  return statusMap[status] || 'warning'
}

// 生命周期
onMounted(() => {
  loadBudgetDetail()
})
</script>

<style scoped>
.budget-detail-container {
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h2 {
  margin: 0;
  color: #333;
}

.header-right {
  display: flex;
  gap: 10px;
}

.loading-container {
  margin: 20px 0;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-card .info-item {
  margin-bottom: 15px;
}

.info-item label {
  font-weight: 600;
  color: #666;
  margin-right: 10px;
}

.description {
  margin: 10px 0;
  line-height: 1.6;
  color: #666;
}

.stats-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stats-card :deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.stats-card :deep(.el-card__body) {
  background: transparent;
}

.stat-item {
  text-align: center;
  padding: 20px 0;
}

.stat-value {
  font-size: 2em;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  opacity: 0.8;
  font-size: 0.9em;
}

.usage-progress {
  margin-top: 20px;
}

.progress-text {
  text-align: center;
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.no-data {
  padding: 50px 0;
  text-align: center;
}
</style>