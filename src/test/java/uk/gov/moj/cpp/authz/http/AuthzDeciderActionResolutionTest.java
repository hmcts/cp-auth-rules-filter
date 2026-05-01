package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.ACTION_HEADER;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.ACTION_HELLO;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.GROUP_LA;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.METHOD_GET;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.METHOD_POST;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.PATH_HELLO;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.PATH_ORDERS_123;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_123;

import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@ExtendWith(MockitoExtension.class)
class AuthzDeciderActionResolutionTest {

    @Mock
    private IdentityClient identityClient;

    @Mock
    private IdentityToGroupsMapper identityToGroupsMapper;

    @Mock
    private DroolsAuthzEngine droolsAuthzEngine;

    @Captor
    private ArgumentCaptor<Action> actionCaptor;

    private HttpAuthzProperties properties;
    private AuthzDecider decider;

    @BeforeEach
    void setUp() {
        properties = new HttpAuthzProperties();
        properties.setUserIdHeader("CJSCPPUID");
        properties.setActionHeader(ACTION_HEADER);
        properties.setActionRequired(false);
        decider = new AuthzDecider(properties, identityClient, identityToGroupsMapper, droolsAuthzEngine,
                new SpringTemplatedUrlFallback(null));
        stubAuthorisedUser();
        when(droolsAuthzEngine.evaluate(any(), actionCaptor.capture())).thenReturn(true);
    }

    @Test
    void usesHeaderActionName() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(ACTION_HEADER, ACTION_HELLO);

        decider.decide(req, USER_123, PATH_HELLO);

        assertEquals(ACTION_HELLO, actionCaptor.getValue().name(), "Action name should match header");
    }

    @Test
    void vendorContentTypeWinsOverActionHeader() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, "/sjp/anything");
        req.addHeader("Content-Type", "application/vnd.sjp.delete-financial-means+json");
        req.addHeader(ACTION_HEADER, "POST /sjp/anything");

        decider.decide(req, USER_123, "/sjp/anything");

        assertEquals("sjp.delete-financial-means", actionCaptor.getValue().name(),
                "Vendor token from Content-Type must take priority");
    }

    @Test
    void vendorAcceptUsedWhenNoContentType() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, "/hearing/draft-result");
        req.addHeader("Accept", "application/json, application/vnd.hearing.get-draft-result+json;q=0.9");

        decider.decide(req, USER_123, "/hearing/draft-result");

        assertEquals("hearing.get-draft-result", actionCaptor.getValue().name(),
                "Vendor token from Accept must be used when Content-Type is absent");
    }

    @Test
    void templatedRouteUsedWhenNeitherHeaderNorVendorSupplied() throws Exception {
        decider = new AuthzDecider(properties, identityClient, identityToGroupsMapper, droolsAuthzEngine,
                new SpringTemplatedUrlFallback(mappingThatReturnsPattern("/api/orders/{id}")));

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123);

        decider.decide(req, USER_123, PATH_ORDERS_123);

        assertEquals("POST /api/orders/{id}", actionCaptor.getValue().name(),
                "Computed action should use the templated route");
    }

    private void stubAuthorisedUser() {
        final IdentityResponse identity = mock(IdentityResponse.class);
        when(identity.userId()).thenReturn(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identity);
        when(identityToGroupsMapper.toGroups(identity)).thenReturn(Set.of(GROUP_LA));
    }

    private static RequestMappingHandlerMapping mappingThatReturnsPattern(final String pattern) throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        final HandlerExecutionChain chain = mock(HandlerExecutionChain.class);
        when(mapping.getHandler(any(HttpServletRequest.class))).thenAnswer(invocation -> {
            final HttpServletRequest req = invocation.getArgument(0);
            req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
            return chain;
        });
        return mapping;
    }
}
