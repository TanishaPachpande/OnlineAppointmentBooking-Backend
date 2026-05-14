package com.medibook.auth.controller;

import com.medibook.auth.entity.Role;
import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AdminController adminController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

        ReflectionTestUtils.setField(adminController, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(adminController, "providerServiceUrl", "http://localhost:8083");

        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setFullName("Admin User");
        mockUser.setEmail("admin@example.com");
        mockUser.setRole(Role.PATIENT);
        mockUser.setIsActive(true);
    }

    @Test
    void getAllUsers() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@example.com"));
    }

    @Test
    void getAllUsers_emptyList() throws Exception {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getUsersByRole() throws Exception {
        when(userRepository.findAllByRole(Role.PATIENT)).thenReturn(List.of(mockUser));

        mockMvc.perform(get("/auth/admin/users/role/PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@example.com"));
    }

    @Test
    void getUsersByRole_emptyResult() throws Exception {
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/admin/users/role/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void activateUser() throws Exception {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(put("/auth/admin/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(content().string("User activated successfully"));

        assert mockUser.getIsActive();
    }

    @Test
    void activateUser_notFound_throwsException() throws Exception {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/auth/admin/users/99/activate"))
                .andExpect(result -> assertThat(result.getResolvedException()).isNotNull());
    }

    @Test
    void deactivateUser() throws Exception {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(put("/auth/admin/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().string("User deactivated successfully"));

        assert !mockUser.getIsActive();
    }

    @Test
    void deactivateUser_notFound_throwsException() throws Exception {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/auth/admin/users/99/deactivate"))
                .andExpect(result -> assertThat(result.getResolvedException()).isNotNull());
    }

    @Test
    void verifyProvider_Success() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Verified", HttpStatus.OK));

        mockMvc.perform(put("/auth/admin/users/providers/1/verify")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Provider verified successfully"));
    }

    @Test
    void verifyProvider_Failure() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(put("/auth/admin/users/providers/1/verify"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to verify provider: Connection refused"));
    }

    @Test
    void verifyProvider_noAuthHeader_stillCallsService() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Verified", HttpStatus.OK));

        mockMvc.perform(put("/auth/admin/users/providers/2/verify"))
                .andExpect(status().isOk())
                .andExpect(content().string("Provider verified successfully"));
    }
}
