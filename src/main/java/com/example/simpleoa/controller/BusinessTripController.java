package com.example.simpleoa.controller;

import com.example.simpleoa.model.BusinessTripRequest;
import com.example.simpleoa.service.BusinessTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

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
    
    @GetMapping("/list")
    public List<BusinessTripRequest> getTravelList() {
        return (List<BusinessTripRequest>) businessTripService.getAllBusinessTrips();
    }
}