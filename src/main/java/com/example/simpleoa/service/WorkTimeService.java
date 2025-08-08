package com.example.simpleoa.service;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import com.example.simpleoa.repository.WorkTimeRecordRepository;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

        // ========== 性能优化：批量查询现有记录 ==========
        // 收集所有需要检查的用户-项目-日期组合
        List<String> checkKeys = new ArrayList<>();
        for (WorkTimeRecord record : workTimeRecords) {
            if (record.getUser() != null && record.getProject() != null && record.getDate() != null) {
                String key = record.getUser().getId() + "-" + record.getProject().getId() + "-" + record.getDate();
                checkKeys.add(key);
            }
        }

        // 批量查询现有记录，避免N+1问题
        if (!checkKeys.isEmpty()) {
            List<String> existingKeys = workTimeRecordRepository.findExistingKeys(checkKeys);
            
            // 如果有现有记录，批量获取它们
            if (!existingKeys.isEmpty()) {
                List<Object[]> existingRecordsData = workTimeRecordRepository.findByUserProjectDateKeys(existingKeys);
                for (Object[] data : existingRecordsData) {
                    String key = (String) data[0];
                    WorkTimeRecord record = (WorkTimeRecord) data[1];
                    existingRecordsMap.put(key, record);
                }
            }
        }

        // ========== 批量处理工时记录 ==========
        List<WorkTimeRecord> recordsToSave = new ArrayList<>();
        List<WorkTimeRecord> recordsToUpdate = new ArrayList<>();

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

            if (existingRecord != null) {
                // 更新现有记录
                existingRecord.setHours(record.getHours());
                if (record.getDescription() != null && !record.getDescription().isEmpty()) {
                    existingRecord.setDescription(record.getDescription());
                }
                if (record.getWorkType() != null && !record.getWorkType().isEmpty()) {
                    existingRecord.setWorkType(record.getWorkType());
                }
                if (record.isApproved()) {
                    existingRecord.setApproved(true);
                }
                if (record.getStatus() != null && !record.getStatus().isEmpty()) {
                    existingRecord.setStatus(record.getStatus());
                }
                recordsToUpdate.add(existingRecord);
                System.out.println("标记更新已存在的工时记录: 用户ID=" + record.getUser().getId() +
                                  ", 项目ID=" + record.getProject().getId() +
                                  ", 日期=" + record.getDate());
            } else {
                // 新建记录
                recordsToSave.add(record);
            }
        }

        // ========== 批量保存操作 ==========
        List<WorkTimeRecord> allRecordsToSave = new ArrayList<>();
        allRecordsToSave.addAll(recordsToUpdate);  // 更新的记录
        allRecordsToSave.addAll(recordsToSave);    // 新建的记录

        // 使用批量保存，减少数据库往返
        savedRecords = workTimeRecordRepository.saveAll(allRecordsToSave);

        // ========== 批量创建审批流程 ==========
        try {
            // 收集需要创建审批流程的项目ID
            Set<Long> projectIds = recordsToSave.stream()
                .filter(r -> r.getProject() != null && r.getProject().getId() != null)
                .map(r -> r.getProject().getId())
                .collect(Collectors.toSet());

            // 批量获取项目信息
            Map<Long, Project> projectMap = new HashMap<>();
            for (Long projectId : projectIds) {
                Project project = projectService.getProjectById(projectId);
                if (project != null) {
                    projectMap.put(projectId, project);
                }
            }

            // 为新记录创建审批流程
            for (WorkTimeRecord savedRecord : savedRecords) {
                if (recordsToSave.contains(savedRecord) && 
                    savedRecord.getProject() != null && 
                    savedRecord.getProject().getId() != null) {
                    
                    Project project = projectMap.get(savedRecord.getProject().getId());
                    if (project != null && project.getManager() != null) {
                        ApprovalFlow approvalFlow = approvalFlowService.createApprovalFlow(savedRecord, project.getManager());
                        
                        // 如果工时记录已经是审批通过状态，同时更新审批流程状态
                        if (savedRecord.isApproved()) {
                            approvalFlowService.updateApprovalFlowStatus(approvalFlow.getId(), "APPROVED", "项目经理填写，自动审批通过");
                            System.out.println("工时记录ID " + savedRecord.getId() + " 已自动审批通过");
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果创建审批流程失败，记录错误但不影响工时记录的保存
            System.err.println("批量创建审批流程失败: " + e.getMessage());
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

    // 分页查询，并根据条件进行筛选
    public Page<WorkTimeRecord> getWorkTimeRecordsByUserAndDateRangePaged(
            User user, LocalDate startDate, LocalDate endDate, Project project, Boolean approved,
            int page, int size) {

        // 创建分页对象
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        Page<WorkTimeRecord> recordPage;

        // 根据传入的参数决定查询方式
        if (project != null && approved != null) {
            recordPage = workTimeRecordRepository.findByUserAndDateBetweenAndProjectAndApproved(user, startDate, endDate, project, approved, pageable);
        } else if (project != null) {
            recordPage = workTimeRecordRepository.findByUserAndDateBetweenAndProject(user, startDate, endDate, project, pageable);
        } else if (approved != null) {
            recordPage = workTimeRecordRepository.findByUserAndDateBetweenAndApproved(user, startDate, endDate, approved, pageable);
        } else {
            recordPage = workTimeRecordRepository.findByUserAndDateBetween(user, startDate, endDate, pageable);
        }

        return recordPage;
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
        // 使用去重查询避免重复记录
        List<WorkTimeRecord> records = workTimeRecordRepository.findDistinctByUserAndDateBetween(user, startDate, endDate);
        Map<String, Object> stats = new HashMap<>();
        
        // 使用新的去重统计方法
        Double totalHours = workTimeRecordRepository.sumHoursByUserAndDateBetweenWithoutDuplicates(user.getId(), startDate, endDate);
        if (totalHours == null) {
            totalHours = 0.0;
        }
        
        // 去重记录数统计：按日期和项目分组
        Map<String, Set<Long>> dateProjectMap = new HashMap<>();
        for (WorkTimeRecord record : records) {
            String dateKey = record.getDate().toString();
            dateProjectMap.computeIfAbsent(dateKey, k -> new HashSet<>()).add(record.getProject().getId());
        }
        int distinctRecordCount = dateProjectMap.values().stream()
                .mapToInt(Set::size)
                .sum();
        
        stats.put("totalHours", totalHours);
        stats.put("recordCount", distinctRecordCount);
        stats.put("averageHoursPerRecord", distinctRecordCount > 0 ? totalHours / distinctRecordCount : 0);

        // 按项目分组统计工时，去重处理，同时为加班计算准备每日总量
        Map<Long, Map<LocalDate, Double>> projectDateHours = new HashMap<>();
        Map<String, Map<Long, Double>> dateProjectHours = new HashMap<>();
        for (WorkTimeRecord record : records) {
            if (record.getProject() != null && record.getProject().getId() != null) {
                Long projectId = record.getProject().getId();
                LocalDate date = record.getDate();
                
                projectDateHours.computeIfAbsent(projectId, k -> new HashMap<>())
                    .merge(date, record.getHours(), Math::max); // 同一天同一项目取最大值
                String dateKey = date.toString();
                dateProjectHours.computeIfAbsent(dateKey, k -> new HashMap<>())
                    .merge(projectId, record.getHours(), Math::max); // 同一天同一项目取最大值
            }
        }
        
        // 计算每个项目的总工时
        Map<Long, Double> projectIdHours = new HashMap<>();
        projectDateHours.forEach((projectId, dateHoursMap) -> {
            double projectTotal = dateHoursMap.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            projectIdHours.put(projectId, projectTotal);
        });

        // 计算加班时长（每天超过8小时的部分）
        double overtimeHours = 0.0;
        for (Map<Long, Double> perProject : dateProjectHours.values()) {
            double dailyTotal = perProject.values().stream().mapToDouble(Double::doubleValue).sum();
            if (dailyTotal > 8.0) {
                overtimeHours += (dailyTotal - 8.0);
            }
        }
        stats.put("overtimeHours", overtimeHours);

        // 计算工作负荷（假设每月标准工时为160小时）
        int months = ((endDate.getYear() - startDate.getYear()) * 12) +
                (endDate.getMonthValue() - startDate.getMonthValue()) + 1;
        double standardHours = months * 160.0;
        double workload = standardHours > 0 ? (totalHours / standardHours) : 0.0;
        stats.put("workload", workload);

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
    public Page<WorkTimeRecord> getWorkTimeRecordsByProjectAndDateRangePagedWithFilters(
            Project project,
            LocalDate startDate,
            LocalDate endDate,
            Boolean approved,
            User user,
            String workType,
            Integer page,
            Integer size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));
        Long userId = (user != null ? user.getId() : null);
        return workTimeRecordRepository.findByProjectAndDateRangeWithFilters(
                project, startDate, endDate, approved, userId, workType, pageable);
    }

    public List<WorkTimeRecord> getWorkTimeRecordsByProjectAndDateRangeFilteredWithFilters(
            Project project,
            LocalDate startDate,
            LocalDate endDate,
            Boolean approved,
            User user,
            String workType) {

        Long userId = (user != null ? user.getId() : null);
        return workTimeRecordRepository.findByProjectAndDateRangeWithFilters(
                project, startDate, endDate, approved, userId, workType);
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

    // ========== 批量填写页面性能优化方法 ==========
    
    /**
     * 批量获取多个项目的工时数据，优化前端批量填写页面性能
     * 
     * @param projectIds 项目ID列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按项目分组的工时记录Map
     */
    @Transactional(readOnly = true)
    public Map<Long, List<WorkTimeRecord>> getBatchWorkTimeByProjects(
            List<Long> projectIds, LocalDate startDate, LocalDate endDate) {
        
        // 使用优化的批量查询
        List<WorkTimeRecord> allRecords = workTimeRecordRepository
            .findByProjectIdsAndDateRangeWithJoins(projectIds, startDate, endDate);
        
        // 按项目ID分组
        return allRecords.stream()
            .collect(Collectors.groupingBy(record -> record.getProject().getId()));
    }
    
    /**
     * 高性能批量用户统计查询
     * 
     * @param userIds 用户ID列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户统计数据列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBatchUserStats(
            List<Long> userIds, LocalDate startDate, LocalDate endDate) {
        
        List<Object[]> statsData = workTimeRecordRepository
            .getUserStatsBatch(userIds, startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : statsData) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("userId", data[0]);
            stat.put("totalHours", data[1] != null ? ((Number) data[1]).doubleValue() : 0.0);
            stat.put("recordCount", data[2] != null ? ((Number) data[2]).longValue() : 0L);
            stat.put("avgHours", data[3] != null ? ((Number) data[3]).doubleValue() : 0.0);
            stat.put("overtimeHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 高性能项目统计查询
     * 
     * @param projectIds 项目ID列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 项目统计数据列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBatchProjectStats(
            List<Long> projectIds, LocalDate startDate, LocalDate endDate) {
        
        List<Object[]> statsData = workTimeRecordRepository
            .getProjectStatsBatch(projectIds, startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : statsData) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("projectId", data[0]);
            stat.put("totalHours", data[1] != null ? ((Number) data[1]).doubleValue() : 0.0);
            stat.put("memberCount", data[2] != null ? ((Number) data[2]).intValue() : 0);
            stat.put("recordCount", data[3] != null ? ((Number) data[3]).longValue() : 0L);
            stat.put("avgHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取工作类型统计
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param projectId 项目ID（可选）
     * @param userId 用户ID（可选）
     * @return 工作类型统计Map
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> getWorkTypeStats(
            LocalDate startDate, LocalDate endDate, Long projectId, Long userId) {
        
        List<Object[]> statsData = workTimeRecordRepository
            .getWorkTypeStats(startDate, endDate, projectId, userId);
        
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Object[] data : statsData) {
            String workType = (String) data[0];
            Double totalHours = data[1] != null ? ((Number) data[1]).doubleValue() : 0.0;
            Long recordCount = data[2] != null ? ((Number) data[2]).longValue() : 0L;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalHours", totalHours);
            stats.put("recordCount", recordCount);
            result.put(workType != null ? workType : "未分类", stats);
        }
        
        return result;
    }
    
    /**
     * 获取时间序列统计数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param period 时间段类型（month, week, day）
     * @return 时间序列统计数据
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTimeSeriesStats(
            LocalDate startDate, LocalDate endDate, String period) {
        
        String dateFormat;
        switch (period.toLowerCase()) {
            case "month":
                dateFormat = "%Y-%m";
                break;
            case "week":
                dateFormat = "%Y-%u";
                break;
            case "day":
            default:
                dateFormat = "%Y-%m-%d";
                break;
        }
        
        List<Object[]> statsData = workTimeRecordRepository
            .getTimeSeriesStats(startDate, endDate, dateFormat);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : statsData) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("period", data[0]);
            stat.put("totalHours", data[1] != null ? ((Number) data[1]).doubleValue() : 0.0);
            stat.put("recordCount", data[2] != null ? ((Number) data[2]).longValue() : 0L);
            stat.put("uniqueUsers", data[3] != null ? ((Number) data[3]).intValue() : 0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取工时排行榜
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param limit 排行数量限制
     * @return 用户工时排行榜
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWorkTimeRanking(
            LocalDate startDate, LocalDate endDate, int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> rankingData = workTimeRecordRepository
            .getTopUsersByHours(startDate, endDate, pageable);
        
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] data : rankingData) {
            Map<String, Object> userStats = new HashMap<>();
            userStats.put("rank", rank++);
            userStats.put("userId", data[0]);
            userStats.put("username", data[1]);
            userStats.put("realName", data[2]);
            userStats.put("totalHours", data[3] != null ? ((Number) data[3]).doubleValue() : 0.0);
            userStats.put("recordCount", data[4] != null ? ((Number) data[4]).longValue() : 0L);
            result.add(userStats);
        }
        
        return result;
    }
    
    /**
     * 获取工时分布统计
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 工时分布统计数据
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getHoursDistribution(LocalDate startDate, LocalDate endDate) {
        
        List<Object[]> distributionData = workTimeRecordRepository
            .getHoursDistribution(startDate, endDate);
        
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] data : distributionData) {
            String range = (String) data[0];
            Long count = data[1] != null ? ((Number) data[1]).longValue() : 0L;
            result.put(range, count);
        }
        
        return result;
    }
    
    /**
     * 高性能分页查询，使用游标分页避免深度分页性能问题
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param lastId 上一页最后记录ID（游标）
     * @param size 页面大小
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCursorPagedRecords(
            LocalDate startDate, LocalDate endDate, Long lastId, int size) {
        
        Pageable pageable = PageRequest.of(0, size);
        List<WorkTimeRecord> records = workTimeRecordRepository
            .findByDateRangeCursor(startDate, endDate, lastId != null ? lastId : 0L, pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", records);
        result.put("hasNext", records.size() == size);
        result.put("lastId", records.isEmpty() ? lastId : records.get(records.size() - 1).getId());
        result.put("size", records.size());
        
        return result;
    }
    
    /**
     * 检查数据完整性
     * 
     * @param userIds 用户ID列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 数据完整性问题列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> checkDataIntegrity(
            List<Long> userIds, LocalDate startDate, LocalDate endDate) {
        
        List<Object[]> issuesData = workTimeRecordRepository
            .findDataIntegrityIssues(userIds, startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : issuesData) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("userId", data[0]);
            issue.put("workDate", data[1]);
            issue.put("recordCount", data[2]);
            issue.put("dailyHours", data[3] != null ? ((Number) data[3]).doubleValue() : 0.0);
            result.add(issue);
        }
        
        return result;
    }
    
    /**
     * 批量导出工时数据，优化大数据量导出性能
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param projectIds 项目ID列表（可选）
     * @param userIds 用户ID列表（可选）
     * @return 导出数据
     */
    @Transactional(readOnly = true)
    public List<WorkTimeRecord> exportWorkTimeData(
            LocalDate startDate, LocalDate endDate, 
            List<Long> projectIds, List<Long> userIds) {
        
        if (projectIds != null && !projectIds.isEmpty()) {
            return workTimeRecordRepository
                .findByProjectIdsAndDateRangeWithJoins(projectIds, startDate, endDate);
        } else if (userIds != null && !userIds.isEmpty()) {
            return workTimeRecordRepository
                .findByUserIdsAndDateRangeCursor(userIds, startDate, endDate, 0L, Pageable.unpaged());
        } else {
            // 大数据量导出使用游标分页
            List<WorkTimeRecord> allRecords = new ArrayList<>();
            Long lastId = 0L;
            int batchSize = 1000;
            
            while (true) {
                Pageable pageable = PageRequest.of(0, batchSize);
                List<WorkTimeRecord> batch = workTimeRecordRepository
                    .findByDateRangeCursor(startDate, endDate, lastId, pageable);
                
                if (batch.isEmpty()) {
                    break;
                }
                
                allRecords.addAll(batch);
                lastId = batch.get(batch.size() - 1).getId();
                
                if (batch.size() < batchSize) {
                    break;
                }
            }
            
            return allRecords;
        }
    }
    
    /**
     * 批量获取项目用户统计信息，优化仪表板性能
     * 
     * @param projectIds 项目ID列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBatchProjectUserStats(
            List<Long> projectIds, LocalDate startDate, LocalDate endDate) {
        
        List<Object[]> statsData = workTimeRecordRepository
            .getProjectUserStatsBatch(projectIds, startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : statsData) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("projectId", data[0]);
            stat.put("userId", data[1]);
            stat.put("username", data[2]);
            stat.put("realName", data[3]);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("recordCount", data[5] != null ? ((Number) data[5]).longValue() : 0L);
            stat.put("avgHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 高效的存在性检查，避免查询完整记录
     * 
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param date 日期
     * @return 是否存在记录
     */
    @Transactional(readOnly = true)
    public boolean existsWorkTimeRecord(Long userId, Long projectId, LocalDate date) {
        return workTimeRecordRepository.existsByUserIdAndProjectIdAndDate(userId, projectId, date);
    }
    
    /**
     * 优化的流式过滤方法，替代原有的内存过滤
     * 
     * @param project 项目
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param approved 审批状态
     * @param user 用户
     * @param workType 工作类型
     * @return 过滤后的记录列表
     */
    @Transactional(readOnly = true)
    public List<WorkTimeRecord> getOptimizedFilteredRecords(
            Project project,
            LocalDate startDate,
            LocalDate endDate,
            Boolean approved,
            User user,
            String workType) {
        
        // 使用数据库层面的过滤，而不是内存过滤
        if (project != null && approved != null) {
            return workTimeRecordRepository.findByProjectAndDateBetweenAndApproved(
                project, startDate, endDate, approved);
        } else {
            List<WorkTimeRecord> records = workTimeRecordRepository.findByProjectAndDateBetween(
                project, startDate, endDate);
            
            // 只在必要时使用流过滤
            return records.stream()
                .filter(record -> approved == null || Boolean.valueOf(record.isApproved()).equals(approved))
                .filter(record -> user == null || record.getUser().getId().equals(user.getId()))
                .filter(record -> workType == null || workType.isEmpty() || workType.equals(record.getWorkType()))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
        }
    }
    
    // ========== 统计报表性能优化方法（解决N+1查询问题）==========
    
    /**
     * 获取综合统计数据 - 一次性获取所有基础统计数据，避免多次查询
     * 替代原有getAllProjectsStats()方法中的多次查询逻辑
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComprehensiveStatistics(LocalDate startDate, LocalDate endDate) {
        // 使用一次SQL查询获取所有基础统计数据
        Object[] statsData = workTimeRecordRepository.getComprehensiveStats(startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        
        // 增加错误处理和调试信息
        if (statsData == null || statsData.length < 10) {
            System.err.println("getComprehensiveStats returned null or insufficient data: " + 
                               (statsData != null ? statsData.length + " elements" : "null"));
            // 返回默认值
            result.put("totalUsers", 0L);
            result.put("totalProjects", 0L);
            result.put("totalRecords", 0L);
            result.put("totalHours", 0.0);
            result.put("avgHours", 0.0);
            result.put("maxHours", 0.0);
            result.put("minHours", 0.0);
            result.put("totalOvertimeHours", 0.0);
            result.put("approvedRecords", 0L);
            result.put("pendingRecords", 0L);
            return result;
        }
        
        try {
            System.out.println("=== 调试 getComprehensiveStats 数据 ===");
            System.out.println("statsData length: " + statsData.length);
            for (int i = 0; i < statsData.length; i++) {
                System.out.println("  [" + i + "]: " + (statsData[i] != null ? statsData[i].getClass().getSimpleName() + " = " + statsData[i] : "null"));
            }
            
            result.put("totalUsers", statsData[0] != null ? ((Number) statsData[0]).longValue() : 0L);
            result.put("totalProjects", statsData[1] != null ? ((Number) statsData[1]).longValue() : 0L);
            result.put("totalRecords", statsData[2] != null ? ((Number) statsData[2]).longValue() : 0L);
            result.put("totalHours", statsData[3] != null ? ((Number) statsData[3]).doubleValue() : 0.0);
            result.put("avgHours", statsData[4] != null ? ((Number) statsData[4]).doubleValue() : 0.0);
            result.put("maxHours", statsData[5] != null ? ((Number) statsData[5]).doubleValue() : 0.0);
            result.put("minHours", statsData[6] != null ? ((Number) statsData[6]).doubleValue() : 0.0);
            result.put("totalOvertimeHours", statsData[7] != null ? ((Number) statsData[7]).doubleValue() : 0.0);
            result.put("approvedRecords", statsData[8] != null ? ((Number) statsData[8]).longValue() : 0L);
            result.put("pendingRecords", statsData[9] != null ? ((Number) statsData[9]).longValue() : 0L);
            
            System.out.println("=== 处理后的结果 ===");
            result.forEach((k, v) -> System.out.println(k + ": " + v));
        } catch (ClassCastException e) {
            System.err.println("Type conversion error in getComprehensiveStatistics: " + e.getMessage());
            System.err.println("statsData types: ");
            for (int i = 0; i < statsData.length; i++) {
                System.err.println("  [" + i + "]: " + (statsData[i] != null ? statsData[i].getClass().getName() + " = " + statsData[i] : "null"));
            }
            throw new RuntimeException("Failed to process comprehensive statistics data", e);
        }
        
        return result;
    }
    
    /**
     * 获取综合统计数据 - 混合方案：使用批量查询但保持原有计算逻辑
     * 避免复杂的SQL聚合查询可能引起的数据类型问题
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComprehensiveStatisticsV2(LocalDate startDate, LocalDate endDate) {
        // 批量获取日期范围内的所有记录
        List<WorkTimeRecord> allRecords = workTimeRecordRepository.findByDateBetween(startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        
        if (allRecords.isEmpty()) {
            result.put("totalUsers", 0L);
            result.put("totalProjects", 0L); 
            result.put("totalRecords", 0L);
            result.put("totalHours", 0.0);
            result.put("avgHours", 0.0);
            result.put("maxHours", 0.0);
            result.put("minHours", 0.0);
            result.put("totalOvertimeHours", 0.0);
            result.put("approvedRecords", 0L);
            result.put("pendingRecords", 0L);
            return result;
        }
        
        // 使用Java Stream API计算统计数据（保持原有逻辑）
        long totalUsers = allRecords.stream()
            .map(r -> r.getUser().getId())
            .distinct()
            .count();
            
        long totalProjects = allRecords.stream()
            .map(r -> r.getProject().getId())
            .distinct()
            .count();
            
        long totalRecords = allRecords.size();
        
        double totalHours = allRecords.stream()
            .mapToDouble(WorkTimeRecord::getHours)
            .sum();
            
        double avgHours = totalHours / totalRecords;
        
        double maxHours = allRecords.stream()
            .mapToDouble(WorkTimeRecord::getHours)
            .max()
            .orElse(0.0);
            
        double minHours = allRecords.stream()
            .mapToDouble(WorkTimeRecord::getHours)
            .min()
            .orElse(0.0);
            
        double totalOvertimeHours = allRecords.stream()
            .filter(record -> record.getHours() > 8.0)
            .mapToDouble(record -> record.getHours() - 8.0)
            .sum();
            
        long approvedRecords = allRecords.stream()
            .filter(WorkTimeRecord::isApproved)
            .count();
            
        long pendingRecords = allRecords.stream()
            .filter(record -> !record.isApproved())
            .count();
        
        result.put("totalUsers", totalUsers);
        result.put("totalProjects", totalProjects);
        result.put("totalRecords", totalRecords);
        result.put("totalHours", totalHours);
        result.put("avgHours", avgHours);
        result.put("maxHours", maxHours);
        result.put("minHours", minHours);
        result.put("totalOvertimeHours", totalOvertimeHours);
        result.put("approvedRecords", approvedRecords);
        result.put("pendingRecords", pendingRecords);
        
        System.out.println("=== V2版本统计结果 ===");
        result.forEach((k, v) -> System.out.println(k + ": " + v));
        
        return result;
    }
    
    /**
     * 获取所有项目聚合统计 - 替代原有getAllProjectsStats()方法，解决N+1查询问题
     * 一次SQL查询获取所有项目的统计数据
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllProjectsAggregatedStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> projectStats = workTimeRecordRepository.getAllProjectsAggregatedStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : projectStats) {
            Map<String, Object> projectStat = new HashMap<>();
            projectStat.put("projectId", data[0]);
            projectStat.put("projectName", data[1]);
            projectStat.put("projectStatus", data[2]);
            projectStat.put("memberCount", data[3] != null ? ((Number) data[3]).intValue() : 0);
            projectStat.put("totalRecords", data[4] != null ? ((Number) data[4]).longValue() : 0L);
            projectStat.put("totalHours", data[5] != null ? ((Number) data[5]).doubleValue() : 0.0);
            projectStat.put("avgHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            projectStat.put("maxDailyHours", data[7] != null ? ((Number) data[7]).doubleValue() : 0.0);
            projectStat.put("totalOvertimeHours", data[8] != null ? ((Number) data[8]).doubleValue() : 0.0);
            projectStat.put("approvedRecords", data[9] != null ? ((Number) data[9]).longValue() : 0L);
            projectStat.put("pendingRecords", data[10] != null ? ((Number) data[10]).longValue() : 0L);
            result.add(projectStat);
        }
        
        return result;
    }
    
    /**
     * 获取所有用户聚合统计 - 替代原有的逐个用户查询逻辑
     * 一次SQL查询获取所有用户的统计数据
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllUsersAggregatedStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> userStats = workTimeRecordRepository.getAllUsersAggregatedStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : userStats) {
            Map<String, Object> userStat = new HashMap<>();
            userStat.put("userId", data[0]);
            userStat.put("username", data[1]);
            userStat.put("realName", data[2]);
            userStat.put("department", data[3]);
            userStat.put("projectCount", data[4] != null ? ((Number) data[4]).intValue() : 0);
            userStat.put("totalRecords", data[5] != null ? ((Number) data[5]).longValue() : 0L);
            userStat.put("totalHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            userStat.put("avgHours", data[7] != null ? ((Number) data[7]).doubleValue() : 0.0);
            userStat.put("maxDailyHours", data[8] != null ? ((Number) data[8]).doubleValue() : 0.0);
            userStat.put("totalOvertimeHours", data[9] != null ? ((Number) data[9]).doubleValue() : 0.0);
            userStat.put("approvedRecords", data[10] != null ? ((Number) data[10]).longValue() : 0L);
            userStat.put("pendingRecords", data[11] != null ? ((Number) data[11]).longValue() : 0L);
            result.add(userStat);
        }
        
        return result;
    }
    
    /**
     * 获取项目用户组合统计 - 一次性获取所有项目的用户工时分布
     * 替代多次API调用的方式
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProjectUserComboStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> comboStats = workTimeRecordRepository.getProjectUserComboStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : comboStats) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("projectId", data[0]);
            stat.put("projectName", data[1]);
            stat.put("userId", data[2]);
            stat.put("userRealName", data[3]);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("recordCount", data[5] != null ? ((Number) data[5]).longValue() : 0L);
            stat.put("avgHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            stat.put("maxHours", data[7] != null ? ((Number) data[7]).doubleValue() : 0.0);
            stat.put("overtimeHours", data[8] != null ? ((Number) data[8]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取日期维度聚合统计 - 支持时间趋势分析
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyTrendStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> dailyStats = workTimeRecordRepository.getDailyAggregatedStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : dailyStats) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("workDate", data[0]);
            stat.put("activeUsers", data[1] != null ? ((Number) data[1]).intValue() : 0);
            stat.put("activeProjects", data[2] != null ? ((Number) data[2]).intValue() : 0);
            stat.put("totalRecords", data[3] != null ? ((Number) data[3]).longValue() : 0L);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("avgHours", data[5] != null ? ((Number) data[5]).doubleValue() : 0.0);
            stat.put("totalOvertimeHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取部门维度聚合统计 - 支持部门间对比分析
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDepartmentAggregatedStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> deptStats = workTimeRecordRepository.getDepartmentAggregatedStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : deptStats) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("department", data[0]);
            stat.put("userCount", data[1] != null ? ((Number) data[1]).intValue() : 0);
            stat.put("projectCount", data[2] != null ? ((Number) data[2]).intValue() : 0);
            stat.put("totalRecords", data[3] != null ? ((Number) data[3]).longValue() : 0L);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("avgHours", data[5] != null ? ((Number) data[5]).doubleValue() : 0.0);
            stat.put("totalOvertimeHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取工作类型聚合统计 - 增强版本
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWorkTypeAggregatedStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> workTypeStats = workTimeRecordRepository.getWorkTypeAggregatedStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : workTypeStats) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("workType", data[0] != null ? data[0] : "未分类");
            stat.put("userCount", data[1] != null ? ((Number) data[1]).intValue() : 0);
            stat.put("projectCount", data[2] != null ? ((Number) data[2]).intValue() : 0);
            stat.put("totalRecords", data[3] != null ? ((Number) data[3]).longValue() : 0L);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("avgHours", data[5] != null ? ((Number) data[5]).doubleValue() : 0.0);
            stat.put("maxHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取月度趋势聚合统计 - 支持长期趋势分析
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlyTrendStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> monthlyStats = workTimeRecordRepository.getMonthlyTrendStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : monthlyStats) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("yearMonth", data[0]);
            stat.put("activeUsers", data[1] != null ? ((Number) data[1]).intValue() : 0);
            stat.put("activeProjects", data[2] != null ? ((Number) data[2]).intValue() : 0);
            stat.put("totalRecords", data[3] != null ? ((Number) data[3]).longValue() : 0L);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("avgHours", data[5] != null ? ((Number) data[5]).doubleValue() : 0.0);
            stat.put("totalOvertimeHours", data[6] != null ? ((Number) data[6]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取审批状态聚合统计 - 支持审批工作台统计分析
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApprovalStatusAggregatedStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> approvalStats = workTimeRecordRepository.getApprovalStatusAggregatedStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : approvalStats) {
            Map<String, Object> stat = new HashMap<>();
            Boolean approvalStatus = (Boolean) data[0];
            stat.put("approvalStatus", approvalStatus != null ? approvalStatus : false);
            stat.put("statusName", (approvalStatus != null && approvalStatus) ? "已审批" : "待审批");
            stat.put("userCount", data[1] != null ? ((Number) data[1]).intValue() : 0);
            stat.put("projectCount", data[2] != null ? ((Number) data[2]).intValue() : 0);
            stat.put("totalRecords", data[3] != null ? ((Number) data[3]).longValue() : 0L);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("avgHours", data[5] != null ? ((Number) data[5]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 获取项目状态聚合统计 - 支持项目状态分析
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProjectStatusAggregatedStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> statusStats = workTimeRecordRepository.getProjectStatusAggregatedStats(startDate, endDate);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] data : statusStats) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("projectStatus", data[0]);
            stat.put("projectCount", data[1] != null ? ((Number) data[1]).intValue() : 0);
            stat.put("userCount", data[2] != null ? ((Number) data[2]).intValue() : 0);
            stat.put("totalRecords", data[3] != null ? ((Number) data[3]).longValue() : 0L);
            stat.put("totalHours", data[4] != null ? ((Number) data[4]).doubleValue() : 0.0);
            stat.put("avgHours", data[5] != null ? ((Number) data[5]).doubleValue() : 0.0);
            result.add(stat);
        }
        
        return result;
    }
    
    /**
     * 统一的综合统计API - 一次调用获取所有统计数据，解决N+1查询问题
     * 该方法将替代原有的fetchStatisticsData()方法中的多次API调用
     * 返回前端期望的数据格式
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUnifiedStatisticsData(LocalDate startDate, LocalDate endDate) {
        return getUnifiedStatisticsData(startDate, endDate, null, null);
    }
    
    /**
     * 统一的综合统计API - 支持项目和用户过滤的重载版本
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUnifiedStatisticsData(LocalDate startDate, LocalDate endDate, Long projectId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        // 先获取项目/用户聚合数据，后续可能用于覆盖顶层汇总
        List<Map<String, Object>> projects = getAllProjectsAggregatedStatistics(startDate, endDate);
        if (projectId != null) {
            // 如果指定了项目ID，只保留该项目
            final long pid = projectId.longValue();
            projects = projects.stream()
                    .filter(p -> {
                        Object v = p.get("projectId");
                        return v instanceof Number && ((Number) v).longValue() == pid;
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        // 如果指定了用户ID，按用户维度统计其各项目工时（仅个人工时），替换项目聚合列表
        if (userId != null) {
            List<Object[]> userProjStats = workTimeRecordRepository.getUserProjectAggregatedStats(userId, startDate, endDate);
            List<Map<String, Object>> userProjectStats = new ArrayList<>();
            for (Object[] row : userProjStats) {
                Map<String, Object> m = new HashMap<>();
                m.put("projectId", row[0]);
                m.put("projectName", row[1]);
                m.put("totalRecords", row[2] != null ? ((Number) row[2]).longValue() : 0L);
                m.put("totalHours", row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
                m.put("avgHours", row[4] != null ? ((Number) row[4]).doubleValue() : 0.0);
                m.put("totalOvertimeHours", row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);
                userProjectStats.add(m);
            }
            // 如果还指定了项目ID，则仅保留该项目
            if (projectId != null) {
                final long pid = projectId.longValue();
                userProjectStats = userProjectStats.stream()
                        .filter(p -> {
                            Object v = p.get("projectId");
                            return v instanceof Number && ((Number) v).longValue() == pid;
                        })
                        .collect(java.util.stream.Collectors.toList());
            }
            projects = userProjectStats;
        }
        result.put("projectStats", projects);
        
        List<Map<String, Object>> users = getAllUsersAggregatedStatistics(startDate, endDate);
        if (userId != null) {
            // 如果指定了用户ID，只保留该用户
            final long uid = userId.longValue();
            users = users.stream()
                    .filter(u -> {
                        Object v = u.get("userId");
                        return v instanceof Number && ((Number) v).longValue() == uid;
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        result.put("userStats", users);
        
        // 基础综合统计（全局范围）
        Map<String, Object> comprehensive = getComprehensiveStatisticsV2(startDate, endDate);
        
        // 初始化顶级字段为全局汇总
        Double totalHours = (Double) comprehensive.get("totalHours");
        Double totalOvertimeHours = (Double) comprehensive.get("totalOvertimeHours");
        Long totalRecords = (Long) comprehensive.get("totalRecords");
        Double avgHours = (Double) comprehensive.get("avgHours");
        
        // 如果指定了userId，则优先用该用户的统计覆盖顶层汇总（个人视角优先）
        if (userId != null) {
            // 当同时传入 projectId 与 userId 时，优先计算该用户在该项目内的汇总
            if (projectId != null) {
                Object[] up = workTimeRecordRepository.getUserAggregatedStatsForProject(userId, projectId, startDate, endDate);
                if (up != null) {
                    double th = up[0] != null ? ((Number) up[0]).doubleValue() : 0.0;
                    long rc = up[1] != null ? ((Number) up[1]).longValue() : 0L;
                    double oh = up[3] != null ? ((Number) up[3]).doubleValue() : 0.0;
                    totalHours = th;
                    totalOvertimeHours = oh;
                    totalRecords = rc;
                    avgHours = rc > 0 ? th / rc : 0.0;
                }
            } else if (!users.isEmpty()) {
                Map<String, Object> u = users.get(0);
                Number uTotalHours = (Number) u.get("totalHours");
                Number uTotalRecords = (Number) u.get("totalRecords");
                Number uOvertime = (Number) u.get("totalOvertimeHours");
                Number uAvg = (Number) u.get("avgHours");
                double th = uTotalHours != null ? uTotalHours.doubleValue() : 0.0;
                long rc = uTotalRecords != null ? uTotalRecords.longValue() : 0L;
                double oh = uOvertime != null ? uOvertime.doubleValue() : 0.0;
                Double ah = uAvg != null ? uAvg.doubleValue() : (rc > 0 ? th / rc : 0.0);
                totalHours = th;
                totalOvertimeHours = oh;
                totalRecords = rc;
                avgHours = ah;
            }
        } else if (projectId != null && !projects.isEmpty()) {
            // 如果仅指定了projectId，则用该项目的统计覆盖顶层汇总
            Map<String, Object> p = projects.get(0);
            Number pTotalHours = (Number) p.get("totalHours");
            Number pTotalRecords = (Number) p.get("totalRecords");
            Number pOvertime = (Number) p.get("totalOvertimeHours");
            double th = pTotalHours != null ? pTotalHours.doubleValue() : 0.0;
            long rc = pTotalRecords != null ? pTotalRecords.longValue() : 0L;
            double oh = pOvertime != null ? pOvertime.doubleValue() : 0.0;
            totalHours = th;
            totalOvertimeHours = oh;
            totalRecords = rc;
            avgHours = rc > 0 ? th / rc : 0.0;
        }
        
        result.put("totalHours", totalHours != null ? totalHours : 0.0);
        result.put("overtimeHours", totalOvertimeHours != null ? totalOvertimeHours : 0.0);
        result.put("recordCount", totalRecords != null ? totalRecords : 0L);
        result.put("averageHoursPerRecord", avgHours != null ? avgHours : 0.0);
        
        // 计算工作负载 - 使用覆盖后的总工时
        int months = ((endDate.getYear() - startDate.getYear()) * 12) +
                (endDate.getMonthValue() - startDate.getMonthValue()) + 1;
        double standardHours = months * 160.0;
        double workload = (standardHours > 0 && totalHours != null) ? totalHours / standardHours : 0.0;
        result.put("workload", workload);
        
        // 添加原来的projects字段格式 - 项目ID到工时的映射（此处的totalHours已按个人视角计算）
        Map<Long, Double> projectIdHours = new HashMap<>();
        for (Map<String, Object> project : projects) {
            Object idObj = project.get("projectId");
            Object hoursObj = project.get("totalHours");
            if (idObj instanceof Number && hoursObj instanceof Number) {
                projectIdHours.put(((Number) idObj).longValue(), ((Number) hoursObj).doubleValue());
            }
        }
        result.put("projects", projectIdHours);
        
        // 项目状态统计
        List<Map<String, Object>> projectStatus = getProjectStatusAggregatedStatistics(startDate, endDate);
        result.put("projectStatus", projectStatus);
        
        return result;
    }
}