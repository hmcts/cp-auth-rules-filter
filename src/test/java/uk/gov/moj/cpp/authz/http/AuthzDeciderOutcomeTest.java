package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.ACTION_HEADER;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_ID_HEADER;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.GROUP_LA;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.METHOD_GET;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.PATH_HELLO;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_123;

import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.AuthzDecider.Allow;
import uk.gov.moj.cpp.authz.http.AuthzDecider.Decision;
import uk.gov.moj.cpp.authz.http.AuthzDecider.Deny;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthzDeciderOutcomeTest {

    @Mock
    private IdentityClient identityClient;

    @Mock
    private IdentityToGroupsMapper identityToGroupsMapper;

    @Mock
    private DroolsAuthzEngine droolsAuthzEngine;

    private final HttpAuthzProperties properties = HttpAuthzProperties.builder()
            .userIdHeader(USER_ID_HEADER)
            .actionHeader(ACTION_HEADER)
            .actionRequired(false)
            .build();

    private final HttpAuthzProperties propertiesActionRequired = HttpAuthzProperties.builder()
            .userIdHeader(USER_ID_HEADER)
            .actionHeader(ACTION_HEADER)
            .actionRequired(true)
            .build();

    private AuthzDecider decider;

    @BeforeEach
    void setUp() {
        decider = new AuthzDecider(properties, identityClient, identityToGroupsMapper, droolsAuthzEngine,
                new SpringTemplatedUrlFallback(null));
    }

    @Test
    void engineApprovesReturnsAllow() {
        stubAuthorisedUser();
        when(droolsAuthzEngine.evaluate(any(), any())).thenReturn(true);

        final Decision decision =
                decider.decide(new MockHttpServletRequest(METHOD_GET, PATH_HELLO), USER_123, PATH_HELLO);

        assertInstanceOf(Allow.class, decision);
    }

    @Test
    void engineRejectsReturnsDeny403() {
        stubAuthorisedUser();
        when(droolsAuthzEngine.evaluate(any(), any())).thenReturn(false);

        final Decision decision =
                decider.decide(new MockHttpServletRequest(METHOD_GET, PATH_HELLO), USER_123, PATH_HELLO);

        final Deny deny = assertInstanceOf(Deny.class, decision);
        assertEquals(403, deny.status());
        assertEquals("Access denied", deny.reason());
    }

    @Test
    void actionRequiredButNeitherHeaderNorVendorReturnsDeny400() {
        decider = new AuthzDecider(propertiesActionRequired, identityClient, identityToGroupsMapper,
                droolsAuthzEngine, new SpringTemplatedUrlFallback(null));

        final Decision decision =
                decider.decide(new MockHttpServletRequest(METHOD_GET, PATH_HELLO), USER_123, PATH_HELLO);

        final Deny deny = assertInstanceOf(Deny.class, decision);
        assertEquals(400, deny.status());
        assertEquals("Missing header: " + ACTION_HEADER, deny.reason());
    }

    private void stubAuthorisedUser() {
        final IdentityResponse identity = mock(IdentityResponse.class);
        when(identity.userId()).thenReturn(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identity);
        when(identityToGroupsMapper.toGroups(identity)).thenReturn(Set.of(GROUP_LA));
    }
}
