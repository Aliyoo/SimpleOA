package com.example.simpleoa.dto.worktime;

import lombok.Data;

/**
 * 成员DTO
 * 精简的用户信息
 */
@Data
public class MemberDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 用户角色
     */
    private String role;
}
