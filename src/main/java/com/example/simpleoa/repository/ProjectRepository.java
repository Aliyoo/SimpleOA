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
}
