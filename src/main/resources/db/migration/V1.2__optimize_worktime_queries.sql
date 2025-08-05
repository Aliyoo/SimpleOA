-- 为工时记录表添加索引以优化查询性能
-- 用于按用户、项目、日期组合查询的复合索引
CREATE INDEX IF NOT EXISTS idx_worktime_user_project_date 
ON work_time_record(user_id, project_id, date);

-- 用于按用户和日期范围查询的索引
CREATE INDEX IF NOT EXISTS idx_worktime_user_date 
ON work_time_record(user_id, date);

-- 用于按项目和日期范围查询的索引
CREATE INDEX IF NOT EXISTS idx_worktime_project_date 
ON work_time_record(project_id, date);

-- 用于按审批状态查询的索引
CREATE INDEX IF NOT EXISTS idx_worktime_approved 
ON work_time_record(approved);

-- 用于优化统计查询的复合索引
CREATE INDEX IF NOT EXISTS idx_worktime_date_hours 
ON work_time_record(date, hours);
