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
          <div class="file-list-container">
            <div
              v-for="(attachment, index) in reimbursementData.attachments"
              :key="index"
              class="file-card"
            >
              <!-- 文件预览/图标 -->
              <div class="file-preview">
                <el-image
                  v-if="isImage(attachment)"
                  :src="getAttachmentUrl(attachment)"
                  fit="cover"
                  class="preview-image"
                  :preview-src-list="getImageAttachments()"
                  :initial-index="getImageIndex(attachment)"
                  :preview-teleported="true"
                  :hide-on-click-modal="true"
                  :z-index="9999"
                  @click.stop
                />
                <el-icon v-else :size="48" :color="getFileIconColor(attachment)">
                  <component :is="getFileIcon(attachment)" />
                </el-icon>
              </div>

              <!-- 文件信息 -->
              <div class="file-info">
                <div class="file-name" :title="getFileName(attachment)">
                  <el-link 
                    :href="getAttachmentUrl(attachment)" 
                    target="_blank" 
                    :underline="false"
                    class="file-link"
                  >
                    {{ getFileName(attachment) }}
                  </el-link>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else>
          <el-empty description="暂无附件" :image-size="60" />
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
import { Picture, Document, Grid, Folder } from '@element-plus/icons-vue'
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

// 判断是否为图片（兼容新旧格式）
function isImage(attachment) {
  const url = getAttachmentUrl(attachment)
  return /\.(jpg|jpeg|png|gif|bmp|webp|tiff)$/i.test(url)
}

// 获取文件名（兼容新旧格式）
function getFileName(attachment) {
  if (typeof attachment === 'string') {
    // 旧格式：纯字符串URL
    return attachment.substring(attachment.lastIndexOf('/') + 1)
  } else {
    // 新格式：{ url, originalName }
    return attachment.originalName || attachment.url.substring(attachment.url.lastIndexOf('/') + 1)
  }
}

// 获取附件URL（兼容新旧格式）
function getAttachmentUrl(attachment) {
  return typeof attachment === 'string' ? attachment : attachment.url
}

// 获取图片附件列表
function getImageAttachments() {
  if (!reimbursementData.value?.attachments) return []
  return reimbursementData.value.attachments
    .filter((attachment) => isImage(attachment))
    .map((attachment) => getAttachmentUrl(attachment))
}

// 获取图片索引
function getImageIndex(attachment) {
  const url = getAttachmentUrl(attachment)
  const imageAttachments = getImageAttachments()
  return imageAttachments.indexOf(url)
}

// 获取文件类型图标（兼容新旧格式）
const getFileIcon = (attachment) => {
  const url = getAttachmentUrl(attachment).toLowerCase()
  if (url.endsWith('.pdf')) return Document
  if (url.endsWith('.doc') || url.endsWith('.docx')) return Document
  if (url.endsWith('.xls') || url.endsWith('.xlsx')) return Grid
  if (url.endsWith('.zip')) return Folder
  return Document
}

// 获取文件图标颜色（兼容新旧格式）
const getFileIconColor = (attachment) => {
  const url = getAttachmentUrl(attachment).toLowerCase()
  if (url.endsWith('.pdf')) return '#F40F02'
  if (url.endsWith('.doc') || url.endsWith('.docx')) return '#2B579A'
  if (url.endsWith('.xls') || url.endsWith('.xlsx')) return '#217346'
  if (url.endsWith('.zip')) return '#FFB900'
  return '#909399'
}

// Handle close action
const handleClose = () => {
  dialogVisible.value = false
  emit('close')
}
</script>

<style scoped>
/* 附件列表样式优化 */
.file-list-container {
  display: flex !important;
  flex-wrap: wrap !important;
  gap: 16px !important;
  margin-top: 16px;
  width: 100%;
}

.file-card {
  position: relative;
  width: 120px !important;
  height: 160px !important; /* 固定高度 */
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 8px;
  box-sizing: border-box; 
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.file-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.file-preview {
  width: 100px !important;
  height: 100px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
  overflow: hidden; 
  flex-shrink: 0;
}

/* 强制图片填满容器 */
.file-preview :deep(.el-image),
.file-preview .preview-image {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.file-preview :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover !important;
}

.file-info {
  width: 100%;
  text-align: center;
}

.file-name {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.2;
}

.file-link {
  font-size: 12px;
  color: #606266;
  text-decoration: none;
}

.file-link:hover {
  color: #409eff;
}
</style>
