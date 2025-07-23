package com.example.simpleoa.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TaskDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String priority;
    private Date dueDate;
    private Long projectId;
    private Long assigneeId;
}