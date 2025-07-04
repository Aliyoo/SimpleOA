<template>
  <div class="reimbursement-container">
    <h1>报销管理</h1>

    <el-button type="primary" @click="openFormDialog()">新建报销申请</el-button>

    <el-table :data="reimbursementList" style="width: 100%; margin-top: 20px;">
      <el-table-column prop="title" label="报销标题"></el-table-column>
      <el-table-column prop="totalAmount" label="总金额"></el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">{{ formatStatus(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间"></el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="openFormDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          <el-button v-if="canApprove(row)" size="small" type="warning" @click="openApprovalDialog(row)">审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Form Dialog -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑报销申请' : '新建报销申请'" width="60%">
      <el-form :model="formData" label-width="120px">
        <el-form-item label="报销标题">
          <el-input v-model="formData.title"></el-input>
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
              <el-button type="danger" size="small" @click="removeItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button @click="addItem" style="margin-top: 10px;">添加明细</el-button>

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
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import axios from '@/utils/axios'; // Assuming you have a configured axios instance
import { Plus } from '@element-plus/icons-vue';

const reimbursementList = ref([]);
const formDialogVisible = ref(false);
const approvalDialogVisible = ref(false);
const isEdit = ref(false);
const formData = ref({
  id: null,
  title: '',
  items: [],
  attachments: []
});
const fileList = ref([]);
const currentReimbursement = ref({});
const approvalComment = ref('');

// Fetch data
const fetchReimbursements = async () => {
  try {
    const response = await axios.get('/api/oa/reimbursement');
    reimbursementList.value = response.data.content;
  } catch (error) {
    ElMessage.error('获取报销列表失败');
  }
};

onMounted(fetchReimbursements);

// Form Dialog Logic
const openFormDialog = (row) => {
  isEdit.value = !!row;
  if (row) {
    formData.value = JSON.parse(JSON.stringify(row));
    fileList.value = formData.value.attachments.map(path => ({ name: path, url: path }));
  } else {
    formData.value = {
      id: null,
      title: '',
      items: [],
      attachments: []
    };
    fileList.value = [];
  }
  formDialogVisible.value = true;
};

const addItem = () => {
  formData.value.items.push({ expenseDate: '', itemCategory: '', description: '', amount: 0 });
};

const removeItem = (index) => {
  formData.value.items.splice(index, 1);
};

const handleUploadSuccess = (response, file) => {
    formData.value.attachments.push(response.data.url); // Adjust based on your upload API response
    fileList.value.push({ name: file.name, url: response.data.url });
};

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      await axios.put(`/api/oa/reimbursement/${formData.value.id}`, formData.value);
      ElMessage.success('更新成功');
    } else {
      await axios.post('/api/oa/reimbursement', formData.value);
      ElMessage.success('创建成功');
    }
    formDialogVisible.value = false;
    fetchReimbursements();
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const handleDelete = (id) => {
    ElMessageBox.confirm('确定要删除吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await axios.delete(`/api/oa/reimbursement/${id}`);
            ElMessage.success('删除成功');
            fetchReimbursements();
        } catch (error) {
            ElMessage.error('删除失败');
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
        await axios.post(`/api/oa/reimbursement/${currentReimbursement.value.id}/approval`, {
            decision,
            comment: approvalComment.value
        });
        ElMessage.success('审批操作成功');
        approvalDialogVisible.value = false;
        fetchReimbursements();
    } catch (error) {
        ElMessage.error('审批失败');
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

</script>

<style scoped>
.reimbursement-container {
  padding: 20px;
}
</style>
