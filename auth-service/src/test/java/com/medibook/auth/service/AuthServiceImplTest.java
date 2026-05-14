package com.medibook.auth.service;

import com.medibook.auth.dto.*;
import com.medibook.auth.entity.AuthProvider;
import com.medibook.auth.entity.Role;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.InvalidCredentialsException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.exception.UserAlreadyExistsException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String TEST_EMAIL = "john@example.com";
    private static final String TEST_PHONE = "9876543210";
    private static final String TEST_PASSWORD = "Password1!";
    private static final String TEST_PASSWORD_HASH = "$2a$10$hashedpassword";
    private static final String CACHED_TOKEN = "cached-token";
    private static final String OLD_PATIENT_TOKEN = "old-patient-token";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private OtpService otpService;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId(1L).fullName("John Doe").email(TEST_EMAIL)
                .passwordHash(TEST_PASSWORD_HASH).phone(TEST_PHONE)
                .role(Role.PATIENT).provider(AuthProvider.LOCAL)
                .isActive(true).createdAt(LocalDateTime.now()).build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    void registerSuccess() {
        RegisterRequestDto req = RegisterRequestDto.builder()
                .fullName("John Doe").email(TEST_EMAIL).password(TEST_PASSWORD)
                .phone(TEST_PHONE).role(Role.PATIENT).build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.existsByPhone(TEST_PHONE)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD_HASH);
        when(userRepository.save(any())).thenReturn(activeUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt-token");
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        AuthResponseDto result = authService.register(req);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getRole()).isEqualTo(Role.PATIENT);
        assertThat(result.getMessage()).contains("registered");
    }

    @Test
    void registerDuplicateEmailThrows() {
        RegisterRequestDto req = RegisterRequestDto.builder()
                .email(TEST_EMAIL).phone(TEST_PHONE).role(Role.PATIENT)
                .fullName("John").password("pass").build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UserAlreadyExistsException.class).hasMessageContaining("email");
    }

    @Test
    void registerDuplicatePhoneThrows() {
        RegisterRequestDto req = RegisterRequestDto.builder()
                .email("new@example.com").phone(TEST_PHONE).role(Role.PATIENT)
                .fullName("John").password("pass").build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone(TEST_PHONE)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UserAlreadyExistsException.class).hasMessageContaining("phone");
    }

    // ── registerWithOtp ────────────────────────────────────────────────────────

    @Test
    void registerWithOtp_success() {
        RegisterWithOtpRequestDto req = RegisterWithOtpRequestDto.builder()
                .fullName("Jane Doe").email(TEST_EMAIL).password(TEST_PASSWORD)
                .phone(TEST_PHONE).role(Role.PATIENT).otp("123456").build();

        when(otpService.verifyOtp(TEST_EMAIL, "123456")).thenReturn(true);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.existsByPhone(TEST_PHONE)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD_HASH);
        when(userRepository.save(any())).thenReturn(activeUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("otp-token");
        doNothing().when(valueOperations).set(anyString(), anyString(), any());
        doNothing().when(otpService).deleteOtp(TEST_EMAIL);

        AuthResponseDto result = authService.registerWithOtp(req);

        assertThat(result.getToken()).isEqualTo("otp-token");
        verify(otpService).deleteOtp(TEST_EMAIL);
    }

    @Test
    void registerWithOtp_invalidOtp_throws() {
        RegisterWithOtpRequestDto req = RegisterWithOtpRequestDto.builder()
                .fullName("Jane").email(TEST_EMAIL).password(TEST_PASSWORD)
                .phone(TEST_PHONE).role(Role.PATIENT).otp("000000").build();

        when(otpService.verifyOtp(TEST_EMAIL, "000000")).thenReturn(false);

        assertThatThrownBy(() -> authService.registerWithOtp(req))
                .isInstanceOf(InvalidCredentialsException.class).hasMessageContaining("OTP");
    }

    @Test
    void registerWithOtp_duplicateEmail_throws() {
        RegisterWithOtpRequestDto req = RegisterWithOtpRequestDto.builder()
                .fullName("Jane").email(TEST_EMAIL).password(TEST_PASSWORD)
                .phone(TEST_PHONE).role(Role.PATIENT).otp("123456").build();

        when(otpService.verifyOtp(TEST_EMAIL, "123456")).thenReturn(true);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.registerWithOtp(req))
                .isInstanceOf(UserAlreadyExistsException.class).hasMessageContaining("email");
    }

    @Test
    void registerWithOtp_duplicatePhone_throws() {
        RegisterWithOtpRequestDto req = RegisterWithOtpRequestDto.builder()
                .fullName("Jane").email("new@example.com").password(TEST_PASSWORD)
                .phone(TEST_PHONE).role(Role.PATIENT).otp("123456").build();

        when(otpService.verifyOtp("new@example.com", "123456")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone(TEST_PHONE)).thenReturn(true);

        assertThatThrownBy(() -> authService.registerWithOtp(req))
                .isInstanceOf(UserAlreadyExistsException.class).hasMessageContaining("phone");
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    void loginSuccessNoCache() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("fresh-token");
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        AuthResponseDto result = authService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD));

        assertThat(result.getToken()).isEqualTo("fresh-token");
        assertThat(result.getMessage()).contains("Login successful");
    }

    @Test
    void loginUsesValidCachedTokenWhenRoleMatches() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(CACHED_TOKEN);
        when(jwtUtil.validateToken(CACHED_TOKEN)).thenReturn(true);
        when(jwtUtil.extractRole(CACHED_TOKEN)).thenReturn("PATIENT");

        AuthResponseDto result = authService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD));
        assertThat(result.getToken()).isEqualTo(CACHED_TOKEN);
    }

    @Test
    void loginRegeneratesTokenWhenRoleMismatch() {
        activeUser.setRole(Role.ADMIN);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(OLD_PATIENT_TOKEN);
        when(jwtUtil.validateToken(OLD_PATIENT_TOKEN)).thenReturn(true);
        when(jwtUtil.extractRole(OLD_PATIENT_TOKEN)).thenReturn("PATIENT");
        when(jwtUtil.generateToken(anyString(), eq("ADMIN"), anyLong())).thenReturn("new-admin-token");
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        AuthResponseDto result = authService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD));
        assertThat(result.getToken()).isEqualTo("new-admin-token");
    }

    @Test
    void loginExpiredCacheRegeneratesToken() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn("expired-token");
        when(jwtUtil.validateToken("expired-token")).thenReturn(false);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("new-token");
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        AuthResponseDto result = authService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD));
        assertThat(result.getToken()).isEqualTo("new-token");
    }

    @Test
    void loginUserNotFoundThrows() {
        when(userRepository.findByEmail("noone@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("noone@example.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWrongPasswordThrows() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongpass", TEST_PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDto(TEST_EMAIL, "wrongpass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginInactiveAccountThrows() {
        activeUser.setIsActive(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class).hasMessageContaining("deactivated");
    }

    // ── getUserByEmail / getUserById ───────────────────────────────────────────

    @Test
    void getUserByEmailFound() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(activeUser));
        assertThat(authService.getUserByEmail(TEST_EMAIL).getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void getUserByEmailNotFoundThrows() {
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getUserByEmail("x@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserByIdFound() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(activeUser));
        assertThat(authService.getUserById(1L).getUserId()).isEqualTo(1L);
    }

    @Test
    void getUserByIdNotFoundThrows() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deactivateAccount ──────────────────────────────────────────────────────

    @Test
    void deactivateAccountSuccess() {
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        String result = authService.deactivateAccount(1L);
        assertThat(result).contains("deactivated");
    }

    @Test
    void deactivateAccountNotFoundThrows() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.deactivateAccount(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
