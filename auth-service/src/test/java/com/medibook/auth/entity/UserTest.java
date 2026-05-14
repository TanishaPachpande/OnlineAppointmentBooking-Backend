package com.medibook.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void prePersist_setsCreatedAtAndDefaults() {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        user.setPhone("9999999999");
        user.setRole(Role.PATIENT);

        user.prePersist();

        assertThat(user.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test
    void prePersist_doesNotOverwriteExistingIsActive() {
        User user = new User();
        user.setIsActive(false);
        user.setRole(Role.ADMIN);

        user.prePersist();

        assertThat(user.getIsActive()).isFalse();
    }

    @Test
    void prePersist_doesNotOverwriteExistingProvider() {
        User user = new User();
        user.setProvider(AuthProvider.GOOGLE);
        user.setRole(Role.PATIENT);

        user.prePersist();

        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void builder_createsUserWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .userId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("hashedpw")
                .phone("1234567890")
                .role(Role.PATIENT)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .createdAt(now)
                .profilePicUrl("http://pic.url/img.png")
                .build();

        assertThat(user.getUserId()).isEqualTo(1L);
        assertThat(user.getFullName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashedpw");
        assertThat(user.getPhone()).isEqualTo("1234567890");
        assertThat(user.getRole()).isEqualTo(Role.PATIENT);
        assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getProfilePicUrl()).isEqualTo("http://pic.url/img.png");
    }

    @Test
    void noArgsConstructor_and_setters_work() {
        User user = new User();
        user.setUserId(10L);
        user.setFullName("Jane");
        user.setEmail("jane@example.com");
        user.setPasswordHash("pw");
        user.setPhone("0000000000");
        user.setRole(Role.ADMIN);
        user.setProvider(AuthProvider.GITHUB);
        user.setIsActive(true);

        assertThat(user.getUserId()).isEqualTo(10L);
        assertThat(user.getFullName()).isEqualTo("Jane");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getProvider()).isEqualTo(AuthProvider.GITHUB);
    }

    @Test
    void allArgsConstructor_works() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(2L, "Alice", "alice@example.com", "hash",
                "9876543210", Role.PATIENT, AuthProvider.LOCAL, true, now, null);

        assertThat(user.getUserId()).isEqualTo(2L);
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getProfilePicUrl()).isNull();
    }
}
