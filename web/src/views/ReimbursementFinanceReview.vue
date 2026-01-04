<template>
  <div class="finance-review-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>财务审查</span>
          <div>
            <el-button type="success" size="small" @click="handleExport">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
            <el-button type="primary" size="small" @click="loadData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 快速统计面板 -->
      <QuickStats :stats="statistics" @filter-by-status="handleStatFilter" />

      <!-- 高级筛选 -->
      <AdvancedFilter @search="handleSearch" @reset="handleReset" />

      <!-- 快捷标签 -->
      <QuickTags @tag-click="handleTagClick" />

      <!-- 数据表格 -->
      <el-table
        :data="requests"
        border
        stripe
        v-loading="loading"
        style="width: 100%; margin-top: 20px"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="报销标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="applicant.realName" label="申请人" width="120" align="center" />
        <el-table-column prop="project.name" label="关联项目" width="150" align="center" />
        <el-table-column prop="totalAmount" label="金额(元)" width="120" align="right">
          <template #default="scope">
            <span style="color: #f56c6c; font-weight: bold;">¥{{ scope.row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="140" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleViewDetail(scope.row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <!-- 只有待财务审查状态才显示审查按钮 -->
            <template v-if="scope.row.status === 'PENDING_FINANCE_REVIEW'">
              <el-button
                size="small"
                type="success"
                @click="handleReview(scope.row, 'APPROVE')"
                :loading="scope.row.reviewing">
                <el-icon><Select /></el-icon>
                通过
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="handleReview(scope.row, 'REJECT')"
                :loading="scope.row.reviewing">
                <el-icon><Close /></el-icon>
                拒绝
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; text-align: right" />
    </el-card>

    <!-- 审查意见对话框 -->
    <el-dialog v-model="dialogVisible" title="审查意见" width="500px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审查决定">
          <el-tag :type="reviewForm.decision === 'APPROVE' ? 'success' : 'danger'">
            {{ reviewForm.decision === 'APPROVE' ? '审查通过' : '审查拒绝' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审查意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审查意见(拒绝时必填)"
            maxlength="500"
            show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReview" :loading="submitting">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <DetailDialog v-model:visible="detailVisible" :data="currentRecord" @close="detailVisible = false" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Select, Close, View, Download } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import QuickStats from '@/components/finance/QuickStats.vue'
import AdvancedFilter from '@/components/finance/AdvancedFilter.vue'
import QuickTags from '@/components/finance/QuickTags.vue'
import DetailDialog from '@/components/finance/DetailDialog.vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const requests = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 统计数据
const statistics = ref({
  pendingReview: { count: 0, amount: 0 },
  pendingLeader: { count: 0, amount: 0 },
  approved: { count: 0, amount: 0 },
  rejected: { count: 0, amount: 0 },
  total: { count: 0, amount: 0 },
  averageAmount: 0
})

const reviewForm = ref({
  decision: '',
  comment: '',
  requestId: null
})

const currentRecord = ref(null)

// 筛选参数
const filterParams = ref({})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      ...filterParams.value
    }

    const response = await axios.get('/api/oa/reimbursement/finance-review-pending', { params })

    if (response.data.code === 200) {
      requests.value = response.data.data.content || []
      total.value = response.data.data.totalElements || 0

      // 添加reviewing状态
      requests.value.forEach(req => {
        req.reviewing = false
      })
    } else {
      ElMessage.error(response.data.message || '加载数据失败')
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const response = await axios.get('/api/oa/reimbursement/finance-statistics')

    if (response.data.code === 200) {
      const data = response.data.data

      statistics.value = {
        pendingReview: {
          count: data.pendingReviewCount || 0,
          amount: data.pendingReviewAmount || 0
        },
        pendingLeader: {
          count: data.pendingLeaderCount || 0,
          amount: data.pendingLeaderAmount || 0
        },
        approved: {
          count: data.approvedCount || 0,
          amount: data.approvedAmount || 0
        },
        rejected: {
          count: data.rejectedCount || 0,
          amount: data.rejectedAmount || 0
        },
        total: {
          count: data.totalCount || 0,
          amount: data.totalAmount || 0
        },
        averageAmount: data.averageAmount || 0
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 处理统计卡片点击
const handleStatFilter = (status) => {
  if (status === 'all') {
    filterParams.value = {}
  } else {
    filterParams.value = {
      status: [status]
    }
  }
  currentPage.value = 1
  loadData()
}

// 处理高级筛选查询
const handleSearch = (params) => {
  filterParams.value = params
  currentPage.value = 1
  loadData()
}

// 处理重置
const handleReset = () => {
  filterParams.value = {}
  currentPage.value = 1
  loadData()
}

// 处理快捷标签点击
const handleTagClick = (tag) => {
  const params = {}

  switch (tag.key) {
    case 'all':
      filterParams.value = {}
      break
    case 'pending':
      filterParams.value = { status: ['PENDING_FINANCE_REVIEW'] }
      break
    case 'thisWeek':
      const weekStart = getWeekStartDate()
      const weekEnd = new Date().toISOString().split('T')[0]
      filterParams.value = { startDate: weekStart, endDate: weekEnd }
      break
    case 'thisMonth':
      const monthStart = getMonthStartDate()
      const monthEnd = new Date().toISOString().split('T')[0]
      filterParams.value = { startDate: monthStart, endDate: monthEnd }
      break
    case 'largeAmount':
      filterParams.value = { minAmount: 5000 }
      break
    case 'overdue':
      const overdueDate = getOverdueDate()
      filterParams.value = { endDate: overdueDate, status: ['PENDING_FINANCE_REVIEW'] }
      break
  }

  currentPage.value = 1
  loadData()
}

// 获取本周开始日期
const getWeekStartDate = () => {
  const now = new Date()
  const day = now.getDay()
  const diff = now.getDate() - day + (day === 0 ? -6 : 1)
  const monday = new Date(now.setDate(diff))
  return monday.toISOString().split('T')[0]
}

// 获取本月开始日期
const getMonthStartDate = () => {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0]
}

// 获取逾期日期(3天前)
const getOverdueDate = () => {
  const now = new Date()
  now.setDate(now.getDate() - 3)
  return now.toISOString().split('T')[0]
}

// 处理查看详情
const handleViewDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
}

// 处理审查操作
const handleReview = async (request, decision) => {
  reviewForm.value = {
    decision,
    comment: decision === 'APPROVE' ? '财务审查通过' : '',
    requestId: request.id
  }
  dialogVisible.value = true
}

// 确认审查
const confirmReview = async () => {
  if (reviewForm.value.decision === 'REJECT' && !reviewForm.value.comment.trim()) {
    ElMessage.warning('请填写拒绝理由')
    return
  }

  submitting.value = true
  try {
    const response = await axios.post(
      `/api/oa/reimbursement/${reviewForm.value.requestId}/finance-review`,
      {
        decision: reviewForm.value.decision,
        comment: reviewForm.value.comment
      }
    )

    if (response.data.code === 200) {
      ElMessage.success('财务审查完成')
      dialogVisible.value = false
      loadData()
      loadStatistics()
    } else {
      ElMessage.error(response.data.message || '审查失败')
    }
  } catch (error) {
    console.error('财务审查失败:', error)
    ElMessage.error('审查失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

// 处理导出
const handleExport = async () => {
  try {
    ElMessage.info('导出功能开发中...')
    // TODO: 实现导出功能
  } catch (error) {
    console.error('导出失败:', error)
  }
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'DRAFT': '草稿',
    'PENDING_MANAGER_APPROVAL': '待项目经理审批',
    'PENDING_LEADER_APPROVAL': '待领导审批',
    'PENDING_FINANCE_REVIEW': '待财务审查',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return statusMap[status] || status
}

// 获取状态标签类型
const getStatusType = (status) => {
  const typeMap = {
    'DRAFT': 'info',
    'PENDING_MANAGER_APPROVAL': 'warning',
    'PENDING_LEADER_APPROVAL': 'warning',
    'PENDING_FINANCE_REVIEW': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return typeMap[status] || 'info'
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadData()
}

// 当前页改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  loadData()
}

onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped>
.finance-review-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
