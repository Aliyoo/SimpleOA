<template>
  <div class="reimbursement-container">
    <h1>报销申请</h1>
    <el-form :model="reimbursementForm" :rules="rules" ref="reimbursementForm" label-width="120px">
      <el-form-item label="报销类型" prop="type">
        <el-select v-model="reimbursementForm.type" placeholder="请选择报销类型">
          <el-option label="差旅费" value="travel"></el-option>
          <el-option label="办公用品" value="office"></el-option>
          <el-option label="招待费" value="entertainment"></el-option>
          <el-option label="其他" value="other"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="报销金额" prop="amount">
        <el-input-number v-model="reimbursementForm.amount" :min="0" :precision="2"></el-input-number>
      </el-form-item>
      <el-form-item label="报销日期" prop="date">
        <el-date-picker v-model="reimbursementForm.date" type="date" placeholder="选择日期"></el-date-picker>
      </el-form-item>
      <el-form-item label="发票附件" prop="attachments">
        <el-upload
          action="/api/upload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :file-list="reimbursementForm.attachments"
          multiple
          :limit="5">
          <el-button type="primary">上传发票</el-button>
          <template #tip>
            <div class="el-upload__tip">
              请上传清晰的发票照片，最多5张
            </div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item label="报销说明" prop="description">
        <el-input type="textarea" v-model="reimbursementForm.description" rows="4"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">提交申请</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref } from 'vue'
import api from '../utils/axios.js'
import { ElMessage } from 'element-plus'

export default {
  setup() {
    const reimbursementForm = ref({
      type: '',
      amount: 0,
      date: '',
      attachments: [],
      description: ''
    })

    const rules = {
      type: [{ required: true, message: '请选择报销类型', trigger: 'change' }],
      amount: [{ required: true, message: '请输入报销金额', trigger: 'blur' }],
      date: [{ required: true, message: '请选择报销日期', trigger: 'change' }],
      description: [{ required: true, message: '请输入报销说明', trigger: 'blur' }]
    }

    const handleUploadSuccess = (response, file, fileList) => {
      reimbursementForm.value.attachments = fileList
      ElMessage.success('上传成功')
    }

    const handleUploadError = (error) => {
      ElMessage.error('上传失败：' + error.message)
    }

    const submitForm = async () => {
      try {
        await api.post('/api/reimbursements', reimbursementForm.value)
        ElMessage.success('报销申请提交成功')
        resetForm()
      } catch (error) {
        ElMessage.error('提交失败：' + error.message)
      }
    }

    const resetForm = () => {
      reimbursementForm.value = {
        type: '',
        amount: 0,
        date: '',
        attachments: [],
        description: ''
      }
    }

    return {
      reimbursementForm,
      rules,
      submitForm,
      resetForm,
      handleUploadSuccess,
      handleUploadError
    }
  }
}
</script>

<style scoped>
.reimbursement-container {
  padding: 20px;
}
</style>