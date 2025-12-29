-- ====================================================================
-- SimpleOA Approval Workflow Refactoring - Phase 3
-- Migration: V23__migrate_leave_approvals
-- Description: Migrate LeaveRequest approval data to polymorphic structure
-- Author: SimpleOA Refactoring Team
-- Date: 2025-12-26
-- ====================================================================

-- Step 1: Migrate existing LeaveRequest approval data to new polymorphic columns
-- This step populates entity_type and entity_id for existing LeaveRequest approvals
-- Note: request_type in approval_flow is 'LEAVE' (not 'LEAVE_REQUEST')

UPDATE approval_flow
SET
    entity_type = 'LEAVE_REQUEST',
    entity_id = leave_request_id,
    approval_status = CASE
        WHEN UPPER(status) IN ('APPROVED', 'PENDING', 'REJECTED', 'CANCELLED', 'REVOKED')
        THEN UPPER(status)
        ELSE 'PENDING'
    END,
    approval_stage = CASE
        -- 根据原有状态推断审批阶段
        WHEN UPPER(status) = 'APPROVED' THEN 'FINAL'
        WHEN UPPER(status) = 'PENDING' THEN 'INITIAL'
        ELSE 'INITIAL'
    END
WHERE request_type = 'LEAVE'
  AND leave_request_id IS NOT NULL
  AND entity_type IS NULL;  -- Only migrate records not already migrated

-- Step 2: Verify migration results
-- Check how many LeaveRequest approvals were migrated

SELECT
    'LeaveRequest approval migration verification' as check_type,
    COUNT(*) as total_leave_approvals,
    COUNT(CASE WHEN entity_type = 'LEAVE_REQUEST' THEN 1 END) as migrated_count,
    COUNT(CASE WHEN entity_type IS NULL THEN 1 END) as unmigrated_count,
    ROUND(COUNT(CASE WHEN entity_type = 'LEAVE_REQUEST' THEN 1 END) * 100.0 / COUNT(*), 2) as migration_percentage
FROM approval_flow
WHERE request_type = 'LEAVE';

-- Step 3: Data consistency checks
-- Ensure all migrated records have valid entity_id references

SELECT
    'LeaveRequest record reference validation' as check_type,
    COUNT(*) as total_leave_approvals,
    COUNT(CASE WHEN entity_id IS NOT NULL THEN 1 END) as with_entity_id,
    COUNT(CASE WHEN entity_id IS NULL THEN 1 END) as missing_entity_id
FROM approval_flow
WHERE request_type = 'LEAVE'
  AND entity_type = 'LEAVE_REQUEST';

-- Step 4: Status distribution check
-- Check the distribution of approval statuses

SELECT
    'LeaveRequest approval status distribution' as check_type,
    approval_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM approval_flow
WHERE request_type = 'LEAVE'
  AND entity_type = 'LEAVE_REQUEST'
GROUP BY approval_status
ORDER BY count DESC;

-- Step 5: Approval stage distribution check
-- Check the distribution of approval stages (for multi-level approval)

SELECT
    'LeaveRequest approval stage distribution' as check_type,
    approval_stage,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM approval_flow
WHERE request_type = 'LEAVE'
  AND entity_type = 'LEAVE_REQUEST'
GROUP BY approval_stage
ORDER BY count DESC;

-- Step 6: Create index for LeaveRequest queries
-- This index optimizes queries for LeaveRequest approvals
-- Note: MySQL doesn't support partial indexes or IF NOT EXISTS for indexes
-- Run silently if index already exists (error is acceptable)

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_approval_leave_lookup ON approval_flow(entity_type, entity_id, approval_status)',
        'SELECT ''Index already exists'' as message'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'approval_flow'
      AND index_name = 'idx_approval_leave_lookup'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 7: Add comment for documentation
-- Document the purpose of the new columns for LeaveRequest approvals

ALTER TABLE approval_flow
MODIFY COLUMN entity_type VARCHAR(50) COMMENT 'Entity type: LEAVE_REQUEST for LeaveRequest approvals';

ALTER TABLE approval_flow
MODIFY COLUMN entity_id BIGINT COMMENT 'Foreign key to leave_request.id when entity_type=LEAVE_REQUEST';

ALTER TABLE approval_flow
MODIFY COLUMN approval_status VARCHAR(20) COMMENT 'Unified approval status: PENDING/APPROVED/REJECTED/CANCELLED/REVOKED';

ALTER TABLE approval_flow
MODIFY COLUMN approval_stage VARCHAR(50) COMMENT 'Approval stage: LeaveRequest uses INITIAL/HR_REVIEW/FINAL (multi-level approval)';

-- ====================================================================
-- Migration Notes:
--
-- 1. This migration is NON-BREAKING - existing data remains accessible via legacy columns
-- 2. Legacy column leave_request_id is kept and will be removed in Phase 5
-- 3. Both legacy and new structures are supported during transition
-- 4. Application code reads/writes to both structures (dual-write strategy)
-- 5. Multi-level approval support: LeaveRequest supports INITIAL → HR_REVIEW → FINAL
--
-- Verification Queries:
--
-- Check if migration was successful:
-- SELECT COUNT(*) FROM approval_flow
-- WHERE request_type = 'LEAVE' AND entity_type = 'LEAVE_REQUEST';
--
-- Expected result: All LeaveRequest approvals should have entity_type='LEAVE_REQUEST'
--
-- Rollback (if needed):
-- UPDATE approval_flow
-- SET entity_type = NULL, entity_id = NULL, approval_status = NULL, approval_stage = NULL
-- WHERE request_type = 'LEAVE';
-- ====================================================================
