package com.example.simpleoa.repository;

import com.example.simpleoa.model.LeaveRequest;
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
    List<LeaveRequest> findByApplicantIdAndLeaveType(Long applicantId, String leaveType);
    
    // 按申请人和状态查询
    List<LeaveRequest> findByApplicantIdAndStatus(Long applicantId, String status);
}
