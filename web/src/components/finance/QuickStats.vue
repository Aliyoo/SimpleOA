<template>
  <el-row :gutter="20" class="quick-stats">
    <el-col :span="4" class="stat-item">
      <el-card class="stat-card pending-review" shadow="hover" @click="handleCardClick('PENDING_FINANCE_REVIEW')">
        <el-statistic title="待财务审查" :value="stats.pendingReview.count">
          <template #prefix>
            <el-icon color="#E6A23C"><Clock /></el-icon>
          </template>
        </el-statistic>
        <div class="stat-amount">¥{{ formatAmount(stats.pendingReview.amount) }}</div>
      </el-card>
    </el-col>

    <el-col :span="4" class="stat-item">
      <el-card class="stat-card pending-leader" shadow="hover" @click="handleCardClick('PENDING_LEADER_APPROVAL')">
        <el-statistic title="待领导审批" :value="stats.pendingLeader.count">
          <template #prefix>
            <el-icon color="#409EFF"><View /></el-icon>
          </template>
        </el-statistic>
        <div class="stat-amount">¥{{ formatAmount(stats.pendingLeader.amount) }}</div>
      </el-card>
    </el-col>

    <el-col :span="4" class="stat-item">
      <el-card class="stat-card approved" shadow="hover" @click="handleCardClick('APPROVED')">
        <el-statistic title="已通过" :value="stats.approved.count">
          <template #prefix>
            <el-icon color="#67C23A"><CircleCheck /></el-icon>
          </template>
        </el-statistic>
        <div class="stat-amount">¥{{ formatAmount(stats.approved.amount) }}</div>
      </el-card>
    </el-col>

    <el-col :span="4" class="stat-item">
      <el-card class="stat-card rejected" shadow="hover" @click="handleCardClick('REJECTED')">
        <el-statistic title="已拒绝" :value="stats.rejected.count">
          <template #prefix>
            <el-icon color="#F56C6C"><CircleClose /></el-icon>
          </template>
        </el-statistic>
        <div class="stat-amount">¥{{ formatAmount(stats.rejected.amount) }}</div>
      </el-card>
    </el-col>

    <el-col :span="4" class="stat-item">
      <el-card class="stat-card total" shadow="hover" @click="handleCardClick('all')">
        <el-statistic title="总计" :value="stats.total.count">
          <template #prefix>
            <el-icon color="#909399"><DataLine /></el-icon>
          </template>
        </el-statistic>
        <div class="stat-amount">¥{{ formatAmount(stats.total.amount) }}</div>
      </el-card>
    </el-col>

    <el-col :span="4" class="stat-item">
      <el-card class="stat-card average" shadow="hover">
        <el-statistic title="平均金额" :value="stats.averageAmount" :precision="2" prefix="¥">
          <template #prefix>
            <el-icon color="#909399"><TrendCharts /></el-icon>
          </template>
        </el-statistic>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { Clock, View, CircleCheck, CircleClose, DataLine, TrendCharts } from '@element-plus/icons-vue'

const props = defineProps({
  stats: {
    type: Object,
    required: true,
    default: () => ({
      pendingReview: { count: 0, amount: 0 },
      pendingLeader: { count: 0, amount: 0 },
      approved: { count: 0, amount: 0 },
      rejected: { count: 0, amount: 0 },
      total: { count: 0, amount: 0 },
      averageAmount: 0
    })
  }
})

const emit = defineEmits(['filter-by-status'])

// 格式化金额显示
const formatAmount = (amount) => {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 处理卡片点击事件
const handleCardClick = (status) => {
  emit('filter-by-status', status)
}
</script>

<style scoped>
.quick-stats {
  margin-bottom: 20px;
}

.stat-item {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-item:hover {
  transform: translateY(-2px);
}

.stat-card {
  border-radius: 8px;
  border: 1px solid #EBEEF5;
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card.pending-review {
  border-top: 3px solid #E6A23C;
}

.stat-card.pending-leader {
  border-top: 3px solid #409EFF;
}

.stat-card.approved {
  border-top: 3px solid #67C23A;
}

.stat-card.rejected {
  border-top: 3px solid #F56C6C;
}

.stat-card.total {
  border-top: 3px solid #909399;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card.total :deep(.el-statistic__head),
.stat-card.total :deep(.el-statistic__content) {
  color: white;
}

.stat-card.average {
  border-top: 3px solid #909399;
}

.stat-amount {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.stat-card.total .stat-amount {
  color: white;
}

:deep(.el-statistic__head) {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

:deep(.el-statistic__content) {
  font-size: 24px;
  font-weight: bold;
}

:deep(.el-card__body) {
  padding: 15px;
}
</style>
