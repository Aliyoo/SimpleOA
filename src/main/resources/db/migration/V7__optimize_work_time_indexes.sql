-- 工时记录表性能优化索引
-- 添加针对批量填写页面的性能优化索引

-- 用户工时查询优化 - 支持按用户和日期查询
CREATE INDEX idx_work_time_user_date ON work_time_record(user_id, date);

-- 项目工时查询优化 - 支持按项目和日期查询  
CREATE INDEX idx_work_time_project_date ON work_time_record(project_id, date);

-- 审批状态查询优化 - 支持按日期和审批状态查询
CREATE INDEX idx_work_time_date_approved ON work_time_record(date, approved);

-- 批量填写去重查询优化 - 支持用户-项目-日期的唯一性检查
CREATE INDEX idx_work_time_user_project_date ON work_time_record(user_id, project_id, date);

-- 统计查询优化 - 支持按日期和工时统计
CREATE INDEX idx_work_time_date_hours ON work_time_record(date, hours);

-- 项目成员查询优化 - 支持快速查找项目成员
CREATE INDEX idx_project_user_project ON project_user(project_id);
CREATE INDEX idx_project_user_user ON project_user(user_id);

-- 审批流程查询优化
CREATE INDEX idx_approval_flow_work_time ON approval_flow(work_time_record_id);
CREATE INDEX idx_approval_flow_approver_status ON approval_flow(approver_id, status);