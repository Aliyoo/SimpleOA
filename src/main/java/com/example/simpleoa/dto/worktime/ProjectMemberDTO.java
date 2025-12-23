package com.example.simpleoa.dto.worktime;

import lombok.Data;
import java.util.List;

/**
 * 项目成员DTO
 * 包含项目信息和成员列表
 */
@Data
public class ProjectMemberDTO {

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目经理ID
     */
    private Long managerId;

    /**
     * 项目成员列表
     */
    private List<MemberDTO> members;
}
