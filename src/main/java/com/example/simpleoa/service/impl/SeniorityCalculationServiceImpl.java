package com.example.simpleoa.service.impl;

import com.example.simpleoa.dto.leave.SeniorityInfo;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.SeniorityCalculationService;
import com.example.simpleoa.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 工龄计算服务实现类
 * 基于员工入职日期精确计算工龄，支持小数年数
 */
@Service
public class SeniorityCalculationServiceImpl implements SeniorityCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(SeniorityCalculationServiceImpl.class);

    @Autowired
    private UserService userService;

    @Override
    @Cacheable(value = "seniority", key = "#userId + ':' + #calculateDate")
    public SeniorityInfo calculateSeniority(Long userId, LocalDate calculateDate) {
        logger.debug("计算用户ID {} 在日期 {} 的工龄", userId, calculateDate);

        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在，ID：" + userId);
        }

        if (user.getHireDate() == null) {
            throw new IllegalArgumentException("用户入职日期为空，ID：" + userId);
        }

        LocalDate hireDate = user.getHireDate().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();

        // 确保计算日期不早于入职日期
        if (calculateDate.isBefore(hireDate)) {
            logger.warn("计算日期 {} 早于用户 {} 的入职日期 {}", calculateDate, userId, hireDate);
            calculateDate = hireDate;
        }

        return calculateSeniorityBetweenDates(hireDate, calculateDate);
    }

    @Override
    public SeniorityInfo calculateCurrentSeniority(Long userId) {
        return calculateSeniority(userId, LocalDate.now());
    }

    @Override
    public List<SeniorityInfo> batchCalculateSeniority(List<Long> userIds, LocalDate calculateDate) {
        logger.info("批量计算 {} 个用户在 {} 的工龄", userIds.size(), calculateDate);

        List<SeniorityInfo> results = new ArrayList<>();
        List<Long> successIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                SeniorityInfo seniorityInfo = calculateSeniority(userId, calculateDate);
                results.add(seniorityInfo);
                successIds.add(userId);
            } catch (Exception e) {
                logger.error("计算用户ID {} 工龄失败: {}", userId, e.getMessage());
                errors.add(String.format("用户ID %d: %s", userId, e.getMessage()));
            }
        }

        logger.info("批量工龄计算完成，成功：{}，失败：{}", successIds.size(), errors.size());

        // 如果有失败的，可以记录错误或抛出异常
        if (!errors.isEmpty()) {
            logger.warn("部分用户工龄计算失败: {}", String.join("; ", errors));
        }

        return results;
    }

    @Override
    public SeniorityInfo calculateYearStartSeniority(Long userId, Integer year) {
        LocalDate yearStartDate = LocalDate.of(year, 1, 1);
        return calculateSeniority(userId, yearStartDate);
    }

    @Override
    @Cacheable(value = "hire_date", key = "#userId")
    public boolean hasValidHireDate(Long userId) {
        try {
            LocalDate hireDate = getHireDate(userId);
            return hireDate != null && !hireDate.isAfter(LocalDate.now());
        } catch (Exception e) {
            logger.warn("检查用户ID {} 入职日期时发生错误: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public LocalDate getHireDate(Long userId) {
        User user = userService.getUserById(userId);
        if (user == null || user.getHireDate() == null) {
            return null;
        }

        return user.getHireDate().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Override
    public SeniorityInfo calculateSeniorityBetweenDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // 确保结束日期不早于开始日期
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        Period period = Period.between(startDate, endDate);

        // 计算精确到月的工龄年数
        double years = period.getYears();
        double months = period.getMonths();
        double seniorityYears = years + (months / 12.0);

        return SeniorityInfo.builder()
            .seniorityYears(seniorityYears)
            .years(period.getYears())
            .months(period.getMonths())
            .days(period.getDays())
            .hireDate(startDate)
            .calculateDate(endDate)
            .build();
    }

    @Override
    public String getSeniorityLevel(Long userId, LocalDate calculateDate) {
        SeniorityInfo seniorityInfo = calculateSeniority(userId, calculateDate);

        if (seniorityInfo.getSeniorityYears() == null) {
            return "未知";
        }

        double years = seniorityInfo.getSeniorityYears();

        if (years < 1.0) {
            return "试用期";
        } else if (years < 3.0) {
            return "初级";
        } else if (years < 5.0) {
            return "中级";
        } else if (years < 10.0) {
            return "高级";
        } else if (years < 15.0) {
            return "资深";
        } else {
            return "专家";
        }
    }

    @Override
    public boolean isProbationEmployee(Long userId, LocalDate calculateDate) {
        try {
            SeniorityInfo seniorityInfo = calculateSeniority(userId, calculateDate);
            return seniorityInfo.isProbation();
        } catch (Exception e) {
            logger.warn("检查用户ID {} 试用期状态时发生错误: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isFormalEmployee(Long userId, LocalDate calculateDate) {
        try {
            SeniorityInfo seniorityInfo = calculateSeniority(userId, calculateDate);
            return seniorityInfo.isFormal();
        } catch (Exception e) {
            logger.warn("检查用户ID {} 正式员工状态时发生错误: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 清除指定用户的工龄缓存
     * @param userId 用户ID
     */
    @CacheEvict(value = "seniority", key = "#userId + ':*'")
    public void evictSeniorityCache(Long userId) {
        logger.debug("清除用户ID {} 的工龄缓存", userId);
    }

    /**
     * 清除所有工龄缓存
     */
    @CacheEvict(value = "seniority", allEntries = true)
    public void evictAllSeniorityCache() {
        logger.info("清除所有工龄缓存");
    }
}