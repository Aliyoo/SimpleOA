package com.example.simpleoa.controller;

import com.example.simpleoa.dto.BusinessTripRequestDTO;
import com.example.simpleoa.model.BusinessTripRequest;
import com.example.simpleoa.service.BusinessTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/travel")
public class BusinessTripController {
    private final BusinessTripService businessTripService;

    @Autowired
    public BusinessTripController(BusinessTripService businessTripService) {
        this.businessTripService = businessTripService;
    }

    
    @GetMapping("/statistics")
    public ResponseEntity<?> getBusinessTripStatistics(@RequestParam String startDate, @RequestParam String endDate) {
        return ResponseEntity.ok(businessTripService.getBusinessTripStatistics(startDate, endDate));
    }

    @PostMapping
    public BusinessTripRequest createBusinessTrip(@RequestBody BusinessTripRequest businessTrip) {
        return businessTripService.createBusinessTrip(businessTrip);
    }

    @PutMapping("/{id}")
    public BusinessTripRequest updateBusinessTrip(@PathVariable Long id, @RequestBody BusinessTripRequest businessTrip) {
        businessTrip.setId(id);
        return businessTripService.updateBusinessTrip(businessTrip);
    }

    @DeleteMapping("/{id}")
    public void deleteBusinessTrip(@PathVariable Long id) {
        businessTripService.deleteBusinessTrip(id);
    }

    @GetMapping("/{id}")
    public BusinessTripRequest getBusinessTripById(@PathVariable Long id) {
        return businessTripService.getBusinessTripById(id);
    }

    @GetMapping
    public List<BusinessTripRequest> getAllBusinessTrips() {
        return (List<BusinessTripRequest>) businessTripService.getAllBusinessTrips();
    }

    @GetMapping("/user/{userId}")
    public List<BusinessTripRequest> getBusinessTripsByUser(@PathVariable Long userId) {
        return (List<BusinessTripRequest>) businessTripService.getBusinessTripsByUser(userId);
    }

    // 前端需要的API接口
    @PostMapping("/apply")
    public BusinessTripRequest applyBusinessTrip(@RequestBody BusinessTripRequest businessTrip) {
        return businessTripService.createBusinessTrip(businessTrip);
    }
    
    // 新的DTO版本API
    @PostMapping("/create")
    public ResponseEntity<BusinessTripRequest> createBusinessTripWithDTO(@RequestBody BusinessTripRequestDTO dto) {
        try {
            // 获取当前用户ID（这里需要从安全上下文获取）
            Long applicantId = getCurrentUserId();
            BusinessTripRequest result = businessTripService.createBusinessTrip(dto, applicantId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}/dto")
    public ResponseEntity<BusinessTripRequest> updateBusinessTripWithDTO(@PathVariable Long id, @RequestBody BusinessTripRequestDTO dto) {
        try {
            BusinessTripRequest result = businessTripService.updateBusinessTrip(id, dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/submit")
    public ResponseEntity<BusinessTripRequest> submitBusinessTrip(@PathVariable Long id) {
        try {
            BusinessTripRequest result = businessTripService.submitBusinessTrip(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<BusinessTripRequest> rejectBusinessTrip(@PathVariable Long id, @RequestBody String comment) {
        try {
            BusinessTripRequest result = businessTripService.rejectBusinessTrip(id, comment);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/project/{projectId}")
    public List<BusinessTripRequest> getBusinessTripsByProject(@PathVariable Long projectId) {
        return businessTripService.getBusinessTripsByProject(projectId);
    }
    
    @GetMapping("/list")
    public List<BusinessTripRequest> getTravelList() {
        return (List<BusinessTripRequest>) businessTripService.getAllBusinessTrips();
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            // 这里需要根据用户名获取用户ID
            // 暂时返回1L作为测试
            return 1L;
        }
        return 1L;
    }
}