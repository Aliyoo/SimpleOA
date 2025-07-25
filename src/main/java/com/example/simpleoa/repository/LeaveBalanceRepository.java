package com.example.simpleoa.repository;

import com.example.simpleoa.model.LeaveBalance;
import com.example.simpleoa.model.LeaveType;
import com.example.simpleoa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    
    // 查找用户某年某类型的请假余额
    Optional<LeaveBalance> findByUserIdAndYearAndLeaveType(Long userId, Integer year, LeaveType leaveType);
    
    // 查找用户某年某类型的请假余额 - 使用 User 对象
    Optional<LeaveBalance> findByUserAndLeaveTypeAndYear(User user, LeaveType leaveType, Integer year);
    
    // 查找用户某年的所有请假余额
    List<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);
    
    // 查找用户某年的所有请假余额 - 使用 User 对象
    List<LeaveBalance> findByUserAndYear(User user, Integer year);
    
    // 查找用户的所有请假余额
    List<LeaveBalance> findByUserId(Long userId);
    
    // 查询用户某年某类型的剩余天数
    @Query("SELECT lb.remainingDays FROM LeaveBalance lb WHERE lb.user.id = :userId AND lb.year = :year AND lb.leaveType = :leaveType")
    Integer getRemainingDays(@Param("userId") Long userId, @Param("year") Integer year, @Param("leaveType") LeaveType leaveType);
    
    // 检查是否存在用户某年某类型的请假余额
    boolean existsByUserAndLeaveTypeAndYear(User user, LeaveType leaveType, Integer year);
}
