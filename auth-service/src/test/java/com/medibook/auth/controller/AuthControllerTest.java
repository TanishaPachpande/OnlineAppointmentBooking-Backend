package com.medibook.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.auth.dto.*;
import com.medibook.auth.entity.Role;
import com.medibook.auth.service.AuthService;
import com.medibook.auth.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private OtpService otpService;
    @InjectMocks private AuthController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthResponseDto sampleAuthResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        sampleAuthResponse = AuthResponseDto.builder()
                .userId(1L).fullName("John Doe").email("john@example.com")
                .role(Role.PATIENT).token("jwt-token").message("Success")
                .build();
    }

    @Test
    void test_returns200() throws Exception {
        mockMvc.perform(get("/auth/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth working"));
    }

    @Test
    void register_returns200() throws Exception {
        RegisterRequestDto req = RegisterRequestDto.builder()
                .fullName("John Doe").email("john@example.com")
                .password("Password1!").phone("9876543210").role(Role.PATIENT).build();

        when(authService.register(any())).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(authService).register(any());
    }

    @Test
    void login_returns200() throws Exception {
        LoginRequestDto req = LoginRequestDto.builder()
                .email("john@example.com").password("Password1!").build();

        when(authService.login(any())).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));

        verify(authService).login(any());
    }

    @Test
    void getByEmail_returns200() throws Exception {
        when(authService.getUserByEmail("john@example.com")).thenReturn(sampleAuthResponse);

        mockMvc.perform(get("/auth/profile/email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getById_returns200() throws Exception {
        when(authService.getUserById(1L)).thenReturn(sampleAuthResponse);

        mockMvc.perform(get("/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void deactivate_returns200() throws Exception {
        when(authService.deactivateAccount(1L)).thenReturn("Account deactivated successfully");

        mockMvc.perform(put("/auth/deactivate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deactivated successfully"));
    }

    @Test
    void sendOtp_returns200() throws Exception {
        SendOtpRequestDto req = SendOtpRequestDto.builder().email("john@example.com").build();
        doNothing().when(otpService).sendOtp(anyString(), any());

        mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully to john@example.com"));

        verify(otpService).sendOtp(eq("john@example.com"), any());
    }

    @Test
    void verifyOtp_Success_returns200() throws Exception {
        VerifyOtpRequestDto req = VerifyOtpRequestDto.builder()
                .email("john@example.com").otp("123456").build();

        when(otpService.verifyOtp("john@example.com", "123456")).thenReturn(true);

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    void verifyOtp_Failure_returns400() throws Exception {
        VerifyOtpRequestDto req = VerifyOtpRequestDto.builder()
                .email("john@example.com").otp("000000").build();

        when(otpService.verifyOtp("john@example.com", "000000")).thenReturn(false);

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    @Test
    void registerWithOtp_returns200() throws Exception {
        RegisterWithOtpRequestDto req = RegisterWithOtpRequestDto.builder()
                .fullName("Jane Doe").email("jane@example.com").password("Password1!")
                .phone("9876543211").role(Role.PATIENT).otp("123456").build();

        when(authService.registerWithOtp(any())).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/auth/register-with-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(authService).registerWithOtp(any());
    }
}