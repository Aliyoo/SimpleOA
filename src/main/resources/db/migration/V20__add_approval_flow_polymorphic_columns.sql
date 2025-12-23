-- ====================================================================
-- SimpleOA Approval Workflow Refactoring - Phase 0
-- Migration: V20__add_approval_flow_polymorphic_columns
-- Description: Add polymorphic columns to approval_flow table for unified design
-- Author: SimpleOA Refactoring Team
-- Date: 2025-01-XX
-- ====================================================================

-- Step 1: Add new polymorphic columns
-- These columns will coexist with legacy foreign keys during migration period

ALTER TABLE approval_flow
ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50) NULL COMMENT 'Entity type enum (WORK_TIME, LEAVE_REQUEST, BUSINESS_TRIP, REIMBURSEMENT)',
ADD COLUMN IF NOT EXISTS entity_id BIGINT NULL COMMENT 'Polymorphic entity ID - references different tables based on entity_type',
ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NULL COMMENT 'Unified approval status enum (PENDING, APPROVED, REJECTED, CANCELLED, REVOKED)',
ADD COLUMN IF NOT EXISTS approval_stage VARCHAR(50) NULL COMMENT 'Current approval stage for multi-level approvals (INITIAL, MANAGER, HR, FINANCE, FINAL)';

-- Step 2: Create indexes for optimized querying
-- These indexes significantly improve query performance for the new design

-- Composite index for entity lookup (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_approval_entity_type_id
ON approval_flow(entity_type, entity_id);

-- Composite index for approver's pending approvals (dashboard query)
CREATE INDEX IF NOT EXISTS idx_approval_approver_status
ON approval_flow(approver_id, status);

-- Index for date range queries
CREATE INDEX IF NOT EXISTS idx_approval_create_time
ON approval_flow(create_time);

-- Partial index for pending approvals (optimizes dashboard performance)
CREATE INDEX IF NOT EXISTS idx_approval_pending
ON approval_flow(approver_id, create_time DESC)
WHERE status = 'PENDING';

-- Step 3: Add constraints for data integrity
-- Ensure entity_type and entity_id are set together

ALTER TABLE approval_flow
ADD CONSTRAINT chk_approval_flow_entity_consistency
CHECK (
    (entity_type IS NULL AND entity_id IS NULL) OR
    (entity_type IS NOT NULL AND entity_id IS NOT NULL)
);

-- Step 4: Add comments for documentation

ALTER TABLE approval_flow
MODIFY COLUMN entity_type VARCHAR(50) COMMENT 'Entity type: WORK_TIME=WorkTimeRecord, LEAVE_REQUEST=LeaveRequest, BUSINESS_TRIP=BusinessTripRequest, REIMBURSEMENT=ReimbursementRequest';

ALTER TABLE approval_flow
MODIFY COLUMN entity_id BIGINT COMMENT 'Foreign key ID - references the primary key of the table specified by entity_type';

ALTER TABLE approval_flow
MODIFY COLUMN approval_stage VARCHAR(50) COMMENT 'Approval stage: NULL=single-level, INITIAL/manager/HR/finance/FINAL=multi-level';

-- Step 5: Verify schema changes
-- Run validation queries to ensure changes applied successfully

SELECT
    'Table structure verification' as check_type,
    COUNT(*) as total_columns,
    COUNT(CASE WHEN column_name IN ('entity_type', 'entity_id', 'approval_stage') THEN 1 END) as new_columns
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'approval_flow';

SELECT
    'Index verification' as check_type,
    COUNT(*) as total_indexes
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'approval_flow'
  AND index_name IN ('idx_approval_entity_type_id', 'idx_approval_approver_status', 'idx_approval_pending');

-- ====================================================================
-- Migration Notes:
--
-- 1. This migration is NON-BREAKING - existing data and functionality remain intact
-- 2. Legacy foreign key columns (work_time_record_id, leave_request_id, etc.) are kept
-- 3. New columns (entity_type, entity_id) will be populated incrementally per module
-- 4. Application code supports both legacy and new structures during transition
-- 5. Legacy columns will be removed in Phase 5 (V24 migration)
--
-- Next Steps:
-- - Phase 1: Migrate WORK_TIME data to new structure
-- - Phase 2: Migrate BUSINESS_TRIP data to new structure
-- - Phase 3: Migrate LEAVE_REQUEST data to new structure
-- - Phase 4: Migrate REIMBURSEMENT data to new structure
-- - Phase 5: Remove legacy foreign key columns (V24)
-- ====================================================================
