<template>
  <div class="annual-rule-management">
    <el-card>
      <div class="header">
        <h2>年假规则配置</h2>
        <div class="actions">
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon> 新增规则
          </el-button>
          <el-button @click="refreshData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button @click="exportRules">
            <el-icon><Download /></el-icon> 导出
          </el-button>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="statistics" v-if="statistics">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总规则数" :value="statistics.totalRules" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="启用规则" :value="statistics.activeRules" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="最小年假" :value="statistics.minDays" suffix="天" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="最大年假" :value="statistics.maxDays" suffix="天" />
          </el-col>
        </el-row>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="rules"
        v-loading="loading"
        stripe
        border
        @row-click="handleRowClick"
        class="rules-table">

        <el-table-column prop="minYears" label="最小工龄(年)" width="120" sortable>
          <template #default="{ row }">
            {{ row.minYears }}
          </template>
        </el-table-column>

        <el-table-column prop="maxYears" label="最大工龄(年)" width="120" sortable>
          <template #default="{ row }">
            {{ row.maxYears || '无上限' }}
          </template>
        </el-table-column>

        <el-table-column prop="annualDays" label="年假天数" width="100" sortable>
          <template #default="{ row }">
            <el-tag type="success">{{ row.annualDays }}天</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="carryoverLimit" label="结转限制" width="100" sortable>
          <template #default="{ row }">
            <el-tag type="info">{{ row.carryoverLimit }}天</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="priority" label="优先级" width="80" sortable>
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="规则描述" show-overflow-tooltip />

        <el-table-column prop="isActive" label="状态" width="80" sortable>
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'">
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="editRule(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button
              size="small"
              :type="row.isActive ? 'warning' : 'success'"
              @click.stop="toggleRuleStatus(row)">
              <el-icon><Switch /></el-icon>
              {{ row.isActive ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click.stop="deleteRule(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 创建/编辑规则对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
      @closed="resetForm">

      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
        @submit.prevent="saveRule">

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最小工龄" prop="minYears">
              <el-input-number
                v-model="form.minYears"
                :min="0"
                :max="50"
                :precision="1"
                placeholder="请输入最小工龄"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="最大工龄" prop="maxYears">
              <el-input-number
                v-model="form.maxYears"
                :min="0"
                :max="50"
                :precision="1"
                placeholder="不填表示无上限"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年假天数" prop="annualDays">
              <el-input-number
                v-model="form.annualDays"
                :min="0"
                :max="30"
                placeholder="请输入年假天数"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="结转限制" prop="carryoverLimit">
              <el-input-number
                v-model="form.carryoverLimit"
                :min="0"
                :max="15"
                placeholder="请输入结转限制"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="优先级" prop="priority">
          <el-input-number
            v-model="form.priority"
            :min="0"
            :max="100"
            placeholder="数字越大优先级越高"
            style="width: 200px"
          />
          <span class="form-tip">数字越大优先级越高，用于处理重叠区间</span>
        </el-form-item>

        <el-form-item label="规则描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入规则描述"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRule" :loading="saving">
            {{ isEdit ? '更新' : '保存' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 规则详情对话框 -->
    <el-dialog
      title="规则详情"
      v-model="detailDialogVisible"
      width="500px">

      <div v-if="selectedRule" class="rule-detail">
        <el-descriptions border>
          <el-descriptions-item label="工龄范围">
            {{ selectedRule.minYears }} - {{ selectedRule.maxYears || '无上限' }}年
          </el-descriptions-item>
          <el-descriptions-item label="年假天数">
            {{ selectedRule.annualDays }}天
          </el-descriptions-item>
          <el-descriptions-item label="结转限制">
            {{ selectedRule.carryoverLimit }}天
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            {{ selectedRule.priority }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedRule.isActive ? 'success' : 'danger'">
              {{ selectedRule.isActive ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" span="2">
            {{ selectedRule.description || '无描述' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" v-if="selectedRule.createdAt">
            {{ formatDateTime(selectedRule.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" v-if="selectedRule.updatedAt">
            {{ formatDateTime(selectedRule.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Plus, Refresh, Download, Edit, Delete, Switch } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios'

// 响应式数据
const rules = ref([])
const loading = ref(false)
const statistics = ref({})
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 对话框状态
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const selectedRule = ref(null)

// 表单数据
const formRef = ref()
const form = reactive({
  id: null,
  minYears: null,
  maxYears: null,
  annualDays: null,
  carryoverLimit: 5,
  priority: 0,
  description: '',
  isActive: true
})

// 表单验证规则
const formRules = ref({
  minYears: [
    { required: true, message: '请输入最小工龄', trigger: 'blur' },
    { type: 'number', min: 0, message: '最小工龄不能小于0', trigger: 'blur' }
  ],
  maxYears: [
    { type: 'number', min: 0, message: '最大工龄不能小于0', trigger: 'blur' }
  ],
  annualDays: [
    { required: true, message: '请输入年假天数', trigger: 'blur' },
    { type: 'number', required: true, min: 0, max: 30, message: '年假天数应在0-30之间', trigger: 'blur' }
  ],
  carryoverLimit: [
    { type: 'number', min: 0, max: 15, message: '结转限制应在0-15之间', trigger: 'blur' }
  ],
  priority: [
    { type: 'number', min: 0, message: '优先级不能小于0', trigger: 'blur' }
  ]
})

// 计算属性
const dialogTitle = computed(() => {
  return isEdit.value ? '编辑年假规则' : '新增年假规则'
})

// 方法
const fetchRules = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/leave/annual-rules', {
      params: {
        page: pagination.page - 1,
        size: pagination.size
      }
    })
    rules.value = response.data.content || response.data
    pagination.total = response.data.totalElements || response.data.length

    // 如果有分页信息，使用分页数据
    if (response.data.content) {
      rules.value = response.data.content
      pagination.total = response.data.totalElements
    }

  } catch (error) {
    ElMessage.error('获取年假规则失败：' + (error.response?.data?.message || error.message))
    rules.value = []
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const response = await api.get('/api/leave/annual-rules/statistics')
    statistics.value = response.data
  } catch (error) {
    console.error('获取统计信息失败', error)
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const editRule = (rule) => {
  isEdit.value = true
  Object.assign(form, {
    id: rule.id,
    minYears: rule.minYears,
    maxYears: rule.maxYears,
    annualDays: rule.annualDays,
    carryoverLimit: rule.carryoverLimit,
    priority: rule.priority,
    description: rule.description,
    isActive: rule.isActive
  })
  dialogVisible.value = true
}

const deleteRule = async (rule) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除规则"${rule.description || rule.getRangeDescription()}"吗？`,
      '确认删除',
      {
        type: 'warning'
      }
    )

    await api.delete(`/api/leave/annual-rules/${rule.id}`)
    ElMessage.success('规则删除成功')
    fetchRules()
    fetchStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + (error.response?.data?.message || error.message))
    }
  }
}

const toggleRuleStatus = async (rule) => {
  try {
    const newStatus = !rule.isActive
    await api.put(`/api/leave/annual-rules/${rule.id}/status`, null, {
      params: { enabled: newStatus }
    })

    rule.isActive = newStatus
    ElMessage.success(`规则已${newStatus ? '启用' : '禁用'}`)
  } catch (error) {
    ElMessage.error('状态切换失败：' + (error.response?.data?.message || error.message))
  }
}

const saveRule = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    saving.value = true

    if (isEdit.value) {
      await api.put(`/api/leave/annual-rules/${form.id}`, form)
      ElMessage.success('规则更新成功')
    } else {
      await api.post('/api/leave/annual-rules', form)
      ElMessage.success('规则创建成功')
    }

    dialogVisible.value = false
    fetchRules()
    fetchStatistics()
  } catch (error) {
    ElMessage.error('保存失败：' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    minYears: null,
    maxYears: null,
    annualDays: null,
    carryoverLimit: 5,
    priority: 0,
    description: '',
    isActive: true
  })
  formRef.value?.resetFields()
}

const refreshData = () => {
  fetchRules()
  fetchStatistics()
}

const exportRules = () => {
  // TODO: 实现导出功能
  ElMessage.info('导出功能待实现')
}

const handleRowClick = (row) => {
  selectedRule.value = row
  detailDialogVisible.value = true
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchRules()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchRules()
}

const getPriorityType = (priority) => {
  if (priority >= 10) return 'danger'
  if (priority >= 5) return 'warning'
  return 'info'
}

const formatDateTime = (dateTime) => {
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  fetchRules()
  fetchStatistics()
})
</script>

<style scoped>
.annual-rule-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.actions {
  display: flex;
  gap: 10px;
}

.statistics {
  margin-bottom: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.rules-table {
  margin-top: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  text-align: right;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.rule-detail {
  padding: 20px 0;
}
</style>