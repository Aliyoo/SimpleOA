package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.constants.ExpenseCategory;
import com.example.simpleoa.dto.ReimbursementItemDTO;
import com.example.simpleoa.repository.*;
import com.example.simpleoa.service.ReimbursementService;
import com.example.simpleoa.service.BudgetService;
import com.example.simpleoa.service.ApprovalFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final BudgetRepository budgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetService budgetService;
    private final ApprovalFlowService approvalFlowService;

    public ReimbursementServiceImpl(ReimbursementRequestRepository reimbursementRequestRepository,
                                    UserRepository userRepository,
                                    ProjectRepository projectRepository,
                                    BudgetRepository budgetRepository,
                                    BudgetItemRepository budgetItemRepository,
                                    BudgetService budgetService,
                                    ApprovalFlowService approvalFlowService) {
        this.reimbursementRequestRepository = reimbursementRequestRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.budgetRepository = budgetRepository;
        this.budgetItemRepository = budgetItemRepository;
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
            List<ReimbursementItem> items = convertDtoItemsToEntities(dto.getItems(), request);
            request.setItems(items);
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
            List<ReimbursementItem> items = convertDtoItemsToEntities(dto.getItems(), request);
            request.getItems().addAll(items);
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

        // 更改为待项目经理审批状态（第一阶段）
        request.setStatus(ReimbursementStatus.PENDING_MANAGER_APPROVAL);
        reimbursementRequestRepository.save(request);

        // 创建项目经理审批流程（第一阶段）
        User managerApprover = findManagerApprover(request);
        if (managerApprover != null) {
            approvalFlowService.createReimbursementApproval(request, managerApprover);
            logger.info("Created manager approval flow (stage 1) for reimbursement request {}", id);
        } else {
            logger.error("No manager approver found for reimbursement request {}", id);
            throw new RuntimeException("未找到项目经理审批人");
        }

        logger.info("Reimbursement request {} submitted for approval (stage 1: manager) by user {}", id, submitterId);
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

    @Transactional
    public void processReimbursementBudgetDeduction(ReimbursementRequest request) {
        if (request.getProject() == null || request.getItems() == null || request.getItems().isEmpty()) {
            logger.info("Reimbursement request {} has no project or items, skipping budget deduction", request.getId());
            return;
        }

        for (ReimbursementItem item : request.getItems()) {
            try {
                String referenceNumber = "REIMB-" + request.getId() + "-ITEM-" + item.getId();
                
                // 幂等性检查 - 如果已经处理过该项，跳过
                if (budgetService.isBudgetExpenseExists(referenceNumber)) {
                    logger.info("Budget expense for reimbursement item {} already exists, skipping", item.getId());
                    continue;
                }
                
                if (item.getBudget() != null) {
                    // 创建预算支出记录
                    createBudgetExpenseFromReimbursement(item, request, referenceNumber);
                    // 调用统一更新方法，重新计算预算金额
                    budgetService.updateBudgetAmounts(item.getBudget().getId());
                } else if (item.getBudgetItem() != null) {
                    // 创建预算支出记录
                    createBudgetExpenseFromReimbursementItem(item, request, referenceNumber);
                    // 调用统一更新方法，重新计算预算金额
                    budgetService.updateBudgetAmounts(item.getBudgetItem().getBudget().getId());
                } else {
                    logger.warn("Reimbursement item {} has no budget or budget item association", item.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to process budget deduction for reimbursement item {}: {}", item.getId(), e.getMessage());
                throw new RuntimeException("预算扣减失败: " + e.getMessage(), e);
            }
        }
    }

    private void createBudgetExpenseFromReimbursement(ReimbursementItem item, ReimbursementRequest request, String referenceNumber) {
        try {
            BudgetExpense expense = new BudgetExpense();
            expense.setBudget(item.getBudget());
            expense.setBudgetItem(item.getBudgetItem());
            expense.setAmount(item.getAmount().doubleValue());
            expense.setExpenseDate(new Date());
            expense.setExpenseType("REIMBURSEMENT");
            expense.setReferenceNumber(referenceNumber);
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

    private void createBudgetExpenseFromReimbursementItem(ReimbursementItem item, ReimbursementRequest request, String referenceNumber) {
        try {
            BudgetExpense expense = new BudgetExpense();
            expense.setBudget(item.getBudgetItem().getBudget());
            expense.setBudgetItem(item.getBudgetItem());
            expense.setAmount(item.getAmount().doubleValue());
            expense.setExpenseDate(new Date());
            expense.setExpenseType("REIMBURSEMENT");
            expense.setReferenceNumber(referenceNumber);
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

    /**
     * 查找项目经理（第一阶段审批人）
     */
    private User findManagerApprover(ReimbursementRequest request) {
        // 优先查找 ROLE_PROJECT_MANAGER
        User projectManager = userRepository.findFirstByRoles_Name("ROLE_PROJECT_MANAGER");
        if (projectManager != null) {
            logger.info("Found project manager: {}", projectManager.getUsername());
            return projectManager;
        }

        // 备选: ROLE_MANAGER
        User manager = userRepository.findFirstByRoles_Name("ROLE_MANAGER");
        if (manager != null) {
            logger.info("Using manager as project manager");
            return manager;
        }

        // 最后备选: 管理员
        User admin = userRepository.findFirstByRoles_Name("ROLE_ADMIN");
        if (admin != null) {
            logger.warn("Using admin as project manager (fallback)");
            return admin;
        }

        logger.error("No project manager found for reimbursement request {}", request.getId());
        return null;
    }

    /**
     * 创建领导审批流程（第二阶段）
     * 当项目经理审批通过后自动调用此方法
     */
    private void createLeaderApprovalFlow(ReimbursementRequest request) {
        User leaderApprover = findLeaderApprover(request);
        if (leaderApprover != null) {
            approvalFlowService.createReimbursementApproval(request, leaderApprover);
            logger.info("Created leader approval flow (stage 2) for reimbursement request {}", request.getId());
        } else {
            logger.error("No leader approver found for reimbursement request {}", request.getId());
            throw new RuntimeException("未找到领导审批人");
        }
    }

    /**
     * 根据申请人角色查找对应的领导审批人（第二阶段审批人）
     */
    private User findLeaderApprover(ReimbursementRequest request) {
        User applicant = request.getApplicant();

        // 获取申请人的所有角色
        List<Role> roles = applicant.getRoles();
        if (roles == null || roles.isEmpty()) {
            logger.warn("Applicant {} has no roles", applicant.getId());
            return null;
        }

        // 根据角色找对应领导
        for (Role role : roles) {
            String roleName = role.getName();

            if ("ROLE_SOFTWARE_STAFF".equals(roleName)) {
                User leader = userRepository.findFirstByRoles_Name("ROLE_SOFTWARE_LEADER");
                if (leader != null) {
                    logger.info("Found software leader for software staff: {}", leader.getUsername());
                    return leader;
                }
            }

            if ("ROLE_HARDWARE_STAFF".equals(roleName)) {
                User leader = userRepository.findFirstByRoles_Name("ROLE_HARDWARE_LEADER");
                if (leader != null) {
                    logger.info("Found hardware leader for hardware staff: {}", leader.getUsername());
                    return leader;
                }
            }
        }

        // 如果没找到对应领导，使用项目经理作为备选
        logger.warn("No specific leader found, using project manager as fallback");
        return findManagerApprover(request);
    }

    @Transactional
    public void processApprovedReimbursement(Long reimbursementId) {
        ReimbursementRequest request = getReimbursementById(reimbursementId);
        try {
            // 先检查状态，确保是待财务审查
            if (request.getStatus() != ReimbursementStatus.PENDING_FINANCE_REVIEW) {
                logger.warn("报销申请 {} 状态不正确，当前状态: {}", reimbursementId, request.getStatus());
            }
            
            // 处理预算扣减
            processReimbursementBudgetDeduction(request);
            
            // 更新报销状态为已审批
            request.setStatus(ReimbursementStatus.APPROVED);
            reimbursementRequestRepository.save(request);
            
            logger.info("Processed approved reimbursement {} with budget deduction", reimbursementId);
        } catch (Exception e) {
            logger.error("Failed to process approved reimbursement {}: {}", reimbursementId, e.getMessage());
            throw new RuntimeException("处理报销审批通过失败: " + e.getMessage(), e);
        }
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

        for (ReimbursementItemDTO item : dto.getItems()) {
            if (item.getBudgetId() != null) {
                Budget budget = budgetRepository.findById(item.getBudgetId()).orElse(null);
                if (budget == null) {
                    return false;
                }

                // 校验预算归属项目一致
                if (budget.getProject() == null || !budget.getProject().getId().equals(dto.getProjectId())) {
                    return false;
                }

                // 校验费用类别与预算类型一致（如果前端传了类别）
                if (item.getItemCategory() != null && budget.getBudgetType() != null && !budget.getBudgetType().equals(item.getItemCategory())) {
                    return false;
                }

                // 预算余额校验
                if (!budgetService.checkBudgetAvailability(dto.getProjectId(), item.getAmount().doubleValue())) {
                    return false;
                }
            } else if (item.getBudgetItemId() != null) {
                BudgetItem budgetItem = budgetItemRepository.findById(item.getBudgetItemId()).orElse(null);
                if (budgetItem == null) {
                    return false;
                }

                Budget parentBudget = budgetItem.getBudget();
                if (parentBudget == null || parentBudget.getProject() == null || !parentBudget.getProject().getId().equals(dto.getProjectId())) {
                    return false;
                }

                if (item.getItemCategory() != null) {
                    String category = item.getItemCategory();
                    String itemCategory = budgetItem.getCategory();
                    String parentType = parentBudget.getBudgetType();
                    boolean matchItem = itemCategory != null && itemCategory.equals(category);
                    boolean matchParent = parentType != null && parentType.equals(category);
                    if (!matchItem && !matchParent) {
                        return false;
                    }
                }

                if (!budgetService.checkBudgetItemAvailability(item.getBudgetItemId(), item.getAmount().doubleValue())) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private List<ReimbursementItem> convertDtoItemsToEntities(List<ReimbursementItemDTO> dtoItems, ReimbursementRequest request) {
        List<ReimbursementItem> items = new ArrayList<>();
        
        for (ReimbursementItemDTO dto : dtoItems) {
            ReimbursementItem item = new ReimbursementItem();
            item.setId(dto.getId());
            item.setExpenseDate(dto.getExpenseDate());
            
            // 验证费用类别
            if (dto.getItemCategory() != null && !ExpenseCategory.isValidCategory(dto.getItemCategory())) {
                throw new IllegalArgumentException("无效的费用类别: " + dto.getItemCategory());
            }
            item.setItemCategory(dto.getItemCategory());
            
            item.setDescription(dto.getDescription());
            item.setAmount(dto.getAmount());
            item.setReimbursementRequest(request);
            
            // 转换预算关联
            if (dto.getBudgetId() != null) {
                Budget budget = budgetRepository.findById(dto.getBudgetId())
                    .orElseThrow(() -> new RuntimeException("预算不存在: " + dto.getBudgetId()));
                item.setBudget(budget);
            }
            
            // 转换预算明细关联
            if (dto.getBudgetItemId() != null) {
                BudgetItem budgetItem = budgetItemRepository.findById(dto.getBudgetItemId())
                    .orElseThrow(() -> new RuntimeException("预算明细不存在: " + dto.getBudgetItemId()));
                item.setBudgetItem(budgetItem);
            }
            
            items.add(item);
        }

        return items;
    }

    @Override
    @Transactional
    public ReimbursementRequest financeReview(Long id, String decision, String comment, Long financeUserId) {
        ReimbursementRequest request = getReimbursementById(id);
        User financeUser = userRepository.findById(financeUserId)
                .orElseThrow(() -> new RuntimeException("Finance user not found"));

        // 验证用户是否为财务人员
        boolean isFinance = financeUser.getRoles().stream()
                .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName()));

        if (!isFinance) {
            throw new SecurityException("只有财务人员可以进行财务审查");
        }

        // 验证状态必须是待财务审查
        if (request.getStatus() != ReimbursementStatus.PENDING_FINANCE_REVIEW) {
            throw new IllegalStateException("报销申请状态不正确，当前状态: " + request.getStatus());
        }

        // 处理审查决定
        if ("APPROVE".equalsIgnoreCase(decision)) {
            // 财务审查通过，处理预算扣除
            processReimbursementBudgetDeduction(request);
            request.setStatus(ReimbursementStatus.APPROVED);
            logger.info("Reimbursement request {} finance review approved, budget deducted", id);
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            // 财务审查拒绝
            request.setStatus(ReimbursementStatus.REJECTED);
            request.setComment(comment);
            logger.info("Reimbursement request {} finance review rejected: {}", id, comment);
        } else {
            throw new IllegalArgumentException("无效的审查决定: " + decision);
        }

        reimbursementRequestRepository.save(request);
        return request;
    }

    @Override
    public Page<ReimbursementRequest> getByStatus(ReimbursementStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return reimbursementRequestRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<ReimbursementRequest> getByStatuses(List<ReimbursementStatus> statuses, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return reimbursementRequestRepository.findByStatusIn(statuses, pageable);
    }

    @Override
    public Page<ReimbursementRequest> getForFinanceReview(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));

        // 查询财务需要关注的所有状态：审批中 + 待审查 + 已通过 + 已拒绝
        List<ReimbursementStatus> financeStatuses = Arrays.asList(
                ReimbursementStatus.PENDING_LEADER_APPROVAL,
                ReimbursementStatus.PENDING_FINANCE_REVIEW,
                ReimbursementStatus.APPROVED,
                ReimbursementStatus.REJECTED
        );

        return reimbursementRequestRepository.findByStatusIn(financeStatuses, pageable);
    }

    @Override
    public Map<String, Object> getFinanceStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 使用unpaged获取所有数据
        Pageable unpaged = Pageable.unpaged();

        // 统计待财务审查
        Page<ReimbursementRequest> pendingReviewPage = reimbursementRequestRepository.findByStatus(
                ReimbursementStatus.PENDING_FINANCE_REVIEW, unpaged);
        List<ReimbursementRequest> pendingReview = pendingReviewPage.getContent();
        int pendingReviewCount = pendingReview.size();
        BigDecimal pendingReviewAmount = pendingReview.stream()
                .map(ReimbursementRequest::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 统计待领导审批
        Page<ReimbursementRequest> pendingLeaderPage = reimbursementRequestRepository.findByStatus(
                ReimbursementStatus.PENDING_LEADER_APPROVAL, unpaged);
        List<ReimbursementRequest> pendingLeader = pendingLeaderPage.getContent();
        int pendingLeaderCount = pendingLeader.size();
        BigDecimal pendingLeaderAmount = pendingLeader.stream()
                .map(ReimbursementRequest::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 统计已通过
        Page<ReimbursementRequest> approvedPage = reimbursementRequestRepository.findByStatus(
                ReimbursementStatus.APPROVED, unpaged);
        List<ReimbursementRequest> approved = approvedPage.getContent();
        int approvedCount = approved.size();
        BigDecimal approvedAmount = approved.stream()
                .map(ReimbursementRequest::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 统计已拒绝
        Page<ReimbursementRequest> rejectedPage = reimbursementRequestRepository.findByStatus(
                ReimbursementStatus.REJECTED, unpaged);
        List<ReimbursementRequest> rejected = rejectedPage.getContent();
        int rejectedCount = rejected.size();
        BigDecimal rejectedAmount = rejected.stream()
                .map(ReimbursementRequest::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算总计
        int totalCount = pendingReviewCount + pendingLeaderCount + approvedCount + rejectedCount;
        BigDecimal totalAmount = pendingReviewAmount.add(pendingLeaderAmount)
                .add(approvedAmount).add(rejectedAmount);

        // 计算平均金额
        BigDecimal averageAmount = totalCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 组装结果
        result.put("pendingReviewCount", pendingReviewCount);
        result.put("pendingReviewAmount", pendingReviewAmount);
        result.put("pendingLeaderCount", pendingLeaderCount);
        result.put("pendingLeaderAmount", pendingLeaderAmount);
        result.put("approvedCount", approvedCount);
        result.put("approvedAmount", approvedAmount);
        result.put("rejectedCount", rejectedCount);
        result.put("rejectedAmount", rejectedAmount);
        result.put("totalCount", totalCount);
        result.put("totalAmount", totalAmount);
        result.put("averageAmount", averageAmount);

        return result;
    }
}
