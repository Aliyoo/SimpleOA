package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String status; // TODO, INPROGRESS, DONE
    private String priority; // LOW, MEDIUM, HIGH
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate completedDate;

    @ManyToOne
    private User assignee;

    @ManyToOne
    private Project project;
}