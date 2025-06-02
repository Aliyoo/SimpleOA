<template>
  <div class="project-manager-time-container">
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
                  style="margin-left: 15px;"
                  @change="onShowAllProjectsChange"
                />
              </div>
              <div>
                <el-button type="primary" @click="refreshProjects">刷新数据</el-button>
                <el-button type="success" :icon="Download" @click="exportBatchFillData">导出Excel</el-button>
              </div>
            </div>

            <div class="batch-projects-container" v-loading="batchLoading">
              <!-- 项目列表 -->
              <div v-for="project in displayProjects" :key="project.id" class="project-section">
                <div class="project-header">
                  <h3>{{ project.name }}</h3>
                  <div class="project-actions">
                    <el-button
                      v-if="isProjectManager(project)"
                      type="primary"
                      size="small"
                      @click="submitProjectTimeRecords(project.id)"
                    >
                      提交
                    </el-button>
                    <el-tag v-else type="info" size="small">只读模式</el-tag>
                  </div>
                </div>

                <!-- 项目成员工时表格 -->
                <el-table
                  :data="project.members"
                  style="width: 100%"
                  border
                  :max-height="300"
                  :cell-class-name="getCellClass"
                >
                  <!-- 固定列 -->
                  <el-table-column type="index" label="序号" width="60" fixed="left" />
                  <el-table-column prop="username" label="用户名" width="120" fixed="left" />
                  <el-table-column prop="realName" label="姓名" width="120" fixed="left" />

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
                        :disabled="!isProjectManager(project)"
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

                <div class="project-empty" v-if="project.members.length === 0">
                  <el-empty description="该项目暂无成员" />
                </div>
              </div>

              <div class="batch-empty" v-if="displayProjects.length === 0 && !batchLoading">
                <el-empty description="暂无项目数据" />
              </div>
            </div>
          </div>
        </div>

        <!-- 统计报表 Tab -->
        <div v-show="activeTab === 'stats'" class="tab-pane">
          <div class="stats-container">
            <div class="stats-header">
              <div class="stats-filters">
                <el-date-picker
                  v-model="statsDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :default-value="getDefaultDateRangeDates()"
                  @change="onStatsDateRangeChange"
                />
              </div>
              <div>
                <el-button type="primary" @click="fetchProjectsStats">刷新统计</el-button>
                <el-button type="success" :icon="Download" @click="exportStatisticalReportData">导出Excel</el-button>
              </div>
            </div>

            <div class="stats-content" v-loading="statsLoading">
              <!-- 总体统计卡片 -->
              <div class="stats-overview">
                <el-row :gutter="20">
                  <el-col :span="6">
                    <el-card shadow="hover" class="stats-card">
                      <template #header>
                        <div class="card-header">
                          <span>总工时</span>
                        </div>
                      </template>
                      <div class="stats-value">{{ overallStats.totalHours || 0 }}</div>
                    </el-card>
                  </el-col>
                  <el-col :span="6">
                    <el-card shadow="hover" class="stats-card">
                      <template #header>
                        <div class="card-header">
                          <span>记录数</span>
                        </div>
                      </template>
                      <div class="stats-value">{{ overallStats.recordCount || 0 }}</div>
                    </el-card>
                  </el-col>
                  <el-col :span="6">
                    <el-card shadow="hover" class="stats-card">
                      <template #header>
                        <div class="card-header">
                          <span>平均工时/记录</span>
                        </div>
                      </template>
                      <div class="stats-value">{{ formatNumber(overallStats.averageHoursPerRecord) }}</div>
                    </el-card>
                  </el-col>
                  <el-col :span="6">
                    <el-card shadow="hover" class="stats-card">
                      <template #header>
                        <div class="card-header">
                          <span>加班工时</span>
                        </div>
                      </template>
                      <div class="stats-value">{{ formatNumber(overallStats.overtimeHours) }}</div>
                    </el-card>
                  </el-col>
                </el-row>
              </div>

              <!-- 项目统计表格 -->
              <div class="stats-projects">
                <h3>项目工时统计</h3>
                <el-table
                  :data="projectsStats"
                  style="width: 100%"
                  border
                  :max-height="400"
                >
                  <el-table-column type="index" label="序号" width="60" />
                  <el-table-column prop="projectName" label="项目名称" min-width="200" />
                  <el-table-column prop="totalHours" label="总工时" width="100" align="center" />
                  <el-table-column prop="percentage" label="占比" width="100" align="center" />
                  <el-table-column label="操作" width="120" align="center">
                    <template #default="scope">
                      <el-button
                        type="primary"
                        size="small"
                        @click="viewProjectDetail(scope.row.projectId)"
                      >
                        详情
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <!-- 项目成员工时统计 -->
              <div class="stats-members">
                <h3>项目成员工时统计</h3>
                <el-table
                  :data="allMemberStats"
                  style="width: 100%"
                  border
                  :max-height="400"
                >
                  <el-table-column type="index" label="序号" width="60" />
                  <el-table-column prop="projectName" label="项目名称" min-width="150" />
                  <el-table-column prop="username" label="用户名" width="120" />
                  <el-table-column prop="realName" label="姓名" width="120" />
                  <el-table-column prop="totalHours" label="总工时" width="100" align="center" />
                  <el-table-column prop="recordCount" label="记录数" width="100" align="center" />
                  <el-table-column prop="averageHours" label="平均工时/记录" width="120" align="center" />
                  <el-table-column prop="percentage" label="项目内占比" width="120" align="center" />
                </el-table>
              </div>

              <!-- 项目详情对话框 -->
              <el-dialog
                v-model="projectDetailVisible"
                title="项目工时详情"
                width="80%"
                destroy-on-close
              >
                <div v-loading="projectDetailLoading">
                  <div v-if="currentProjectDetail.projectName" class="project-detail-header">
                    <h2>{{ currentProjectDetail.projectName }}</h2>
                    <div class="project-detail-summary">
                      <div class="detail-item">
                        <span class="label">总工时:</span>
                        <span class="value">{{ currentProjectDetail.totalHours }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">记录数:</span>
                        <span class="value">{{ currentProjectDetail.recordCount }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">平均工时/记录:</span>
                        <span class="value">{{ formatNumber(currentProjectDetail.averageHoursPerRecord) }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">加班工时:</span>
                        <span class="value">{{ formatNumber(currentProjectDetail.overtimeHours) }}</span>
                      </div>
                    </div>
                  </div>

                  <!-- 成员工时统计 -->
                  <div class="member-stats">
                    <h3>成员工时统计</h3>
                    <el-table
                      :data="memberStats"
                      style="width: 100%"
                      border
                      :max-height="300"
                    >
                      <el-table-column type="index" label="序号" width="60" />
                      <el-table-column prop="username" label="用户名" width="120" />
                      <el-table-column prop="realName" label="姓名" width="120" />
                      <el-table-column prop="totalHours" label="总工时" width="100" align="center" />
                      <el-table-column prop="percentage" label="占比" width="100" align="center" />
                      <el-table-column prop="recordCount" label="记录数" width="100" align="center" />
                      <el-table-column prop="averageHours" label="平均工时/记录" width="120" align="center" />
                    </el-table>
                  </div>
                </div>
              </el-dialog>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { APP_CONFIG } from '../utils/config.js'
import { ref, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { Download } from '@element-plus/icons-vue'

// 定义 tabs 数组，用于自定义 tabs 导航
const tabs = [
  { name: 'batch', label: '批量填写' },
  { name: 'stats', label: '统计报表' }
]

const activeTab = ref('batch')
const userStore = useUserStore()
const currentUser = ref(userStore.user)
const managedProjects = ref([])
const allProjects = ref([])
const showAllProjects = ref(false)
const displayProjects = ref([])
const batchDateRange = ref([])
const batchDates = ref([])
const batchLoading = ref(false)

// 统计报表相关数据
const statsDateRange = ref([])
const statsLoading = ref(false)
const overallStats = ref({
  totalHours: 0,
  recordCount: 0,
  averageHoursPerRecord: 0,
  overtimeHours: 0,
  workload: 0
})
const projectsStats = ref([])
const projectDetailVisible = ref(false)
const projectDetailLoading = ref(false)
const currentProjectDetail = ref({})
const memberStats = ref([])
const allMemberStats = ref([])

// 获取项目经理管理的项目列表
const fetchManagedProjects = async () => {
  if (!currentUser.value || !currentUser.value.id) {
    if (userStore.user && userStore.user.id) {
      currentUser.value = userStore.user
    } else {
      ElMessage.warning('未获取到当前用户信息')
      return
    }
  }

  try {
    batchLoading.value = true
    const response = await api.get(`/api/projects/manager/${currentUser.value.id}`)
    managedProjects.value = response.data
    console.log('获取到管理的项目列表:', managedProjects.value)

    // 更新显示的项目列表
    updateDisplayProjects()
  } catch (error) {
    console.error('获取管理的项目列表失败:', error)
    ElMessage.error('获取管理的项目列表失败: ' + error.message)
    batchLoading.value = false
  }
}

// 获取所有项目列表
const fetchAllProjects = async () => {
  if (!currentUser.value || !currentUser.value.id) {
    if (userStore.user && userStore.user.id) {
      currentUser.value = userStore.user
    } else {
      ElMessage.warning('未获取到当前用户信息')
      return
    }
  }

  try {
    batchLoading.value = true
    const response = await api.get('/api/projects')
    allProjects.value = response.data
    console.log('获取到所有项目列表:', allProjects.value)

    // 更新显示的项目列表
    updateDisplayProjects()
  } catch (error) {
    console.error('获取所有项目列表失败:', error)
    ElMessage.error('获取所有项目列表失败: ' + error.message)
    batchLoading.value = false
  }
}

// 更新显示的项目列表
const updateDisplayProjects = async () => {
  try {
    // 根据开关状态决定显示哪些项目
    const projects = showAllProjects.value ? allProjects.value : managedProjects.value

    if (!projects || projects.length === 0) {
      displayProjects.value = []
      batchLoading.value = false
      return
    }

    // 为每个项目获取成员并初始化工时数据
    const projectsWithMembers = []

    for (const project of projects) {
      try {
        // 获取项目成员
        const response = await api.get(`/api/projects/${project.id}/members`)
        const members = response.data

        // 为每个成员初始化工时数据
        const membersWithHours = members.map(member => {
          const hours = {}
          batchDates.value.forEach(date => {
            hours[date] = 0
          })

          return {
            ...member,
            hours
          }
        })

        // 添加到项目列表
        projectsWithMembers.push({
          ...project,
          members: membersWithHours
        })
      } catch (error) {
        console.error(`获取项目 ${project.id} 成员列表失败:`, error)
        // 添加到项目列表，但成员为空
        projectsWithMembers.push({
          ...project,
          members: []
        })
      }
    }

    // 更新显示的项目列表
    displayProjects.value = projectsWithMembers
    console.log('更新显示的项目列表:', displayProjects.value)

    // 加载已提交的工时数据
    loadSubmittedWorkTime()
  } finally {
    batchLoading.value = false
  }
}

// 切换显示所有项目或仅管理的项目
const onShowAllProjectsChange = (value) => {
  console.log('切换项目显示模式:', value ? '所有项目' : '我管理的项目')

  if (value && allProjects.value.length === 0) {
    // 如果切换到显示所有项目，但还没有获取过所有项目列表，则获取
    fetchAllProjects()
  } else {
    // 否则直接更新显示的项目列表
    updateDisplayProjects()
  }
}

// 刷新项目数据
const refreshProjects = () => {
  if (showAllProjects.value) {
    fetchAllProjects()
  } else {
    fetchManagedProjects()
  }
}

// 初始化批量日期范围
const initBatchDateRange = () => {
  console.log('初始化批量日期范围，当前值:', batchDateRange.value)

  // 检查日期范围是否有效
  let needsReset = true

  if (batchDateRange.value && batchDateRange.value.length === 2) {
    try {
      const startDate = new Date(batchDateRange.value[0])
      const endDate = new Date(batchDateRange.value[1])

      if (!isNaN(startDate.getTime()) && !isNaN(endDate.getTime()) && startDate <= endDate) {
        console.log('当前批量日期范围有效，无需重置')
        needsReset = false
      } else {
        console.warn('当前批量日期范围无效，需要重置')
      }
    } catch (error) {
      console.error('解析批量日期范围时出错:', error)
    }
  } else {
        console.warn('批量日期范围不完整，需要重置')
      }

      if (needsReset) {
        // 获取默认日期范围
        const defaultRange = getDefaultDateRange()
        console.log('获取到的默认日期范围:', defaultRange)

        if (defaultRange && defaultRange.length === 2) {
          batchDateRange.value = defaultRange
          console.log('已重置批量日期范围为默认值:', batchDateRange.value)
        } else {
          // 如果默认日期范围无效，则使用当前月的日期范围
          const now = new Date()
          const year = now.getFullYear()
          const month = now.getMonth() + 1

          // 上个月的25号到当前月的24号
          let startMonth, startYear, endMonth, endYear

          if (now.getDate() < 25) {
            // 如果当前日期小于25号，则是上上个月25号到上个月24号
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
            // 如果当前日期大于等于25号，则是上个月25号到当前月24号
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

          console.log('已设置批量日期范围为上月25日至本月24日:', batchDateRange.value)
        }
      }

      // 生成批量表格
      generateBatchTable()
    }

    // 生成批量表格
    const generateBatchTable = () => {
      console.log('generateBatchTable 被调用，日期范围:', batchDateRange.value)

      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        console.warn('批量填写日期范围无效:', batchDateRange.value)
        ElMessage.warning('请选择日期范围')
        return
      }

      batchLoading.value = true
      console.log('开始生成批量表格，日期范围:', batchDateRange.value[0], '至', batchDateRange.value[1])

      try {
        // 确保日期格式正确
        const startDate = new Date(batchDateRange.value[0])
        const endDate = new Date(batchDateRange.value[1])

        console.log('解析后的日期对象:', startDate, endDate)

        if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
          console.error('日期格式无效:', batchDateRange.value)
          ElMessage.warning('日期格式无效，请重新选择')
          batchLoading.value = false
          return
        }

        if (startDate > endDate) {
          console.error('开始日期晚于结束日期:', startDate, endDate)
          ElMessage.warning('开始日期不能晚于结束日期')
          batchLoading.value = false
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
        console.log('生成的日期列表:', batchDates.value)

        // 根据当前显示模式获取项目列表
        if (showAllProjects.value) {
          // 显示所有项目
          if (allProjects.value.length === 0) {
            fetchAllProjects()
          } else {
            updateDisplayProjects()
          }
        } else {
          // 显示管理的项目
          if (managedProjects.value.length === 0) {
            fetchManagedProjects()
          } else {
            updateDisplayProjects()
          }
        }
      } catch (error) {
        console.error('生成批量表格失败:', error)
        ElMessage.error('生成批量表格失败: ' + error.message)
        batchLoading.value = false
      }
    }

    // 加载已提交的工时数据
    const loadSubmittedWorkTime = async () => {
      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        console.warn('批量填写日期范围无效，无法加载已提交工时')
        return
      }

      if (!displayProjects.value || displayProjects.value.length === 0) {
        console.warn('没有项目数据，无法加载已提交工时')
        return
      }

      try {
        batchLoading.value = true
        console.log('加载已提交工时，日期范围:', batchDateRange.value[0], '至', batchDateRange.value[1])

        // 基本查询参数
        const baseParams = {
          startDate: batchDateRange.value[0],
          endDate: batchDateRange.value[1],
          _t: new Date().getTime(), // 添加时间戳参数，避免浏览器缓存
          size: 1000 // 设置较大的分页大小，确保能获取到所有记录
        }

        // 为每个项目获取工时记录
        for (const project of displayProjects.value) {
          try {
            // 构建查询参数
            const params = {
              ...baseParams,
              projectId: project.id
            }

            // 调用API获取工时记录
            const response = await api.get('/api/worktime/project/range', { params })

            // 处理返回的数据
            let workTimeRecords = []
            if (Array.isArray(response.data)) {
              workTimeRecords = response.data
            } else if (response.data && response.data.content) {
              workTimeRecords = response.data.content
            }

            console.log(`获取到项目 ${project.id} 工时记录:`, workTimeRecords.length, '条')

            // 将已提交的工时数据填充到项目成员表格中
            if (workTimeRecords.length > 0) {
              // 创建一个映射，用于存储已提交的工时记录
              const submittedHoursMap = {}

              // 处理已提交的工时记录
              workTimeRecords.forEach(record => {
                const userId = record.user?.id
                const date = record.date
                const hours = record.hours

                if (userId && date && hours) {
                  if (!submittedHoursMap[userId]) {
                    submittedHoursMap[userId] = {}
                  }
                  submittedHoursMap[userId][date] = hours
                }
              })

              // 填充已提交的工时数据
              project.members.forEach(member => {
                const userId = member.id
                const submittedHours = submittedHoursMap[userId] || {}

                batchDates.value.forEach(date => {
                  // 如果该日期有已提交的工时记录，则更新
                  if (submittedHours[date]) {
                    member.hours[date] = submittedHours[date]
                  }
                })
              })
            }
          } catch (error) {
            console.error(`获取项目 ${project.id} 工时记录失败:`, error)
          }
        }
      } catch (error) {
        console.error('加载已提交工时失败:', error)
        ElMessage.error('加载已提交工时失败: ' + error.message)
      } finally {
        batchLoading.value = false
      }
    }

    // 提交指定项目的工时记录
    const submitProjectTimeRecords = async (projectId) => {
      if (!projectId) {
        ElMessage.warning('项目ID无效')
        return
      }

      // 查找项目
      const project = displayProjects.value.find(p => p.id === projectId)
      if (!project) {
        ElMessage.warning('找不到指定的项目')
        return
      }

      const records = []
      project.members.forEach(member => {
        Object.entries(member.hours).forEach(([date, hours]) => {
          if (hours > 0) {
            records.push({
              project: { id: projectId },
              user: { id: member.id },
              date,
              hours,
              workType: '开发', // 默认值，可根据需求调整
              description: '项目经理批量填写',
              approved: true // 项目经理填写的工时自动审批通过
            })
          }
        })
      })

      if (records.length === 0) {
        ElMessage.warning('没有需要提交的工时记录')
        return
      }

      try {
        batchLoading.value = true
        await api.post('/api/worktime/batch', records)
        ElMessage.success(`成功提交 ${records.length} 条工时记录`)
        // 加载已提交的工时数据
        loadSubmittedWorkTime()
      } catch (error) {
        console.error('批量提交工时失败:', error)
        ElMessage.error('批量提交工时失败: ' + error.message)
      } finally {
        batchLoading.value = false
      }
    }

    // 从全局配置获取默认日期范围（字符串格式）
    const getDefaultDateRange = () => {
      try {
        return APP_CONFIG?.DEFAULT_DATE_RANGE?.getRange() || []
      } catch (error) {
        console.error('获取默认日期范围失败:', error)
        return []
      }
    }

    // 从全局配置获取默认日期范围（Date对象格式）
    const getDefaultDateRangeDates = () => {
      try {
        return APP_CONFIG?.DEFAULT_DATE_RANGE?.getRangeDates() || [new Date(), new Date()]
      } catch (error) {
        console.error('获取默认日期范围(Date对象)失败:', error)
        return [new Date(), new Date()]
      }
    }

    const formatDateLabel = (dateStr) => {
      const [, month, day] = dateStr.split('-')
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

    const getCellClass = ({ columnIndex }) => {
      if (columnIndex > 2 && columnIndex < batchDates.value.length + 3) {
        const date = batchDates.value[columnIndex - 3]
        const day = new Date(date).getDay()
        if (day === 0 || day === 6) {
          return 'weekend-cell'
        }
      }
      return ''
    }

    const onBatchDateRangeChange = (range) => {
      if (range && range.length === 2) {
        batchDateRange.value = range
        generateBatchTable()
      }
    }

    // 统计报表相关方法
    const initStatsDateRange = () => {
      console.log('初始化统计日期范围')

      // 获取默认日期范围
      const defaultRange = getDefaultDateRange()

      if (defaultRange && defaultRange.length === 2) {
        statsDateRange.value = defaultRange
      } else {
        // 如果默认日期范围无效，则使用当前月的日期范围
        const now = new Date()
        const year = now.getFullYear()
        const month = now.getMonth() + 1

        // 上个月的25号到当前月的24号
        let startMonth, startYear, endMonth, endYear

        if (now.getDate() < 25) {
          // 如果当前日期小于25号，则是上上个月25号到上个月24号
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
          // 如果当前日期大于等于25号，则是上个月25号到当前月24号
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

        statsDateRange.value = [
          `${startYear}-${String(startMonth).padStart(2, '0')}-25`,
          `${endYear}-${String(endMonth).padStart(2, '0')}-24`
        ]
      }
    }

    const onStatsDateRangeChange = (range) => {
      if (range && range.length === 2) {
        statsDateRange.value = range
        fetchProjectsStats()
      }
    }

    // 获取项目经理管理的所有项目的工时统计数据
    const fetchProjectsStats = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        ElMessage.warning('未获取到当前用户信息')
        return
      }

      if (!statsDateRange.value || statsDateRange.value.length !== 2) {
        ElMessage.warning('请选择日期范围')
        return
      }

      try {
        statsLoading.value = true

        // 获取项目经理管理的项目列表
        if (managedProjects.value.length === 0) {
          await fetchManagedProjects()
        }

        if (managedProjects.value.length === 0) {
          ElMessage.warning('未找到您管理的项目')
          statsLoading.value = false
          return
        }

        // 获取所有项目的工时统计数据
        const startDate = statsDateRange.value[0]
        const endDate = statsDateRange.value[1]

        // 构建查询参数
        const params = {
          startDate,
          endDate,
          _t: new Date().getTime() // 添加时间戳参数，避免浏览器缓存
        }

        // 调用API获取所有项目的工时统计数据
        const response = await api.get('/api/worktime/projects/stats', { params })

        // 处理返回的数据
        let stats = {}
        if (response.data && response.data.data) {
          stats = response.data.data
        } else if (response.data) {
          stats = response.data
        }

        // 处理项目工时数据
        const projectHours = stats.projects || {}

        // 过滤出项目经理管理的项目
        const managedProjectIds = managedProjects.value.map(p => p.id)

        // 只保留项目经理管理的项目的工时数据
        const managedProjectHours = {}
        let managedTotalHours = 0

        // 从所有项目中筛选出管理的项目，并计算总工时
        Object.entries(projectHours).forEach(([projectId, hours]) => {
          const projectIdNum = parseInt(projectId)
          if (managedProjectIds.includes(projectIdNum)) {
            managedProjectHours[projectId] = hours
            managedTotalHours += hours
          }
        })

        // 构建项目统计数据
        projectsStats.value = Object.entries(managedProjectHours)
          .map(([projectId, hours]) => {
            const project = managedProjects.value.find(p => p.id === parseInt(projectId)) || { name: '未知项目' }
            return {
              projectId: parseInt(projectId),
              projectName: project.name,
              totalHours: hours,
              percentage: managedTotalHours > 0 ? ((hours / managedTotalHours) * 100).toFixed(2) + '%' : '0%'
            }
          })

        // 按工时降序排序
        projectsStats.value.sort((a, b) => b.totalHours - a.totalHours)

        // 更新总体统计数据，只包含项目经理管理的项目
        let managedRecordCount = 0
        let managedOvertimeHours = 0
        let managedAverageHoursPerRecord = 0
        let managedWorkload = 0

        // 获取管理项目的详细统计数据
        for (const project of managedProjects.value) {
          try {
            // 获取项目详情统计数据
            const projectResponse = await api.get(`/api/worktime/project/${project.id}/stats`, {
              params: {
                startDate,
                endDate,
                _t: new Date().getTime()
              }
            })

            // 处理返回的数据
            let projectStats = {}
            if (projectResponse.data && projectResponse.data.data) {
              projectStats = projectResponse.data.data
            } else if (projectResponse.data) {
              projectStats = projectResponse.data
            }

            // 累加记录数和加班工时
            managedRecordCount += projectStats.recordCount || 0
            managedOvertimeHours += projectStats.overtimeHours || 0

            // 工作负荷取平均值
            if (projectStats.workload) {
              managedWorkload += projectStats.workload
            }
          } catch (error) {
            console.error(`获取项目 ${project.id} 统计数据失败:`, error)
          }
        }

        // 计算平均工时/记录
        managedAverageHoursPerRecord = managedRecordCount > 0 ? managedTotalHours / managedRecordCount : 0

        // 计算平均工作负荷
        managedWorkload = managedProjects.value.length > 0 ? managedWorkload / managedProjects.value.length : 0

        // 更新总体统计数据
        overallStats.value = {
          totalHours: managedTotalHours,
          recordCount: managedRecordCount,
          averageHoursPerRecord: managedAverageHoursPerRecord,
          overtimeHours: managedOvertimeHours,
          workload: managedWorkload
        }

        console.log('获取到项目统计数据:', projectsStats.value)
        console.log('更新后的总体统计数据:', overallStats.value)

        // 获取所有管理项目的成员工时统计
        allMemberStats.value = [] // 清空之前的数据

        // 为每个管理的项目获取成员工时统计
        for (const project of managedProjects.value) {
          try {
            // 获取项目成员列表
            const membersResponse = await api.get(`/api/projects/${project.id}/members`)
            const members = membersResponse.data || []

            // 获取项目工时记录
            const recordsResponse = await api.get('/api/worktime/project/range', {
              params: {
                projectId: project.id,
                startDate,
                endDate,
                size: 1000,
                _t: new Date().getTime()
              }
            })

            // 处理返回的数据
            let workTimeRecords = []
            if (Array.isArray(recordsResponse.data)) {
              workTimeRecords = recordsResponse.data
            } else if (recordsResponse.data && recordsResponse.data.content) {
              workTimeRecords = recordsResponse.data.content
            }

            // 按用户分组统计工时
            const userHoursMap = {}
            const userRecordsMap = {}

            workTimeRecords.forEach(record => {
              const userId = record.user?.id
              if (userId) {
                if (!userHoursMap[userId]) {
                  userHoursMap[userId] = 0
                  userRecordsMap[userId] = 0
                }
                userHoursMap[userId] += record.hours || 0
                userRecordsMap[userId] += 1
              }
            })

            // 计算项目总工时
            const projectTotalHours = Object.values(userHoursMap).reduce((sum, hours) => sum + hours, 0)

            // 构建成员工时统计数据
            const projectMemberStats = members.map(member => {
              const userId = member.id
              const totalHours = userHoursMap[userId] || 0
              const recordCount = userRecordsMap[userId] || 0

              return {
                projectId: project.id,
                projectName: project.name,
                userId,
                username: member.username,
                realName: member.realName || member.username,
                totalHours,
                recordCount,
                averageHours: recordCount > 0 ? (totalHours / recordCount).toFixed(2) : '0',
                percentage: projectTotalHours > 0
                  ? ((totalHours / projectTotalHours) * 100).toFixed(2) + '%'
                  : '0%'
              }
            })

            // 添加到所有成员统计数据中
            allMemberStats.value = [...allMemberStats.value, ...projectMemberStats]
          } catch (error) {
            console.error(`获取项目 ${project.id} 成员工时统计失败:`, error)
          }
        }

        // 按工时降序排序
        allMemberStats.value.sort((a, b) => b.totalHours - a.totalHours)

        console.log('获取到所有项目成员工时统计数据:', allMemberStats.value)
      } catch (error) {
        console.error('获取项目统计数据失败:', error)
        ElMessage.error('获取项目统计数据失败: ' + error.message)
      } finally {
        statsLoading.value = false
      }
    }

    // 查看项目详情
    const viewProjectDetail = async (projectId) => {
      if (!projectId) return

      try {
        projectDetailLoading.value = true
        projectDetailVisible.value = true

        // 获取项目详情
        const project = managedProjects.value.find(p => p.id === projectId) || { name: '未知项目' }

        // 获取项目工时统计数据
        const startDate = statsDateRange.value[0]
        const endDate = statsDateRange.value[1]

        // 调用API获取项目工时统计数据
        const response = await api.get(`/api/worktime/project/${projectId}/stats`, {
          params: {
            startDate,
            endDate,
            _t: new Date().getTime() // 添加时间戳参数，避免浏览器缓存
          }
        })

        // 处理返回的数据
        let stats = {}
        if (response.data && response.data.data) {
          stats = response.data.data
        } else if (response.data) {
          stats = response.data
        }

        // 更新项目详情数据
        currentProjectDetail.value = {
          projectId,
          projectName: project.name,
          totalHours: stats.totalHours || 0,
          recordCount: stats.recordCount || 0,
          averageHoursPerRecord: stats.averageHoursPerRecord || 0,
          overtimeHours: stats.overtimeHours || 0
        }

        // 获取项目成员列表
        const membersResponse = await api.get(`/api/projects/${projectId}/members`)
        const members = membersResponse.data || []

        // 获取项目工时记录
        const recordsResponse = await api.get('/api/worktime/project/range', {
          params: {
            projectId,
            startDate,
            endDate,
            size: 1000, // 设置较大的分页大小，确保能获取到所有记录
            _t: new Date().getTime() // 添加时间戳参数，避免浏览器缓存
          }
        })

        // 处理返回的数据
        let workTimeRecords = []
        if (Array.isArray(recordsResponse.data)) {
          workTimeRecords = recordsResponse.data
        } else if (recordsResponse.data && recordsResponse.data.content) {
          workTimeRecords = recordsResponse.data.content
        }

        // 按用户分组统计工时
        const userHoursMap = {}
        const userRecordsMap = {}

        workTimeRecords.forEach(record => {
          const userId = record.user?.id
          if (userId) {
            if (!userHoursMap[userId]) {
              userHoursMap[userId] = 0
              userRecordsMap[userId] = 0
            }
            userHoursMap[userId] += record.hours || 0
            userRecordsMap[userId] += 1
          }
        })

        // 构建成员工时统计数据
        memberStats.value = members.map(member => {
          const userId = member.id
          const totalHours = userHoursMap[userId] || 0
          const recordCount = userRecordsMap[userId] || 0

          return {
            userId,
            username: member.username,
            realName: member.realName || member.username,
            totalHours,
            recordCount,
            averageHours: recordCount > 0 ? (totalHours / recordCount).toFixed(2) : '0',
            percentage: currentProjectDetail.value.totalHours > 0
              ? ((totalHours / currentProjectDetail.value.totalHours) * 100).toFixed(2) + '%'
              : '0%'
          }
        })

        // 按工时降序排序
        memberStats.value.sort((a, b) => b.totalHours - a.totalHours)

        console.log('获取到成员工时统计数据:', memberStats.value)
      } catch (error) {
        console.error('获取项目详情失败:', error)
        ElMessage.error('获取项目详情失败: ' + error.message)
      } finally {
        projectDetailLoading.value = false
      }
    }

    // 格式化数字，保留两位小数
    const formatNumber = (num) => {
      if (num === undefined || num === null) return '0'
      return parseFloat(num).toFixed(2)
    }

    // 判断当前用户是否是项目的管理者
    const isProjectManager = (project) => {
      if (!project || !currentUser.value) return false

      // 检查项目的manager字段
      if (project.manager) {
        if (typeof project.manager === 'object' && project.manager.id) {
          return project.manager.id === currentUser.value.id
        } else if (typeof project.manager === 'number') {
          return project.manager === currentUser.value.id
        }
      }

      // 检查项目的managerId字段
      if (project.managerId) {
        return project.managerId === currentUser.value.id
      }

      // 检查项目是否在managedProjects列表中
      return managedProjects.value.some(p => p.id === project.id)
    }

    // 导出工时批量填写Excel
    const exportBatchFillData = async () => {
      console.log('导出工时批量填写Excel')
      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        ElMessage.warning('请选择有效的日期范围')
        return
      }

      if (!displayProjects.value || displayProjects.value.length === 0) {
        ElMessage.warning('没有可导出的项目数据')
        return
      }

      batchLoading.value = true
      try {
        const projectsData = displayProjects.value.map(project => ({
          projectId: project.id,
          projectName: project.name,
          members: project.members.map(member => ({
            userId: member.id,
            username: member.username,
            realName: member.realName,
            hours: member.hours
          }))
        }))

        const payload = {
          startDate: batchDateRange.value[0],
          endDate: batchDateRange.value[1],
          projectsData
        }

        console.log('Export payload:', payload)

        const response = await api.post('/api/export/batch-fill', payload, {
          responseType: 'blob' // Important for file download
        })

        const blob = new Blob([response.data], { type: response.headers['content-type'] })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        // Extract filename from content-disposition header if available
        const contentDisposition = response.headers['content-disposition']
        let filename = 'batch_fill_export.xlsx' // Default filename
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename="?(.+)"?/)
          if (filenameMatch && filenameMatch.length > 1) {
            filename = filenameMatch[1]
          }
        }
        link.setAttribute('download', filename)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)

        ElMessage.success('成功导出批量填写数据')
      } catch (error) {
        console.error('导出工时批量填写Excel失败:', error)
        ElMessage.error('导出工时批量填写Excel失败: ' + (error.response?.data?.message || error.message))
      } finally {
        batchLoading.value = false
      }
    }

    // 导出统计报表Excel
    const exportStatisticalReportData = async () => {
      console.log('导出统计报表Excel')
      if (!statsDateRange.value || statsDateRange.value.length !== 2) {
        ElMessage.warning('请选择有效的日期范围')
        return
      }

      statsLoading.value = true
      try {
        const params = {
          startDate: statsDateRange.value[0],
          endDate: statsDateRange.value[1]
        }

        const response = await api.get('/api/export/statistical-report', {
          params,
          responseType: 'blob' // Important for file download
        })

        const blob = new Blob([response.data], { type: response.headers['content-type'] })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        // Extract filename from content-disposition header if available
        const contentDisposition = response.headers['content-disposition']
        let filename = 'statistical_report_export.xlsx' // Default filename
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename="?(.+)"?/)
          if (filenameMatch && filenameMatch.length > 1) {
            filename = filenameMatch[1]
          }
        }
        link.setAttribute('download', filename)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)

        ElMessage.success('成功导出统计报表数据')
      } catch (error) {
        console.error('导出统计报表Excel失败:', error)
        ElMessage.error('导出统计报表Excel失败: ' + (error.response?.data?.message || error.message))
      } finally {
        statsLoading.value = false
      }
    }

    onMounted(async () => {
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

      // 初始化批量日期范围
      initBatchDateRange()

      // 获取项目经理管理的项目列表
      await fetchManagedProjects()

      // 初始化统计日期范围
      initStatsDateRange()

      // 获取项目统计数据
      fetchProjectsStats()
    })

</script>

<style scoped>
.project-manager-time-container {
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

.batch-container {
  margin-top: 20px;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 10px; /* Added to provide spacing for multiple buttons */
}

.batch-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.batch-table-container {
  margin-top: 20px;
  overflow-x: auto;
}

.batch-empty {
  margin-top: 20px;
  text-align: center;
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
}

.project-empty {
  padding: 20px;
  text-align: center;
}

.weekend-cell {
  background-color: #f0f9ff;
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

/* 统计报表样式 */
.stats-container {
  margin-top: 20px;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 10px; /* Added to provide spacing for multiple buttons */
}

.stats-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.stats-content {
  margin-top: 20px;
}

.stats-overview {
  margin-bottom: 30px;
}

.stats-card {
  text-align: center;
}

.card-header {
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
}

.stats-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stats-projects {
  margin-top: 30px;
}

.stats-projects h3 {
  margin-bottom: 15px;
  font-size: 18px;
  color: #333;
}

.stats-members {
  margin-top: 30px;
}

.stats-members h3 {
  margin-bottom: 15px;
  font-size: 18px;
  color: #333;
}

.project-detail-header {
  margin-bottom: 20px;
}

.project-detail-header h2 {
  margin-bottom: 10px;
  font-size: 20px;
  color: #333;
}

.project-detail-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-item .label {
  font-weight: bold;
  margin-right: 5px;
}

.detail-item .value {
  color: #409eff;
  font-weight: bold;
}

.member-stats h3 {
  margin-bottom: 15px;
  font-size: 18px;
  color: #333;
}
</style>
