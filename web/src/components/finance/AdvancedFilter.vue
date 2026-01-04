<template>
  <el-card class="filter-card" shadow="never">
    <el-form :model="filterForm" inline label-width="auto" class="filter-form">
      <!-- 状态筛选 -->
      <el-form-item label="状态">
        <el-select
          v-model="filterForm.status"
          placeholder="选择状态"
          clearable
          multiple
          collapse-tags
          collapse-tags-tooltip
          style="width: 200px"
        >
          <el-option label="待财务审查" value="PENDING_FINANCE_REVIEW" />
          <el-option label="待领导审批" value="PENDING_LEADER_APPROVAL" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
      </el-form-item>

      <!-- 时间范围筛选 -->
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="filterForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>

      <!-- 金额范围筛选 -->
      <el-form-item label="金额范围">
        <el-input-number
          v-model="filterForm.minAmount"
          placeholder="最小金额"
          :min="0"
          :precision="2"
          controls-position="right"
          style="width: 120px"
        />
        <span style="margin: 0 8px">-</span>
        <el-input-number
          v-model="filterForm.maxAmount"
          placeholder="最大金额"
          :min="0"
          :precision="2"
          controls-position="right"
          style="width: 120px"
        />
      </el-form-item>

      <!-- 申请人筛选 -->
      <el-form-item label="申请人">
        <el-input
          v-model="filterForm.applicant"
          placeholder="申请人姓名"
          clearable
          style="width: 150px"
        >
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 标题关键字筛选 -->
      <el-form-item label="标题关键字">
        <el-input
          v-model="filterForm.keyword"
          placeholder="搜索标题"
          clearable
          style="width: 200px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 项目筛选 -->
      <el-form-item label="关联项目">
        <el-select
          v-model="filterForm.projectId"
          placeholder="选择项目"
          clearable
          filterable
          style="width: 180px"
        >
          <el-option
            v-for="project in projects"
            :key="project.id"
            :label="project.name"
            :value="project.id"
          />
        </el-select>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" @click="handleSearch" :icon="Search">
          查询
        </el-button>
        <el-button @click="handleReset" :icon="RefreshLeft">
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, RefreshLeft, User } from '@element-plus/icons-vue'
import axios from '@/utils/axios'

const filterForm = reactive({
  status: [],
  dateRange: null,
  minAmount: null,
  maxAmount: null,
  applicant: '',
  keyword: '',
  projectId: null
})

const projects = ref([])

const emit = defineEmits(['search', 'reset'])

// 加载项目列表
const loadProjects = async () => {
  try {
    // 修复：使用正确的API路径 /api/projects
    const response = await axios.get('/api/projects')
    // 后端直接返回数组，不是 { code, data } 格式
    projects.value = Array.isArray(response.data) ? response.data : (response.data.data || [])
  } catch (error) {
    console.error('加载项目列表失败:', error)
  }
}

// 处理查询
const handleSearch = () => {
  const params = { ...filterForm }

  // 处理日期范围
  if (filterForm.dateRange && filterForm.dateRange.length === 2) {
    params.startDate = filterForm.dateRange[0]
    params.endDate = filterForm.dateRange[1]
  }
  delete params.dateRange

  // 如果金额为空,设置为null
  if (!params.minAmount && params.minAmount !== 0) params.minAmount = null
  if (!params.maxAmount && params.maxAmount !== 0) params.maxAmount = null

  emit('search', params)
}

// 处理重置
const handleReset = () => {
  filterForm.status = []
  filterForm.dateRange = null
  filterForm.minAmount = null
  filterForm.maxAmount = null
  filterForm.applicant = ''
  filterForm.keyword = ''
  filterForm.projectId = null
  emit('reset')
}

onMounted(() => {
  loadProjects()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
  margin-right: 20px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
