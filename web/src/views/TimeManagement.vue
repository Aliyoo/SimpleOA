<template>
  <div class="time-management-container">
    <h1>我的工时</h1>

    <!-- 自定义 Tab 导航 -->
    <div class="custom-tabs">
      <div class="custom-tabs-header">
        <div
          v-for="tab in tabs"
          :key="tab.name"
          :class="['custom-tab', { 'active': activeTab === tab.name }]"
          @click="activeTab = tab.name"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- Tab 内容区域 -->
      <div class="custom-tabs-content">
        <!-- 工时填报 Tab -->
        <div v-show="activeTab === 'report'" class="tab-pane">
          <el-form :model="timeReportForm" label-width="100px" class="report-form">
            <el-form-item label="项目名称" prop="project">
              <el-select v-model="timeReportForm.project.id" placeholder="请选择项目">
                <el-option
                  v-for="project in projectList"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                />
              </el-select>
              <div class="form-item-hint" v-if="projectList.length === 0">
                <el-alert
                  title="您没有参与任何项目，请联系管理员添加您到项目中"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </div>
            </el-form-item>

          <el-form-item label="工作日期" prop="date">
            <el-date-picker
              v-model="timeReportForm.date"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-form-item label="工时(小时)" prop="hours">
            <el-input-number v-model="timeReportForm.hours" :min="0.5" :max="24" :step="0.5" />
          </el-form-item>

          <el-form-item label="工作类型" prop="workType">
            <el-select v-model="timeReportForm.workType" placeholder="请选择工作类型">
              <el-option label="开发" value="开发" />
              <el-option label="测试" value="测试" />
              <el-option label="设计" value="设计" />
              <el-option label="文档" value="文档" />
              <el-option label="会议" value="会议" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>

          <el-form-item label="工作内容" prop="description">
            <el-input
              v-model="timeReportForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入工作内容"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitTimeReport">提交</el-button>
          </el-form-item>
        </el-form>
        </div>

        <!-- 审批列表 Tab -->
        <div v-show="activeTab === 'approval'" class="tab-pane">
          <div class="approval-container">
          <!-- 搜索和筛选区域 -->
          <div class="search-bar">
            <el-form :inline="true" :model="approvalSearchForm" class="search-form">
              <el-form-item label="项目">
                <el-select v-model="approvalSearchForm.projectId" placeholder="选择项目" clearable style="width: 200px;">
                  <el-option
                    v-for="project in projectList"
                    :key="project.id"
                    :label="project.name"
                    :value="project.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="日期范围">
                <el-date-picker
                  v-model="approvalSearchForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  clearable
                  style="width: 300px;"
                />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="approvalSearchForm.status" placeholder="选择状态" clearable style="width: 150px;">
                  <el-option label="待审批" value="pending" />
                  <el-option label="已通过" value="approved" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchApprovalList">查询</el-button>
                <el-button @click="resetApprovalSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 查询结果提示 -->
          <div class="query-result-info" v-if="!approvalLoading && approvalList.length > 0">
            当前显示 <span class="highlight">{{ approvalList.length }}</span> 条记录，
            时间范围： <span class="highlight">{{ approvalSearchForm.dateRange[0] || '-' }}</span> 至 <span class="highlight">{{ approvalSearchForm.dateRange[1] || '-' }}</span>
          </div>

          <!-- 工时记录列表 -->
          <el-table
            :data="approvalList"
            style="width: 100%"
            v-loading="approvalLoading"
            :row-key="row => row.id"
          >
            <el-table-column label="项目" width="180">
              <template #default="scope">
                {{ scope.row.project?.name || '未知项目' }}
              </template>
            </el-table-column>
            <el-table-column prop="date" label="工作日期" width="120">
              <template #default="scope">
                <span>{{ scope.row.date }}</span>
                <el-tooltip content="工作日期" placement="top">
                  <el-icon class="ml-1"><Calendar /></el-icon>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="hours" label="工时" width="100" />
            <el-table-column prop="workType" label="工作类型" width="100" />
            <el-table-column prop="description" label="工作内容" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.approved)">
                  {{ scope.row.approved ? '已通过' : '待审批' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="scope">
                <el-button
                  size="small"
                  type="success"
                  @click="approveTimeReport(scope.row)"
                  v-if="!scope.row.approved && isApprover"
                >
                  通过
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  @click="rejectTimeReport(scope.row)"
                  v-if="!scope.row.approved && isApprover"
                >
                  驳回
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  @click="deleteTimeReport(scope.row)"
                  v-if="!scope.row.approved"
                >
                  删除
                </el-button>
                <el-button
                  size="small"
                  type="info"
                  @click="viewTimeReportDetail(scope.row)"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页控件 -->
          <div class="pagination-container">
            <el-pagination
              :current-page="approvalPagination.currentPage"
              :page-size="approvalPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="approvalPagination.total"
              @size-change="handleApprovalSizeChange"
              @current-change="handleApprovalCurrentChange"
              @update:current-page="val => approvalPagination.currentPage = val"
              @update:page-size="val => approvalPagination.pageSize = val"
            />
          </div>

          <!-- 详情对话框 -->
          <el-dialog v-model="timeReportDetailVisible" title="工时记录详情" width="50%">
            <div v-if="selectedTimeReport">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="项目">{{ selectedTimeReport.project?.name || '未知项目' }}</el-descriptions-item>
                <el-descriptions-item label="工作日期">{{ selectedTimeReport.date }}</el-descriptions-item>
                <el-descriptions-item label="工时">{{ selectedTimeReport.hours }}</el-descriptions-item>
                <el-descriptions-item label="工作类型">{{ selectedTimeReport.workType }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusTagType(selectedTimeReport.approved)">
                    {{ selectedTimeReport.approved ? '已通过' : '待审批' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="工作内容" :span="2">{{ selectedTimeReport.description }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="timeReportDetailVisible = false">关闭</el-button>
                <el-button type="success" @click="approveTimeReport(selectedTimeReport)" v-if="selectedTimeReport && !selectedTimeReport.approved && isApprover">通过</el-button>
                <el-button type="warning" @click="rejectTimeReport(selectedTimeReport)" v-if="selectedTimeReport && !selectedTimeReport.approved && isApprover">驳回</el-button>
                <el-button type="danger" @click="deleteTimeReport(selectedTimeReport)" v-if="selectedTimeReport && !selectedTimeReport.approved">删除</el-button>
              </span>
            </template>
          </el-dialog>
        </div>
        </div>

        <!-- 批量填写 Tab -->
        <div v-show="activeTab === 'batch'" class="tab-pane">
          <div class="batch-container">
            <!-- 工时填写说明 -->
            <el-alert
              title="工时填写说明"
              type="info"
              :closable="false"
              style="margin-bottom: 20px;"
            >
              <template #default>
                <div>
                  <p>• 默认时间范围：上月25日至本月24日</p>
                  <p>• 工时数必须为1-8之间的整数</p>
                  <p>• 前三列（序号、项目名称、项目经理）为冻结列，始终可见</p>
                  <p>• 可在对应项目和日期的单元格中直接填写工时数</p>
                </div>
              </template>
            </el-alert>

            <div class="batch-header">
              <div class="date-range-container">
                <el-date-picker
                  v-model="batchDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :default-value="getDefaultWorkTimeRange()"
                  @change="onBatchDateRangeChange"
                  style="width: 300px;"
                />
                <el-button @click="setDefaultWorkTimeRange" type="info" plain size="small">
                  恢复默认范围
                </el-button>
              </div>
              <div class="batch-actions">
                <el-button type="primary" @click="submitBatchTimeRecords" :disabled="!hasValidTimeData">
                  批量提交工时
                </el-button>
                <el-button @click="clearAllTimeData" type="warning" plain>
                  清空所有数据
                </el-button>
              </div>
            </div>

            <!-- 工时统计显示 -->
            <div class="batch-summary" v-if="batchProjects.length > 0">
              <el-card shadow="hover" style="margin-bottom: 20px;">
                <template #header>
                  <span>工时统计</span>
                </template>
                <div class="summary-stats">
                  <div class="stat-item">
                    <span class="stat-label">总工时：</span>
                    <span class="stat-value">{{ totalBatchHours }} 小时</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">已填写天数：</span>
                    <span class="stat-value">{{ filledDaysCount }} 天</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">平均每日工时：</span>
                    <span class="stat-value">{{ averageDailyHours }} 小时</span>
                  </div>
                </div>
              </el-card>
            </div>

            <div class="batch-table-container" v-loading="batchLoading">
              <el-table
                :data="batchProjects"
                style="width: 100%"
                border
                :max-height="500"
                :cell-class-name="getCellClass"
                stripe
                highlight-current-row
              >
                <!-- 固定列 -->
                <el-table-column type="index" label="序号" width="60" fixed="left" align="center" />
                <el-table-column prop="name" label="项目名称" width="200" fixed="left" show-overflow-tooltip>
                  <template #default="scope">
                    <div class="project-info">
                      <el-tag size="small" :type="getProjectStatusTagType(scope.row.status)">
                        {{ getProjectStatusText(scope.row.status) }}
                      </el-tag>
                      <div class="project-name">{{ scope.row.name }}</div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="managerName" label="项目经理" width="120" fixed="left" align="center" />

                <!-- 动态日期列 -->
                <el-table-column
                  v-for="date in batchDates"
                  :key="date"
                  :label="formatDateLabel(date)"
                  :prop="date"
                  width="80"
                  align="center"
                  :class-name="getDateColumnClass(date)"
                >
                  <template #header>
                    <div class="date-header">
                      <div class="date-text">{{ formatDateLabel(date) }}</div>
                      <div class="weekday-text">{{ getWeekday(date) }}</div>
                    </div>
                  </template>
                  <template #default="scope">
                    <el-input-number
                      v-model="scope.row.hours[date]"
                      :min="1"
                      :max="8"
                      :step="1"
                      :precision="0"
                      :controls="false"
                      size="small"
                      style="width: 60px"
                      @change="validateHours(scope.row.hours[date], scope.row, date)"
                      :class="getInputClass(scope.row.hours[date])"
                      placeholder="0"
                    />
                  </template>
                </el-table-column>

                <!-- 合计列 -->
                <el-table-column label="合计" width="80" fixed="right" align="center">
                  <template #default="scope">
                    <el-tag :type="getTotalHoursTagType(calculateTotalHours(scope.row.hours))">
                      {{ calculateTotalHours(scope.row.hours) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>

              <div class="batch-empty" v-if="batchProjects.length === 0 && !batchLoading">
                <el-empty description="暂无项目数据，请联系管理员添加您到项目中" />
              </div>
            </div>

            <!-- 快捷操作按钮 -->
            <div class="quick-actions" v-if="batchProjects.length > 0" style="margin-top: 20px;">
              <el-card shadow="hover">
                <template #header>
                  <span>快捷操作</span>
                </template>
                <div class="action-buttons">
                  <el-button @click="fillWorkdaysWithEight" type="success" plain size="small">
                    工作日填8小时
                  </el-button>
                  <el-button @click="fillAllDaysWithValue(8)" type="primary" plain size="small">
                    全部填8小时
                  </el-button>
                  <el-button @click="fillAllDaysWithValue(4)" type="warning" plain size="small">
                    全部填4小时
                  </el-button>
                  <el-button @click="clearWeekends" type="info" plain size="small">
                    清空周末
                  </el-button>
                </div>
              </el-card>
            </div>
          </div>
        </div>

        <!-- 统计报表 Tab -->
        <div v-show="activeTab === 'statistics'" class="tab-pane">
          <div class="statistics-container">
          <div class="statistics-header">
            <div class="statistics-filters">
              <el-date-picker
                v-model="statisticsDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :default-value="getDefaultDateRangeDates()"
                @change="onDateRangeChange"
                style="width: 300px;"
              />
              <el-select
                v-model="selectedProjectId"
                placeholder="选择项目"
                clearable
                style="width: 200px; margin-left: 10px;"
              >
                <el-option
                  key="all"
                  label="所有项目"
                  value=""
                />
                <el-option
                  v-for="project in projectList"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                />
              </el-select>
            </div>
            <el-button type="primary" @click="fetchStatisticsData" :disabled="!statisticsDateRange || statisticsDateRange.length !== 2">刷新数据</el-button>
          </div>

          <div class="statistics-summary">
            <el-card shadow="hover">
              <template #header>
                <div class="clearfix">
                  <span>工时汇总</span>
                </div>
              </template>
              <div class="summary-content">
                <div class="summary-item">
                  <span class="summary-label">总工时：</span>
                  <span class="summary-value">{{ summaryData.totalHours }} 小时</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">加班时长：</span>
                  <span class="summary-value">{{ summaryData.overtimeHours }} 小时</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">工作负荷：</span>
                  <span class="summary-value">{{ summaryData.workload }}%</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">记录数量：</span>
                  <span class="summary-value">{{ summaryData.recordCount }} 条</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">平均工时：</span>
                  <span class="summary-value">{{ summaryData.averageHoursPerRecord }} 小时/条</span>
                </div>
              </div>
            </el-card>
          </div>

          <!-- 项目统计表格 -->
          <div class="statistics-section">
            <h3 class="section-title">项目工时统计</h3>
            <el-table :data="statisticsData" style="width: 100%;" :row-class-name="tableRowClassName">
              <el-table-column prop="projectName" label="项目" />
              <el-table-column prop="totalHours" label="总工时" sortable>
                <template #default="scope">
                  {{ scope.row.totalHours.toFixed(2) }} 小时
                </template>
              </el-table-column>
              <el-table-column prop="percentage" label="占比" sortable />
            </el-table>
          </div>

          <!-- 图表展示 -->
          <div class="statistics-section">
            <h3 class="section-title">工时分布图表</h3>
            <div class="chart-container">
              <el-empty description="暂无数据" v-if="!statisticsData.length" />
              <div v-else>
                <div id="timeStatisticsChart" style="width: 100%; height: 400px"></div>
              </div>
            </div>
          </div>

          <!-- 详细工时报表 -->
          <div class="statistics-section">
            <h3 class="section-title">详细工时报表</h3>
            <div class="report-tools">
              <el-button type="success" @click="exportWorkTimeReport" :loading="exportLoading">
                <el-icon><Download /></el-icon>
                导出Excel报表
              </el-button>
              <el-button type="primary" @click="generateWeeklyReport" :loading="reportLoading">
                生成周报
              </el-button>
              <el-button type="warning" @click="generateMonthlyReport" :loading="reportLoading">
                生成月报
              </el-button>
            </div>
            
            <!-- 工时明细表 -->
            <el-table :data="detailReportData" style="width: 100%; margin-top: 20px;" stripe border>
              <el-table-column prop="date" label="日期" width="120" sortable />
              <el-table-column prop="projectName" label="项目" width="200" />
              <el-table-column prop="workType" label="工作类型" width="100" />
              <el-table-column prop="hours" label="工时" width="80" align="center">
                <template #default="scope">
                  {{ scope.row.hours }} 小时
                </template>
              </el-table-column>
              <el-table-column prop="description" label="工作内容" show-overflow-tooltip />
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusTagType(scope.row.approved)">
                    {{ scope.row.approved ? '已审核' : '待审核' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
            
            <!-- 分页 -->
            <div class="pagination-container">
              <el-pagination
                :current-page="reportPagination.currentPage"
                :page-size="reportPagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="reportPagination.total"
                @size-change="handleReportSizeChange"
                @current-change="handleReportCurrentChange"
              />
            </div>
          </div>

          <!-- 加班统计 -->
          <div class="statistics-section">
            <h3 class="section-title">加班统计分析</h3>
            <el-card shadow="hover">
              <div class="overtime-stats">
                <div class="overtime-summary">
                  <div class="summary-item">
                    <span class="label">总加班时长：</span>
                    <span class="value">{{ overtimeStats.totalOvertimeHours || 0 }} 小时</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">加班天数：</span>
                    <span class="value">{{ overtimeStats.overtimeDays || 0 }} 天</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">平均每日加班：</span>
                    <span class="value">{{ overtimeStats.averageOvertimePerDay || 0 }} 小时</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">最长单日工作：</span>
                    <span class="value">{{ overtimeStats.maxDailyHours || 0 }} 小时</span>
                  </div>
                </div>
                
                <!-- 加班趋势图 -->
                <div class="overtime-chart">
                  <div id="overtimeChart" style="width: 100%; height: 300px;"></div>
                </div>
              </div>
            </el-card>
          </div>
        </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 批量填写相关样式 */
.batch-container {
  padding: 20px;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.date-range-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.batch-actions {
  display: flex;
  gap: 10px;
}

.batch-summary .summary-stats {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

.summary-stats .stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat-label {
  color: #666;
  font-weight: normal;
}

.stat-value {
  color: #409EFF;
  font-weight: bold;
  font-size: 16px;
}

/* 项目信息样式 */
.project-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.project-name {
  font-weight: 500;
}

/* 日期列头样式 */
.date-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.date-text {
  font-weight: 500;
  font-size: 12px;
}

.weekday-text {
  font-size: 10px;
  color: #666;
}

/* 周末列样式 */
:deep(.weekend-column) {
  background-color: #f5f7fa !important;
}

:deep(.weekend-column .cell) {
  background-color: #f5f7fa;
}

/* 周末单元格样式 */
:deep(.weekend-cell) {
  background-color: #f5f7fa !important;
}

/* 输入框样式 */
:deep(.valid-hours .el-input__inner) {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

:deep(.invalid-hours .el-input__inner) {
  border-color: #f56c6c;
  background-color: #fef0f0;
}

/* 快捷操作样式 */
.quick-actions .action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* 表格行样式 */
:deep(.el-table .el-table__row:hover > td) {
  background-color: #f5f7fa;
}

/* 冻结列样式优化 */
:deep(.el-table__fixed-left) {
  box-shadow: 2px 0 6px rgba(0, 0, 0, 0.1);
}

:deep(.el-table__fixed-right) {
  box-shadow: -2px 0 6px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .batch-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .date-range-container {
    justify-content: center;
  }
  
  .batch-actions {
    justify-content: center;
  }
  
  .summary-stats {
    justify-content: center;
  }
}

/* 通用样式 */
.time-management-container {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.custom-tabs {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.custom-tabs-header {
  display: flex;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

.custom-tab {
  padding: 16px 24px;
  cursor: pointer;
  color: #666;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
  position: relative;
}

.custom-tab:hover {
  color: #409EFF;
  background: rgba(64, 158, 255, 0.05);
}

.custom-tab.active {
  color: #409EFF;
  background: white;
  border-bottom-color: #409EFF;
  font-weight: 500;
}

.custom-tabs-content {
  padding: 24px;
}

.tab-pane {
  min-height: 500px;
}

/* 表单样式 */
.report-form {
  max-width: 800px;
}

.form-item-hint {
  margin-top: 10px;
}

/* 搜索栏样式 */
.search-bar {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.search-form {
  margin: 0;
}

/* 查询结果信息 */
.query-result-info {
  margin-bottom: 15px;
  padding: 10px 15px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
  color: #0050b3;
}

.highlight {
  font-weight: bold;
  color: #1890ff;
}

/* 分页样式 */
.pagination-container {
  margin-top: 20px;
  text-align: right;
}

/* 统计样式 */
.statistics-container {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.statistics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.statistics-filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.statistics-summary {
  margin-bottom: 24px;
}

.summary-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.summary-label {
  color: #666;
  font-size: 14px;
}

.summary-value {
  color: #409EFF;
  font-weight: 600;
  font-size: 16px;
}

.statistics-section {
  margin-bottom: 32px;
}

.section-title {
  margin-bottom: 16px;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  border-left: 4px solid #409EFF;
  padding-left: 12px;
}

.chart-container {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 表格行样式 */
:deep(.el-table .success-row) {
  background: #f0f9ff;
}

:deep(.el-table .warning-row) {
  background: #fdf6ec;
}

/* 状态标签 */
.el-tag {
  margin-right: 8px;
}

/* 报表工具栏样式 */
.report-tools {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

/* 加班统计样式 */
.overtime-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.overtime-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.overtime-summary .summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
}

.overtime-summary .label {
  color: #666;
  font-weight: 500;
}

.overtime-summary .value {
  color: #409EFF;
  font-weight: bold;
  font-size: 18px;
}

.overtime-chart {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 响应式设计 - 移动端 */
@media (max-width: 768px) {
  .report-tools {
    flex-direction: column;
  }
  
  .overtime-summary {
    grid-template-columns: 1fr;
  }
  
  .overtime-summary .summary-item {
    flex-direction: column;
    text-align: center;
    gap: 10px;
  }
}
</style>

<script setup>
import { APP_CONFIG } from '../utils/config.js'
import { ref, reactive, onMounted, watch, computed } from 'vue'
import api from '../utils/axios.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { useUserStore } from '../stores/user'
import { Calendar, Download } from '@element-plus/icons-vue'

// 定义 tabs 数组，用于自定义 tabs 导航
const tabs = [
  { name: 'report', label: '工时填报' },
  { name: 'batch', label: '批量填写' },
  { name: 'approval', label: '审批列表' },
  { name: 'statistics', label: '统计报表' }
]

// 添加日志以检查默认日期范围是否正确设置
console.log('默认日期范围(字符串):', APP_CONFIG.DEFAULT_DATE_RANGE.getRange());
console.log('默认日期范围(Date对象):', APP_CONFIG.DEFAULT_DATE_RANGE.getRangeDates());
const statisticsDateRange = ref([])
console.log('statisticsDateRange initial value:', statisticsDateRange.value);
const batchDateRange = ref([])
const activeTab = ref('report')
const userStore = useUserStore()
const currentUser = ref(userStore.user)

const summaryData = ref({
  totalHours: '0.00',
  overtimeHours: '0.00',
  workload: '0',
  recordCount: 0,
  averageHoursPerRecord: '0.00'
});

const timeReportForm = reactive({
  project: { id: '' },
  user: { id: '' },
  date: '',
  hours: 8,
  workType: '开发',
  description: '',
  approved: false
})

const projectList = ref([])
const approvalList = ref([])
const statisticsData = ref([])
const selectedProjectId = ref('') // 默认为空字符串，表示所有项目
const isApprover = ref(false)

// 报表相关数据
const detailReportData = ref([])
const overtimeStats = ref({
  totalOvertimeHours: 0,
  overtimeDays: 0,
  averageOvertimePerDay: 0,
  maxDailyHours: 0
})

// 加载状态
const exportLoading = ref(false)
const reportLoading = ref(false)

// 报表分页
const reportPagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 批量填写相关数据
const batchDates = ref([])
const batchProjects = ref([])
const batchLoading = ref(false)

// 计算属性：总工时
const totalBatchHours = computed(() => {
  return batchProjects.value.reduce((total, project) => {
    return total + calculateTotalHours(project.hours || {});
  }, 0);
});

// 计算属性：已填写天数
const filledDaysCount = computed(() => {
  const filledDays = new Set();
  batchProjects.value.forEach(project => {
    Object.entries(project.hours || {}).forEach(([date, hours]) => {
      if (hours && hours > 0) {
        filledDays.add(date);
      }
    });
  });
  return filledDays.size;
});

// 计算属性：平均每日工时
const averageDailyHours = computed(() => {
  if (filledDaysCount.value === 0) return '0.00';
  return (totalBatchHours.value / filledDaysCount.value).toFixed(2);
});

// 计算属性：是否有有效的工时数据
const hasValidTimeData = computed(() => {
  return batchProjects.value.some(project => 
    Object.values(project.hours || {}).some(hours => hours && hours > 0)
  );
});

// 审批列表相关数据
const approvalLoading = ref(false)
const approvalSearchForm = reactive({
  projectId: '',
  // 设置默认日期范围
  dateRange: [],
  status: ''
})

// 初始化函数
const initApprovalSearchForm = () => {
  approvalSearchForm.dateRange = getDefaultDateRange()
}
const approvalPagination = reactive({
  currentPage: 1,
  pageSize: 20, // 增加默认分页大小，显示更多记录
  total: 0
})
const timeReportDetailVisible = ref(false)
const selectedTimeReport = ref(null)

const fetchProjects = async () => {
  if (!currentUser.value || !currentUser.value.id) {
    // 如果没有用户信息，尝试从缓存中获取
    if (userStore.user && userStore.user.id) {
      // 用户信息存在于缓存中
      currentUser.value = userStore.user
    } else {
          ElMessage.warning('未获取到当前用户信息')
          return
        }
      }

      try {
        // 获取当前用户参与的项目列表
        const response = await api.get(`/api/projects/user/${currentUser.value.id}`)
        projectList.value = response.data
        console.log('获取到用户参与的项目列表:', projectList.value)

        // 在项目列表加载完成后初始化批量填写表格
        if (activeTab.value === 'batch') {
          initBatchDateRange()
        }
      } catch (error) {
        console.error('获取项目列表失败:', error)
        ElMessage.error('获取项目列表失败: ' + error.message)
      }
    }

    const fetchApprovalList = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          ElMessage.warning('未获取到当前用户信息')
          return
        }
      }

      approvalLoading.value = true

      try {
        // 构建查询参数
        const params = {
          _t: new Date().getTime() // 添加时间戳参数，避免浏览器缓存
        }

        // 如果有项目筛选
        if (approvalSearchForm.projectId) {
          params.projectId = approvalSearchForm.projectId
        }

        // 如果有日期范围筛选
        if (approvalSearchForm.dateRange && approvalSearchForm.dateRange.length === 2) {
          params.startDate = approvalSearchForm.dateRange[0]
          params.endDate = approvalSearchForm.dateRange[1]
        } else {
          // 默认日期范围：当前年度的所有日期
          const now = new Date()
          const year = now.getFullYear()

          // 当前年的第一天
          params.startDate = `${year}-01-01`

          // 当前年的最后一天
          params.endDate = `${year}-12-31`
        }

        // 如果有状态筛选
        if (approvalSearchForm.status) {
          params.approved = approvalSearchForm.status === 'approved'
        }

        // 添加分页参数
        params.page = approvalPagination.currentPage - 1 // 后端从0开始计数
        params.size = approvalPagination.pageSize

        console.log('获取工时记录，参数:', params)

        // 调用API获取工时记录
        const response = await api.get(`/api/worktime/user/${currentUser.value.id}/range`, { params })

        // 处理返回的数据
        if (Array.isArray(response.data)) {
          // 如果返回的是数组，则直接使用
          approvalList.value = response.data
          approvalPagination.total = response.data.length // 注意：这里只是临时处理，实际应该由后端返回总数
        } else if (response.data && response.data.content) {
          // 如果返回的是分页对象，则提取content属性
          approvalList.value = response.data.content
          approvalPagination.total = response.data.totalElements || 0
        } else {
          approvalList.value = []
          approvalPagination.total = 0
        }

        // 按日期降序排序
        approvalList.value.sort((a, b) => new Date(b.date) - new Date(a.date))

        console.log('获取到工时记录:', approvalList.value.length, '条')
      } catch (error) {
        console.error('获取工时记录失败:', error)
        ElMessage.error('获取工时记录失败: ' + error.message)
        approvalList.value = []
      } finally {
        approvalLoading.value = false
      }
    }

    // 搜索审批列表
    const searchApprovalList = () => {
      approvalPagination.currentPage = 1 // 重置到第一页
      fetchApprovalList()
    }

    // 重置搜索条件
    const resetApprovalSearch = () => {
      approvalSearchForm.projectId = ''
      approvalSearchForm.dateRange = []
      approvalSearchForm.status = ''
      searchApprovalList()
    }

    // 分页大小变化处理
    const handleApprovalSizeChange = (size) => {
      approvalPagination.pageSize = size
      fetchApprovalList()
    }

    // 分页页码变化处理
    const handleApprovalCurrentChange = (page) => {
      approvalPagination.currentPage = page
      fetchApprovalList()
    }

    // 查看工时记录详情
    const viewTimeReportDetail = (record) => {
      selectedTimeReport.value = record
      timeReportDetailVisible.value = true
    }

    const rejectTimeReport = async (record) => {
      try {
        const reason = await ElMessageBox.prompt('请输入驳回原因', '驳回工时', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入驳回原因'
        })

        await api.put(`/api/worktime/reject/${record.id}`, { reason: reason.value })
        ElMessage.success('工时已驳回')
        record.approved = false
        fetchApprovalList() // 刷新列表
      } catch (error) {
        if (error === 'cancel') {
          return
        }
        console.error('驳回工时失败:', error)
        ElMessage.error('驳回工时失败: ' + error.message)
      }
    }

    const fetchStatisticsData = async () => {
      console.log('fetchStatisticsData 被调用，日期范围:', statisticsDateRange.value);

      if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2 || !currentUser.value || !currentUser.value.id) {
        console.log('条件检查未通过:', {
          hasDateRange: !!statisticsDateRange.value,
          dateRangeLength: statisticsDateRange.value ? statisticsDateRange.value.length : 0,
          hasCurrentUser: !!currentUser.value,
          hasUserId: currentUser.value ? !!currentUser.value.id : false
        });

        if (!currentUser.value || !currentUser.value.id) {
          console.log('尝试获取用户信息');
          if (userStore.user && userStore.user.id) {
            console.log('从 userStore 获取到用户信息');
            currentUser.value = userStore.user
          } else if (userStore.token) {
            console.log('尝试从服务器获取用户信息');
            await userStore.fetchUser()
            if (userStore.user && userStore.user.id) {
              console.log('从服务器获取到用户信息');
              currentUser.value = userStore.user
            } else {
              console.log('无法获取用户信息');
              ElMessage.warning('未获取到当前用户信息')
              return
            }
          } else {
            console.log('无法获取用户信息，没有 token');
            ElMessage.warning('未获取到当前用户信息')
            return
          }
        }

        if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
          console.log('日期范围无效，退出函数');
          return
        }
      }

      try {
        console.log('开始处理日期范围并获取统计数据');

        // 确保日期格式正确
        let startDate, endDate;

        // 检查日期格式是否为 YYYY-MM-DD
        if (statisticsDateRange.value[0].includes('-') && statisticsDateRange.value[0].split('-').length === 3) {
          // 已经是 YYYY-MM-DD 格式
          startDate = statisticsDateRange.value[0];
        } else if (statisticsDateRange.value[0].includes('-') && statisticsDateRange.value[0].split('-').length === 2) {
          // 是 YYYY-MM 格式，转换为 YYYY-MM-01
          startDate = `${statisticsDateRange.value[0]}-01`;
        } else {
          // 其他格式，使用当前日期
          const now = new Date();
          startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
        }

        // 检查结束日期格式
        if (statisticsDateRange.value[1].includes('-') && statisticsDateRange.value[1].split('-').length === 3) {
          // 已经是 YYYY-MM-DD 格式
          endDate = statisticsDateRange.value[1];
        } else if (statisticsDateRange.value[1].includes('-') && statisticsDateRange.value[1].split('-').length === 2) {
          // 是 YYYY-MM 格式，计算月末
          const [endYear, endMonthStr] = statisticsDateRange.value[1].split('-');
          const endMonthNum = parseInt(endMonthStr);
          // 使用下个月的第0天来获取当前月的最后一天
          const lastDay = new Date(parseInt(endYear), endMonthNum, 0).getDate();
          endDate = `${statisticsDateRange.value[1]}-${String(lastDay).padStart(2, '0')}`;
        } else {
          // 其他格式，使用当前日期的月末
          const now = new Date();
          const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
          endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
        }

        console.log('处理后的日期范围:', startDate, '至', endDate);

        console.log('获取统计数据，日期范围:', startDate, '至', endDate);
        console.log('当前用户ID:', currentUser.value.id);
        console.log('选择的项目ID:', selectedProjectId.value);

        let apiUrl;
        let params = { startDate, endDate };

        // 根据是否选择了特定项目决定使用哪个API
        if (selectedProjectId.value) {
          // 获取特定项目的统计数据
          apiUrl = `/api/worktime/project/${selectedProjectId.value}/stats`;
          console.log('使用项目统计API:', apiUrl);
        } else {
          // 如果没有选择特定项目，则获取所有项目的统计数据或用户的统计数据
          if (currentUser.value && currentUser.value.id) {
            // 获取当前用户的统计数据
            apiUrl = `/api/worktime/user/${currentUser.value.id}/stats`;
            console.log('使用用户统计API:', apiUrl);
          } else {
            // 获取所有项目的统计数据
            apiUrl = `/api/worktime/projects/stats`;
            console.log('使用所有项目统计API:', apiUrl);
          }
        }

        console.log('最终API请求URL:', apiUrl);
        const response = await api.get(apiUrl, { params })

        console.log('API响应数据:', response.data);

        // 检查响应数据结构
        let responseData;
        if (response.data && response.data.code === 200 && response.data.data) {
          // 新的嵌套结构 {code, message, data}
          responseData = response.data.data;
          console.log('使用嵌套数据结构:', responseData);
        } else {
          // 旧的直接结构
          responseData = response.data;
          console.log('使用直接数据结构:', responseData);
        }

        // 从响应中获取数据
        const totalHours = responseData.totalHours || 0;
        const overtimeHours = responseData.overtimeHours || 0;
        const workload = responseData.workload || 0;
        const recordCount = responseData.recordCount || 0;
        const averageHoursPerRecord = responseData.averageHoursPerRecord || 0;
        const projectHours = responseData.projects || {};

        console.log('项目工时数据:', projectHours);
        console.log('统计数据详情:', {
          totalHours,
          overtimeHours,
          workload,
          recordCount,
          averageHoursPerRecord
        });

        // 处理项目工时数据用于表格显示
        if (selectedProjectId.value) {
          // 如果选择了特定项目，只显示该项目的数据
          const project = projectList.value.find(p => p.id === parseInt(selectedProjectId.value)) || { name: '未知项目' };
          statisticsData.value = [{
            projectName: project.name,
            totalHours: totalHours,
            percentage: '100%'
          }];
        } else {
          // 如果没有选择特定项目，显示所有项目的数据
          statisticsData.value = Object.entries(projectHours).map(([projectId, hours]) => {
            const project = projectList.value.find(p => p.id === parseInt(projectId)) || { name: '未知项目' };
            return {
              projectName: project.name,
              totalHours: hours,
              percentage: totalHours > 0 ? ((hours / totalHours) * 100).toFixed(2) + '%' : '0%'
            };
          });

          // 按工时降序排序
          statisticsData.value.sort((a, b) => b.totalHours - a.totalHours);
        }

        // 更新汇总数据
        summaryData.value = {
          totalHours: totalHours.toFixed(2),
          overtimeHours: overtimeHours.toFixed(2),
          workload: (workload * 100).toFixed(2), // 转换为百分比
          recordCount: recordCount,
          averageHoursPerRecord: averageHoursPerRecord.toFixed(2)
        };

        console.log('获取到统计数据:', statisticsData.value);

        // 初始化图表
        if (statisticsData.value.length > 0) {
          console.log('准备初始化图表，数据条数:', statisticsData.value.length);
          setTimeout(() => {
            initStatisticsChart()
          }, 100)
        } else {
          console.log('没有统计数据，不初始化图表');
        }

        // 获取详细报表数据
        await fetchDetailReportData();
        
        // 计算加班统计
        calculateOvertimeStats();
      } catch (error) {
        console.error('获取统计数据失败:', error)
        ElMessage.error('获取统计数据失败: ' + error.message)
        statisticsData.value = []
        summaryData.value = {
          totalHours: '0.00',
          overtimeHours: '0.00',
          workload: '0',
          recordCount: 0,
          averageHoursPerRecord: '0.00'
        };
      }
    }

    const initStatisticsChart = () => {
      try {
        console.log('开始初始化图表');
        const chartDom = document.getElementById('timeStatisticsChart')
        if (!chartDom) {
          console.error('找不到图表DOM元素');
          return;
        }

        console.log('准备图表数据');
        // 确保统计数据有效
        if (!statisticsData.value || statisticsData.value.length === 0) {
          console.warn('没有有效的统计数据用于图表');
          return;
        }

        // 准备图表数据
        const chartData = statisticsData.value.map(item => ({
          value: item.totalHours,
          name: item.projectName
        }));
        console.log('图表数据:', chartData);

        // 初始化图表
        console.log('初始化ECharts实例');
        const myChart = echarts.init(chartDom)

        const option = {
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} 小时 ({d}%)'
          },
          legend: {
            top: '5%',
            left: 'center'
          },
          series: [
            {
              name: '工时分布',
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: false,
                position: 'center'
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 20,
                  fontWeight: 'bold'
                }
              },
              labelLine: {
                show: false
              },
              data: chartData
            }
          ]
        }

        console.log('设置图表选项');
        myChart.setOption(option)
        console.log('图表初始化完成');
      } catch (error) {
        console.error('初始化图表失败:', error);
      }
    }

    const submitTimeReport = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          ElMessage.warning('未获取到当前用户信息')
          return
        }
      }

      timeReportForm.user.id = currentUser.value.id

      try {
        await api.post('/api/worktime', timeReportForm)
        ElMessage.success('工时提交成功')
        // 重置表单
        timeReportForm.project.id = ''
        timeReportForm.date = ''
        timeReportForm.hours = 8
        timeReportForm.workType = '开发'
        timeReportForm.description = ''
      } catch (error) {
        console.error('提交工时失败:', error)
        ElMessage.error('提交工时失败: ' + error.message)
      }
    }

    const approveTimeReport = async (record) => {
      try {
        await api.put(`/api/worktime/approve/${record.id}`)
        ElMessage.success('审批通过')
        record.approved = true
        fetchApprovalList() // 刷新列表
      } catch (error) {
        console.error('审批工时失败:', error)
        ElMessage.error('审批工时失败: ' + error.message)
      }
    }

    const deleteTimeReport = async (record) => {
      try {
        await ElMessageBox.confirm('确定要删除这条工时记录吗？', '确认删除', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await api.delete(`/api/worktime/${record.id}`)
        ElMessage.success('删除成功')
        fetchApprovalList() // 刷新列表
      } catch (error) {
        if (error === 'cancel') {
          return
        }
        console.error('删除工时失败:', error)
        ElMessage.error('删除工时失败: ' + error.message)
      }
    }

    // 从全局配置获取默认日期范围（字符串格式）
    const getDefaultDateRange = () => {
      try {
        return APP_CONFIG?.DEFAULT_DATE_RANGE?.getRange() || [];
      } catch (error) {
        console.error('获取默认日期范围失败:', error);
        return [];
      }
    }

    // 从全局配置获取默认日期范围（Date对象格式）
    const getDefaultDateRangeDates = () => {
      try {
        return APP_CONFIG?.DEFAULT_DATE_RANGE?.getRangeDates() || [new Date(), new Date()];
      } catch (error) {
        console.error('获取默认日期范围(Date对象)失败:', error);
        // 返回默认值：当前日期和当前日期
        return [new Date(), new Date()];
      }
    }

    // 获取工时默认日期范围（上月25日至本月24日）
    const getDefaultWorkTimeRange = () => {
      const now = new Date();
      const currentYear = now.getFullYear();
      const currentMonth = now.getMonth() + 1; // getMonth() 返回0-11

      let startDate, endDate;

      if (currentMonth === 1) {
        // 如果是1月，上月是去年12月
        startDate = new Date(currentYear - 1, 11, 25); // 去年12月25日
        endDate = new Date(currentYear, 0, 24); // 今年1月24日
      } else {
        // 其他月份
        startDate = new Date(currentYear, currentMonth - 2, 25); // 上月25日
        endDate = new Date(currentYear, currentMonth - 1, 24); // 本月24日
      }

      return [startDate, endDate];
    }

    // 设置默认工时范围
    const setDefaultWorkTimeRange = () => {
      const defaultRange = getDefaultWorkTimeRange();
      const formatDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      };
      
      batchDateRange.value = [formatDate(defaultRange[0]), formatDate(defaultRange[1])];
      onBatchDateRangeChange(batchDateRange.value);
    }

    const initBatchDateRange = () => {
      console.log('初始化批量日期范围，当前值:', batchDateRange.value);

      // 检查日期范围是否有效
      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        // 使用工时默认范围（上月25日至本月24日）
        setDefaultWorkTimeRange();
        return;
      }

      console.log('使用现有日期范围:', batchDateRange.value);
      // 生成批量表格
      generateBatchTable();

      // 加载已提交的工时数据
      loadSubmittedWorkTime();
    }

    const generateBatchTable = () => {
      console.log('generateBatchTable 被调用，日期范围:', batchDateRange.value);

      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        console.warn('批量填写日期范围无效:', batchDateRange.value);
        ElMessage.warning('请选择日期范围')
        return
      }

      batchLoading.value = true
      console.log('开始生成批量表格，日期范围:', batchDateRange.value[0], '至', batchDateRange.value[1]);

      try {
        // 确保日期格式正确
        const startDate = new Date(batchDateRange.value[0])
        const endDate = new Date(batchDateRange.value[1])

        console.log('解析后的日期对象:', startDate, endDate);

        if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
          console.error('日期格式无效:', batchDateRange.value);
          ElMessage.warning('日期格式无效，请重新选择')
          batchLoading.value = false
          return
        }

        if (startDate > endDate) {
          console.error('开始日期晚于结束日期:', startDate, endDate);
          ElMessage.warning('开始日期不能晚于结束日期')
          batchLoading.value = false
          return
        }

        // 计算日期列表
        const dates = []
        let currentDate = new Date(startDate)
        while (currentDate <= endDate) {
          const year = currentDate.getFullYear()
          const month = String(currentDate.getMonth() + 1).padStart(2, '0')
          const day = String(currentDate.getDate()).padStart(2, '0')
          dates.push(`${year}-${month}-${day}`)
          currentDate.setDate(currentDate.getDate() + 1)
        }
        batchDates.value = dates
        console.log('生成的日期列表:', batchDates.value);

        // 检查项目列表
        if (!projectList.value || projectList.value.length === 0) {
          console.warn('项目列表为空，尝试重新获取');
          fetchProjects().then(() => {
            if (projectList.value && projectList.value.length > 0) {
              console.log('成功获取项目列表，继续生成表格');
              buildBatchProjects(dates);
              // 加载已提交的工时数据
              loadSubmittedWorkTime();
            } else {
              console.error('无法获取项目列表');
              ElMessage.warning('无法获取项目列表，请刷新页面重试');
            }
          }).catch(error => {
            console.error('获取项目列表失败:', error);
            ElMessage.error('获取项目列表失败: ' + error.message);
          }).finally(() => {
            batchLoading.value = false;
          });
        } else {
          // 构建项目数据
          buildBatchProjects(dates);
          batchLoading.value = false;
        }
      } catch (error) {
        console.error('生成批量表格失败:', error)
        ElMessage.error('生成批量表格失败: ' + error.message)
        batchLoading.value = false
      }
    }

    // 加载已提交的工时数据
    const loadSubmittedWorkTime = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          console.warn('未获取到当前用户信息，无法加载已提交工时');
          return
        }
      }

      if (!batchDateRange.value || batchDateRange.value.length !== 2) {
        console.warn('批量填写日期范围无效，无法加载已提交工时');
        return
      }

      try {
        batchLoading.value = true;
        console.log('加载已提交工时，日期范围:', batchDateRange.value[0], '至', batchDateRange.value[1]);

        // 构建查询参数
        const params = {
          startDate: batchDateRange.value[0],
          endDate: batchDateRange.value[1],
          _t: new Date().getTime(), // 添加时间戳参数，避免浏览器缓存
          size: 1000 // 设置较大的分页大小，确保能获取到所有记录
        }

        console.log('查询工时记录参数:', params);

        // 调用API获取工时记录
        const response = await api.get(`/api/worktime/user/${currentUser.value.id}/range`, { params });

        // 处理返回的数据
        let workTimeRecords = [];
        if (Array.isArray(response.data)) {
          // 如果返回的是数组，则直接使用
          workTimeRecords = response.data;
        } else if (response.data && response.data.content) {
          // 如果返回的是分页对象，则提取content属性
          workTimeRecords = response.data.content;
        }

        console.log('获取到工时记录:', workTimeRecords.length, '条');

        // 将已提交的工时数据填充到批量表格中
        if (workTimeRecords.length > 0) {
          // 创建一个映射，用于存储已提交的工时记录
          const submittedHoursMap = {};

          // 处理已提交的工时记录
          workTimeRecords.forEach(record => {
            const projectId = record.project?.id;
            const date = record.date;
            const hours = record.hours;

            if (projectId && date && hours) {
              if (!submittedHoursMap[projectId]) {
                submittedHoursMap[projectId] = {};
              }
              submittedHoursMap[projectId][date] = hours;
            }
          });

          // 填充已提交的工时数据，但不覆盖用户尚未提交的数据
          batchProjects.value.forEach(project => {
            const projectId = project.id;
            const submittedHours = submittedHoursMap[projectId] || {};

            batchDates.value.forEach(date => {
              // 如果该日期有已提交的工时记录，则更新
              if (submittedHours[date]) {
                project.hours[date] = submittedHours[date];
              }
            });
          });

          console.log('已填充工时数据到批量表格');
        }
      } catch (error) {
        console.error('加载已提交工时失败:', error);
        ElMessage.error('加载已提交工时失败: ' + error.message);
      } finally {
        batchLoading.value = false;
      }
    }

    // 辅助函数：构建批量项目数据
    const buildBatchProjects = (dates) => {
      console.log('构建批量项目数据，项目数量:', projectList.value.length);
      batchProjects.value = projectList.value.map(project => {
        const hours = {}
        dates.forEach(date => {
          hours[date] = 0
        })

        // 获取项目经理名称
        let managerName = '未知';
        if (project.managerName) {
          // 如果直接有managerName字段
          managerName = project.managerName;
        } else if (project.manager) {
          // 如果有manager对象
          if (typeof project.manager === 'string') {
            // 如果manager是字符串
            managerName = project.manager;
          } else if (project.manager.name) {
            // 如果manager对象有name属性
            managerName = project.manager.name;
          } else if (project.manager.username) {
            // 如果manager对象有username属性
            managerName = project.manager.username;
          } else if (project.manager.firstName && project.manager.lastName) {
            // 如果manager对象有firstName和lastName属性
            managerName = `${project.manager.firstName} ${project.manager.lastName}`;
          } else if (project.manager.firstName) {
            // 如果manager对象只有firstName属性
            managerName = project.manager.firstName;
          } else if (project.manager.lastName) {
            // 如果manager对象只有lastName属性
            managerName = project.manager.lastName;
          }
        }

        console.log('项目:', project.name, '经理:', managerName);

        return {
          id: project.id,
          name: project.name,
          managerName: managerName,
          hours
        }
      })
      console.log('批量项目数据构建完成，数量:', batchProjects.value.length);
    }

    const formatDateLabel = (dateStr) => {
      const [, month, day] = dateStr.split('-') // 使用逗号跳过不需要的年份变量
      return `${month}/${day}`
    }

    const calculateTotalHours = (hoursObj) => {
      return Object.values(hoursObj).reduce((sum, val) => sum + (val || 0), 0)
    }

    const validateHours = (hours, project, date) => {
      if (hours !== null && hours !== undefined && hours !== '') {
        if (hours < 1 || hours > 8) {
          ElMessage.warning('工时必须在1-8小时之间')
          // 重置为空值
          if (project && date) {
            project.hours[date] = null;
          }
          return false
        }
      }
      return true
    }

    const getCellClass = ({ columnIndex }) => {
      if (columnIndex > 2 && columnIndex < batchDates.value.length + 3) {
        const date = batchDates.value[columnIndex - 3]
        const day = new Date(date).getDay()
        if (day === 0 || day === 6) {
          return 'weekend-cell'
        }
      }
      return ''
    }

    // 获取星期几显示
    const getWeekday = (dateStr) => {
      const date = new Date(dateStr);
      const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
      return weekdays[date.getDay()];
    }

    // 判断是否为周末
    const isWeekend = (dateStr) => {
      const date = new Date(dateStr);
      const day = date.getDay();
      return day === 0 || day === 6; // 0是周日，6是周六
    }

    // 获取日期列的样式类
    const getDateColumnClass = (date) => {
      return isWeekend(date) ? 'weekend-column' : 'weekday-column';
    }

    // 获取输入框样式类
    const getInputClass = (hours) => {
      if (!hours || hours === 0) return '';
      if (hours >= 1 && hours <= 8) return 'valid-hours';
      return 'invalid-hours';
    }

    // 获取项目状态标签类型
    const getProjectStatusTagType = (status) => {
      const statusMap = {
        'PLANNING': 'info',
        'IN_PROGRESS': 'success',
        'COMPLETED': '',
        'ON_HOLD': 'warning',
        'CANCELLED': 'danger'
      };
      return statusMap[status] || 'info';
    }

    // 获取项目状态文本
    const getProjectStatusText = (status) => {
      const statusMap = {
        'PLANNING': '规划中',
        'IN_PROGRESS': '进行中',
        'COMPLETED': '已完成',
        'ON_HOLD': '暂停',
        'CANCELLED': '已取消'
      };
      return statusMap[status] || '未知';
    }

    // 获取总工时标签类型
    const getTotalHoursTagType = (hours) => {
      if (hours === 0) return 'info';
      if (hours <= 40) return 'success';
      if (hours <= 60) return 'warning';
      return 'danger';
    }

    // 快捷操作：工作日填8小时
    const fillWorkdaysWithEight = () => {
      batchProjects.value.forEach(project => {
        batchDates.value.forEach(date => {
          if (!isWeekend(date)) {
            project.hours[date] = 8;
          }
        });
      });
      ElMessage.success('已为所有工作日填写8小时');
    }

    // 快捷操作：全部填指定小时数
    const fillAllDaysWithValue = (hours) => {
      batchProjects.value.forEach(project => {
        batchDates.value.forEach(date => {
          project.hours[date] = hours;
        });
      });
      ElMessage.success(`已为所有日期填写${hours}小时`);
    }

    // 快捷操作：清空周末
    const clearWeekends = () => {
      batchProjects.value.forEach(project => {
        batchDates.value.forEach(date => {
          if (isWeekend(date)) {
            project.hours[date] = null;
          }
        });
      });
      ElMessage.success('已清空所有周末的工时');
    }

    // 清空所有工时数据
    const clearAllTimeData = () => {
      ElMessageBox.confirm('确定要清空所有工时数据吗？', '确认清空', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        batchProjects.value.forEach(project => {
          project.hours = {};
          batchDates.value.forEach(date => {
            project.hours[date] = null;
          });
        });
        ElMessage.success('已清空所有工时数据');
      }).catch(() => {
        // 用户取消操作
      });
    }

    // 获取详细报表数据
    const fetchDetailReportData = async () => {
      if (!currentUser.value?.id || !statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
        return;
      }

      try {
        const params = {
          startDate: statisticsDateRange.value[0],
          endDate: statisticsDateRange.value[1],
          page: reportPagination.currentPage - 1,
          size: reportPagination.pageSize
        };

        if (selectedProjectId.value) {
          params.projectId = selectedProjectId.value;
        }

        const response = await api.get(`/api/worktime/user/${currentUser.value.id}/range`, { params });
        
        if (Array.isArray(response.data)) {
          detailReportData.value = response.data.map(record => ({
            ...record,
            projectName: record.project?.name || '未知项目'
          }));
          reportPagination.total = response.data.length;
        } else if (response.data?.content) {
          detailReportData.value = response.data.content.map(record => ({
            ...record,
            projectName: record.project?.name || '未知项目'
          }));
          reportPagination.total = response.data.totalElements || 0;
        } else {
          detailReportData.value = [];
          reportPagination.total = 0;
        }
      } catch (error) {
        console.error('获取详细报表数据失败:', error);
        detailReportData.value = [];
      }
    }

    // 计算加班统计
    const calculateOvertimeStats = () => {
      if (!detailReportData.value.length) {
        overtimeStats.value = {
          totalOvertimeHours: 0,
          overtimeDays: 0,
          averageOvertimePerDay: 0,
          maxDailyHours: 0
        };
        return;
      }

      // 按日期聚合工时
      const dailyHours = {};
      detailReportData.value.forEach(record => {
        if (record.approved) { // 只统计已审核的工时
          const date = record.date;
          dailyHours[date] = (dailyHours[date] || 0) + record.hours;
        }
      });

      let totalOvertimeHours = 0;
      let overtimeDays = 0;
      let maxDailyHours = 0;

      Object.values(dailyHours).forEach(hours => {
        maxDailyHours = Math.max(maxDailyHours, hours);
        if (hours > 8) {
          totalOvertimeHours += (hours - 8);
          overtimeDays++;
        }
      });

      overtimeStats.value = {
        totalOvertimeHours: totalOvertimeHours.toFixed(2),
        overtimeDays,
        averageOvertimePerDay: overtimeDays > 0 ? (totalOvertimeHours / overtimeDays).toFixed(2) : 0,
        maxDailyHours: maxDailyHours.toFixed(2)
      };

      // 初始化加班趋势图
      setTimeout(() => {
        initOvertimeChart(dailyHours);
      }, 100);
    }

    // 初始化加班趋势图
    const initOvertimeChart = (dailyHours) => {
      try {
        const chartDom = document.getElementById('overtimeChart');
        if (!chartDom) return;

        const chart = echarts.init(chartDom);
        
        // 准备数据
        const dates = Object.keys(dailyHours).sort();
        const hoursData = dates.map(date => dailyHours[date]);
        const overtimeData = dates.map(date => Math.max(0, dailyHours[date] - 8));

        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'cross'
            }
          },
          legend: {
            data: ['总工时', '加班时长', '标准工时']
          },
          xAxis: {
            type: 'category',
            data: dates,
            axisLabel: {
              formatter: function(value) {
                return value.substring(5); // 只显示月-日
              }
            }
          },
          yAxis: {
            type: 'value',
            name: '小时'
          },
          series: [
            {
              name: '总工时',
              type: 'line',
              data: hoursData,
              itemStyle: { color: '#409EFF' }
            },
            {
              name: '加班时长',
              type: 'bar',
              data: overtimeData,
              itemStyle: { color: '#F56C6C' }
            },
            {
              name: '标准工时',
              type: 'line',
              data: dates.map(() => 8),
              itemStyle: { color: '#67C23A' },
              lineStyle: { type: 'dashed' }
            }
          ]
        };

        chart.setOption(option);
      } catch (error) {
        console.error('初始化加班图表失败:', error);
      }
    }

    // 导出工时报表
    const exportWorkTimeReport = async () => {
      if (!currentUser.value?.id) {
        ElMessage.warning('未获取到当前用户信息');
        return;
      }

      exportLoading.value = true;
      try {
        const params = {
          startDate: statisticsDateRange.value[0],
          endDate: statisticsDateRange.value[1],
          format: 'excel'
        };

        if (selectedProjectId.value) {
          params.projectId = selectedProjectId.value;
        }

        const response = await api.get(`/api/export/worktime/${currentUser.value.id}`, { 
          params,
          responseType: 'blob'
        });

        // 创建下载链接
        const blob = new Blob([response.data]);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `工时报表_${currentUser.value.firstName}_${statisticsDateRange.value[0]}_${statisticsDateRange.value[1]}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        ElMessage.success('报表导出成功');
      } catch (error) {
        console.error('导出报表失败:', error);
        ElMessage.error('导出报表失败: ' + error.message);
      } finally {
        exportLoading.value = false;
      }
    }

    // 生成周报
    const generateWeeklyReport = async () => {
      reportLoading.value = true;
      try {
        // 获取本周的开始日期（周一）
        const now = new Date();
        const day = now.getDay();
        const diff = now.getDate() - day + (day === 0 ? -6 : 1); // 调整到周一
        const monday = new Date(now.setDate(diff));
        const weekStartDate = monday.toISOString().split('T')[0];

        const response = await api.get(`/api/worktime/user/${currentUser.value.id}/weekly-report`, {
          params: { weekStartDate }
        });

        // 显示周报内容
        const report = response.data;
        const reportContent = `
          本周工时汇总 (${weekStartDate} - ${new Date(monday.getTime() + 6 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]})
          
          总工时: ${report.totalHours || 0} 小时
          工作天数: ${report.workingDays || 0} 天
          平均每日工时: ${report.averageHoursPerDay || 0} 小时
          项目分布: ${Object.entries(report.projectHours || {}).map(([project, hours]) => `${project}: ${hours}小时`).join(', ')}
        `;

        ElMessageBox.alert(reportContent, '周报生成完成', {
          confirmButtonText: '确定',
          type: 'success'
        });
      } catch (error) {
        console.error('生成周报失败:', error);
        ElMessage.error('生成周报失败: ' + error.message);
      } finally {
        reportLoading.value = false;
      }
    }

    // 生成月报
    const generateMonthlyReport = async () => {
      reportLoading.value = true;
      try {
        // 获取本月的开始日期
        const now = new Date();
        const monthStartDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;

        const response = await api.get(`/api/worktime/user/${currentUser.value.id}/monthly-report`, {
          params: { monthStartDate }
        });

        // 显示月报内容
        const report = response.data;
        const reportContent = `
          本月工时汇总 (${monthStartDate} - ${now.toISOString().split('T')[0]})
          
          总工时: ${report.totalHours || 0} 小时
          工作天数: ${report.workingDays || 0} 天
          平均每日工时: ${report.averageHoursPerDay || 0} 小时
          加班时长: ${report.overtimeHours || 0} 小时
          项目分布: ${Object.entries(report.projectHours || {}).map(([project, hours]) => `${project}: ${hours}小时`).join(', ')}
        `;

        ElMessageBox.alert(reportContent, '月报生成完成', {
          confirmButtonText: '确定',
          type: 'success'
        });
      } catch (error) {
        console.error('生成月报失败:', error);
        ElMessage.error('生成月报失败: ' + error.message);
      } finally {
        reportLoading.value = false;
      }
    }

    // 报表分页处理
    const handleReportSizeChange = (size) => {
      reportPagination.pageSize = size;
      fetchDetailReportData();
    }

    const handleReportCurrentChange = (page) => {
      reportPagination.currentPage = page;
      fetchDetailReportData();
    }

    const submitBatchTimeRecords = async () => {
      if (!currentUser.value || !currentUser.value.id) {
        // 如果没有用户信息，尝试从缓存中获取
        if (userStore.user && userStore.user.id) {
          // 用户信息存在于缓存中
          currentUser.value = userStore.user
        } else {
          ElMessage.warning('未获取到当前用户信息')
          return
        }
      }

      const records = []
      batchProjects.value.forEach(project => {
        Object.entries(project.hours).forEach(([date, hours]) => {
          if (hours > 0) {
            records.push({
              project: { id: project.id },
              user: { id: currentUser.value.id },
              date,
              hours,
              workType: '开发', // 默认值，可根据需求调整
              description: '批量填写',
              approved: false
            })
          }
        })
      })

      if (records.length === 0) {
        ElMessage.warning('没有需要提交的工时记录')
        return
      }

      try {
        await api.post('/api/worktime/batch', records)
        ElMessage.success(`成功提交 ${records.length} 条工时记录`)
        // 加载已提交的工时数据，而不是重置整个表格
        loadSubmittedWorkTime()
      } catch (error) {
        console.error('批量提交工时失败:', error)
        ElMessage.error('批量提交工时失败: ' + error.message)
      }
    }

    const getStatusTagType = (approved) => {
      return approved ? 'success' : 'warning'
    }

    const onDateRangeChange = (range) => {
      console.log('统计报表日期范围已更改:', range);
      if (range && range.length === 2) {
        // 日期范围变化时自动获取统计数据
        fetchStatisticsData();
      }
    }

    const onBatchDateRangeChange = (range) => {
      console.log('批量填写日期范围已更改:', range);
      if (range && range.length === 2) {
        // 确保日期范围有效
        try {
          const startDate = new Date(range[0]);
          const endDate = new Date(range[1]);

          if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
            console.error('日期格式无效');
            ElMessage.warning('日期格式无效，请重新选择');
            return;
          }

          if (startDate > endDate) {
            console.error('开始日期不能晚于结束日期');
            ElMessage.warning('开始日期不能晚于结束日期');
            return;
          }

          // 更新日期范围并生成表格
          batchDateRange.value = range;
          console.log('更新后的批量日期范围:', batchDateRange.value);
          generateBatchTable();
          // 加载已提交的工时数据
          loadSubmittedWorkTime();
        } catch (error) {
          console.error('处理日期范围时出错:', error);
          ElMessage.error('处理日期范围时出错: ' + error.message);
        }
      } else {
        console.warn('日期范围无效或不完整');
      }
    }

    // 监听项目选择变化
    watch(selectedProjectId, (newValue) => {
      console.log('选择的项目已更改:', newValue);
      if (statisticsDateRange.value && statisticsDateRange.value.length === 2) {
        // 项目选择变化时自动获取统计数据
        fetchStatisticsData();
      }
    });

    const tableRowClassName = () => {
      // 可以根据行数据设置不同的类名
      return '';
    }

    onMounted(async () => {
      try {
        // 初始化审批搜索表单
        initApprovalSearchForm()

        // 在页面加载时确保从缓存中获取用户信息，如果没有则从服务器获取
        if (!currentUser.value || !currentUser.value.id) {
          if (userStore.user && userStore.user.id) {
            currentUser.value = userStore.user
          } else if (userStore.token) {
            await userStore.fetchUser()
            if (userStore.user && userStore.user.id) {
              currentUser.value = userStore.user
            }
          }
        }

        // 先获取项目列表
        await fetchProjects()

        // 然后获取审批列表
        await fetchApprovalList()
      } catch (error) {
        console.error('组件初始化失败:', error)
        ElMessage.error('页面加载失败，请刷新重试')
      }

      // 初始化统计日期范围为当前和上个月
      if (!statisticsDateRange.value || statisticsDateRange.value.length !== 2) {
        // 获取默认日期范围
        const defaultRange = getDefaultDateRange();
        console.log('获取到的默认日期范围:', defaultRange);

        if (defaultRange && defaultRange.length === 2) {
          // 使用全局配置的默认日期范围
          statisticsDateRange.value = defaultRange;
          console.log('已设置统计日期范围为默认值:', statisticsDateRange.value);
        } else {
          // 如果默认日期范围无效，则使用上个月1号到当前月最后一天
          const now = new Date();
          const year = now.getFullYear();
          const month = now.getMonth() + 1;
          const lastMonth = month === 1 ? 12 : month - 1;
          const lastYear = month === 1 ? year - 1 : year;

          // 上个月的第一天
          const startDate = `${lastYear}-${String(lastMonth).padStart(2, '0')}-01`;

          // 当前月的最后一天
          const lastDay = new Date(year, month, 0).getDate();
          const endDate = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;

          statisticsDateRange.value = [startDate, endDate];
          console.log('已设置统计日期范围为上月1日至本月最后一天:', statisticsDateRange.value);
        }
      }

      // 自动获取统计数据
      if (statisticsDateRange.value && statisticsDateRange.value.length === 2) {
        fetchStatisticsData();
      }

      // 检查当前用户是否为审批人（这里是简化的逻辑，实际应根据权限系统判断）
      if (currentUser.value && currentUser.value.role) {
        // 确保 roleStr 是字符串类型，并检查是否包含 'manager'
        const roleStr = Array.isArray(currentUser.value.role) ? currentUser.value.role.join(',') : String(currentUser.value.role);
        if (roleStr.includes('manager')) {
          isApprover.value = true
        }
      }
    })

    watch(activeTab, (newTab) => {
      if (newTab === 'batch' && projectList.value.length > 0) {
        initBatchDateRange()
      } else if (newTab === 'approval') {
        fetchApprovalList()
      }
    })
</script>

<style scoped>
.time-management-container {
  padding: 20px;
  max-width: 100%;
  overflow-x: auto;
}

h1 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
  text-align: center;
}

.report-form {
  max-width: 600px;
  margin: 0 auto;
}

.approval-container {
  margin-top: 20px;
}

.search-bar {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.query-result-info {
  margin-bottom: 16px;
  padding: 10px 15px;
  background-color: #e6f7ff;
  border: 1px solid #bae7ff;
  border-radius: 4px;
  color: #1890ff;
  font-size: 14px;
}

.highlight {
  font-weight: bold;
  color: #0052cc;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.batch-container {
  margin-top: 20px;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.batch-table-container {
  margin-top: 20px;
  overflow-x: auto;
}

.batch-empty {
  margin-top: 20px;
  text-align: center;
}

.statistics-container {
  margin-top: 20px;
}

.statistics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.statistics-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.statistics-summary {
  margin-bottom: 20px;
}

.summary-content {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
}

.summary-item {
  margin: 10px 0;
  min-width: 200px;
}

.summary-label {
  color: #909399;
  font-size: 14px;
}

.summary-value {
  color: #303133;
  font-size: 18px;
  font-weight: bold;
}

.statistics-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.chart-container {
  margin-top: 30px;
}

.weekend-cell {
  background-color: #f0f9ff;
}

/* 自定义 Tab 样式 */
.custom-tabs {
  margin-bottom: 20px;
}

.custom-tabs-header {
  display: flex;
  border-bottom: 2px solid #e4e7ed;
  margin-bottom: 15px;
}

.custom-tab {
  padding: 10px 20px;
  margin-right: 5px;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  font-size: 14px;
  color: #606266;
}

.custom-tab:hover {
  color: #409eff;
}

.custom-tab.active {
  color: #409eff;
  border-bottom: 2px solid #409eff;
}

.tab-pane {
  padding: 10px 0;
}

.form-item-hint {
  margin-top: 8px;
  font-size: 12px;
}
</style>