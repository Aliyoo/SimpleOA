package com.example.simpleoa.repository;

import com.example.simpleoa.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 节假日数据访问接口
 */
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * 根据日期查找节假日
     */
    Optional<Holiday> findByDate(LocalDate date);

    /**
     * 根据日期范围查询节假日，按日期排序
     */
    List<Holiday> findByDateBetweenOrderByDate(LocalDate startDate, LocalDate endDate);

    /**
     * 按日期排序获取所有节假日
     */
    List<Holiday> findAllByOrderByDateDesc();

    /**
     * 根据年份和月份查询节假日
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.date) = :year AND MONTH(h.date) = :month ORDER BY h.date")
    List<Holiday> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 根据节假日类型查询节假日
     */
    List<Holiday> findByTypeOrderByDate(String type);

    /**
     * 检查指定日期是否是节假日
     */
    boolean existsByDate(LocalDate date);

    /**
     * 根据年份查询节假日
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.date) = :year ORDER BY h.date")
    List<Holiday> findByYear(@Param("year") int year);

    /**
     * 根据名称查找节假日
     */
    List<Holiday> findByNameContainingIgnoreCase(String name);
}
