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
        assertThat(identityClient.constructUrl("http://localhost", "/path").toString()).isEqualTo("http://localhost/path");
        String path = "/usersgroups-query-api/permissions";
        assertThat(identityClient.constructUrl("http://localhost:8090", path).toString()).isEqualTo("http://localhost:8090/usersgroups-query-api/permissions");
    }

    @Test
    void properties_url_should_error_when_bad_url() {
        assertThrows(RuntimeException.class, () -> identityClient.constructUrl("this--bad-url", "path"));
        assertThrows(RuntimeException.class, () -> identityClient.constructUrl("http://localhost-%%$^&& iuyi", "path"));
    }
}