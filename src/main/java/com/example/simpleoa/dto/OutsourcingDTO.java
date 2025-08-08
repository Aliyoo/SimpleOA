package com.example.simpleoa.dto;

import lombok.Data;

@Data
public class OutsourcingDTO {
    private String projectName;
    private String company;
    private String startTime;
    private String endTime;
    private Double budget;
    private String description;
}
