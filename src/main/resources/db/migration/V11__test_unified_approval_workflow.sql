-- V11: 测试统一审批工作流

-- 验证报销请求和审批流程的关联关系
-- 这个脚本仅用于测试，实际环境中不会自动创建测试数据

-- 检查表结构是否正确
-- approval_flow 表应该有 reimbursement_request_id 字段
-- reimbursement_request 表应该有适当的状态字段
-- user 表应该有适当的角色关联

-- 添加约束检查
ALTER TABLE approval_flow 
ADD CONSTRAINT chk_approval_request_type 
CHECK (
    (request_type = 'WORKTIME' AND work_time_record_id IS NOT NULL) OR
    (request_type = 'LEAVE' AND leave_request_id IS NOT NULL) OR
    (request_type = 'BUSINESS_TRIP' AND business_trip_request_id IS NOT NULL) OR
    (request_type = 'REIMBURSEMENT' AND reimbursement_request_id IS NOT NULL)
);

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_approval_flow_request_type ON approval_flow(request_type);
CREATE INDEX IF NOT EXISTS idx_approval_flow_status ON approval_flow(status);
CREATE INDEX IF NOT EXISTS idx_approval_flow_approver_status ON approval_flow(approver_id, status);
CREATE INDEX IF NOT EXISTS idx_reimbursement_request_status ON reimbursement_request(status);
CREATE INDEX IF NOT EXISTS idx_reimbursement_request_applicant ON reimbursement_request(applicant_id);

-- 添加注释说明
COMMENT ON COLUMN approval_flow.request_type IS '审批类型：WORKTIME-工时, LEAVE-请假, BUSINESS_TRIP-出差, REIMBURSEMENT-报销';
COMMENT ON COLUMN approval_flow.status IS '审批状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝';