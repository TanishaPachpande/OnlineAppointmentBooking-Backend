package com.medibook.auth.security;

import com.medibook.auth.entity.AuthProvider;
import com.medibook.auth.entity.Role;
import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId(1L).fullName("John Doe").email("john@example.com")
                .passwordHash("$2a$10$hash").phone("9876543210")
                .role(Role.PATIENT).provider(AuthProvider.LOCAL)
                .isActive(true).build();
    }

    @Test
    void loadUserByUsername_found_returnsUserDetails() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

        UserDetails details = service.loadUserByUsername("john@example.com");

        assertThat(details.getUsername()).isEqualTo("john@example.com");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("PATIENT");
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    void loadUserByUsername_adminRole_hasAdminAuthority() {
        activeUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(activeUser));

        UserDetails details = service.loadUserByUsername("admin@example.com");

        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ADMIN");
    }

    @Test
    void loadUserByUsername_inactiveUser_isNotEnabled() {
        activeUser.setIsActive(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

        UserDetails details = service.loadUserByUsername("john@example.com");

        assertThat(details.isEnabled()).isFalse();
    }
}
