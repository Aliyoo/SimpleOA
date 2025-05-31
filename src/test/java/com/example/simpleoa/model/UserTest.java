package com.example.simpleoa.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void getAuthorities_noRoles_shouldReturnEmpty() {
        User user = new User();
        user.setRoles(Collections.emptyList()); // Explicitly set empty list
        assertTrue(user.getAuthorities().isEmpty(), "Authorities should be empty when user has no roles.");
    }

    @Test
    void getAuthorities_oneRoleOnePermission_shouldReturnOneAuthority() {
        User user = new User();
        Role role = new Role();
        Permission perm1 = new Permission();
        perm1.setName("VIEW_PROFILE");
        role.setPermissions(Collections.singleton(perm1));
        user.setRoles(Collections.singletonList(role));

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(1, authorities.size(), "Should have one authority.");
        assertTrue(authorities.contains(new SimpleGrantedAuthority("VIEW_PROFILE")), "Should contain VIEW_PROFILE authority.");
    }

    @Test
    void getAuthorities_oneRoleMultipleDistinctPermissions_shouldReturnMultipleAuthorities() {
        User user = new User();
        Role role = new Role();
        Permission perm1 = new Permission();
        perm1.setName("EDIT_DOCUMENT");
        Permission perm2 = new Permission();
        perm2.setName("DELETE_DOCUMENT");

        Set<Permission> permissions = new HashSet<>();
        permissions.add(perm1);
        permissions.add(perm2);
        role.setPermissions(permissions);
        user.setRoles(Collections.singletonList(role));

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(2, authorities.size(), "Should have two authorities.");
        List<String> authorityStrings = authorities.stream()
                                                   .map(GrantedAuthority::getAuthority)
                                                   .collect(Collectors.toList());
        assertTrue(authorityStrings.contains("EDIT_DOCUMENT"), "Should contain EDIT_DOCUMENT authority.");
        assertTrue(authorityStrings.contains("DELETE_DOCUMENT"), "Should contain DELETE_DOCUMENT authority.");
    }

    @Test
    void getAuthorities_multipleRolesDistinctPermissions_shouldReturnCombinedDistinctAuthorities() {
        User user = new User();

        Permission perm1 = new Permission();
        perm1.setName("MANAGE_USERS");
        Permission perm2 = new Permission();
        perm2.setName("MANAGE_SETTINGS");

        Role roleAdmin = new Role();
        roleAdmin.setPermissions(Collections.singleton(perm1));

        Role roleSystem = new Role();
        roleSystem.setPermissions(Collections.singleton(perm2));

        user.setRoles(Arrays.asList(roleAdmin, roleSystem));

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(2, authorities.size(), "Should have two distinct authorities from two roles.");
        List<String> authorityStrings = authorities.stream()
                                                   .map(GrantedAuthority::getAuthority)
                                                   .collect(Collectors.toList());
        assertTrue(authorityStrings.contains("MANAGE_USERS"), "Should contain MANAGE_USERS authority.");
        assertTrue(authorityStrings.contains("MANAGE_SETTINGS"), "Should contain MANAGE_SETTINGS authority.");
    }

    @Test
    void getAuthorities_multipleRolesWithOverlappingPermissions_shouldReturnDistinctAuthorities() {
        User user = new User();

        Permission permCommon = new Permission();
        permCommon.setName("VIEW_REPORTS");
        Permission permRole1Specific = new Permission();
        permRole1Specific.setName("EDIT_REPORTS");
        Permission permRole2Specific = new Permission();
        permRole2Specific.setName("APPROVE_REPORTS");

        Role roleAnalyst = new Role();
        Set<Permission> analystPermissions = new HashSet<>();
        analystPermissions.add(permCommon);
        analystPermissions.add(permRole1Specific);
        roleAnalyst.setPermissions(analystPermissions); // VIEW_REPORTS, EDIT_REPORTS

        Role roleManager = new Role();
        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(permCommon);
        managerPermissions.add(permRole2Specific);
        roleManager.setPermissions(managerPermissions); // VIEW_REPORTS, APPROVE_REPORTS

        user.setRoles(Arrays.asList(roleAnalyst, roleManager));

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertNotNull(authorities, "Authorities collection should not be null.");

        // Expected: VIEW_REPORTS, EDIT_REPORTS, APPROVE_REPORTS (3 distinct permissions)
        List<String> authorityStrings = authorities.stream()
                                                   .map(GrantedAuthority::getAuthority)
                                                   .sorted() // Sort for consistent assertion order if needed
                                                   .collect(Collectors.toList());

        assertEquals(3, authorities.size(), "Should have three distinct authorities after merging overlapping permissions.");
        assertTrue(authorityStrings.contains("VIEW_REPORTS"), "Should contain VIEW_REPORTS authority.");
        assertTrue(authorityStrings.contains("EDIT_REPORTS"), "Should contain EDIT_REPORTS authority.");
        assertTrue(authorityStrings.contains("APPROVE_REPORTS"), "Should contain APPROVE_REPORTS authority.");
    }

    @Test
    void getAuthorities_roleWithNoPermissions_shouldReturnEmpty() {
        User user = new User();
        Role roleWithNoPermissions = new Role();
        roleWithNoPermissions.setPermissions(Collections.emptySet()); // Role has no permissions
        user.setRoles(Collections.singletonList(roleWithNoPermissions));

        assertTrue(user.getAuthorities().isEmpty(), "Authorities should be empty if the role has no permissions.");
    }

    @Test
    void getAuthorities_multipleRolesOneWithPermissionsOneWithout_shouldReturnPermissionsFromPopulatedRole() {
        User user = new User();

        Permission perm1 = new Permission();
        perm1.setName("ACCESS_SYSTEM");

        Role roleWithPerms = new Role();
        roleWithPerms.setPermissions(Collections.singleton(perm1));

        Role roleWithoutPerms = new Role();
        roleWithoutPerms.setPermissions(Collections.emptySet());

        user.setRoles(Arrays.asList(roleWithPerms, roleWithoutPerms));

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(1, authorities.size(), "Should have one authority from the role that has permissions.");
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ACCESS_SYSTEM")), "Should contain ACCESS_SYSTEM authority.");
    }
}
