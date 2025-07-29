<template>
  <div class="role-management-container">
    <h1>角色管理</h1>
    <el-button type="primary" @click="showAddDialog">添加角色</el-button>

    <el-table :data="roleList" style="width: 100%">
      <el-table-column prop="name" label="角色名称" width="180" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle">
      <el-form ref="roleFormRef" :model="roleForm" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" rows="4" />
        </el-form-item>
        <el-form-item label="权限配置" prop="permissions">
          <el-tree
            ref="roleTreeRef"
            :data="permissionTree"
            show-checkbox
            node-key="id"
            :props="defaultProps"
            :default-checked-keys="roleForm.permissions"
            @check="handleCheckChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import api from '../utils/axios.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const roleList = ref([])
const permissionTree = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentRoleId = ref('')
const roleTreeRef = ref(null)
const roleFormRef = ref(null)

const roleForm = ref({
  id: null,
  name: '',
  description: '',
  permissions: []
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  permissions: [
    {
      type: 'array',
      required: true,
      message: '请至少选择一个权限',
      trigger: 'change'
    }
  ]
}

const defaultProps = {
  children: 'children',
  label: 'name'
}

const fetchRoles = async () => {
  try {
    const response = await api.get('/api/roles')
    roleList.value = response.data
  } catch (error) {
    ElMessage.error('获取角色列表失败: ' + error.message)
  }
}

const fetchPermissions = async () => {
  try {
    const response = await api.get('/api/permissions/tree')
    permissionTree.value = response.data
  } catch (error) {
    ElMessage.error('获取权限树失败: ' + error.message)
  }
}

const showAddDialog = async () => {
  dialogTitle.value = '添加角色'
  isEdit.value = false
  roleForm.value.id = null
  roleForm.value.name = ''
  roleForm.value.description = ''
  roleForm.value.permissions = []
  dialogVisible.value = true
  await nextTick()
  roleTreeRef.value?.setCheckedKeys([])
  roleFormRef.value?.resetFields()
}

const handleEdit = async (role) => {
  dialogTitle.value = '编辑角色'
  isEdit.value = true
  roleForm.value.id = role.id
  roleForm.value.name = role.name || ''
  roleForm.value.description = role.description || ''
  if (Array.isArray(role.permissions)) {
    roleForm.value.permissions = role.permissions.map((p) => p.id)
  } else {
    roleForm.value.permissions = []
  }
  dialogVisible.value = true
  await nextTick()
  roleTreeRef.value?.setCheckedKeys(roleForm.value.permissions || [])
  roleFormRef.value?.clearValidate()
}

const handleDelete = async (role) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色 "${role.name}" 吗?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.delete(`/api/roles/${role.id}`)
    ElMessage.success('删除成功')
    fetchRoles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const handleCheckChange = (data, checkedInfo) => {
  // 过滤掉分组节点，只保留具体权限节点
  const filteredKeys = checkedInfo.checkedKeys.filter((key) => {
    return !String(key).startsWith('resource_')
  })
  roleForm.value.permissions = filteredKeys
  roleFormRef.value?.validateField('permissions')
}

const submitForm = async () => {
  if (!roleFormRef.value) return
  await roleFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const payload = { ...roleForm.value }

        if (isEdit.value) {
          await api.put(`/api/roles/${payload.id}`, payload)
          ElMessage.success('更新成功')
        } else {
          await api.post('/api/roles', payload)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        fetchRoles()
      } catch (error) {
        ElMessage.error('操作失败: ' + error.message)
      }
    }
  })
}

onMounted(() => {
  fetchRoles()
  fetchPermissions()
})
</script>

<style scoped>
.role-management-container {
  padding: 20px;
}
</style>
