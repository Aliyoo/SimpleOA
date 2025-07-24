-- V9: 添加报销与预算关联字段
-- 为报销明细表添加预算关联字段
ALTER TABLE reimbursement_item ADD COLUMN budget_id BIGINT;
ALTER TABLE reimbursement_item ADD COLUMN budget_item_id BIGINT;

-- 添加外键约束
ALTER TABLE reimbursement_item ADD CONSTRAINT fk_reimbursement_item_budget 
    FOREIGN KEY (budget_id) REFERENCES budget (id);
ALTER TABLE reimbursement_item ADD CONSTRAINT fk_reimbursement_item_budget_item 
    FOREIGN KEY (budget_item_id) REFERENCES budget_item (id);

-- 为预算支出表添加报销关联字段
ALTER TABLE budget_expense ADD COLUMN reimbursement_request_id BIGINT;
ALTER TABLE budget_expense ADD COLUMN reimbursement_item_id BIGINT;

-- 添加外键约束
ALTER TABLE budget_expense ADD CONSTRAINT fk_budget_expense_reimbursement_request 
    FOREIGN KEY (reimbursement_request_id) REFERENCES reimbursement_request (id);
ALTER TABLE budget_expense ADD CONSTRAINT fk_budget_expense_reimbursement_item 
    FOREIGN KEY (reimbursement_item_id) REFERENCES reimbursement_item (id);

-- 添加索引以提升查询性能
CREATE INDEX idx_reimbursement_item_budget_id ON reimbursement_item (budget_id);
CREATE INDEX idx_reimbursement_item_budget_item_id ON reimbursement_item (budget_item_id);
CREATE INDEX idx_budget_expense_reimbursement_request_id ON budget_expense (reimbursement_request_id);
CREATE INDEX idx_budget_expense_reimbursement_item_id ON budget_expense (reimbursement_item_id);