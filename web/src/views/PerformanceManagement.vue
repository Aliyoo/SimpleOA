<template>
  <div class="performance-container">
    <h1>绩效管理</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="绩效考核标准" name="criteria">
        <div class="operation-bar">
          <el-button type="primary" @click="handleCreateCriteria">新建考核标准</el-button>
          <el-input
            v-model="criteriaSearch"
            placeholder="搜索考核标准"
            style="width: 300px; margin-left: 10px"
            clearable
          />
        </div>
        
        <el-table :data="filteredCriteria" style="width: 100%; margin-top: 20px">
          <el-table-column prop="name" label="标准名称" width="180" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="weight" label="权重" width="100">
            <template #default="scope">
              {{ scope.row.weight }}%
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="120">
            <template #default="scope">
              <el-tag>{{ scope.row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isActive" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.isActive ? 'success' : 'info'">
                {{ scope.row.isActive ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="handleEditCriteria(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteCriteria(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="绩效评估" name="evaluation">
        <div class="operation-bar">
          <el-button type="primary" @click="handleCreateEvaluation">新建评估</el-button>
          <el-input
            v-model="evaluationSearch"
            placeholder="搜索评估"
            style="width: 300px; margin-left: 10px"
            clearable
          />
        </div>
        
        <el-table :data="filteredEvaluations" style="width: 100%; margin-top: 20px">
          <el-table-column prop="employeeName" label="员工姓名" width="120" />
          <el-table-column prop="projectName" label="项目名称" width="180" />
          <el-table-column prop="evaluationPeriod" label="评估周期" width="180" />
          <el-table-column prop="overallScore" label="总分" width="100">
            <template #default="scope">
              <span :class="getScoreClass(scope.row.overallScore)">{{ scope.row.overallScore }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="rating" label="等级" width="100">
            <template #default="scope">
              <el-tag :type="getRatingTagType(scope.row.rating)">
                {{ scope.row.rating }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getEvaluationStatusType(scope.row.status)">
                {{ getEvaluationStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="evaluatorName" label="评估人" width="120" />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="handleViewEvaluation(scope.row)">查看</el-button>
              <el-button 
                size="small" 
                type="primary" 
                @click="handleEditEvaluation(scope.row)"
                :disabled="scope.row.status === 'APPROVED'"
              >编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="绩效奖金" name="bonus">
        <div class="operation-bar">
          <el-button type="primary" @click="handleCreateBonus">新建奖金</el-button>
          <el-input
            v-model="bonusSearch"
            placeholder="搜索奖金"
            style="width: 300px; margin-left: 10px"
            clearable
          />
        </div>
        
        <el-table :data="filteredBonuses" style="width: 100%; margin-top: 20px">
          <el-table-column prop="employeeName" label="员工姓名" width="120" />
          <el-table-column prop="projectName" label="项目名称" width="180" />
          <el-table-column prop="bonusPeriod" label="奖金周期" width="180" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="scope">
              {{ formatCurrency(scope.row.amount) }}
            </template>
          </el-table-column>
          <el-table-column prop="bonusType" label="奖金类型" width="120">
            <template #default="scope">
              <el-tag>{{ scope.row.bonusType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getBonusStatusType(scope.row.status)">
                {{ getBonusStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="approverName" label="审批人" width="120" />
          <el-table-column prop="paymentDate" label="支付日期" width="180" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="handleViewBonus(scope.row)">查看</el-button>
              <el-button 
                size="small" 
                type="primary" 
                @click="handleApproveBonus(scope.row)"
                :disabled="scope.row.status !== 'PENDING'"
              >审批</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="绩效统计" name="statistics">
        <div class="statistics-container">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>平均绩效分</span>
                  </div>
                </template>
                <div class="statistics-value">{{ averageScore.toFixed(2) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>总奖金金额</span>
                  </div>
                </template>
                <div class="statistics-value">{{ formatCurrency(totalBonusAmount) }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>评估数量</span>
                  </div>
                </template>
                <div class="statistics-value">{{ evaluations.length }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="statistics-card">
                <template #header>
                  <div class="card-header">
                    <span>奖金数量</span>
                  </div>
                </template>
                <div class="statistics-value">{{ bonuses.length }}</div>
              </el-card>
            </el-col>
          </el-row>
          
          <div class="charts-container">
            <div class="chart-wrapper">
              <h3>绩效分布</h3>
              <!-- 这里可以使用 ECharts 等图表库展示绩效分布 -->
              <div class="chart-placeholder">绩效分布图表</div>
            </div>
            <div class="chart-wrapper">
              <h3>奖金分布</h3>
              <!-- 这里可以使用 ECharts 等图表库展示奖金分布 -->
              <div class="chart-placeholder">奖金分布图表</div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Dialogs for Criteria, Evaluation, Bonus would go here -->
    <!-- Example: Criteria Dialog -->
    <el-dialog 
      :title="criteriaForm.id ? '编辑考核标准' : '新建考核标准'" 
      v-model="criteriaDialogVisible"
      @closed="resetCriteriaForm"
    >
      <el-form :model="criteriaForm" :rules="criteriaRules" ref="criteriaFormRef" label-width="100px">
        <el-form-item label="标准名称" prop="name">
          <el-input v-model="criteriaForm.name" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="criteriaForm.description" />
        </el-form-item>
        <el-form-item label="权重(%)" prop="weight">
          <el-input-number v-model="criteriaForm.weight" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="criteriaForm.category" />
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
           <el-switch v-model="criteriaForm.isActive" :active-value="true" :inactive-value="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="criteriaDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCriteriaForm" :loading="submittingCriteria">确定</el-button>
      </template>
    </el-dialog>

    <!-- Placeholder for Evaluation Dialog -->
    <!-- <el-dialog title="绩效评估详情/编辑" v-model="evaluationDialogVisible"> ... </el-dialog> -->

    <!-- Placeholder for Bonus Dialog -->
    <!-- <el-dialog title="绩效奖金详情/审批" v-model="bonusDialogVisible"> ... </el-dialog> -->

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios.js'

// --- Refs and Reactive Variables ---

const activeTab = ref('criteria')

// Criteria
const criteria = ref([])
const criteriaSearch = ref('')
const criteriaDialogVisible = ref(false)
const submittingCriteria = ref(false)
const criteriaFormRef = ref(null)
const initialCriteriaFormState = () => ({ 
    id: null, name: '', description: '', weight: 50, category: '', isActive: true 
});
const criteriaForm = reactive(initialCriteriaFormState()); // Using reactive is fine

// Evaluation
const evaluations = ref([])
const evaluationSearch = ref('')
// Add refs for evaluation dialog and form if implemented
// const evaluationDialogVisible = ref(false)
// const evaluationForm = reactive({ ... })

// Bonus
const bonuses = ref([])
const bonusSearch = ref('')
// Add refs for bonus dialog and form if implemented
// const bonusDialogVisible = ref(false)
// const bonusForm = reactive({ ... })

// Statistics (Placeholder values)
const averageScore = computed(() => {
    if (evaluations.value.length === 0) return 0;
    const total = evaluations.value.reduce((sum, item) => sum + (item.overallScore || 0), 0);
    return total / evaluations.value.length;
});
const totalBonusAmount = computed(() => {
    return bonuses.value.reduce((sum, item) => sum + (item.amount || 0), 0);
});

// --- Computed Properties for Filtering ---

const filteredCriteria = computed(() => {
  if (!criteriaSearch.value) return criteria.value;
  const searchLower = criteriaSearch.value.toLowerCase();
  return criteria.value.filter(item => 
    item.name?.toLowerCase().includes(searchLower) ||
    item.description?.toLowerCase().includes(searchLower) ||
    item.category?.toLowerCase().includes(searchLower)
  );
});

const filteredEvaluations = computed(() => {
  if (!evaluationSearch.value) return evaluations.value;
   const searchLower = evaluationSearch.value.toLowerCase();
  return evaluations.value.filter(item => 
    item.employeeName?.toLowerCase().includes(searchLower) ||
    item.projectName?.toLowerCase().includes(searchLower) ||
    item.evaluationPeriod?.toLowerCase().includes(searchLower) ||
    item.evaluatorName?.toLowerCase().includes(searchLower)
  );
});

const filteredBonuses = computed(() => {
  if (!bonusSearch.value) return bonuses.value;
   const searchLower = bonusSearch.value.toLowerCase();
  return bonuses.value.filter(item => 
    item.employeeName?.toLowerCase().includes(searchLower) ||
    item.projectName?.toLowerCase().includes(searchLower) ||
    item.bonusPeriod?.toLowerCase().includes(searchLower) ||
    item.bonusType?.toLowerCase().includes(searchLower)
  );
});

// --- Validation Rules ---

const criteriaRules = {
  name: [{ required: true, message: '请输入标准名称', trigger: 'blur' }],
  weight: [{ required: true, type: 'number', message: '请输入权重', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
};

// --- Methods ---

// Data Fetching
const fetchCriteria = async () => {
  try {
    const response = await api.get('/api/performance/criteria'); // Adjust API endpoint
    criteria.value = response.data;
  } catch (error) {
    ElMessage.error('获取考核标准失败: ' + error.message);
  }
};

const fetchEvaluations = async () => {
  try {
    const response = await api.get('/api/performance/evaluations'); // Adjust API endpoint
    evaluations.value = response.data;
  } catch (error) {
    ElMessage.error('获取绩效评估失败: ' + error.message);
  }
};

const fetchBonuses = async () => {
  try {
    const response = await api.get('/api/performance/bonuses'); // Adjust API endpoint
    bonuses.value = response.data;
  } catch (error) {
    ElMessage.error('获取绩效奖金失败: ' + error.message);
  }
};

// Criteria Dialog Methods
const resetCriteriaForm = () => {
    Object.assign(criteriaForm, initialCriteriaFormState());
    criteriaFormRef.value?.clearValidate();
};

const handleCreateCriteria = () => {
    resetCriteriaForm();
    criteriaDialogVisible.value = true;
};

const handleEditCriteria = (item) => {
    Object.assign(criteriaForm, item);
    criteriaDialogVisible.value = true;
    // nextTick(() => criteriaFormRef.value?.clearValidate()); // Clear validation on edit open
};

const submitCriteriaForm = async () => {
    if (!criteriaFormRef.value) return;
    await criteriaFormRef.value.validate(async (valid) => {
        if (valid) {
            submittingCriteria.value = true;
            try {
                const payload = { ...criteriaForm };
                if (payload.id) {
                    await api.put(`/api/performance/criteria/${payload.id}`, payload);
                    ElMessage.success('更新成功');
                } else {
                    await api.post('/api/performance/criteria', payload);
                    ElMessage.success('创建成功');
                }
                criteriaDialogVisible.value = false;
                fetchCriteria();
            } catch (error) {
                ElMessage.error('操作失败: ' + error.message);
            } finally {
                submittingCriteria.value = false;
            }
        }
    });
};

const handleDeleteCriteria = async (item) => {
    try {
        await ElMessageBox.confirm(`确定删除标准 "${item.name}"?`, '提示', { type: 'warning' });
        await api.delete(`/api/performance/criteria/${item.id}`);
        ElMessage.success('删除成功');
        fetchCriteria();
    } catch (error) {
        if (error !== 'cancel') {
            ElMessage.error('删除失败: ' + error.message);
        }
    }
};

// Evaluation Methods (Placeholders - Implement Dialogs and API calls)
const handleCreateEvaluation = () => { ElMessage.info('新建评估功能待实现'); };
const handleViewEvaluation = (item) => { ElMessage.info('查看评估功能待实现'); };
const handleEditEvaluation = (item) => { ElMessage.info('编辑评估功能待实现'); };

// Bonus Methods (Placeholders - Implement Dialogs and API calls)
const handleCreateBonus = () => { ElMessage.info('新建奖金功能待实现'); };
const handleViewBonus = (item) => { ElMessage.info('查看奖金功能待实现'); };
const handleApproveBonus = (item) => { ElMessage.info('审批奖金功能待实现'); };

// Helper functions for display
const getScoreClass = (score) => { /* ... return class based on score ... */ return ''; };
const getRatingTagType = (rating) => { /* ... return tag type based on rating ... */ return 'info'; };
const getEvaluationStatusType = (status) => { /* ... return tag type based on status ... */ return 'info'; };
const getEvaluationStatusText = (status) => { /* ... return text based on status ... */ return status; };
const getBonusStatusType = (status) => { /* ... return tag type based on status ... */ return 'info'; };
const getBonusStatusText = (status) => { /* ... return text based on status ... */ return status; };
const formatCurrency = (amount) => { 
    return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(amount || 0); 
};

// --- Lifecycle Hook ---
onMounted(() => {
    fetchCriteria();
    fetchEvaluations();
    fetchBonuses();
});

</script>

<style scoped>
.performance-container {
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
  font-weight: bold;
}
.statistics-value {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  padding: 10px 0;
}
.charts-container {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}
.chart-wrapper {
  flex: 1;
  border: 1px solid #ebeef5;
  padding: 15px;
  border-radius: 4px;
}
.chart-placeholder {
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #909399;
  background-color: #f5f7fa;
}
</style>