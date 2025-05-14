package com.example.simpleoa.controller;

import com.example.simpleoa.model.BusinessTripRequest;
import com.example.simpleoa.service.BusinessTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business-trips")
public class BusinessTripController {
    private final BusinessTripService businessTripService;

    @Autowired
    public BusinessTripController(BusinessTripService businessTripService) {
        this.businessTripService = businessTripService;
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

    @PutMapping("/{id}/approve")
    public BusinessTripRequest approveBusinessTrip(@PathVariable Long id, 
                                          @RequestParam String status, 
                                          @RequestParam(required = false) String comment) {
        return businessTripService.approveBusinessTrip(id, status, comment);
    }
}