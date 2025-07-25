package com.example.simpleoa.controller;

import com.example.simpleoa.model.*;
import com.example.simpleoa.service.ApprovalFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApprovalFlowController.class)
public class ApprovalFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApprovalFlowService approvalFlowService;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;
    private User mockApplicant;
    private Project mockProject;

    @BeforeEach
    public void setUp() {
        // 创建模拟用户（审批人）
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("approver");
        mockUser.setRealName("审批人");

        // 创建模拟申请人
        mockApplicant = new User();
        mockApplicant.setId(2L);
        mockApplicant.setUsername("applicant");
        mockApplicant.setRealName("申请人");

        // 创建模拟项目
        mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setName("测试项目");
    }

    @Test
    @WithMockUser(username = "approver", roles = {"USER"})
    public void testGetMyApprovalsWithReimbursement() throws Exception {
        // 创建模拟的报销申请
        ReimbursementRequest reimbursementRequest = new ReimbursementRequest();
        reimbursementRequest.setId(1L);
        reimbursementRequest.setTitle("差旅费报销");
        reimbursementRequest.setTotalAmount(new BigDecimal("1000.00"));
        reimbursementRequest.setApplicant(mockApplicant);
        reimbursementRequest.setProject(mockProject);
        reimbursementRequest.setStatus(ReimbursementStatus.PENDING_MANAGER_APPROVAL);
        reimbursementRequest.setCreateTime(LocalDateTime.now());

        // 创建模拟的审批流程
        ApprovalFlow approvalFlow = new ApprovalFlow();
        approvalFlow.setId(1L);
        approvalFlow.setApprover(mockUser);
        approvalFlow.setReimbursementRequest(reimbursementRequest);
        approvalFlow.setRequestType("REIMBURSEMENT");
        approvalFlow.setStatus("PENDING");
        approvalFlow.setCreateTime(new Date());

        List<ApprovalFlow> mockApprovals = Arrays.asList(approvalFlow);

        // 模拟服务返回
        when(approvalFlowService.getApprovalFlowsByApprover(1L)).thenReturn(mockApprovals);

        // 执行测试并打印响应
        MvcResult result = mockMvc.perform(get("/api/approval/my-approvals")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].requestType").value("REIMBURSEMENT"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].reimbursementRequest").exists())
                .andExpect(jsonPath("$[0].reimbursementRequest.title").value("差旅费报销"))
                .andExpect(jsonPath("$[0].reimbursementRequest.totalAmount").value(1000.00))
                .andExpect(jsonPath("$[0].reimbursementRequest.applicant").exists())
                .andExpect(jsonPath("$[0].reimbursementRequest.applicant.username").value("applicant"))
                .andExpect(jsonPath("$[0].reimbursementRequest.applicant.realName").value("申请人"))
                .andExpect(jsonPath("$[0].reimbursementRequest.project").exists())
                .andExpect(jsonPath("$[0].reimbursementRequest.project.name").value("测试项目"))
                .andReturn();

        // 打印响应内容以便调试
        System.out.println("Response Content: " + result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(username = "approver", roles = {"USER"})
    public void testGetMyApprovalsWithFilters() throws Exception {
        // 创建模拟的报销申请
        ReimbursementRequest reimbursementRequest = new ReimbursementRequest();
        reimbursementRequest.setId(2L);
        reimbursementRequest.setTitle("办公用品报销");
        reimbursementRequest.setTotalAmount(new BigDecimal("500.00"));
        reimbursementRequest.setApplicant(mockApplicant);
        reimbursementRequest.setProject(mockProject);
        reimbursementRequest.setStatus(ReimbursementStatus.APPROVED);
        reimbursementRequest.setCreateTime(LocalDateTime.now());

        // 创建模拟的审批流程
        ApprovalFlow approvalFlow = new ApprovalFlow();
        approvalFlow.setId(2L);
        approvalFlow.setApprover(mockUser);
        approvalFlow.setReimbursementRequest(reimbursementRequest);
        approvalFlow.setRequestType("REIMBURSEMENT");
        approvalFlow.setStatus("APPROVED");
        approvalFlow.setCreateTime(new Date());

        List<ApprovalFlow> mockApprovals = Arrays.asList(approvalFlow);

        // 模拟服务返回
        when(approvalFlowService.getApprovalFlowsByApproverAndTypeAndStatus(1L, "REIMBURSEMENT", "APPROVED"))
                .thenReturn(mockApprovals);

        // 执行测试
        mockMvc.perform(get("/api/approval/my-approvals")
                        .param("requestType", "REIMBURSEMENT")
                        .param("status", "APPROVED")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].requestType").value("REIMBURSEMENT"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].reimbursementRequest.title").value("办公用品报销"))
                .andExpect(jsonPath("$[0].reimbursementRequest.totalAmount").value(500.00))
                .andExpect(jsonPath("$[0].reimbursementRequest.applicant.username").value("applicant"));
    }
}
