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
    
    // 查找用户某年某类型的请假余额 - 使用 User ID
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.user.id = :userId AND lb.year = :year AND lb.leaveType = :leaveType")
    Optional<LeaveBalance> findByUserIdAndYearAndLeaveType(@Param("userId") Long userId, @Param("year") Integer year, @Param("leaveType") LeaveType leaveType);
    
    // 查找用户某年某类型的请假余额 - 使用 User 对象
    Optional<LeaveBalance> findByUserAndLeaveTypeAndYear(User user, LeaveType leaveType, Integer year);
    
    // 查找用户某年的所有请假余额 - 使用 User ID
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.user.id = :userId AND lb.year = :year")
    List<LeaveBalance> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);
    
    // 查找用户某年的所有请假余额 - 使用 User 对象
    List<LeaveBalance> findByUserAndYear(User user, Integer year);
    
    // 查找用户的所有请假余额 - 使用 User ID
    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.user.id = :userId")
    List<LeaveBalance> findByUserId(@Param("userId") Long userId);
    
    // 查找某年的所有请假余额
    List<LeaveBalance> findByYear(Integer year);
    
    // 查询用户某年某类型的剩余天数
    @Query("SELECT lb.remainingDays FROM LeaveBalance lb WHERE lb.user.id = :userId AND lb.year = :year AND lb.leaveType = :leaveType")
    Integer getRemainingDays(@Param("userId") Long userId, @Param("year") Integer year, @Param("leaveType") LeaveType leaveType);
    
    // 检查是否存在用户某年某类型的请假余额
    boolean existsByUserAndLeaveTypeAndYear(User user, LeaveType leaveType, Integer year);
}
