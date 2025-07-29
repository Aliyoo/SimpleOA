<template>
  <el-dialog
    v-model="dialogVisible"
    width="70%"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    @close="handleClose"
  >
    <template #header>
      <span>报销详情</span>
    </template>
    <div v-loading="loading" style="min-height: 200px">
      <div v-if="reimbursementData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题">{{ reimbursementData.title }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{
            reimbursementData.applicant?.realName || reimbursementData.applicant?.username || '未知'
          }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ reimbursementData.project?.name || '无' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getReimbursementStatusTagType(reimbursementData.status)">
              {{ formatReimbursementStatus(reimbursementData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDate(reimbursementData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="总金额">{{ formatMoney(reimbursementData.totalAmount) }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="reimbursementData.items" style="width: 100%; margin-top: 20px">
          <el-table-column prop="expenseDate" label="日期" width="150">
            <template #default="scope">
              {{ formatDate(scope.row.expenseDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="itemCategory" label="类别" width="150"></el-table-column>
          <el-table-column prop="description" label="描述"></el-table-column>
          <el-table-column prop="amount" label="金额 (¥)" width="150">
            <template #default="scope">
              {{ formatMoney(scope.row.amount) }}
            </template>
          </el-table-column>
          <el-table-column label="预算来源" width="180">
            <template #default="scope">
              {{ scope.row.budget?.name || scope.row.budgetItem?.name || '无' }}
            </template>
          </el-table-column>
        </el-table>

        <el-divider>附件</el-divider>
        <div v-if="reimbursementData.attachments && reimbursementData.attachments.length > 0">
          <div
            v-for="(attachment, index) in reimbursementData.attachments"
            :key="index"
            style="display: inline-block; margin-right: 10px; margin-bottom: 10px"
          >
            <template v-if="isImage(attachment)">
              <el-image
                :src="attachment"
                style="width: 100px; height: 100px"
                fit="cover"
                :preview-src-list="getImageAttachments()"
                :initial-index="getImageIndex(attachment)"
              >
                <template #placeholder>
                  <div class="image-slot">图片加载中...</div>
                </template>
                <template #error>
                  <div class="image-slot">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </template>
            <template v-else>
              <div class="file-attachment">
                <el-link :href="attachment" target="_blank" type="primary">{{ getFileName(attachment) }}</el-link>
              </div>
            </template>
          </div>
        </div>
        <div v-else>
          <el-empty description="暂无附件" />
        </div>
      </div>
      <div v-else-if="!loading" style="text-align: center; color: #999; padding: 40px">无数据</div>
    </div>
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import api from '../utils/axios.js'
import { formatMoney, formatDate, formatReimbursementStatus, getReimbursementStatusTagType } from '../utils/format.js'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  reimbursement: {
    type: [Object, Number],
    default: null
  }
})

// Emits
const emit = defineEmits(['close'])

// Local state
const dialogVisible = ref(props.visible)
const reimbursementData = ref(null)
const loading = ref(false)

// Fetch reimbursement data
const fetchData = async () => {
  console.log('ReimbursementDetail fetchData called with:', props.reimbursement)
  console.log('Type of reimbursement prop:', typeof props.reimbursement)
  
  if (typeof props.reimbursement === 'number') {
    // Fetch by ID
    console.log('Fetching by ID:', props.reimbursement)
    loading.value = true
    try {
      const response = await api.get(`/api/oa/reimbursement/${props.reimbursement}`)
      console.log('API response in detail component:', response.data)
      reimbursementData.value = response.data.data || response.data
    } catch (error) {
      console.error('获取报销信息失败:', error)
      ElMessage.error('获取报销信息失败')
      reimbursementData.value = null
    } finally {
      loading.value = false
    }
  } else if (props.reimbursement) {
    // Use provided object
    console.log('Using provided object:', props.reimbursement)
    reimbursementData.value = props.reimbursement
  } else {
    console.log('No reimbursement data provided')
    reimbursementData.value = null
  }
  
  console.log('Final reimbursementData in detail component:', reimbursementData.value)
}

// Watch visible prop
watch(
  () => props.visible,
  (newVal) => {
    console.log('Visible changed to:', newVal)
    dialogVisible.value = newVal
    if (newVal && props.reimbursement) {
      console.log('Calling fetchData from visible watch')
      fetchData()
    }
  },
  { immediate: true }
)

// Watch reimbursement prop
watch(
  () => props.reimbursement,
  (newVal) => {
    console.log('Reimbursement prop changed to:', newVal)
    if (newVal && props.visible) {
      console.log('Calling fetchData from reimbursement watch')
      fetchData()
    }
  },
  { immediate: true }
)

// 格式化函数现在从统一的格式化工具导入

// 判断是否为图片
function isImage(url) {
  return /\.(jpg|jpeg|png|gif|bmp|webp|tiff)$/.test(url.toLowerCase())
}

// 获取文件名
function getFileName(url) {
  return url.substring(url.lastIndexOf('/') + 1)
}

// 获取图片附件
function getImageAttachments() {
  if (!reimbursementData.value?.attachments) return []
  return reimbursementData.value.attachments.filter((attachment) => isImage(attachment))
}

// 获取图片索引
function getImageIndex(url) {
  const imageAttachments = getImageAttachments()
  return imageAttachments.indexOf(url)
}

// Handle close action
const handleClose = () => {
  dialogVisible.value = false
  emit('close')
}
</script>

<style scoped>
.image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100px;
  background: #f2f2f2;
}

.file-attachment {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
  text-align: center;
  word-break: break-all;
  padding: 5px;
}

.file-attachment:hover {
  border-color: #409eff;
  background: #f0f9ff;
}
</style>
