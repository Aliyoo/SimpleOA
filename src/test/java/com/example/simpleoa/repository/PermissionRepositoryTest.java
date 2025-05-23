package com.example.simpleoa.repository;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.model.Role;
import com.example.simpleoa.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PermissionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User user1, user2, user3;
    private Role role1, role2, role3;
    private Permission perm1, perm2, perm3;

    @BeforeEach
    void setUp() {
        // Permissions
        perm1 = new Permission();
        perm1.setName("VIEW_TASK");
        perm1.setDescription("View task details");
        perm1.setPermissionType("FUNCTIONAL");
        perm1.setIsActive(true);
        entityManager.persist(perm1);

        perm2 = new Permission();
        perm2.setName("EDIT_TASK");
        perm2.setDescription("Edit task details");
        perm2.setPermissionType("FUNCTIONAL");
        perm2.setIsActive(true);
        entityManager.persist(perm2);

        perm3 = new Permission();
        perm3.setName("DELETE_TASK");
        perm3.setDescription("Delete task");
        perm3.setPermissionType("FUNCTIONAL");
        perm3.setIsActive(true);
        entityManager.persist(perm3);

        // Roles
        role1 = new Role();
        role1.setName("Worker");
        role1.setDescription("Standard worker role");
        Set<Permission> workerPermissions = new HashSet<>(Arrays.asList(perm1, perm2));
        role1.setPermissions(workerPermissions);
        entityManager.persist(role1);

        role2 = new Role();
        role2.setName("Manager");
        role2.setDescription("Manager role with more permissions");
        Set<Permission> managerPermissions = new HashSet<>(Arrays.asList(perm1, perm2, perm3));
        role2.setPermissions(managerPermissions);
        entityManager.persist(role2);
        
        role3 = new Role();
        role3.setName("EmptyRole");
        role3.setDescription("Role with no permissions");
        role3.setPermissions(Collections.emptySet());
        entityManager.persist(role3);


        // Users
        user1 = new User();
        user1.setUsername("user1");
        user1.setRealName("User One");
        user1.setPassword("password");
        user1.setEmail("user1@example.com");
        user1.setRoles(Collections.singletonList(role1)); // User1 has VIEW_TASK, EDIT_TASK
        entityManager.persist(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setRealName("User Two");
        user2.setPassword("password");
        user2.setEmail("user2@example.com");
        user2.setRoles(Collections.emptyList()); // User2 has no roles
        entityManager.persist(user2);
        
        user3 = new User();
        user3.setUsername("user3");
        user3.setRealName("User Three");
        user3.setPassword("password");
        user3.setEmail("user3@example.com");
        user3.setRoles(Collections.singletonList(role3)); // User3 has EmptyRole (no permissions)
        entityManager.persist(user3);


        entityManager.flush(); // Ensure data is persisted before tests run
    }

    // --- Tests for hasPermission ---

    @Test
    void hasPermission_userHasPermission_shouldReturnTrue() {
        assertTrue(permissionRepository.hasPermission(user1.getId(), "VIEW_TASK"));
        assertTrue(permissionRepository.hasPermission(user1.getId(), "EDIT_TASK"));
    }

    @Test
    void hasPermission_userDoesNotHavePermission_shouldReturnFalse() {
        // perm3 (DELETE_TASK) exists but is not assigned to user1's role (role1)
        assertFalse(permissionRepository.hasPermission(user1.getId(), "DELETE_TASK"));
    }

    @Test
    void hasPermission_userHasNoRoles_shouldReturnFalse() {
        assertFalse(permissionRepository.hasPermission(user2.getId(), "VIEW_TASK"));
    }

    @Test
    void hasPermission_userRoleHasNoPermissions_shouldReturnFalse() {
        assertFalse(permissionRepository.hasPermission(user3.getId(), "VIEW_TASK"));
    }
    
    @Test
    void hasPermission_permissionNameDoesNotExist_shouldReturnFalse() {
        assertFalse(permissionRepository.hasPermission(user1.getId(), "NON_EXISTENT_PERMISSION"));
    }

    @Test
    void hasPermission_userIdDoesNotExist_shouldReturnFalse() {
        Long nonExistentUserId = Long.MAX_VALUE; // A user ID that is very unlikely to exist
        assertFalse(permissionRepository.hasPermission(nonExistentUserId, "VIEW_TASK"));
    }

    // --- Tests for findByUserId ---

    @Test
    void findByUserId_userWithMultiplePermissions_shouldReturnDistinctPermissions() {
        // User1 has role1 (VIEW_TASK, EDIT_TASK)
        List<Permission> foundPermissions = permissionRepository.findByUserId(user1.getId());
        assertThat(foundPermissions).hasSize(2);
        Set<String> permissionNames = foundPermissions.stream().map(Permission::getName).collect(Collectors.toSet());
        assertThat(permissionNames).containsExactlyInAnyOrder("VIEW_TASK", "EDIT_TASK");

        // Add role2 (VIEW_TASK, EDIT_TASK, DELETE_TASK) to user1 for overlapping permissions test
        user1.setRoles(Arrays.asList(role1, role2));
        entityManager.persistAndFlush(user1);

        List<Permission> updatedPermissions = permissionRepository.findByUserId(user1.getId());
        assertThat(updatedPermissions).hasSize(3); // Should be distinct: VIEW_TASK, EDIT_TASK, DELETE_TASK
        Set<String> updatedPermissionNames = updatedPermissions.stream().map(Permission::getName).collect(Collectors.toSet());
        assertThat(updatedPermissionNames).containsExactlyInAnyOrder("VIEW_TASK", "EDIT_TASK", "DELETE_TASK");
    }
    
    @Test
    void findByUserId_userWithOneRoleMultiplePermissions_shouldReturnPermissions() {
        // User1 has role1 (VIEW_TASK, EDIT_TASK)
        List<Permission> foundPermissions = permissionRepository.findByUserId(user1.getId());
        assertThat(foundPermissions).hasSize(2);
        List<String> names = foundPermissions.stream().map(Permission::getName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("VIEW_TASK", "EDIT_TASK");
    }


    @Test
    void findByUserId_userHasNoPermissions_noRoles_shouldReturnEmptyList() {
        // user2 has no roles
        List<Permission> foundPermissions = permissionRepository.findByUserId(user2.getId());
        assertThat(foundPermissions).isEmpty();
    }
    
    @Test
    void findByUserId_userHasNoPermissions_roleHasNoPermissions_shouldReturnEmptyList() {
        // user3 has role3 (EmptyRole with no permissions)
        List<Permission> foundPermissions = permissionRepository.findByUserId(user3.getId());
        assertThat(foundPermissions).isEmpty();
    }

    @Test
    void findByUserId_userIdDoesNotExist_shouldReturnEmptyList() {
        Long nonExistentUserId = Long.MAX_VALUE;
        List<Permission> foundPermissions = permissionRepository.findByUserId(nonExistentUserId);
        assertThat(foundPermissions).isEmpty();
    }
}
