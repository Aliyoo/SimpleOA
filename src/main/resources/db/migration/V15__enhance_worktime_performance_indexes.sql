-- 工时查询统计性能优化 - 增强索引
-- 基于V7索引的补充优化，专门针对统计查询和批量操作

-- 用户统计查询复合索引 - 优化用户工时统计页面
CREATE INDEX  idx_work_time_user_date_hours ON work_time_record(user_id, date, hours);

-- 项目成员批量查询优化 - 支持项目经理查看所有成员工时
CREATE INDEX  idx_work_time_project_user_date ON work_time_record(project_id, user_id, date);

-- 审批状态统计优化 - 支持审批工作台快速统计
CREATE INDEX  idx_work_time_approved_date_user ON work_time_record(approved, date, user_id);

-- 工作类型统计查询优化 - 支持按工作类型统计
CREATE INDEX  idx_work_time_work_type_date ON work_time_record(work_type, date);

-- 时间范围聚合查询优化 - 支持月报、周报等时间范围统计
CREATE INDEX  idx_work_time_date_project_hours ON work_time_record(date, project_id, hours);

-- 复合条件筛选优化 - 支持多条件筛选查询
CREATE INDEX  idx_work_time_user_project_approved ON work_time_record(user_id, project_id, approved, date);

-- 批量更新优化 - 支持批量审批等操作
CREATE INDEX  idx_work_time_status_user ON work_time_record(status, user_id);

-- 分页查询优化 - 支持按ID游标分页，避免深度分页问题
CREATE INDEX  idx_work_time_id_date ON work_time_record(id, date);

-- 统计汇总查询优化 - 支持Dashboard统计
CREATE INDEX  idx_work_time_date_approved_hours ON work_time_record(date, approved, hours);

-- 数据导出查询优化 - 支持大数据量导出
CREATE INDEX  idx_work_time_created_at_user ON work_time_record(created_at, user_id);

-- 添加统计信息更新
ANALYZE TABLE work_time_record;

-- 验证索引创建结果
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'work_time_record'
AND INDEX_NAME LIKE 'idx_work_time_%'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;