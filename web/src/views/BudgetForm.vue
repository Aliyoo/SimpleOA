<template>
  <div class="budget-form-container">
    <div class="form-header">
      <h2>{{ isEdit ? '编辑预算' : '新建预算' }}</h2>
      <div>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </div>
    </div>

    <el-card class="form-card">
      <el-form ref="budgetFormRef" :model="budgetForm" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预算名称" prop="name">
              <el-input v-model="budgetForm.name" placeholder="请输入预算名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="所属项目" prop="projectId">
              <el-select v-model="budgetForm.projectId" placeholder="请选择项目" style="width: 100%" filterable>
                <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预算总金额" prop="totalAmount">
              <el-input-number
                v-model="budgetForm.totalAmount"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="请输入总金额"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="budgetForm.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="活跃" value="活跃" />
                <el-option label="暂停" value="暂停" />
                <el-option label="已完成" value="已完成" />
                <el-option label="已取消" value="已取消" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="budgetForm.startDate"
                type="date"
                placeholder="选择开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="budgetForm.endDate" type="date" placeholder="选择结束日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="预算描述">
          <el-input v-model="budgetForm.description" type="textarea" :rows="4" placeholder="请输入预算描述" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预算项目列表（编辑时显示） -->
    <el-card v-if="isEdit && budgetItems.length > 0" class="items-card">
      <template #header>
        <div class="card-header">
          <span>预算项目</span>
          <el-button type="primary" size="small" @click="showAddItemDialog = true"> 添加项目 </el-button>
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
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getItemStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editBudgetItem(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteBudgetItem(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加预算项目对话框 -->
    <el-dialog v-model="showAddItemDialog" title="添加预算项目" width="600px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemFormRules" label-width="100px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="itemForm.name" placeholder="请输入项目名称" />
        </el-form-item>

        <el-form-item label="类别" prop="category">
          <el-select v-model="itemForm.category" placeholder="请选择类别" style="width: 100%">
            <el-option label="人工费用" value="人工费用" />
            <el-option label="设备费用" value="设备费用" />
            <el-option label="材料费用" value="材料费用" />
            <el-option label="差旅费用" value="差旅费用" />
            <el-option label="管理费用" value="管理费用" />
            <el-option label="其他费用" value="其他费用" />
          </el-select>
        </el-form-item>

        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="itemForm.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入金额"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="itemForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="活跃" value="活跃" />
            <el-option label="暂停" value="暂停" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="itemForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddItemDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 响应式数据
const budgetFormRef = ref()
const itemFormRef = ref()
const showAddItemDialog = ref(false)

const budgetForm = reactive({
  name: '',
  projectId: null,
  totalAmount: null,
  status: '活跃',
  startDate: '',
  endDate: '',
  description: ''
})

const itemForm = reactive({
  name: '',
  category: '',
  amount: null,
  status: '活跃',
  description: ''
})

const projects = ref([])
const budgetItems = ref([])

// 计算属性
const isEdit = computed(() => route.params.id && route.params.id !== 'new')

// 表单验证规则
const formRules = {
  name: [{ required: true, message: '请输入预算名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  totalAmount: [{ required: true, message: '请输入总金额', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

const itemFormRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

// 方法
const loadProjects = async () => {
  try {
    const response = await axios.get('/api/projects')
    projects.value = response.data
  } catch (error) {
    ElMessage.error('加载项目列表失败')
    console.error('Error loading projects:', error)
  }
}

const loadBudget = async (id) => {
  try {
    const response = await axios.get(`/api/budgets/${id}`)
    const budget = response.data

    Object.assign(budgetForm, {
      name: budget.name,
      projectId: budget.project?.id,
      totalAmount: budget.totalAmount,
      status: budget.status,
      startDate: budget.startDate,
      endDate: budget.endDate,
      description: budget.description
    })

    // 加载预算项目
    const itemsResponse = await axios.get(`/api/budgets/${id}/items`)
    budgetItems.value = itemsResponse.data
  } catch (error) {
    ElMessage.error('加载预算信息失败')
    console.error('Error loading budget:', error)
  }
}

const handleSubmit = async () => {
  try {
    await budgetFormRef.value.validate()

    const submitData = {
      ...budgetForm,
      project: { id: budgetForm.projectId }
    }

    let response
    if (isEdit.value) {
      response = await axios.put(`/api/budgets/${route.params.id}`, submitData)
      ElMessage.success('预算更新成功')
    } else {
      response = await axios.post('/api/budgets', submitData)
      ElMessage.success('预算创建成功')
    }

    router.push('/budget-management')
  } catch (error) {
    if (error.message) return // 表单验证错误

    ElMessage.error(isEdit.value ? '更新预算失败' : '创建预算失败')
    console.error('Error submitting budget:', error)
  }
}

const handleCancel = () => {
  router.push('/budget-management')
}

const handleAddItem = async () => {
  try {
    await itemFormRef.value.validate()

    const itemData = {
      ...itemForm,
      budget: { id: route.params.id }
    }

    await axios.post(`/api/budgets/${route.params.id}/items`, itemData)
    ElMessage.success('预算项目添加成功')

    // 重新加载预算项目
    const response = await axios.get(`/api/budgets/${route.params.id}/items`)
    budgetItems.value = response.data

    // 重置表单
    Object.assign(itemForm, {
      name: '',
      category: '',
      amount: null,
      status: '活跃',
      description: ''
    })

    showAddItemDialog.value = false
  } catch (error) {
    if (error.message) return // 表单验证错误

    ElMessage.error('添加预算项目失败')
    console.error('Error adding budget item:', error)
  }
}

const editBudgetItem = (item) => {
  // TODO: 实现编辑预算项目功能
  ElMessage.info('编辑功能待实现')
}

const deleteBudgetItem = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要删除预算项目 "${item.name}" 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await axios.delete(`/api/budgets/items/${item.id}`)
    ElMessage.success('删除成功')

    // 重新加载预算项目
    const response = await axios.get(`/api/budgets/${route.params.id}/items`)
    budgetItems.value = response.data
  } catch (error) {
    if (error === 'cancel') return

    ElMessage.error('删除失败')
    console.error('Error deleting budget item:', error)
  }
}

const formatCurrency = (value) => {
  return value ? `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '¥0.00'
}

const getItemStatusType = (status) => {
  const statusMap = {
    活跃: 'success',
    暂停: 'warning',
    已完成: 'info'
  }
  return statusMap[status] || 'default'
}

// 生命周期
onMounted(() => {
  loadProjects()
  if (isEdit.value) {
    loadBudget(route.params.id)
  }
})
</script>

<style scoped>
.budget-form-container {
  padding: 20px;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.form-header h2 {
  margin: 0;
  color: #333;
}

.form-card {
  margin-bottom: 20px;
}

.items-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
