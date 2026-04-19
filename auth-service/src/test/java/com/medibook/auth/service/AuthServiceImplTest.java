package com.medibook.auth.service;

import com.medibook.auth.dto.AuthResponseDto;
import com.medibook.auth.dto.LoginRequestDto;
import com.medibook.auth.dto.RegisterRequestDto;
import com.medibook.auth.entity.AuthProvider;
import com.medibook.auth.entity.Role;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.InvalidCredentialsException;
import com.medibook.auth.exception.UserAlreadyExistsException;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDto registerRequestDto;
    private LoginRequestDto loginRequestDto;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequestDto = RegisterRequestDto.builder()
                .fullName("Tanisha Pachpande")
                .email("tanisha@gmail.com")
                .password("Tanisha@123")
                .phone("9876543210")
                .role(Role.PATIENT)
                .build();

        loginRequestDto = LoginRequestDto.builder()
                .email("tanisha@gmail.com")
                .password("Tanisha@123")
                .build();

        user = User.builder()
                .userId(1L)
                .fullName("Tanisha Pachpande")
                .email("tanisha@gmail.com")
                .passwordHash("encodedPassword")
                .phone("9876543210")
                .role(Role.PATIENT)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
    }

    @Test
    void register_ShouldRegisterUserSuccessfully() {
        when(userRepository.existsByEmail(registerRequestDto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequestDto.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(registerRequestDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name())).thenReturn("mock-jwt-token");

        AuthResponseDto response = authService.register(registerRequestDto);

        assertNotNull(response);
        assertEquals("tanisha@gmail.com", response.getEmail());
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("User registered successfully", response.getMessage());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequestDto.getEmail())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequestDto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        when(userRepository.findByEmail(loginRequestDto.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequestDto.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name())).thenReturn("mock-jwt-token");

        AuthResponseDto response = authService.login(loginRequestDto);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_ShouldThrowException_WhenPasswordIsInvalid() {
        when(userRepository.findByEmail(loginRequestDto.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequestDto.getPassword(), user.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequestDto));
    }
}