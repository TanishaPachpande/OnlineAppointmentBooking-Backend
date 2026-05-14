package com.medibook.auth.oauth2;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieOAuth2AuthorizationRequestRepositoryTest {

    private CookieOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieOAuth2AuthorizationRequestRepository();
    }

    private OAuth2AuthorizationRequest buildAuthRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId("client-id")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .scope("email", "profile")
                .state("test-state-xyz")
                .build();
    }

    @Test
    void saveAndLoad_authorizationRequest_roundTrip() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest authRequest = buildAuthRequest();

        repository.saveAuthorizationRequest(authRequest, request, response);

        // Simulate browser sending cookie back
        String cookieValue = null;
        for (jakarta.servlet.http.Cookie c : response.getCookies()) {
            if (CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME.equals(c.getName())) {
                cookieValue = c.getValue();
                break;
            }
        }
        assertThat(cookieValue).isNotNull();

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookieValue));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request2);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo("test-state-xyz");
        assertThat(loaded.getClientId()).isEqualTo("client-id");
    }

    @Test
    void loadAuthorizationRequest_noCookies_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);
        assertThat(result).isNull();
    }

    @Test
    void loadAuthorizationRequest_wrongCookie_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("OTHER_COOKIE", "somevalue"));
        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);
        assertThat(result).isNull();
    }

    @Test
    void saveAuthorizationRequest_nullRequest_deletesCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "oldvalue"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(null, request, response);

        boolean found = false;
        for (jakarta.servlet.http.Cookie c : response.getCookies()) {
            if (CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME.equals(c.getName())) {
                found = true;
                assertThat(c.getMaxAge()).isZero();
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void removeAuthorizationRequest_returnsRequestAndDeletesCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        OAuth2AuthorizationRequest authRequest = buildAuthRequest();

        repository.saveAuthorizationRequest(authRequest, request, saveResponse);

        String cookieValue = null;
        for (jakarta.servlet.http.Cookie c : saveResponse.getCookies()) {
            if (CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME.equals(c.getName())) {
                cookieValue = c.getValue();
                break;
            }
        }

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookieValue));
        MockHttpServletResponse removeResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request2, removeResponse);
        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("test-state-xyz");
    }

    @Test
    void removeAuthorizationRequest_noCookies_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(request, response);
        assertThat(result).isNull();
    }

    @Test
    void loadAuthorizationRequest_invalidCookieValue_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "not-valid-base64!!!"));

        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);
        assertThat(result).isNull();
    }
}
