<template>
  <div class="time-management-container">
    <h1>我的工时</h1>

    <!-- 自定义 Tab 导航 -->
    <div class="custom-tabs">
      <div class="custom-tabs-header">
        <div
          v-for="tab in tabs"
          :key="tab.name"
          :class="['custom-tab', { 'active': activeTab === tab.name }]"
          @click="activeTab = tab.name"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- Tab 内容区域 -->
      <div class="custom-tabs-content">
        <!-- 工时填报 Tab -->
        <div v-show="activeTab === 'report'" class="tab-pane">
          <el-form :model="timeReportForm" label-width="100px" class="report-form">
            <el-form-item label="项目名称" prop="project">
              <el-select v-model="timeReportForm.project.id" placeholder="请选择项目">
                <el-option
                  v-for="project in projectList"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                />
              </el-select>
              <div class="form-item-hint" v-if="projectList.length === 0">
                <el-alert
                  title="您没有参与任何项目，请联系管理员添加您到项目中"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </div>
            </el-form-item>

          <el-form-item label="工作日期" prop="date">
            <el-date-picker
              v-model="timeReportForm.date"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-form-item label="工时(小时)" prop="hours">
            <el-input-number v-model="timeReportForm.hours" :min="0.5" :max="24" :step="0.5" />
          </el-form-item>

          <el-form-item label="工作类型" prop="workType">
            <el-select v-model="timeReportForm.workType" placeholder="请选择工作类型">
              <el-option label="开发" value="开发" />
              <el-option label="测试" value="测试" />
              <el-option label="设计" value="设计" />
              <el-option label="文档" value="文档" />
              <el-option label="会议" value="会议" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>

          <el-form-item label="工作内容" prop="description">
            <el-input
              v-model="timeReportForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入工作内容"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitTimeReport">提交</el-button>
          </el-form-item>
        </el-form>
        </div>

        <!-- 工时列表 Tab -->
        <div v-show="activeTab === 'approval'" class="tab-pane">
          <div class="approval-container">
          <!-- 搜索和筛选区域 -->
          <div class="search-bar">
            <el-form :inline="true" :model="approvalSearchForm" class="search-form">
              <el-form-item label="项目">
                <el-select v-model="approvalSearchForm.projectId" placeholder="选择项目" clearable style="width: 200px;">
                  <el-option
                    v-for="project in projectList"
                    :key="project.id"
                    :label="project.name"
                    :value="project.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="日期范围">
                <el-date-picker
                  v-model="approvalSearchForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  clearable
                  style="width: 300px;"
                />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="approvalSearchForm.status" placeholder="选择状态" clearable style="width: 150px;">
                  <el-option label="待审批" value="pending" />
                  <el-option label="已通过" value="approved" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchApprovalList">查询</el-button>
                <el-button @click="resetApprovalSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 查询结果提示 -->
          <div class="query-result-info" v-if="!approvalLoading && approvalList.length > 0">
            当前显示 <span class="highlight">{{ approvalList.length }}</span> 条记录，
            时间范围： <span class="highlight">{{ approvalSearchForm.dateRange[0] || '-' }}</span> 至 <span class="highlight">{{ approvalSearchForm.dateRange[1] || '-' }}</span>
          </div>

          <!-- 工时记录列表 -->
          <el-table
            :data="approvalList"
            style="width: 100%"
            v-loading="approvalLoading"
            :row-key="row => row.id"
          >
            <el-table-column label="项目" width="180">
              <template #default="scope">
                {{ scope.row.project?.name || '未知项目' }}
              </template>
            </el-table-column>
            <el-table-column prop="date" label="工作日期" width="120">
              <template #default="scope">
                <span>{{ scope.row.date }}</span>
                <el-tooltip content="工作日期" placement="top">
                  <el-icon class="ml-1"><Calendar /></el-icon>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="hours" label="工时" width="100" />
            <el-table-column prop="workType" label="工作类型" width="100" />
            <el-table-column prop="description" label="工作内容" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.approved)">
                  {{ scope.row.approved ? '已通过' : '待审批' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="scope">
                <el-button
                  size="small"
                  type="success"
                  @click="approveTimeReport(scope.row)"
                  v-if="!scope.row.approved && isApprover"
                >
                  通过
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  @click="rejectTimeReport(scope.row)"
                  v-if="!scope.row.approved && isApprover"
                >
                  驳回
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  @click="deleteTimeReport(scope.row)"
                  v-if="!scope.row.approved"
                >
                  删除
                </el-button>
                <el-button
                  size="small"
                  type="info"
                  @click="viewTimeReportDetail(scope.row)"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页控件 -->
          <div class="pagination-container">
            <el-pagination
              :current-page="approvalPagination.currentPage"
              :page-size="approvalPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="approvalPagination.total"
              @size-change="handleApprovalSizeChange"
              @current-change="handleApprovalCurrentChange"
              @update:current-page="val => approvalPagination.currentPage = val"
              @update:page-size="val => approvalPagination.pageSize = val"
            />
          </div>

          <!-- 详情对话框 -->
          <el-dialog v-model="timeReportDetailVisible" title="工时记录详情" width="50%">
            <div v-if="selectedTimeReport">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="项目">{{ selectedTimeReport.project?.name || '未知项目' }}</el-descriptions-item>
                <el-descriptions-item label="工作日期">{{ selectedTimeReport.date }}</el-descriptions-item>
                <el-descriptions-item label="工时">{{ selectedTimeReport.hours }}</el-descriptions-item>
                <el-descriptions-item label="工作类型">{{ selectedTimeReport.workType }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusTagType(selectedTimeReport.approved)">
                    {{ selectedTimeReport.approved ? '已通过' : '待审批' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="工作内容" :span="2">{{ selectedTimeReport.description }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="timeReportDetailVisible = false">关闭</el-button>
                <el-button type="success" @click="approveTimeReport(selectedTimeReport)" v-if="selectedTimeReport && !selectedTimeReport.approved && isApprover">通过</el-button>
                <el-button type="warning" @click="rejectTimeReport(selectedTimeReport)" v-if="selectedTimeReport && !selectedTimeReport.approved && isApprover">驳回</el-button>
                <el-button type="danger" @click="deleteTimeReport(selectedTimeReport)" v-if="selectedTimeReport && !selectedTimeReport.approved">删除</el-button>
              </span>
            </template>
          </el-dialog>
        </div>
        </div>

        <!-- 批量填写 Tab -->
        <div v-show="activeTab === 'batch'" class="tab-pane">
          <div class="batch-container">
          <div class="batch-header">
            <el-date-picker
              v-model="batchDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :default-value="getDefaultDateRangeDates()"
              @change="onBatchDateRangeChange"
            />
            <el-button type="primary" @click="submitBatchTimeRecords">批量提交</el-button>
          </div>

          <div class="batch-table-container" v-loading="batchLoading">
            <!-- 项目数据表格 -->
            <el-table
              v-if="batchProjects.length > 0"
              :data="batchProjects"
              style="width: 100%"
              border
              :max-height="500"
              :cell-class-name="getCellClass"
            >
              <!-- 固定列 -->
              <el-table-column type="index" label="序号" width="60" fixed="left" />
              <el-table-column prop="name" label="项目名称" width="200" fixed="left" />
              <el-table-column prop="managerName" label="项目经理" width="120" fixed="left" />

              <!-- 动态日期列 -->
              <el-table-column
                v-for="date in batchDates"
                :key="date"
                :label="formatDateLabel(date)"
                :prop="date"
                width="80"
                align="center"
              >
                <template #default="scope">
                  <el-input-number
                    v-model="scope.row.hours[date]"
                    :min="0"
                    :max="8"
                    :step="1"
                    :precision="0"
                    :controls="false"
                    size="small"
                    style="width: 60px"
                    :disabled="!isWorkdayForDate(date)"
                    @change="validateHours(scope.row.hours[date])"
                  />
                </template>
              </el-table-column>

              <!-- 合计列 -->
              <el-table-column label="合计" width="80" fixed="right" align="center">
                <template #default="scope">
                  {{ calculateTotalHours(scope.row.hours) }}
                </template>
              </el-table-column>
            </el-table>

            <!-- 空状态显示 -->
            <div class="batch-empty" v-if="batchProjects.length === 0 && !batchLoading">
              <el-empty description="暂无项目数据">
                <template #description>
                  <div class="empty-description">
                    <p v-if="projectLoadingState.hasError">
                      获取项目数据失败，请检查网络连接或联系管理员
                    </p>
                    <p v-else-if="projectLoadingState.retryCount >= projectLoadingState.maxRetries">
                      已尝试多次获取项目数据，请联系管理员确认您的项目权限
                    </p>
                    <p v-else>
                      当前没有可用的项目数据
                    </p>
                  </div>
                </template>
                <template #extra>
                  <el-button 
                    type="primary" 
                    @click="retryFetchProjects"
                    :disabled="projectLoadingState.isLoading || projectLoadingState.retryCount >= projectLoadingState.maxRetries"
                    :loading="projectLoadingState.isLoading"
                  >
                    {{ projectLoadingState.isLoading ? '重新获取中...' : '重新获取项目' }}
                  </el-button>
                  <el-button 
                    v-if="projectLoadingState.retryCount >= projectLoadingState.maxRetries"
                    type="info" 
                    @click="resetProjectState"
                  >
                    重置状态
                  </el-button>
                </template>
              </el-empty>
            </div>

            <!-- 项目加载状态提示 -->
            <div class="batch-status-info" v-if="!batchLoading && batchProjects.length > 0">
              <el-alert
                v-if="projectLoadingState.retryCount > 0 && projectLoadingState.retryCount < projectLoadingState.maxRetries"
                :title="`已重试 ${projectLoadingState.retryCount} 次获取项目数据`"
                type="warning"
                :closable="false"
                show-icon
              />
            </div>
          </div>
        </div>
        </div>

        <!-- 统计报表 Tab -->
        <div v-show="activeTab === 'statistics'" class="tab-pane">
          <div class="statistics-container">
          <div class="statistics-header">
            <div class="statistics-filters">
              <el-date-picker
                v-model="statisticsDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :default-value="getDefaultDateRangeDates()"
                @change="onDateRangeChange"
                style="width: 300px;"
              />
              <el-select
                v-model="selectedProjectId"
                placeholder="选择项目"
                clearable
                style="width: 200px; margin-left: 10px;"
              >
                <el-option
                  key="all"
                  label="所有项目"
                  value=""
                />
                <el-option
                  v-for="project in projectList"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                />
              </el-select>
            </div>
            <el-button type="primary" @click="fetchStatisticsData" :disabled="!statisticsDateRange || statisticsDateRange.length !== 2">刷新数据</el-button>
          </div>

          <div class="statistics-summary">
            <el-card shadow="hover">
              <template #header>
                <div class="clearfix">
                  <span>工时汇总</span>
                </div>
              </template>
              <div class="summary-content">
                <div class="summary-item">
                  <span class="summary-label">总工时：</span>
                  <span class="summary-value">{{ summaryData.totalHours }} 小时</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">加班时长：</span>
                  <span class="summary-value">{{ summaryData.overtimeHours }} 小时</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">工作负荷：</span>
                  <span class="summary-value">{{ summaryData.workload }}%</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">记录数量：</span>
                  <span class="summary-value">{{ summaryData.recordCount }} 条</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">平均工时：</span>
                  <span class="summary-value">{{ summaryData.averageHoursPerRecord }} 小时/条</span>
                </div>
              </div>
            </el-card>
          </div>

          <!-- 项目统计表格 -->
          <div class="statistics-section">
            <h3 class="section-title">项目工时统计</h3>
            <el-table :data="statisticsData" style="width: 100%;" :row-class-name="tableRowClassName">
              <el-table-column prop="projectName" label="项目" />
              <el-table-column prop="totalHours" label="总工时" sortable>
                <template #default="scope">
                  {{ scope.row.totalHours.toFixed(2) }} 小时
                </template>
              </el-table-column>
              <el-table-column prop="percentage" label="占比" sortable />
            </el-table>
          </div>

          <!-- 图表展示 -->
          <div class="statistics-section">
            <h3 class="section-title">工时分布图表</h3>
            <div class="chart-container">
              <el-empty description="暂无数据" v-if="!statisticsData.length" />
              <div v-else>
                <div id="timeStatisticsChart" style="width: 100%; height: 400px"></div>
              </div>
            </div>
          </div>
        </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { APP_CONFIG } from '../utils/config.js'
import { ref, reactive, onMounted, watch } from 'vue'
import api from '../utils/axios.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { useUserStore } from '../stores/user'
import { Calendar } from '@element-plus/icons-vue'

// 定义 tabs 数组，用于自定义 tabs 导航
const tabs = [
  { name: 'report', label: '工时填报' },
  { name: 'batch', label: '批量填写' },
  { name: 'approval', label: '工时列表' },
  { name: 'statistics', label: '统计报表' }
]

// 添加日志以检查默认日期范围是否正确设置
console.log('默认日期范围(字符串):', APP_CONFIG.DEFAULT_DATE_RANGE.getRange());
console.log('默认日期范围(Date对象):', APP_CONFIG.DEFAULT_DATE_RANGE.getRangeDates());
const statisticsDateRange = ref([])
console.log('statisticsDateRange initial value:', statisticsDateRange.value);
const batchDateRange = ref([])
const activeTab = ref('report')
const userStore = useUserStore()
const currentUser = ref(userStore.user)

const summaryData = ref({
  totalHours: '0.00',
  overtimeHours: '0.00',
  workload: '0',
  recordCount: 0,
  averageHoursPerRecord: '0.00'
});

const timeReportForm = reactive({
  project: { id: '' },
  user: { id: '' },
  date: '',
  hours: 8,
  workType: '开发',
  description: '',
  approved: false
})

const projectList = ref([])
const approvalList = ref([])
const statisticsData = ref([])
const selectedProjectId = ref('') // 默认为空字符串，表示所有项目
const isApprover = ref(false)

// Enhanced state management for project loading and batch operations
const projectLoadingState = ref({
  isLoading: false,
  hasError: false,
  errorMessage: '',
  lastFetchTime: null,
  retryCount: 0,
  maxRetries: 3,
  cacheValid: false,
  lastErrorTime: null,
  errorCooldown: 30000 // 30秒冷却时间，避免频繁显示错误
})

const batchState = ref({
  isEmpty: false,
  isFiltered: false,
  showEmptyState: false,
  filterCriteria: {},
  lastGenerationTime: null,
  isGenerating: false,
  hasShownEmptyWarning: false // 标记是否已显示过空项目警告
})

// 批量填写相关数据
const batchDates = ref([])
    const batchProjects = ref([])
    const batchLoading = ref(false)
    const workdayCache = ref(new Map()) // 工作日缓存

// 工时列表相关数据
const approvalLoading = ref(false)
const approvalSearchForm = reactive({
  projectId: '',
  // 设置默认日期范围
  dateRange: [],
  status: ''
})

// 初始化函数
const initApprovalSearchForm = () => {
  approvalSearchForm.dateRange = getDefaultDateRange()
}
const approvalPagination = reactive({
  currentPage: 1,
  pageSize: 20, // 增加默认分页大小，显示更多记录
  total: 0
})
const timeReportDetailVisible = ref(false)
const selectedTimeReport = ref(null)

const fetchProjects = async (forceRefresh = false) => {
  // 检查是否正在加载中，避免重复请求
  if (projectLoadingState.value.isLoading) {
    console.log('项目列表正在加载中，跳过重复请求')
    return
  }

  // 检查缓存是否有效且不强制刷新
  if (!forceRefresh && projectLoadingState.value.cacheValid && projectList.value.length > 0) {
    console.log('使用缓存的项目列表')
    return
  }

  if (!currentUser.value || !currentUser.value.id) {
    // 如果没有用户信息，尝试从缓存中获取
    if (userStore.user && userStore.user.id) {
      // 用户信息存在于缓存中
      currentUser.value = userStore.user
    } else {
      // 只在冷却时间过后才显示警告
      const now = Date.now()
      if (!projectLoadingState.value.lastErrorTime || 
          (now - projectLoadingState.value.lastErrorTime) > projectLoadingState.value.errorCooldown) {
        ElMessage.warning('未获取到当前用户信息')
        projectLoadingState.value.lastErrorTime = now
      }
      return
    }
  }

  projectLoadingState.value.isLoading = true
  projectLoadingState.value.hasError = false

  try {
    // 获取当前用户参与的项目列表
    const response = await api.get(`/api/projects/user/${currentUser.value.id}`)
    projectList.value = response.data
    console.log('获取到用户参与的项目列表:', projectList.value)

    // 更新加载状态
    projectLoadingState.value.cacheValid = true
    projectLoadingState.value.lastFetchTime = Date.now()
    projectLoadingState.value.retryCount = 0
    projectLoadingState.value.hasError = false

    // 在项目列表加载完成后初始化批量填写表格
    if (activeTab.value === 'batch') {
      await initBatchDateRange()
    }
  } catch (error) {
    console.error('获取项目列表失败:', error)
    projectLoadingState.value.hasError = true
    projectLoadingState.value.errorMessage = error.message
    projectLoadingState.value.retryCount++

    // 只在冷却时间过后才显示错误消息
    const now = Date.now()
    if (!projectLoadingState.value.lastErrorTime || 
        (now - projectLoadingState.value.lastErrorTime) > projectLoadingState.value.errorCooldown) {
      ElMessage.error('获取项目列表失败: ' + error.message)
      projectLoadingState.value.lastErrorTime = now
    }
  } finally {
    projectLoadingState.value.isLoading = false
  }
}

    const fetchApprovalList = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          ElMessage.warning('未获取到当前用户信息')
          return
        }
      }

      approvalLoading.value = true

      try {
        // 构建查询参数
        const params = {
          _t: new Date().getTime() // 添加时间戳参数，避免浏览器缓存
        }

        // 如果有项目筛选
        if (approvalSearchForm.projectId) {
          params.projectId = approvalSearchForm.projectId
        }

        // 如果有日期范围筛选
        if (approvalSearchForm.dateRange && approvalSearchForm.dateRange.length === 2) {
          params.startDate = approvalSearchForm.dateRange[0]
          params.endDate = approvalSearchForm.dateRange[1]
        } else {
          // 默认日期范围：当前年度的所有日期
          const now = new Date()
          const year = now.getFullYear()

          // 当前年的第一天
          params.startDate = `${year}-01-01`

          // 当前年的最后一天
          params.endDate = `${year}-12-31`
        }

        // 如果有状态筛选
        if (approvalSearchForm.status) {
          params.approved = approvalSearchForm.status === 'approved'
        }

        // 添加分页参数
        params.page = approvalPagination.currentPage - 1 // 后端从0开始计数
        params.size = approvalPagination.pageSize

        console.log('获取工时记录，参数:', params)

        // 调用API获取工时记录
        const response = await api.get(`/api/worktime/user/${currentUser.value.id}/range`, { params })

        // 处理返回的数据
        if (Array.isArray(response.data)) {
          // 如果返回的是数组，则直接使用
          approvalList.value = response.data
          approvalPagination.total = response.data.length // 注意：这里只是临时处理，实际应该由后端返回总数
        } else if (response.data && response.data.content) {
          // 如果返回的是分页对象，则提取content属性
          approvalList.value = response.data.content
          approvalPagination.total = response.data.totalElements || 0
        } else {
          approvalList.value = []
          approvalPagination.total = 0
        }

        // 按日期降序排序
        approvalList.value.sort((a, b) => new Date(b.date) - new Date(a.date))

        console.log('获取到工时记录:', approvalList.value.length, '条')
      } catch (error) {
        console.error('获取工时记录失败:', error)
        ElMessage.error('获取工时记录失败: ' + error.message)
        approvalList.value = []
      } finally {
        approvalLoading.value = false
      }
    }

    // 搜索工时列表
    const searchApprovalList = () => {
      approvalPagination.currentPage = 1 // 重置到第一页
      fetchApprovalList()
    }

    // 重置搜索条件
    const resetApprovalSearch = () => {
      approvalSearchForm.projectId = ''
      approvalSearchForm.dateRange = []
      approvalSearchForm.status = ''
      searchApprovalList()
    }

    // 分页大小变化处理
    const handleApprovalSizeChange = (size) => {
      approvalPagination.pageSize = size
      fetchApprovalList()
    }

    // 分页页码变化处理
    const handleApprovalCurrentChange = (page) => {
      approvalPagination.currentPage = page
      fetchApprovalList()
    }

    // 查看工时记录详情
    const viewTimeReportDetail = (record) => {
      selectedTimeReport.value = record
      timeReportDetailVisible.value = true
    }

    const rejectTimeReport = async (record) => {
      try {
        const reason = await ElMessageBox.prompt('请输入驳回原因', '驳回工时', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入驳回原因'
        })

        await api.put(`/api/worktime/reject/${record.id}`, { reason: reason.value })
        ElMessage.success('工时已驳回')
        record.approved = false
        fetchApprovalList() // 刷新列表
      } catch (error) {
        if (error === 'cancel') {
          return
        }
        console.error('驳回工时失败:', error)
        ElMessage.error('驳回工时失败: ' + error.message)
      }
    }

    const fetchStatisticsData = async () => {
      console.log('fetchStatisticsData 被调用，日期范围:', statisticsDateRange.value);

      if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2 || !currentUser.value || !currentUser.value.id) {
        console.log('条件检查未通过:', {
          hasDateRange: !!statisticsDateRange.value,
          dateRangeLength: statisticsDateRange.value ? statisticsDateRange.value.length : 0,
          hasCurrentUser: !!currentUser.value,
          hasUserId: currentUser.value ? !!currentUser.value.id : false
        });

        if (!currentUser.value || !currentUser.value.id) {
          console.log('尝试获取用户信息');
          if (userStore.user && userStore.user.id) {
            console.log('从 userStore 获取到用户信息');
            currentUser.value = userStore.user
          } else if (userStore.token) {
            console.log('尝试从服务器获取用户信息');
            await userStore.fetchUser()
            if (userStore.user && userStore.user.id) {
              console.log('从服务器获取到用户信息');
              currentUser.value = userStore.user
            } else {
              console.log('无法获取用户信息');
              ElMessage.warning('未获取到当前用户信息')
              return
            }
          } else {
            console.log('无法获取用户信息，没有 token');
            ElMessage.warning('未获取到当前用户信息')
            return
          }
        }

        if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
          console.log('日期范围无效，退出函数');
          return
        }
      }

      try {
        console.log('开始处理日期范围并获取统计数据');

        // 确保日期格式正确
        let startDate, endDate;

        // 检查日期格式是否为 YYYY-MM-DD
        if (statisticsDateRange.value[0].includes('-') && statisticsDateRange.value[0].split('-').length === 3) {
          // 已经是 YYYY-MM-DD 格式
          startDate = statisticsDateRange.value[0];
        } else if (statisticsDateRange.value[0].includes('-') && statisticsDateRange.value[0].split('-').length === 2) {
          // 是 YYYY-MM 格式，转换为 YYYY-MM-01
          startDate = `${statisticsDateRange.value[0]}-01`;
        } else {
          // 其他格式，使用当前日期
          const now = new Date();
          startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
        }

        // 检查结束日期格式
        if (statisticsDateRange.value[1].includes('-') && statisticsDateRange.value[1].split('-').length === 3) {
          // 已经是 YYYY-MM-DD 格式
          endDate = statisticsDateRange.value[1];
        } else if (statisticsDateRange.value[1].includes('-') && statisticsDateRange.value[1].split('-').length === 2) {
          // 是 YYYY-MM 格式，计算月末
          const [endYear, endMonthStr] = statisticsDateRange.value[1].split('-');
          const endMonthNum = parseInt(endMonthStr);
          // 使用下个月的第0天来获取当前月的最后一天
          const lastDay = new Date(parseInt(endYear), endMonthNum, 0).getDate();
          endDate = `${statisticsDateRange.value[1]}-${String(lastDay).padStart(2, '0')}`;
        } else {
          // 其他格式，使用当前日期的月末
          const now = new Date();
          const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
          endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
        }

        console.log('处理后的日期范围:', startDate, '至', endDate);

        console.log('获取统计数据，日期范围:', startDate, '至', endDate);
        console.log('当前用户ID:', currentUser.value.id);
        console.log('选择的项目ID:', selectedProjectId.value);

        let apiUrl;
        let params = { startDate, endDate };

        // 根据是否选择了特定项目决定使用哪个API
        if (selectedProjectId.value) {
          // 获取特定项目的统计数据
          apiUrl = `/api/worktime/project/${selectedProjectId.value}/stats`;
          console.log('使用项目统计API:', apiUrl);
        } else {
          // 如果没有选择特定项目，则获取所有项目的统计数据或用户的统计数据
          if (currentUser.value && currentUser.value.id) {
            // 获取当前用户的统计数据
            apiUrl = `/api/worktime/user/${currentUser.value.id}/stats`;
            console.log('使用用户统计API:', apiUrl);
          } else {
            // 获取所有项目的统计数据
            apiUrl = `/api/worktime/projects/stats`;
            console.log('使用所有项目统计API:', apiUrl);
          }
        }

        console.log('最终API请求URL:', apiUrl);
        const response = await api.get(apiUrl, { params })

        console.log('API响应数据:', response.data);

        // 检查响应数据结构
        let responseData;
        if (response.data && response.data.code === 200 && response.data.data) {
          // 新的嵌套结构 {code, message, data}
          responseData = response.data.data;
          console.log('使用嵌套数据结构:', responseData);
        } else {
          // 旧的直接结构
          responseData = response.data;
          console.log('使用直接数据结构:', responseData);
        }

        // 从响应中获取数据
        const totalHours = responseData.totalHours || 0;
        const overtimeHours = responseData.overtimeHours || 0;
        const workload = responseData.workload || 0;
        const recordCount = responseData.recordCount || 0;
        const averageHoursPerRecord = responseData.averageHoursPerRecord || 0;
        const projectHours = responseData.projects || {};

        console.log('项目工时数据:', projectHours);
        console.log('统计数据详情:', {
          totalHours,
          overtimeHours,
          workload,
          recordCount,
          averageHoursPerRecord
        });

        // 处理项目工时数据用于表格显示
        if (selectedProjectId.value) {
          // 如果选择了特定项目，只显示该项目的数据
          const project = projectList.value.find(p => p.id === parseInt(selectedProjectId.value)) || { name: '未知项目' };
          statisticsData.value = [{
            projectName: project.name,
            totalHours: totalHours,
            percentage: '100%'
          }];
        } else {
          // 如果没有选择特定项目，显示所有项目的数据
          statisticsData.value = Object.entries(projectHours).map(([projectId, hours]) => {
            const project = projectList.value.find(p => p.id === parseInt(projectId)) || { name: '未知项目' };
            return {
              projectName: project.name,
              totalHours: hours,
              percentage: totalHours > 0 ? ((hours / totalHours) * 100).toFixed(2) + '%' : '0%'
            };
          });

          // 按工时降序排序
          statisticsData.value.sort((a, b) => b.totalHours - a.totalHours);
        }

        // 更新汇总数据
        summaryData.value = {
          totalHours: totalHours.toFixed(2),
          overtimeHours: overtimeHours.toFixed(2),
          workload: (workload * 100).toFixed(2), // 转换为百分比
          recordCount: recordCount,
          averageHoursPerRecord: averageHoursPerRecord.toFixed(2)
        };

        console.log('获取到统计数据:', statisticsData.value);

        // 初始化图表
        if (statisticsData.value.length > 0) {
          console.log('准备初始化图表，数据条数:', statisticsData.value.length);
          setTimeout(() => {
            initStatisticsChart()
          }, 100)
        } else {
          console.log('没有统计数据，不初始化图表');
        }
      } catch (error) {
        console.error('获取统计数据失败:', error)
        ElMessage.error('获取统计数据失败: ' + error.message)
        statisticsData.value = []
        summaryData.value = {
          totalHours: '0.00',
          overtimeHours: '0.00',
          workload: '0',
          recordCount: 0,
          averageHoursPerRecord: '0.00'
        };
      }
    }

    const initStatisticsChart = () => {
      try {
        console.log('开始初始化图表');
        const chartDom = document.getElementById('timeStatisticsChart')
        if (!chartDom) {
          console.error('找不到图表DOM元素');
          return;
        }

        console.log('准备图表数据');
        // 确保统计数据有效
        if (!statisticsData.value || statisticsData.value.length === 0) {
          console.warn('没有有效的统计数据用于图表');
          return;
        }

        // 准备图表数据
        const chartData = statisticsData.value.map(item => ({
          value: item.totalHours,
          name: item.projectName
        }));
        console.log('图表数据:', chartData);

        // 初始化图表
        console.log('初始化ECharts实例');
        const myChart = echarts.init(chartDom)

        const option = {
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} 小时 ({d}%)'
          },
          legend: {
            top: '5%',
            left: 'center'
          },
          series: [
            {
              name: '工时分布',
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: false,
                position: 'center'
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 20,
                  fontWeight: 'bold'
                }
              },
              labelLine: {
                show: false
              },
              data: chartData
            }
          ]
        }

        console.log('设置图表选项');
        myChart.setOption(option)
        console.log('图表初始化完成');
      } catch (error) {
        console.error('初始化图表失败:', error);
      }
    }

    const submitTimeReport = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          ElMessage.warning('未获取到当前用户信息')
          return
        }
      }

      timeReportForm.user.id = currentUser.value.id

      try {
        await api.post('/api/worktime', timeReportForm)
        ElMessage.success('工时提交成功')
        // 重置表单
        timeReportForm.project.id = ''
        timeReportForm.date = ''
        timeReportForm.hours = 8
        timeReportForm.workType = '开发'
        timeReportForm.description = ''
      } catch (error) {
        console.error('提交工时失败:', error)
        ElMessage.error('提交工时失败: ' + error.message)
      }
    }

    const approveTimeReport = async (record) => {
      try {
        await api.put(`/api/worktime/approve/${record.id}`)
        ElMessage.success('审批通过')
        record.approved = true
        fetchApprovalList() // 刷新列表
      } catch (error) {
        console.error('审批工时失败:', error)
        ElMessage.error('审批工时失败: ' + error.message)
      }
    }

    const deleteTimeReport = async (record) => {
      try {
        await ElMessageBox.confirm('确定要删除这条工时记录吗？', '确认删除', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await api.delete(`/api/worktime/${record.id}`)
        ElMessage.success('删除成功')
        fetchApprovalList() // 刷新列表
      } catch (error) {
        if (error === 'cancel') {
          return
        }
        console.error('删除工时失败:', error)
        ElMessage.error('删除工时失败: ' + error.message)
      }
    }

    // 从全局配置获取默认日期范围（字符串格式）
    const getDefaultDateRange = () => {
      try {
        return APP_CONFIG?.DEFAULT_DATE_RANGE?.getRange() || [];
      } catch (error) {
        console.error('获取默认日期范围失败:', error);
        return [];
      }
    }

    // 从全局配置获取默认日期范围（Date对象格式）
    const getDefaultDateRangeDates = () => {
      try {
        return APP_CONFIG?.DEFAULT_DATE_RANGE?.getRangeDates() || [new Date(), new Date()];
      } catch (error) {
        console.error('获取默认日期范围(Date对象)失败:', error);
        // 返回默认值：当前日期和当前日期
        return [new Date(), new Date()];
      }
    }

    const initBatchDateRange = async () => {
      console.log('初始化批量日期范围，当前值:', batchDateRange.value);

      // 检查日期范围是否有效
      let needsReset = true;

      if (batchDateRange.value && batchDateRange.value.length === 2) {
        try {
          const startDate = new Date(batchDateRange.value[0]);
          const endDate = new Date(batchDateRange.value[1]);

          if (!isNaN(startDate.getTime()) && !isNaN(endDate.getTime()) && startDate <= endDate) {
            console.log('当前批量日期范围有效，无需重置');
            needsReset = false;
          } else {
            console.warn('当前批量日期范围无效，需要重置');
          }
        } catch (error) {
          console.error('解析批量日期范围时出错:', error);
        }
      } else {
        console.warn('批量日期范围不完整，需要重置');
      }

      if (needsReset) {
        // 获取默认日期范围
        const defaultRange = getDefaultDateRange();
        console.log('获取到的默认日期范围:', defaultRange);

        if (defaultRange && defaultRange.length === 2) {
          batchDateRange.value = defaultRange;
          console.log('已重置批量日期范围为默认值:', batchDateRange.value);
        } else {
          // 如果默认日期范围无效，则使用当前月的日期范围
          const now = new Date();
          const year = now.getFullYear();
          const month = now.getMonth() + 1;

          // 上个月的25号到当前月的24号
          let startMonth, startYear, endMonth, endYear;

          if (now.getDate() < 25) {
            // 如果当前日期小于25号，则是上上个月25号到上个月24号
            if (month === 1) {
              startMonth = 11;
              startYear = year - 1;
              endMonth = 12;
              endYear = year - 1;
            } else if (month === 2) {
              startMonth = 12;
              startYear = year - 1;
              endMonth = 1;
              endYear = year;
            } else {
              startMonth = month - 2;
              startYear = year;
              endMonth = month - 1;
              endYear = year;
            }
          } else {
            // 如果当前日期大于等于25号，则是上个月25号到当前月24号
            if (month === 1) {
              startMonth = 12;
              startYear = year - 1;
              endMonth = 1;
              endYear = year;
            } else {
              startMonth = month - 1;
              startYear = year;
              endMonth = month;
              endYear = year;
            }
          }

          batchDateRange.value = [
            `${startYear}-${String(startMonth).padStart(2, '0')}-25`,
            `${endYear}-${String(endMonth).padStart(2, '0')}-24`
          ];

          console.log('已设置批量日期范围为上月25日至本月24日:', batchDateRange.value);
        }
      }

      // 预加载工作日数据
      await fetchWorkdays(batchDateRange.value);
      
      // 生成批量表格
      generateBatchTable();

      // 加载已提交的工时数据
      loadSubmittedWorkTime();
    }

    const generateBatchTable = async () => {
      console.log('generateBatchTable 被调用，日期范围:', batchDateRange.value);

      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        console.warn('批量填写日期范围无效:', batchDateRange.value);
        ElMessage.warning('请选择日期范围')
        return
      }

      // 防止重复生成
      if (batchState.value.isGenerating) {
        console.log('批量表格正在生成中，跳过重复请求')
        return
      }

      batchState.value.isGenerating = true
      batchLoading.value = true
      console.log('开始生成批量表格，日期范围:', batchDateRange.value[0], '至', batchDateRange.value[1]);

      try {
        // 确保日期格式正确
        const startDate = new Date(batchDateRange.value[0])
        const endDate = new Date(batchDateRange.value[1])

        console.log('解析后的日期对象:', startDate, endDate);

        if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
          console.error('日期格式无效:', batchDateRange.value);
          ElMessage.warning('日期格式无效，请重新选择')
          return
        }

        if (startDate > endDate) {
          console.error('开始日期晚于结束日期:', startDate, endDate);
          ElMessage.warning('开始日期不能晚于结束日期')
          return
        }

        // 计算日期列表
        const dates = []
        let currentDate = new Date(startDate)
        while (currentDate <= endDate) {
          const year = currentDate.getFullYear()
          const month = String(currentDate.getMonth() + 1).padStart(2, '0')
          const day = String(currentDate.getDate()).padStart(2, '0')
          dates.push(`${year}-${month}-${day}`)
          currentDate.setDate(currentDate.getDate() + 1)
        }
        batchDates.value = dates
        console.log('生成的日期列表:', batchDates.value);
        console.log('批量日期数组详情:', {
          total: batchDates.value.length,
          first: batchDates.value[0],
          last: batchDates.value[batchDates.value.length - 1],
          samples: batchDates.value.slice(0, 10)
        });

        // 检查项目列表
        if (!projectList.value || projectList.value.length === 0) {
          console.warn('项目列表为空，尝试获取项目数据');
          
          // 检查是否已经达到最大重试次数
          if (projectLoadingState.value.retryCount >= projectLoadingState.value.maxRetries) {
            console.warn('已达到最大重试次数，显示空状态');
            batchState.value.isEmpty = true
            batchState.value.showEmptyState = true
            
            // 只在第一次显示空状态警告时提示用户
            if (!batchState.value.hasShownEmptyWarning) {
              ElMessage.info('当前没有可用的项目数据，请联系管理员确认您的项目权限')
              batchState.value.hasShownEmptyWarning = true
            }
            
            batchProjects.value = []
            return
          }

          try {
            await fetchProjects(true) // 强制刷新项目列表
            
            if (projectList.value && projectList.value.length > 0) {
              console.log('成功获取项目列表，继续生成表格');
              buildBatchProjects(dates);
              batchState.value.isEmpty = false
              batchState.value.showEmptyState = false
              // 加载已提交的工时数据
              await loadSubmittedWorkTime();
            } else {
              console.warn('获取项目列表后仍为空');
              batchState.value.isEmpty = true
              batchState.value.showEmptyState = true
              batchProjects.value = []
              
              // 只在冷却时间过后才显示警告
              const now = Date.now()
              if (!batchState.value.hasShownEmptyWarning || 
                  !projectLoadingState.value.lastErrorTime ||
                  (now - projectLoadingState.value.lastErrorTime) > projectLoadingState.value.errorCooldown) {
                ElMessage.warning('暂无可用项目，请联系管理员确认您的项目权限')
                batchState.value.hasShownEmptyWarning = true
                projectLoadingState.value.lastErrorTime = now
              }
            }
          } catch (error) {
            console.error('获取项目列表失败:', error);
            batchState.value.isEmpty = true
            batchState.value.showEmptyState = true
            batchProjects.value = []
            
            // 只在冷却时间过后才显示错误消息
            const now = Date.now()
            if (!projectLoadingState.value.lastErrorTime || 
                (now - projectLoadingState.value.lastErrorTime) > projectLoadingState.value.errorCooldown) {
              ElMessage.error('获取项目列表失败，请稍后重试')
              projectLoadingState.value.lastErrorTime = now
            }
          }
        } else {
          // 构建项目数据
          buildBatchProjects(dates);
          batchState.value.isEmpty = false
          batchState.value.showEmptyState = false
          // 加载已提交的工时数据
          await loadSubmittedWorkTime();
        }
      } catch (error) {
        console.error('生成批量表格失败:', error)
        batchState.value.isEmpty = true
        batchState.value.showEmptyState = true
        
        // 只在冷却时间过后才显示错误消息
        const now = Date.now()
        if (!projectLoadingState.value.lastErrorTime || 
            (now - projectLoadingState.value.lastErrorTime) > projectLoadingState.value.errorCooldown) {
          ElMessage.error('生成批量表格失败: ' + error.message)
          projectLoadingState.value.lastErrorTime = now
        }
      } finally {
        batchLoading.value = false
        batchState.value.isGenerating = false
        batchState.value.lastGenerationTime = Date.now()
      }
    }

    // 加载已提交的工时数据
    const loadSubmittedWorkTime = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          console.warn('未获取到当前用户信息，无法加载已提交工时');
          return
        }
      }

      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        console.warn('批量填写日期范围无效，无法加载已提交工时');
        return
      }

      try {
        batchLoading.value = true;
        console.log('加载已提交工时，日期范围:', batchDateRange.value[0], '至', batchDateRange.value[1]);

        // 构建查询参数
        const params = {
          startDate: batchDateRange.value[0],
          endDate: batchDateRange.value[1],
          _t: new Date().getTime(), // 添加时间戳参数，避免浏览器缓存
          size: 1000 // 设置较大的分页大小，确保能获取到所有记录
        }

        console.log('查询工时记录参数:', params);

        // 调用API获取工时记录
        const response = await api.get(`/api/worktime/user/${currentUser.value.id}/range`, { params });

        // 处理返回的数据
        let workTimeRecords = [];
        if (Array.isArray(response.data)) {
          // 如果返回的是数组，则直接使用
          workTimeRecords = response.data;
        } else if (response.data && response.data.content) {
          // 如果返回的是分页对象，则提取content属性
          workTimeRecords = response.data.content;
        }

        console.log('获取到工时记录:', workTimeRecords.length, '条');

        // 将已提交的工时数据填充到批量表格中
        if (workTimeRecords.length > 0) {
          // 创建一个映射，用于存储已提交的工时记录
          const submittedHoursMap = {};

          // 处理已提交的工时记录
          workTimeRecords.forEach(record => {
            const projectId = record.project?.id;
            const date = record.date;
            const hours = record.hours;

            if (projectId && date && hours) {
              if (!submittedHoursMap[projectId]) {
                submittedHoursMap[projectId] = {};
              }
              submittedHoursMap[projectId][date] = hours;
            }
          });

          // 填充已提交的工时数据，但不覆盖用户尚未提交的数据
          batchProjects.value.forEach(project => {
            const projectId = project.id;
            const submittedHours = submittedHoursMap[projectId] || {};

            batchDates.value.forEach(date => {
              // 如果该日期有已提交的工时记录，则更新
              if (submittedHours[date]) {
                project.hours[date] = submittedHours[date];
              }
            });
          });

          console.log('已填充工时数据到批量表格');
        }
      } catch (error) {
        console.error('加载已提交工时失败:', error);
        ElMessage.error('加载已提交工时失败: ' + error.message);
      } finally {
        batchLoading.value = false;
      }
    }

    // 辅助函数：构建批量项目数据
    const buildBatchProjects = (dates) => {
      console.log('构建批量项目数据，项目数量:', projectList.value.length);
      batchProjects.value = projectList.value.map(project => {
        const hours = {}
        dates.forEach(date => {
          hours[date] = 0
        })

        // 获取项目经理名称
        let managerName = '未知';
        if (project.managerName) {
          // 如果直接有managerName字段
          managerName = project.managerName;
        } else if (project.manager) {
          // 如果有manager对象
          if (typeof project.manager === 'string') {
            // 如果manager是字符串
            managerName = project.manager;
          } else if (project.manager.realName) {
            // 优先使用realName（真实姓名）
            managerName = project.manager.realName;
          } else if (project.manager.name) {
            // 如果manager对象有name属性
            managerName = project.manager.name;
          } else if (project.manager.username) {
            // 最后使用username（登录账号）
            managerName = project.manager.username;
          } else if (project.manager.firstName && project.manager.lastName) {
            // 如果manager对象有firstName和lastName属性
            managerName = `${project.manager.firstName} ${project.manager.lastName}`;
          } else if (project.manager.firstName) {
            // 如果manager对象只有firstName属性
            managerName = project.manager.firstName;
          } else if (project.manager.lastName) {
            // 如果manager对象只有lastName属性
            managerName = project.manager.lastName;
          }
        }

        console.log('项目:', project.name, '经理:', managerName);

        return {
          id: project.id,
          name: project.name,
          managerName: managerName,
          hours
        }
      })
      console.log('批量项目数据构建完成，数量:', batchProjects.value.length);
    }

    const formatDateLabel = (dateStr) => {
      const [, month, day] = dateStr.split('-') // 使用逗号跳过不需要的年份变量
      return `${month}/${day}`
    }

    const calculateTotalHours = (hoursObj) => {
      return Object.values(hoursObj).reduce((sum, val) => sum + (val || 0), 0)
    }

    const validateHours = (value) => {
      if (value < 0) return 0
      if (value > 8) return 8
      return value
    }

    // 获取工作日数据
    const fetchWorkdays = async (dateRange) => {
      if (!dateRange || dateRange.length !== 2) {
        console.warn('日期范围无效')
        return
      }
      
      const cacheKey = `${dateRange[0]}_${dateRange[1]}`
      if (workdayCache.value.has(cacheKey)) {
        console.log('使用缓存的工作日数据')
        return workdayCache.value.get(cacheKey)
      }
      
      try {
        const startDate = new Date(dateRange[0])
        const endDate = new Date(dateRange[1])
        
        const response = await api.get('/api/workdays/by-range', {
          params: {
            startDate: dateRange[0],
            endDate: dateRange[1]
          }
        })
        
        const workdays = response.data || []
        // 后端已修复时区问题，直接使用日期字符串
        const workdaySet = new Set(workdays.map(wd => {
          // 如果后端返回的是 'yyyy-MM-dd' 格式，直接使用
          if (typeof wd.date === 'string' && wd.date.match(/^\d{4}-\d{2}-\d{2}$/)) {
            return wd.date
          }
          // 如果是时间戳格式，则提取日期部分
          return wd.date.split('T')[0]
        }))
        
        // 缓存结果
        workdayCache.value.set(cacheKey, workdaySet)
        console.log('获取到工作日数据:', workdaySet.size, '个工作日')
        console.log('工作日列表:', Array.from(workdaySet).sort())
        console.log('工作日缓存详情:', {
          cacheKey,
          workdayCount: workdaySet.size,
          workdayArray: Array.from(workdaySet).sort()
        })
        
        // 详细分析7月5日和6日的状态
        console.log('特别检查 2025-07-05:', workdaySet.has('2025-07-05') ? '工作日' : '非工作日');
        console.log('特别检查 2025-07-06:', workdaySet.has('2025-07-06') ? '工作日' : '非工作日');
        console.log('特别检查 2025-07-04:', workdaySet.has('2025-07-04') ? '工作日' : '非工作日');
        console.log('特别检查 2025-07-07:', workdaySet.has('2025-07-07') ? '工作日' : '非工作日');
        
        return workdaySet
      } catch (error) {
        console.error('获取工作日数据失败:', error)
        ElMessage.error('获取工作日数据失败')
        return new Set()
      }
    }
    
    // 检查是否为工作日
    const isWorkday = async (date) => {
      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        // 如果没有日期范围，默认按周末判断
        const day = new Date(date).getDay()
        return day !== 0 && day !== 6
      }
      
      const workdaySet = await fetchWorkdays(batchDateRange.value)
      return workdaySet.has(date)
    }
    
    // 同步检查是否为工作日（用于模板中的禁用状态）
    const isWorkdayForDate = (date) => {
      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        // 如果没有日期范围，默认按周末判断
        const day = new Date(date).getDay()
        return day !== 0 && day !== 6
      }
      
      const cacheKey = `${batchDateRange.value[0]}_${batchDateRange.value[1]}`
      const workdaySet = workdayCache.value.get(cacheKey)
      if (workdaySet) {
        return workdaySet.has(date)
      }
      
      // 如果没有缓存，默认按周末判断
      const day = new Date(date).getDay()
      return day !== 0 && day !== 6
    }

    const getCellClass = ({ columnIndex }) => {
      // 添加调试信息
      console.log('getCellClass called with columnIndex:', columnIndex, 'batchDates length:', batchDates.value.length);
      
      if (columnIndex > 2 && columnIndex < batchDates.value.length + 3) {
        const dateIndex = columnIndex - 3;
        const date = batchDates.value[dateIndex];
        
        console.log('Column', columnIndex, '-> Date index:', dateIndex, '-> Date:', date);
        
        if (!date) {
          console.warn('Date is undefined for columnIndex:', columnIndex, 'dateIndex:', dateIndex);
          return '';
        }
        
        const day = new Date(date).getDay()
        
        // 检查是否在工作日缓存中
        if (batchDateRange.value && batchDateRange.value.length === 2) {
          const cacheKey = `${batchDateRange.value[0]}_${batchDateRange.value[1]}`
          const workdaySet = workdayCache.value.get(cacheKey)
          if (workdaySet) {
            const isWorkday = workdaySet.has(date);
            console.log('Date:', date, 'is workday:', isWorkday);
            if (!isWorkday) {
              return 'non-workday-cell' // 非工作日样式
            }
          }
        }
        
        // 周末样式（作为备用）
        if (day === 0 || day === 6) {
          console.log('Date:', date, 'is weekend');
          return 'weekend-cell'
        }
      }
      return ''
    }

    const submitBatchTimeRecords = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          ElMessage.warning('未获取到当前用户信息')
          return
        }
      }

      const records = []
      batchProjects.value.forEach(project => {
        Object.entries(project.hours).forEach(([date, hours]) => {
          if (hours > 0) {
            records.push({
              project: { id: project.id },
              user: { id: currentUser.value.id },
              date,
              hours,
              workType: '开发', // 默认值，可根据需求调整
              description: '批量填写',
              approved: false
            })
          }
        })
      })

      if (records.length === 0) {
        ElMessage.warning('没有需要提交的工时记录')
        return
      }

      try {
        await api.post('/api/worktime/batch', records)
        ElMessage.success(`成功提交 ${records.length} 条工时记录`)
        // 加载已提交的工时数据，而不是重置整个表格
        loadSubmittedWorkTime()
      } catch (error) {
        console.error('批量提交工时失败:', error)
        ElMessage.error('批量提交工时失败: ' + error.message)
      }
    }

    const getStatusTagType = (approved) => {
      return approved ? 'success' : 'warning'
    }

    const onDateRangeChange = (range) => {
      console.log('统计报表日期范围已更改:', range);
      if (range && range.length === 2) {
        // 日期范围变化时自动获取统计数据
        fetchStatisticsData();
      }
    }

    const onBatchDateRangeChange = async (range) => {
      console.log('批量填写日期范围已更改:', range);
      if (range && range.length === 2) {
        // 确保日期范围有效
        try {
          const startDate = new Date(range[0]);
          const endDate = new Date(range[1]);

          if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
            console.error('日期格式无效');
            ElMessage.warning('日期格式无效，请重新选择');
            return;
          }

          if (startDate > endDate) {
            console.error('开始日期不能晚于结束日期');
            ElMessage.warning('开始日期不能晚于结束日期');
            return;
          }

          // 更新日期范围
          batchDateRange.value = range;
          console.log('更新后的批量日期范围:', batchDateRange.value);
          
          // 预加载工作日数据
          await fetchWorkdays(range);
          
          // 生成表格
          generateBatchTable();
          // 加载已提交的工时数据
          loadSubmittedWorkTime();
        } catch (error) {
          console.error('处理日期范围时出错:', error);
          ElMessage.error('处理日期范围时出错: ' + error.message);
        }
      } else {
        console.warn('日期范围无效或不完整');
      }
    }

    // 监听项目选择变化
    watch(selectedProjectId, (newValue) => {
      console.log('选择的项目已更改:', newValue);
      if (statisticsDateRange.value && statisticsDateRange.value.length === 2) {
        // 项目选择变化时自动获取统计数据
        fetchStatisticsData();
      }
    });

    const tableRowClassName = () => {
      // 可以根据行数据设置不同的类名
      return '';
    }

    // 重试获取项目数据
    const retryFetchProjects = async () => {
      console.log('用户手动重试获取项目数据')
      
      // 重置错误状态
      projectLoadingState.value.hasError = false
      projectLoadingState.value.errorMessage = ''
      batchState.value.hasShownEmptyWarning = false
      
      // 强制刷新项目列表
      await fetchProjects(true)
      
      // 如果获取成功，重新生成批量表格
      if (projectList.value && projectList.value.length > 0) {
        await generateBatchTable()
      }
    }

    // 重置项目加载状态
    const resetProjectState = () => {
      console.log('重置项目加载状态')
      
      projectLoadingState.value.retryCount = 0
      projectLoadingState.value.hasError = false
      projectLoadingState.value.errorMessage = ''
      projectLoadingState.value.lastErrorTime = null
      projectLoadingState.value.cacheValid = false
      
      batchState.value.hasShownEmptyWarning = false
      batchState.value.isEmpty = false
      batchState.value.showEmptyState = false
      
      ElMessage.success('状态已重置，您可以重新尝试获取项目数据')
    }

    onMounted(async () => {
      try {
        // 初始化工时列表搜索表单
        initApprovalSearchForm()

        // 在页面加载时确保从缓存中获取用户信息，如果没有则从服务器获取
        if (!currentUser.value || !currentUser.value.id) {
          if (userStore.user && userStore.user.id) {
            currentUser.value = userStore.user
          } else if (userStore.token) {
            await userStore.fetchUser()
            if (userStore.user && userStore.user.id) {
              currentUser.value = userStore.user
            }
          }
        }

        // 先获取项目列表
        await fetchProjects()

        // 然后获取工时列表
        await fetchApprovalList()
      } catch (error) {
        console.error('组件初始化失败:', error)
        ElMessage.error('页面加载失败，请刷新重试')
      }

      // 初始化统计日期范围为当前和上个月
      if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
        // 获取默认日期范围
        const defaultRange = getDefaultDateRange();
        console.log('获取到的默认日期范围:', defaultRange);

        if (defaultRange && defaultRange.length === 2) {
          // 使用全局配置的默认日期范围
          statisticsDateRange.value = defaultRange;
          console.log('已设置统计日期范围为默认值:', statisticsDateRange.value);
        } else {
          // 如果默认日期范围无效，则使用上个月1号到当前月最后一天
          const now = new Date();
          const year = now.getFullYear();
          const month = now.getMonth() + 1;
          const lastMonth = month === 1 ? 12 : month - 1;
          const lastYear = month === 1 ? year - 1 : year;

          // 上个月的第一天
          const startDate = `${lastYear}-${String(lastMonth).padStart(2, '0')}-01`;

          // 当前月的最后一天
          const lastDay = new Date(year, month, 0).getDate();
          const endDate = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;

          statisticsDateRange.value = [startDate, endDate];
          console.log('已设置统计日期范围为上月1日至本月最后一天:', statisticsDateRange.value);
        }
      }

      // 自动获取统计数据
      if (statisticsDateRange.value && statisticsDateRange.value.length === 2) {
        fetchStatisticsData();
      }

      // 检查当前用户是否为审批人（这里是简化的逻辑，实际应根据权限系统判断）
      if (currentUser.value && currentUser.value.role) {
        // 确保 roleStr 是字符串类型，并检查是否包含 'manager'
        const roleStr = Array.isArray(currentUser.value.role) ? currentUser.value.role.join(',') : String(currentUser.value.role);
        if (roleStr.includes('manager')) {
          isApprover.value = true
        }
      }
    })

    watch(activeTab, async (newTab) => {
      if (newTab === 'batch' && projectList.value.length > 0) {
        await initBatchDateRange()
      } else if (newTab === 'approval') {
        fetchApprovalList()
      }
    })
</script>

<style scoped>
.time-management-container {
  padding: 20px;
  max-width: 100%;
  overflow-x: auto;
}

h1 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
  text-align: center;
}

.report-form {
  max-width: 600px;
  margin: 0 auto;
}

.approval-container {
  margin-top: 20px;
}

.search-bar {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.query-result-info {
  margin-bottom: 16px;
  padding: 10px 15px;
  background-color: #e6f7ff;
  border: 1px solid #bae7ff;
  border-radius: 4px;
  color: #1890ff;
  font-size: 14px;
}

.highlight {
  font-weight: bold;
  color: #0052cc;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.batch-container {
  margin-top: 20px;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.batch-table-container {
  margin-top: 20px;
  overflow-x: auto;
}

.batch-empty {
  margin-top: 20px;
  text-align: center;
}

.statistics-container {
  margin-top: 20px;
}

.statistics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.statistics-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.statistics-summary {
  margin-bottom: 20px;
}

.summary-content {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
}

.summary-item {
  margin: 10px 0;
  min-width: 200px;
}

.summary-label {
  color: #909399;
  font-size: 14px;
}

.summary-value {
  color: #303133;
  font-size: 18px;
  font-weight: bold;
}

.statistics-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.chart-container {
  margin-top: 30px;
}

.weekend-cell {
  background-color: #f0f9ff;
}

.non-workday-cell {
  background-color: #fff2f0;
}

.non-workday-cell .el-input-number {
  background-color: #fff2f0;
}

.non-workday-cell .el-input-number.is-disabled {
  background-color: #f5f5f5;
  color: #c0c4cc;
  cursor: not-allowed;
}

/* 自定义 Tab 样式 */
.custom-tabs {
  margin-bottom: 20px;
}

.custom-tabs-header {
  display: flex;
  border-bottom: 2px solid #e4e7ed;
  margin-bottom: 15px;
}

.custom-tab {
  padding: 10px 20px;
  margin-right: 5px;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  font-size: 14px;
  color: #606266;
}

.custom-tab:hover {
  color: #409eff;
}

.custom-tab.active {
  color: #409eff;
  border-bottom: 2px solid #409eff;
}

.tab-pane {
  padding: 10px 0;
}

.form-item-hint {
  margin-top: 8px;
  font-size: 12px;
}

/* 批量填写优化样式 */
.batch-empty {
  margin-top: 40px;
  padding: 20px;
}

.empty-description {
  margin: 10px 0;
  color: #909399;
  font-size: 14px;
  line-height: 1.5;
}

.empty-description p {
  margin: 8px 0;
}

.batch-status-info {
  margin-top: 15px;
}

.batch-status-info .el-alert {
  margin-bottom: 10px;
}

/* 项目加载状态样式 */
.project-loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #909399;
  font-size: 14px;
}

.project-loading-indicator .el-icon {
  margin-right: 8px;
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 重试按钮样式 */
.retry-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.retry-actions .el-button {
  min-width: 120px;
}
</style>