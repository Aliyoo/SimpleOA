package com.example.simpleoa.controller;

import com.example.simpleoa.dto.worktime.WorkTimeBatchFillDTO;
import com.example.simpleoa.service.WorkTimeBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工时批量填写控制器
 * 提供高性能的批量查询和保存接口，避免N+1查询问题
 */
@RestController
@RequestMapping("/api/worktime/batch")
@Slf4j
@RequiredArgsConstructor
public class WorkTimeBatchController {

    private final WorkTimeBatchService batchService;

    /**
     * 获取批量填报考勤数据
     * 一次性返回所有项目和成员信息，以及扁平化的工时数据
     *
     * @param managerId       项目经理ID
     * @param startDate       开始日期 (YYYY-MM-DD)
     * @param endDate         结束日期 (YYYY-MM-DD)
     * @param showAllProjects 是否显示所有项目（true）还是只显示管理的项目（false）
     * @return WorkTimeBatchFillDTO 批量填报考勤数据
     */
    @GetMapping("/fill-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<WorkTimeBatchFillDTO> getBatchFillData(
            @RequestParam Long managerId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "false") boolean showAllProjects) {

        log.info("收到批量填报考勤数据请求: managerId={}, startDate={}, endDate={}, showAllProjects={}",
                managerId, startDate, endDate, showAllProjects);

        try {
            WorkTimeBatchFillDTO result = batchService.getBatchFillData(
                    managerId, startDate, endDate, showAllProjects);

            log.info("批量查询完成: 项目数={}, 成员总数={}, 工时记录数={}",
                    result.getTotalProjects(),
                    result.getTotalMembers(),
                    result.getWorkTimeData().size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("批量查询工时数据失败: managerId={}, startDate={}, endDate={}",
                    managerId, startDate, endDate, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 健康管理端点
     * 用于检查服务是否正常运行
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("WorkTimeBatchController is running");
    }
}
