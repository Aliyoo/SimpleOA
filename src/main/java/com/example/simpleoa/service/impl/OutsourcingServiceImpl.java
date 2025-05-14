package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Outsourcing;
import com.example.simpleoa.model.OutsourcingContract;
import com.example.simpleoa.model.OutsourcingProgress;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.repository.OutsourcingContractRepository;
import com.example.simpleoa.repository.OutsourcingProgressRepository;
import com.example.simpleoa.repository.OutsourcingRepository;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.service.OutsourcingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OutsourcingServiceImpl implements OutsourcingService {

    private final OutsourcingRepository outsourcingRepository;
    private final OutsourcingContractRepository contractRepository;
    private final OutsourcingProgressRepository progressRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public OutsourcingServiceImpl(
            OutsourcingRepository outsourcingRepository,
            OutsourcingContractRepository contractRepository,
            OutsourcingProgressRepository progressRepository,
            ProjectRepository projectRepository) {
        this.outsourcingRepository = outsourcingRepository;
        this.contractRepository = contractRepository;
        this.progressRepository = progressRepository;
        this.projectRepository = projectRepository;
    }

    // 外包基本管理
    @Override
    @Transactional
    public Outsourcing createOutsourcing(Outsourcing outsourcing) {
        if (outsourcing.getProject() != null && outsourcing.getProject().getId() != null) {
            Project project = projectRepository.findById(outsourcing.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            outsourcing.setProject(project);
        }
        
        if (outsourcing.getCompletionPercentage() == null) {
            outsourcing.setCompletionPercentage(0);
        }
        
        if (outsourcing.getStatus() == null) {
            outsourcing.setStatus("新建");
        }
        
        return outsourcingRepository.save(outsourcing);
    }

    @Override
    @Transactional
    public Outsourcing updateOutsourcing(Outsourcing outsourcing) {
        Outsourcing existingOutsourcing = outsourcingRepository.findById(outsourcing.getId())
                .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
        
        if (outsourcing.getProject() != null && outsourcing.getProject().getId() != null) {
            Project project = projectRepository.findById(outsourcing.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            existingOutsourcing.setProject(project);
        }
        
        existingOutsourcing.setName(outsourcing.getName());
        existingOutsourcing.setVendor(outsourcing.getVendor());
        existingOutsourcing.setContactPerson(outsourcing.getContactPerson());
        existingOutsourcing.setContactPhone(outsourcing.getContactPhone());
        existingOutsourcing.setContactEmail(outsourcing.getContactEmail());
        existingOutsourcing.setDescription(outsourcing.getDescription());
        existingOutsourcing.setBudget(outsourcing.getBudget());
        existingOutsourcing.setStartDate(outsourcing.getStartDate());
        existingOutsourcing.setEndDate(outsourcing.getEndDate());
        existingOutsourcing.setStatus(outsourcing.getStatus());
        existingOutsourcing.setCompletionPercentage(outsourcing.getCompletionPercentage());
        
        return outsourcingRepository.save(existingOutsourcing);
    }

    @Override
    @Transactional
    public void deleteOutsourcing(Long id) {
        Outsourcing outsourcing = outsourcingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
        
        // 删除相关的进度记录
        List<OutsourcingProgress> progressList = progressRepository.findByOutsourcingId(id);
        progressRepository.deleteAll(progressList);
        
        // 删除相关的合同
        List<OutsourcingContract> contractList = contractRepository.findByOutsourcingId(id);
        contractRepository.deleteAll(contractList);
        
        // 删除外包记录
        outsourcingRepository.delete(outsourcing);
    }

    @Override
    public Outsourcing getOutsourcingById(Long id) {
        return outsourcingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
    }

    @Override
    public List<Outsourcing> getAllOutsourcing() {
        return outsourcingRepository.findAll();
    }

    @Override
    public List<Outsourcing> getOutsourcingByProject(Long projectId) {
        return outsourcingRepository.findByProjectId(projectId);
    }

    @Override
    public List<Outsourcing> getOutsourcingByVendor(String vendor) {
        return outsourcingRepository.findByVendor(vendor);
    }

    @Override
    public List<Outsourcing> getOutsourcingByStatus(String status) {
        return outsourcingRepository.findByStatus(status);
    }

    // 外包目标分配
    @Override
    @Transactional
    public Outsourcing assignOutsourcing(Long outsourcingId, Long projectId) {
        Outsourcing outsourcing = outsourcingRepository.findById(outsourcingId)
                .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        
        outsourcing.setProject(project);
        return outsourcingRepository.save(outsourcing);
    }

    // 外包合同管理
    @Override
    @Transactional
    public OutsourcingContract createContract(OutsourcingContract contract) {
        if (contract.getOutsourcing() != null && contract.getOutsourcing().getId() != null) {
            Outsourcing outsourcing = outsourcingRepository.findById(contract.getOutsourcing().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
            contract.setOutsourcing(outsourcing);
        }
        
        if (contract.getPaidAmount() == null) {
            contract.setPaidAmount(0.0);
        }
        
        if (contract.getStatus() == null) {
            contract.setStatus("有效");
        }
        
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public OutsourcingContract updateContract(OutsourcingContract contract) {
        OutsourcingContract existingContract = contractRepository.findById(contract.getId())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        
        if (contract.getOutsourcing() != null && contract.getOutsourcing().getId() != null) {
            Outsourcing outsourcing = outsourcingRepository.findById(contract.getOutsourcing().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
            existingContract.setOutsourcing(outsourcing);
        }
        
        existingContract.setContractNumber(contract.getContractNumber());
        existingContract.setSignDate(contract.getSignDate());
        existingContract.setEffectiveDate(contract.getEffectiveDate());
        existingContract.setExpirationDate(contract.getExpirationDate());
        existingContract.setTotalAmount(contract.getTotalAmount());
        existingContract.setPaidAmount(contract.getPaidAmount());
        existingContract.setTerms(contract.getTerms());
        existingContract.setStatus(contract.getStatus());
        existingContract.setAttachmentPath(contract.getAttachmentPath());
        
        return contractRepository.save(existingContract);
    }

    @Override
    @Transactional
    public void deleteContract(Long id) {
        OutsourcingContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        contractRepository.delete(contract);
    }

    @Override
    public OutsourcingContract getContractById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
    }

    @Override
    public List<OutsourcingContract> getContractsByOutsourcing(Long outsourcingId) {
        return contractRepository.findByOutsourcingId(outsourcingId);
    }

    @Override
    public OutsourcingContract getContractByNumber(String contractNumber) {
        return contractRepository.findByContractNumber(contractNumber)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
    }

    // 外包进度跟踪
    @Override
    @Transactional
    public OutsourcingProgress createProgress(OutsourcingProgress progress) {
        if (progress.getOutsourcing() != null && progress.getOutsourcing().getId() != null) {
            Outsourcing outsourcing = outsourcingRepository.findById(progress.getOutsourcing().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
            progress.setOutsourcing(outsourcing);
            
            // 更新外包项目的完成百分比
            outsourcing.setCompletionPercentage(progress.getCompletionPercentage());
            outsourcingRepository.save(outsourcing);
        }
        
        return progressRepository.save(progress);
    }

    @Override
    @Transactional
    public OutsourcingProgress updateProgress(OutsourcingProgress progress) {
        OutsourcingProgress existingProgress = progressRepository.findById(progress.getId())
                .orElseThrow(() -> new EntityNotFoundException("Progress not found"));
        
        existingProgress.setReportDate(progress.getReportDate());
        existingProgress.setCompletionPercentage(progress.getCompletionPercentage());
        existingProgress.setProgressDescription(progress.getProgressDescription());
        existingProgress.setMilestone(progress.getMilestone());
        existingProgress.setIssues(progress.getIssues());
        existingProgress.setNextSteps(progress.getNextSteps());
        existingProgress.setReportedBy(progress.getReportedBy());
        
        // 更新外包项目的完成百分比
        if (existingProgress.getOutsourcing() != null) {
            Outsourcing outsourcing = existingProgress.getOutsourcing();
            outsourcing.setCompletionPercentage(progress.getCompletionPercentage());
            outsourcingRepository.save(outsourcing);
        }
        
        return progressRepository.save(existingProgress);
    }

    @Override
    @Transactional
    public void deleteProgress(Long id) {
        OutsourcingProgress progress = progressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Progress not found"));
        progressRepository.delete(progress);
    }

    @Override
    public OutsourcingProgress getProgressById(Long id) {
        return progressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Progress not found"));
    }

    @Override
    public List<OutsourcingProgress> getProgressByOutsourcing(Long outsourcingId) {
        return progressRepository.findByOutsourcingId(outsourcingId);
    }

    @Override
    public List<OutsourcingProgress> getProgressByDateRange(Long outsourcingId, Date startDate, Date endDate) {
        return progressRepository.findByOutsourcingIdAndReportDateBetween(outsourcingId, startDate, endDate);
    }

    @Override
    public Map<String, Object> getOutsourcingStats(Long outsourcingId) {
        Outsourcing outsourcing = outsourcingRepository.findById(outsourcingId)
                .orElseThrow(() -> new EntityNotFoundException("Outsourcing not found"));
        
        List<OutsourcingContract> contracts = contractRepository.findByOutsourcingId(outsourcingId);
        List<OutsourcingProgress> progressList = progressRepository.findByOutsourcingId(outsourcingId);
        
        // 计算合同总金额和已支付金额
        double totalContractAmount = 0.0;
        double totalPaidAmount = 0.0;
        for (OutsourcingContract contract : contracts) {
            totalContractAmount += contract.getTotalAmount();
            totalPaidAmount += contract.getPaidAmount();
        }
        
        // 计算进度统计
        Map<String, Object> progressStats = new HashMap<>();
        if (!progressList.isEmpty()) {
            // 按时间排序
            progressList.sort(Comparator.comparing(OutsourcingProgress::getReportDate));
            
            // 提取最新进度
            OutsourcingProgress latestProgress = progressList.get(progressList.size() - 1);
            progressStats.put("latestProgress", latestProgress);
            
            // 计算进度变化率
            if (progressList.size() > 1) {
                OutsourcingProgress previousProgress = progressList.get(progressList.size() - 2);
                int progressChange = latestProgress.getCompletionPercentage() - previousProgress.getCompletionPercentage();
                progressStats.put("progressChange", progressChange);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("outsourcing", outsourcing);
        result.put("contractCount", contracts.size());
        result.put("totalContractAmount", totalContractAmount);
        result.put("totalPaidAmount", totalPaidAmount);
        result.put("paymentPercentage", totalContractAmount > 0 ? (totalPaidAmount / totalContractAmount) * 100 : 0);
        result.put("progressStats", progressStats);
        
        return result;
    }
}
