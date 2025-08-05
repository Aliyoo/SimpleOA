package com.example.simpleoa.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class BudgetSearchDTO {
    // 搜索关键词（预算名称）
    private String keyword;
    
    // 项目ID
    private Long projectId;
    
    // 预算状态
    private String status;
    
    // 开始日期范围
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDateFrom;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDateTo;
    
    // 结束日期范围
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDateFrom;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDateTo;
    
    // 预算金额范围
    private Double totalAmountFrom;
    private Double totalAmountTo;
    
    // 已使用金额范围
    private Double usedAmountFrom;
    private Double usedAmountTo;
    
    // 分页参数
    private Integer page = 0;
    private Integer size = 10;
    
    // 排序参数
    private String sortBy = "createTime";
    private String sortDirection = "desc";
}