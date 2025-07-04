package com.example.simpleoa.service;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import com.example.simpleoa.repository.WorkTimeRecordRepository;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class WorkTimeService {
    private final WorkTimeRecordRepository workTimeRecordRepository;

    @Autowired
    public WorkTimeService(WorkTimeRecordRepository workTimeRecordRepository) {
        this.workTimeRecordRepository = workTimeRecordRepository;
    }

    @Autowired
    @org.springframework.context.annotation.Lazy
    private ApprovalFlowService approvalFlowService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private ProjectService projectService;

    public WorkTimeRecord submitWorkTime(WorkTimeRecord workTimeRecord) {
        // 保存工时记录
        WorkTimeRecord savedRecord = workTimeRecordRepository.save(workTimeRecord);

        try {
            // 获取项目经理作为审批人
            if (savedRecord.getProject() != null && savedRecord.getProject().getId() != null) {
                Project project = projectService.getProjectById(savedRecord.getProject().getId());
                if (project != null && project.getManager() != null) {
                    // 创建审批流程
                    approvalFlowService.createApprovalFlow(savedRecord, project.getManager());
                }
            }
        } catch (Exception e) {
            // 如果创建审批流程失败，记录错误但不影响工时记录的保存
            System.err.println("创建审批流程失败: " + e.getMessage());
        }

        return savedRecord;
    }

    public List<WorkTimeRecord> submitBatchWorkTime(List<WorkTimeRecord> workTimeRecords) {
        List<WorkTimeRecord> savedRecords = new ArrayList<>();
        Map<String, WorkTimeRecord> existingRecordsMap = new HashMap<>();

        // 首先收集所有需要检查的用户-项目-日期组合
        for (WorkTimeRecord record : workTimeRecords) {
            if (record.getUser() != null && record.getProject() != null && record.getDate() != null) {
                // 查找是否已存在相同用户、项目和日期的记录
                List<WorkTimeRecord> existingRecords = workTimeRecordRepository.findByUserAndProjectAndDate(
                        record.getUser(), record.getProject(), record.getDate());

                // 如果存在记录，保存到映射中以便后续更新
                if (!existingRecords.isEmpty()) {
                    String key = record.getUser().getId() + "-" + record.getProject().getId() + "-" + record.getDate();
                    existingRecordsMap.put(key, existingRecords.get(0));
                }
            }
        }

        // 验证工时记录
        for (WorkTimeRecord record : workTimeRecords) {
            // 确保工时数在 0-8 之间
            if (record.getHours() < 0) {
                record.setHours(0.0);
            } else if (record.getHours() > 8) {
                record.setHours(8.0);
            }

            // 检查是否存在相同用户、项目和日期的记录
            String key = record.getUser().getId() + "-" + record.getProject().getId() + "-" + record.getDate();
            WorkTimeRecord existingRecord = existingRecordsMap.get(key);

            WorkTimeRecord savedRecord;

            if (existingRecord != null) {
                // 如果存在记录，则更新工时和描述
                existingRecord.setHours(record.getHours());
                if (record.getDescription() != null && !record.getDescription().isEmpty()) {
                    existingRecord.setDescription(record.getDescription());
                }
                if (record.getWorkType() != null && !record.getWorkType().isEmpty()) {
                    existingRecord.setWorkType(record.getWorkType());
                }

                // 保存更新后的记录
                savedRecord = workTimeRecordRepository.save(existingRecord);
                System.out.println("更新已存在的工时记录: 用户ID=" + record.getUser().getId() +
                                  ", 项目ID=" + record.getProject().getId() +
                                  ", 日期=" + record.getDate());
            } else {
                // 如果不存在记录，则创建新记录
                savedRecord = workTimeRecordRepository.save(record);

                try {
                    // 获取项目经理作为审批人
                    if (savedRecord.getProject() != null && savedRecord.getProject().getId() != null) {
                        Project project = projectService.getProjectById(savedRecord.getProject().getId());
                        if (project != null && project.getManager() != null) {
                            // 创建审批流程
                            approvalFlowService.createApprovalFlow(savedRecord, project.getManager());
                        }
                    }
                } catch (Exception e) {
                    // 如果创建审批流程失败，记录错误但不影响工时记录的保存
                    System.err.println("创建审批流程失败: " + e.getMessage());
                }
            }

            savedRecords.add(savedRecord);
        }

        return savedRecords;
    }

    public List<WorkTimeRecord> getWorkTimeRecordsByUser(User user) {
        return workTimeRecordRepository.findByUser(user);
    }

    public List<WorkTimeRecord> getWorkTimeRecordsByProject(Project project) {
        return workTimeRecordRepository.findByProject(project);
    }

    public List<WorkTimeRecord> getWorkTimeRecordsByUserAndDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate);
    }

    public List<WorkTimeRecord> getWorkTimeRecordsByUserAndDateRangeFiltered(
            User user, LocalDate startDate, LocalDate endDate, Project project, Boolean approved) {

        // 根据传入的参数决定查询方式
        if (project != null && approved != null) {
            // 按用户、日期范围、项目和审批状态查询
            return workTimeRecordRepository.findByUserAndDateBetweenAndProjectAndApproved(
                    user, startDate, endDate, project, approved);
        } else if (project != null) {
            // 按用户、日期范围和项目查询
            return workTimeRecordRepository.findByUserAndDateBetweenAndProject(
                    user, startDate, endDate, project);
        } else if (approved != null) {
            // 按用户、日期范围和审批状态查询
            return workTimeRecordRepository.findByUserAndDateBetweenAndApproved(
                    user, startDate, endDate, approved);
        } else {
            // 只按用户和日期范围查询
            return workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate);
        }
    }

    public Map<String, Object> getWorkTimeRecordsByUserAndDateRangePaged(
            User user, LocalDate startDate, LocalDate endDate, Project project, Boolean approved,
            int page, int size) {

        // 创建分页对象
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        Page<WorkTimeRecord> recordPage;

        // 根据传入的参数决定查询方式
        if (project != null && approved != null) {
            // 按用户、日期范围、项目和审批状态查询
            recordPage = workTimeRecordRepository.findByUserAndDateBetweenAndProjectAndApproved(
                    user, startDate, endDate, project, approved, pageable);
        } else if (project != null) {
            // 按用户、日期范围和项目查询
            recordPage = workTimeRecordRepository.findByUserAndDateBetweenAndProject(
                    user, startDate, endDate, project, pageable);
        } else if (approved != null) {
            // 按用户、日期范围和审批状态查询
            recordPage = workTimeRecordRepository.findByUserAndDateBetweenAndApproved(
                    user, startDate, endDate, approved, pageable);
        } else {
            // 只按用户和日期范围查询
            recordPage = workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate, pageable);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", recordPage.getContent());
        result.put("totalElements", recordPage.getTotalElements());
        result.put("totalPages", recordPage.getTotalPages());
        result.put("number", recordPage.getNumber());
        result.put("size", recordPage.getSize());

        return result;
    }

    public WorkTimeRecord approveWorkTime(Long recordId) {
        WorkTimeRecord record = workTimeRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("工时记录不存在"));
        record.setApproved(true);
        record.setStatus("APPROVED");
        return workTimeRecordRepository.save(record);
    }

    public WorkTimeRecord rejectWorkTime(Long recordId, String reason) {
        WorkTimeRecord record = workTimeRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("工时记录不存在"));
        record.setApproved(false);
        record.setStatus("REJECTED: " + reason);
        return workTimeRecordRepository.save(record);
    }

    public void deleteWorkTime(Long recordId) {
        if (!workTimeRecordRepository.existsById(recordId)) {
            throw new RuntimeException("工时记录不存在");
        }
        workTimeRecordRepository.deleteById(recordId);
    }

    public WorkTimeRecord saveWorkTimeRecord(WorkTimeRecord workTimeRecord) {
        if (workTimeRecord == null) {
            throw new IllegalArgumentException("工时记录不能为空");
        }
        return workTimeRecordRepository.save(workTimeRecord);
    }

    public Double getTotalHoursByProject(Project project) {
        return workTimeRecordRepository.sumHoursByProject(project);
    }

    public Double getTotalHoursByUser(User user) {
        return workTimeRecordRepository.sumHoursByUser(user);
    }

    public Double getTotalHoursByUserAndDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return workTimeRecordRepository.sumHoursByUserAndDateBetween(user, startDate, endDate);
    }

    public Map<String, Object> getDetailedStatsByProject(Project project, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByProjectAndDateBetween(project, startDate, endDate);
        Map<String, Object> stats = new HashMap<>();
        Double totalHours = workTimeRecordRepository.sumHoursByProjectAndDateBetween(project, startDate, endDate);
        stats.put("totalHours", totalHours);
        stats.put("recordCount", records.size());
        stats.put("averageHoursPerRecord", records.isEmpty() ? 0 : totalHours / records.size());

        // 计算加班时长
        double overtimeHours = records.stream()
                .filter(record -> record.getHours() > 8.0)
                .mapToDouble(record -> record.getHours() - 8.0)
                .sum();
        stats.put("overtimeHours", overtimeHours);

        // 计算工作负荷（假设每月标准工时为160小时）
        int months = ((endDate.getYear() - startDate.getYear()) * 12) +
                (endDate.getMonthValue() - startDate.getMonthValue()) + 1;
        double standardHours = months * 160.0;
        double workload = standardHours > 0 ? totalHours / standardHours : 0;
        stats.put("workload", workload);

        // 创建项目工时映射，只包含当前项目
        Map<Long, Double> projectIdHours = new HashMap<>();
        projectIdHours.put(project.getId(), totalHours);
        stats.put("projects", projectIdHours);

        return stats;
    }

    public Map<String, Object> getDetailedStatsByUser(User user, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHours", workTimeRecordRepository.sumHoursByUserAndDateBetween(user, startDate, endDate));
        stats.put("recordCount", records.size());
        stats.put("averageHoursPerRecord", records.isEmpty() ? 0 : (Double)stats.get("totalHours") / records.size());

        // 按项目分组统计工时
        Map<Project, Double> projectHours = records.stream()
                .collect(Collectors.groupingBy(
                    WorkTimeRecord::getProject,
                    Collectors.summingDouble(WorkTimeRecord::getHours)
                ));

        // 将Project对象转换为项目ID，以便前端处理
        Map<Long, Double> projectIdHours = new HashMap<>();
        projectHours.forEach((project, hours) -> {
            if (project != null && project.getId() != null) {
                projectIdHours.put(project.getId(), hours);
            }
        });

        // 添加项目工时数据
        stats.put("projects", projectIdHours);

        return stats;
    }

    public Map<String, Object> generateWeeklyReport(User user, LocalDate weekStartDate) {
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        List<WorkTimeRecord> records = workTimeRecordRepository.findByUserAndDateBetween(user, weekStartDate, weekEndDate);
        Map<String, Object> report = new HashMap<>();
        report.put("totalHours", workTimeRecordRepository.sumHoursByUserAndDateBetween(user, weekStartDate, weekEndDate));
        report.put("recordCount", records.size());
        report.put("projects", records.stream()
            .collect(Collectors.groupingBy(
                WorkTimeRecord::getProject,
                Collectors.summingDouble(WorkTimeRecord::getHours)
            )));
        return report;
    }

    public Map<String, Object> generateMonthlyReport(User user, LocalDate monthStartDate) {
        LocalDate monthEndDate = monthStartDate.plusMonths(1).minusDays(1);

        List<WorkTimeRecord> records = workTimeRecordRepository.findByUserAndDateBetween(user, monthStartDate, monthEndDate);
        Map<String, Object> report = new HashMap<>();
        report.put("totalHours", workTimeRecordRepository.sumHoursByUserAndDateBetween(user, monthStartDate, monthEndDate));
        report.put("recordCount", records.size());
        report.put("projects", records.stream()
            .collect(Collectors.groupingBy(
                WorkTimeRecord::getProject,
                Collectors.summingDouble(WorkTimeRecord::getHours)
            )));
        //report.put("averageDailyHours", report.get("totalHours") / (records.isEmpty() ? 1 : records.size()));
        report.put("averageDailyHours", 0);
        return report;
    }

    public Map<String, Double> getHoursByProjectType(Project project, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByProjectAndDateBetween(project, startDate, endDate);
        return records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getProject().getType(),
                Collectors.summingDouble(WorkTimeRecord::getHours)
            ));
    }

    public Map<String, Double> getHoursByUserAndProjectType(User user, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate);
        return records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getProject().getType(),
                Collectors.summingDouble(WorkTimeRecord::getHours)
            ));
    }

    public Map<ProjectStatus, Double> getHoursByProjectStatus(Project project, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByProjectAndDateBetween(project, startDate, endDate);
        return records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getProject().getStatus(),
                Collectors.summingDouble(WorkTimeRecord::getHours)
            ));
    }

    public Map<ProjectStatus, Double> getHoursByUserAndProjectStatus(User user, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate);
        return records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getProject().getStatus(),
                Collectors.summingDouble(WorkTimeRecord::getHours)
            ));
    }

    public Map<String, Double> getHoursByProjectPriority(Project project, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByProjectAndDateBetween(project, startDate, endDate);
        return records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getProject().getPriority(),
                Collectors.summingDouble(WorkTimeRecord::getHours)
            ));
    }

    public Map<String, Double> getHoursByUserAndProjectPriority(User user, LocalDate startDate, LocalDate endDate) {
        List<WorkTimeRecord> records = workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate);
        return records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getProject().getPriority(),
                Collectors.summingDouble(WorkTimeRecord::getHours)
            ));
    }

    /**
     * 获取所有项目的统计数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含统计数据的Map
     */
    public Map<String, Object> getAllProjectsStats(LocalDate startDate, LocalDate endDate) {
        // 获取指定日期范围内的所有工时记录
        List<WorkTimeRecord> allRecords = workTimeRecordRepository.findByDateBetween(startDate, endDate);

        // 计算总工时
        double totalHours = allRecords.stream()
                .mapToDouble(WorkTimeRecord::getHours)
                .sum();

        // 计算记录数量
        int recordCount = allRecords.size();

        // 计算平均每条记录的工时
        double averageHoursPerRecord = recordCount > 0 ? totalHours / recordCount : 0;

        // 计算加班时长
        double overtimeHours = allRecords.stream()
                .filter(record -> record.getHours() > 8.0)
                .mapToDouble(record -> record.getHours() - 8.0)
                .sum();

        // 计算工作负荷（假设每月标准工时为160小时）
        int months = ((endDate.getYear() - startDate.getYear()) * 12) +
                (endDate.getMonthValue() - startDate.getMonthValue()) + 1;
        double standardHours = months * 160.0;
        double workload = standardHours > 0 ? totalHours / standardHours : 0;

        // 按项目分组统计工时
        Map<Project, Double> projectHours = allRecords.stream()
                .collect(Collectors.groupingBy(
                    WorkTimeRecord::getProject,
                    Collectors.summingDouble(WorkTimeRecord::getHours)
                ));

        // 将Project对象转换为项目ID，以便前端处理
        Map<Long, Double> projectIdHours = new HashMap<>();
        projectHours.forEach((project, hours) -> {
            if (project != null && project.getId() != null) {
                projectIdHours.put(project.getId(), hours);
            }
        });

        // 构建返回结果
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHours", totalHours);
        stats.put("recordCount", recordCount);
        stats.put("averageHoursPerRecord", averageHoursPerRecord);
        stats.put("overtimeHours", overtimeHours);
        stats.put("workload", workload);
        stats.put("projects", projectIdHours);

        return stats;
    }

    /**
     * 获取项目在指定日期范围内的工时记录，支持按审批状态筛选
     * @param project 项目
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param approved 审批状态
     * @return 工时记录列表
     */
    public List<WorkTimeRecord> getWorkTimeRecordsByProjectAndDateRangeFiltered(
            Project project, LocalDate startDate, LocalDate endDate, Boolean approved) {

        if (approved != null) {
            // 按项目、日期范围和审批状态查询
            return workTimeRecordRepository.findByProjectAndDateBetweenAndApproved(
                    project, startDate, endDate, approved);
        } else {
            // 只按项目和日期范围查询
            return workTimeRecordRepository.findByProjectAndDateBetween(project, startDate, endDate);
        }
    }

    /**
     * 获取项目在指定日期范围内的工时记录，支持分页和按审批状态筛选
     * @param project 项目
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param approved 审批状态
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    public Map<String, Object> getWorkTimeRecordsByProjectAndDateRangePaged(
            Project project, LocalDate startDate, LocalDate endDate, Boolean approved,
            int page, int size) {

        // 创建分页对象
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        Page<WorkTimeRecord> recordPage;

        if (approved != null) {
            // 按项目、日期范围和审批状态查询
            recordPage = workTimeRecordRepository.findByProjectAndDateBetweenAndApproved(
                    project, startDate, endDate, approved, pageable);
        } else {
            // 只按项目和日期范围查询
            recordPage = workTimeRecordRepository.findByProjectAndDateBetween(
                    project, startDate, endDate, pageable);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", recordPage.getContent());
        result.put("totalElements", recordPage.getTotalElements());
        result.put("totalPages", recordPage.getTotalPages());
        result.put("number", recordPage.getNumber());
        result.put("size", recordPage.getSize());

        return result;
    }

    // 新增带多重筛选的查询方法
    public Map<String, Object> getWorkTimeRecordsByProjectAndDateRangePagedWithFilters(
            Project project,
            LocalDate startDate,
            LocalDate endDate,
            Boolean approved,
            User user,
            String workType,
            Integer page,
            Integer size) {
        
        // 获取所有符合基本条件的记录
        List<WorkTimeRecord> allRecords = getWorkTimeRecordsByProjectAndDateRangeFilteredWithFilters(
                project, startDate, endDate, approved, user, workType);
        
        // 手动分页
        int start = page * size;
        int end = Math.min(start + size, allRecords.size());
        List<WorkTimeRecord> pageContent = allRecords.subList(start, end);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageContent);
        result.put("totalElements", allRecords.size());
        result.put("totalPages", (int) Math.ceil((double) allRecords.size() / size));
        result.put("number", page);
        result.put("size", size);
        
        return result;
    }

    public List<WorkTimeRecord> getWorkTimeRecordsByProjectAndDateRangeFilteredWithFilters(
            Project project,
            LocalDate startDate,
            LocalDate endDate,
            Boolean approved,
            User user,
            String workType) {
        
        List<WorkTimeRecord> records = workTimeRecordRepository.findByProjectAndDateBetween(
                project, startDate, endDate);
        
        // 应用筛选条件
        return records.stream()
                .filter(record -> approved == null || record.getApproved().equals(approved))
                .filter(record -> user == null || record.getUser().getId().equals(user.getId()))
                .filter(record -> workType == null || workType.isEmpty() || workType.equals(record.getWorkType()))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate())) // 按日期降序排序
                .collect(java.util.stream.Collectors.toList());
    }

    // 批量审核通过
    public int batchApproveWorkTime(List<Long> recordIds) {
        int successCount = 0;
        for (Long recordId : recordIds) {
            try {
                approveWorkTime(recordId);
                successCount++;
            } catch (Exception e) {
                System.err.println("审核工时记录 " + recordId + " 失败: " + e.getMessage());
            }
        }
        return successCount;
    }

    // 批量驳回
    public int batchRejectWorkTime(List<Long> recordIds, String reason) {
        int successCount = 0;
        for (Long recordId : recordIds) {
            try {
                rejectWorkTime(recordId, reason);
                successCount++;
            } catch (Exception e) {
                System.err.println("驳回工时记录 " + recordId + " 失败: " + e.getMessage());
            }
        }
        return successCount;
    }
}