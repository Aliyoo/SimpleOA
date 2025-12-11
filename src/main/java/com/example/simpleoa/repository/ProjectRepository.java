package com.example.simpleoa.repository;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    // 按项目名称查询
    List<Project> findByName(String name);

    // 按项目状态查询
    List<Project> findByStatus(String status);

    // 按项目类型查询
    List<Project> findByType(String type);

    // 按项目优先级查询
    List<Project> findByPriority(String priority);

    //// 按创建日期范围查询
    //List<Project> findByCreateTimeBetween(Date startDate, Date endDate);
    //
    //// 按截止日期范围查询
    //List<Project> findByDeadlineBetween(Date startDate, Date endDate);
    //
    //// 查询即将到期的项目（截止日期在未来7天内）
    //@Query("SELECT p FROM Project p WHERE p.deadline BETWEEN CURRENT_DATE AND CURRENT_DATE + 7")
    //List<Project> findUpcomingDeadlineProjects();

    // 统计不同状态的项目数量
    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> countProjectsByStatus();

    // 查询用户参与的项目
    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.id = :userId")
    List<Project> findByUserId(@Param("userId") Long userId);

    // 查询项目经理管理的项目
    @Query("SELECT p FROM Project p WHERE p.manager.id = :managerId")
    List<Project> findByManagerId(@Param("managerId") Long managerId);

    // Dashboard Service 需要的方法

    // 按项目状态统计数量
    long countByStatus(ProjectStatus status);

    // ========== 批量查询优化 - 用于批量填报考勤页面 ==========

    // 查询所有活跃项目
    @Query("SELECT p FROM Project p WHERE p.status = 'ACTIVE'")
    List<Project> findAllActiveProjects();

    // 查询项目经理管理的活跃项目
    @Query("SELECT p FROM Project p WHERE p.manager.id = :managerId AND p.status = 'ACTIVE'")
    List<Project> findActiveProjectsByManagerId(@Param("managerId") Long managerId);

    // ========== 关键优化：一次性查询所有项目成员 ==========
    // 使用原生SQL查询多个项目的所有成员，避免N+1查询问题
    @Query(value = """
        SELECT
            pm.project_id as projectId,
            u.id as userId,
            u.username as username,
            u.real_name as realName,
            u.role as role,
            u.email as email
        FROM project_members pm
        JOIN user u ON pm.user_id = u.id
        WHERE pm.project_id IN (:projectIds)
        ORDER BY pm.project_id, u.username
        """, nativeQuery = true)
    List<Object[]> findProjectMembersBatch(@Param("projectIds") List<Long> projectIds);
}
