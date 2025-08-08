package com.example.simpleoa.repository;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WorkTimeRecordRepository extends JpaRepository<WorkTimeRecord, Long> {
    List<WorkTimeRecord> findByUser(User user);
    List<WorkTimeRecord> findByProject(Project project);

    // 按用户和日期范围查询工时记录
    List<WorkTimeRecord> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // 按项目和日期范围查询工时记录
    List<WorkTimeRecord> findByProjectAndDateBetween(Project project, LocalDate startDate, LocalDate endDate);

    // 按日期范围查询所有工时记录
    List<WorkTimeRecord> findByDateBetween(LocalDate startDate, LocalDate endDate);

    // 按用户、项目和日期查询工时记录（用于检查重复记录）
    List<WorkTimeRecord> findByUserAndProjectAndDate(User user, Project project, LocalDate date);

    // 统计项目总工时
    @Query("SELECT SUM(w.hours) FROM WorkTimeRecord w WHERE w.project = :project")
    Double sumHoursByProject(@Param("project") Project project);

    // 统计用户总工时
    @Query("SELECT SUM(w.hours) FROM WorkTimeRecord w WHERE w.user = :user")
    Double sumHoursByUser(@Param("user") User user);

    // 统计用户在指定日期范围内的总工时
    @Query("SELECT SUM(w.hours) FROM WorkTimeRecord w WHERE w.user = :user AND w.date BETWEEN :startDate AND :endDate")
    Double sumHoursByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 统计项目在指定日期范围内的总工时
    @Query("SELECT SUM(w.hours) FROM WorkTimeRecord w WHERE w.project = :project AND w.date BETWEEN :startDate AND :endDate")
    Double sumHoursByProjectAndDateBetween(@Param("project") Project project, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 按用户、日期范围和项目查询工时记录
    List<WorkTimeRecord> findByUserAndDateBetweenAndProject(User user, LocalDate startDate, LocalDate endDate, Project project);

    // 按用户、日期范围和审批状态查询工时记录
    List<WorkTimeRecord> findByUserAndDateBetweenAndApproved(User user, LocalDate startDate, LocalDate endDate, Boolean approved);

    // 按用户、日期范围、项目和审批状态查询工时记录
    List<WorkTimeRecord> findByUserAndDateBetweenAndProjectAndApproved(User user, LocalDate startDate, LocalDate endDate, Project project, Boolean approved);

    // 分页查询方法
    Page<WorkTimeRecord> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<WorkTimeRecord> findByUserAndDateBetweenAndProject(User user, LocalDate startDate, LocalDate endDate, Project project, Pageable pageable);
    Page<WorkTimeRecord> findByUserAndDateBetweenAndApproved(User user, LocalDate startDate, LocalDate endDate, Boolean approved, Pageable pageable);
    Page<WorkTimeRecord> findByUserAndDateBetweenAndProjectAndApproved(User user, LocalDate startDate, LocalDate endDate, Project project, Boolean approved, Pageable pageable);

    // 按项目、日期范围和审批状态查询工时记录
    List<WorkTimeRecord> findByProjectAndDateBetweenAndApproved(Project project, LocalDate startDate, LocalDate endDate, Boolean approved);

    // 按项目、日期范围和审批状态查询工时记录（分页）
    Page<WorkTimeRecord> findByProjectAndDateBetween(Project project, LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<WorkTimeRecord> findByProjectAndDateBetweenAndApproved(Project project, LocalDate startDate, LocalDate endDate, Boolean approved, Pageable pageable);
    
    // 查询某用户在日期范围内参与过的项目ID
    @Query("SELECT DISTINCT w.project.id FROM WorkTimeRecord w WHERE w.user.id = :userId AND w.date BETWEEN :startDate AND :endDate")
    List<Long> findDistinctProjectIdsByUserAndDateRange(@Param("userId") Long userId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
    
    // ========== 新增：数据库端多条件过滤（支持分页与非分页） ==========
    @Query("SELECT w FROM WorkTimeRecord w " +
           "WHERE w.project = :project " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "AND (:approved IS NULL OR w.approved = :approved) " +
           "AND (:userId IS NULL OR w.user.id = :userId) " +
           "AND (:workType IS NULL OR :workType = '' OR w.workType = :workType) " +
           "ORDER BY w.date DESC, w.id DESC")
    Page<WorkTimeRecord> findByProjectAndDateRangeWithFilters(
        @Param("project") Project project,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("approved") Boolean approved,
        @Param("userId") Long userId,
        @Param("workType") String workType,
        Pageable pageable);

    @Query("SELECT w FROM WorkTimeRecord w " +
           "WHERE w.project = :project " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "AND (:approved IS NULL OR w.approved = :approved) " +
           "AND (:userId IS NULL OR w.user.id = :userId) " +
           "AND (:workType IS NULL OR :workType = '' OR w.workType = :workType) " +
           "ORDER BY w.date DESC, w.id DESC")
    List<WorkTimeRecord> findByProjectAndDateRangeWithFilters(
        @Param("project") Project project,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("approved") Boolean approved,
        @Param("userId") Long userId,
        @Param("workType") String workType);

    // Dashboard Service 需要的方法
    
    // 按审批状态统计记录数
    long countByApproved(Boolean approved);
    
    // 按审批状态和日期范围统计记录数
    @Query("SELECT COUNT(w) FROM WorkTimeRecord w WHERE w.approved = :approved AND w.date BETWEEN :startDate AND :endDate")
    long countByApprovedAndDateRange(@Param("approved") Boolean approved, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // 按日期范围统计总工时
    @Query("SELECT SUM(w.hours) FROM WorkTimeRecord w WHERE w.date BETWEEN :startDate AND :endDate")
    Double sumHoursByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // 优化的聚合统计查询
    @Query("SELECT w.project.id, SUM(w.hours) as totalHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.project.id " +
           "ORDER BY totalHours DESC")
    List<Object[]> aggregateProjectStats(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);
    
    // 获取基本统计数据（总工时、记录数、加班工时）
    @Query("SELECT " +
           "SUM(w.hours), " +
           "COUNT(w), " +
           "SUM(CASE WHEN w.hours > 8.0 THEN (w.hours - 8.0) ELSE 0 END) " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate")
    Object[] getBasicStats(@Param("startDate") LocalDate startDate, 
                          @Param("endDate") LocalDate endDate);

    // ========== 批量填写页面性能优化查询方法 ==========
    
    // 批量查询多个项目的工时记录，使用FETCH JOIN避免N+1问题
    @Query("SELECT w FROM WorkTimeRecord w " +
           "LEFT JOIN FETCH w.user " +
           "LEFT JOIN FETCH w.project " +
           "WHERE w.project.id IN :projectIds " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "ORDER BY w.project.id, w.user.id, w.date")
    List<WorkTimeRecord> findByProjectIdsAndDateRangeWithJoins(
        @Param("projectIds") List<Long> projectIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    // 批量查询去重检查，一次性检查多个用户-项目-日期组合
    @Query("SELECT CONCAT(w.user.id, '-', w.project.id, '-', w.date) as key, w " +
           "FROM WorkTimeRecord w " +
           "WHERE CONCAT(w.user.id, '-', w.project.id, '-', w.date) IN :keys")
    List<Object[]> findByUserProjectDateKeys(@Param("keys") List<String> keys);
    
    // 优化的项目工时统计，批量获取多个项目的统计信息
    @Query("SELECT " +
           "w.project.id as projectId, " +
           "w.user.id as userId, " +
           "w.user.username as username, " +
           "w.user.realName as realName, " +
           "SUM(w.hours) as totalHours, " +
           "COUNT(w) as recordCount, " +
           "AVG(w.hours) as avgHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.project.id IN :projectIds " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.project.id, w.user.id, w.user.username, w.user.realName")
    List<Object[]> getProjectUserStatsBatch(@Param("projectIds") List<Long> projectIds,
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    // 批量查询用户在指定项目和日期范围内的工时，使用游标分页
    @Query("SELECT w FROM WorkTimeRecord w " +
           "LEFT JOIN FETCH w.user " +
           "LEFT JOIN FETCH w.project " +
           "WHERE w.id > :lastId " +
           "AND w.user.id IN :userIds " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "ORDER BY w.id ASC")
    List<WorkTimeRecord> findByUserIdsAndDateRangeCursor(
        @Param("userIds") List<Long> userIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("lastId") Long lastId,
        Pageable pageable);
    
    // 高效的存在性检查，避免返回完整对象
    @Query("SELECT COUNT(w) > 0 FROM WorkTimeRecord w " +
           "WHERE w.user.id = :userId " +
           "AND w.project.id = :projectId " +
           "AND w.date = :date")
    boolean existsByUserIdAndProjectIdAndDate(
        @Param("userId") Long userId,
        @Param("projectId") Long projectId,
        @Param("date") LocalDate date);
    
    // 批量存在性检查
    @Query("SELECT CONCAT(w.user.id, '-', w.project.id, '-', w.date) " +
           "FROM WorkTimeRecord w " +
           "WHERE CONCAT(w.user.id, '-', w.project.id, '-', w.date) IN :keys")
    List<String> findExistingKeys(@Param("keys") List<String> keys);
    
    // 针对单个用户的项目维度聚合统计（用于个人视角的项目工时统计）
    @Query("SELECT " +
           "w.project.id as projectId, " +
           "w.project.name as projectName, " +
           "COUNT(w) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.user.id = :userId " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.project.id, w.project.name " +
           "ORDER BY totalHours DESC")
    List<Object[]> getUserProjectAggregatedStats(@Param("userId") Long userId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // 针对单个用户在指定项目内的聚合统计（用于userId+projectId联合过滤时）
    @Query("SELECT " +
           "SUM(w.hours) as totalHours, " +
           "COUNT(w) as totalRecords, " +
           "AVG(w.hours) as avgHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.user.id = :userId " +
           "AND w.project.id = :projectId " +
           "AND w.date BETWEEN :startDate AND :endDate")
    Object[] getUserAggregatedStatsForProject(@Param("userId") Long userId,
                                             @Param("projectId") Long projectId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    
    // 获取用户在指定日期范围内的去重工时记录（避免重复统计）
    @Query("SELECT DISTINCT w FROM WorkTimeRecord w " +
           "WHERE w.user = :user " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "ORDER BY w.date DESC, w.project.id")
    List<WorkTimeRecord> findDistinctByUserAndDateBetween(
        @Param("user") User user, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    // 统计用户在指定日期范围内的总工时（通过子查询避免重复统计同一天同一项目的多条记录）
    @Query(value = "SELECT SUM(maxHours) FROM (" +
           "SELECT date, project_id, MAX(hours) as maxHours " +
           "FROM work_time_record " +
           "WHERE user_id = :userId " +
           "AND date BETWEEN :startDate AND :endDate " +
           "GROUP BY date, project_id" +
           ") AS subquery", nativeQuery = true)
    Double sumHoursByUserAndDateBetweenWithoutDuplicates(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    // ========== 新增高性能查询方法 ==========
    
    // 批量查询多用户统计数据，用于管理员仪表板
    @Query("SELECT " +
           "w.user.id as userId, " +
           "SUM(w.hours) as totalHours, " +
           "COUNT(w) as recordCount, " +
           "AVG(w.hours) as avgHours, " +
           "SUM(CASE WHEN w.hours > 8.0 THEN (w.hours - 8.0) ELSE 0 END) as overtimeHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.user.id IN :userIds " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.user.id")
    List<Object[]> getUserStatsBatch(@Param("userIds") List<Long> userIds,
                                   @Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);
    
    // 高性能分页查询，使用游标避免深度分页性能问题
    @Query("SELECT w FROM WorkTimeRecord w " +
           "LEFT JOIN FETCH w.user " +
           "LEFT JOIN FETCH w.project " +
           "WHERE w.id > :lastId " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "ORDER BY w.id ASC")
    List<WorkTimeRecord> findByDateRangeCursor(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("lastId") Long lastId,
        Pageable pageable);
    
    // 批量获取项目工时统计，一次性获取多个项目的统计信息
    @Query("SELECT " +
           "w.project.id as projectId, " +
           "SUM(w.hours) as totalHours, " +
           "COUNT(DISTINCT w.user.id) as memberCount, " +
           "COUNT(w) as recordCount, " +
           "AVG(w.hours) as avgHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.project.id IN :projectIds " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.project.id")
    List<Object[]> getProjectStatsBatch(@Param("projectIds") List<Long> projectIds,
                                      @Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);
    
    // 工作类型统计查询，支持按工作类型分组统计
    @Query("SELECT w.workType, SUM(w.hours), COUNT(w) " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "AND (:projectId IS NULL OR w.project.id = :projectId) " +
           "AND (:userId IS NULL OR w.user.id = :userId) " +
           "GROUP BY w.workType " +
           "ORDER BY SUM(w.hours) DESC")
    List<Object[]> getWorkTypeStats(@Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("projectId") Long projectId,
                                  @Param("userId") Long userId);
    
    // 时间段统计查询，支持按月、周等时间段分组
    @Query(value = "SELECT " +
           "DATE_FORMAT(date, :dateFormat) as period, " +
           "SUM(hours) as totalHours, " +
           "COUNT(*) as recordCount, " +
           "COUNT(DISTINCT user_id) as uniqueUsers " +
           "FROM work_time_record " +
           "WHERE date BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_FORMAT(date, :dateFormat) " +
           "ORDER BY period", nativeQuery = true)
    List<Object[]> getTimeSeriesStats(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("dateFormat") String dateFormat);
    
    // 获取Top工时用户，支持排行榜功能
    @Query("SELECT " +
           "w.user.id as userId, " +
           "w.user.username as username, " +
           "w.user.realName as realName, " +
           "SUM(w.hours) as totalHours, " +
           "COUNT(w) as recordCount " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.user.id, w.user.username, w.user.realName " +
           "ORDER BY SUM(w.hours) DESC")
    List<Object[]> getTopUsersByHours(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    Pageable pageable);
    
    // 批量检查工时记录完整性，用于数据验证
    @Query(value = "SELECT " +
           "user_id, " +
           "DATE_FORMAT(date, '%Y-%m-%d') as work_date, " +
           "COUNT(*) as record_count, " +
           "SUM(hours) as daily_hours " +
           "FROM work_time_record " +
           "WHERE user_id IN :userIds " +
           "AND date BETWEEN :startDate AND :endDate " +
           "GROUP BY user_id, DATE_FORMAT(date, '%Y-%m-%d') " +
           "HAVING COUNT(*) > 1 OR SUM(hours) > 24", nativeQuery = true)
    List<Object[]> findDataIntegrityIssues(@Param("userIds") List<Long> userIds,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
    
    // 获取工时分布统计，用于工时分布分析
    @Query(value = "SELECT " +
           "CASE " +
           "    WHEN hours <= 4 THEN '0-4小时' " +
           "    WHEN hours <= 8 THEN '4-8小时' " +
           "    WHEN hours <= 12 THEN '8-12小时' " +
           "    ELSE '12小时以上' " +
           "END as hours_range, " +
           "COUNT(*) as count " +
           "FROM work_time_record " +
           "WHERE date BETWEEN :startDate AND :endDate " +
           "GROUP BY " +
           "CASE " +
           "    WHEN hours <= 4 THEN '0-4小时' " +
           "    WHEN hours <= 8 THEN '4-8小时' " +
           "    WHEN hours <= 12 THEN '8-12小时' " +
           "    ELSE '12小时以上' " +
           "END " +
           "ORDER BY count DESC", nativeQuery = true)
    List<Object[]> getHoursDistribution(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    // ========== 统计报表性能优化查询方法（解决N+1查询问题）==========
    
    // 综合统计查询 - 一次性获取所有基础统计数据，避免多次查询
    @Query(value = "SELECT " +
           "COUNT(DISTINCT w.user_id) as totalUsers, " +
           "COUNT(DISTINCT w.project_id) as totalProjects, " +
           "COUNT(*) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "MAX(w.hours) as maxHours, " +
           "MIN(w.hours) as minHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours, " +
           "COUNT(CASE WHEN w.approved = true THEN 1 END) as approvedRecords, " +
           "COUNT(CASE WHEN w.approved = false THEN 1 END) as pendingRecords " +
           "FROM work_time_record w " +
           "WHERE w.date BETWEEN :startDate AND :endDate", nativeQuery = true)
    Object[] getComprehensiveStats(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
    
    // 项目和用户组合统计 - 一次性获取所有项目的用户工时分布
    @Query("SELECT " +
           "w.project.id as projectId, " +
           "w.project.name as projectName, " +
           "w.user.id as userId, " +
           "w.user.realName as userRealName, " +
           "SUM(w.hours) as totalHours, " +
           "COUNT(w) as recordCount, " +
           "AVG(w.hours) as avgHours, " +
           "MAX(w.hours) as maxHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as overtimeHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.project.id, w.project.name, w.user.id, w.user.realName " +
           "ORDER BY w.project.id, totalHours DESC")
    List<Object[]> getProjectUserComboStats(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    // 聚合项目统计查询 - 替代原有的逐个项目查询
    @Query("SELECT " +
           "w.project.id as projectId, " +
           "w.project.name as projectName, " +
           "w.project.status as projectStatus, " +
           "COUNT(DISTINCT w.user.id) as memberCount, " +
           "COUNT(w) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "MAX(w.hours) as maxDailyHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours, " +
           "COUNT(CASE WHEN w.approved = true THEN 1 END) as approvedRecords, " +
           "COUNT(CASE WHEN w.approved = false OR w.approved IS NULL THEN 1 END) as pendingRecords " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.project.id, w.project.name, w.project.status " +
           "ORDER BY totalHours DESC")
    List<Object[]> getAllProjectsAggregatedStats(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
    
    // 聚合用户统计查询 - 替代原有的逐个用户查询 
    @Query("SELECT " +
           "w.user.id as userId, " +
           "w.user.username as username, " +
           "w.user.realName as realName, " +
           "w.user.department as department, " +
           "COUNT(DISTINCT w.project.id) as projectCount, " +
           "COUNT(w) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "MAX(w.hours) as maxDailyHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours, " +
           "COUNT(CASE WHEN w.approved = true THEN 1 END) as approvedRecords, " +
           "COUNT(CASE WHEN w.approved = false OR w.approved IS NULL THEN 1 END) as pendingRecords " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.user.id, w.user.username, w.user.realName, w.user.department " +
           "ORDER BY totalHours DESC")
    List<Object[]> getAllUsersAggregatedStats(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
    
    // 日期维度聚合统计 - 支持时间趋势分析
    @Query(value = "SELECT " +
           "DATE_FORMAT(w.date, '%Y-%m-%d') as workDate, " +
           "COUNT(DISTINCT w.user_id) as activeUsers, " +
           "COUNT(DISTINCT w.project_id) as activeProjects, " +
           "COUNT(*) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours " +
           "FROM work_time_record w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_FORMAT(w.date, '%Y-%m-%d') " +
           "ORDER BY workDate", nativeQuery = true)
    List<Object[]> getDailyAggregatedStats(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
    
    // 部门维度聚合统计 - 支持部门间对比分析
    @Query("SELECT " +
           "w.user.department as department, " +
           "COUNT(DISTINCT w.user.id) as userCount, " +
           "COUNT(DISTINCT w.project.id) as projectCount, " +
           "COUNT(w) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "AND w.user.department IS NOT NULL " +
           "GROUP BY w.user.department " +
           "ORDER BY totalHours DESC")
    List<Object[]> getDepartmentAggregatedStats(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
    
    // 工作类型聚合统计 - 增强版本支持更详细的工作类型分析
    @Query("SELECT " +
           "w.workType as workType, " +
           "COUNT(DISTINCT w.user.id) as userCount, " +
           "COUNT(DISTINCT w.project.id) as projectCount, " +
           "COUNT(w) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "MAX(w.hours) as maxHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "AND w.workType IS NOT NULL " +
           "GROUP BY w.workType " +
           "ORDER BY totalHours DESC")
    List<Object[]> getWorkTypeAggregatedStats(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
    
    // 月度趋势聚合统计 - 支持长期趋势分析
    @Query(value = "SELECT " +
           "DATE_FORMAT(w.date, '%Y-%m') as yearMonth, " +
           "COUNT(DISTINCT w.user_id) as activeUsers, " +
           "COUNT(DISTINCT w.project_id) as activeProjects, " +
           "COUNT(*) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours, " +
           "SUM(CASE WHEN w.hours > 8 THEN (w.hours - 8) ELSE 0 END) as totalOvertimeHours " +
           "FROM work_time_record w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_FORMAT(w.date, '%Y-%m') " +
           "ORDER BY yearMonth", nativeQuery = true)
    List<Object[]> getMonthlyTrendStats(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
    
    // 审批状态聚合统计 - 支持审批工作台统计分析
    @Query("SELECT " +
           "COALESCE(w.approved, false) as approvalStatus, " +
           "COUNT(DISTINCT w.user.id) as userCount, " +
           "COUNT(DISTINCT w.project.id) as projectCount, " +
           "COUNT(w) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY COALESCE(w.approved, false) " +
           "ORDER BY approvalStatus DESC")
    List<Object[]> getApprovalStatusAggregatedStats(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
    
    // 项目状态聚合统计 - 支持项目状态分析
    @Query("SELECT " +
           "w.project.status as projectStatus, " +
           "COUNT(DISTINCT w.project.id) as projectCount, " +
           "COUNT(DISTINCT w.user.id) as userCount, " +
           "COUNT(w) as totalRecords, " +
           "SUM(w.hours) as totalHours, " +
           "AVG(w.hours) as avgHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "AND w.project.status IS NOT NULL " +
           "GROUP BY w.project.status " +
           "ORDER BY totalHours DESC")
    List<Object[]> getProjectStatusAggregatedStats(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
