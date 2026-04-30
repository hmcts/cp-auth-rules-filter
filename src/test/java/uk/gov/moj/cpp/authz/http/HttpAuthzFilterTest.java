package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@ExtendWith(MockitoExtension.class)
class HttpAuthzFilterTest {

    private static final String USER_ID_HEADER = "CJSCPPUID";
    private static final String ACTION_HEADER = "CPP-ACTION";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String PATH_HELLO = "/api/hello";
    private static final String PATH_ECHO = "/api/echo";
    private static final String PATH_EXCLUDED = "/usersgroups-query-api/query/api/rest/ping";
    private static final String PATH_EXCLUDED_METRICS = "/metrics/prometheus";
    private static final String USER_123 = "user-123";
    private static final String USER_ABC = "user-abc";
    private static final String ACTION_GET_HELLO = "GET /api/hello";
    private static final String ACTION_POST_ECHO = "POST /api/echo";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String PATH_ATTRIBUTE = "path";
    private static final String PATH_ORDERS_123 = "/api/orders/123";

    @Mock
    private IdentityClient identityClient;

    @Mock
    private IdentityToGroupsMapper identityToGroupsMapper;

    @Mock
    private DroolsAuthzEngine droolsAuthzEngine;

    @Mock
    private FilterChain filterChain;

    private HttpAuthzProperties httpAuthzProperties;
    private HttpAuthzFilter httpAuthzFilter;

    @BeforeEach
    void setUp() {
        httpAuthzProperties = new HttpAuthzProperties();
        httpAuthzProperties.setEnabled(true);
        httpAuthzProperties.setUserIdHeader(USER_ID_HEADER);
        httpAuthzProperties.setActionHeader(ACTION_HEADER);
        httpAuthzProperties.setAcceptHeader("application/vnd.usersgroups.get-logged-in-user-permissions+json");
        httpAuthzProperties.setDroolsClasspathPattern("classpath*:/uk/gov/moj/cpp/authz/demo/*.drl");
        httpAuthzProperties.setReloadOnEachRequest(false);
        httpAuthzProperties.setActionRequired(false);
        httpAuthzProperties.setDenyWhenNoRules(true);
        httpAuthzProperties.setExcludePathPrefixes(List.of("/usersgroups-query-api/", "/actuator/"));

        httpAuthzFilter = new HttpAuthzFilter(
                httpAuthzProperties, identityClient, identityToGroupsMapper, droolsAuthzEngine,
                new SpringTemplatedUrlFallback(null));
    }


    @Test
    void forwardsRequestUnchangedWhenPathIsExcluded() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_EXCLUDED);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        httpAuthzFilter.doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
    }

    @Test
    void returns401WhenUserIdHeaderIsMissing() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus(), "Expected 401 when user id header is missing");
    }

    @Test
    void returns400WhenActionHeaderIsRequiredButMissing() throws Exception {
        httpAuthzProperties.setActionRequired(true);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER, USER_123);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(400, res.getStatus(), "Expected 400 when action header is required but missing");
    }

    @Test
    void allowsRequestWhenEngineApproves() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER, USER_123);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        when(droolsAuthzEngine.evaluate(any(), any())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(200, res.getStatus(), "Expected 200 when engine approves");
    }

    @Test
    void principalAttributeIsSetWhenEngineApproves() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER, USER_123);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        when(droolsAuthzEngine.evaluate(any(), any())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        final Object principalAttr = req.getAttribute(AuthzPrincipal.class.getName());
        assertNotNull(principalAttr, "Principal should be attached to the request");
    }

    @Test
    void deniesRequestWhenEngineRejects() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER, USER_123);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of("Guests"));
        when(droolsAuthzEngine.evaluate(any(), any())).thenReturn(false);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(403, res.getStatus(), "Expected 403 when engine rejects");
    }

    @Test
    void usesHeaderActionName() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER, USER_123);
        req.addHeader(ACTION_HEADER, ACTION_GET_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(ACTION_GET_HELLO, captor.getValue().name(), "Action name should match header");
    }

    @Test
    void usesHeaderMethodAttribute() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER, USER_123);
        req.addHeader(ACTION_HEADER, ACTION_GET_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(METHOD_GET, captor.getValue().attributes().get("method"), "Method attribute should be GET");
    }

    @Test
    void usesHeaderPathAttribute() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER, USER_123);
        req.addHeader(ACTION_HEADER, ACTION_GET_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(PATH_HELLO, captor.getValue().attributes().get(PATH_ATTRIBUTE), "Path attribute should be /api/hello");
    }

    @Test
    void computesActionName() throws IOException, ServletException {
        httpAuthzProperties.setActionRequired(false);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ECHO);
        req.addHeader(USER_ID_HEADER, USER_123);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(ACTION_POST_ECHO, captor.getValue().name(), "Computed action should be method + path");
    }

    @Test
    void computesMethodAttribute() throws IOException, ServletException {
        httpAuthzProperties.setActionRequired(false);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ECHO);
        req.addHeader(USER_ID_HEADER, USER_123);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(METHOD_POST, captor.getValue().attributes().get("method"), "Method attribute should be POST");
    }

    @Test
    void computesPathAttribute() throws IOException, ServletException {
        httpAuthzProperties.setActionRequired(false);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ECHO);
        req.addHeader(USER_ID_HEADER, USER_123);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals(PATH_ECHO, captor.getValue().attributes().get(PATH_ATTRIBUTE), "Path attribute should be /api/echo");
    }

    @Test
    void honorsMultipleExcludePrefixes() throws Exception {
        httpAuthzProperties.setExcludePathPrefixes(List.of("/health/", "/metrics/", "/usersgroups-query-api/"));

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_EXCLUDED_METRICS);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        httpAuthzFilter.doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
    }

    @Test
    void resolvesActionFromContentTypeVendorWinsOverHeader() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, "/sjp/anything");
        req.addHeader(USER_ID_HEADER, USER_123);
        req.addHeader("Content-Type", "application/vnd.sjp.delete-financial-means+json");
        req.addHeader(ACTION_HEADER, "POST /sjp/anything"); // should be ignored in favor of vendor
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));

        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals("sjp.delete-financial-means", captor.getValue().name(),
                "Vendor token from Content-Type must take priority");
    }

    @Test
    void resolvesActionFromAcceptWhenNoContentType() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, "/hearing/draft-result");
        req.addHeader(USER_ID_HEADER, USER_123);
        req.addHeader("Accept", "application/json, application/vnd.hearing.get-draft-result+json;q=0.9");
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_123);
        when(identityClient.fetchIdentity(USER_123)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));

        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        httpAuthzFilter.doFilter(req, res, filterChain);

        assertEquals("hearing.get-draft-result", captor.getValue().name(),
                "Vendor token from Accept must be used when Content-Type is absent");
    }

    @Test
    void fallbackPathAttributeStaysAsRawPathEvenWhenTemplated() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123);
        req.addHeader(USER_ID_HEADER, USER_ABC);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_ABC);
        when(identityClient.fetchIdentity(USER_ABC)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        filterWithMapping(mappingThatReturnsPattern("/api/orders/{id}")).doFilter(req, res, filterChain);

        assertEquals(PATH_ORDERS_123, captor.getValue().attributes().get(PATH_ATTRIBUTE),
                "Path attribute should remain the raw URI for downstream rule access");
    }

    @Test
    void vendorContentTypeWinsAndSkipsTemplating() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123);
        req.addHeader(USER_ID_HEADER, USER_ABC);
        req.addHeader("Content-Type", "application/vnd.sjp.delete-financial-means+json");
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_ABC);
        when(identityClient.fetchIdentity(USER_ABC)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        filterWithMapping(mapping).doFilter(req, res, filterChain);

        assertEquals("sjp.delete-financial-means", captor.getValue().name(),
                "Vendor token from Content-Type must still win without invoking handler mapping");
        verify(mapping, times(0)).getHandler(any(HttpServletRequest.class));
    }

    @Test
    void explicitActionHeaderWinsAndSkipsTemplating() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123);
        req.addHeader(USER_ID_HEADER, USER_ABC);
        req.addHeader(ACTION_HEADER, "orders.create");
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_ABC);
        when(identityClient.fetchIdentity(USER_ABC)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        filterWithMapping(mapping).doFilter(req, res, filterChain);

        assertEquals("orders.create", captor.getValue().name(),
                "Explicit CPP-ACTION header must still win without invoking handler mapping");
        verify(mapping, times(0)).getHandler(any(HttpServletRequest.class));
    }

    @Test
    void vendorAcceptWinsAndSkipsTemplating() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);

        final MockHttpServletRequest req = new MockHttpServletRequest("GET", PATH_ORDERS_123);
        req.addHeader(USER_ID_HEADER, USER_ABC);
        req.addHeader("Accept", "application/json, application/vnd.hearing.get-draft-result+json;q=0.9");
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_ABC);
        when(identityClient.fetchIdentity(USER_ABC)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        filterWithMapping(mapping).doFilter(req, res, filterChain);

        assertEquals("hearing.get-draft-result", captor.getValue().name(),
                "Vendor token from Accept must win without invoking handler mapping");
        verify(mapping, times(0)).getHandler(any(HttpServletRequest.class));
    }

    @Test
    void methodAttributeMatchesRequestMethodWhenTemplated() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest("DELETE", PATH_ORDERS_123);
        req.addHeader(USER_ID_HEADER, USER_ABC);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_ABC);
        when(identityClient.fetchIdentity(USER_ABC)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        filterWithMapping(mappingThatReturnsPattern("/api/orders/{id}")).doFilter(req, res, filterChain);

        assertEquals("DELETE", captor.getValue().attributes().get("method"),
                "Method attribute must remain the request method when path is templated");
    }

    @Test
    void actionRequiredTrueShortCircuitsBeforeTemplating() throws Exception {
        httpAuthzProperties.setActionRequired(true);
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123);
        req.addHeader(USER_ID_HEADER, USER_ABC);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        filterWithMapping(mapping).doFilter(req, res, filterChain);

        assertEquals(400, res.getStatus(),
                "actionRequired=true must reject when neither vendor nor header is supplied");
        verify(mapping, times(0)).getHandler(any(HttpServletRequest.class));
        verify(droolsAuthzEngine, times(0)).evaluate(any(), any());
    }

    @Test
    void excludedPathDoesNotInvokeHandlerMapping() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);

        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_EXCLUDED);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        filterWithMapping(mapping).doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
        verify(mapping, times(0)).getHandler(any(HttpServletRequest.class));
    }

    @Test
    void optionsRequestDoesNotInvokeHandlerMapping() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);

        final MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", PATH_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        filterWithMapping(mapping).doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
        verify(mapping, times(0)).getHandler(any(HttpServletRequest.class));
    }

    @Test
    void pathAttributeStaysRawEvenWhenHandlerMappingFails() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123);
        req.addHeader(USER_ID_HEADER, USER_ABC);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_ABC);
        when(identityClient.fetchIdentity(USER_ABC)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        when(droolsAuthzEngine.evaluate(any(), captor.capture())).thenReturn(true);

        filterWithMapping(mappingThatReturnsNoHandler()).doFilter(req, res, filterChain);

        assertEquals(PATH_ORDERS_123, captor.getValue().attributes().get(PATH_ATTRIBUTE),
                "Path attribute must remain the raw URI even when templating cannot resolve a pattern");
    }

    private static IdentityResponse mockIdentity(final String userId) {
        final IdentityResponse identity = mock(IdentityResponse.class);
        when(identity.userId()).thenReturn(userId);
        return identity;
    }

    private HttpAuthzFilter filterWithMapping(final RequestMappingHandlerMapping mapping) {
        return new HttpAuthzFilter(
                httpAuthzProperties, identityClient, identityToGroupsMapper, droolsAuthzEngine,
                new SpringTemplatedUrlFallback(mapping));
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

    private static RequestMappingHandlerMapping mappingThatReturnsNoHandler() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandler(any(HttpServletRequest.class))).thenReturn(null);
        return mapping;
    }
}
