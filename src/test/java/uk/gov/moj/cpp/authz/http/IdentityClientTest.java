package uk.gov.moj.cpp.authz.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class IdentityClientTest {

    @InjectMocks
    IdentityClient identityClient;

    @Test
    void properties_url_should_accept_valid_url() {
        identityClient.sanitizeUrl("http://localhost");
        identityClient.sanitizeUrl("https://localhost");
        identityClient.sanitizeUrl("http://localhost:8080");
        identityClient.sanitizeUrl("http://localhost:8080/anything");
        identityClient.sanitizeUrl("http://localhost:8080/anything?param=xyz");
        String defaultIdentityUrl =  "http://localhost:8080/usersgroups-query-api/query/api/rest/usersgroups/users/logged-in-user/permissions";
        identityClient.sanitizeUrl(defaultIdentityUrl);
        // no exception
    }

    @Test
    void properties_url_should_error_when_bad_url() {
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUrl(null));
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUrl(""));
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUrl(","));
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUrl("this--bad-url"));
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUrl("http://localhost-%%$^&& iuyi"));
    }
}