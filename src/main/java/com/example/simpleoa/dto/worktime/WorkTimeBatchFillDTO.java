package com.example.simpleoa.dto.worktime;

import lombok.Data;
import java.util.List;
import java.util.Set;

/**
 * 批量填报考勤数据DTO
 * 一次性返回所有项目和成员信息，以及扁平化的工时数据
 */
@Data
public class WorkTimeBatchFillDTO {

    /**
     * 项目列表（包含成员信息）
     */
    private List<ProjectMemberDTO> projects;

    /**
     * 工时数据（扁平化存储，非嵌套结构）
     */
    private List<WorkTimeRecordDTO> workTimeData;

    /**
     * 工作日数据集合
     */
    private Set<String> workdays;

    /**
     * 项目数量
     */
    private int totalProjects;

    /**
     * 成员总数
     */
    private int totalMembers;

    /**
     * 日期范围天数
     */
    private int dateRangeDays;
}
