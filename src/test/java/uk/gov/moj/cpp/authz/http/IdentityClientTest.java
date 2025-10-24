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
    void request_header_should_accept_alphanumeric_user_id() {
        identityClient.sanitizeUserId(null);
        identityClient.sanitizeUserId("");
        identityClient.sanitizeUserId("1");
        identityClient.sanitizeUserId("user-1");
        identityClient.sanitizeUserId("userid99");
        identityClient.sanitizeUserId("USER");
        identityClient.sanitizeUserId("5d35a9ac-e1f6-4f8e-9cc9-8184cf9fdb2d");
        // no exception
    }

    @Test
    void request_header_should_error_when_bad_user_id() {
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUserId("/evil-user"));
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUserId("bad:"));
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUserId("bad%"));
        assertThrows(RuntimeException.class, () -> identityClient.sanitizeUserId("5d35a9ac-%-e1f6-4f8e-9cc9-8184cf9fdb2d"));
    }

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

    @Test
    void sanitize_for_log_should_remove_bad_chars(){
        assertThat(identityClient.sanitizeForLog("any")).isEqualTo("any");
        assertThat(identityClient.sanitizeForLog("OK")).isEqualTo("OK");
        assertThat(identityClient.sanitizeForLog("-bad%or!")).isEqualTo("-bador");
        assertThat(identityClient.sanitizeForLog("bad\nstuff")).isEqualTo("badstuff");
    }
}