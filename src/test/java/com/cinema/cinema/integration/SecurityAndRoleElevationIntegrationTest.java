package com.cinema.cinema.integration;

import com.cinema.cinema.domain.service.JwtTokenProvider;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.ChangeRoleRequest;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAndRoleElevationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Should reject unauthenticated request on PATCH /api/v1/users/{id}/role")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangeRoleRequest("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject CLIENT role user on PATCH /api/v1/users/{id}/role")
    void shouldRejectClientRole() throws Exception {
        RoleEntity clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "CLIENT", "Client")));
        UserEntity clientUser = userRepository.save(new UserEntity(null, "client@cinema.com", "Client User", Set.of(clientRole), LocalDateTime.now()));

        String clientToken = jwtTokenProvider.generateToken(clientUser.getId(), clientUser.getEmail(), clientUser.getFullName(), Set.of("CLIENT"));

        mockMvc.perform(patch("/api/v1/users/" + clientUser.getId() + "/role")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangeRoleRequest("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Double verification: Should reject token with ADMIN claim if DB user is not ADMIN")
    void shouldRejectWhenDbRoleDoesNotMatchTokenClaim() throws Exception {
        RoleEntity clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "CLIENT", "Client")));
        UserEntity clientUser = userRepository.save(new UserEntity(null, "fakeadmin@cinema.com", "Fake Admin", Set.of(clientRole), LocalDateTime.now()));

        // Forged token containing ADMIN claim
        String forgedToken = jwtTokenProvider.generateToken(clientUser.getId(), clientUser.getEmail(), clientUser.getFullName(), Set.of("ADMIN"));

        mockMvc.perform(patch("/api/v1/users/" + clientUser.getId() + "/role")
                        .header("Authorization", "Bearer " + forgedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangeRoleRequest("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Root admin can successfully promote a CLIENT to ADMIN")
    void shouldAllowAdminToPromoteClientToAdmin() throws Exception {
        RoleEntity adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "ADMIN", "Administrator")));
        RoleEntity clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "CLIENT", "Client")));

        UserEntity rootUser = userRepository.save(new UserEntity(null, "root@root.root", "Root Administrator", Set.of(adminRole), LocalDateTime.now()));
        UserEntity targetUser = userRepository.save(new UserEntity(null, "target@cinema.com", "Target User", Set.of(clientRole), LocalDateTime.now()));

        String adminToken = jwtTokenProvider.generateToken(rootUser.getId(), rootUser.getEmail(), rootUser.getFullName(), Set.of("ADMIN"));

        mockMvc.perform(patch("/api/v1/users/" + targetUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangeRoleRequest("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId()))
                .andExpect(jsonPath("$.roles").isArray());
    }
}
