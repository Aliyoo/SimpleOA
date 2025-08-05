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

        // 按项目分组统计工时，去重处理
        Map<Long, Map<LocalDate, Double>> projectDateHours = new HashMap<>();
        for (WorkTimeRecord record : records) {
            if (record.getProject() != null && record.getProject().getId() != null) {
                Long projectId = record.getProject().getId();
                LocalDate date = record.getDate();
                
                projectDateHours.computeIfAbsent(projectId, k -> new HashMap<>())
                    .merge(date, record.getHours(), Math::max); // 同一天同一项目取最大值
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

        // 获取所有符合基本条件的记录
        List<WorkTimeRecord> allRecords = getWorkTimeRecordsByProjectAndDateRangeFilteredWithFilters(
                project, startDate, endDate, approved, user, workType);

        // 创建 Pageable 对象
        Pageable pageable = PageRequest.of(page, size);

        // 计算当前页的起始位置
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allRecords.size());

        // 获取当前页的记录列表
        List<WorkTimeRecord> pageContent = allRecords.subList(start, end);

        // 构建并返回 Page 对象
        return new PageImpl<>(pageContent, pageable, allRecords.size());
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
                .filter(record -> approved == null || Boolean.valueOf(record.isApproved()).equals(approved))
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
            stat.put("totalHours", data[2]);
            stat.put("recordCount", data[3]);
            stat.put("avgHours", data[4]);
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
}