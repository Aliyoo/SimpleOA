-- ====================================================================
-- SimpleOA Approval Workflow Refactoring - Phase 4
-- Migration: V24__migrate_reimbursement_approvals
-- Description: Migrate Reimbursement approval data to polymorphic structure
-- Author: SimpleOA Refactoring Team
-- Date: 2025-12-26
-- ====================================================================

-- Step 1: Migrate existing Reimbursement approval data to new polymorphic columns
-- Note: request_type in approval_flow is 'REIMBURSEMENT'

UPDATE approval_flow
SET
    entity_type = 'REIMBURSEMENT',
    entity_id = reimbursement_request_id,
    approval_status = CASE
        WHEN UPPER(status) IN ('APPROVED', 'PENDING', 'REJECTED', 'CANCELLED', 'REVOKED')
        THEN UPPER(status)
        ELSE 'PENDING'
    END,
    approval_stage = CASE
        WHEN UPPER(status) = 'APPROVED' THEN 'FINAL'
        WHEN UPPER(status) = 'PENDING' THEN 'INITIAL'
        ELSE 'INITIAL'
    END
WHERE request_type = 'REIMBURSEMENT'
  AND reimbursement_request_id IS NOT NULL
  AND entity_type IS NULL;

-- Step 2: Verify migration results
SELECT
    'Reimbursement approval migration verification' as check_type,
    COUNT(*) as total_reimbursement_approvals,
    COUNT(CASE WHEN entity_type = 'REIMBURSEMENT' THEN 1 END) as migrated_count,
    COUNT(CASE WHEN entity_type IS NULL THEN 1 END) as unmigrated_count,
    ROUND(COUNT(CASE WHEN entity_type = 'REIMBURSEMENT' THEN 1 END) * 100.0 / COUNT(*), 2) as migration_percentage
FROM approval_flow
WHERE request_type = 'REIMBURSEMENT';

-- Step 3: Data consistency checks
SELECT
    'Reimbursement record reference validation' as check_type,
    COUNT(*) as total_reimbursement_approvals,
    COUNT(CASE WHEN entity_id IS NOT NULL THEN 1 END) as with_entity_id,
    COUNT(CASE WHEN entity_id IS NULL THEN 1 END) as missing_entity_id
FROM approval_flow
WHERE request_type = 'REIMBURSEMENT'
  AND entity_type = 'REIMBURSEMENT';

-- Step 4: Status distribution check
SELECT
    'Reimbursement approval status distribution' as check_type,
    approval_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM approval_flow
WHERE request_type = 'REIMBURSEMENT'
  AND entity_type = 'REIMBURSEMENT'
GROUP BY approval_status
ORDER BY count DESC;

-- Step 5: Create index for Reimbursement queries
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_approval_reimbursement_lookup ON approval_flow(entity_type, entity_id, approval_status)',
        'SELECT ''Index already exists'' as message'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'approval_flow'
      AND index_name = 'idx_approval_reimbursement_lookup'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 6: Add comment for documentation
ALTER TABLE approval_flow
MODIFY COLUMN entity_type VARCHAR(50) COMMENT 'Entity type: REIMBURSEMENT for ReimbursementRequest approvals';

ALTER TABLE approval_flow
MODIFY COLUMN entity_id BIGINT COMMENT 'Foreign key to reimbursement_request.id when entity_type=REIMBURSEMENT';

ALTER TABLE approval_flow
MODIFY COLUMN approval_status VARCHAR(20) COMMENT 'Unified approval status: PENDING/APPROVED/REJECTED/CANCELLED/REVOKED';

ALTER TABLE approval_flow
MODIFY COLUMN approval_stage VARCHAR(50) COMMENT 'Approval stage: Reimbursement uses INITIAL/FINANCE/FINAL (multi-level approval)';

-- ====================================================================
-- Migration Notes:
--
-- 1. This migration is NON-BREAKING - existing data remains accessible via legacy columns
-- 2. Legacy column reimbursement_request_id is kept and will be removed in Phase 5
-- 3. Both legacy and new structures are supported during transition
-- 4. Application code reads/writes to both structures (dual-write strategy)
-- 5. Multi-level approval support: Reimbursement supports INITIAL → FINANCE → FINAL
--
-- Verification Queries:
--
-- SELECT COUNT(*) FROM approval_flow
-- WHERE request_type = 'REIMBURSEMENT' AND entity_type = 'REIMBURSEMENT';
--
-- Rollback (if needed):
-- UPDATE approval_flow
-- SET entity_type = NULL, entity_id = NULL, approval_status = NULL, approval_stage = NULL
-- WHERE request_type = 'REIMBURSEMENT';
-- ====================================================================
