package com.example.simpleoa.dto;

import lombok.Data;
import java.util.Date;

@Data
public class WorkTimeRecordDTO {
    private Long id;
    private Date workDate;
    private Double workHours;
    private String description;
    private String type;
    private String status;
    private Long userId;
    private Long projectId;
    private String comment;
}