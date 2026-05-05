package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.ACTION_HEADER;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.GROUP_LA;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.METHOD_ATTRIBUTE;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.METHOD_GET;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.METHOD_POST;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.PATH_ATTRIBUTE;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.PATH_ECHO;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.PATH_HELLO;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_123;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_ID_HEADER;

import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthzDeciderPrincipalTest {

    @Mock
    private IdentityClient identityClient;

    @Mock
    private IdentityToGroupsMapper identityToGroupsMapper;

    @Mock
    private DroolsAuthzEngine droolsAuthzEngine;

    @Captor
    private ArgumentCaptor<Action> actionCaptor;

    private final HttpAuthzProperties properties = HttpAuthzProperties.builder()
            .userIdHeader(USER_ID_HEADER)
            .actionHeader(ACTION_HEADER)
            .actionRequired(false)
            .build();

    private AuthzDecider decider;

    @BeforeEach
    void setUp() {
        decider = new AuthzDecider(properties, identityClient, identityToGroupsMapper, droolsAuthzEngine,
                new SpringTemplatedUrlFallback(null));
        stubAuthorisedUser();
    }

    @Test
    void setsAuthzPrincipalAsRequestAttribute() {
        when(droolsAuthzEngine.evaluate(any(), any())).thenReturn(true);
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);

        decider.decide(req, USER_123, PATH_HELLO);

        assertNotNull(req.getAttribute(AuthzPrincipal.class.getName()),
                "Principal should be attached to the request");
    }

    @Test
    void principalAttributeIsAlsoSetOnDeny() {
        when(droolsAuthzEngine.evaluate(any(), any())).thenReturn(false);
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);

        decider.decide(req, USER_123, PATH_HELLO);

        assertNotNull(req.getAttribute(AuthzPrincipal.class.getName()),
                "Principal should be attached to the request even when the decision is Deny");
    }

    @Test
    void setsMethodAttributeOnAction() {
        when(droolsAuthzEngine.evaluate(any(), actionCaptor.capture())).thenReturn(true);
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ECHO);

        decider.decide(req, USER_123, PATH_ECHO);

        assertEquals(METHOD_POST, actionCaptor.getValue().attributes().get(METHOD_ATTRIBUTE),
                "Method attribute should be POST");
    }

    @Test
    void setsPathAttributeOnAction() {
        when(droolsAuthzEngine.evaluate(any(), actionCaptor.capture())).thenReturn(true);
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ECHO);

        decider.decide(req, USER_123, PATH_ECHO);

        assertEquals(PATH_ECHO, actionCaptor.getValue().attributes().get(PATH_ATTRIBUTE),
                "Path attribute should be /api/echo");
    }

    private void stubAuthorisedUser() {
        final IdentityResponse identity = mock(IdentityResponse.class);
        when(identity.userId()).thenReturn(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identity);
        when(identityToGroupsMapper.toGroups(identity)).thenReturn(Set.of(GROUP_LA));
    }
}
