package com.medibook.auth.oauth2;

import com.medibook.auth.entity.*;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String frontendRedirectUri;

    @Value("${app.redis.token-ttl-hours:24}")
    private long tokenTtlHours;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                JwtUtil jwtUtil,
                                RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();
        String registrationId = token.getAuthorizedClientRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = extractEmail(registrationId, attributes);
        String name  = extractName(registrationId, attributes);

        if (email == null || email.isBlank()) {
            log.error("Could not extract email from OAuth2 provider: {}", registrationId);
            response.sendRedirect(frontendRedirectUri + "?error=email_missing");
            return;
        }

        AuthProvider provider = "google".equalsIgnoreCase(registrationId)
                ? AuthProvider.GOOGLE : AuthProvider.GITHUB;

        // ── FIX: findByEmail finds the user regardless of provider ──
        // If user already exists (even as LOCAL), we REUSE their existing role.
        // We never overwrite role — an ADMIN who logs in via Google stays ADMIN.
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // Only brand-new users get PATIENT as default
            log.info("Creating new OAuth2 user: {} via {}", email, registrationId);
            User newUser = User.builder()
                    .fullName(name != null ? name : email)
                    .email(email)
                    .passwordHash("OAUTH2_NO_PASSWORD")
                    .phone("0000000000")
                    .role(Role.PATIENT)   // default only for genuinely new users
                    .provider(provider)
                    .isActive(true)
                    .build();
            return userRepository.save(newUser);
        });

        // ── FIX: Update provider to GOOGLE if user originally registered locally ──
        // This links the Google account to the existing LOCAL account cleanly.
        if (user.getProvider() == AuthProvider.LOCAL) {
            user.setProvider(provider);
            userRepository.save(user);
            log.info("Linked Google OAuth2 to existing LOCAL account: {}", email);
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Deactivated user tried OAuth2 login: {}", email);
            response.sendRedirect(frontendRedirectUri + "?error=account_deactivated");
            return;
        }

        // Generate JWT using the user's ACTUAL role (not hardcoded PATIENT)
        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());

        // Cache in Redis
        redisTemplate.opsForValue()
                .set(TOKEN_KEY_PREFIX + email, jwt, Duration.ofHours(tokenTtlHours));

        log.info("OAuth2 login successful for: {} (role={}) via {}", email, user.getRole(), registrationId);

        // Redirect to frontend with token, role, and fullName
        String safeFullName = URLEncoder.encode(
                user.getFullName() != null ? user.getFullName() : "", StandardCharsets.UTF_8);
        String redirectUrl = String.format("%s?token=%s&userId=%d&role=%s&fullName=%s",
                frontendRedirectUri, jwt, user.getUserId(), user.getRole().name(), safeFullName);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String extractEmail(String provider, Map<String, Object> attrs) {
        if ("google".equalsIgnoreCase(provider)) {
            return (String) attrs.get("email");
        }
        if ("github".equalsIgnoreCase(provider)) {
            Object email = attrs.get("email");
            if (email != null) return email.toString();
            Object login = attrs.get("login");
            return login != null ? login + "@github.com" : null;
        }
        return null;
    }

    private String extractName(String provider, Map<String, Object> attrs) {
        if ("google".equalsIgnoreCase(provider)) {
            return (String) attrs.get("name");
        }
        if ("github".equalsIgnoreCase(provider)) {
            Object name = attrs.get("name");
            if (name != null) return name.toString();
            Object login = attrs.get("login");
            return login != null ? login.toString() : null;
        }
        return null;
    }
}