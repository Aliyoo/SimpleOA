-- ====================================================================
-- SimpleOA Approval Workflow Refactoring - Phase 1
-- Migration: V21__migrate_worktime_approvals
-- Description: Migrate WorkTime approval data to polymorphic structure
-- Author: SimpleOA Refactoring Team
-- Date: 2025-12-23
-- ====================================================================

-- Step 1: Migrate existing WorkTime approval data to new polymorphic columns
-- This step populates entity_type and entity_id for existing WorkTime approvals

UPDATE approval_flow
SET
    entity_type = 'WORK_TIME',
    entity_id = work_time_record_id,
    approval_status = CASE
        WHEN UPPER(status) IN ('APPROVED', 'PENDING', 'REJECTED', 'CANCELLED', 'REVOKED')
        THEN UPPER(status)
        ELSE 'PENDING'
    END,
    approval_stage = 'INITIAL'
WHERE request_type = 'WORKTIME'
  AND work_time_record_id IS NOT NULL
  AND entity_type IS NULL;  -- Only migrate records not already migrated

-- Step 2: Verify migration results
-- Check how many WorkTime approvals were migrated

SELECT
    'WorkTime approval migration verification' as check_type,
    COUNT(*) as total_worktime_approvals,
    COUNT(CASE WHEN entity_type = 'WORK_TIME' THEN 1 END) as migrated_count,
    COUNT(CASE WHEN entity_type IS NULL THEN 1 END) as unmigrated_count,
    ROUND(COUNT(CASE WHEN entity_type = 'WORK_TIME' THEN 1 END) * 100.0 / COUNT(*), 2) as migration_percentage
FROM approval_flow
WHERE request_type = 'WORKTIME';

-- Step 3: Data consistency checks
-- Ensure all migrated records have valid entity_id references

SELECT
    'WorkTime record reference validation' as check_type,
    COUNT(*) as total_worktime_approvals,
    COUNT(CASE WHEN entity_id IS NOT NULL THEN 1 END) as with_entity_id,
    COUNT(CASE WHEN entity_id IS NULL THEN 1 END) as missing_entity_id
FROM approval_flow
WHERE request_type = 'WORKTIME'
  AND entity_type = 'WORK_TIME';

-- Step 4: Status distribution check
-- Check the distribution of approval statuses

SELECT
    'WorkTime approval status distribution' as check_type,
    approval_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM approval_flow
WHERE request_type = 'WORKTIME'
  AND entity_type = 'WORK_TIME'
GROUP BY approval_status
ORDER BY count DESC;

-- Step 5: Create index for WorkTime queries
-- This index optimizes queries for WorkTime approvals
-- Note: MySQL doesn't support partial indexes or IF NOT EXISTS for indexes
-- Run silently if index already exists (error is acceptable)

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_approval_worktime_lookup ON approval_flow(entity_type, entity_id, approval_status)',
        'SELECT ''Index already exists'' as message'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'approval_flow'
      AND index_name = 'idx_approval_worktime_lookup'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 6: Add comment for documentation
-- Document the purpose of the new columns for WorkTime approvals

ALTER TABLE approval_flow
MODIFY COLUMN entity_type VARCHAR(50) COMMENT 'Entity type: WORK_TIME for WorkTimeRecord approvals';

ALTER TABLE approval_flow
MODIFY COLUMN entity_id BIGINT COMMENT 'Foreign key to work_time_record.id when entity_type=WORK_TIME';

ALTER TABLE approval_flow
MODIFY COLUMN approval_status VARCHAR(20) COMMENT 'Unified approval status: PENDING/APPROVED/REJECTED/CANCELLED/REVOKED';

ALTER TABLE approval_flow
MODIFY COLUMN approval_stage VARCHAR(50) COMMENT 'Approval stage: WorkTime uses INITIAL (single-level approval)';

-- ====================================================================
-- Migration Notes:
--
-- 1. This migration is NON-BREAKING - existing data remains accessible via legacy columns
-- 2. Legacy column work_time_record_id is kept and will be removed in Phase 5
-- 3. Both legacy and new structures are supported during transition
-- 4. Application code reads/writes to both structures (dual-write strategy)
--
-- Verification Queries:
--
-- Check if migration was successful:
-- SELECT COUNT(*) FROM approval_flow
-- WHERE request_type = 'WORKTIME' AND entity_type = 'WORK_TIME';
--
-- Expected result: All WorkTime approvals should have entity_type='WORK_TIME'
--
-- Rollback (if needed):
-- UPDATE approval_flow
-- SET entity_type = NULL, entity_id = NULL, approval_status = NULL, approval_stage = NULL
-- WHERE request_type = 'WORKTIME';
-- ====================================================================
