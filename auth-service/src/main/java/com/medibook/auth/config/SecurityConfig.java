package com.medibook.auth.config;

import com.medibook.auth.oauth2.CookieOAuth2AuthorizationRequestRepository;
import com.medibook.auth.oauth2.OAuth2SuccessHandler;
import com.medibook.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final CookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
						  OAuth2SuccessHandler oAuth2SuccessHandler,
						  CookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.oAuth2SuccessHandler = oAuth2SuccessHandler;
		this.cookieAuthRequestRepository = cookieAuthRequestRepository;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())

				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/auth/register",
								"/auth/login",
								"/auth/test",
								"/auth/send-otp",
								"/auth/verify-otp",
								"/auth/register-with-otp",
								// OAuth2 endpoints
								"/login/oauth2/**",
								"/oauth2/**",
								"/auth/oauth2/callback",
								// Swagger
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html"
						).permitAll()

						// ── FIX: Restrict admin endpoints to ADMIN authority only ──
						// Uses hasAuthority (not hasRole) because CustomUserDetailsService
						// now sets SimpleGrantedAuthority("ADMIN") without "ROLE_" prefix
						.requestMatchers("/auth/admin/**").hasAuthority("ADMIN")

						.anyRequest().authenticated()
				)

				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)

				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(endpoint -> endpoint
								.authorizationRequestRepository(cookieAuthRequestRepository)
						)
						.redirectionEndpoint(redir -> redir
								.baseUri("/login/oauth2/code/*")
						)
						.successHandler(oAuth2SuccessHandler)
				)

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
			throws Exception {
		return configuration.getAuthenticationManager();
	}
}
