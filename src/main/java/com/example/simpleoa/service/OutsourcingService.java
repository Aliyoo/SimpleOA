package com.example.simpleoa.service;

import com.example.simpleoa.model.Outsourcing;
import com.example.simpleoa.model.OutsourcingContract;
import com.example.simpleoa.model.OutsourcingProgress;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface OutsourcingService {
    // 外包基本管理
    Outsourcing createOutsourcing(Outsourcing outsourcing);
    Outsourcing updateOutsourcing(Outsourcing outsourcing);
    void deleteOutsourcing(Long id);
    Outsourcing getOutsourcingById(Long id);
    List<Outsourcing> getAllOutsourcing();
    List<Outsourcing> getOutsourcingByProject(Long projectId);
    List<Outsourcing> getOutsourcingByVendor(String vendor);
    List<Outsourcing> getOutsourcingByStatus(String status);
    
    // 外包目标分配
    Outsourcing assignOutsourcing(Long outsourcingId, Long projectId);
    
    // 外包合同管理
    OutsourcingContract createContract(OutsourcingContract contract);
    OutsourcingContract updateContract(OutsourcingContract contract);
    void deleteContract(Long id);
    OutsourcingContract getContractById(Long id);
    List<OutsourcingContract> getContractsByOutsourcing(Long outsourcingId);
    OutsourcingContract getContractByNumber(String contractNumber);
    
    // 外包进度跟踪
    OutsourcingProgress createProgress(OutsourcingProgress progress);
    OutsourcingProgress updateProgress(OutsourcingProgress progress);
    void deleteProgress(Long id);
    OutsourcingProgress getProgressById(Long id);
    List<OutsourcingProgress> getProgressByOutsourcing(Long outsourcingId);
    List<OutsourcingProgress> getProgressByDateRange(Long outsourcingId, Date startDate, Date endDate);
    Map<String, Object> getOutsourcingStats(Long outsourcingId);
}
