package com.example.simpleoa.service;

import com.example.simpleoa.model.BusinessTripRequest;
import org.springframework.stereotype.Service;

@Service
public interface BusinessTripService {
    BusinessTripRequest createBusinessTrip(BusinessTripRequest businessTripRequest);
    BusinessTripRequest updateBusinessTrip(BusinessTripRequest businessTripRequest);
    void deleteBusinessTrip(Long id);
    BusinessTripRequest getBusinessTripById(Long id);
    Iterable<BusinessTripRequest> getAllBusinessTrips();
    Iterable<BusinessTripRequest> getBusinessTripsByUser(Long userId);
    BusinessTripRequest approveBusinessTrip(Long id, String status, String comment);
}