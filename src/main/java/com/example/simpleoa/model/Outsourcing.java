package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "outsourcing")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Outsourcing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String vendor;

    @Column(nullable = false)
    private String contactPerson;

    @Column
    private String contactPhone;

    @Column
    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double budget;

    @Column(nullable = false)
    private Date startDate;

    @Column(nullable = false)
    private Date endDate;

    @Column
    private String status;

    @Column
    private Integer completionPercentage;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
}
