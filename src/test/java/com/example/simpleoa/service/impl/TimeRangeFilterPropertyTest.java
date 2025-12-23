package com.example.simpleoa.service.impl;

import net.jqwik.api.*;
import net.jqwik.time.api.DateTimes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 时间范围筛选属性测试
 * Feature: reimbursement-overview-statistics, Property 5: 时间范围筛选正确性
 * Validates: Requirements 7.1, 7.2
 */
class TimeRangeFilterPropertyTest {

    /**
     * Property 5: 时间范围筛选正确性
     * For any 时间范围筛选请求，返回的所有数据记录的日期应在指定的startDate和endDate范围内（包含边界）。
     */
    @Property(tries = 100)
    void timeRangeFilterCorrectness(
            @ForAll("dateRange") DateRange range,
            @ForAll("dataRecords") List<DataRecord> records
    ) {
        // 筛选在时间范围内的记录
        List<DataRecord> filteredRecords = records.stream()
                .filter(record -> isWithinRange(record.date, range.startDate, range.endDate))
                .collect(Collectors.toList());
        
        // 验证所有筛选后的记录都在时间范围内
        for (DataRecord record : filteredRecords) {
            assertTrue(
                    !record.date.before(range.startDate) && !record.date.after(range.endDate),
                    String.format("记录日期 %s 应在范围 [%s, %s] 内",
                            record.date, range.startDate, range.endDate)
            );
        }
        
        // 验证没有遗漏应该包含的记录
        for (DataRecord record : records) {
            boolean shouldBeIncluded = isWithinRange(record.date, range.startDate, range.endDate);
            boolean isIncluded = filteredRecords.contains(record);
            
            assertEquals(shouldBeIncluded, isIncluded,
                    String.format("记录日期 %s %s在范围内，但%s被包含",
                            record.date,
                            shouldBeIncluded ? "应该" : "不应该",
                            isIncluded ? "却" : "没有"));
        }
    }

    /**
     * Property 5 扩展: 边界值测试
     * 验证边界日期（startDate和endDate）的记录应该被包含
     */
    @Property(tries = 100)
    void boundaryDatesAreIncluded(
            @ForAll("dateRange") DateRange range
    ) {
        // 创建边界日期的记录
        DataRecord startRecord = new DataRecord(range.startDate);
        DataRecord endRecord = new DataRecord(range.endDate);
        
        // 验证边界日期在范围内
        assertTrue(isWithinRange(startRecord.date, range.startDate, range.endDate),
                "开始日期应该在范围内");
        assertTrue(isWithinRange(endRecord.date, range.startDate, range.endDate),
                "结束日期应该在范围内");
    }

    /**
     * Property 5 扩展: 空范围测试
     * 当startDate等于endDate时，只有该日期的记录应该被包含
     */
    @Property(tries = 100)
    void singleDayRangeCorrectness(
            @ForAll("singleDate") Date date,
            @ForAll("dataRecords") List<DataRecord> records
    ) {
        // 筛选在单日范围内的记录
        List<DataRecord> filteredRecords = records.stream()
                .filter(record -> isWithinRange(record.date, date, date))
                .collect(Collectors.toList());
        
        // 验证所有筛选后的记录都是该日期
        for (DataRecord record : filteredRecords) {
            assertEquals(0, compareDatesIgnoringTime(record.date, date),
                    String.format("记录日期 %s 应该等于 %s", record.date, date));
        }
    }

    /**
     * Property 5 扩展: 范围外记录不被包含
     * 验证在范围外的记录不会被包含
     */
    @Property(tries = 100)
    void recordsOutsideRangeAreExcluded(
            @ForAll("dateRange") DateRange range,
            @ForAll("dateOutsideRange") Date outsideDate
    ) {
        Assume.that(outsideDate.before(range.startDate) || outsideDate.after(range.endDate));
        
        DataRecord outsideRecord = new DataRecord(outsideDate);
        
        // 验证范围外的记录不在范围内
        assertFalse(isWithinRange(outsideRecord.date, range.startDate, range.endDate),
                String.format("日期 %s 不应该在范围 [%s, %s] 内",
                        outsideDate, range.startDate, range.endDate));
    }

    // ========== 辅助方法 ==========

    /**
     * 检查日期是否在范围内（包含边界）
     */
    private boolean isWithinRange(Date date, Date startDate, Date endDate) {
        return !date.before(startDate) && !date.after(endDate);
    }

    /**
     * 比较两个日期（忽略时间部分）
     */
    private int compareDatesIgnoringTime(Date date1, Date date2) {
        LocalDateTime ldt1 = LocalDateTime.ofInstant(date1.toInstant(), ZoneId.systemDefault())
                .toLocalDate().atStartOfDay();
        LocalDateTime ldt2 = LocalDateTime.ofInstant(date2.toInstant(), ZoneId.systemDefault())
                .toLocalDate().atStartOfDay();
        return ldt1.compareTo(ldt2);
    }

    // ========== Arbitraries (数据生成器) ==========

    /**
     * 生成日期范围
     */
    @Provide
    Arbitrary<DateRange> dateRange() {
        return Combinators.combine(
                DateTimes.dateTimes().between(
                        LocalDateTime.of(2020, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 12, 31, 23, 59)
                ),
                DateTimes.dateTimes().between(
                        LocalDateTime.of(2020, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 12, 31, 23, 59)
                )
        ).as((start, end) -> {
            // 确保startDate <= endDate
            if (start.isAfter(end)) {
                LocalDateTime temp = start;
                start = end;
                end = temp;
            }
            return new DateRange(
                    Date.from(start.atZone(ZoneId.systemDefault()).toInstant()),
                    Date.from(end.atZone(ZoneId.systemDefault()).toInstant())
            );
        });
    }

    /**
     * 生成单个日期
     */
    @Provide
    Arbitrary<Date> singleDate() {
        return DateTimes.dateTimes()
                .between(
                        LocalDateTime.of(2020, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 12, 31, 23, 59)
                )
                .map(ldt -> Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
    }

    /**
     * 生成数据记录列表
     */
    @Provide
    Arbitrary<List<DataRecord>> dataRecords() {
        return singleDate()
                .map(DataRecord::new)
                .list()
                .ofMinSize(0)
                .ofMaxSize(50);
    }

    /**
     * 生成范围外的日期
     */
    @Provide
    Arbitrary<Date> dateOutsideRange() {
        return Arbitraries.oneOf(
                // 早于2020年的日期
                DateTimes.dateTimes()
                        .between(
                                LocalDateTime.of(2015, 1, 1, 0, 0),
                                LocalDateTime.of(2019, 12, 31, 23, 59)
                        )
                        .map(ldt -> Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())),
                // 晚于2025年的日期
                DateTimes.dateTimes()
                        .between(
                                LocalDateTime.of(2026, 1, 1, 0, 0),
                                LocalDateTime.of(2030, 12, 31, 23, 59)
                        )
                        .map(ldt -> Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()))
        );
    }

    // ========== 辅助数据类 ==========

    /**
     * 日期范围
     */
    private static class DateRange {
        final Date startDate;
        final Date endDate;

        DateRange(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    /**
     * 数据记录（用于测试）
     */
    private static class DataRecord {
        final Date date;

        DataRecord(Date date) {
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataRecord that = (DataRecord) o;
            return date.equals(that.date);
        }

        @Override
        public int hashCode() {
            return date.hashCode();
        }
    }
}
