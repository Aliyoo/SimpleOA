package com.example.simpleoa.dto.worktime;

import lombok.Data;

/**
 * 工时记录DTO
 * 扁平化存储结构，便于前端建立索引
 */
@Data
public class WorkTimeRecordDTO {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作日期 (YYYY-MM-DD格式)
     */
    private String date;

    /**
     * 工时（0-8小时）
     */
    private Double hours;

    /**
     * 是否已审批
     */
    private Boolean approved;
}
