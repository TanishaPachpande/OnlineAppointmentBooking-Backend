package com.medibook.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String email = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        if (!isAuthorized(path, method, role)) {
            log.warn("Access denied for user={} role={} path={}", email, role, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Authenticated-User", email)
                .header("X-Authenticated-Role", role)
                .build();

        log.info("JWT validated. user={} role={} path={}", email, role, path);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        return path.startsWith("/auth/register")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/test")
                || (path.startsWith("/providers") && method == HttpMethod.GET)
                || path.startsWith("/slots/available")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-ui.html");
    }

    private boolean isAuthorized(String path, HttpMethod method, String role) {
        if (role == null) {
            return false;
        }

        // ADMIN: full access
        if ("ADMIN".equals(role)) {
            return true;
        }

        // PROVIDER rules
        if ("PROVIDER".equals(role)) {
            return isProviderAllowed(path, method);
        }

        // PATIENT rules
        if ("PATIENT".equals(role)) {
            return isPatientAllowed(path, method);
        }

        return false;
    }

    private boolean isProviderAllowed(String path, HttpMethod method) {
        return path.startsWith("/auth/profile")
                || path.startsWith("/providers/user")
                || path.startsWith("/slots")
                || path.startsWith("/appointments/provider")
                || path.startsWith("/appointments/")
                || path.startsWith("/payments");
    }

    private boolean isPatientAllowed(String path, HttpMethod method) {
        return path.startsWith("/auth/profile")
                || (path.startsWith("/providers") && method == HttpMethod.GET)
                || path.startsWith("/slots/available")
                || path.startsWith("/appointments")
                || path.startsWith("/payments");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}