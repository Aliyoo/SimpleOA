<template>
  <div class="setting-container">
    <h1>系统设置</h1>
    
    <el-form :model="settingForm" label-width="120px">
      <el-form-item label="公司名称">
        <el-input v-model="settingForm.companyName" />
      </el-form-item>
      <el-form-item label="公司地址">
        <el-input v-model="settingForm.companyAddress" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="settingForm.phone" />
      </el-form-item>
      <el-form-item label="系统名称">
        <el-input v-model="settingForm.systemName" />
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="submitForm">保存设置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'

const settingForm = ref({
  companyName: '',
  companyAddress: '',
  phone: '',
  systemName: ''
})

const fetchSettings = async () => {
  try {
    const response = await api.get('/api/settings')
    settingForm.value = response.data
  } catch (error) {
    ElMessage.error('获取设置失败: ' + error.message)
  }
}

const submitForm = async () => {
  try {
    await api.post('/api/settings', settingForm.value)
    ElMessage.success('设置保存成功')
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  }
}

onMounted(() => {
  fetchSettings()
})
</script setup>

<style scoped>
.setting-container {
  padding: 20px;
}
</style>
