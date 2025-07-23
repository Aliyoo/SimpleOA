package com.example.simpleoa.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String priority;
    private String type;
    private Date startDate;
    private Date endDate;
    private Long managerId;
    private List<Long> memberIds;
}