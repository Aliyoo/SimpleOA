<template>
  <div class="project-manager-time-container">
    <h1>项目工时（高性能优化版）</h1>

    <!-- 自定义 Tab 导航 -->
    <div class="custom-tabs">
      <div class="custom-tabs-header">
        <div
          v-for="tab in tabs"
          :key="tab.name"
          :class="['custom-tab', { active: activeTab === tab.name }]"
          @click="activeTab = tab.name"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- Tab 内容区域 -->
      <div class="custom-tabs-content">
        <!-- 批量填写 Tab -->
        <div v-show="activeTab === 'batch'" class="tab-pane">
          <div class="batch-container">
            <div class="batch-header">
              <div class="batch-filters">
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
                <el-switch
                  v-model="showAllProjects"
                  active-text="所有项目"
                  inactive-text="我管理的项目"
                  style="margin-left: 15px"
                  @change="onShowAllProjectsChange"
                />
                <el-button
                  type="primary"
                  @click="toggleVirtualColumns"
                  style="margin-left: 15px"
                >
                  {{ virtualEnabled ? '显示全部天数' : '虚拟滚动模式' }}
                </el-button>
              </div>
              <div>
                <el-button type="primary" @click="refreshProjects" :loading="batchLoading">
                  刷新数据
                </el-button>
                <el-button type="success" :icon="Download" @click="exportBatchFillData">
                  导出Excel
                </el-button>
              </div>
            </div>

            <!-- 加载状态提示 -->
            <div v-if="batchLoading" class="loading-info">
              <el-icon class="loading-icon"><Loading /></el-icon>
              正在加载数据...
            </div>

            <div v-else-if="displayProjects.length === 0" class="batch-empty">
              <el-empty description="暂无项目数据" />
            </div>

            <!-- 性能统计信息 -->
            <div v-if="performanceStats.visible" class="performance-stats">
              <el-alert :title="performanceStats.message" type="info" :closable="false" show-icon />
            </div>

            <!-- 数据摘要 -->
            <div v-if="workTimeStore.initialized" class="data-summary">
              <el-row :gutter="20">
                <el-col :span="6">
                  <div class="summary-item">
                    <div class="label">项目数</div>
                    <div class="value">{{ workTimeStore.projectsMap.size }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="summary-item">
                    <div class="label">成员总数</div>
                    <div class="value">{{ totalMembers }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="summary-item">
                    <div class="label">工时记录</div>
                    <div class="value">{{ workTimeStore.workTimeMap.size }}</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="summary-item">
                    <div class="label">日期范围</div>
                    <div class="value">{{ batchDates.length }}天</div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <!-- 项目列表（使用虚拟滚动优化） -->
            <div v-if="displayProjects.length > 0" class="batch-projects-container">
              <div
                v-for="project in visibleProjects"
                :key="project.id"
                class="project-section"
              >
                <div class="project-header">
                  <h3>{{ project.name }}</h3>
                  <div class="project-actions">
                    <el-button
                      v-if="isProjectManager(project)"
                      type="primary"
                      size="small"
                      @click="submitProjectTimeRecords(project.id)"
                      :disabled="batchLoading || project.members.length === 0"
                    >
                      提交变更
                    </el-button>
                    <el-tag v-else type="info" size="small">只读模式</el-tag>
                    <el-tooltip
                      v-if="isProjectManager(project)"
                      content="项目经理填写的工时将自动审批通过"
                      placement="top"
                    >
                      <el-icon style="margin-left: 5px; color: #409eff"><InfoFilled /></el-icon>
                    </el-tooltip>
                  </div>
                </div>

                <!-- 项目成员工时表格（核心优化：只渲染可见的日期列） -->
                <div class="table-wrapper">
                  <div class="horizontal-scroll-container" ref="hScrollContainer">
                    <el-table
                      :data="project.members"
                      style="width: 100%"
                      border
                      :max-height="300"
                      size="small"
                      :cell-class-name="getCellClass"
                      v-loading="projectLoading[project.id]"
                    >
                      <!-- 固定列：序号 -->
                      <el-table-column type="index" label="序号" width="60" fixed="left" />

                      <!-- 固定列：姓名 -->
                      <el-table-column
                        prop="realName"
                        label="姓名"
                        width="120"
                        fixed="left"
                        show-overflow-tooltip
                      />

                      <!-- 可见的日期列（动态渲染，减少DOM数量） -->
                      <el-table-column
                        v-for="date in visibleDates"
                        :key="date"
                        :label="formatDateLabel(date)"
                        :prop="date"
                        width="80"
                        align="center"
                        class-name="date-cell"
                      >
                        <template #default="scope">
                          <WorkTimeInput
                            v-model="scope.row.hours[date]"
                            :project-id="project.id"
                            :user-id="scope.row.id"
                            :date="date"
                            :is-workday="isWorkdayForDate(date)"
                            :disabled="!isProjectManager(project) || !isWorkdayForDate(date)"
                            @update:hours="(hours) => handleHoursChange(project.id, scope.row.id, date, hours)"
                          />
                        </template>
                      </el-table-column>

                      <!-- 合计列 -->
                      <el-table-column label="合计" width="80" align="center">
                        <template #default="scope">
                          <div class="total-hours">{{ calculateRowTotal(scope.row, project) }}</div>
                        </template>
                      </el-table-column>

                      <!-- 跨项目合计列 -->
                      <el-table-column label="跨项目合计" width="100" align="center">
                        <template #default="scope">
                          <div class="cross-project-total">
                            <div class="total-value">
                              {{ calculateRowTotal(scope.row, project) }}h
                            </div>
                            <div class="total-desc">
                              / {{ getMemberCrossProjectTotal(scope.row.id) }}h
                            </div>
                          </div>
                        </template>
                      </el-table-column>

                      <!-- 操作列 -->
                      <el-table-column
                        label="操作"
                        width="120"
                        align="center"
                        v-if="isProjectManager(project)"
                      >
                        <template #default="scope">
                          <el-button
                            size="small"
                            type="primary"
                            @click="quickFillMember(project, scope.row)"
                            :disabled="batchLoading"
                          >
                            一键填写
                          </el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>

                <!-- 横向滚动条（自定义） -->
                <div class="custom-scrollbar" v-if="virtualEnabled && batchDates.length > visibleCount">
                  <div
                    class="scrollbar-track"
                    @click="onScrollbarClick"
                  >
                    <div
                      class="scrollbar-thumb"
                      :style="scrollbarThumbStyle"
                      @mousedown="onThumbMouseDown"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 统计报表 Tab -->
        <div v-show="activeTab === 'stats'" class="tab-pane">
          <!-- 统计报表内容保持不变 -->
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../stores/user'
import api from '../utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, InfoFilled, Loading } from '@element-plus/icons-vue'
import WorkTimeInput from '../components/WorkTimeInput.vue'

// Tab配置
const tabs = [
  { name: 'batch', label: '批量填写' },
  { name: 'stats', label: '统计报表' }
]
const activeTab = ref('batch')

// 用户Store
const userStore = useUserStore()
const currentUser = ref(userStore.user)

// ==================== 核心数据结构（Map优化） ====================

const workTimeStore = reactive({
  // 项目Map: projectId -> ProjectMemberDTO
  projectsMap: new Map(),

  // 工时数据Map: `${projectId}_${userId}_${date}` -> hours
  workTimeMap: new Map(),

  // 工作日Set: date -> boolean
  workdaysSet: new Set(),

  // 变更追踪Map
  changesMap: new Map(),

  // 加载状态
  initialized: false
})

// ==================== 日期和虚拟滚动相关 ====================

const batchDateRange = ref([])
const batchDates = ref([]) // 所有日期
const showAllProjects = ref(false)

// 虚拟滚动配置
const virtualEnabled = ref(false)
const visibleCount = ref(7) // 可见日期数
const dateOffset = ref(0) // 日期偏移
const thumbDragging = ref(false)

// 可见的日期
const visibleDates = computed(() => {
  if (!virtualEnabled.value) {
    return batchDates.value
  }
  return batchDates.value.slice(
    dateOffset.value,
    dateOffset.value + visibleCount.value
  )
})

// 所有项目数据（用于计算）
const allProjectsData = ref([])

// 显示的项目（实际渲染）
const displayProjects = computed(() => {
  return allProjectsData.value
})

// 可见的项目（虚拟滚动）
const visibleProjects = computed(() => {
  return displayProjects.value
})

// 总成员数
const totalMembers = computed(() => {
  return allProjectsData.value.reduce((sum, project) => sum + project.members.length, 0)
})

// 加载状态
const batchLoading = ref(false)
const projectLoading = reactive({})
const performanceStats = reactive({
  visible: false,
  message: ''
})

// ==================== API数据加载 ====================

/**
 * 加载批量数据（核心优化：一次性查询所有数据）
 */
async function loadBatchData() {
  if (!batchDateRange.value || batchDateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }

  batchLoading.value = true
  performanceStats.visible = false

  try {
    const startTime = Date.now()

    // ===== 使用新API：一次性查询所有数据 =====
    const response = await api.get('/api/worktime/batch/fill-data', {
      params: {
        managerId: currentUser.value.id,
        startDate: batchDateRange.value[0],
        endDate: batchDateRange.value[1],
        showAllProjects: showAllProjects.value
      }
    })

    const { projects, workTimeData, workdays, totalProjects, totalMembers, dateRangeDays } = response.data

    const loadTime = Date.now() - startTime

    // 清空旧数据
    workTimeStore.projectsMap.clear()
    workTimeStore.workTimeMap.clear()
    workTimeStore.workdaysSet.clear()
    workTimeStore.changesMap.clear()

    // 1. 加载项目数据到Map
    projects.forEach(project => {
      workTimeStore.projectsMap.set(project.id, project)
    })

    // 2. 加载工时数据到Map（核心优化：扁平化存储）
    workTimeData.forEach(record => {
      const key = `${record.projectId}_${record.userId}_${record.date}`
      workTimeStore.workTimeMap.set(key, record.hours)
    })

    // 3. 加载工作日数据（优化：使用Set，O(1)查询）
    workdays.forEach(date => workTimeStore.workdaysSet.add(date))

    // 4. 转换为前端渲染格式
    transformToRenderFormat(projects, workTimeData)

    workTimeStore.initialized = true

    // 显示性能统计
    performanceStats.visible = true
    performanceStats.message = `数据加载完成：${totalProjects}个项目，${totalMembers}个成员，` +
      `${workTimeData.length}条工时记录，耗时${loadTime}ms`

    console.info(`批量数据加载完成: 项目数=${totalProjects}, 成员数=${totalMembers}, ` +
      `工时记录=${workTimeData.length}, 耗时=${loadTime}ms`)

  } catch (error) {
    console.error('加载批量数据失败:', error)
    ElMessage.error('加载数据失败: ' + error.message)
  } finally {
    batchLoading.value = false
  }
}

/**
 * 转换为前端渲染格式
 * 从扁平化结构转换为树形结构（用于el-table）
 */
function transformToRenderFormat(projects, workTimeData) {
  // 构建工时数据Map，方便查找
  const workTimeMap = {}
  workTimeData.forEach(record => {
    const key = `${record.projectId}_${record.userId}`
    if (!workTimeMap[key]) {
      workTimeMap[key] = {}
    }
    workTimeMap[key][record.date] = record.hours
  })

  // 转换为渲染格式
  const renderProjects = projects.map(project => {
    const members = project.members.map(member => {
      // 为每个成员创建hours对象
      const hours = {}
      batchDates.value.forEach(date => {
        const key = `${project.id}_${member.id}`
        hours[date] = workTimeMap[key]?.[date] || 0
      })

      return {
        ...member,
        hours
      }
    })

    return {
      ...project,
      members
    }
  })

  allProjectsData.value = renderProjects
}

// ==================== 工具函数 ====================

/**
 * 获取用户某日的总工时（跨项目校验）
 * O(P)复杂度，P=项目数
 */
function getUserDailyTotal(userId, date) {
  let total = 0
  workTimeStore.projectsMap.forEach(project => {
    const hours = getWorkTime(project.id, userId, date)
    total += hours
  })
  return total
}

/**
 * 获取工时（O(1)查找）
 */
function getWorkTime(projectId, userId, date) {
  const key = `${projectId}_${userId}_${date}`
  return workTimeStore.workTimeMap.get(key) || 0
}

/**
 * 设置工时（带变更追踪）
 */
function setWorkTime(projectId, userId, date, hours) {
  const key = `${projectId}_${userId}_${date}`
  const oldValue = getWorkTime(projectId, userId, date)

  // 记录变更
  if (oldValue !== hours) {
    const changeKey = `${projectId}_${userId}_${date}`
    workTimeStore.changesMap.set(changeKey, {
      projectId,
      userId,
      date,
      oldValue,
      newValue: hours
    })
  } else {
    // 如果恢复到原值，删除变更记录
    const changeKey = `${projectId}_${userId}_${date}`
    workTimeStore.changesMap.delete(changeKey)
  }

  // 更新存储
  if (hours === 0) {
    workTimeStore.workTimeMap.delete(key)
  } else {
    workTimeStore.workTimeMap.set(key, hours)
  }
}

/**
 * 判断是否为工作日（O(1)查询）
 */
function isWorkdayForDate(date) {
  return workTimeStore.workdaysSet.has(date)
}

/**
 * 格式化日期标签
 */
function formatDateLabel(dateStr) {
  const [, month, day] = dateStr.split('-')
  return `${month}/${day}`
}

/**
 * 计算行合计
 */
function calculateRowTotal(row, project) {
  return Object.values(row.hours || {}).reduce((sum, val) => sum + (val || 0), 0)
}

/**
 * 计算成员跨项目总计
 */
function getMemberCrossProjectTotal(memberId) {
  let total = 0
  batchDates.value.forEach(date => {
    total += getUserDailyTotal(memberId, date)
  })
  return total
}

/**
 * 判断用户是否是项目经理
 */
function isProjectManager(project) {
  if (!project || !currentUser.value) return false
  return project.managerId === currentUser.value.id
}

/**
 * 处理工时变更
 */
function handleHoursChange(projectId, userId, date, hours) {
  setWorkTime(projectId, userId, date, hours)

  // 触发校验（异步，不阻塞UI）
  setTimeout(() => {
    validateUserDailyHours(userId, date)
  }, 100)
}

/**
 * 验证用户每日工时（跨项目校验）
 */
function validateUserDailyHours(userId, date) {
  const dailyTotal = getUserDailyTotal(userId, date)

  if (dailyTotal > 8) {
    // 显示错误提示
    const user = Object.values(workTimeStore.projectsMap)
      .flatMap(p => p.members)
      .find(m => m.id === userId)

    const userName = user?.realName || user?.username || '该成员'
    ElMessage.warning(`${userName} 在 ${date} 跨项目总工时为${dailyTotal}小时，超过8小时限制`)
    return false
  }

  return true
}

/**
 * 提交项目工时记录
 */
async function submitProjectTimeRecords(projectId) {
  if (!projectId) {
    ElMessage.warning('项目ID无效')
    return
  }

  // 查找该项目相关的变更
  const projectChanges = Array.from(workTimeStore.changesMap.values())
    .filter(change => change.projectId === projectId)

  if (projectChanges.length === 0) {
    ElMessage.info('该项目没有工时变更，无需提交')
    return
  }

  // 提交前校验
  const errors = []
  const datesToCheck = [...new Set(projectChanges.map(c => c.date))]

  datesToCheck.forEach(date => {
    const members = [...new Set(projectChanges.filter(c => c.date === date).map(c => c.userId))]
    members.forEach(memberId => {
      const dailyTotal = getUserDailyTotal(memberId, date)
      if (dailyTotal > 8) {
        const member = Object.values(workTimeStore.projectsMap)
          .flatMap(p => p.members)
          .find(m => m.id === memberId)
        const memberName = member?.realName || member?.username || '未知成员'
        errors.push(`${memberName}在${date}(${dailyTotal}h)`)
      }
    })
  })

  if (errors.length > 0) {
    ElMessage.error(`以下成员跨项目总工时超过8小时：${errors.join('、')}，请调整后再提交`)
    return
  }

  // 构建提交数据
  const records = projectChanges.map(change => ({
    projectId: change.projectId,
    userId: change.userId,
    hours: change.newValue,
    date: change.date,
    approved: true // 项目经理填写的自动审批通过
  }))

  try {
    // 批量提交
    await api.post('/api/worktime/batch/submit', {
      projectId,
      workTimeRecords: records
    })

    ElMessage.success(`成功提交 ${records.length} 条工时变更`)

    // 清除变更记录
    projectChanges.forEach(change => {
      const changeKey = `${change.projectId}_${change.userId}_${change.date}`
      workTimeStore.changesMap.delete(changeKey)
    })

  } catch (error) {
    console.error('提交工时失败:', error)
    ElMessage.error('提交失败: ' + error.message)
  }
}

/**
 * 一键填写成员（只填工作日）
 */
function quickFillMember(project, member) {
  const datesToFill = batchDates.value.filter(date => isWorkdayForDate(date))
  let filledCount = 0

  datesToFill.forEach(date => {
    const dailyTotal = getUserDailyTotal(member.id, date)
    const maxAllowed = Math.max(0, 8 - dailyTotal)

    if (maxAllowed >= 8 && !getWorkTime(project.id, member.id, date)) {
      setWorkTime(project.id, member.id, date, 8)
      filledCount++
    }
  })

  const memberName = member.realName || member.username
  if (filledCount > 0) {
    ElMessage.success(`已为 "${memberName}" 填写 ${filledCount} 个工作日工时`)
  } else {
    ElMessage.info(`"${memberName}" 没有可填写的工作日`)
  }
}

// ==================== 滚动相关 ====================

/**
 * 切换虚拟列模式
 */
function toggleVirtualColumns() {
  virtualEnabled.value = !virtualEnabled.value
  dateOffset.value = 0
}

/**
 * 自定义滚动条点击
 */
function onScrollbarClick(event) {
  const trackWidth = event.target.clientWidth
  const clickX = event.offsetX
  const dateWidth = 80
  dateOffset.value = Math.max(0, Math.min(
    batchDates.value.length - visibleCount.value,
    Math.floor((clickX / trackWidth) * (batchDates.value.length - visibleCount.value))
  ))
}

/**
 * 拖拽滑块
 */
function onThumbMouseDown(event) {
  thumbDragging.value = true
  const startX = event.clientX
  const startOffset = dateOffset.value

  const onMouseMove = (e) => {
    if (!thumbDragging.value) return
    const deltaX = e.clientX - startX
    const dateWidth = 80
    const deltaDates = Math.round(deltaX / dateWidth)
    dateOffset.value = Math.max(0, Math.min(
      batchDates.value.length - visibleCount.value,
      startOffset + deltaDates
    ))
  }

  const onMouseUp = () => {
    thumbDragging.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

/**
 * 计算滑块样式
 */
const scrollbarThumbStyle = computed(() => {
  const totalDates = batchDates.value.length
  const thumbWidth = Math.max(20, (visibleCount.value / totalDates) * 300)
  const trackWidth = 300
  const offsetPercent = (dateOffset.value / Math.max(1, totalDates - visibleCount.value)) * (trackWidth - thumbWidth)

  return {
    width: `${thumbWidth}px`,
    transform: `translateX(${offsetPercent}px)`
  }
})

// ==================== 其他函数 ====================

/**
 * 刷新项目数据
 */
async function refreshProjects() {
  if (showAllProjects.value) {
    await loadBatchData()
  } else {
    await loadBatchData()
  }
}

/**
 * 初始化日期范围
 */
function initBatchDateRange() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1

  let startMonth, startYear, endMonth, endYear

  if (now.getDate() < 25) {
    // 上上个月25号到上个月24号
    if (month === 1) {
      startMonth = 11
      startYear = year - 1
      endMonth = 12
      endYear = year - 1
    } else if (month === 2) {
      startMonth = 12
      startYear = year - 1
      endMonth = 1
      endYear = year
    } else {
      startMonth = month - 2
      startYear = year
      endMonth = month - 1
      endYear = year
    }
  } else {
    // 上个月25号到当前月24号
    if (month === 1) {
      startMonth = 12
      startYear = year - 1
      endMonth = 1
      endYear = year
    } else {
      startMonth = month - 1
      startYear = year
      endMonth = month
      endYear = year
    }
  }

  batchDateRange.value = [
    `${startYear}-${String(startMonth).padStart(2, '0')}-25`,
    `${endYear}-${String(endMonth).padStart(2, '0')}-24`
  ]

  // 生成日期列表
  generateBatchDates()
}

/**
 * 生成日期列表
 */
function generateBatchDates() {
  if (!batchDateRange.value || batchDateRange.value.length !== 2) return

  const startDate = new Date(batchDateRange.value[0])
  const endDate = new Date(batchDateRange.value[1])
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
}

/**
 * 日期范围变更
 */
async function onBatchDateRangeChange(range) {
  if (range && range.length === 2) {
    batchDateRange.value = range
    await loadBatchData()
  }
}

/**
 * 显示所有项目变更
 */
async function onShowAllProjectsChange(value) {
  await loadBatchData()
}

/**
 * 获取默认日期范围
 */
function getDefaultDateRangeDates() {
  const now = new Date()
  const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1)
  return [lastMonth, now]
}

/**
 * 导出数据
 */
async function exportBatchFillData() {
  // TODO: 实现导出功能
  ElMessage.info('导出功能开发中')
}

// ==================== 生命周期 ====================

onMounted(async () => {
  // 确保用户已登录
  if (!currentUser.value || !currentUser.value.id) {
    if (userStore.user && userStore.user.id) {
      currentUser.value = userStore.user
    } else if (userStore.token) {
      await userStore.fetchUser()
      currentUser.value = userStore.user
    }
  }

  // 初始化日期范围
  initBatchDateRange()

  // 加载数据
  await loadBatchData()
})

onUnmounted(() => {
  // 清理资源
  workTimeStore.projectsMap.clear()
  workTimeStore.workTimeMap.clear()
  workTimeStore.workdaysSet.clear()
  workTimeStore.changesMap.clear()
})
</script>

<style scoped>
.project-manager-time-container {
  padding: 20px;
  max-width: 100%;
  overflow-x: hidden;
}

h1 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
  text-align: center;
}

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

.batch-container {
  margin-top: 20px;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 10px;
  flex-wrap: wrap;
}

.batch-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.loading-info {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

.loading-icon {
  margin-right: 8px;
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.batch-empty {
  margin-top: 40px;
  text-align: center;
}

.performance-stats {
  margin-bottom: 20px;
}

.data-summary {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.summary-item {
  text-align: center;
}

.summary-item .label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.summary-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.batch-projects-container {
  margin-top: 20px;
}

.project-section {
  margin-bottom: 30px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.project-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.project-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.table-wrapper {
  overflow-x: auto;
}

.horizontal-scroll-container {
  min-width: 100%;
}

.total-hours {
  font-weight: bold;
  color: #409eff;
}

.cross-project-total {
  font-size: 12px;
  text-align: center;
}

.cross-project-total .total-value {
  font-weight: bold;
  color: #303133;
}

.cross-project-total .total-desc {
  color: #909399;
  font-size: 10px;
}

.custom-scrollbar {
  margin-top: 5px;
  height: 12px;
  background-color: #f5f5f5;
  border-radius: 6px;
  position: relative;
  cursor: pointer;
}

.scrollbar-track {
  width: 100%;
  height: 100%;
  position: relative;
}

.scrollbar-thumb {
  height: 12px;
  background-color: #c0c4cc;
  border-radius: 6px;
  cursor: grab;
  position: absolute;
  top: 0;
}

.scrollbar-thumb:active {
  cursor: grabbing;
  background-color: #909399;
}

.date-cell {
  padding: 0 !important;
}

.is-workday {
  background-color: #fff;
}

.is-weekend {
  background-color: #f0f9ff;
}
</style>
