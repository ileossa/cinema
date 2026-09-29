package com.cinema.cinema.integration;

import com.cinema.cinema.domain.port.out.MediaStorageOut;
import com.cinema.cinema.domain.service.JwtTokenProvider;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.ConfirmUploadRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.UploadUrlRequest;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.cinema.cinema.infrastructure.listener.SecurityMediaListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMediaManagementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MediaStorageOut mediaStorageMock;

    @Autowired
    private SecurityMediaListener securityMediaListener;

    @Test
    @DisplayName("Should generate presigned upload URL for admin")
    void shouldGeneratePresignedUploadUrlForAdmin() throws Exception {
        RoleEntity adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "ADMIN", "Administrator")));
        UserEntity adminUser = userRepository.save(new UserEntity(null, "admin@cinema.com", "Admin User", Set.of(adminRole), LocalDateTime.now()));
        String adminToken = jwtTokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getFullName(), Set.of("ADMIN"));

        given(mediaStorageMock.generatePresignedUploadUrl("poster.jpg", "image/jpeg"))
                .willReturn("http://localhost:9000/cinema-media/poster.jpg?X-Amz-Signature=test");

        mockMvc.perform(post("/api/v1/admin/media/upload-url")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UploadUrlRequest("poster.jpg", "image/jpeg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("poster.jpg"))
                .andExpect(jsonPath("$.uploadUrl").value("http://localhost:9000/cinema-media/poster.jpg?X-Amz-Signature=test"));
    }

    @Test
    @DisplayName("Should reject media endpoints when accessed without admin role")
    void shouldRejectWhenNotAdmin() throws Exception {
        RoleEntity clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "CLIENT", "Client")));
        UserEntity clientUser = userRepository.save(new UserEntity(null, "client@cinema.com", "Client User", Set.of(clientRole), LocalDateTime.now()));
        String clientToken = jwtTokenProvider.generateToken(clientUser.getId(), clientUser.getEmail(), clientUser.getFullName(), Set.of("CLIENT"));

        mockMvc.perform(post("/api/v1/admin/media/upload-url")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UploadUrlRequest("poster.jpg", "image/jpeg"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should confirm upload and trigger async event when file exists")
    void shouldConfirmUploadWhenFileExists() throws Exception {
        RoleEntity adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "ADMIN", "Administrator")));
        UserEntity adminUser = userRepository.save(new UserEntity(null, "admin@cinema.com", "Admin User", Set.of(adminRole), LocalDateTime.now()));
        String adminToken = jwtTokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getFullName(), Set.of("ADMIN"));

        given(mediaStorageMock.fileExists("valid-poster.jpg")).willReturn(true);
        given(mediaStorageMock.getFileSize("valid-poster.jpg")).willReturn(204800L);
        given(mediaStorageMock.getContentType("valid-poster.jpg")).willReturn("image/jpeg");

        int initialCount = securityMediaListener.getProcessedEventsCount();

        mockMvc.perform(post("/api/v1/admin/media/confirm")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ConfirmUploadRequest("valid-poster.jpg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("valid-poster.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.size").value(204800));

        // Wait a short moment for async listener
        Thread.sleep(200);
        assertThat(securityMediaListener.getProcessedEventsCount()).isGreaterThanOrEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when confirming non existing file")
    void shouldRejectConfirmWhenFileDoesNotExist() throws Exception {
        RoleEntity adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new RoleEntity(null, "ADMIN", "Administrator")));
        UserEntity adminUser = userRepository.save(new UserEntity(null, "admin@cinema.com", "Admin User", Set.of(adminRole), LocalDateTime.now()));
        String adminToken = jwtTokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getFullName(), Set.of("ADMIN"));

        given(mediaStorageMock.fileExists("missing.jpg")).willReturn(false);

        mockMvc.perform(post("/api/v1/admin/media/confirm")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ConfirmUploadRequest("missing.jpg"))))
                .andExpect(status().isBadRequest());
    }
}
