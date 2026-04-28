package com.medibook.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        log.info("➡️ Incoming request: {} {}", method, path); // ✅ Log every request

        if (isPublicPath(path, method)) {
            log.info("✅ Public path, skipping auth: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.info("🔑 Auth header: {}", authHeader != null ? "present" : "MISSING"); // ✅ Log header presence

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        log.info("🔐 Token (first 20 chars): {}", token.substring(0, Math.min(20, token.length()))); // ✅ Log token

        boolean isValid = jwtUtil.validateToken(token);
        log.info("✔️ Token valid: {}", isValid); // ✅ Log validation result

        if (!isValid) {
            return onError(exchange, "Invalid JWT Token", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);
        log.info("👤 Email: {}, Role: {}", email, role); // ✅ Log extracted claims

        if (!isAuthorized(path, method, role)) {
            log.warn("🚫 Access denied for role: {} on path: {}", role, path);
            return onError(exchange, "Access Denied for role: " + role, HttpStatus.FORBIDDEN);
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Authenticated-User", email)
                .header("X-Authenticated-Role", role)
                .build();

        log.info("✅ Forwarding request to downstream: {} {}", method, path);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        log.warn("❌ Auth Error: {} | Path: {}", err, exchange.getRequest().getURI().getPath());
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        return path.startsWith("/auth/register")
                || path.startsWith("/auth/login")
                || (path.startsWith("/providers") && method == HttpMethod.GET)
                || path.startsWith("/v3/api-docs");
    }

    private boolean isAuthorized(String path, HttpMethod method, String role) {
        if (role == null) return false;
        if ("ADMIN".equals(role)) return true;
        if ("PROVIDER".equals(role)) return isProviderAllowed(path, method);
        if ("PATIENT".equals(role)) return isPatientAllowed(path, method);
        return false;
    }

    private boolean isProviderAllowed(String path, HttpMethod method) {
        return path.startsWith("/providers")
                || path.startsWith("/slots")
                || path.startsWith("/appointments")
                || path.startsWith("/notifications")
                || path.startsWith("/reviews")
                || path.startsWith("/records");
    }

    private boolean isPatientAllowed(String path, HttpMethod method) {
        return (path.startsWith("/providers") && method == HttpMethod.GET)
                || (path.startsWith("/slots") && method == HttpMethod.GET)
                || path.startsWith("/appointments")
                || path.startsWith("/notifications")
                || path.startsWith("/records")
                || path.startsWith("/payments")
                || path.startsWith("/reviews");
    }
}