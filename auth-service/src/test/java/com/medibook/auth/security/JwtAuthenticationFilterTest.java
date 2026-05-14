package com.medibook.auth.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService customUserDetailsService;
    @InjectMocks private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_publicPaths_returnsTrue() {
        String[] publicPaths = {
            "/auth/register", "/auth/login", "/auth/test",
            "/auth/send-otp", "/auth/verify-otp", "/auth/register-with-otp"
        };
        for (String path : publicPaths) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setServletPath(path);
            assertThat(filter.shouldNotFilter(req))
                    .as("Path: " + path)
                    .isTrue();
        }
    }

    @Test
    void shouldNotFilter_protectedPath_returnsFalse() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/auth/admin/users");
        assertThat(filter.shouldNotFilter(req)).isFalse();
    }

    @Test
    void shouldNotFilter_oauth2Path_returnsTrue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/oauth2/authorization/google");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void shouldNotFilter_loginOauth2Path_returnsTrue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/login/oauth2/code/google");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void shouldNotFilter_swaggerUiPath_returnsTrue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/swagger-ui/index.html");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void shouldNotFilter_swaggerUiHtml_returnsTrue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/swagger-ui.html");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void shouldNotFilter_v3ApiDocsPath_returnsTrue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/v3/api-docs/swagger-config");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void doFilterInternal_noAuthHeader_continuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        UserDetails userDetails = new User("john@example.com", "hash",
                List.of(new SimpleGrantedAuthority("PATIENT")));

        when(jwtUtil.extractUsername("valid-token")).thenReturn("john@example.com");
        when(customUserDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .extracting(auth -> auth.getName())
                .isEqualTo("john@example.com");
    }

    @Test
    void doFilterInternal_invalidToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        UserDetails userDetails = new User("john@example.com", "hash",
                List.of(new SimpleGrantedAuthority("PATIENT")));

        when(jwtUtil.extractUsername("invalid-token")).thenReturn("john@example.com");
        when(customUserDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_bearerPrefixMissing_continuesChainWithoutAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic sometoken");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
