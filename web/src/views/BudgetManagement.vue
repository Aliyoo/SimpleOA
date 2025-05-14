<template>
  <div class="budget-container">
    <h1>预算管理</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="预算列表" name="budgetList">
        <div class="operation-bar">
          <el-button type="primary" @click="handleCreateBudget">新建预算</el-button>
          <el-input
            v-model="budgetSearch"
            placeholder="搜索预算"
            style="width: 300px; margin-left: 10px"
            clearable
          />
        </div>
        
        <el-table :data="filteredBudgets" style="width: 100%; margin-top: 20px">
          <el-table-column prop="name" label="预算名称" width="180" />
          <el-table-column prop="projectName" label="所属项目" width="180" />
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
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getBudgetStatusType(scope.row.status)">
                {{ getBudgetStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="handleViewBudget(scope.row)">查看</el-button>
              <el-button size="small" type="primary" @click="handleEditBudget(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteBudget(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="预算统计" name="budgetStatistics">
        <div class="statistics-container">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>总预算金额</span>
                  </div>
                </template>
                <div class="statistics-value">{{ formatCurrency(totalBudgetAmount) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>已使用金额</span>
                  </div>
                </template>
                <div class="statistics-value">{{ formatCurrency(totalUsedAmount) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>剩余金额</span>
                  </div>
                </template>
                <div class="statistics-value">{{ formatCurrency(totalRemainingAmount) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>使用率</span>
                  </div>
                </template>
                <div class="statistics-value">{{ totalUsagePercentage }}%</div>
              </el-card>
            </el-col>
          </el-row>
          
          <div class="charts-container">
            <div class="chart-wrapper">
              <h3>预算分布</h3>
              <!-- 这里可以使用 ECharts 等图表库展示预算分布 -->
              <div class="chart-placeholder">预算分布图表</div>
            </div>
            <div class="chart-wrapper">
              <h3>预算使用趋势</h3>
              <!-- 这里可以使用 ECharts 等图表库展示预算使用趋势 -->
              <div class="chart-placeholder">预算使用趋势图表</div>
            </div>
          </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="预算预警" name="budgetAlerts">
        <div class="alerts-container">
          <el-table :data="budgetAlerts" style="width: 100%">
            <el-table-column prop="budgetName" label="预算名称" width="180" />
            <el-table-column prop="alertType" label="预警类型" width="120">
              <template #default="scope">
                <el-tag :type="getAlertTypeTag(scope.row.alertType)">
                  {{ getAlertTypeText(scope.row.alertType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="threshold" label="预警阈值" width="120">
              <template #default="scope">
                {{ scope.row.alertType === 'PERCENTAGE' ? scope.row.threshold + '%' : formatCurrency(scope.row.threshold) }}
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
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button size="small" type="primary" @click="handleViewAlert(scope.row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Budget Form Dialog -->
    <el-dialog 
      :title="budgetForm.id ? '编辑预算' : '新建预算'" 
      v-model="budgetDialogVisible"
      @closed="resetBudgetForm"
    >
      <el-form :model="budgetForm" :rules="budgetRules" ref="budgetFormRef" label-width="100px">
        <el-form-item label="预算名称" prop="name">
          <el-input v-model="budgetForm.name" />
        </el-form-item>
        <el-form-item label="所属项目" prop="projectId">
           <!-- Assuming projects are fetched into a ref named 'projectOptions' -->
           <el-select v-model="budgetForm.projectId" placeholder="选择项目" style="width: 100%;">
             <el-option 
               v-for="project in projectOptions" 
               :key="project.id" 
               :label="project.name" 
               :value="project.id" 
             />
           </el-select>
        </el-form-item>
        <el-form-item label="总预算" prop="totalAmount">
          <el-input-number v-model="budgetForm.totalAmount" :precision="2" :step="1000" :min="0" controls-position="right" style="width: 100%;"/>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="budgetForm.description" />
        </el-form-item>
         <el-form-item label="状态" prop="status">
          <el-select v-model="budgetForm.status" placeholder="选择状态" style="width: 100%;">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="待定" value="PENDING" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <!-- Add more fields as needed (e.g., budget items, start/end dates) -->
      </el-form>
      <template #footer>
        <el-button @click="budgetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBudgetForm" :loading="submittingBudget">确定</el-button>
      </template>
    </el-dialog>

     <!-- Placeholder for Alert View Dialog -->
     <!-- <el-dialog title="查看预警详情" v-model="alertViewDialogVisible"> ... </el-dialog> -->

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios.js'

// --- Refs and Reactive Variables ---

const activeTab = ref('budgetList')

// Budget List
const budgets = ref([])
const budgetSearch = ref('')
const budgetDialogVisible = ref(false)
const submittingBudget = ref(false)
const budgetFormRef = ref(null)
const projectOptions = ref([]) // For project selection

// Budget Form
const initialBudgetFormState = () => ({
    id: null,
    name: '',
    projectId: null,
    totalAmount: 0,
    description: '',
    status: 'ACTIVE',
    // Add other relevant fields, e.g., budgetItems: []
});
const budgetForm = reactive(initialBudgetFormState());

// Budget Alerts
const budgetAlerts = ref([])
// const alertViewDialogVisible = ref(false); // For viewing alert details
// const selectedAlert = ref(null);

// --- Computed Properties ---

const filteredBudgets = computed(() => {
  if (!budgetSearch.value) return budgets.value;
  const searchLower = budgetSearch.value.toLowerCase();
  return budgets.value.filter(item => 
    item.name?.toLowerCase().includes(searchLower) ||
    item.projectName?.toLowerCase().includes(searchLower)
  );
});

const totalBudgetAmount = computed(() => {
  return budgets.value.reduce((sum, budget) => sum + (budget.totalAmount || 0), 0);
});

const totalUsedAmount = computed(() => {
  return budgets.value.reduce((sum, budget) => sum + (budget.usedAmount || 0), 0);
});

const totalRemainingAmount = computed(() => {
  return totalBudgetAmount.value - totalUsedAmount.value;
});

const totalUsagePercentage = computed(() => {
  if (totalBudgetAmount.value === 0) return 0;
  return Math.round((totalUsedAmount.value / totalBudgetAmount.value) * 100);
});

// --- Validation Rules ---
const budgetRules = {
    name: [{ required: true, message: '请输入预算名称', trigger: 'blur' }],
    projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
    totalAmount: [
        { required: true, message: '请输入总预算金额', trigger: ['blur', 'change'] },
        { type: 'number', min: 0, message: '预算金额不能为负', trigger: ['blur', 'change'] }
    ],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

// --- Methods ---

// Data Fetching
const fetchBudgets = async () => {
  try {
    const response = await api.get('/api/budgets'); // Adjust endpoint if needed
    budgets.value = response.data;
  } catch (error) {
    ElMessage.error('获取预算列表失败: ' + error.message);
  }
};

const fetchBudgetAlerts = async () => {
  try {
    const response = await api.get('/api/budgets/alerts'); // Adjust endpoint if needed
    budgetAlerts.value = response.data;
  } catch (error) {
    ElMessage.error('获取预算预警失败: ' + error.message);
  }
};

const fetchProjectsForSelect = async () => {
    try {
        // Assuming an endpoint to fetch projects suitable for selection
        const response = await api.get('/api/projects/selectable'); // Or /api/projects
        projectOptions.value = response.data; // Expecting [{id: 1, name: 'Project A'}, ...]
    } catch (error) {
        ElMessage.error('获取项目列表失败: ' + error.message);
    }
};

// Budget Dialog Methods
const resetBudgetForm = () => {
    Object.assign(budgetForm, initialBudgetFormState());
    budgetFormRef.value?.clearValidate();
};

const handleCreateBudget = () => {
    resetBudgetForm();
    budgetDialogVisible.value = true;
};

const handleEditBudget = (item) => {
    // Ensure projectId is correctly assigned
    const formData = {
        ...item,
        projectId: item.projectId || item.project?.id // Handle if project object is nested
    };
    Object.assign(budgetForm, formData);
    budgetDialogVisible.value = true;
     // nextTick(() => budgetFormRef.value?.clearValidate());
};

const submitBudgetForm = async () => {
    if (!budgetFormRef.value) return;
    await budgetFormRef.value.validate(async (valid) => {
        if (valid) {
            submittingBudget.value = true;
            try {
                const payload = { ...budgetForm };
                if (payload.id) {
                    await api.put(`/api/budgets/${payload.id}`, payload);
                    ElMessage.success('预算更新成功');
                } else {
                    await api.post('/api/budgets', payload);
                    ElMessage.success('预算创建成功');
                }
                budgetDialogVisible.value = false;
                fetchBudgets();
            } catch (error) {
                ElMessage.error('操作失败: ' + error.message);
            } finally {
                submittingBudget.value = false;
            }
        }
    });
};

const handleDeleteBudget = async (item) => {
    try {
        await ElMessageBox.confirm(`确定删除预算 "${item.name}"?`, '提示', { type: 'warning' });
        await api.delete(`/api/budgets/${item.id}`);
        ElMessage.success('删除成功');
        fetchBudgets();
    } catch (error) {
        if (error !== 'cancel') {
            ElMessage.error('删除失败: ' + error.message);
        }
    }
};

// Placeholder for view budget detail (could open a new page or dialog)
const handleViewBudget = (item) => { 
    ElMessage.info('查看预算详情功能待实现'); 
    // Example: router.push(`/budget/detail/${item.id}`);
};

// Alert Methods
const handleViewAlert = (alert) => {
     ElMessage.info('查看预警详情功能待实现'); 
    // Example: 
    // selectedAlert.value = alert;
    // alertViewDialogVisible.value = true;
};

// Helper functions for display
const formatCurrency = (value) => {
    return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value || 0); 
};

const calculateUsagePercentage = (budget) => {
  if (!budget || budget.totalAmount === 0 || budget.totalAmount == null) return 0;
  return Math.max(0, Math.min(100, Math.round(((budget.usedAmount || 0) / budget.totalAmount) * 100)));
};

const getProgressStatus = (budget) => {
  const percentage = calculateUsagePercentage(budget);
  if (percentage >= 90) return 'exception';
  if (percentage >= 75) return 'warning'; // Adjusted threshold
  return 'success';
};

const getBudgetStatusType = (status) => {
  const statusMap = { 'ACTIVE': 'success', 'PENDING': 'info', 'CLOSED': 'info', 'OVERBUDGET': 'danger' };
  return statusMap[status] || 'info';
};

const getBudgetStatusText = (status) => {
  const statusMap = { 'ACTIVE': '活跃', 'PENDING': '待定', 'CLOSED': '关闭', 'OVERBUDGET': '超预算' };
  return statusMap[status] || status;
};

const getAlertTypeTag = (type) => {
    return type === 'PERCENTAGE' ? 'warning' : 'danger';
};

const getAlertTypeText = (type) => {
    return type === 'PERCENTAGE' ? '百分比预警' : '金额预警';
};

// --- Lifecycle Hook ---
onMounted(() => {
    fetchBudgets();
    fetchBudgetAlerts();
    fetchProjectsForSelect(); // Fetch projects for the dropdown
});

</script>

<style scoped>
.budget-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.statistics-container {
  margin-top: 20px;
}

.statistics-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.statistics-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  text-align: center;
  padding: 10px 0;
}

.charts-container {
  margin-top: 30px;
  display: flex;
  justify-content: space-between;
}

.chart-wrapper {
  width: 48%;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  padding: 20px;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #F5F7FA;
  color: #909399;
}

.alerts-container {
  margin-top: 20px;
}
</style>