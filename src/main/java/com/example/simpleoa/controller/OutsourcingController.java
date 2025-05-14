package com.example.simpleoa.controller;

import com.example.simpleoa.model.Outsourcing;
import com.example.simpleoa.model.OutsourcingContract;
import com.example.simpleoa.model.OutsourcingProgress;
import com.example.simpleoa.service.OutsourcingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outsourcing")
public class OutsourcingController {
    private final OutsourcingService outsourcingService;

    @Autowired
    public OutsourcingController(OutsourcingService outsourcingService) {
        this.outsourcingService = outsourcingService;
    }

    // 外包信息录入
    @PostMapping
    public Outsourcing createOutsourcing(@RequestBody Outsourcing outsourcing) {
        return outsourcingService.createOutsourcing(outsourcing);
    }

    @PutMapping("/{id}")
    public Outsourcing updateOutsourcing(@PathVariable Long id, @RequestBody Outsourcing outsourcing) {
        outsourcing.setId(id);
        return outsourcingService.updateOutsourcing(outsourcing);
    }

    @DeleteMapping("/{id}")
    public void deleteOutsourcing(@PathVariable Long id) {
        outsourcingService.deleteOutsourcing(id);
    }

    @GetMapping("/{id}")
    public Outsourcing getOutsourcingById(@PathVariable Long id) {
        return outsourcingService.getOutsourcingById(id);
    }

    @GetMapping
    public List<Outsourcing> getAllOutsourcing() {
        return outsourcingService.getAllOutsourcing();
    }

    @GetMapping("/project/{projectId}")
    public List<Outsourcing> getOutsourcingByProject(@PathVariable Long projectId) {
        return outsourcingService.getOutsourcingByProject(projectId);
    }

    @GetMapping("/vendor/{vendor}")
    public List<Outsourcing> getOutsourcingByVendor(@PathVariable String vendor) {
        return outsourcingService.getOutsourcingByVendor(vendor);
    }

    @GetMapping("/status/{status}")
    public List<Outsourcing> getOutsourcingByStatus(@PathVariable String status) {
        return outsourcingService.getOutsourcingByStatus(status);
    }

    // 外包目标分配
    @PostMapping("/{id}/assign")
    public Outsourcing assignOutsourcing(@PathVariable Long id, @RequestParam Long projectId) {
        return outsourcingService.assignOutsourcing(id, projectId);
    }

    // 外包合同管理
    @PostMapping("/contract")
    public OutsourcingContract createContract(@RequestBody OutsourcingContract contract) {
        return outsourcingService.createContract(contract);
    }

    @PutMapping("/contract/{id}")
    public OutsourcingContract updateContract(@PathVariable Long id, @RequestBody OutsourcingContract contract) {
        contract.setId(id);
        return outsourcingService.updateContract(contract);
    }

    @DeleteMapping("/contract/{id}")
    public void deleteContract(@PathVariable Long id) {
        outsourcingService.deleteContract(id);
    }

    @GetMapping("/contract/{id}")
    public OutsourcingContract getContractById(@PathVariable Long id) {
        return outsourcingService.getContractById(id);
    }

    @GetMapping("/{outsourcingId}/contracts")
    public List<OutsourcingContract> getContractsByOutsourcing(@PathVariable Long outsourcingId) {
        return outsourcingService.getContractsByOutsourcing(outsourcingId);
    }

    @GetMapping("/contract/number/{contractNumber}")
    public OutsourcingContract getContractByNumber(@PathVariable String contractNumber) {
        return outsourcingService.getContractByNumber(contractNumber);
    }

    // 外包进度跟踪
    @PostMapping("/{id}/progress")
    public OutsourcingProgress createProgress(@PathVariable Long id, @RequestBody OutsourcingProgress progress) {
        Outsourcing outsourcing = new Outsourcing();
        outsourcing.setId(id);
        progress.setOutsourcing(outsourcing);
        return outsourcingService.createProgress(progress);
    }

    @PutMapping("/progress/{id}")
    public OutsourcingProgress updateProgress(@PathVariable Long id, @RequestBody OutsourcingProgress progress) {
        progress.setId(id);
        return outsourcingService.updateProgress(progress);
    }

    @DeleteMapping("/progress/{id}")
    public void deleteProgress(@PathVariable Long id) {
        outsourcingService.deleteProgress(id);
    }

    @GetMapping("/progress/{id}")
    public OutsourcingProgress getProgressById(@PathVariable Long id) {
        return outsourcingService.getProgressById(id);
    }

    @GetMapping("/{outsourcingId}/progress-list")
    public List<OutsourcingProgress> getProgressByOutsourcing(@PathVariable Long outsourcingId) {
        return outsourcingService.getProgressByOutsourcing(outsourcingId);
    }

    @GetMapping("/{outsourcingId}/progress/date-range")
    public List<OutsourcingProgress> getProgressByDateRange(
            @PathVariable Long outsourcingId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return outsourcingService.getProgressByDateRange(outsourcingId, startDate, endDate);
    }

    @GetMapping("/{outsourcingId}/stats")
    public Map<String, Object> getOutsourcingStats(@PathVariable Long outsourcingId) {
        return outsourcingService.getOutsourcingStats(outsourcingId);
    }
}
