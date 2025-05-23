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
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Permission other = (Permission) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
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

    // 关联角色
    @ManyToMany
    @JoinTable(name = "role_permission",
        joinColumns = @JoinColumn(name = "permission_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

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