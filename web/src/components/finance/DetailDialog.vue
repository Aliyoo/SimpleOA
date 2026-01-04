<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="`报销详情 - ${data?.title || ''}`"
    width="900px"
    @close="handleClose"
  >
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 基本信息 Tab -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报销ID">{{ data?.id }}</el-descriptions-item>
          <el-descriptions-item label="报销标题">{{ data?.title }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ data?.applicant?.realName }}</el-descriptions-item>
          <el-descriptions-item label="申请人账号">{{ data?.applicant?.username }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ data?.applicant?.department?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联项目">{{ data?.project?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报销总金额">
            <span style="color: #f56c6c; font-weight: bold; font-size: 18px">
              ¥{{ data?.totalAmount }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(data?.status)">{{ getStatusText(data?.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ data?.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ data?.updateTime }}</el-descriptions-item>
          <el-descriptions-item label="报销说明" :span="2">
            {{ data?.description || '无' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- 费用明细 Tab -->
      <el-tab-pane label="费用明细" name="items">
        <el-table :data="data?.items || []" border stripe>
          <el-table-column prop="expenseDate" label="费用日期" width="120" align="center" />
          <el-table-column prop="itemCategory" label="费用类别" width="120" align="center" />
          <el-table-column prop="description" label="费用说明" min-width="200" />
          <el-table-column prop="amount" label="金额(元)" width="120" align="right">
            <template #default="scope">
              <span style="color: #f56c6c; font-weight: bold">¥{{ scope.row.amount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="invoiceNumber" label="发票号" width="150" align="center" />
          <el-table-column label="附件" width="100" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.attachment" type="success" size="small">有</el-tag>
              <el-tag v-else type="info" size="small">无</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-divider />

        <el-row :gutter="20">
          <el-col :span="12">
            <el-statistic title="明细数量" :value="data?.items?.length || 0" />
          </el-col>
          <el-col :span="12">
            <el-statistic title="总计金额" :value="data?.totalAmount || 0" :precision="2" prefix="¥" />
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 附件 Tab -->
      <el-tab-pane label="附件" name="attachments">
        <div v-if="data?.attachments && data.attachments.length > 0">
          <div class="file-list-container">
            <div
              v-for="(attachment, index) in data.attachments"
              :key="index"
              class="file-card"
            >
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
        <el-empty v-else description="暂无附件" :image-size="60" />
      </el-tab-pane>

      <!-- 审批历史 Tab -->
      <el-tab-pane label="审批历史" name="approval">
        <el-timeline>
          <el-timeline-item
            v-for="(record, index) in approvalHistory"
            :key="index"
            :timestamp="record.timestamp"
            :type="record.type"
            placement="top"
          >
            <el-card>
              <h4>{{ record.title }}</h4>
              <p><strong>操作人:</strong> {{ record.operator }}</p>
              <p v-if="record.comment"><strong>意见:</strong> {{ record.comment }}</p>
              <p><strong>时间:</strong> {{ record.timestamp }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>

      <!-- 财务审查 Tab -->
      <el-tab-pane label="财务审查" name="finance">
        <el-timeline v-if="data?.status === 'PENDING_FINANCE_REVIEW' || hasFinanceReview">
          <el-timeline-item
            v-if="data?.status === 'PENDING_FINANCE_REVIEW'"
            timestamp="待审查"
            type="warning"
            placement="top"
          >
            <el-alert
              title="等待财务审查"
              type="warning"
              description="此申请正在等待财务人员进行审查"
              :closable="false"
              show-icon
            />
          </el-timeline-item>

          <el-timeline-item
            v-for="review in financeReviews"
            :key="review.id"
            :timestamp="review.reviewTime"
            :type="review.decision === 'APPROVE' ? 'success' : 'danger'"
            placement="top"
          >
            <el-card>
              <h4>{{ review.decision === 'APPROVE' ? '审查通过' : '审查拒绝' }}</h4>
              <p><strong>审查人:</strong> {{ review.reviewerName }}</p>
              <p v-if="review.comment"><strong>审查意见:</strong> {{ review.comment }}</p>
              <p><strong>审查时间:</strong> {{ review.reviewTime }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-else description="暂无财务审查记录" />
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Document, Grid, Folder } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  data: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'update:visible'])

const activeTab = ref('basic')

// 计算是否有财务审查记录
const hasFinanceReview = computed(() => {
  return props.data?.financeReviews && props.data.financeReviews.length > 0
})

// 财务审查记录
const financeReviews = computed(() => {
  return props.data?.financeReviews || []
})

// 审批历史(模拟数据,实际应该从后端获取)
const approvalHistory = computed(() => {
  const history = []

  // 提交记录
  if (props.data?.createTime) {
    history.push({
      title: '提交申请',
      operator: props.data?.applicant?.realName || '未知',
      timestamp: props.data.createTime,
      type: 'primary',
      comment: props.data?.description || '提交报销申请'
    })
  }

  // 财务审查记录
  if (hasFinanceReview.value) {
    financeReviews.value.forEach(review => {
      history.push({
        title: review.decision === 'APPROVE' ? '财务审查通过' : '财务审查拒绝',
        operator: review.reviewerName,
        timestamp: review.reviewTime,
        type: review.decision === 'APPROVE' ? 'success' : 'danger',
        comment: review.comment
      })
    })
  }

  // 领导审批记录(如果有)
  if (props.data?.status === 'APPROVED' || props.data?.status === 'REJECTED') {
    history.push({
      title: props.data?.status === 'APPROVED' ? '领导审批通过' : '领导审批拒绝',
      operator: props.data?.approver?.realName || '系统',
      timestamp: props.data?.approvalTime || props.data?.updateTime,
      type: props.data?.status === 'APPROVED' ? 'success' : 'danger',
      comment: props.data?.approvalComment || '审批完成'
    })
  }

  return history
})

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'PENDING_LEADER_APPROVAL': '待领导审批',
    'PENDING_FINANCE_REVIEW': '待财务审查',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return statusMap[status] || status
}

// 获取状态标签类型
const getStatusType = (status) => {
  const typeMap = {
    'PENDING_LEADER_APPROVAL': 'info',
    'PENDING_FINANCE_REVIEW': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return typeMap[status] || 'info'
}

const handleClose = () => {
  emit('close')
  emit('update:visible', false)
}

// 监听visible变化,重置tab
watch(() => props.visible, (newVal) => {
  if (newVal) {
    activeTab.value = 'basic'
  }
})

// 附件相关函数（兼容新旧格式）
const getAttachmentUrl = (attachment) => {
  return typeof attachment === 'string' ? attachment : attachment.url
}

const getFileName = (attachment) => {
  if (typeof attachment === 'string') {
    return attachment.substring(attachment.lastIndexOf('/') + 1)
  }
  return attachment.originalName || attachment.url.substring(attachment.url.lastIndexOf('/') + 1)
}

const isImage = (attachment) => {
  const url = getAttachmentUrl(attachment)
  return /\.(jpg|jpeg|png|gif|bmp|webp|tiff)$/i.test(url)
}

const getImageAttachments = () => {
  if (!props.data?.attachments) return []
  return props.data.attachments
    .filter((att) => isImage(att))
    .map((att) => getAttachmentUrl(att))
}

const getImageIndex = (attachment) => {
  const url = getAttachmentUrl(attachment)
  return getImageAttachments().indexOf(url)
}

const getFileIcon = (attachment) => {
  const url = getAttachmentUrl(attachment).toLowerCase()
  if (url.endsWith('.pdf')) return Document
  if (url.endsWith('.doc') || url.endsWith('.docx')) return Document
  if (url.endsWith('.xls') || url.endsWith('.xlsx')) return Grid
  if (url.endsWith('.zip')) return Folder
  return Document
}

const getFileIconColor = (attachment) => {
  const url = getAttachmentUrl(attachment).toLowerCase()
  if (url.endsWith('.pdf')) return '#F40F02'
  if (url.endsWith('.doc') || url.endsWith('.docx')) return '#2B579A'
  if (url.endsWith('.xls') || url.endsWith('.xlsx')) return '#217346'
  if (url.endsWith('.zip')) return '#FFB900'
  return '#909399'
}
</script>

<style scoped>
:deep(.el-descriptions__label) {
  width: 120px;
  font-weight: 500;
}

:deep(.el-timeline-item__timestamp) {
  font-weight: 500;
}

:deep(.el-timeline-item__wrapper) {
  padding-left: 20px;
}

:deep(.el-card) {
  margin-bottom: 10px;
}

:deep(.el-card h4) {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #303133;
}

:deep(.el-card p) {
  margin: 5px 0;
  color: #606266;
}

/* 附件列表样式 */
.file-list-container {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
}

.file-card {
  width: 120px;
  height: 160px;
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
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.file-preview :deep(.el-image),
.file-preview .preview-image {
  width: 100%;
  height: 100%;
}

.file-preview :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-info {
  width: 100%;
  text-align: center;
}

.file-name {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-link {
  font-size: 12px;
  color: #606266;
}

.file-link:hover {
  color: #409eff;
}
</style>
