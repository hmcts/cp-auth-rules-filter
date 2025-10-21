package uk.gov.moj.cpp.authz.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdentityClientTest {

    @InjectMocks
    IdentityClient identityClient;

    @Test
    void request_header_should_accept_alphanumeric_user_id(){
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
    void request_header_should_error_when_bad_user_id(){
        assertThrows(RuntimeException.class, ()-> identityClient.sanitizeUserId("/evil-user"));
        assertThrows(RuntimeException.class, ()-> identityClient.sanitizeUserId("bad:"));
        assertThrows(RuntimeException.class, ()-> identityClient.sanitizeUserId("bad%"));
        assertThrows(RuntimeException.class, ()-> identityClient.sanitizeUserId("5d35a9ac-%-e1f6-4f8e-9cc9-8184cf9fdb2d"));
    }
}