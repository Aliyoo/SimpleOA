-- IMPORTANT:
-- 1. ALWAYS BACKUP YOUR DATABASE BEFORE RUNNING ANY MIGRATION SCRIPTS.
-- 2. THESE SCRIPTS ARE SUGGESTIONS. PLEASE REVIEW AND ADJUST SYNTAX
--    ACCORDING TO YOUR SPECIFIC DATABASE SYSTEM (e.g., MySQL, PostgreSQL, SQL Server, Oracle).
-- 3. TEST THESE SCRIPTS IN A DEVELOPMENT OR STAGING ENVIRONMENT BEFORE
--    APPLYING THEM TO YOUR PRODUCTION DATABASE.

-- =====================================================================================
-- Migration for user_roles to role_user
-- =====================================================================================

-- Comment: Migrating data from the old user_roles table to the new unified role_user table.
-- This ensures that existing user-role relationships are preserved.

-- For PostgreSQL (uses ON CONFLICT DO NOTHING):
-- INSERT INTO role_user (user_id, role_id)
-- SELECT user_id, role_id FROM user_roles
-- ON CONFLICT (user_id, role_id) DO NOTHING;

-- For MySQL (uses INSERT IGNORE):
-- INSERT IGNORE INTO role_user (user_id, role_id)
-- SELECT user_id, role_id FROM user_roles;

-- For SQL Server (uses NOT EXISTS):
-- INSERT INTO role_user (user_id, role_id)
-- SELECT ur.user_id, ur.role_id
-- FROM user_roles ur
-- WHERE NOT EXISTS (
--     SELECT 1
--     FROM role_user ru
--     WHERE ru.user_id = ur.user_id AND ru.role_id = ur.role_id
-- );

-- For Oracle (uses MERGE):
-- MERGE INTO role_user ru
-- USING user_roles ur
-- ON (ru.user_id = ur.user_id AND ru.role_id = ur.role_id)
-- WHEN NOT MATCHED THEN
--   INSERT (ru.user_id, ru.role_id) VALUES (ur.user_id, ur.role_id);

-- Please uncomment and adapt ONE of the above INSERT statements suitable for your database.
-- Example for PostgreSQL is uncommented below. Please verify and change if needed.
INSERT INTO role_user (user_id, role_id)
SELECT user_id, role_id FROM user_roles
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Comment: Removing the old user_roles table after data migration.
DROP TABLE IF EXISTS user_roles;

-- =====================================================================================
-- Cleanup for permission_role_mapping
-- =====================================================================================

-- Comment: Removing the permission_role_mapping table as it is no longer defined
-- by the application entities. Role-permission relationships are now managed
-- directly within the Role entity's permissions collection.
DROP TABLE IF EXISTS permission_role_mapping;

-- End of migration scripts.
-- Remember to review and test thoroughly.

-- =====================================================================================
-- Add project_id to reimbursement_request table
-- =====================================================================================
-- Comment: Adding a foreign key column to associate reimbursement requests with projects.
-- This allows tracking reimbursements against specific projects for cost analysis.

-- Add the project_id column, allowing NULL values if a reimbursement is not project-specific.
ALTER TABLE reimbursement_request ADD COLUMN project_id BIGINT NULL;

-- Add a foreign key constraint to link project_id to the id column of the project table.
-- Note: Ensure the 'project' table and its 'id' column exist before running this.
-- The ON DELETE SET NULL behavior means if a project is deleted, the project_id in
-- reimbursement_request will be set to NULL rather than deleting the reimbursement request.
-- Adjust ON DELETE behavior (e.g., ON DELETE RESTRICT) as per business rules.
ALTER TABLE reimbursement_request
ADD CONSTRAINT fk_reimbursement_project
FOREIGN KEY (project_id) REFERENCES project(id)
ON DELETE SET NULL; -- Or ON DELETE RESTRICT, ON DELETE CASCADE, as appropriate.

-- Example for MySQL if a specific syntax is preferred:
-- ALTER TABLE reimbursement_request ADD COLUMN project_id BIGINT NULL;
-- ALTER TABLE reimbursement_request ADD CONSTRAINT fk_reimbursement_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL;

-- Example for PostgreSQL (syntax is usually similar for this):
-- ALTER TABLE reimbursement_request ADD COLUMN project_id BIGINT NULL;
-- ALTER TABLE reimbursement_request ADD CONSTRAINT fk_reimbursement_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL;
