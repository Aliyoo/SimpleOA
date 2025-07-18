<template>
  <div class="reimbursement-container">
    <h1>报销管理</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="报销申请" name="apply">
        <el-form :model="reimbursementForm" :rules="formRules" ref="reimbursementFormRef" label-width="120px" class="apply-form">
          <el-form-item label="报销标题" prop="title">
            <el-input v-model="reimbursementForm.title" placeholder="请输入报销标题" />
          </el-form-item>
          
          <el-form-item label="关联项目" prop="projectId">
            <el-select v-model="reimbursementForm.projectId" placeholder="请选择关联项目" clearable style="width: 100%;">
              <el-option
                v-for="project in projectList"
                :key="project.id"
                :label="project.name"
                :value="project.id"
              />
            </el-select>
          </el-form-item>
          
          <el-divider>费用明细</el-divider>
          <el-table :data="reimbursementForm.items" size="small" style="margin-bottom: 20px;">
            <el-table-column label="费用日期">
              <template #default="{ row, $index }">
                <el-date-picker 
                  v-model="row.expenseDate" 
                  type="date" 
                  value-format="YYYY-MM-DD"
                  placeholder="选择费用日期"
                />
              </template>
            </el-table-column>
            <el-table-column label="费用类别">
              <template #default="{ row, $index }">
                <el-select v-model="row.itemCategory" placeholder="选择费用类别">
                  <el-option label="交通费" value="交通费" />
                  <el-option label="住宿费" value="住宿费" />
                  <el-option label="餐费" value="餐费" />
                  <el-option label="办公用品" value="办公用品" />
                  <el-option label="通讯费" value="通讯费" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="费用说明">
              <template #default="{ row, $index }">
                <el-input v-model="row.description" placeholder="请输入费用说明" />
              </template>
            </el-table-column>
            <el-table-column label="金额">
              <template #default="{ row, $index }">
                <el-input-number v-model="row.amount" :precision="2" :min="0" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ $index }">
                <el-button type="danger" size="small" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button @click="addItem" style="margin-bottom: 20px;">添加明细</el-button>
          
          <el-form-item label="总金额">
            <el-input-number v-model="totalAmount" :precision="2" disabled />
          </el-form-item>
          
          <el-divider>凭证上传</el-divider>
          <el-upload
            v-model:file-list="fileList"
            action="/api/files/upload" 
            multiple
            :on-success="handleUploadSuccess"
            list-type="picture-card">
            <el-icon><Plus /></el-icon>
          </el-upload>
          
          <el-form-item>
            <el-button type="primary" @click="submitReimbursement">提交申请</el-button>
            <el-button @click="resetForm">重置表单</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="报销列表" name="list">
        <!-- 筛选查询栏 -->
        <div class="filter-container">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-select v-model="listFilters.status" placeholder="选择状态" clearable @change="fetchReimbursementList">
                <el-option label="全部状态" :value="null" />
                <el-option
                  v-for="status in statusOptions"
                  :key="status.value"
                  :label="status.label"
                  :value="status.value"
                />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-date-picker
                v-model="listFilters.startDate"
                type="date"
                placeholder="开始日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="fetchReimbursementList"
              />
            </el-col>
            <el-col :span="6">
              <el-date-picker
                v-model="listFilters.endDate"
                type="date"
                placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="fetchReimbursementList"
              />
            </el-col>
            <el-col :span="6">
              <el-input
                v-model="listFilters.keyword"
                placeholder="搜索标题关键字"
                clearable
                @clear="fetchReimbursementList"
                @keyup.enter="fetchReimbursementList"
              >
                <template #append>
                  <el-button @click="fetchReimbursementList">
                    <el-icon><Search /></el-icon>
                  </el-button>
                </template>
              </el-input>
            </el-col>
          </el-row>
          <el-row style="margin-top: 10px;">
            <el-col :span="24">
              <el-button @click="resetFilters">重置筛选</el-button>
              <el-button type="primary" @click="fetchReimbursementList">查询</el-button>
            </el-col>
          </el-row>
        </div>
        <el-table :data="reimbursementList" style="width: 100%" v-loading="listLoading">
          <el-table-column prop="title" label="报销标题" width="200" />
          <el-table-column prop="projectName" label="关联项目" width="150" />
          <el-table-column prop="totalAmount" label="总金额" width="120">
            <template #default="scope">
              ¥{{ scope.row.totalAmount }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="150">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ formatStatus(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button 
                size="small" 
                type="primary" 
                @click="editReimbursement(scope.row)"
                :disabled="scope.row.status !== 'DRAFT'"
              >
                编辑
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="deleteReimbursement(scope.row)"
                :disabled="scope.row.status !== 'DRAFT'"
              >
                删除
              </el-button>
              <el-button 
                v-if="canApprove(scope.row)" 
                size="small" 
                type="warning" 
                @click="openApprovalDialog(scope.row)"
              >
                审批
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="统计报表" name="statistics">
        <div class="statistics-container">
          <el-date-picker
            v-model="statisticsDateRange"
            type="monthrange"
            range-separator="至"
            start-placeholder="开始月份"
            end-placeholder="结束月份"
            format="YYYY-MM"
            value-format="YYYY-MM"
            @change="fetchStatisticsData"
          />
          <el-button type="primary" @click="fetchStatisticsData" style="margin-left: 10px">查询</el-button>
          
          <!-- 汇总信息卡片 -->
          <div class="summary-cards">
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">报销总额</div>
                <div class="card-value">¥{{ statisticsSummary.totalAmount || 0 }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">报销次数</div>
                <div class="card-value">{{ statisticsSummary.totalCount || 0 }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">通过率</div>
                <div class="card-value">{{ statisticsSummary.approvalRate || '0%' }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">最常类别</div>
                <div class="card-value">{{ statisticsSummary.mostUsedCategory || '-' }}</div>
              </div>
            </el-card>
            <el-card class="summary-card">
              <div class="card-content">
                <div class="card-title">平均金额</div>
                <div class="card-value">¥{{ statisticsSummary.avgAmount || 0 }}</div>
              </div>
            </el-card>
          </div>
          
          <el-table :data="statisticsData" style="width: 100%; margin-top: 20px">
            <el-table-column prop="category" label="费用类别" />
            <el-table-column prop="totalAmount" label="总金额" />
            <el-table-column prop="count" label="次数" />
            <el-table-column prop="avgAmount" label="平均金额" />
          </el-table>
          
          <div class="chart-container">
            <el-empty description="暂无数据" v-if="!statisticsData.length" />
            <div v-else>
              <div id="reimbursementStatisticsChart" style="width: 100%; height: 400px"></div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Form Dialog -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑报销申请' : '新建报销申请'" width="60%">
      <el-form :model="formData" label-width="120px">
        <el-form-item label="报销标题" prop="title">
          <el-input v-model="formData.title"></el-input>
        </el-form-item>

        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="formData.projectId" placeholder="请选择关联项目" clearable style="width: 100%;">
            <el-option
              v-for="project in projectList"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </el-form-item>

        <el-divider>费用明细</el-divider>
        <el-table :data="formData.items" size="small">
          <el-table-column label="费用日期">
            <template #default="{ row }">
              <el-date-picker v-model="row.expenseDate" type="date" value-format="YYYY-MM-DD"></el-date-picker>
            </template>
          </el-table-column>
          <el-table-column label="费用类别">
            <template #default="{ row }">
              <el-input v-model="row.itemCategory"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="描述">
            <template #default="{ row }">
              <el-input v-model="row.description"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="金额">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :precision="2"></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ $index }">
              <el-button type="danger" size="small" @click="removeItemForDialog($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button @click="addItemForDialog" style="margin-top: 10px;">添加明细</el-button>

        <el-divider>凭证上传</el-divider>
        <el-upload
          v-model:file-list="fileList"
          action="/api/files/upload" 
          multiple
          :on-success="handleUploadSuccess"
          list-type="picture-card">
          <el-icon><Plus /></el-icon>
        </el-upload>

      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- Approval Dialog -->
    <el-dialog v-model="approvalDialogVisible" title="报销审批" width="50%">
        <!-- Display reimbursement details here -->
        <p><strong>标题:</strong> {{ currentReimbursement.title }}</p>
        <p><strong>总金额:</strong> {{ currentReimbursement.totalAmount }}</p>
        <el-form-item label="审批意见">
            <el-input type="textarea" v-model="approvalComment"></el-input>
        </el-form-item>
        <template #footer>
            <el-button @click="handleApproval('reject')" type="danger">驳回</el-button>
            <el-button @click="handleApproval('approve')" type="primary">通过</el-button>
        </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios.js'
import { Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'
import { APP_CONFIG } from '../utils/config.js'

const userStore = useUserStore()
const currentUser = computed(() => userStore.user)

// Tab控制
const activeTab = ref('apply')

// 报销申请表单
const reimbursementForm = reactive({
  title: '',
  projectId: null,
  items: [],
  attachments: []
})

// 表单验证规则
const formRules = {
  title: [
    { required: true, message: '请输入报销标题', trigger: 'blur' },
    { min: 2, max: 100, message: '报销标题长度在2到100个字符', trigger: 'blur' }
  ]
}

const reimbursementFormRef = ref(null)
const reimbursementList = ref([])
const projectList = ref([])
const statisticsData = ref([])
const statisticsSummary = ref({})
const statisticsDateRange = ref([])
const fileList = ref([])

// 筛选相关变量
const listFilters = reactive({
  status: null,
  startDate: null,
  endDate: null,
  keyword: ''
})
const listLoading = ref(false)
const statusOptions = ref([])

// 计算总金额
const totalAmount = computed(() => {
  return reimbursementForm.items.reduce((sum, item) => sum + (item.amount || 0), 0)
})

// 原有的弹窗相关变量
const formDialogVisible = ref(false)
const approvalDialogVisible = ref(false)
const isEdit = ref(false)

const initialFormData = () => ({
  id: null,
  title: '',
  projectId: null,
  items: [],
  attachments: []
})
const formData = ref(initialFormData())

const currentReimbursement = ref({})
const approvalComment = ref('')

// Fetch data - 使用统一的API调用
const fetchProjects = async () => {
  if (!currentUser.value || !currentUser.value.id) {
    console.log('用户信息不存在，跳过项目列表加载');
    projectList.value = []; // 设置为空数组
    return;
  }
  try {
    const response = await api.get(`/api/projects/user/${currentUser.value.id}`);
    projectList.value = response.data || [];
  } catch (error) {
    console.error('获取项目列表失败:', error);
    // 不显示错误消息，因为项目列表不是必需的
    projectList.value = []; // Ensure it's an empty array on error
  }
};

onMounted(async () => {
  // 首先尝试获取用户信息，但不阻塞其他功能
  if (!userStore.user) {
    try {
      await userStore.fetchUser();
      console.log('用户信息获取成功:', userStore.user);
    } catch (error) {
      console.error('获取用户信息失败:', error);
      // 不显示错误消息，因为路由守卫已经处理了认证
    }
  }
  
  // 初始化状态选项
  initializeStatusOptions();
  
  // 继续加载其他数据
  fetchReimbursementList();
  fetchProjects();
  
  // 从全局配置获取默认月份范围
  if (APP_CONFIG && APP_CONFIG.DEFAULT_DATE_RANGE) {
    statisticsDateRange.value = APP_CONFIG.DEFAULT_DATE_RANGE.getMonthRange();
  }
  fetchStatisticsData()
});

// Form Dialog Logic
const openFormDialog = (row) => {
  isEdit.value = !!row;
  if (row) {
    formData.value = JSON.parse(JSON.stringify(row));
    // Ensure projectId is set, default to null if not present in row
    formData.value.projectId = row.projectId || null;
    fileList.value = (formData.value.attachments || []).map(path => {
      // Assuming path is a string URL, if it's an object, adjust accordingly
      const name = typeof path === 'string' ? path.substring(path.lastIndexOf('/') + 1) : 'attachment';
      return { name: name, url: path };
    });
  } else {
    formData.value = initialFormData();
    fileList.value = [];
  }
  formDialogVisible.value = true;
};

// 原有的弹窗相关函数（保留用于弹窗）
const addItemForDialog = () => {
  formData.value.items.push({ expenseDate: '', itemCategory: '', description: '', amount: 0 });
};

const removeItemForDialog = (index) => {
  formData.value.items.splice(index, 1);
};

const handleUploadSuccessForDialog = (response, file) => {
    formData.value.attachments.push(response.data?.url || response.url);
    fileList.value.push({ name: file.name, url: response.data?.url || response.url });
};

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      await api.put(`/api/oa/reimbursement/${formData.value.id}`, formData.value);
      ElMessage.success('更新成功');
    } else {
      await api.post('/api/oa/reimbursement', formData.value);
      ElMessage.success('创建成功');
    }
    formDialogVisible.value = false;
    fetchReimbursementList();
  } catch (error) {
    console.error('操作失败:', error);
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message));
  }
};

const handleDelete = (id) => {
    ElMessageBox.confirm('确定要删除吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await api.delete(`/api/oa/reimbursement/${id}`);
            ElMessage.success('删除成功');
            fetchReimbursementList();
        } catch (error) {
            console.error('删除失败:', error);
            ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message));
        }
    });
};

// Approval Dialog Logic
const openApprovalDialog = (row) => {
    currentReimbursement.value = row;
    approvalComment.value = '';
    approvalDialogVisible.value = true;
};

const handleApproval = async (decision) => {
    try {
        await api.post(`/api/oa/reimbursement/${currentReimbursement.value.id}/approval`, {
            decision,
            comment: approvalComment.value
        });
        ElMessage.success('审批操作成功');
        approvalDialogVisible.value = false;
        fetchReimbursementList();
    } catch (error) {
        console.error('审批失败:', error);
        ElMessage.error('审批失败: ' + (error.response?.data?.message || error.message));
    }
};

// Helpers
const canApprove = (row) => {
    // Simplified logic. In a real app, this would check the user's role and the request's status.
    return row.status === 'PENDING_MANAGER_APPROVAL' || row.status === 'PENDING_FINANCE_APPROVAL';
};

const formatStatus = (status) => {
    const statusMap = {
        DRAFT: '草稿',
        PENDING_MANAGER_APPROVAL: '待部门经理审批',
        PENDING_FINANCE_APPROVAL: '待财务审批',
        APPROVED: '已通过',
        REJECTED: '已驳回'
    };
    return statusMap[status] || status;
};

const getStatusTagType = (status) => {
    const tagMap = {
        DRAFT: 'info',
        PENDING_MANAGER_APPROVAL: 'warning',
        PENDING_FINANCE_APPROVAL: 'warning',
        APPROVED: 'success',
        REJECTED: 'danger'
    };
    return tagMap[status] || 'primary';
};

// 初始化状态选项
const initializeStatusOptions = () => {
  statusOptions.value = [
    { label: '草稿', value: 'DRAFT' },
    { label: '待部门经理审批', value: 'PENDING_MANAGER_APPROVAL' },
    { label: '待财务审批', value: 'PENDING_FINANCE_APPROVAL' },
    { label: '已通过', value: 'APPROVED' },
    { label: '已驳回', value: 'REJECTED' }
  ]
}

// 筛选功能的列表获取函数
const fetchReimbursementList = async () => {
  listLoading.value = true
  try {
    const params = {}
    
    // 添加筛选参数
    if (listFilters.status) {
      params.status = listFilters.status
    }
    if (listFilters.startDate) {
      params.startDate = listFilters.startDate
    }
    if (listFilters.endDate) {
      params.endDate = listFilters.endDate
    }
    if (listFilters.keyword && listFilters.keyword.trim()) {
      params.keyword = listFilters.keyword.trim()
    }
    
    const response = await api.get('/api/oa/reimbursement', { params })
    reimbursementList.value = response.data.content || response.data
  } catch (error) {
    console.error('获取报销列表失败:', error)
    ElMessage.error('获取报销列表失败: ' + (error.response?.data?.message || error.message))
  } finally {
    listLoading.value = false
  }
}

// 重置筛选条件
const resetFilters = () => {
  Object.assign(listFilters, {
    status: null,
    startDate: null,
    endDate: null,
    keyword: ''
  })
  fetchReimbursementList()
}

const fetchStatisticsData = async () => {
  if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
    console.log('日期范围未设置，跳过统计数据获取')
    return
  }
  
  try {
    // 添加详细的调试信息
    console.log('=== 开始调试日期格式问题 ===')
    console.log('statisticsDateRange.value:', statisticsDateRange.value)
    console.log('statisticsDateRange.value 类型:', typeof statisticsDateRange.value)
    console.log('第一个元素:', statisticsDateRange.value[0], '类型:', typeof statisticsDateRange.value[0])
    console.log('第二个元素:', statisticsDateRange.value[1], '类型:', typeof statisticsDateRange.value[1])
    
    // 将月份范围转换为日期范围
    let startMonth = statisticsDateRange.value[0] // 可能是 Date 对象或字符串
    let endMonth = statisticsDateRange.value[1]   // 可能是 Date 对象或字符串
    
    console.log('原始 startMonth:', startMonth, 'instanceof Date:', startMonth instanceof Date)
    console.log('原始 endMonth:', endMonth, 'instanceof Date:', endMonth instanceof Date)
    
    // 确保转换为 YYYY-MM 格式的字符串
    if (startMonth instanceof Date) {
      startMonth = startMonth.getFullYear() + '-' + String(startMonth.getMonth() + 1).padStart(2, '0')
    }
    if (endMonth instanceof Date) {
      endMonth = endMonth.getFullYear() + '-' + String(endMonth.getMonth() + 1).padStart(2, '0')
    }
    
    console.log('转换后 startMonth:', startMonth)
    console.log('转换后 endMonth:', endMonth)
    
    // 开始日期: 月份的第一天
    const startDate = `${startMonth}-01`
    
    // 结束日期: 月份的最后一天
    const [endYear, endMonthNum] = endMonth.split('-')
    const endDate = new Date(parseInt(endYear), parseInt(endMonthNum), 0).getDate()
    const endDateString = `${endMonth}-${String(endDate).padStart(2, '0')}`
    
    console.log('最终 startDate:', startDate)
    console.log('最终 endDateString:', endDateString)
    console.log('准备发送的参数:', { startDate, endDate: endDateString })
    console.log('=== 调试信息结束 ===')
    
    const response = await api.get('/api/oa/reimbursement/statistics', {
      params: {
        startDate: startDate,
        endDate: endDateString
      }
    })
    statisticsData.value = response.data.details || []
    statisticsSummary.value = response.data.summary || {}
    renderChart()
  } catch (error) {
    console.error('获取统计数据失败:', error)
    console.error('错误响应:', error.response)
    // 显示详细错误以便调试
    ElMessage.error('获取统计数据失败: ' + (error.response?.data?.message || error.message))
    statisticsData.value = []
    statisticsSummary.value = {}
  }
}

const renderChart = () => {
  const chartDom = document.getElementById('reimbursementStatisticsChart')
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '报销费用分布',
        type: 'pie',
        radius: '50%',
        data: statisticsData.value.map(item => ({
          value: item.totalAmount,
          name: item.category
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  myChart.setOption(option)
}

const submitReimbursement = async () => {
  // 表单验证
  if (!reimbursementFormRef.value) return
  
  try {
    await reimbursementFormRef.value.validate()
    
    // 检查是否有费用明细
    if (!reimbursementForm.items.length) {
      ElMessage.error('请至少添加一项费用明细')
      return
    }
    
    // 准备提交数据
    const submitData = {
      title: reimbursementForm.title,
      projectId: reimbursementForm.projectId,
      items: reimbursementForm.items,
      attachments: reimbursementForm.attachments
    }
    
    await api.post('/api/oa/reimbursement', submitData)
    ElMessage.success('报销申请提交成功')
    
    // 重置表单
    resetForm()
    
    // 刷新列表
    fetchReimbursementList()
    
    // 切换到列表页面
    activeTab.value = 'list'
  } catch (error) {
    if (error.name === 'ValidationError') {
      ElMessage.error('请正确填写表单信息')
    } else {
      ElMessage.error('提交失败: ' + error.message)
    }
  }
}

const resetForm = () => {
  // 重置表单数据
  Object.assign(reimbursementForm, {
    title: '',
    projectId: null,
    items: [],
    attachments: []
  })
  
  // 重置文件列表
  fileList.value = []
  
  // 重置表单验证
  if (reimbursementFormRef.value) {
    reimbursementFormRef.value.resetFields()
  }
}

// 添加明细项
const addReimbursementItem = () => {
  reimbursementForm.items.push({
    expenseDate: '',
    itemCategory: '',
    description: '',
    amount: 0
  })
}

// 移除明细项
const removeReimbursementItem = (index) => {
  reimbursementForm.items.splice(index, 1)
}

// 编辑报销申请
const editReimbursement = (row) => {
  // 将数据复制到申请表单
  Object.assign(reimbursementForm, {
    id: row.id,
    title: row.title,
    projectId: row.projectId || row.project?.id,
    items: JSON.parse(JSON.stringify(row.items || [])),
    attachments: row.attachments || []
  })
  
  // 设置文件列表
  fileList.value = (row.attachments || []).map(path => {
    const name = typeof path === 'string' ? path.substring(path.lastIndexOf('/') + 1) : 'attachment'
    return { name: name, url: path }
  })
  
  // 切换到申请页面
  activeTab.value = 'apply'
}

// 删除报销申请
const deleteReimbursement = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个报销申请吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await api.delete(`/api/oa/reimbursement/${row.id}`)
    ElMessage.success('删除成功')
    fetchReimbursementList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// 处理文件上传成功
const handleReimbursementUploadSuccess = (response, file) => {
  reimbursementForm.attachments.push(response.data?.url || response.url)
  fileList.value.push({ name: file.name, url: response.data?.url || response.url })
}

// 修正原有函数中的一些问题
const addItem = () => {
  if (activeTab.value === 'apply') {
    addReimbursementItem()
  } else {
    // 为原有的弹窗添加项目
    formData.value.items.push({ expenseDate: '', itemCategory: '', description: '', amount: 0 })
  }
}

const removeItem = (index) => {
  if (activeTab.value === 'apply') {
    removeReimbursementItem(index)
  } else {
    // 为原有的弹窗移除项目
    formData.value.items.splice(index, 1)
  }
}

const handleUploadSuccess = (response, file) => {
  if (activeTab.value === 'apply') {
    handleReimbursementUploadSuccess(response, file)
  } else {
    // 原有的弹窗上传处理
    formData.value.attachments.push(response.data?.url || response.url)
    fileList.value.push({ name: file.name, url: response.data?.url || response.url })
  }
}

</script>

<style scoped>
.reimbursement-container {
  padding: 20px;
}

.apply-form {
  max-width: 800px;
  margin-top: 20px;
}

.statistics-container {
  margin-top: 20px;
}

.chart-container {
  margin-top: 30px;
}

.filter-container {
  margin-bottom: 20px;
}

.summary-cards {
  display: flex;
  gap: 20px;
  margin: 20px 0;
}

.summary-card {
  flex: 1;
  text-align: center;
}

.card-content {
  padding: 20px;
}

.card-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}
</style>
