<template>
  <div class="user-management-container">
    <h1>用户管理</h1>
    <el-button type="primary" @click="showAddDialog">添加用户</el-button>
    
    <el-table :data="userList" style="width: 100%">
      <el-table-column prop="username" label="用户名" width="180" />
      <el-table-column prop="realName" label="真实姓名" width="180" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phoneNumber" label="电话" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-switch
            v-model="scope.row.enabled"
            :active-value="1"
            :inactive-value="0"
            :loading="scope.row.statusLoading"
            :before-change="() => confirmStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="角色" width="200">
        <template #default="scope">
          <el-tag v-for="role in (Array.isArray(scope.row.roles) ? scope.row.roles : [])" :key="role.id" style="margin-right: 5px">{{ role.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          <el-button size="small" @click="handleAssignRoles(scope.row)">分配角色</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" @closed="onDialogClosed">
      <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit"/>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" />
        </el-form-item>
        <el-form-item label="密码" prop="newPassword" v-else>
          <el-input v-model="userForm.newPassword" type="password" placeholder="留空则不修改密码"/>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="电话" prop="phoneNumber">
          <el-input v-model="userForm.phoneNumber" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-select v-model="userForm.enabled" placeholder="请选择状态">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="roleDialogVisible" title="分配角色" @closed="onRoleDialogClosed">
      <el-form :model="roleForm" ref="roleAssignFormRef" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="roleForm.username" disabled />
        </el-form-item>
        <el-form-item label="角色分配" prop="roles">
          <el-select v-model="roleForm.roles" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRoleForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import api from '../utils/axios.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const userList = ref([])
const roleList = ref([])
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const userFormRef = ref(null)
const roleAssignFormRef = ref(null)

const userForm = ref({
  id: null,
  username: '',
  password: '',
  newPassword: '',
  realName: '',
  email: '',
  phoneNumber: '',
  enabled: 1
})

const roleForm = ref({
  id: '',
  username: '',
  roles: []
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: ['blur', 'change'] }],
  newPassword: [],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] }
  ],
  phoneNumber: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  enabled: [{ required: true, type: 'number', message: '请选择状态', trigger: 'change' }]
}

const fetchUsers = async () => {
  try {
    const response = await api.get('/api/users')
    userList.value = response.data.map(user => ({ 
        ...user, 
        enabled: typeof user.enabled === 'string' ? parseInt(user.enabled, 10) : user.enabled, 
        statusLoading: false 
    }))
  } catch (error) {
    ElMessage.error('获取用户列表失败: ' + error.message)
  }
}

const fetchRoles = async () => {
  try {
    const response = await api.get('/api/roles')
    roleList.value = response.data
  } catch (error) {
    ElMessage.error('获取角色列表失败: ' + error.message)
  }
}

const showAddDialog = async () => {
  isEdit.value = false
  dialogTitle.value = '添加用户'
  userForm.value = {
      id: null,
      username: '',
      password: '',
      newPassword: '',
      realName: '',
      email: '',
      phoneNumber: '',
      enabled: 1
  };
  dialogVisible.value = true
  await nextTick();
  userFormRef.value?.clearValidate();
}

const handleEdit = async (user) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  userForm.value.id = user.id;
  userForm.value.username = Array.isArray(user.username) ? (user.username[0] || '') : (user.username || '');
  userForm.value.password = '';
  userForm.value.newPassword = '';
  userForm.value.realName = Array.isArray(user.realName) ? (user.realName[0] || '') : (user.realName || '');
  userForm.value.email = Array.isArray(user.email) ? (user.email[0] || '') : (user.email || '');
  userForm.value.phoneNumber = Array.isArray(user.phoneNumber) ? (user.phoneNumber[0] || '') : (user.phoneNumber || '');
  userForm.value.enabled = typeof user.enabled === 'string' ? parseInt(user.enabled, 10) : user.enabled;
  dialogVisible.value = true;
  await nextTick();
  userFormRef.value?.clearValidate();
}

const submitForm = async () => {
  if (!userFormRef.value) return
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const payload = { ...userForm.value };
        if (isEdit.value) {
            delete payload.password;
            if (!payload.newPassword) {
                delete payload.newPassword;
            }
            payload.enabled = Number(payload.enabled);
            await api.put(`/api/users/${payload.id}`, payload)
            ElMessage.success('更新成功')
        } else {
            delete payload.newPassword;
            payload.enabled = Number(payload.enabled);
            await api.post('/api/users', payload)
            ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        fetchUsers()
      } catch (error) {
        ElMessage.error('操作失败: ' + error.message)
      }
    }
  })
}

const onDialogClosed = () => {
    // Optional: Reset form fully when dialog closes if needed
    // userFormRef.value?.resetFields(); 
}

const handleDelete = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗?`, 
      '提示', 
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await api.delete(`/api/users/${user.id}`)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
     if (error !== 'cancel') {
        ElMessage.error('删除失败: ' + error.message)
     }
  }
}

const confirmStatusChange = async (user) => {
    const newEnabledState = user.enabled === 1 ? 0 : 1;
    const actionText = newEnabledState === 1 ? '启用' : '禁用';
    try {
        await ElMessageBox.confirm(
            `确定要${actionText}用户 ${user.username} 吗?`,
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }
        );
        await updateUserStatus(user, newEnabledState); 
        return false;
    } catch (error) {
        return false;
    }
};

const updateUserStatus = async (user, newEnabledState) => {
    user.statusLoading = true;
    try {
        await api.patch(`/api/users/${user.id}/status`, { enabled: newEnabledState })
        user.enabled = newEnabledState; 
        ElMessage.success('状态更新成功')
    } catch (error) {
        ElMessage.error('状态更新失败: ' + error.message)
    } finally {
        user.statusLoading = false; 
    }
};

const handleAssignRoles = async (user) => {
  if (roleList.value.length === 0) {
      await fetchRoles(); 
  }
  roleForm.value = {
    id: user.id,
    username: user.username,
    roles: Array.isArray(user.roles) ? user.roles.map(role => role.id) : []
  }
  roleDialogVisible.value = true
  await nextTick();
  roleAssignFormRef.value?.clearValidate();
}

const submitRoleForm = async () => {
  try {
    await api.put(`/api/users/${roleForm.value.id}/roles`, {
      roleIds: roleForm.value.roles
    })
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    ElMessage.error('角色分配失败: ' + error.message)
  }
}

const onRoleDialogClosed = () => {
    // Optional: Reset role assignment form if needed
}

onMounted(() => {
  fetchUsers()
  // Fetch roles initially or defer until assign roles clicked
  // fetchRoles() 
})

</script>

<style scoped>
.user-management-container {
  padding: 20px;
}
</style>