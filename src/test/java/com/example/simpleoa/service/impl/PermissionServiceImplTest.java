package com.example.simpleoa.service.impl;

import com.example.simpleoa.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void checkUserPermission_whenRepositoryReturnsTrue_shouldReturnTrue() {
        Long userId = 1L;
        String permissionName = "VIEW_DATA";

        // Mocking the repository call
        when(permissionRepository.hasPermission(userId, permissionName)).thenReturn(true);

        // Calling the service method
        boolean result = permissionService.checkUserPermission(userId, permissionName);

        // Assertions
        assertTrue(result, "checkUserPermission should return true when repository.hasPermission returns true");

        // Verify that permissionRepository.hasPermission was called exactly once with the correct arguments
        verify(permissionRepository, times(1)).hasPermission(userId, permissionName);
    }

    @Test
    void checkUserPermission_whenRepositoryReturnsFalse_shouldReturnFalse() {
        Long userId = 2L;
        String permissionName = "EDIT_DATA";

        // Mocking the repository call
        when(permissionRepository.hasPermission(userId, permissionName)).thenReturn(false);

        // Calling the service method
        boolean result = permissionService.checkUserPermission(userId, permissionName);

        // Assertions
        assertFalse(result, "checkUserPermission should return false when repository.hasPermission returns false");

        // Verify that permissionRepository.hasPermission was called exactly once with the correct arguments
        verify(permissionRepository, times(1)).hasPermission(userId, permissionName);
    }

    @Test
    void checkUserPermission_verifyParametersPassedToRepository() {
        Long specificUserId = 100L;
        String specificPermissionName = "DELETE_USER";

        // We don't care about the return value for this test, just the interaction.
        // We can let it return default false (or true, doesn't matter for this verification)
        when(permissionRepository.hasPermission(specificUserId, specificPermissionName)).thenReturn(false);

        permissionService.checkUserPermission(specificUserId, specificPermissionName);

        // Verify that permissionRepository.hasPermission was called with the specific userId and permissionName
        verify(permissionRepository).hasPermission(eq(specificUserId), eq(specificPermissionName));

        // Verify it was called once
        verify(permissionRepository, times(1)).hasPermission(specificUserId, specificPermissionName);
    }
}
