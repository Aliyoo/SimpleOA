package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ReimbursementService;
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
    private final ProjectRepository projectRepository; // Added ProjectRepository

    public ReimbursementServiceImpl(ReimbursementRequestRepository reimbursementRequestRepository,
                                    UserRepository userRepository,
                                    ProjectRepository projectRepository) { // Added to constructor
        this.reimbursementRequestRepository = reimbursementRequestRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository; // Initialize ProjectRepository
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
    public ReimbursementRequest approveOrReject(Long id, String decision, String comment, Long approverId) {
        ReimbursementRequest request = getReimbursementById(id);
        User approver = userRepository.findById(approverId).orElseThrow(() -> new RuntimeException("User not found"));

        boolean isManager = approver.getRoles().stream().anyMatch(role -> "ROLE_MANAGER".equals(role.getName()));
        boolean isFinance = approver.getRoles().stream().anyMatch(role -> "ROLE_FINANCE".equals(role.getName()));

        switch (request.getStatus()) {
            case PENDING_MANAGER_APPROVAL:
                if (!isManager) {
                    throw new SecurityException("User does not have permission to approve as a manager.");
                }
                if ("approve".equalsIgnoreCase(decision)) {
                    request.setStatus(ReimbursementStatus.PENDING_FINANCE_APPROVAL);
                } else {
                    request.setStatus(ReimbursementStatus.REJECTED);
                }
                break;
            case PENDING_FINANCE_APPROVAL:
                if (!isFinance) {
                    throw new SecurityException("User does not have permission to approve as finance.");
                }
                if ("approve".equalsIgnoreCase(decision)) {
                    request.setStatus(ReimbursementStatus.APPROVED);
                } else {
                    request.setStatus(ReimbursementStatus.REJECTED);
                }
                break;
            default:
                throw new IllegalStateException("Request is not in a state that can be approved or rejected.");
        }

        request.setComment(comment);
        return reimbursementRequestRepository.save(request);
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
}
