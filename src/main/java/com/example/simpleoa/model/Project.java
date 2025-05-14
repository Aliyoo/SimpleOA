package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.*;


@Entity
@Data
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    private String priority = "0";

    /** 项目类型 */
    private String type = "0";

    private Date startDate;

    private Date endDate;

    // 项目经理
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;
    
    @ManyToMany
    @JoinTable(
        name = "project_user",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
}

