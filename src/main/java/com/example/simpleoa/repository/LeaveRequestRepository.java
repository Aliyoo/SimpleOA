package com.example.simpleoa.repository;

import com.example.simpleoa.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    @Query("SELECT l FROM LeaveRequest l WHERE l.applicant.id = :userId")
    List<LeaveRequest> findByUserId(@Param("userId") Long userId);
    
    // 按状态查询
    List<LeaveRequest> findByStatus(String status);
    
    //// 按请假类型查询
    //List<LeaveRequest> findByType(String type);
    
    // 按请假开始日期范围查询
    List<LeaveRequest> findByStartDateBetween(Date startDate, Date endDate);
    
    // 按请假结束日期范围查询
    List<LeaveRequest> findByEndDateBetween(Date startDate, Date endDate);
    
    // 查询当前进行中的请假
    @Query("SELECT l FROM LeaveRequest l WHERE :currentDate BETWEEN l.startDate AND l.endDate")
    List<LeaveRequest> findCurrentLeaves(@Param("currentDate") Date currentDate);
    
    // 按申请日期范围查询
    //List<LeaveRequest> findByApplyDateBetween(Date startDate, Date endDate);
    
    // 统计用户的请假天数
    @Query("SELECT SUM(DATEDIFF(l.endDate, l.startDate) + 1) FROM LeaveRequest l WHERE l.applicant.id = :userId AND l.status = 'APPROVED'")
    Integer sumLeaveDaysByUserId(@Param("userId") Long userId);
    
    // 按请假类型统计用户的请假天数
    //@Query("SELECT SUM(DATEDIFF(l.endDate, l.startDate) + 1) FROM LeaveRequest l WHERE l.applicant.id = :userId AND l.type = :type AND l.status = 'APPROVED'")
    //Integer sumLeaveDaysByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);
    
    // 统计部门的请假申请数量
    @Query("SELECT l.applicant.department, COUNT(l) FROM LeaveRequest l GROUP BY l.applicant.department")
    List<Object[]> countLeavesByDepartment();
    
    // 查询即将开始的请假（未来7天内）
    @Query("SELECT l FROM LeaveRequest l WHERE l.startDate BETWEEN CURRENT_DATE AND CURRENT_DATE + 7 AND l.status = 'APPROVED'")
    List<LeaveRequest> findUpcomingLeaves();
}