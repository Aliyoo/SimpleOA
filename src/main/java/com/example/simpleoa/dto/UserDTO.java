package com.example.simpleoa.dto;

import lombok.Data;
import java.util.Date;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phoneNumber;
    private Integer enabled;
    private String department;
    private String employeeNumber;
    private Date hireDate;
}