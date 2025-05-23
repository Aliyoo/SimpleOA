<template>
  <div class="permission-container">
    <h1>权限管理</h1>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="权限列表" name="permissionList">
        <div class="operation-bar">
          <el-button type="primary" @click="handleCreatePermission">新建权限</el-button>
          <el-input
            v-model="permissionSearch"
            placeholder="搜索权限"
            style="width: 300px; margin-left: 10px"
            clearable
          />
        </div>

        <el-table :data="filteredPermissions" style="width: 100%; margin-top: 20px">
          <el-table-column prop="name" label="权限名称" width="180" />
          <el-table-column prop="description" label="权限描述" width="250" />
          <el-table-column prop="permissionType" label="权限类型" width="120">
            <template #default="scope">
              <el-tag :type="getPermissionTypeTag(scope.row.permissionType)">
                {{ scope.row.permissionType === 'FUNCTIONAL' ? '功能权限' : '数据权限' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resource" label="资源" width="120" />
          <el-table-column prop="action" label="操作类型" width="120" />
          <el-table-column prop="isActive" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.isActive ? 'success' : 'danger'">
                {{ scope.row.isActive ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="handleEditPermission(scope.row)">编辑</el-button>
              <el-button
                size="small"
                type="primary"
                @click="handleConfigurePermission(scope.row)"
                :disabled="!scope.row.isActive"
              >配置</el-button>
              <el-button
                size="small"
                type="danger"
                @click="handleDeletePermission(scope.row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 权限表单对话框 -->
    <el-dialog
      :title="permissionForm.id ? '编辑权限' : '新建权限'"
      v-model="permissionDialogVisible"
      width="50%"
      @closed="resetPermissionForm"
    >
      <el-form :model="permissionForm" label-width="120px" :rules="permissionRules" ref="permissionFormRef">
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="permissionForm.name" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="权限描述" prop="description">
          <el-input v-model="permissionForm.description" type="textarea" placeholder="请输入权限描述" />
        </el-form-item>
        <el-form-item label="权限类型" prop="permissionType">
          <el-select v-model="permissionForm.permissionType" placeholder="请选择权限类型" style="width: 100%">
            <el-option label="功能权限" value="FUNCTIONAL" />
            <el-option label="数据权限" value="DATA" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源" prop="resource">
          <el-input v-model="permissionForm.resource" placeholder="请输入资源名称，如PROJECT, USER等" />
        </el-form-item>
        <el-form-item label="操作类型" prop="action">
          <el-select v-model="permissionForm.action" placeholder="请选择操作类型" style="width: 100%">
            <el-option label="读取" value="READ" />
            <el-option label="创建" value="CREATE" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="全部" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope" v-if="permissionForm.permissionType === 'DATA'">
          <el-select v-model="permissionForm.dataScope" placeholder="请选择数据范围" style="width: 100%">
            <el-option label="全部数据" value="ALL" />
            <el-option label="部门数据" value="DEPARTMENT" />
            <el-option label="个人数据" value="SELF" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
          <el-switch
            v-model="permissionForm.isActive"
            :active-value="true"
            :inactive-value="false"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="permissionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPermissionForm" :loading="submittingPermission">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog
      title="权限配置"
      v-model="permissionConfigDialogVisible"
      width="60%"
    >
      <div v-if="selectedPermission">
        <h3>{{ selectedPermission.name }}</h3>
        <p>{{ selectedPermission.description }}</p>

        <el-tabs v-model="configActiveTab">
          <el-tab-pane label="功能权限配置" name="functionConfig" v-if="selectedPermission.permissionType === 'FUNCTIONAL'">
            <el-form :model="functionPermissionForm">
              <el-form-item v-for="(access, funcName) in functionPermissionForm.permissions" :key="funcName" :label="funcName">
                <el-switch v-model="functionPermissionForm.permissions[funcName]" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveFunctionPermission" :loading="savingFunctionPermission">
                  保存配置
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="数据权限配置" name="dataConfig" v-if="selectedPermission.permissionType === 'DATA'">
            <el-form :model="dataPermissionForm">
              <el-form-item v-for="(access, dataType) in dataPermissionForm.permissions" :key="dataType" :label="dataType">
                <el-select v-model="dataPermissionForm.permissions[dataType]" style="width: 100%">
                  <el-option label="全部数据" value="ALL" />
                  <el-option label="部门数据" value="DEPARTMENT" />
                  <el-option label="个人数据" value="SELF" />
                  <el-option label="自定义" value="CUSTOM" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveDataPermission" :loading="savingDataPermission">
                  保存配置
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios.js'

// --- Refs and Reactive Variables ---

// Tab states
const activeTab = ref('permissionList')
const configActiveTab = ref('functionConfig')

// Permission List
const permissions = ref([])
const permissionSearch = ref('')
const permissionDialogVisible = ref(false)
const submittingPermission = ref(false)
const permissionFormRef = ref(null)

// Permission Form (using reactive is fine here)
const initialPermissionFormState = () => ({
    id: null,
    name: '',
    description: '',
    permissionType: 'FUNCTIONAL',
    resource: '',
    action: '',
    dataScope: 'ALL',
    isActive: true
});
const permissionForm = reactive(initialPermissionFormState());

// Permission Configuration Dialog
const permissionConfigDialogVisible = ref(false)
const selectedPermission = ref(null)
const functionPermissionForm = reactive({ permissions: {} })
const dataPermissionForm = reactive({ permissions: {} })
const savingFunctionPermission = ref(false)
const savingDataPermission = ref(false)

// --- Computed Properties ---

const filteredPermissions = computed(() => {
  if (!permissionSearch.value) {
    return permissions.value;
  }
  return permissions.value.filter(p =>
    p.name.toLowerCase().includes(permissionSearch.value.toLowerCase()) ||
    p.description.toLowerCase().includes(permissionSearch.value.toLowerCase())
  );
});

// --- Validation Rules ---
const permissionRules = {
    name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
    permissionType: [{ required: true, message: '请选择权限类型', trigger: 'change' }],
    resource: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
    action: [{ required: true, message: '请选择操作类型', trigger: 'change' }],
    isActive: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

// --- Methods ---

// Helper for tag types
const getPermissionTypeTag = (type) => {
    return type === 'FUNCTIONAL' ? 'primary' : 'success';
};

// Fetch data
const fetchPermissions = async () => {
    try {
        const response = await api.get('/api/permissions');
        permissions.value = response.data;
    } catch (error) {
        ElMessage.error('获取权限列表失败: ' + error.message);
    }
};

// Permission Dialog
const resetPermissionForm = () => {
    Object.assign(permissionForm, initialPermissionFormState());
    permissionFormRef.value?.clearValidate();
};

const handleCreatePermission = () => {
    resetPermissionForm();
    permissionDialogVisible.value = true;
};

const handleEditPermission = (permission) => {
    // Using Object.assign with reactive is safe
    Object.assign(permissionForm, permission);
    permissionDialogVisible.value = true;
    nextTick(() => {
         permissionFormRef.value?.clearValidate();
    });
};

const submitPermissionForm = async () => {
    if (!permissionFormRef.value) return;
    await permissionFormRef.value.validate(async (valid) => {
        if (valid) {
            submittingPermission.value = true;
            try {
                const payload = { ...permissionForm };
                if (payload.id) {
                    await api.put(`/api/permissions/${payload.id}`, payload);
                    ElMessage.success('权限更新成功');
                } else {
                    await api.post('/api/permissions', payload);
                    ElMessage.success('权限创建成功');
                }
                permissionDialogVisible.value = false;
                fetchPermissions(); // Refresh list
            } catch (error) {
                ElMessage.error('操作失败: ' + error.message);
            } finally {
                submittingPermission.value = false;
            }
        }
    });
};

const handleDeletePermission = async (permission) => {
    try {
        await ElMessageBox.confirm(
            `确定要删除权限 "${permission.name}" 吗?`,
            '提示',
            { type: 'warning' }
        );
        await api.delete(`/api/permissions/${permission.id}`);
        ElMessage.success('删除成功');
        fetchPermissions(); // Refresh list
    } catch (error) {
        if (error !== 'cancel') {
            ElMessage.error('删除失败: ' + error.message);
        }
    }
};

// Permission Configuration Dialog (Example structure - needs specific API endpoints)
const handleConfigurePermission = async (permission) => {
    selectedPermission.value = permission;
    // TODO: Fetch specific configuration based on permission type and ID from backend
    // Example placeholder:
    if (permission.permissionType === 'FUNCTIONAL') {
        // Replace with actual fetched config
        functionPermissionForm.permissions = { 'Can View Dashboard': true, 'Can Edit Profile': false };
        configActiveTab.value = 'functionConfig';
    } else {
        // Replace with actual fetched config
        dataPermissionForm.permissions = { 'Sales Data': 'DEPARTMENT', 'HR Data': 'SELF' };
        configActiveTab.value = 'dataConfig';
    }
    permissionConfigDialogVisible.value = true;
};

const saveFunctionPermission = async () => {
    savingFunctionPermission.value = true;
    try {
        // TODO: Call API to save function permission config
        // await api.put(`/api/permissions/${selectedPermission.value.id}/function-config`, functionPermissionForm.permissions);
        ElMessage.success('功能权限配置已保存');
        permissionConfigDialogVisible.value = false;
    } catch (error) {
        ElMessage.error('保存功能权限配置失败: ' + error.message);
    } finally {
        savingFunctionPermission.value = false;
    }
};

const saveDataPermission = async () => {
    savingDataPermission.value = true;
    try {
        // TODO: Call API to save data permission config
        // await api.put(`/api/permissions/${selectedPermission.value.id}/data-config`, dataPermissionForm.permissions);
        ElMessage.success('数据权限配置已保存');
        permissionConfigDialogVisible.value = false;
    } catch (error) {
        ElMessage.error('保存数据权限配置失败: ' + error.message);
    } finally {
        savingDataPermission.value = false;
    }
};


// --- Lifecycle Hook ---
onMounted(() => {
    fetchPermissions();
});

</script>

<style scoped>
.permission-container {
  padding: 20px;
}
.operation-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
.role-permission-container {
  margin-top: 20px;
}
.role-list, .permission-assignment {
  border: 1px solid #ebeef5;
  padding: 15px;
  border-radius: 4px;
  min-height: 400px; /* Ensure minimum height */
}
h3 {
  margin-top: 0;
  margin-bottom: 15px;
}
.no-role-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: #909399;
}
</style>
