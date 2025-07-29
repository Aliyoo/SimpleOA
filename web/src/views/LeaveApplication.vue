<template>
  <div class="leave-application-container">
    <h1>请假申请</h1>
    <el-form ref="leaveForm" :model="leaveForm" :rules="rules" label-width="120px">
      <el-form-item label="请假类型" prop="type">
        <el-select v-model="leaveForm.type" placeholder="请选择请假类型">
          <el-option label="年假" value="ANNUAL_LEAVE"></el-option>
          <el-option label="病假" value="SICK_LEAVE"></el-option>
          <el-option label="事假" value="PERSONAL_LEAVE"></el-option>
          <el-option label="婚假" value="MARRIAGE_LEAVE"></el-option>
          <el-option label="产假" value="MATERNITY_LEAVE"></el-option>
          <el-option label="陪产假" value="PATERNITY_LEAVE"></el-option>
          <el-option label="丧假" value="BEREAVEMENT_LEAVE"></el-option>
          <el-option label="其他" value="OTHER"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker v-model="leaveForm.startTime" type="datetime" placeholder="选择开始时间"> </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker v-model="leaveForm.endTime" type="datetime" placeholder="选择结束时间"> </el-date-picker>
      </el-form-item>
      <el-form-item label="请假原因" prop="reason">
        <el-input v-model="leaveForm.reason" type="textarea" rows="4"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">提交申请</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'

const leaveForm = ref({
  type: '',
  startTime: '',
  endTime: '',
  reason: ''
})

const rules = {
  type: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }]
}

const submitForm = async () => {
  try {
    await api.post('/api/leave/apply', leaveForm.value)
    ElMessage.success('请假申请提交成功')
    resetForm()
  } catch (error) {
    ElMessage.error('提交失败：' + error.message)
  }
}

const resetForm = () => {
  leaveForm.value = {
    type: '',
    startTime: '',
    endTime: '',
    reason: ''
  }
}
</script>

<style scoped>
.leave-application-container {
  padding: 20px;
}
</style>
