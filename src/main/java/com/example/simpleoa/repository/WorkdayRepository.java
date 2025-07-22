package com.example.simpleoa.repository;

import com.example.simpleoa.model.Workday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 工作日数据访问接口
 */
public interface WorkdayRepository extends JpaRepository<Workday, Long> {
    
    /**
     * 根据日期查找工作日
     */
    Optional<Workday> findByDate(Date date);
    
    /**
     * 根据日期范围查询工作日，按日期排序
     */
    List<Workday> findByDateBetweenOrderByDate(Date startDate, Date endDate);
    
    /**
     * 按日期排序获取所有工作日
     */
    List<Workday> findAllByOrderByDateDesc();
    
    /**
     * 根据年份和月份查询工作日
     */
    @Query("SELECT w FROM Workday w WHERE YEAR(w.date) = :year AND MONTH(w.date) = :month ORDER BY w.date")
    List<Workday> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
    
    /**
     * 根据工作日类型查询工作日
     */
    List<Workday> findByWorkTypeOrderByDate(String workType);
    
    /**
     * 检查指定年月是否已存在工作日
     */
    @Query("SELECT COUNT(w) > 0 FROM Workday w WHERE YEAR(w.date) = :year AND MONTH(w.date) = :month")
    boolean existsByYearAndMonth(@Param("year") int year, @Param("month") int month);
    
    /**
     * 检查指定日期是否已存在工作日记录
     */
    boolean existsByDate(Date date);
    
    /**
     * 根据年份查询工作日
     */
    @Query("SELECT w FROM Workday w WHERE YEAR(w.date) = :year ORDER BY w.date")
    List<Workday> findByYear(@Param("year") int year);
}
