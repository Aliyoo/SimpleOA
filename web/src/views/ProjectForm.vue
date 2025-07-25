<template>
  <div class="project-form-container">
    <h1>{{ formTitle }}</h1>
    <el-form :model="projectForm" :rules="rules" ref="projectFormRef" label-width="120px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="projectForm.name"></el-input>
      </el-form-item>
      <el-form-item label="开始日期" prop="startDate">
        <el-date-picker v-model="projectForm.startDate" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD"></el-date-picker>
      </el-form-item>
      <el-form-item label="结束日期" prop="endDate">
        <el-date-picker v-model="projectForm.endDate" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD"></el-date-picker>
      </el-form-item>
      <el-form-item label="项目经理" prop="managerId">
        <el-select v-model="projectForm.managerId" placeholder="请选择项目经理">
          <el-option
            v-for="member in memberOptions"
            :key="member.id"
            :label="member.name"
            :value="member.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="项目成员" prop="memberIds">
        <el-select v-model="projectForm.memberIds" multiple placeholder="请选择成员" style="width: 100%;">
          <el-option
            v-for="member in memberOptions"
            :key="member.id"
            :label="member.name"
            :value="member.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="项目描述" prop="description">
        <el-input type="textarea" v-model="projectForm.description" rows="4"></el-input>
      </el-form-item>
      <el-form-item label="项目状态" prop="status">
        <el-select v-model="projectForm.status" placeholder="请选择项目状态">
          <!-- <el-option label="规划中" :value="'PLANNING'"></el-option>
          <el-option label="进行中" :value="'IN_PROGRESS'"></el-option> -->
          <el-option label="需求阶段" :value="'REQUIREMENT'"></el-option>
          <el-option label="设计阶段" :value="'DESIGN'"></el-option>
          <el-option label="开发阶段" :value="'DEVELOPMENT'"></el-option>
          <el-option label="终验阶段" :value="'ACCEPTANCE'"></el-option>
          <el-option label="已完成" :value="'COMPLETED'"></el-option>
          <el-option label="已取消" :value="'CANCELLED'"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input v-model="projectForm.priority"></el-input>
      </el-form-item>
      <el-form-item label="项目类型" prop="type">
        <el-input v-model="projectForm.type"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">提交</el-button>
        <el-button @click="resetForm">重置</el-button>
        <el-button @click="goBack">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, defineEmits } from 'vue'
import api from '../utils/axios.js'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const memberOptions = ref([])
const projectFormRef = ref(null)

const projectForm = ref({
  id: null,
  name: '',
  description: '',
  startDate: '',
  endDate: '',
  managerId: null,
  memberIds: [],
  status: '', // 默认状态为空
  priority: '0', // 默认优先级
  type: '0', // 默认类型
})

const formTitle = computed(() => {
  return route.params.id ? '编辑项目' : '创建项目'
})

const rules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  managerId: [{ required: true, message: '请选择项目经理', trigger: 'change' }],
  memberIds: [{ required: true, type:'array', message: '请选择至少一名成员', trigger: 'change' }],
  description: [{ required: false, message: '请输入项目描述', trigger: 'blur' }],
  status: [{ required: true, message: '请选择项目状态', trigger: 'change' }],
  priority: [{ required: false, message: '请输入优先级', trigger: 'blur' }],
  type: [{ required: false, message: '请输入项目类型', trigger: 'blur' }],
}

const fetchProject = async (id) => {
  try {
    const response = await api.get(`/api/projects/${id}`)
    const data = response.data;
    projectForm.value.id = data.id;
    projectForm.value.name = data.name;
    projectForm.value.description = data.description;
    projectForm.value.startDate = data.startDate;
    projectForm.value.endDate = data.endDate;
    projectForm.value.managerId = data.manager?.id || data.managerId;
    projectForm.value.memberIds = Array.isArray(data.members) ? data.members.map(m => m.id) : (data.memberIds || []);
    projectForm.value.status = data.status;
    projectForm.value.priority = data.priority;
    projectForm.value.type = data.type;
  } catch (error) {
    ElMessage.error('获取项目详情失败: ' + error.message)
  }
}

const fetchMembers = async () => {
  try {
    const response = await api.get('/api/users/selectable')
    memberOptions.value = response.data.map(user => ({ id: user.id, name: user.realName || user.username }))
  } catch (error) { 
     ElMessage.error('获取成员列表失败: ' + error.message)
  }
}

// 定义 emit 事件
const emit = defineEmits(['projectCreated']);

const submitForm = async () => {
  if (!projectFormRef.value) return;
  await projectFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 使用原始实体格式发送数据
        const payload = {
          name: projectForm.value.name,
          description: projectForm.value.description,
          startDate: projectForm.value.startDate,
          endDate: projectForm.value.endDate,
          status: projectForm.value.status,
          priority: projectForm.value.priority,
          type: projectForm.value.type,
          manager: projectForm.value.managerId ? { id: projectForm.value.managerId } : null,
          members: projectForm.value.memberIds ? projectForm.value.memberIds.map(id => ({ id })) : []
        };

        // 如果是编辑模式，添加id
        if (route.params.id) {
          payload.id = projectForm.value.id;
        }

        console.log('提交项目的 payload:', JSON.stringify(payload, null, 2));

        if (route.params.id) {
          await api.put(`/api/projects/${route.params.id}`, payload);
          ElMessage.success('项目更新成功');
        } else {
          await api.post('/api/projects', payload);
          ElMessage.success('项目创建成功');
          emit('projectCreated');
        }
        router.push('/projects');
      } catch (error) {
        console.error("保存项目失败:", error);
        const errorMsg = error.response?.data?.message || error.response?.data?.error || error.message;
        ElMessage.error(`保存项目失败: ${errorMsg}`);
      }
    }
  });
}

const resetForm = () => {
  projectForm.value.id = null;
  projectForm.value.name = '';
  projectForm.value.description = '';
  projectForm.value.startDate = '';
  projectForm.value.endDate = '';
  projectForm.value.managerId = null;
  projectForm.value.memberIds = [];
  projectForm.value.status = '';
  projectForm.value.priority = '0';
  projectForm.value.type = '0';
  nextTick(() => {
    projectFormRef.value?.clearValidate();
  });
}

const goBack = () => {
  router.push('/projects')
}

onMounted(() => {
  fetchMembers();
  if (route.params.id) {
    fetchProject(route.params.id)
  } else {
    resetForm();
  }
})
</script>

<style scoped>
.project-form-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
h1 {
    text-align: center;
    margin-bottom: 30px;
}
.el-select {
    width: 100%;
}
</style>