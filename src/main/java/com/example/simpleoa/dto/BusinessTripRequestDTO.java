package com.example.simpleoa.dto;

import lombok.Data;
import java.util.Date;

@Data
public class BusinessTripRequestDTO {
    private Long id;
    private String destination;
    private Date startDate;
    private Date endDate;
    private String purpose;
    private String status;
    private Long userId;
    private String comment;
}