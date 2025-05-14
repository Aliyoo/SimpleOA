package com.example.simpleoa.repository;

import com.example.simpleoa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    // 按用户名查询
    User findByUsername(String username);
    
    // 按用户名模糊查询
    List<User> findByUsernameLike(String username);

    // 排除某些用户名查询
    List<User> findByUsernameNotIn(List<String> usernames);
    
    // 按邮箱查询
    User findByEmail(String email);
    
    // 按部门查询
    List<User> findByDepartment(String department);
    
    //// 按职位查询
    //List<User> findByPosition(String position);
    
    //// 按状态查询
    //List<User> findByStatus(String status);
    
    //// 按创建日期范围查询
    //List<User> findByCreateTimeBetween(Date startDate, Date endDate);
    //
    //// 按最后登录时间范围查询
    //List<User> findByLastLoginTimeBetween(Date startDate, Date endDate);
    
    // 查询特定角色的用户
    //@Query("SELECT u FROM User u JOIN u.roles r WHERE r.id = :roleId")
    //List<User> findByRoleId(@Param("roleId") Long roleId);
    
    // 查询特定项目的成员
    //@Query("SELECT u FROM User u JOIN u.projects p WHERE p.id = :projectId")
    //List<User> findByProjectId(@Param("projectId") Long projectId);
    
    // 检查用户名是否存在
    boolean existsByUsername(String username);
    
    // 检查邮箱是否存在
    boolean existsByEmail(String email);
    
    // 统计不同部门的用户数量
    @Query("SELECT u.department, COUNT(u) FROM User u GROUP BY u.department")
    List<Object[]> countUsersByDepartment();
    
    // 查询最近注册的用户
    //@Query("SELECT u FROM User u ORDER BY u.createTime DESC")
    //List<User> findRecentUsers(org.springframework.data.domain.Pageable pageable);
}