<template>
  <div class="integration-container">
    <h1>模块集成</h1>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="系统仪表盘" name="dashboard">
        <div class="dashboard-container">
          <el-row :gutter="20">
            <el-col v-for="(card, index) in dashboardCards" :key="index" :span="6">
              <el-card class="dashboard-card">
                <template #header>
                  <div class="card-header">
                    <span>{{ card.title }}</span>
                  </div>
                </template>
                <div class="dashboard-value">{{ card.value }}</div>
                <div class="dashboard-trend">
                  <span :class="card.trend === 'up' ? 'trend-up' : 'trend-down'">
                    <i :class="card.trend === 'up' ? 'el-icon-top' : 'el-icon-bottom'"></i>
                    {{ card.trendValue }}
                  </span>
                  <span class="trend-period">{{ card.trendPeriod }}</span>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <div class="charts-container">
            <div class="chart-wrapper">
              <h3>项目状态分布</h3>
              <!-- 这里可以使用 ECharts 等图表库展示项目状态分布 -->
              <div class="chart-placeholder">项目状态分布图表</div>
            </div>
            <div class="chart-wrapper">
              <h3>预算使用趋势</h3>
              <!-- 这里可以使用 ECharts 等图表库展示预算使用趋势 -->
              <div class="chart-placeholder">预算使用趋势图表</div>
            </div>
          </div>

          <div class="recent-activities">
            <h3>最近活动</h3>
            <el-timeline>
              <el-timeline-item
                v-for="(activity, index) in recentActivities"
                :key="index"
                :timestamp="activity.time"
                :type="getActivityType(activity.type)"
              >
                {{ activity.content }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="项目预算汇总" name="projectBudget">
        <div class="project-budget-container">
          <div class="operation-bar">
            <el-select v-model="selectedProject" placeholder="选择项目" style="width: 300px">
              <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
            </el-select>
            <el-button type="primary" style="margin-left: 10px" @click="fetchProjectBudgetSummary"> 查询 </el-button>
          </div>

          <div v-if="budgetSummary" class="budget-summary">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目名称">{{ budgetSummary.project.name }}</el-descriptions-item>
              <el-descriptions-item label="项目状态">
                <el-tag :type="getProjectStatusType(budgetSummary.project.status)">
                  {{ getProjectStatusText(budgetSummary.project.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="总预算">{{
                formatCurrency(budgetSummary.totalBudget)
              }}</el-descriptions-item>
              <el-descriptions-item label="总支出">{{
                formatCurrency(budgetSummary.totalExpenses)
              }}</el-descriptions-item>
              <el-descriptions-item label="剩余预算">{{
                formatCurrency(budgetSummary.remainingBudget)
              }}</el-descriptions-item>
              <el-descriptions-item label="使用率"
                >{{ budgetSummary.utilizationRate.toFixed(2) }}%</el-descriptions-item
              >
            </el-descriptions>

            <el-tabs style="margin-top: 20px">
              <el-tab-pane label="预算列表">
                <el-table :data="budgetSummary.budgets" style="width: 100%">
                  <el-table-column prop="name" label="预算名称" width="180" />
                  <el-table-column prop="totalAmount" label="总预算" width="120">
                    <template #default="scope">
                      {{ formatCurrency(scope.row.totalAmount) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="usedAmount" label="已使用" width="120">
                    <template #default="scope">
                      {{ formatCurrency(scope.row.usedAmount) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="使用率" width="180">
                    <template #default="scope">
                      <el-progress
                        :percentage="calculateUsagePercentage(scope.row)"
                        :status="getProgressStatus(scope.row)"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="描述" />
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="支出列表">
                <el-table :data="budgetSummary.expenses" style="width: 100%">
                  <el-table-column prop="description" label="支出描述" width="180" />
                  <el-table-column prop="amount" label="支出金额" width="120">
                    <template #default="scope">
                      {{ formatCurrency(scope.row.amount) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="expenseDate" label="支出日期" width="120" />
                  <el-table-column prop="paymentMethod" label="支付方式" width="120" />
                  <el-table-column prop="approvalStatus" label="审批状态" width="100">
                    <template #default="scope">
                      <el-tag :type="getApprovalStatusType(scope.row.approvalStatus)">
                        {{ getApprovalStatusText(scope.row.approvalStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="createTime" label="创建时间" width="180" />
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="预警列表">
                <el-table :data="budgetSummary.alerts" style="width: 100%">
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
                        scope.row.alertType === 'PERCENTAGE'
                          ? scope.row.threshold + '%'
                          : formatCurrency(scope.row.threshold)
                      }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="message" label="预警消息" />
                  <el-table-column prop="status" label="状态" width="100">
                    <template #default="scope">
                      <el-tag :type="scope.row.status === 'ACTIVE' ? 'danger' : 'info'">
                        {{ scope.row.status === 'ACTIVE' ? '已触发' : '未触发' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="createTime" label="创建时间" width="180" />
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>

          <div v-else-if="selectedProject" class="loading-container">
            <el-empty description="正在加载数据..."></el-empty>
          </div>

          <div v-else class="no-project-selected">
            <el-empty description="请选择一个项目"></el-empty>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="项目绩效报表" name="projectPerformance">
        <div class="project-performance-container">
          <div class="operation-bar">
            <el-select v-model="selectedProjectForPerformance" placeholder="选择项目" style="width: 300px">
              <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
            </el-select>
            <el-date-picker
              v-model="performanceDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="margin-left: 10px; width: 350px"
            />
            <el-button type="primary" style="margin-left: 10px" @click="fetchProjectPerformanceReport">
              查询
            </el-button>
          </div>

          <div v-if="performanceReport" class="performance-report">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目名称">{{ performanceReport.project.name }}</el-descriptions-item>
              <el-descriptions-item label="项目状态">
                <el-tag :type="getProjectStatusType(performanceReport.project.status)">
                  {{ getProjectStatusText(performanceReport.project.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="平均绩效分">{{
                performanceReport.averagePerformanceScore.toFixed(2)
              }}</el-descriptions-item>
              <el-descriptions-item label="评估数量">{{
                performanceReport.performanceEvaluations.length
              }}</el-descriptions-item>
            </el-descriptions>

            <el-tabs style="margin-top: 20px">
              <el-tab-pane label="工时统计">
                <el-table :data="performanceReport.worktimeStats" style="width: 100%">
                  <el-table-column prop="employeeName" label="员工姓名" width="120" />
                  <el-table-column prop="totalHours" label="总工时" width="100">
                    <template #default="scope"> {{ scope.row.totalHours }} 小时 </template>
                  </el-table-column>
                  <el-table-column prop="billableHours" label="可计费工时" width="120">
                    <template #default="scope"> {{ scope.row.billableHours }} 小时 </template>
                  </el-table-column>
                  <el-table-column prop="utilization" label="利用率" width="120">
                    <template #default="scope">
                      {{ ((scope.row.billableHours / scope.row.totalHours) * 100).toFixed(2) }}%
                    </template>
                  </el-table-column>
                  <el-table-column prop="taskCount" label="任务数量" width="100" />
                  <el-table-column prop="completedTaskCount" label="已完成任务" width="100" />
                  <el-table-column label="完成率" width="180">
                    <template #default="scope">
                      <el-progress
                        :percentage="((scope.row.completedTaskCount / scope.row.taskCount) * 100).toFixed(2)"
                        :status="getTaskCompletionStatus(scope.row)"
                      />
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="绩效评估">
                <el-table :data="performanceReport.performanceEvaluations" style="width: 100%">
                  <el-table-column prop="employeeName" label="员工姓名" width="120" />
                  <el-table-column prop="evaluationPeriod" label="评估周期" width="180" />
                  <el-table-column prop="overallScore" label="总分" width="100">
                    <template #default="scope">
                      <span :class="getScoreClass(scope.row.overallScore)">{{ scope.row.overallScore }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="rating" label="等级" width="100">
                    <template #default="scope">
                      <el-tag :type="getRatingTagType(scope.row.rating)">
                        {{ scope.row.rating }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" width="100">
                    <template #default="scope">
                      <el-tag :type="getEvaluationStatusType(scope.row.status)">
                        {{ getEvaluationStatusText(scope.row.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="evaluatorName" label="评估人" width="120" />
                  <el-table-column prop="createTime" label="创建时间" width="180" />
                </el-table>
              </el-tab-pane>
            </el-tabs>

            <div class="charts-container" style="margin-top: 30px">
              <div class="chart-wrapper">
                <h3>员工绩效分布</h3>
                <!-- 这里可以使用 ECharts 等图表库展示员工绩效分布 -->
                <div class="chart-placeholder">员工绩效分布图表</div>
              </div>
              <div class="chart-wrapper">
                <h3>工时利用率趋势</h3>
                <!-- 这里可以使用 ECharts 等图表库展示工时利用率趋势 -->
                <div class="chart-placeholder">工时利用率趋势图表</div>
              </div>
            </div>
          </div>

          <div v-else-if="selectedProjectForPerformance" class="loading-container">
            <el-empty description="正在加载数据..."></el-empty>
          </div>

          <div v-else class="no-project-selected">
            <el-empty description="请选择一个项目"></el-empty>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="报销与付款关联" name="reimbursementPayment">
        <div class="reimbursement-payment-container">
          <div class="operation-bar">
            <el-select v-model="selectedProjectForReimbursement" placeholder="选择项目" style="width: 300px">
              <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
            </el-select>
            <el-button type="primary" style="margin-left: 10px" @click="fetchReimbursementPaymentData">
              查询
            </el-button>
          </div>

          <div v-if="reimbursementPaymentData" class="data-container">
            <el-table :data="reimbursementPaymentData" style="width: 100%" border>
              <el-table-column prop="reimbursementId" label="报销单号" width="120" />
              <el-table-column prop="reimbursementAmount" label="报销金额" width="120" />
              <el-table-column prop="paymentId" label="付款单号" width="120" />
              <el-table-column prop="paymentAmount" label="付款金额" width="120" />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="getReimbursementStatusType(scope.row.status)">
                    {{ getReimbursementStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column label="操作" width="180">
                <template #default="scope">
                  <el-button size="small" @click="handleEditAssociation(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleDeleteAssociation(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-else-if="selectedProjectForReimbursement" class="loading-container">
            <el-empty description="正在加载数据..."></el-empty>
          </div>

          <div v-else class="no-project-selected">
            <el-empty description="请选择一个项目"></el-empty>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="审批流配置" name="approvalFlow">
        <div class="approval-flow-container">
          <div class="operation-bar">
            <el-select v-model="selectedProjectForApproval" placeholder="选择项目" style="width: 300px">
              <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
            </el-select>
            <el-button type="primary" style="margin-left: 10px" @click="fetchApprovalFlowData"> 查询 </el-button>
            <el-button type="success" style="margin-left: 10px" @click="handleCreateApprovalFlow">
              新建审批流
            </el-button>
          </div>

          <div v-if="approvalFlowData" class="data-container">
            <el-table :data="approvalFlowData" style="width: 100%" border>
              <el-table-column prop="flowName" label="审批流名称" width="180" />
              <el-table-column prop="flowType" label="审批类型" width="120" />
              <el-table-column prop="approvers" label="审批人" width="200">
                <template #default="scope">
                  <el-tag v-for="approver in scope.row.approvers" :key="approver.id" style="margin-right: 5px">
                    {{ approver.name }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="getApprovalFlowStatusType(scope.row.status)">
                    {{ getApprovalFlowStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="scope">
                  <el-button size="small" @click="handleEditApprovalFlow(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleDeleteApprovalFlow(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-else-if="selectedProjectForApproval" class="loading-container">
            <el-empty description="正在加载数据..."></el-empty>
          </div>

          <div v-else class="no-project-selected">
            <el-empty description="请选择一个项目"></el-empty>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const activeTab = ref('dashboard')
// Existing data properties
// ...

// New data properties for reimbursement/payment
const selectedProjectForReimbursement = ref(null)
const reimbursementPaymentData = ref(null)

// New data properties for approval flow
const selectedProjectForApproval = ref(null)
const approvalFlowData = ref(null)

// Existing methods
// ...

// New methods for reimbursement/payment
const fetchReimbursementPaymentData = () => {
  // TODO: Implement API call to fetch reimbursement/payment data
  console.log('Fetching reimbursement/payment data for project:', selectedProjectForReimbursement.value)
}

const handleEditAssociation = (row) => {
  // TODO: Implement edit functionality
  console.log('Editing association:', row)
}

const handleDeleteAssociation = (row) => {
  // TODO: Implement delete functionality
  console.log('Deleting association:', row)
}

const getReimbursementStatusType = (status) => {
  const statusMap = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger',
    paid: ''
  }
  return statusMap[status] || ''
}

const getReimbursementStatusText = (status) => {
  const statusTextMap = {
    pending: '待审批',
    approved: '已批准',
    rejected: '已拒绝',
    paid: '已付款'
  }
  return statusTextMap[status] || status
}

// New methods for approval flow
const fetchApprovalFlowData = () => {
  // TODO: Implement API call to fetch approval flow data
  console.log('Fetching approval flow data for project:', selectedProjectForApproval.value)
}

const handleCreateApprovalFlow = () => {
  // TODO: Implement create functionality
  console.log('Creating new approval flow')
}

const handleEditApprovalFlow = (row) => {
  // TODO: Implement edit functionality
  console.log('Editing approval flow:', row)
}

const handleDeleteApprovalFlow = (row) => {
  // TODO: Implement delete functionality
  console.log('Deleting approval flow:', row)
}

const getApprovalFlowStatusType = (status) => {
  const statusMap = {
    active: 'success',
    inactive: 'danger',
    draft: 'warning'
  }
  return statusMap[status] || ''
}

const getApprovalFlowStatusText = (status) => {
  const statusTextMap = {
    active: '启用',
    inactive: '禁用',
    draft: '草稿'
  }
  return statusTextMap[status] || status
}

// Project status methods
const getProjectStatusType = (status) => {
  const statusMap = {
    CANCELLED: 'danger',
    REQUIREMENT: 'info',
    DESIGN: 'warning',
    DEVELOPMENT: 'primary',
    ACCEPTANCE: 'warning',
    COMPLETED: 'success'
  }
  return statusMap[status] || 'info'
}

const getProjectStatusText = (status) => {
  const statusMap = {
    CANCELLED: '已取消',
    REQUIREMENT: '需求阶段',
    DESIGN: '设计阶段',
    DEVELOPMENT: '开发阶段',
    ACCEPTANCE: '终验阶段',
    COMPLETED: '已完成'
  }
  return statusMap[status] || status
}

// Additional helper methods that might be missing
const formatCurrency = (value) => {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value || 0)
}

const calculateUsagePercentage = (budget) => {
  if (!budget || budget.totalAmount === 0 || budget.totalAmount == null) return 0
  return Math.max(0, Math.min(100, Math.round(((budget.usedAmount || 0) / budget.totalAmount) * 100)))
}

const getProgressStatus = (budget) => {
  const percentage = calculateUsagePercentage(budget)
  if (percentage >= 90) return 'exception'
  if (percentage >= 75) return 'warning'
  return 'success'
}

const getApprovalStatusType = (status) => {
  const statusMap = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return statusMap[status] || 'info'
}

const getApprovalStatusText = (status) => {
  const statusMap = {
    pending: '待审批',
    approved: '已批准',
    rejected: '已拒绝'
  }
  return statusMap[status] || status
}

const getAlertTypeTag = (type) => {
  return type === 'PERCENTAGE' ? 'warning' : 'danger'
}

const getAlertTypeText = (type) => {
  return type === 'PERCENTAGE' ? '百分比预警' : '金额预警'
}

const getActivityType = (type) => {
  const typeMap = {
    info: 'primary',
    warning: 'warning',
    error: 'danger',
    success: 'success'
  }
  return typeMap[type] || 'primary'
}

const getTaskCompletionStatus = (row) => {
  const completionRate = (row.completedTaskCount / row.taskCount) * 100
  if (completionRate >= 90) return 'success'
  if (completionRate >= 75) return 'warning'
  return 'exception'
}

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 70) return 'score-average'
  return 'score-poor'
}

const getRatingTagType = (rating) => {
  const ratingMap = {
    '优秀': 'success',
    '良好': 'primary',
    '一般': 'warning',
    '较差': 'danger'
  }
  return ratingMap[rating] || 'info'
}

const getEvaluationStatusType = (status) => {
  const statusMap = {
    completed: 'success',
    pending: 'warning',
    draft: 'info'
  }
  return statusMap[status] || 'info'
}

const getEvaluationStatusText = (status) => {
  const statusMap = {
    completed: '已完成',
    pending: '待评估',
    draft: '草稿'
  }
  return statusMap[status] || status
}

// Data properties that might be missing
const dashboardCards = ref([
  { title: '项目总数', value: 25, trend: 'up', trendValue: '+5', trendPeriod: '较上月' },
  { title: '开发中项目', value: 12, trend: 'up', trendValue: '+3', trendPeriod: '较上月' },
  { title: '待审批', value: 8, trend: 'down', trendValue: '-2', trendPeriod: '较昨日' },
  { title: '预算使用率', value: '75%', trend: 'up', trendValue: '+5%', trendPeriod: '较上月' }
])

const recentActivities = ref([
  { time: '2024-01-15 10:30', type: 'info', content: '用户张三提交了新的项目申请' },
  { time: '2024-01-15 09:15', type: 'success', content: '项目ABC已完成开发阶段' },
  { time: '2024-01-15 08:45', type: 'warning', content: '项目XYZ预算使用率达到80%' },
  { time: '2024-01-14 17:20', type: 'info', content: '用户李四申请了年假' }
])

const projects = ref([])
const selectedProject = ref(null)
const budgetSummary = ref(null)
const selectedProjectForPerformance = ref(null)
const performanceDateRange = ref([])
const performanceReport = ref(null)

// Methods for fetching data
const fetchProjectBudgetSummary = () => {
  // TODO: Implement API call
  console.log('Fetching budget summary for project:', selectedProject.value)
}

const fetchProjectPerformanceReport = () => {
  // TODO: Implement API call
  console.log('Fetching performance report for project:', selectedProjectForPerformance.value)
}
</script>

<style scoped>
.integration-container {
  padding: 20px;
}

/* Existing styles */
/* ... */

/* New styles for reimbursement/payment */
.reimbursement-payment-container {
  padding: 15px;
}

/* New styles for approval flow */
.approval-flow-container {
  padding: 15px;
}

.operation-bar {
  margin-bottom: 20px;
}

.data-container {
  margin-top: 20px;
}

.loading-container,
.no-project-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}
</style>
