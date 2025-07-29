<template>
  <div class="system-config-container">
    <h1>系统配置</h1>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="基础配置" name="basicConfig">
        <div class="operation-bar">
          <el-button type="primary" @click="handleCreateConfig">新建配置</el-button>
          <el-button type="success" @click="handleInitializeConfig">初始化默认配置</el-button>
          <el-input v-model="configSearch" placeholder="搜索配置" style="width: 300px; margin-left: 10px" clearable />
        </div>

        <el-table :data="filteredConfigs" style="width: 100%; margin-top: 20px">
          <el-table-column prop="configKey" label="配置键" width="200" />
          <el-table-column prop="configValue" label="配置值" width="200" />
          <el-table-column prop="description" label="描述" width="250" />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="scope">
              <el-tag :type="getTagType(scope.row.category)">
                {{ scope.row.category }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isSystem" label="系统配置" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.isSystem ? 'info' : 'success'">
                {{ scope.row.isSystem ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isEditable" label="可编辑" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.isEditable ? 'success' : 'info'">
                {{ scope.row.isEditable ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" :disabled="!scope.row.isEditable" @click="handleEditConfig(scope.row)"
                >编辑</el-button
              >
              <el-button
                size="small"
                type="danger"
                :disabled="scope.row.isSystem"
                @click="handleDeleteConfig(scope.row)"
                >删除</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="分类配置" name="categoryConfig">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="category-list">
              <h3>配置分类</h3>
              <el-menu :default-active="selectedCategory" class="category-menu" @select="handleCategorySelect">
                <el-menu-item v-for="category in categories" :key="category" :index="category">
                  <span>{{ category }}</span>
                </el-menu-item>
              </el-menu>
            </div>
          </el-col>
          <el-col :span="18">
            <div v-if="selectedCategory" class="category-configs">
              <h3>{{ selectedCategory }} 配置</h3>
              <el-form :model="categoryConfigForm" label-width="180px" class="category-config-form">
                <el-form-item
                  v-for="config in categoryConfigs"
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
                  <el-input
                    v-if="isTextConfig(config.configKey)"
                    v-model="categoryConfigForm[config.configKey]"
                    :disabled="!config.isEditable"
                  />
                  <el-input-number
                    v-else-if="isNumberConfig(config.configKey)"
                    v-model="categoryConfigForm[config.configKey]"
                    :disabled="!config.isEditable"
                  />
                  <el-switch
                    v-else-if="isBooleanConfig(config.configKey)"
                    v-model="categoryConfigForm[config.configKey]"
                    :disabled="!config.isEditable"
                    active-text="是"
                    inactive-text="否"
                  />
                  <el-input v-else v-model="categoryConfigForm[config.configKey]" :disabled="!config.isEditable" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingCategoryConfigs" @click="saveCategoryConfigs"
                    >保存配置</el-button
                  >
                </el-form-item>
              </el-form>
            </div>
            <div v-else class="no-category-selected">
              <el-empty description="请选择一个配置分类"></el-empty>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 配置表单对话框 -->
    <el-dialog
      v-model="configDialogVisible"
      :title="configForm.id ? '编辑配置' : '新建配置'"
      width="50%"
      @closed="resetConfigForm"
    >
      <el-form ref="configFormRef" :model="configForm" label-width="120px" :rules="configRules">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="configForm.configKey" placeholder="请输入配置键" :disabled="!!configForm.id" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="configForm.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="configForm.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="configForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
            <el-option label="新分类" value="NEW" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="configForm.category === 'NEW'" label="新分类名称" prop="newCategory">
          <el-input v-model="configForm.newCategory" placeholder="请输入新分类名称" />
        </el-form-item>
        <el-form-item label="系统配置" prop="isSystem">
          <el-switch
            v-model="configForm.isSystem"
            :active-value="true"
            :inactive-value="false"
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>
        <el-form-item label="可编辑" prop="isEditable">
          <el-switch
            v-model="configForm.isEditable"
            :active-value="true"
            :inactive-value="false"
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="configDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submittingConfig" @click="submitConfigForm"> 确定 </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/axios.js'

const activeTab = ref('basicConfig')

const configs = ref([])
const configSearch = ref('')
const configDialogVisible = ref(false)
const submittingConfig = ref(false)
const configFormRef = ref(null)

const initialConfigFormState = () => ({
  id: null,
  configKey: '',
  configValue: '',
  description: '',
  category: '',
  newCategory: '',
  isSystem: false,
  isEditable: true
})
const configForm = reactive(initialConfigFormState())

const categories = ref([])
const selectedCategory = ref('')
const categoryConfigs = ref([])
const categoryConfigForm = reactive({})
const savingCategoryConfigs = ref(false)

const filteredConfigs = computed(() => {
  if (!configSearch.value) return configs.value
  const searchLower = configSearch.value.toLowerCase()
  return configs.value.filter(
    (item) =>
      item.configKey?.toLowerCase().includes(searchLower) ||
      item.description?.toLowerCase().includes(searchLower) ||
      item.category?.toLowerCase().includes(searchLower)
  )
})

const validateNewCategory = (rule, value, callback) => {
  if (configForm.category === 'NEW' && !value) {
    callback(new Error('请输入新分类名称'))
  } else {
    callback()
  }
}

const configRules = {
  configKey: [
    { required: true, message: '请输入配置键', trigger: 'blur' },
    { pattern: /^[a-z0-9_.]+$/, message: '配置键只能包含小写字母、数字、下划线和点', trigger: 'blur' }
  ],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }],
  newCategory: [{ validator: validateNewCategory, trigger: 'blur' }],
  isSystem: [{ required: true, message: '请选择是否系统配置', trigger: 'change' }],
  isEditable: [{ required: true, message: '请选择是否可编辑', trigger: 'change' }]
}

const fetchConfigs = async () => {
  try {
    const response = await api.get('/api/system/configs')
    configs.value = response.data
    const uniqueCategories = [...new Set(response.data.map((c) => c.category).filter(Boolean))]
    categories.value = uniqueCategories.sort()
    if (!selectedCategory.value && categories.value.length > 0) {
      const currentCategoryExists = categories.value.includes(selectedCategory.value)
      selectedCategory.value = currentCategoryExists ? selectedCategory.value : categories.value[0]
    }
    if (selectedCategory.value && !categories.value.includes(selectedCategory.value)) {
      selectedCategory.value = categories.value.length > 0 ? categories.value[0] : ''
    }
  } catch (error) {
    ElMessage.error('获取配置列表失败: ' + error.message)
    configs.value = []
    categories.value = []
    selectedCategory.value = ''
  }
}

const resetConfigForm = () => {
  Object.assign(configForm, initialConfigFormState())
  configFormRef.value?.clearValidate()
}

const handleCreateConfig = () => {
  resetConfigForm()
  configDialogVisible.value = true
}

const handleEditConfig = (config) => {
  const configData = {
    ...config,
    isSystem: Boolean(config.isSystem),
    isEditable: Boolean(config.isEditable)
  }
  Object.assign(configForm, configData)
  configDialogVisible.value = true
  nextTick(() => configFormRef.value?.clearValidate())
}

const submitConfigForm = async () => {
  if (!configFormRef.value) return
  await configFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingConfig.value = true
      try {
        let payload = { ...configForm }
        if (payload.category === 'NEW') {
          if (!payload.newCategory) {
            ElMessage.error('请输入新分类名称')
            submittingConfig.value = false
            return
          }
          payload.category = payload.newCategory.trim()
        }
        delete payload.newCategory
        payload.isSystem = Boolean(payload.isSystem)
        payload.isEditable = Boolean(payload.isEditable)

        if (payload.id) {
          await api.put(`/api/system/configs/${payload.id}`, payload)
          ElMessage.success('配置更新成功')
        } else {
          await api.post('/api/system/configs', payload)
          ElMessage.success('配置创建成功')
        }
        configDialogVisible.value = false
        await fetchConfigs()
      } catch (error) {
        ElMessage.error('操作失败: ' + error.message)
      } finally {
        submittingConfig.value = false
      }
    }
  })
}

const handleDeleteConfig = async (config) => {
  if (config.isSystem) {
    ElMessage.warning('系统配置无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除配置 "${config.configKey}"?`, '提示', { type: 'warning' })
    await api.delete(`/api/system/configs/${config.id}`)
    ElMessage.success('删除成功')
    fetchConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const handleInitializeConfig = async () => {
  try {
    await ElMessageBox.confirm('确定要初始化默认配置吗？这将覆盖现有部分配置。', '提示', { type: 'warning' })
    await api.post('/api/system/configs/initialize')
    ElMessage.success('默认配置初始化成功')
    fetchConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('初始化失败: ' + error.message)
    }
  }
}

const handleCategorySelect = (index) => {
  if (selectedCategory.value !== index) {
    selectedCategory.value = index
  }
}

watch(
  selectedCategory,
  (newCategory) => {
    Object.keys(categoryConfigForm).forEach((key) => delete categoryConfigForm[key])

    if (!newCategory || !configs.value) {
      categoryConfigs.value = []
      return
    }
    categoryConfigs.value = configs.value.filter((c) => c.category === newCategory)

    categoryConfigs.value.forEach((config) => {
      let value = config.configValue
      if (isBooleanConfig(config.configKey)) {
        value = value === 'true' || value === '1'
      } else if (isNumberConfig(config.configKey)) {
        const num = parseFloat(value)
        if (!isNaN(num)) value = num
      }
      categoryConfigForm[config.configKey] = value
    })
  },
  { immediate: true }
)

const saveCategoryConfigs = async () => {
  savingCategoryConfigs.value = true
  let hasChanges = false
  const updates = []

  try {
    const originalConfigs = configs.value.filter((c) => c.category === selectedCategory.value)

    originalConfigs.forEach((config) => {
      const formValue = categoryConfigForm[config.configKey]
      const originalValue = config.configValue
      let formValueAsString = String(formValue)

      if (isBooleanConfig(config.configKey)) {
        formValueAsString = formValue ? 'true' : 'false'
      }

      if (formValueAsString !== String(originalValue)) {
        updates.push({
          id: config.id,
          configKey: config.configKey,
          configValue: formValueAsString
        })
        hasChanges = true
      }
    })

    if (hasChanges) {
      await api.put('/api/system/configs/batch', updates)
      ElMessage.success(`"${selectedCategory.value}" 分类配置已保存`)
      await fetchConfigs()
    } else {
      ElMessage.info('配置未更改')
    }
  } catch (error) {
    ElMessage.error('保存分类配置失败: ' + error.message)
  } finally {
    savingCategoryConfigs.value = false
  }
}

const isTextConfig = (key) => !isNumberConfig(key) && !isBooleanConfig(key)
const isNumberConfig = (key) =>
  key?.toLowerCase().includes('count') ||
  key?.toLowerCase().includes('limit') ||
  key?.toLowerCase().includes('size') ||
  key?.toLowerCase().includes('duration') ||
  key?.toLowerCase().includes('interval') ||
  key?.toLowerCase().includes('port')
const isBooleanConfig = (key) =>
  key?.toLowerCase().startsWith('is_') ||
  key?.toLowerCase().startsWith('enable_') ||
  key?.toLowerCase().includes('enabled') ||
  key?.toLowerCase().includes('active')

const getTagType = (category) => {
  if (!category) return 'info'
  const types = ['primary', 'success', 'info', 'warning', 'danger']
  let hash = 0
  for (let i = 0; i < category.length; i++) {
    hash = category.charCodeAt(i) + ((hash << 5) - hash)
  }
  return types[Math.abs(hash) % types.length]
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
.system-config-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.category-list {
  border-right: 1px solid #ebeef5;
  padding-right: 15px;
  height: calc(100vh - 200px);
  overflow-y: auto;
}

.category-menu {
  border-right: none;
}

.category-configs {
  padding-left: 15px;
}

.category-config-form .el-form-item {
  margin-bottom: 18px;
}

.no-category-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: #909399;
}
</style>
