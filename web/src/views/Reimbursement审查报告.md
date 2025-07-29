# 前端 Reimbursement.vue 功能完整性审查报告

## 审查时间

2024年1月

## 审查总结

经过对 `Reimbursement.vue` 组件的详细审查，发现以下问题需要修复和完善：

## 1. 生命周期检查结果

### ✅ 已实现的功能：

- **仅草稿可编辑/删除**：代码第239-240行和第255-256行已实现该限制
- **状态文案映射**：第672-680行已定义完整的状态映射，包括 `PENDING_FINANCE_APPROVAL`
- **流程跳转**：第653-661行实现了查看审批的跳转功能

### ❌ 缺失的功能：

- **经理审批/财务审批待办入口**：当前只能跳转到审批管理页面查看，缺少在报销页面直接显示待办的 Tab 或区域

## 2. 状态枚举映射

### ✅ 已实现：

- 第675行已包含 `PENDING_FINANCE_APPROVAL: '待财务审批'`
- 第686行已包含对应的标签类型映射

## 3. 预算选择组件

### ✅ 已实现：

- 第76-99行已实现预算(budgetId)和预算明细(budgetItemId)选择组件
- 第523-542行实现了获取可用预算的功能
- 第1045-1088行实现了预算验证功能

## 4. 财务审批权限

### ❌ 需要修改：

- 当前代码中没有针对 `ROLE_FINANCE` 角色显示"财务审批"操作按钮的逻辑
- 审批操作都在 `Approvals.vue` 中处理，但缺少角色判断

## 5. 统计报表

### ✅ 已实现：

- 第751-815行使用了正确的日期范围和状态字段
- 第306行、第330行已实现金额格式化（使用 ¥ 符号）

## 修改建议

### 1. 添加待办审批 Tab

在报销列表页增加一个待办审批的 Tab，让经理和财务可以直接在报销页面看到需要审批的申请。

### 2. 添加财务审批按钮

需要在适当位置（如查看详情对话框）添加财务审批按钮，并根据用户角色控制显示。

### 3. 完善角色权限判断

需要添加判断用户是否具有财务角色的方法。

## 需要修改的代码

以下是需要添加或修改的代码部分：

### 1. 添加角色判断方法（在 computed 部分）

```javascript
// 判断是否是财务角色
const isFinanceRole = computed(() => {
  return currentUser.value?.roles?.some((role) => role.name === 'ROLE_FINANCE' || role.name === 'FINANCE')
})

// 判断是否是项目经理角色
const isManagerRole = computed(() => {
  return currentUser.value?.roles?.some((role) => role.name === 'ROLE_MANAGER' || role.name === 'MANAGER')
})
```

### 2. 在报销列表添加待审批 Tab

```vue
<el-tab-pane label="待审批" name="pending" v-if="isManagerRole || isFinanceRole">
  <div class="tab-content">
    <!-- 待审批列表 -->
  </div>
</el-tab-pane>
```

### 3. 添加财务审批 API 调用

```javascript
// 财务审批通过
const approveByFinance = async (reimbursementId) => {
  try {
    await api.post(`/api/oa/reimbursement/${reimbursementId}/finance-approve`)
    ElMessage.success('财务审批通过')
    fetchReimbursementList()
  } catch (error) {
    ElMessage.error('财务审批失败: ' + error.message)
  }
}

// 财务审批拒绝
const rejectByFinance = async (reimbursementId) => {
  try {
    await api.post(`/api/oa/reimbursement/${reimbursementId}/finance-reject`)
    ElMessage.success('财务审批已拒绝')
    fetchReimbursementList()
  } catch (error) {
    ElMessage.error('财务审批拒绝失败: ' + error.message)
  }
}
```

## 总结

整体来看，`Reimbursement.vue` 的功能实现较为完整，主要缺失的是：

1. 财务角色的审批入口和权限判断
2. 在报销页面直接展示待审批项目的功能

建议按照上述修改建议进行代码完善，以满足完整的报销审批流程需求。
