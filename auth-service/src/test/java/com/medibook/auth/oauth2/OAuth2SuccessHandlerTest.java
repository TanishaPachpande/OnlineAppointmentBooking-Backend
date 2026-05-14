package com.medibook.auth.oauth2;

import com.medibook.auth.entity.AuthProvider;
import com.medibook.auth.entity.Role;
import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendRedirectUri", "http://localhost:5173/oauth2/callback");
        ReflectionTestUtils.setField(handler, "tokenTtlHours", 24L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private OAuth2AuthenticationToken buildToken(String registrationId, Map<String, Object> attrs) {
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttributes()).thenReturn(attrs);
        OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(oAuth2User);
        when(token.getAuthorizedClientRegistrationId()).thenReturn(registrationId);
        return token;
    }

    private User buildActiveUser(String email, Role role, AuthProvider provider) {
        return User.builder()
                .userId(1L)
                .fullName("Test User")
                .email(email)
                .passwordHash("hash")
                .phone("0000000000")
                .role(role)
                .provider(provider)
                .isActive(true)
                .build();
    }

    @Test
    void onAuthenticationSuccess_googleExistingUser_redirectsWithToken() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "user@example.com");
        attrs.put("name", "Test User");
        OAuth2AuthenticationToken token = buildToken("google", attrs);

        User user = buildActiveUser("user@example.com", Role.PATIENT, AuthProvider.GOOGLE);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("token=jwt-token");
        assertThat(response.getRedirectedUrl()).contains("role=PATIENT");
    }

    @Test
    void onAuthenticationSuccess_newGoogleUser_createsUserAndRedirects() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "newuser@example.com");
        attrs.put("name", "New User");
        OAuth2AuthenticationToken token = buildToken("google", attrs);

        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        User savedUser = buildActiveUser("newuser@example.com", Role.PATIENT, AuthProvider.GOOGLE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("new-jwt");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        verify(userRepository).save(any(User.class));
        assertThat(response.getRedirectedUrl()).contains("token=new-jwt");
    }

    @Test
    void onAuthenticationSuccess_deactivatedUser_redirectsWithError() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "inactive@example.com");
        attrs.put("name", "Inactive User");
        OAuth2AuthenticationToken token = buildToken("google", attrs);

        User user = buildActiveUser("inactive@example.com", Role.PATIENT, AuthProvider.GOOGLE);
        user.setIsActive(false);
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("error=account_deactivated");
    }

    @Test
    void onAuthenticationSuccess_missingEmail_redirectsWithError() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        // no email key
        OAuth2AuthenticationToken token = buildToken("google", attrs);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("error=email_missing");
    }

    @Test
    void onAuthenticationSuccess_githubUser_withEmailAttr() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "ghuser@example.com");
        attrs.put("name", "GH User");
        attrs.put("login", "ghuser");
        OAuth2AuthenticationToken token = buildToken("github", attrs);

        User user = buildActiveUser("ghuser@example.com", Role.PATIENT, AuthProvider.GITHUB);
        when(userRepository.findByEmail("ghuser@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("gh-jwt");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("token=gh-jwt");
    }

    @Test
    void onAuthenticationSuccess_githubUser_noEmailFallsBackToLogin() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        // no email, only login
        attrs.put("login", "ghlogin");
        OAuth2AuthenticationToken token = buildToken("github", attrs);

        User user = buildActiveUser("ghlogin@github.com", Role.PATIENT, AuthProvider.GITHUB);
        when(userRepository.findByEmail("ghlogin@github.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("gh-jwt2");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("token=gh-jwt2");
    }

    @Test
    void onAuthenticationSuccess_localUserLinkedToGoogle_updatesProvider() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "local@example.com");
        attrs.put("name", "Local User");
        OAuth2AuthenticationToken token = buildToken("google", attrs);

        User user = buildActiveUser("local@example.com", Role.PATIENT, AuthProvider.LOCAL);
        when(userRepository.findByEmail("local@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("linked-jwt");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        verify(userRepository, atLeastOnce()).save(any(User.class));
        assertThat(response.getRedirectedUrl()).contains("token=linked-jwt");
    }

    @Test
    void onAuthenticationSuccess_githubUser_withNameAttr_usesName() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "namedgh@example.com");
        attrs.put("name", "Named GH User");
        attrs.put("login", "namedlogin");
        OAuth2AuthenticationToken token = buildToken("github", attrs);

        User user = buildActiveUser("namedgh@example.com", Role.PATIENT, AuthProvider.GITHUB);
        when(userRepository.findByEmail("namedgh@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("named-gh-jwt");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("token=named-gh-jwt");
    }

    @Test
    void onAuthenticationSuccess_newGoogleUser_nullName_usesEmailAsName() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "nonameuser@example.com");
        // name is null / absent
        OAuth2AuthenticationToken token = buildToken("google", attrs);

        when(userRepository.findByEmail("nonameuser@example.com")).thenReturn(Optional.empty());
        User savedUser = buildActiveUser("nonameuser@example.com", Role.PATIENT, AuthProvider.GOOGLE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("noname-jwt");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("token=noname-jwt");
    }

    @Test
    void onAuthenticationSuccess_githubUser_noEmailNoLogin_redirectsWithError() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        // neither email nor login
        OAuth2AuthenticationToken token = buildToken("github", attrs);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl()).contains("error=email_missing");
    }
}
