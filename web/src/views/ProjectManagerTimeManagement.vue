<template>
  <div class="project-manager-time-container">
    <h1>项目工时审核</h1>
    
    <!-- 项目列表 -->
    <div class="projects-overview">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>我负责的项目</span>
            <el-button type="primary" @click="refreshProjects" :loading="projectsLoading">
              刷新数据
            </el-button>
          </div>
        </template>
        
        <div class="projects-grid">
          <div 
            v-for="project in manageableProjects" 
            :key="project.id"
            class="project-card"
            @click="selectProject(project)"
            :class="{ 'selected': selectedProject?.id === project.id }"
          >
            <div class="project-header">
              <h3>{{ project.name }}</h3>
              <el-tag :type="getProjectStatusTagType(project.status)">
                {{ getProjectStatusText(project.status) }}
              </el-tag>
            </div>
            
            <div class="project-stats">
              <div class="stat-row">
                <span class="stat-label">待审核工时：</span>
                <span class="stat-value pending">{{ project.pendingHours || 0 }} 小时</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">已审核工时：</span>
                <span class="stat-value approved">{{ project.approvedHours || 0 }} 小时</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">本月总工时：</span>
                <span class="stat-value total">{{ (project.pendingHours || 0) + (project.approvedHours || 0) }} 小时</span>
              </div>
            </div>
            
            <div class="project-actions">
              <el-button 
                type="primary" 
                size="small"
                @click.stop="goToAudit(project)"
                :disabled="(project.pendingHours || 0) === 0"
              >
                审核工时 ({{ project.pendingHours || 0 }})
              </el-button>
            </div>
          </div>
        </div>
        
        <div v-if="manageableProjects.length === 0 && !projectsLoading" class="empty-projects">
          <el-empty description="您暂未负责任何项目" />
        </div>
      </el-card>
    </div>

    <!-- 工时审核详情 -->
    <div v-if="selectedProject" class="audit-details">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>{{ selectedProject.name }} - 工时审核</span>
            <div class="header-actions">
              <el-date-picker
                v-model="auditDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="onAuditDateRangeChange"
                style="width: 300px; margin-right: 10px;"
              />
              <el-button @click="batchApprove" type="success" :disabled="selectedRecords.length === 0">
                批量通过 ({{ selectedRecords.length }})
              </el-button>
              <el-button @click="batchReject" type="warning" :disabled="selectedRecords.length === 0">
                批量驳回 ({{ selectedRecords.length }})
              </el-button>
            </div>
          </div>
        </template>

        <!-- 筛选和搜索 -->
        <div class="audit-filters">
          <el-form :inline="true" :model="auditSearchForm" class="filter-form">
            <el-form-item label="员工">
              <el-select v-model="auditSearchForm.userId" placeholder="选择员工" clearable style="width: 200px;">
                <el-option
                  v-for="member in projectMembers"
                  :key="member.id"
                  :label="member.firstName + ' ' + member.lastName"
                  :value="member.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="auditSearchForm.status" placeholder="选择状态" clearable style="width: 150px;">
                <el-option label="待审核" value="pending" />
                <el-option label="已通过" value="approved" />
                <el-option label="已驳回" value="rejected" />
              </el-select>
            </el-form-item>
            <el-form-item label="工作类型">
              <el-select v-model="auditSearchForm.workType" placeholder="选择类型" clearable style="width: 150px;">
                <el-option label="开发" value="开发" />
                <el-option label="测试" value="测试" />
                <el-option label="设计" value="设计" />
                <el-option label="文档" value="文档" />
                <el-option label="会议" value="会议" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchAuditRecords">查询</el-button>
              <el-button @click="resetAuditSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 工时记录表格 -->
        <el-table
          :data="auditRecords"
          style="width: 100%"
          v-loading="auditLoading"
          @selection-change="onSelectionChange"
          :row-key="row => row.id"
          stripe
        >
          <el-table-column type="selection" width="55" />
          <el-table-column label="员工" width="120">
            <template #default="scope">
              {{ scope.row.user?.firstName + ' ' + scope.row.user?.lastName }}
            </template>
          </el-table-column>
          <el-table-column prop="date" label="工作日期" width="120" sortable />
          <el-table-column prop="hours" label="工时" width="80" align="center" sortable />
          <el-table-column prop="workType" label="工作类型" width="100" />
          <el-table-column prop="description" label="工作内容" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getAuditStatusTagType(scope.row.approved, scope.row.rejected)">
                {{ getAuditStatusText(scope.row.approved, scope.row.rejected) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button
                size="small"
                type="success"
                @click="approveRecord(scope.row)"
                v-if="!scope.row.approved && !scope.row.rejected"
              >
                通过
              </el-button>
              <el-button
                size="small"
                type="warning"
                @click="rejectRecord(scope.row)"
                v-if="!scope.row.approved && !scope.row.rejected"
              >
                驳回
              </el-button>
              <el-button
                size="small"
                type="info"
                @click="viewRecordDetail(scope.row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            :current-page="auditPagination.currentPage"
            :page-size="auditPagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="auditPagination.total"
            @size-change="handleAuditSizeChange"
            @current-change="handleAuditCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="recordDetailVisible" title="工时记录详情" width="50%">
      <div v-if="selectedRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="员工">
            {{ selectedRecord.user?.firstName + ' ' + selectedRecord.user?.lastName }}
          </el-descriptions-item>
          <el-descriptions-item label="项目">{{ selectedProject?.name }}</el-descriptions-item>
          <el-descriptions-item label="工作日期">{{ selectedRecord.date }}</el-descriptions-item>
          <el-descriptions-item label="工时">{{ selectedRecord.hours }} 小时</el-descriptions-item>
          <el-descriptions-item label="工作类型">{{ selectedRecord.workType }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getAuditStatusTagType(selectedRecord.approved, selectedRecord.rejected)">
              {{ getAuditStatusText(selectedRecord.approved, selectedRecord.rejected) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(selectedRecord.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审核时间" v-if="selectedRecord.auditTime">
            {{ formatDateTime(selectedRecord.auditTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="工作内容" :span="2">
            {{ selectedRecord.description }}
          </el-descriptions-item>
          <el-descriptions-item label="驳回原因" :span="2" v-if="selectedRecord.rejected && selectedRecord.rejectReason">
            {{ selectedRecord.rejectReason }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="recordDetailVisible = false">关闭</el-button>
          <el-button 
            type="success" 
            @click="approveRecord(selectedRecord)" 
            v-if="selectedRecord && !selectedRecord.approved && !selectedRecord.rejected"
          >
            通过
          </el-button>
          <el-button 
            type="warning" 
            @click="rejectRecord(selectedRecord)" 
            v-if="selectedRecord && !selectedRecord.approved && !selectedRecord.rejected"
          >
            驳回
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../stores/user'
import api from '../utils/axios.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const currentUser = ref(userStore.user)

// 响应式数据
const manageableProjects = ref([])
const selectedProject = ref(null)
const auditRecords = ref([])
const projectMembers = ref([])
const selectedRecords = ref([])
const selectedRecord = ref(null)

// 加载状态
const projectsLoading = ref(false)
const auditLoading = ref(false)

// 对话框状态
const recordDetailVisible = ref(false)

// 日期范围
const auditDateRange = ref([])

// 搜索表单
const auditSearchForm = reactive({
  userId: '',
  status: '',
  workType: ''
})

// 分页数据
const auditPagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 计算属性
const pendingCount = computed(() => {
  return auditRecords.value.filter(record => !record.approved && !record.rejected).length
})

// 方法定义
const refreshProjects = async () => {
  await fetchManageableProjects()
  if (selectedProject.value) {
    await fetchAuditRecords()
  }
}

const fetchManageableProjects = async () => {
  if (!currentUser.value?.id) {
    ElMessage.warning('未获取到当前用户信息')
    return
  }

  projectsLoading.value = true
  try {
    // 获取当前用户作为项目经理的项目
    const response = await api.get(`/api/projects/manager/${currentUser.value.id}`)
    manageableProjects.value = response.data || []

    // 为每个项目获取工时统计
    for (const project of manageableProjects.value) {
      await fetchProjectWorkTimeStats(project)
    }
  } catch (error) {
    console.error('获取可管理项目失败:', error)
    ElMessage.error('获取项目列表失败: ' + error.message)
  } finally {
    projectsLoading.value = false
  }
}

const fetchProjectWorkTimeStats = async (project) => {
  try {
    // 获取当前月的工时统计
    const now = new Date()
    const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
    const endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    const endDateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(endDate.getDate()).padStart(2, '0')}`

    const [pendingResponse, approvedResponse] = await Promise.all([
      api.get(`/api/worktime/project/range`, {
        params: {
          projectId: project.id,
          startDate,
          endDate: endDateStr,
          approved: false
        }
      }),
      api.get(`/api/worktime/project/range`, {
        params: {
          projectId: project.id,
          startDate,
          endDate: endDateStr,
          approved: true
        }
      })
    ])

    const pendingRecords = Array.isArray(pendingResponse.data) ? pendingResponse.data : []
    const approvedRecords = Array.isArray(approvedResponse.data) ? approvedResponse.data : []

    project.pendingHours = pendingRecords.reduce((sum, record) => sum + (record.hours || 0), 0)
    project.approvedHours = approvedRecords.reduce((sum, record) => sum + (record.hours || 0), 0)
  } catch (error) {
    console.error(`获取项目 ${project.name} 工时统计失败:`, error)
    project.pendingHours = 0
    project.approvedHours = 0
  }
}

const selectProject = (project) => {
  selectedProject.value = project
  resetAuditSearch()
  initAuditDateRange()
  fetchProjectMembers()
  fetchAuditRecords()
}

const goToAudit = (project) => {
  selectProject(project)
}

const initAuditDateRange = () => {
  const now = new Date()
  const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  const endDateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(endDate.getDate()).padStart(2, '0')}`
  
  auditDateRange.value = [startDate, endDateStr]
}

const fetchProjectMembers = async () => {
  if (!selectedProject.value) return

  try {
    const response = await api.get(`/api/projects/${selectedProject.value.id}/members`)
    projectMembers.value = response.data || []
  } catch (error) {
    console.error('获取项目成员失败:', error)
    projectMembers.value = []
  }
}

const fetchAuditRecords = async () => {
  if (!selectedProject.value) return

  auditLoading.value = true
  try {
    const params = {
      projectId: selectedProject.value.id,
      page: auditPagination.currentPage - 1,
      size: auditPagination.pageSize
    }

    if (auditDateRange.value && auditDateRange.value.length === 2) {
      params.startDate = auditDateRange.value[0]
      params.endDate = auditDateRange.value[1]
    }

    if (auditSearchForm.userId) {
      params.userId = auditSearchForm.userId
    }

    if (auditSearchForm.status) {
      if (auditSearchForm.status === 'pending') {
        params.approved = false
      } else if (auditSearchForm.status === 'approved') {
        params.approved = true
      }
    }

    if (auditSearchForm.workType) {
      params.workType = auditSearchForm.workType
    }

    const response = await api.get('/api/worktime/project/range', { params })
    
    if (Array.isArray(response.data)) {
      auditRecords.value = response.data
      auditPagination.total = response.data.length
    } else if (response.data?.content) {
      auditRecords.value = response.data.content
      auditPagination.total = response.data.totalElements || 0
    } else {
      auditRecords.value = []
      auditPagination.total = 0
    }

    // 按日期降序排序
    auditRecords.value.sort((a, b) => new Date(b.date) - new Date(a.date))
  } catch (error) {
    console.error('获取工时审核记录失败:', error)
    ElMessage.error('获取工时记录失败: ' + error.message)
    auditRecords.value = []
  } finally {
    auditLoading.value = false
  }
}

const onAuditDateRangeChange = () => {
  auditPagination.currentPage = 1
  fetchAuditRecords()
}

const searchAuditRecords = () => {
  auditPagination.currentPage = 1
  fetchAuditRecords()
}

const resetAuditSearch = () => {
  auditSearchForm.userId = ''
  auditSearchForm.status = ''
  auditSearchForm.workType = ''
  auditPagination.currentPage = 1
  if (selectedProject.value) {
    fetchAuditRecords()
  }
}

const onSelectionChange = (selection) => {
  selectedRecords.value = selection
}

const approveRecord = async (record) => {
  try {
    await api.put(`/api/worktime/approve/${record.id}`)
    ElMessage.success('工时审核通过')
    record.approved = true
    record.auditTime = new Date().toISOString()
    
    // 刷新项目统计
    await fetchProjectWorkTimeStats(selectedProject.value)
  } catch (error) {
    console.error('审核工时失败:', error)
    ElMessage.error('审核工时失败: ' + error.message)
  }
}

const rejectRecord = async (record) => {
  try {
    const reason = await ElMessageBox.prompt('请输入驳回原因', '驳回工时', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入驳回原因'
    })

    await api.put(`/api/worktime/reject/${record.id}`, { reason: reason.value })
    ElMessage.success('工时已驳回')
    record.rejected = true
    record.rejectReason = reason.value
    record.auditTime = new Date().toISOString()
    
    // 刷新项目统计
    await fetchProjectWorkTimeStats(selectedProject.value)
  } catch (error) {
    if (error === 'cancel') return
    console.error('驳回工时失败:', error)
    ElMessage.error('驳回工时失败: ' + error.message)
  }
}

const batchApprove = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请选择要审核的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量通过选中的 ${selectedRecords.value.length} 条工时记录吗？`,
      '批量审核',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const promises = selectedRecords.value.map(record => 
      api.put(`/api/worktime/approve/${record.id}`)
    )
    
    await Promise.all(promises)
    ElMessage.success(`成功批量通过 ${selectedRecords.value.length} 条工时记录`)
    
    // 更新记录状态
    selectedRecords.value.forEach(record => {
      record.approved = true
      record.auditTime = new Date().toISOString()
    })
    
    selectedRecords.value = []
    await fetchProjectWorkTimeStats(selectedProject.value)
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量审核失败:', error)
    ElMessage.error('批量审核失败: ' + error.message)
  }
}

const batchReject = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请选择要驳回的记录')
    return
  }

  try {
    const reason = await ElMessageBox.prompt('请输入批量驳回原因', '批量驳回', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入驳回原因'
    })

    const promises = selectedRecords.value.map(record => 
      api.put(`/api/worktime/reject/${record.id}`, { reason: reason.value })
    )
    
    await Promise.all(promises)
    ElMessage.success(`成功批量驳回 ${selectedRecords.value.length} 条工时记录`)
    
    // 更新记录状态
    selectedRecords.value.forEach(record => {
      record.rejected = true
      record.rejectReason = reason.value
      record.auditTime = new Date().toISOString()
    })
    
    selectedRecords.value = []
    await fetchProjectWorkTimeStats(selectedProject.value)
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量驳回失败:', error)
    ElMessage.error('批量驳回失败: ' + error.message)
  }
}

const viewRecordDetail = (record) => {
  selectedRecord.value = record
  recordDetailVisible.value = true
}

const handleAuditSizeChange = (size) => {
  auditPagination.pageSize = size
  fetchAuditRecords()
}

const handleAuditCurrentChange = (page) => {
  auditPagination.currentPage = page
  fetchAuditRecords()
}

// 辅助方法
const getProjectStatusTagType = (status) => {
  const statusMap = {
    'PLANNING': 'info',
    'IN_PROGRESS': 'success',
    'COMPLETED': '',
    'ON_HOLD': 'warning',
    'CANCELLED': 'danger'
  }
  return statusMap[status] || 'info'
}

const getProjectStatusText = (status) => {
  const statusMap = {
    'PLANNING': '规划中',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'ON_HOLD': '暂停',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || '未知'
}

const getAuditStatusTagType = (approved, rejected) => {
  if (approved) return 'success'
  if (rejected) return 'danger'
  return 'warning'
}

const getAuditStatusText = (approved, rejected) => {
  if (approved) return '已通过'
  if (rejected) return '已驳回'
  return '待审核'
}

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  fetchManageableProjects()
  initAuditDateRange()
})
</script>

<style scoped>
.project-manager-time-container {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.projects-overview {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.project-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.project-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409EFF;
}

.project-card.selected {
  border-color: #409EFF;
  background: #f0f9ff;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.project-header h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
}

.project-stats {
  margin-bottom: 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-value {
  font-weight: 600;
  font-size: 16px;
}

.stat-value.pending {
  color: #E6A23C;
}

.stat-value.approved {
  color: #67C23A;
}

.stat-value.total {
  color: #409EFF;
}

.project-actions {
  text-align: center;
}

.empty-projects {
  text-align: center;
  padding: 60px 0;
}

.audit-details {
  background: white;
  border-radius: 8px;
}

.audit-filters {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.filter-form {
  margin: 0;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

@media (max-width: 768px) {
  .projects-grid {
    grid-template-columns: 1fr;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
}
</style>
