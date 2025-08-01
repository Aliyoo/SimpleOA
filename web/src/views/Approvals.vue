<template>
  <div class="approvals-container">
    <h1>审批管理</h1>

    <!-- 筛选条件区域 -->
    <div class="filter-container">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <!-- 日期范围筛选 -->
        <el-form-item label="创建日期">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            style="width: 320px"
          />
        </el-form-item>

        <!-- 审批状态筛选 -->
        <el-form-item label="审批状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>

        <!-- 项目筛选 -->
        <el-form-item v-if="activeTab === 'WORKTIME'" label="项目">
          <el-select
            v-model="filterForm.projectId"
            placeholder="全部项目"
            clearable
            style="width: 200px"
            :loading="projects.length === 0"
          >
            <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
            <template #empty>
              <p class="select-empty-text">{{ projects.length === 0 ? '加载项目列表中...' : '没有匹配的项目' }}</p>
            </template>
          </el-select>
        </el-form-item>

        <!-- 搜索按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleFilter">搜索</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮区域 -->
    <div class="operation-container">
      <el-button type="primary" @click="handleCreate">新建审批</el-button>
      <el-button type="success" :disabled="selectedApprovals.length === 0" @click="handleBatchApprove"
        >批量通过</el-button
      >
      <el-button type="danger" :disabled="selectedApprovals.length === 0" @click="handleBatchReject"
        >批量拒绝</el-button
      >
    </div>

    <!-- TAB区域 -->
    <el-tabs v-model="activeTab" class="approval-tabs" @tab-change="handleTabChange">
      <!-- 工时审批 -->
      <el-tab-pane label="工时审批" name="WORKTIME">
        <el-table
          ref="worktimeApprovalTable"
          v-loading="loading"
          :data="getApprovalsByType('WORKTIME')"
          style="width: 100%"
          border
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          :row-style="{ height: '55px' }"
          @selection-change="handleSelectionChange"
        >
          <!-- 选择列 -->
          <el-table-column type="selection" width="55" :selectable="isSelectable" />

          <!-- 审批标题 -->
          <el-table-column prop="title" label="审批标题" min-width="180">
            <template #default="{ row }">
              {{ getApprovalTitle(row) }}
            </template>
          </el-table-column>

          <!-- 项目 -->
          <el-table-column prop="project" label="项目" min-width="120">
            <template #default="{ row }">
              <template v-if="row.workTimeRecord && row.workTimeRecord.project">
                {{ row.workTimeRecord.project.name }}
              </template>
            </template>
          </el-table-column>

          <!-- 日期 -->
          <el-table-column prop="date" label="日期" width="100">
            <template #default="{ row }">
              <template v-if="row.workTimeRecord && row.workTimeRecord.date">
                {{ formatDate(row.workTimeRecord.date) }}
              </template>
            </template>
          </el-table-column>

          <!-- 工时详情 -->
          <el-table-column prop="details" label="工时" min-width="100">
            <template #default="{ row }">
              <template v-if="row.workTimeRecord && row.workTimeRecord.hours">
                {{ row.workTimeRecord.hours }} 小时
              </template>
            </template>
          </el-table-column>

          <!-- 状态 -->
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- 创建时间 -->
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleApprove(row)"
                >同意</el-button
              >
              <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleReject(row)"
                >拒绝</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <!-- 工时审批分页 -->
        <div v-if="getApprovalsByType('WORKTIME').length > 0" class="pagination-container">
          <el-pagination
            v-model="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="getApprovalsTotalByType('WORKTIME')"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty description="暂无工时审批" />
        </div>
      </el-tab-pane>

      <!-- 请假审批 -->
      <el-tab-pane label="请假审批" name="LEAVE">
        <el-table
          ref="leaveApprovalTable"
          v-loading="loading"
          :data="getApprovalsByType('LEAVE')"
          style="width: 100%"
          border
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          :row-style="{ height: '55px' }"
          @selection-change="handleSelectionChange"
        >
          <!-- 选择列 -->
          <el-table-column type="selection" width="55" :selectable="isSelectable" />

          <!-- 审批标题 -->
          <el-table-column prop="title" label="审批标题" min-width="180">
            <template #default="{ row }">
              {{ getApprovalTitle(row) }}
            </template>
          </el-table-column>

          <!-- 请假类型 -->
          <el-table-column prop="leaveType" label="请假类型" width="100">
            <template #default="{ row }">
              <template v-if="row.leaveRequest">
                {{ row.leaveRequest.type || '' }}
              </template>
            </template>
          </el-table-column>

          <!-- 开始日期 -->
          <el-table-column prop="startDate" label="开始日期" width="100">
            <template #default="{ row }">
              <template v-if="row.leaveRequest && row.leaveRequest.startDate">
                {{ formatDate(row.leaveRequest.startDate) }}
              </template>
            </template>
          </el-table-column>

          <!-- 结束日期 -->
          <el-table-column prop="endDate" label="结束日期" width="100">
            <template #default="{ row }">
              <template v-if="row.leaveRequest && row.leaveRequest.endDate">
                {{ formatDate(row.leaveRequest.endDate) }}
              </template>
            </template>
          </el-table-column>

          <!-- 状态 -->
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- 创建时间 -->
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleApprove(row)"
                >同意</el-button
              >
              <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleReject(row)"
                >拒绝</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <!-- 请假审批分页 -->
        <div v-if="getApprovalsByType('LEAVE').length > 0" class="pagination-container">
          <el-pagination
            v-model="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="getApprovalsTotalByType('LEAVE')"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty description="暂无请假审批" />
        </div>
      </el-tab-pane>

      <!-- 出差审批 -->
      <el-tab-pane label="出差审批" name="BUSINESS_TRIP">
        <el-table
          ref="businessTripApprovalTable"
          v-loading="loading"
          :data="getApprovalsByType('BUSINESS_TRIP')"
          style="width: 100%"
          border
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          :row-style="{ height: '55px' }"
          @selection-change="handleSelectionChange"
        >
          <!-- 选择列 -->
          <el-table-column type="selection" width="55" :selectable="isSelectable" />

          <!-- 审批标题 -->
          <el-table-column prop="title" label="审批标题" min-width="180">
            <template #default="{ row }">
              {{ getApprovalTitle(row) }}
            </template>
          </el-table-column>

          <!-- 目的地 -->
          <el-table-column prop="destination" label="目的地" min-width="120">
            <template #default="{ row }">
              <template v-if="row.businessTripRequest">
                {{ row.businessTripRequest.destination || '' }}
              </template>
            </template>
          </el-table-column>

          <!-- 开始日期 -->
          <el-table-column prop="startDate" label="开始日期" width="100">
            <template #default="{ row }">
              <template v-if="row.businessTripRequest && row.businessTripRequest.startDate">
                {{ formatDate(row.businessTripRequest.startDate) }}
              </template>
            </template>
          </el-table-column>

          <!-- 结束日期 -->
          <el-table-column prop="endDate" label="结束日期" width="100">
            <template #default="{ row }">
              <template v-if="row.businessTripRequest && row.businessTripRequest.endDate">
                {{ formatDate(row.businessTripRequest.endDate) }}
              </template>
            </template>
          </el-table-column>

          <!-- 状态 -->
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- 创建时间 -->
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleApprove(row)"
                >同意</el-button
              >
              <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleReject(row)"
                >拒绝</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <!-- 出差审批分页 -->
        <div v-if="getApprovalsByType('BUSINESS_TRIP').length > 0" class="pagination-container">
          <el-pagination
            v-model="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="getApprovalsTotalByType('BUSINESS_TRIP')"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty description="暂无出差审批" />
        </div>
      </el-tab-pane>

      <!-- 报销审批 -->
      <el-tab-pane label="报销审批" name="REIMBURSEMENT">
        <el-table
          ref="reimbursementApprovalTable"
          v-loading="loading"
          :data="getApprovalsByType('REIMBURSEMENT')"
          style="width: 100%"
          border
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          :row-style="{ height: '55px' }"
          @selection-change="handleSelectionChange"
        >
          <!-- 选择列 -->
          <el-table-column type="selection" width="55" :selectable="isSelectable" />

          <!-- 报销标题 -->
          <el-table-column prop="reimbursementRequest.title" label="报销标题" min-width="180">
            <template #default="{ row }">
              <template v-if="row.reimbursementRequest">
                {{ row.reimbursementRequest.title || '' }}
              </template>
            </template>
          </el-table-column>

          <!-- 报销金额 -->
          <el-table-column prop="reimbursementRequest.totalAmount" label="报销金额" width="120">
            <template #default="{ row }">
              <template v-if="row.reimbursementRequest && row.reimbursementRequest.totalAmount">
                <span class="amount-text">
                  {{ formatMoney(row.reimbursementRequest.totalAmount) }}
                </span>
              </template>
            </template>
          </el-table-column>

          <!-- 申请人 -->
          <el-table-column prop="reimbursementRequest.applicant" label="申请人" width="120">
            <template #default="{ row }">
              <template v-if="row.reimbursementRequest && row.reimbursementRequest.applicant">
                {{ row.reimbursementRequest.applicant.realName || row.reimbursementRequest.applicant.username }}
              </template>
            </template>
          </el-table-column>

          <!-- 项目 -->
          <el-table-column prop="reimbursementRequest.project" label="所属项目" width="150">
            <template #default="{ row }">
              <template v-if="row.reimbursementRequest && row.reimbursementRequest.project">
                {{ row.reimbursementRequest.project.name || '无' }}
              </template>
              <template v-else>无</template>
            </template>
          </el-table-column>

          <!-- 状态 -->
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- 创建时间 -->
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleApprove(row)"
                >同意</el-button
              >
              <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleReject(row)"
                >拒绝</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <!-- 报销审批分页 -->
        <div v-if="getApprovalsByType('REIMBURSEMENT').length > 0" class="pagination-container">
          <el-pagination
            v-model="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="getApprovalsTotalByType('REIMBURSEMENT')"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty description="暂无报销审批" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 报销详情弹框 -->
    <ReimbursementDetail
      v-if="detailDialogVisible"
      :visible="detailDialogVisible"
      :reimbursement="currentApprovalDetail"
      @close="closeDetailDialog"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios'
import { formatMoney, formatDate, formatReimbursementStatus, getReimbursementStatusTagType } from '../utils/format.js'

// 懒加载组件
const ReimbursementDetail = defineAsyncComponent(() => import('./ReimbursementDetail.vue'))

const router = useRouter()
const approvals = ref([])
const projects = ref([])
const loading = ref(false)
const approvalTable = ref(null)
const selectedApprovals = ref([])

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)

// TAB相关
const activeTab = ref('WORKTIME')

// 筛选表单
const filterForm = ref({
  dateRange: [],
  requestType: '',
  status: '',
  projectId: ''
})

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近三个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

// 根据筛选条件过滤审批列表
const filteredApprovals = computed(() => {
  let result = [...approvals.value]

  // 按审批类型筛选
  if (filterForm.value.requestType) {
    result = result.filter((item) => item.requestType === filterForm.value.requestType)
  }

  // 按状态筛选
  if (filterForm.value.status) {
    result = result.filter((item) => item.status === filterForm.value.status)
  }

  // 按日期范围筛选
  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    const startDate = new Date(filterForm.value.dateRange[0])
    const endDate = new Date(filterForm.value.dateRange[1])
    endDate.setHours(23, 59, 59, 999) // 设置为当天结束时间

    result = result.filter((item) => {
      const createTime = new Date(item.createTime)
      return createTime >= startDate && createTime <= endDate
    })
  }

  // 按项目筛选（仅工时审批）
  if (filterForm.value.projectId && filterForm.value.requestType === 'WORKTIME') {
    result = result.filter(
      (item) =>
        item.workTimeRecord &&
        item.workTimeRecord.project &&
        item.workTimeRecord.project.id === filterForm.value.projectId
    )
  }

  // 更新总数
  totalCount.value = result.length

  // 分页处理
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value

  return result.slice(startIndex, endIndex)
})

// 获取审批列表
const fetchApprovals = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {}

    if (filterForm.value.requestType) {
      params.requestType = filterForm.value.requestType
    }

    if (filterForm.value.status) {
      params.status = filterForm.value.status
    }

    if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
      params.startDate = filterForm.value.dateRange[0]
      params.endDate = filterForm.value.dateRange[1]
    }

    if (filterForm.value.projectId) {
      params.projectId = filterForm.value.projectId
    }

    // 获取当前用户需要审批的审批流程，带筛选条件
    const response = await api.get('/api/approval/my-approvals', { params })
    approvals.value = response.data
    totalCount.value = response.data.length
    console.log('审批数据:', response.data)

    // 输出详细信息便于调试
    if (response.data && response.data.length > 0) {
      response.data.forEach((approval, index) => {
        console.log(`审批 ${index + 1}:`, {
          id: approval.id,
          type: approval.requestType,
          status: approval.status,
          workTimeRecord: approval.workTimeRecord,
          leaveRequest: approval.leaveRequest,
          businessTripRequest: approval.businessTripRequest,
          reimbursementRequest: approval.reimbursementRequest
        })
      })
    } else {
      console.log('没有审批数据')
    }
  } catch (error) {
    console.error('获取审批列表失败:', error)
    ElMessage.error('获取审批列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 获取项目列表（用于筛选）
const fetchProjects = async () => {
  try {
    // 尝试获取项目列表
    const response = await api.get('/api/projects')
    projects.value = response.data
    console.log('成功获取项目列表:', projects.value.length, '个项目')
  } catch (error) {
    console.error('获取项目列表失败:', error)

    // 如果获取项目列表失败，尝试从审批列表中提取项目信息
    setTimeout(() => {
      if (approvals.value && approvals.value.length > 0) {
        const projectMap = new Map()

        // 从审批列表中提取项目信息
        approvals.value.forEach((approval) => {
          if (approval.requestType === 'WORKTIME' && approval.workTimeRecord && approval.workTimeRecord.project) {
            const project = approval.workTimeRecord.project
            if (project.id && project.name) {
              projectMap.set(project.id, project)
            }
          }
        })

        // 转换为数组
        projects.value = Array.from(projectMap.values())
        console.log('从审批列表中提取项目信息:', projects.value.length, '个项目')
      }
    }, 500) // 延迟执行，确保审批列表已加载
  }
}

const handleCreate = () => {
  // 显示选择对话框
  ElMessageBox.confirm('请选择要创建的审批类型', '创建审批', {
    confirmButtonText: '工时申请',
    cancelButtonText: '请假申请',
    distinguishCancelAndClose: true,
    type: 'info'
  })
    .then(() => {
      // 跳转到工时管理页面
      router.push('/time-management')
    })
    .catch((action) => {
      if (action === 'cancel') {
        // 跳转到请假管理页面
        router.push('/leave-management')
      }
    })
}

// 详情对话框状态
const detailDialogVisible = ref(false)
const currentApprovalDetail = ref(null)
const detailLoading = ref(false)

const handleView = async (approval) => {
  if (approval.requestType === 'REIMBURSEMENT' && approval.reimbursementRequest) {
    try {
      detailLoading.value = true
      detailDialogVisible.value = true

      // 获取详细的报销信息
      const response = await api.get(`/api/oa/reimbursement/${approval.reimbursementRequest.id}`)
      currentApprovalDetail.value = {
        ...(response.data.data || response.data),
        approvalInfo: approval
      }
    } catch (error) {
      console.error('获取报销详情失败:', error)
      ElMessage.error('获取报销详情失败: ' + (error.response?.data?.message || error.message))
      detailDialogVisible.value = false
    } finally {
      detailLoading.value = false
    }
  } else {
    ElMessage.warning('查看功能目前仅支持报销审批')
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentApprovalDetail.value = null
}

const handleApprove = async (approval) => {
  try {
    // 更新审批流程状态
    await api.put(`/api/approval/${approval.id}?status=APPROVED`)

    // 如果是工时审批，同时更新工时记录状态
    if (approval.requestType === 'WORKTIME' && approval.workTimeRecord && approval.workTimeRecord.id) {
      try {
        await api.put(`/api/worktime/approve/${approval.workTimeRecord.id}`)
        console.log('工时记录状态已更新，ID:', approval.workTimeRecord.id)
      } catch (workTimeError) {
        console.error('更新工时记录状态失败:', workTimeError)
      }
    }

    // 刷新审批列表
    await fetchApprovals()
    ElMessage.success('已通过审批')
  } catch (error) {
    console.error('审批操作失败:', error)
    ElMessage.error('操作失败: ' + error.message)
  }
}

const handleReject = async (approval) => {
  try {
    // 更新审批流程状态
    await api.put(`/api/approval/${approval.id}?status=REJECTED`)

    // 如果是工时审批，同时更新工时记录状态
    if (approval.requestType === 'WORKTIME' && approval.workTimeRecord && approval.workTimeRecord.id) {
      try {
        // 将工时记录的approved设置为false
        const workTimeRecord = approval.workTimeRecord
        workTimeRecord.approved = false
        await api.put(`/api/worktime/${workTimeRecord.id}`, workTimeRecord)
        console.log('工时记录状态已更新，ID:', workTimeRecord.id)
      } catch (workTimeError) {
        console.error('更新工时记录状态失败:', workTimeError)
      }
    }

    // 刷新审批列表
    await fetchApprovals()
    ElMessage.success('已拒绝审批')
  } catch (error) {
    console.error('审批操作失败:', error)
    ElMessage.error('操作失败: ' + error.message)
  }
}

// 批量审批通过
const handleBatchApprove = async () => {
  if (selectedApprovals.value.length === 0) {
    ElMessage.warning('请先选择要审批的记录')
    return
  }

  try {
    // 确认操作
    await ElMessageBox.confirm(`确定要批量通过选中的 ${selectedApprovals.value.length} 条审批记录吗？`, '批量审批', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    loading.value = true
    const pendingApprovals = selectedApprovals.value.filter((item) => item.status === 'PENDING')

    if (pendingApprovals.length === 0) {
      ElMessage.warning('选中的记录中没有待审批的记录')
      loading.value = false
      return
    }

    // 使用批量审批API
    const approvalIds = pendingApprovals.map((approval) => approval.id)
    const response = await api.post('/api/approval/batch-approve', approvalIds)
    console.log('批量审批响应:', response.data)

    // 刷新审批列表
    await fetchApprovals()
    ElMessage.success(`已批量通过 ${pendingApprovals.length} 条审批`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量审批操作失败:', error)
      ElMessage.error('批量审批失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

// 批量审批拒绝
const handleBatchReject = async () => {
  if (selectedApprovals.value.length === 0) {
    ElMessage.warning('请先选择要审批的记录')
    return
  }

  try {
    // 确认操作
    await ElMessageBox.confirm(`确定要批量拒绝选中的 ${selectedApprovals.value.length} 条审批记录吗？`, '批量审批', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    loading.value = true
    const pendingApprovals = selectedApprovals.value.filter((item) => item.status === 'PENDING')

    if (pendingApprovals.length === 0) {
      ElMessage.warning('选中的记录中没有待审批的记录')
      loading.value = false
      return
    }

    // 使用批量审批API
    const approvalIds = pendingApprovals.map((approval) => approval.id)
    const response = await api.post('/api/approval/batch-reject', approvalIds)
    console.log('批量拒绝响应:', response.data)

    // 刷新审批列表
    await fetchApprovals()
    ElMessage.success(`已批量拒绝 ${pendingApprovals.length} 条审批`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量审批操作失败:', error)
      ElMessage.error('批量审批失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

// 表格选择变更事件
const handleSelectionChange = (selection) => {
  selectedApprovals.value = selection
}

// 判断行是否可选择（只有待审批的记录可以选择）
const isSelectable = (row) => {
  return row.status === 'PENDING'
}

// 筛选处理
const handleFilter = () => {
  currentPage.value = 1 // 重置到第一页
}

// 重置筛选条件
const resetFilter = () => {
  filterForm.value = {
    dateRange: [],
    requestType: '',
    status: '',
    projectId: ''
  }
  currentPage.value = 1 // 重置到第一页
}

// 分页大小变更
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1 // 重置到第一页
}

// 当前页变更
const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 辅助函数，用于获取审批标题
const getApprovalTitle = (approval) => {
  if (approval.workTimeRecord) {
    return `工时审批 - ${approval.workTimeRecord.user?.realName || approval.workTimeRecord.user?.username || '未知用户'}`
  } else if (approval.leaveRequest) {
    return `请假审批 - ${approval.leaveRequest.applicant?.realName || approval.leaveRequest.applicant?.username || '未知用户'}`
  } else if (approval.businessTripRequest) {
    return `出差审批 - ${approval.businessTripRequest.applicant?.realName || approval.businessTripRequest.applicant?.username || '未知用户'}`
  } else if (approval.reimbursementRequest) {
    const title = approval.reimbursementRequest.title ? ` - ${approval.reimbursementRequest.title}` : ''
    return `报销审批 - ${approval.reimbursementRequest.applicant?.realName || approval.reimbursementRequest.applicant?.username || '未知用户'}${title}`
  } else {
    return '未知审批类型'
  }
}

// 辅助函数，用于获取请求类型文本
const getRequestTypeText = (requestType) => {
  const typeMap = {
    WORKTIME: '工时',
    LEAVE: '请假',
    BUSINESS_TRIP: '出差',
    REIMBURSEMENT: '报销'
  }
  return typeMap[requestType] || requestType
}

// 辅助函数，用于获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return statusMap[status] || status
}

// 辅助函数，用于获取状态标签类型
const getStatusTagType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return typeMap[status] || 'info'
}

// 辅助函数，用于格式化日期时间
const formatDateTime = (dateTimeStr) => {
  return formatDate(dateTimeStr, 'datetime')
}

// TAB相关函数
// 根据类型获取审批数据
const getApprovalsByType = (type) => {
  let result = approvals.value.filter((item) => item.requestType === type)

  // 应用筛选条件
  if (filterForm.value.status) {
    result = result.filter((item) => item.status === filterForm.value.status)
  }

  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    const startDate = new Date(filterForm.value.dateRange[0])
    const endDate = new Date(filterForm.value.dateRange[1])
    endDate.setHours(23, 59, 59, 999)

    result = result.filter((item) => {
      const createTime = new Date(item.createTime)
      return createTime >= startDate && createTime <= endDate
    })
  }

  if (filterForm.value.projectId && type === 'WORKTIME') {
    result = result.filter(
      (item) =>
        item.workTimeRecord &&
        item.workTimeRecord.project &&
        item.workTimeRecord.project.id === filterForm.value.projectId
    )
  }

  // 分页处理
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value
  return result.slice(startIndex, endIndex)
}

// 获取指定类型的审批总数
const getApprovalsTotalByType = (type) => {
  let result = approvals.value.filter((item) => item.requestType === type)

  // 应用筛选条件
  if (filterForm.value.status) {
    result = result.filter((item) => item.status === filterForm.value.status)
  }

  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    const startDate = new Date(filterForm.value.dateRange[0])
    const endDate = new Date(filterForm.value.dateRange[1])
    endDate.setHours(23, 59, 59, 999)

    result = result.filter((item) => {
      const createTime = new Date(item.createTime)
      return createTime >= startDate && createTime <= endDate
    })
  }

  if (filterForm.value.projectId && type === 'WORKTIME') {
    result = result.filter(
      (item) =>
        item.workTimeRecord &&
        item.workTimeRecord.project &&
        item.workTimeRecord.project.id === filterForm.value.projectId
    )
  }

  return result.length
}

// TAB切换事件
const handleTabChange = (tabName) => {
  activeTab.value = tabName
  currentPage.value = 1 // 重置分页
  selectedApprovals.value = [] // 清空选中

  // 如果切换到非工时类型，清除项目筛选
  if (tabName !== 'WORKTIME') {
    filterForm.value.projectId = ''
  }

  console.log('切换到TAB:', tabName)
}

onMounted(async () => {
  await fetchApprovals() // 先获取审批列表
  fetchProjects() // 然后获取项目列表用于筛选
})
</script>

<style scoped>
.approvals-container {
  padding: 20px;
}

.approvals-container h1 {
  margin-bottom: 20px;
}

.filter-container {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.filter-form .el-form-item {
  margin-right: 15px;
  margin-bottom: 10px;
}

/* 优化下拉框样式 */
.filter-form .el-select {
  width: 100%;
}

.filter-form .el-select .el-input__inner {
  width: 100%;
}

/* 日期选择器样式 */
.filter-form .el-date-editor.el-input {
  width: 100%;
}

/* 下拉框空状态文本样式 */
.select-empty-text {
  padding: 10px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.amount-text {
  font-weight: 600;
  color: #f56c6c;
}

.operation-container {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.approvals-container .el-table {
  margin-top: 10px;
}

/* 表格内容过长时显示省略号 */
.approvals-container .el-table .cell {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 确保表头和数据行对齐 */
.approvals-container .el-table th {
  background-color: #f5f7fa !important;
  color: #606266 !important;
  font-weight: bold;
  text-align: center;
}

.approvals-container .el-table td {
  padding: 8px 0;
}

/* 表格行高度固定 */
.approvals-container .el-table .el-table__row {
  height: 55px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* TAB样式优化 */
.approval-tabs {
  margin-top: 20px;
}

.approval-tabs .el-tabs__header {
  margin-bottom: 0;
}

.approval-tabs .el-tabs__nav-wrap::after {
  background-color: #e4e7ed;
}

.approval-tabs .el-tabs__item {
  padding: 0 24px;
  font-size: 16px;
  font-weight: 500;
}

.approval-tabs .el-tabs__item.is-active {
  color: #409eff;
  font-weight: 600;
}

/* 空状态样式 */
.empty-state {
  padding: 60px 0;
  text-align: center;
  background-color: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-top: 20px;
}

.empty-state .el-empty {
  padding: 20px 0;
}

.empty-state .el-empty__description {
  color: #909399;
  font-size: 14px;
}

/* TAB内容区域样式 */
.el-tab-pane {
  margin-top: 20px;
}

/* 表格容器样式 */
.approval-tabs .el-table {
  border-radius: 4px;
  overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-form {
    flex-direction: column;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 15px;
  }

  .operation-container {
    flex-direction: column;
    gap: 8px;
  }

  .operation-container .el-button {
    width: 100%;
  }

  .approval-tabs .el-tabs__item {
    padding: 0 12px;
    font-size: 14px;
  }
}
</style>
