package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ApprovalFlowRepository;
import com.example.simpleoa.repository.LeaveBalanceRepository;
import com.example.simpleoa.repository.LeaveRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.LeaveService;
import com.example.simpleoa.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LeaveServiceCompleteImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final ApprovalFlowRepository approvalFlowRepository;
    private final NotificationService notificationService;
    
    @Autowired
    public LeaveServiceCompleteImpl(LeaveRepository leaveRepository,
                                   LeaveBalanceRepository leaveBalanceRepository,
                                   UserRepository userRepository,
                                   ApprovalFlowRepository approvalFlowRepository,
                                   NotificationService notificationService) {
        this.leaveRepository = leaveRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userRepository = userRepository;
        this.approvalFlowRepository = approvalFlowRepository;
        this.notificationService = notificationService;
    }
    
    @Override
    public LeaveRequest createLeave(LeaveRequest leaveRequest) {
        return leaveRepository.save(leaveRequest);
    }
    
    @Override
    public LeaveRequest updateLeave(LeaveRequest leaveRequest) {
        return leaveRepository.save(leaveRequest);
    }
    
    @Override
    public void deleteLeave(Long id) {
        leaveRepository.deleteById(id);
    }
    
    @Override
    public LeaveRequest getLeaveById(Long id) {
        return leaveRepository.findById(id).orElse(null);
    }
    
    @Override
    public List<LeaveRequest> getAllLeaves() {
        return leaveRepository.findAll();
    }
    
    @Override
    public List<LeaveRequest> getLeavesByUser(Long userId) {
        return leaveRepository.findByApplicantId(userId);
    }
    
    @Override
    @Transactional
    public LeaveRequest applyLeave(LeaveRequest leaveRequest) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("未找到当前登录用户");
        }
        
        // 设置申请人
        leaveRequest.setApplicant(currentUser);
        leaveRequest.setStatus(LeaveStatus.PENDING_MANAGER_APPROVAL);
        
        // 计算请假天数
        double leaveDays = calculateLeaveDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());
        leaveRequest.setLeaveDays(leaveDays);
        
        // 检查并初始化请假余额
        int year = leaveRequest.getStartDate().getYear();
        ensureLeaveBalanceInitialized(currentUser.getId(), year);
        if (!checkLeaveBalance(currentUser.getId(), leaveRequest.getLeaveType(), year, leaveDays)) {
            throw new RuntimeException("请假余额不足，当前剩余天数不够申请 " + leaveDays + " 天");
        }
        
        // 检查请假冲突
        if (hasLeaveConflict(currentUser.getId(), leaveRequest.getStartDate(), leaveRequest.getEndDate())) {
            throw new RuntimeException("该时间段已有请假记录");
        }
        
        // 保存请假申请
        LeaveRequest savedRequest = leaveRepository.save(leaveRequest);
        
        // 创建审批流程
        createApprovalFlow(savedRequest);
        
        // 发送通知
        if (notificationService != null) {
            notificationService.sendLeaveApplicationNotification(savedRequest);
        }
        
        return savedRequest;
    }
    
    @Override
    @Transactional
    public LeaveRequest approveLeave(Long id, String comment) {
        LeaveRequest leaveRequest = getLeaveById(id);
        if (leaveRequest == null) {
            throw new RuntimeException("请假申请不存在");
        }
        
        User currentUser = getCurrentUser();
        
        // 根据当前状态更新到下一个状态
        if (leaveRequest.getStatus() == LeaveStatus.PENDING_MANAGER_APPROVAL) {
            // 部门经理审批通过，转到HR审批
            leaveRequest.setStatus(LeaveStatus.PENDING_HR_APPROVAL);
        } else if (leaveRequest.getStatus() == LeaveStatus.PENDING_HR_APPROVAL) {
            // HR审批通过，最终审批通过
            leaveRequest.setStatus(LeaveStatus.APPROVED);
            leaveRequest.setApproveTime(LocalDateTime.now());
            leaveRequest.setApprover(currentUser);
            
            // 更新请假余额
            int year = leaveRequest.getStartDate().getYear();
            updateLeaveBalance(leaveRequest.getApplicant().getId(), 
                            leaveRequest.getLeaveType(), 
                            year, 
                            leaveRequest.getLeaveDays());
        }
        
        leaveRequest.setComment(comment);
        LeaveRequest updatedRequest = leaveRepository.save(leaveRequest);
        
        // 更新审批流程
        updateApprovalFlow(id, "APPROVED", comment);
        
        // 发送通知
        if (notificationService != null) {
            notificationService.sendLeaveApprovalNotification(updatedRequest);
        }
        
        return updatedRequest;
    }
    
    @Override
    @Transactional
    public LeaveRequest rejectLeave(Long id, String comment) {
        LeaveRequest leaveRequest = getLeaveById(id);
        if (leaveRequest == null) {
            throw new RuntimeException("请假申请不存在");
        }
        
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setComment(comment);
        LeaveRequest updatedRequest = leaveRepository.save(leaveRequest);
        
        // 更新审批流程
        updateApprovalFlow(id, "REJECTED", comment);
        
        // 发送通知
        if (notificationService != null) {
            notificationService.sendLeaveRejectionNotification(updatedRequest);
        }
        
        return updatedRequest;
    }
    
    @Override
    @Transactional
    public LeaveRequest cancelLeave(Long id) {
        LeaveRequest leaveRequest = getLeaveById(id);
        if (leaveRequest == null) {
            throw new RuntimeException("请假申请不存在");
        }
        
        User currentUser = getCurrentUser();
        if (!leaveRequest.getApplicant().getId().equals(currentUser.getId())) {
            throw new RuntimeException("只能撤销自己的请假申请");
        }
        
        if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
            throw new RuntimeException("已通过的申请不能撤销");
        }
        
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        return leaveRepository.save(leaveRequest);
    }
    
    @Override
    public List<LeaveRequest> getApprovalList() {
        User currentUser = getCurrentUser();
        // 根据当前用户角色返回待审批列表
        List<LeaveStatus> pendingStatuses = Arrays.asList(
            LeaveStatus.PENDING_MANAGER_APPROVAL,
            LeaveStatus.PENDING_HR_APPROVAL
        );
        return leaveRepository.findByStatusIn(pendingStatuses);
    }
    
    @Override
    public List<LeaveRequest> getMyApplications() {
        User currentUser = getCurrentUser();
        return leaveRepository.findByApplicantId(currentUser.getId());
    }
    
    @Override
    public Page<LeaveRequest> getMyApplicationsPage(Pageable pageable) {
        User currentUser = getCurrentUser();
        return leaveRepository.findByApplicantId(currentUser.getId(), pageable);
    }
    
    @Override
    public List<LeaveRequest> getLeavesByStatus(LeaveStatus status) {
        return leaveRepository.findByStatusIn(Arrays.asList(status));
    }
    
    @Override
    public List<LeaveRequest> getLeavesByType(LeaveType type) {
        User currentUser = getCurrentUser();
        return leaveRepository.findByApplicantIdAndLeaveType(currentUser.getId(), type);
    }
    
    @Override
    public Map<String, Object> getLeaveStatistics(String startDate, String endDate) {
        User currentUser = getCurrentUser();
        LocalDateTime startDateTime = parseDateTime(startDate);
        LocalDateTime endDateTime = parseDateTime(endDate);
        
        List<LeaveRequest> leaves = leaveRepository.findByApplicantIdAndDateRange(
            currentUser.getId(), startDateTime, endDateTime);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 汇总统计
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", leaves.size());
        summary.put("totalDays", leaves.stream().mapToDouble(LeaveRequest::getLeaveDays).sum());
        summary.put("approvedCount", leaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count());
        summary.put("pendingCount", leaves.stream().filter(l -> 
            l.getStatus() == LeaveStatus.PENDING_MANAGER_APPROVAL || 
            l.getStatus() == LeaveStatus.PENDING_HR_APPROVAL).count());
        
        // 按类型统计
        Map<LeaveType, DoubleSummaryStatistics> typeStats = leaves.stream()
            .collect(Collectors.groupingBy(LeaveRequest::getLeaveType,
                    Collectors.summarizingDouble(LeaveRequest::getLeaveDays)));
        
        List<Map<String, Object>> typeDetails = new ArrayList<>();
        for (Map.Entry<LeaveType, DoubleSummaryStatistics> entry : typeStats.entrySet()) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("type", entry.getKey().getDisplayName());
            detail.put("count", entry.getValue().getCount());
            detail.put("totalDays", entry.getValue().getSum());
            typeDetails.add(detail);
        }
        
        statistics.put("summary", summary);
        statistics.put("typeDetails", typeDetails);
        
        return statistics;
    }
    
    @Override
    public Map<String, Object> getDepartmentStatistics() {
        // 实现部门统计逻辑
        Map<String, Object> statistics = new HashMap<>();
        // TODO: 实现具体逻辑
        return statistics;
    }
    
    @Override
    public Map<String, Object> getPersonalStatistics(Long userId, Integer year) {
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserIdAndYear(userId, year);
        Map<String, Object> statistics = new HashMap<>();
        
        List<Map<String, Object>> balanceDetails = balances.stream().map(balance -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("type", balance.getLeaveType().getDisplayName());
            detail.put("total", balance.getTotalDays());
            detail.put("used", balance.getUsedDays());
            detail.put("remaining", balance.getRemainingDays());
            return detail;
        }).collect(Collectors.toList());
        
        statistics.put("year", year);
        statistics.put("balances", balanceDetails);
        
        return statistics;
    }
    
    @Override
    public List<LeaveBalance> getUserLeaveBalance(Long userId, Integer year) {
        return leaveBalanceRepository.findByUserIdAndYear(userId, year);
    }
    
    @Override
    @Transactional
    public LeaveBalance updateLeaveBalance(Long userId, LeaveType leaveType, Integer year, Double usedDays) {
        Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByUserIdAndYearAndLeaveType(userId, year, leaveType);
        
        if (!balanceOpt.isPresent()) {
            throw new RuntimeException("未找到对应的请假余额记录");
        }
        
        LeaveBalance balance = balanceOpt.get();
        balance.setUsedDays((int) Math.ceil(balance.getUsedDays() + usedDays));
        balance.calculateRemainingDays();
        
        return leaveBalanceRepository.save(balance);
    }
    
    @Override
    @Transactional
    public void initializeUserLeaveBalance(Long userId, Integer year) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        for (LeaveType type : LeaveType.values()) {
            if (type == LeaveType.OTHER) continue;
            
            Optional<LeaveBalance> existing = leaveBalanceRepository.findByUserIdAndYearAndLeaveType(userId, year, type);
            if (!existing.isPresent()) {
                LeaveBalance balance = new LeaveBalance();
                balance.setUser(user);
                balance.setLeaveType(type);
                balance.setYear(year);
                balance.setTotalDays(type.getDefaultDays());
                balance.setUsedDays(0);
                balance.setRemainingDays(type.getDefaultDays());
                leaveBalanceRepository.save(balance);
            }
        }
    }
    
    // 确保用户请假余额已初始化
    private void ensureLeaveBalanceInitialized(Long userId, Integer year) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        for (LeaveType type : LeaveType.values()) {
            if (type == LeaveType.OTHER) continue;
            
            Optional<LeaveBalance> existing = leaveBalanceRepository.findByUserIdAndYearAndLeaveType(userId, year, type);
            if (!existing.isPresent()) {
                LeaveBalance balance = new LeaveBalance();
                balance.setUser(user);
                balance.setLeaveType(type);
                balance.setYear(year);
                balance.setTotalDays(type.getDefaultDays());
                balance.setUsedDays(0);
                balance.setRemainingDays(type.getDefaultDays());
                leaveBalanceRepository.save(balance);
                log.info("初始化用户 {} 的 {} 年 {} 类型请假余额", user.getUsername(), year, type.getDisplayName());
            }
        }
    }
    
    @Override
    public boolean checkLeaveBalance(Long userId, LeaveType leaveType, Integer year, Double requestDays) {
        if (leaveType == LeaveType.OTHER) {
            return true; // 其他类型不检查余额
        }
        
        Integer remainingDays = leaveBalanceRepository.getRemainingDays(userId, year, leaveType);
        return remainingDays != null && remainingDays >= requestDays;
    }
    
    @Override
    public boolean hasLeaveConflict(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<LeaveRequest> overlappingLeaves = leaveRepository.findOverlappingLeaves(
            startDate, endDate, LeaveStatus.APPROVED);
        
        return overlappingLeaves.stream()
            .anyMatch(leave -> leave.getApplicant().getId().equals(userId));
    }
    
    @Override
    public byte[] exportLeaveRecords(Long userId, String startDate, String endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("请假记录");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"申请时间", "请假类型", "开始时间", "结束时间", "请假天数", "请假事由", "状态", "审批人", "审批时间"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 查询数据
            LocalDateTime start = parseDateTime(startDate);
            LocalDateTime end = parseDateTime(endDate);
            List<LeaveRequest> leaves = leaveRepository.findByApplicantIdAndDateRange(userId, start, end);
            
            // 填充数据
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            int rowNum = 1;
            for (LeaveRequest leave : leaves) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(leave.getCreateTime().format(formatter));
                row.createCell(1).setCellValue(leave.getLeaveType().getDisplayName());
                row.createCell(2).setCellValue(leave.getStartDate().format(formatter));
                row.createCell(3).setCellValue(leave.getEndDate().format(formatter));
                row.createCell(4).setCellValue(leave.getLeaveDays());
                row.createCell(5).setCellValue(leave.getReason());
                row.createCell(6).setCellValue(leave.getStatus().name());
                row.createCell(7).setCellValue(leave.getApprover() != null ? leave.getApprover().getRealName() : "");
                row.createCell(8).setCellValue(leave.getApproveTime() != null ? leave.getApproveTime().format(formatter) : "");
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("导出请假记录失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }
    
    // 辅助方法
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByUsername(authentication.getName());
        }
        return null;
    }
    
    private double calculateLeaveDays(LocalDateTime startDate, LocalDateTime endDate) {
        long days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        // TODO: 可以根据上下午进一步精确计算
        return days;
    }
    
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr.length() == 7) { // YYYY-MM format
            return LocalDateTime.parse(dateStr + "-01 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } else { // YYYY-MM-DD format
            return LocalDateTime.parse(dateStr + " 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }
    
    private void createApprovalFlow(LeaveRequest leaveRequest) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setLeaveRequest(leaveRequest);
        flow.setRequestType("LEAVE");
        flow.setStatus("PENDING");
        flow.setCreateTime(new Date());
        
        // 设置审批人（这里需要根据实际业务逻辑设置）
        // 例如：找到申请人的部门经理
        User manager = findDepartmentManager(leaveRequest.getApplicant());
        if (manager != null) {
            flow.setApprover(manager);
        }
        
        approvalFlowRepository.save(flow);
    }
    
    private void updateApprovalFlow(Long leaveRequestId, String status, String comment) {
        // 更新审批流程状态
        List<ApprovalFlow> flows = approvalFlowRepository.findByLeaveRequestId(leaveRequestId);
        for (ApprovalFlow flow : flows) {
            if ("PENDING".equals(flow.getStatus())) {
                flow.setStatus(status);
                flow.setComment(comment);
                flow.setApprovalTime(new Date());
                approvalFlowRepository.save(flow);
                break;
            }
        }
    }
    
    private User findDepartmentManager(User employee) {
        // TODO: 实现查找部门经理的逻辑
        // 这里简化处理，返回null
        return null;
    }
}
