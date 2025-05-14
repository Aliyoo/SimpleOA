<template>
  <div class="profile-container">
    <h1>个人中心</h1>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="个人信息" name="info">
        <el-form :model="userInfo" label-width="100px" class="info-form">
          <el-form-item label="用户名">
            <el-input v-model="userInfo.username" disabled />
          </el-form-item>
          
          <el-form-item label="姓名">
            <el-input v-model="userInfo.name" />     
          </el-form-item>
          
          <el-form-item label="邮箱">
            <el-input v-model="userInfo.email" />
          </el-form-item>
          
          <el-form-item label="手机号">
            <el-input v-model="userInfo.phone" />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="updateProfile">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="修改密码" name="password">
        <el-form :model="passwordForm" label-width="100px" class="password-form" :rules="passwordRules" ref="passwordForm">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="changePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'

export default {
  setup() {
    const activeTab = ref('info')
    
    const userInfo = reactive({
      username: '',
      name: '',
      email: '',
      phone: ''
    })
    
    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    const passwordRules = {
      oldPassword: [
        { required: true, message: '请输入原密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== passwordForm.newPassword) {
              callback(new Error('两次输入密码不一致!'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }
    
    const fetchUserInfo = async () => {
      try {
        const response = await api.get('/api/user/profile')
        Object.assign(userInfo, response.data)
      } catch (error) {
        ElMessage.error('获取用户信息失败: ' + error.message)
      }
    }
    
    const updateProfile = async () => {
      try {
        await api.put('/api/user/profile', userInfo)
        ElMessage.success('个人信息更新成功')
      } catch (error) {
        ElMessage.error('更新失败: ' + error.message)
      }
    }
    
    const changePassword = () => {
      passwordFormRef.value.validate(async (valid) => {
        if (valid) {
          try {
            await api.post('/api/user/change-password', {
              oldPassword: passwordForm.oldPassword,
              newPassword: passwordForm.newPassword
            })
            ElMessage.success('密码修改成功')
            passwordFormRef.value.resetFields()
          } catch (error) {
            ElMessage.error('密码修改失败: ' + error.message)
          }
        }
      })
    }
    
    onMounted(() => {
      fetchUserInfo()
    })
    
    return {
      activeTab,
      userInfo,
      passwordForm,
      passwordRules,
      updateProfile,
      changePassword,
      passwordFormRef
    }
  }
}
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

.info-form, .password-form {
  max-width: 600px;
  margin-top: 20px;
}
</style>