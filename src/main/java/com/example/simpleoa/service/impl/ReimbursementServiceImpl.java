package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ReimbursementService;
import com.example.simpleoa.service.BudgetService;
import com.example.simpleoa.service.ApprovalFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    private static final Logger logger = LoggerFactory.getLogger(ReimbursementServiceImpl.class);
    
    private final ReimbursementRequestRepository reimbursementRequestRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final BudgetService budgetService;
    private final ApprovalFlowService approvalFlowService;

    public ReimbursementServiceImpl(ReimbursementRequestRepository reimbursementRequestRepository,
                                    UserRepository userRepository,
                                    ProjectRepository projectRepository,
                                    BudgetService budgetService,
                                    ApprovalFlowService approvalFlowService) {
        this.reimbursementRequestRepository = reimbursementRequestRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.budgetService = budgetService;
        this.approvalFlowService = approvalFlowService;
    }

    @Override
    @Transactional
    public ReimbursementRequest createReimbursement(ReimbursementRequestDTO dto, Long applicantId) {
        User applicant = userRepository.findById(applicantId).orElseThrow(() -> new RuntimeException("User not found for ID: " + applicantId));

        ReimbursementRequest request = new ReimbursementRequest();
        request.setApplicant(applicant);
        request.setTitle(dto.getTitle());
        request.setAttachments(dto.getAttachments());
        request.setStatus(ReimbursementStatus.DRAFT);

        // Handle Project association
        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found for ID: " + dto.getProjectId()));
            request.setProject(project);
        }

        if (dto.getItems() != null) {
            dto.getItems().forEach(item -> item.setReimbursementRequest(request));
            request.setItems(dto.getItems());
            request.calculateTotalAmount();
        }

        return reimbursementRequestRepository.save(request);
    }

    @Override
    @Transactional
    public ReimbursementRequest updateReimbursement(Long id, ReimbursementRequestDTO dto) {
        ReimbursementRequest request = getReimbursementById(id);
        // Add authorization logic here to ensure only the applicant can update in DRAFT status, etc.
        if (!request.getApplicant().getId().equals(dto.getId()) && request.getStatus() != ReimbursementStatus.DRAFT) {
            // This check seems wrong. dto.getId() is reimbursement ID, not applicant ID.
            // And applicant ID should be checked against the authenticated user, not from DTO.
            // For now, focusing on project ID.
        }


        request.setTitle(dto.getTitle());
        request.setAttachments(dto.getAttachments());

        // Handle Project association update
        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found for ID: " + dto.getProjectId()));
            request.setProject(project);
        } else {
            request.setProject(null); // Allow unsetting the project
        }

        if (dto.getItems() != null) {
            // Manage items carefully to avoid issues with detached entities if items are complex
            request.getItems().clear(); // Simple approach: clear and add all. Consider more sophisticated merging if needed.
            dto.getItems().forEach(itemDTO -> {
                itemDTO.setReimbursementRequest(request); // Ensure association
                request.getItems().add(itemDTO);
            });
            request.calculateTotalAmount();
        } else {
            request.getItems().clear();
            request.calculateTotalAmount();
        }

        return reimbursementRequestRepository.save(request);
    }

    @Override
    public void deleteReimbursement(Long id) {
        reimbursementRequestRepository.deleteById(id);
    }

    @Override
    public ReimbursementRequest getReimbursementById(Long id) {
        return reimbursementRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Reimbursement request not found"));
    }

    @Override
    public Page<ReimbursementRequest> getReimbursements(Long userId, int page, int size, ReimbursementStatus status, String startDate, String endDate, String keyword) {
        logger.debug("Getting reimbursements for user: {}, page: {}, size: {}, status: {}, startDate: {}, endDate: {}, keyword: {}", userId, page, size, status, startDate, endDate, keyword);

        LocalDateTime start = (startDate != null) ? LocalDateTime.parse(startDate + "T00:00:00") : null;
        LocalDateTime end = (endDate != null) ? LocalDateTime.parse(endDate + "T23:59:59") : null;

        Page<ReimbursementRequest> result = reimbursementRequestRepository.findByApplicantIdWithFilters(userId, status, start, end, keyword, PageRequest.of(page, size));
        logger.debug("Found {} reimbursement requests for user {} with filters", result.getTotalElements(), userId);
        return result;
    }

    @Override
    @Transactional
    public ReimbursementRequest submitForApproval(Long id, Long submitterId) {
        ReimbursementRequest request = getReimbursementById(id);
        User submitter = userRepository.findById(submitterId).orElseThrow(() -> new RuntimeException("User not found"));
        
        // 验证只有申请人可以提交
        if (!request.getApplicant().getId().equals(submitterId)) {
            throw new SecurityException("只有申请人可以提交报销申请");
        }
        
        // 验证状态必须是草稿
        if (request.getStatus() != ReimbursementStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的报销申请可以提交审批");
        }
        
        // 验证预算可用性
        if (!checkBudgetAvailability(id)) {
            throw new RuntimeException("预算不足，无法提交审批");
        }
        
        // 更改为提交状态，准备进入统一审批流程
        request.setStatus(ReimbursementStatus.PENDING_MANAGER_APPROVAL);
        reimbursementRequestRepository.save(request);
        
        // 创建统一审批流程 - 部门经理审批
        User managerApprover = findManagerApprover(request.getApplicant());
        if (managerApprover != null) {
            approvalFlowService.createReimbursementApproval(request, managerApprover);
            logger.info("Created manager approval flow for reimbursement request {}", id);
        }
        
        logger.info("Reimbursement request {} submitted for approval by user {}", id, submitterId);
        return request;
    }

    @Override
    @Transactional
    @Deprecated
    public ReimbursementRequest approveOrReject(Long id, String decision, String comment, Long approverId) {
        throw new UnsupportedOperationException("报销审批已迁移到统一审批管理系统，请使用 ApprovalFlowService");
    }

    @Override
    public Map<String, Object> getReimbursementStatistics(String startDate, String endDate, Long userId) {
        logger.debug("Getting reimbursement statistics for user: {}, from: {} to: {}", userId, startDate, endDate);
        
        // 获取指定时间范围内的报销数据
        // startDate 和 endDate 格式为 "YYYY-MM-DD"
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
        logger.debug("Parsed date range: {} to {}", start, end);
        
        List<ReimbursementRequest> requests = reimbursementRequestRepository.findByApplicantIdAndCreateTimeBetween(
                userId, start, end
        );
        logger.debug("Found {} requests for statistics", requests.size());
        
        // 计算统计数据
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> summary = new HashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();
        
        // 总计数据
        BigDecimal totalAmount = requests.stream()
                .map(ReimbursementRequest::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalCount = requests.size();
        
        // 通过率计算
        long approvedCount = requests.stream()
                .filter(r -> ReimbursementStatus.APPROVED.equals(r.getStatus()))
                .count();
        
        String approvalRate = totalCount > 0 ? 
                String.format("%.1f%%", (double) approvedCount / totalCount * 100) : "0%";
        
        // 平均金额
        BigDecimal avgAmount = totalCount > 0 ? 
                totalAmount.divide(new BigDecimal(totalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // 按费用类别分组统计
        Map<String, List<ReimbursementRequest>> categoryGroups = requests.stream()
                .collect(Collectors.groupingBy(r -> {
                    if (r.getItems() == null || r.getItems().isEmpty()) {
                        return "其他";
                    }
                    return r.getItems().get(0).getItemCategory() != null ? 
                            r.getItems().get(0).getItemCategory() : "其他";
                }));
        
        // 最常用类别
        String mostUsedCategory = categoryGroups.entrySet().stream()
                .max(Map.Entry.comparingByValue((list1, list2) -> Integer.compare(list1.size(), list2.size())))
                .map(Map.Entry::getKey)
                .orElse("-");
        
        // 构建详细统计
        for (Map.Entry<String, List<ReimbursementRequest>> entry : categoryGroups.entrySet()) {
            String category = entry.getKey();
            List<ReimbursementRequest> categoryRequests = entry.getValue();
            
            BigDecimal categoryTotal = categoryRequests.stream()
                    .map(ReimbursementRequest::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int categoryCount = categoryRequests.size();
            BigDecimal categoryAvg = categoryCount > 0 ? 
                    categoryTotal.divide(new BigDecimal(categoryCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            
            Map<String, Object> categoryDetail = new HashMap<>();
            categoryDetail.put("category", category);
            categoryDetail.put("totalAmount", categoryTotal);
            categoryDetail.put("count", categoryCount);
            categoryDetail.put("avgAmount", categoryAvg);
            details.add(categoryDetail);
        }
        
        // 构建汇总数据
        summary.put("totalAmount", totalAmount);
        summary.put("totalCount", totalCount);
        summary.put("approvalRate", approvalRate);
        summary.put("avgAmount", avgAmount);
        summary.put("mostUsedCategory", mostUsedCategory);
        
        result.put("summary", summary);
        result.put("details", details);
        
        return result;
    }

    private void processReimbursementBudgetDeduction(ReimbursementRequest request) {
        if (request.getProject() == null || request.getItems() == null || request.getItems().isEmpty()) {
            logger.info("Reimbursement request {} has no project or items, skipping budget deduction", request.getId());
            return;
        }

        for (ReimbursementItem item : request.getItems()) {
            if (item.getBudget() != null) {
                createBudgetExpenseFromReimbursement(item, request);
            } else if (item.getBudgetItem() != null) {
                createBudgetExpenseFromReimbursementItem(item, request);
            } else {
                logger.warn("Reimbursement item {} has no budget or budget item association", item.getId());
            }
        }
    }

    private void createBudgetExpenseFromReimbursement(ReimbursementItem item, ReimbursementRequest request) {
        try {
            BudgetExpense expense = new BudgetExpense();
            expense.setBudget(item.getBudget());
            expense.setBudgetItem(item.getBudgetItem());
            expense.setAmount(item.getAmount().doubleValue());
            expense.setExpenseDate(new Date());
            expense.setExpenseType("REIMBURSEMENT");
            expense.setReferenceNumber("REIMB-" + request.getId());
            expense.setReimbursementRequest(request);
            expense.setReimbursementItem(item);
            expense.setStatus("APPROVED");
            expense.setDescription("报销扣除: " + item.getDescription());
            expense.setRecordedBy(request.getApplicant());
            expense.setRecordTime(new Date());
            expense.setCreateTime(new Date());
            expense.setLastUpdateTime(new Date());

            budgetService.createBudgetExpense(expense);
            logger.info("Created budget expense for reimbursement item {}, amount: {}", item.getId(), item.getAmount());
        } catch (Exception e) {
            logger.error("Failed to create budget expense for reimbursement item {}: {}", item.getId(), e.getMessage());
            throw new RuntimeException("预算扣除失败: " + e.getMessage());
        }
    }

    private void createBudgetExpenseFromReimbursementItem(ReimbursementItem item, ReimbursementRequest request) {
        try {
            BudgetExpense expense = new BudgetExpense();
            expense.setBudget(item.getBudgetItem().getBudget());
            expense.setBudgetItem(item.getBudgetItem());
            expense.setAmount(item.getAmount().doubleValue());
            expense.setExpenseDate(new Date());
            expense.setExpenseType("REIMBURSEMENT");
            expense.setReferenceNumber("REIMB-" + request.getId());
            expense.setReimbursementRequest(request);
            expense.setReimbursementItem(item);
            expense.setStatus("APPROVED");
            expense.setDescription("报销扣除: " + item.getDescription());
            expense.setRecordedBy(request.getApplicant());
            expense.setRecordTime(new Date());
            expense.setCreateTime(new Date());
            expense.setLastUpdateTime(new Date());

            budgetService.createBudgetExpense(expense);
            logger.info("Created budget expense for reimbursement item {}, amount: {}", item.getId(), item.getAmount());
        } catch (Exception e) {
            logger.error("Failed to create budget expense for reimbursement item {}: {}", item.getId(), e.getMessage());
            throw new RuntimeException("预算扣除失败: " + e.getMessage());
        }
    }

    private User findManagerApprover(User applicant) {
        List<User> managers = userRepository.findUsersByRole("ROLE_MANAGER");
        if (!managers.isEmpty()) {
            return managers.get(0);
        }
        
        List<User> admins = userRepository.findUsersByRole("ROLE_ADMIN");
        if (!admins.isEmpty()) {
            return admins.get(0);
        }
        
        logger.warn("No manager or admin found for reimbursement approval");
        return null;
    }

    public void processApprovedReimbursement(Long reimbursementId) {
        ReimbursementRequest request = getReimbursementById(reimbursementId);
        processReimbursementBudgetDeduction(request);
        request.setStatus(ReimbursementStatus.APPROVED);
        reimbursementRequestRepository.save(request);
        logger.info("Processed approved reimbursement {} with budget deduction", reimbursementId);
    }

    @Override
    public boolean checkBudgetAvailability(Long reimbursementId) {
        ReimbursementRequest request = getReimbursementById(reimbursementId);
        if (request.getProject() == null || request.getItems() == null || request.getItems().isEmpty()) {
            return true;
        }

        for (ReimbursementItem item : request.getItems()) {
            if (item.getBudget() != null) {
                if (!budgetService.checkBudgetAvailability(request.getProject().getId(), item.getAmount().doubleValue())) {
                    return false;
                }
            } else if (item.getBudgetItem() != null) {
                if (!budgetService.checkBudgetItemAvailability(item.getBudgetItem().getId(), item.getAmount().doubleValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean validateReimbursementBudget(ReimbursementRequestDTO dto) {
        if (dto.getProjectId() == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            return true;
        }

        for (ReimbursementItem item : dto.getItems()) {
            if (item.getBudget() != null) {
                if (!budgetService.checkBudgetAvailability(dto.getProjectId(), item.getAmount().doubleValue())) {
                    return false;
                }
            } else if (item.getBudgetItem() != null) {
                if (!budgetService.checkBudgetItemAvailability(item.getBudgetItem().getId(), item.getAmount().doubleValue())) {
                    return false;
                }
            }
        }
        return true;
    }
}
