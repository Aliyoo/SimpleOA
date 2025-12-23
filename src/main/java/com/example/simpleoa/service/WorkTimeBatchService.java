package com.example.simpleoa.service;

import com.example.simpleoa.dto.worktime.*;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import com.example.simpleoa.model.Workday;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.WorkTimeRecordRepository;
import com.example.simpleoa.repository.WorkdayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工时批量填写服务
 * 提供批量查询和保存工时数据的功能，避免N+1查询问题
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkTimeBatchService {

    private final ProjectRepository projectRepository;
    private final WorkTimeRecordRepository workTimeRecordRepository;
    private final WorkdayRepository workdayRepository;

    /**
     * 获取批量填报考勤数据
     * 一次性查询所有项目和成员信息，以及扁平化的工时数据
     *
     * @param managerId       项目经理ID
     * @param startDateStr    开始日期 (YYYY-MM-DD)
     * @param endDateStr      结束日期 (YYYY-MM-DD)
     * @param showAllProjects 是否显示所有项目（true）还是只显示管理的项目（false）
     * @return WorkTimeBatchFillDTO 批量填报考勤数据
     */
    @Transactional(readOnly = true)
    public WorkTimeBatchFillDTO getBatchFillData(Long managerId, String startDateStr, String endDateStr, boolean showAllProjects) {
        long startTime = System.currentTimeMillis();

        // 解析日期
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        log.info("开始批量查询工时数据: managerId={}, startDate={}, endDate={}, showAllProjects={}",
                managerId, startDate, endDate, showAllProjects);

        // ========== 第一步：查询项目列表 ==========
        List<Project> projects = showAllProjects
                ? projectRepository.findAllActiveProjects()
                : projectRepository.findActiveProjectsByManagerId(managerId);

        if (projects.isEmpty()) {
            log.warn("未找到任何项目数据，managerId={}, showAllProjects={}", managerId, showAllProjects);
            WorkTimeBatchFillDTO emptyResult = new WorkTimeBatchFillDTO();
            emptyResult.setProjects(Collections.emptyList());
            emptyResult.setWorkTimeData(Collections.emptyList());
            emptyResult.setWorkdays(Collections.emptySet());
            emptyResult.setTotalProjects(0);
            emptyResult.setTotalMembers(0);
            emptyResult.setDateRangeDays((int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
            return emptyResult;
        }

        // ========== 第二步：一次性查询所有项目成员 ==========
        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        // 使用原生SQL批量查询项目成员
        List<Object[]> memberResults = projectRepository.findProjectMembersBatch(projectIds);

        // 构建项目ID -> 成员列表的映射
        Map<Long, List<User>> projectMembersMap = new HashMap<>();
        for (Object[] row : memberResults) {
            Long projectId = ((Number) row[0]).longValue();

            User user = new User();
            user.setId(((Number) row[1]).longValue());
              user.setUsername((String) row[2]);
            user.setRealName((String) row[3]);

            projectMembersMap.computeIfAbsent(projectId, k -> new ArrayList<>()).add(user);
        }

        log.info("查询到 {} 个项目，共 {} 条成员记录", projects.size(), memberResults.size());

        // ========== 第三步：一次性查询所有工时记录 ==========
        java.sql.Date sqlStartDate = java.sql.Date.valueOf(startDate);
        java.sql.Date sqlEndDate = java.sql.Date.valueOf(endDate);

        // 使用原生SQL批量查询工时记录（性能更好）
        List<Object[]> workTimeResults = workTimeRecordRepository.findWorkTimeRecordsBatchNative(
                projectIds, sqlStartDate, sqlEndDate);

        log.info("查询到 {} 条工时记录", workTimeResults.size());

        // ========== 第四步：查询工作日数据 ==========
        List<Workday> workdays = workdayRepository.findByDateBetweenOrderByDate(
                sqlStartDate.toLocalDate(), sqlEndDate.toLocalDate());

        Set<String> workdaySet = workdays.stream()
                .map(w -> w.getDate().toString()) // YYYY-MM-DD格式
                .collect(Collectors.toSet());

        log.info("查询到 {} 个工作日", workdaySet.size());

        // ========== 第五步：构建DTO ==========

        // 1. 构建ProjectMemberDTO列表
        List<ProjectMemberDTO> projectDTOs = projects.stream()
                .map(project -> {
                    ProjectMemberDTO dto = new ProjectMemberDTO();
                    dto.setId(project.getId());
                    dto.setName(project.getName());
                    dto.setManagerId(
                            project.getManager() != null ? project.getManager().getId() : null
                    );

                    // 获取项目成员
                    List<User> members = projectMembersMap.getOrDefault(project.getId(), Collections.emptyList());

                    // 转换为MemberDTO
                    List<MemberDTO> memberDTOs = members.stream()
                            .map(user -> {
                                MemberDTO memberDTO = new MemberDTO();
                                memberDTO.setId(user.getId());
                                memberDTO.setUsername(user.getUsername());
                                memberDTO.setRealName(user.getRealName());
                                // User类没有role字段，只有roles列表
                                // memberDTO.setRole(user.getRole());
                                return memberDTO;
                            })
                            .collect(Collectors.toList());

                    dto.setMembers(memberDTOs);
                    return dto;
                })
                .collect(Collectors.toList());

        // 2. 构建WorkTimeRecordDTO列表（扁平化存储）
        List<WorkTimeRecordDTO> workTimeDTOs = workTimeResults.stream()
                .map(row -> {
                    WorkTimeRecordDTO dto = new WorkTimeRecordDTO();
                    dto.setProjectId(((Number) row[1]).longValue()); // project_id
                    dto.setUserId(((Number) row[2]).longValue());    // user_id
                    dto.setDate(row[3].toString());                   // date (YYYY-MM-DD)
                    Object hoursObj = row[4];                          // hours
                    if (hoursObj instanceof Double) {
                        dto.setHours((Double) hoursObj);
                    } else if (hoursObj instanceof Number) {
                        dto.setHours(((Number) hoursObj).doubleValue());
                    } else {
                        dto.setHours(0.0);
                    }
                    dto.setApproved((Boolean) row[5]);                // approved
                    return dto;
                })
                .collect(Collectors.toList());

        // 3. 计算总成员数
        int totalMembers = projectDTOs.stream()
                .mapToInt(p -> p.getMembers().size())
                .sum();

        // 4. 构建最终结果
        WorkTimeBatchFillDTO result = new WorkTimeBatchFillDTO();
        result.setProjects(projectDTOs);
        result.setWorkTimeData(workTimeDTOs);
        result.setWorkdays(workdaySet);
        result.setTotalProjects(projectDTOs.size());
        result.setTotalMembers(totalMembers);
        result.setDateRangeDays((int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);

        long endTime = System.currentTimeMillis();
        log.info("批量查询完成: 项目数={}, 成员总数={}, 工时记录数={}, 耗时={}ms",
                result.getTotalProjects(),
                result.getTotalMembers(),
                workTimeDTOs.size(),
                endTime - startTime);

        return result;
    }

    /**
     * 保存批量工时数据
     * 只保存有变更的数据
     *
     * @param projectId 项目ID
     * @param workTimeData 工时数据列表
     * @return 保存的记录数
     */
    @Transactional
    public int saveBatchWorkTime(Long projectId, List<WorkTimeRecordDTO> workTimeData) {
        if (workTimeData == null || workTimeData.isEmpty()) {
            log.warn("没有需要保存的工时数据");
            return 0;
        }

        log.info("开始批量保存工时数据: projectId={}, 记录数={}", projectId, workTimeData.size());

        int savedCount = 0;

        for (WorkTimeRecordDTO dto : workTimeData) {
            try {
                // 检查是否已存在
                LocalDate date = LocalDate.parse(dto.getDate());
                boolean exists = workTimeRecordRepository.existsByUserIdAndProjectIdAndDate(
                        dto.getUserId(), projectId, date);

                WorkTimeRecord record;
                if (exists) {
                    // 更新现有记录
                    List<WorkTimeRecord> existingRecords = workTimeRecordRepository
                            .findByUserAndProjectAndDate(
                                    findUserById(dto.getUserId()),
                                    findProjectById(projectId),
                                    date
                            );
                    if (!existingRecords.isEmpty()) {
                        record = existingRecords.get(0);
                    } else {
                        record = new WorkTimeRecord();
                        record.setUser(findUserById(dto.getUserId()));
                        record.setProject(findProjectById(projectId));
                        record.setDate(date);
                    }
                } else {
                    // 创建新记录
                    record = new WorkTimeRecord();
                    record.setUser(findUserById(dto.getUserId()));
                    record.setProject(findProjectById(projectId));
                    record.setDate(date);
                }

                // 设置工时
                Object hoursObj = dto.getHours();
                if (hoursObj instanceof Double) {
                    record.setHours((Double) hoursObj);
                } else if (hoursObj instanceof Integer) {
                    record.setHours(((Integer) hoursObj).doubleValue());
                } else if (hoursObj instanceof Number) {
                    record.setHours(((Number) hoursObj).doubleValue());
                } else {
                    record.setHours(Double.parseDouble(hoursObj.toString()));
                }
                record.setApproved(dto.getApproved() != null ? dto.getApproved() : false);

                // 保存
                workTimeRecordRepository.save(record);
                savedCount++;

            } catch (Exception e) {
                log.error("保存工时记录失败: projectId={}, userId={}, date={}",
                        projectId, dto.getUserId(), dto.getDate(), e);
            }
        }

        log.info("批量保存完成: projectId={}, 成功保存 {} 条记录", projectId, savedCount);
        return savedCount;
    }

    /**
     * 根据ID查找用户（临时方法）
     * TODO: 应该注入UserRepository
     */
    private User findUserById(Long userId) {
        // TODO: 实现真正的查找逻辑
        User user = new User();
        user.setId(userId);
        return user;
    }

    /**
     * 根据ID查找项目（临时方法）
     * TODO: 应该注入ProjectRepository
     */
    private Project findProjectById(Long projectId) {
        // TODO: 实现真正的查找逻辑
        Project project = new Project();
        project.setId(projectId);
        return project;
    }

    /**
     * 获取工时变更检测
     * 对比新旧数据，找出需要更新的记录
     *
     * @param originalData 原始数据（来自数据库）
     * @param newData 新数据（来自前端）
     * @return 需要更新的记录
     */
    public List<WorkTimeRecordDTO> detectChanges(List<WorkTimeRecordDTO> originalData, List<WorkTimeRecordDTO> newData) {
        // 构建原始数据Map，方便O(1)查找
        Map<String, WorkTimeRecordDTO> originalMap = originalData.stream()
                .collect(Collectors.toMap(
                        dto -> String.format("%d_%d_%s", dto.getProjectId(), dto.getUserId(), dto.getDate()),
                        dto -> dto
                ));

        // 找出变更的记录
        List<WorkTimeRecordDTO> changes = new ArrayList<>();

        for (WorkTimeRecordDTO newDto : newData) {
            String key = String.format("%d_%d_%s", newDto.getProjectId(), newDto.getUserId(), newDto.getDate());

            WorkTimeRecordDTO original = originalMap.get(key);

            // 如果是新增或修改，加入变更列表
            if (original == null) {
                // 新增（hours > 0 才需要保存）
                if (newDto.getHours() > 0.0) {
                    changes.add(newDto);
                }
            } else if (!original.getHours().equals(newDto.getHours())) {
                // 修改（包括删除：hours变为0）
                changes.add(newDto);
            }
        }

        log.info("变更检测完成: 总记录数={}, 变更记录数={}", newData.size(), changes.size());
        return changes;
    }
}
