package com.example.simpleoa.repository;

import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.LeaveStatus;
import com.example.simpleoa.model.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByApplicantId(Long applicantId);
    
    // 按申请人和日期范围查询
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.applicant.id = :applicantId AND lr.startDate >= :startDate AND lr.endDate <= :endDate")
    List<LeaveRequest> findByApplicantIdAndDateRange(@Param("applicantId") Long applicantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 按申请人和请假类型查询
    List<LeaveRequest> findByApplicantIdAndLeaveType(Long applicantId, LeaveType leaveType);
    
    // 按申请人和状态查询
    List<LeaveRequest> findByApplicantIdAndStatus(Long applicantId, LeaveStatus status);
    
    // 分页查询申请人的请假记录
    Page<LeaveRequest> findByApplicantId(Long applicantId, Pageable pageable);
    
    // 查询待审批的请假申请
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status IN (:statuses)")
    List<LeaveRequest> findByStatusIn(@Param("statuses") List<LeaveStatus> statuses);
    
    // 查询某段时间内的请假记录
    @Query("SELECT lr FROM LeaveRequest lr WHERE " +
           "(lr.startDate <= :endDate AND lr.endDate >= :startDate) " +
           "AND lr.status = :status")
    List<LeaveRequest> findOverlappingLeaves(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           @Param("status") LeaveStatus status);
    
    // 统计某年某类型的请假天数
    @Query("SELECT SUM(lr.leaveDays) FROM LeaveRequest lr WHERE lr.applicant.id = :userId " +
           "AND YEAR(lr.startDate) = :year AND lr.leaveType = :leaveType AND lr.status = :status")
    Double sumLeaveDaysByUserAndYearAndType(@Param("userId") Long userId, 
                                          @Param("year") Integer year, 
                                          @Param("leaveType") LeaveType leaveType,
                                          @Param("status") LeaveStatus status);
    
    // 按状态查询
    List<LeaveRequest> findByStatus(LeaveStatus status);
    
    // 按请假类型查询
    List<LeaveRequest> findByLeaveType(LeaveType leaveType);
    
    // 按申请人和年份查询
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.applicant.id = :userId AND YEAR(lr.startDate) = :year")
    List<LeaveRequest> findByApplicantIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);
}
