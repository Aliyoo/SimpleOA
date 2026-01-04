<template>
  <div class="quick-tags">
    <el-tag
      v-for="tag in tags"
      :key="tag.key"
      :type="activeTag === tag.key ? tag.type : 'info'"
      :effect="activeTag === tag.key ? 'dark' : 'plain'"
      class="tag-item"
      @click="handleTagClick(tag)"
    >
      <el-icon v-if="tag.icon">
        <component :is="tag.icon" />
      </el-icon>
      {{ tag.label }}
    </el-tag>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  List,
  Clock,
  Calendar,
  Money,
  Warning
} from '@element-plus/icons-vue'

const activeTag = ref('all')

const tags = [
  { key: 'all', label: '全部', type: 'info', icon: List },
  { key: 'pending', label: '待我审查', type: 'warning', icon: Clock },
  { key: 'thisWeek', label: '本周', type: 'primary', icon: Calendar },
  { key: 'thisMonth', label: '本月', type: 'primary', icon: Calendar },
  { key: 'largeAmount', label: '大额(>5000)', type: 'danger', icon: Money },
  { key: 'overdue', label: '逾期未审', type: 'warning', icon: Warning }
]

const emit = defineEmits(['tag-click'])

const handleTagClick = (tag) => {
  activeTag.value = tag.key
  emit('tag-click', tag)
}
</script>

<style scoped>
.quick-tags {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.tag-item :deep(.el-icon) {
  font-size: 14px;
}
</style>
