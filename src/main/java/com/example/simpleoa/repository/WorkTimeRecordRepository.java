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
           "SUM(w.hours) as totalHours, " +
           "COUNT(w) as recordCount, " +
           "AVG(w.hours) as avgHours " +
           "FROM WorkTimeRecord w " +
           "WHERE w.project.id IN :projectIds " +
           "AND w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.project.id, w.user.id")
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
}
