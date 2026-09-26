package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.port.in.AuthUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@WithMockUser
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @Test
    @DisplayName("should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        AuthUseCase.AuthResult authResult = new AuthUseCase.AuthResult(1L, "user@cinema.com", "Test User", "jwt.token.mock");
        given(authUseCase.login("user@cinema.com", "password123")).willReturn(authResult);

        String json = """
                {
                    "email": "user@cinema.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("user@cinema.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.token").value("jwt.token.mock"));
    }

    @Test
    @DisplayName("should return 401 on invalid credentials")
    void shouldReturn401OnInvalidCredentials() throws Exception {
        given(authUseCase.login("user@cinema.com", "wrongpassword"))
                .willThrow(new IllegalArgumentException("Invalid email or password"));

        String json = """
                {
                    "email": "user@cinema.com",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 400 on empty login request")
    void shouldReturn400OnEmptyLoginRequest() throws Exception {
        String json = """
                {
                    "email": "",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should register successfully")
    void shouldRegisterSuccessfully() throws Exception {
        AuthUseCase.AuthResult authResult = new AuthUseCase.AuthResult(2L, "new@cinema.com", "New User", "jwt.token.new");
        given(authUseCase.register("new@cinema.com", "New User", "password123")).willReturn(authResult);

        String json = """
                {
                    "email": "new@cinema.com",
                    "fullName": "New User",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.email").value("new@cinema.com"))
                .andExpect(jsonPath("$.fullName").value("New User"))
                .andExpect(jsonPath("$.token").value("jwt.token.new"));
    }

    @Test
    @DisplayName("should return 409 when user already exists")
    void shouldReturn409WhenUserAlreadyExists() throws Exception {
        given(authUseCase.register("existing@cinema.com", "User", "pwd"))
                .willThrow(new IllegalStateException("User already exists with email: existing@cinema.com"));

        String json = """
                {
                    "email": "existing@cinema.com",
                    "fullName": "User",
                    "password": "pwd"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
