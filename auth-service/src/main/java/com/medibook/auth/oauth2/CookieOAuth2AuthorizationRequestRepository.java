package com.medibook.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/**
 * Stores the OAuth2 authorization request (including the 'state' and 'code_verifier')
 * in a SHORT-LIVED COOKIE instead of the HTTP session.
 *
 * Why this fixes [authorization_request_not_found]:
 *  - HttpSession-based storage breaks when the server restarts (session is lost).
 *  - HttpSession can also break behind a load balancer or when the API Gateway
 *    routes requests to different ports, causing the session cookie to mismatch.
 *  - A cookie travels with every browser request regardless of server restarts,
 *    so the state is always available when Google redirects back.
 *
 * Cookie spec:
 *  - Name:     OAUTH2_AUTH_REQUEST
 *  - MaxAge:   180 seconds (3 minutes — long enough for the OAuth2 flow)
 *  - HttpOnly: true  (JavaScript cannot read it)
 *  - Path:     /     (available to all paths)
 *  - SameSite: Lax   (needed for cross-site redirect from Google back to your app)
 */
@Component
@Slf4j
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("Loading OAuth2 authorization request from cookie");
        return getCookieValue(request, COOKIE_NAME)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            // Null means the flow is complete — delete the cookie
            deleteCookie(request, response, COOKIE_NAME);
            log.debug("OAuth2 authorization request cookie deleted (null request passed)");
            return;
        }

        String serialized = serialize(authorizationRequest);
        Cookie cookie = new Cookie(COOKIE_NAME, serialized);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
        // SameSite=Lax is required: Google redirects back cross-site, and Lax
        // allows the cookie to be sent on top-level navigations (GET redirects).
        // We set it via the Set-Cookie header directly because the Cookie API
        // in older Servlet specs doesn't have a setSameSite method.
        response.addCookie(cookie);
        // Override with explicit SameSite=Lax header
        String cookieHeader = String.format(
                "%s=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
                COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS
        );
        response.addHeader("Set-Cookie", cookieHeader);

        log.debug("OAuth2 authorization request saved to cookie");
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        log.debug("Removing OAuth2 authorization request cookie");
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response, COOKIE_NAME);
        return authRequest;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private java.util.Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return java.util.Optional.empty();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return java.util.Optional.of(cookie.getValue());
            }
        }
        return java.util.Optional.empty();
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    Cookie blank = new Cookie(name, "");
                    blank.setPath("/");
                    blank.setMaxAge(0);
                    response.addCookie(blank);
                    return;
                }
            }
        }
    }

    private String serialize(OAuth2AuthorizationRequest request) {
        return Base64.getUrlEncoder().encodeToString(
                SerializationUtils.serialize(request));
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                    Base64.getUrlDecoder().decode(value));
        } catch (Exception e) {
            log.warn("Failed to deserialize OAuth2 authorization request from cookie: {}", e.getMessage());
            return null;
        }
    }
}
