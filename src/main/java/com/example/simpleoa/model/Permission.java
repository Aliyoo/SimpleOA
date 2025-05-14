package com.example.simpleoa.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "permission")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private String permissionType; // 'FUNCTIONAL' or 'DATA'

    @Column
    private String resource; // 资源名称，如'PROJECT', 'USER', 'BUDGET'等

    @Column
    private String action; // 操作，如'READ', 'WRITE', 'DELETE'等

    @Column
    private String dataScope; // 数据范围，如'ALL', 'DEPARTMENT', 'SELF'等

    @Column
    private Boolean isActive;

    // 关联的所有角色
    @ManyToMany
    @JoinTable(name = "permission_role_mapping",
        joinColumns = @JoinColumn(name = "permission_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
    private Set<Role> roles;

    // 关联的所有用户
    //@ManyToMany
    //@JoinTable(name = "permission_user_mapping",
    //    joinColumns = @JoinColumn(name = "permission_id"),
    //    inverseJoinColumns = @JoinColumn(name = "user_id"))
    //private Set<User> users;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateTime;

    @ElementCollection
    @CollectionTable(name = "permission_function_mapping",
        joinColumns = @JoinColumn(name = "permission_id"))
    @MapKeyColumn(name = "function_name")
    @Column(name = "has_access")
    private Map<String, Boolean> functionPermissions = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "permission_data_mapping",
        joinColumns = @JoinColumn(name = "permission_id"))
    @MapKeyColumn(name = "data_type")
    @Column(name = "access_level")
    private Map<String, String> dataPermissions = new HashMap<>();
}