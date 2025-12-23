package com.example.simpleoa.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntityType枚举测试
 *
 * @author SimpleOA Refactoring Team
 */
@DisplayName("EntityType枚举测试")
class EntityTypeTest {

    @Test
    @DisplayName("应该正确从request_type转换")
    void shouldConvertFromRequestType() {
        assertEquals(EntityType.WORK_TIME, EntityType.fromRequestType("WORKTIME"));
        assertEquals(EntityType.LEAVE_REQUEST, EntityType.fromRequestType("LEAVE"));
        assertEquals(EntityType.BUSINESS_TRIP, EntityType.fromRequestType("BUSINESS_TRIP"));
        assertEquals(EntityType.REIMBURSEMENT, EntityType.fromRequestType("REIMBURSEMENT"));

        // 测试大小写不敏感
        assertEquals(EntityType.WORK_TIME, EntityType.fromRequestType("worktime"));
        assertEquals(EntityType.LEAVE_REQUEST, EntityType.fromRequestType("leave"));
    }

    @Test
    @DisplayName("应该正确转换为request_type")
    void shouldConvertToRequestType() {
        assertEquals("WORKTIME", EntityType.WORK_TIME.toRequestType());
        assertEquals("LEAVE", EntityType.LEAVE_REQUEST.toRequestType());
        assertEquals("BUSINESS_TRIP", EntityType.BUSINESS_TRIP.toRequestType());
        assertEquals("REIMBURSEMENT", EntityType.REIMBURSEMENT.toRequestType());
    }

    @Test
    @DisplayName("双向转换应该保持一致性")
    void shouldMaintainConsistencyInBidirectionalConversion() {
        for (EntityType type : EntityType.values()) {
            String requestType = type.toRequestType();
            EntityType converted = EntityType.fromRequestType(requestType);
            assertEquals(type, converted, "Bidirectional conversion failed for " + type);
        }
    }

    @Test
    @DisplayName("应该正确返回code和description")
    void shouldReturnCorrectCodeAndDescription() {
        assertEquals("WorkTimeRecord", EntityType.WORK_TIME.getCode());
        assertEquals("工时记录", EntityType.WORK_TIME.getDescription());

        assertEquals("LeaveRequest", EntityType.LEAVE_REQUEST.getCode());
        assertEquals("请假申请", EntityType.LEAVE_REQUEST.getDescription());

        assertEquals("BusinessTripRequest", EntityType.BUSINESS_TRIP.getCode());
        assertEquals("出差申请", EntityType.BUSINESS_TRIP.getDescription());

        assertEquals("ReimbursementRequest", EntityType.REIMBURSEMENT.getCode());
        assertEquals("报销申请", EntityType.REIMBURSEMENT.getDescription());
    }

    @Test
    @DisplayName("应该处理无效的request_type")
    void shouldHandleInvalidRequestType() {
        assertNull(EntityType.fromRequestType(null));
        assertNull(EntityType.fromRequestType("INVALID_TYPE"));
        assertNull(EntityType.fromRequestType(""));
    }
}
