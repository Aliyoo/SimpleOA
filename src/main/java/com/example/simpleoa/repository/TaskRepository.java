package com.example.simpleoa.repository;

import com.example.simpleoa.model.Task;
import com.example.simpleoa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    
    // 根据指派用户查找任务
    List<Task> findByAssignee(User assignee);
    
    // 统计指派给特定用户的任务数量
    long countByAssignee(User assignee);
    
    // 统计指派给特定用户且状态不为已完成的任务数量
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee = :assignee AND t.status != 'DONE'")
    long countPendingTasksByAssignee(@Param("assignee") User assignee);
}
