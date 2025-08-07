package com.example.simpleoa.controller;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.ReimbursementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReimbursementControllerTest {

    @InjectMocks
    private ReimbursementController reimbursementController;

    @Mock
    private ReimbursementService reimbursementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSubmitReimbursement() {
        // Arrange
        User user = new User();
        user.setId(1L);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).thenReturn(user);

        when(reimbursementService.submitForApproval(any(Long.class), any(Long.class))).thenReturn(new ReimbursementRequest());

        // Act
        ResponseEntity<?> response = reimbursementController.submitForApproval(1L, user);

        // Assert
        assertNotNull(response);
    }
}
