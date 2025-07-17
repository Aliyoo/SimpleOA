package com.example.simpleoa.repository;

import com.example.simpleoa.model.BusinessTripRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface BusinessTripRequestRepository extends JpaRepository<BusinessTripRequest, Long> {
    @Query("SELECT b FROM BusinessTripRequest b WHERE b.applicant.id = :userId")
    List<BusinessTripRequest> findByUserId(@Param("userId") Long userId);
    
    // 按状态查询
    List<BusinessTripRequest> findByStatus(String status);
    
    // 按出差开始日期范围查询
    List<BusinessTripRequest> findByStartDateBetween(Date startDate, Date endDate);
    
    // 按出差结束日期范围查询
    List<BusinessTripRequest> findByEndDateBetween(Date startDate, Date endDate);
    
    // 查询当前进行中的出差
    @Query("SELECT b FROM BusinessTripRequest b WHERE :currentDate BETWEEN b.startDate AND b.endDate")
    List<BusinessTripRequest> findCurrentBusinessTrips(@Param("currentDate") Date currentDate);
    
    // 按目的地查询
    List<BusinessTripRequest> findByDestination(String destination);
    
    // 按目的地模糊查询
    List<BusinessTripRequest> findByDestinationContaining(String destination);
    
    // 按申请日期范围查询
    //List<BusinessTripRequest> findByApplyDateBetween(Date startDate, Date endDate);
    
    // 统计用户的出差天数
    @Query("SELECT SUM(DATEDIFF(b.endDate, b.startDate) + 1) FROM BusinessTripRequest b WHERE b.applicant.id = :userId AND b.status = 'APPROVED'")
    Integer sumBusinessTripDaysByUserId(@Param("userId") Long userId);
    
    // 统计部门的出差申请数量
    @Query("SELECT b.applicant.department, COUNT(b) FROM BusinessTripRequest b GROUP BY b.applicant.department")
    List<Object[]> countBusinessTripsByDepartment();
    
    // 查询即将开始的出差（未来7天内）
    @Query("SELECT b FROM BusinessTripRequest b WHERE b.startDate BETWEEN CURRENT_DATE AND CURRENT_DATE + 7 AND b.status = 'APPROVED'")
    List<BusinessTripRequest> findUpcomingBusinessTrips();
    
    // 按申请人查询
    List<BusinessTripRequest> findByApplicantId(Long applicantId);
    
    // 按申请人和日期范围查询
    @Query("SELECT b FROM BusinessTripRequest b WHERE b.applicant.id = :applicantId AND b.startTime >= :startDate AND b.startTime <= :endDate")
    List<BusinessTripRequest> findByApplicantIdAndDateRange(@Param("applicantId") Long applicantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 按申请人和目的地查询
    List<BusinessTripRequest> findByApplicantIdAndDestination(Long applicantId, String destination);
    
    // 按申请人和状态查询
    List<BusinessTripRequest> findByApplicantIdAndStatus(Long applicantId, String status);
}