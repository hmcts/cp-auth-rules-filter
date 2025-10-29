package uk.gov.moj.cpp.authz.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.moj.cpp.authz.drools.AuthAction;
import uk.gov.moj.cpp.authz.drools.DroolsAuthEngine;
import uk.gov.moj.cpp.authz.http.config.HttpAuthHeaderProperties;
import uk.gov.moj.cpp.authz.http.config.HttpAuthPathProperties;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpAuthFilterTest {

    private static final String USER_ID_HEADER_NAME = "CJSCPPUID";
    private static final String ACTION_HEADER_NAME = "CPP-ACTION";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String PATH_HELLO = "/api/hello";
    private static final String PATH_ECHO = "/api/echo";
    private static final String PATH_EXCLUDED = "/actuator";
    private static final String PATH_EXCLUDED_METRICS = "/metrics/prometheus";
    private static final UUID USER_ID = UUID.fromString("a05078bd-b189-4fd9-8c6e-181e9a123456");
    private static final UUID USER_ID_UC = UUID.fromString("E3F58BF7-FB59-4E5C-8ED9-E6A0F5966743");
    private static final String ACTION_GET_HELLO = "GET /api/hello";
    private static final String ACTION_POST_ECHO = "POST /api/echo";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String VENDOR_DELETE_MEDIA_TYPE = "\"application/vnd.sjp.delete-financial-means+json\"";
    private static final String PATH_ATTRIBUTE = "path";

    @Mock
    RequestActionResolver actionResolver;

    @Mock
    private IdentityClient identityClient;

    @Mock
    private IdentityToGroupsMapper identityToGroupsMapper;

    @Mock
    private DroolsAuthEngine droolsAuthEngine;

    @Mock
    private FilterChain filterChain;

    @Test
    void forwardsRequestUnchangedWhenPathIsExcluded() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_EXCLUDED);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);
        authzFilter.doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
    }

    @Test
    void returns401WhenUserIdHeaderIsMissing() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus(), "Expected 401 when user id header is missing");
    }

    @Test
    void returns400WhenActionHeaderIsRequiredButMissing() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        ResolvedAction resolvedAction = new ResolvedAction("Name", false, false);
        when(actionResolver.resolve(req, actionTrueHeader().getActionHeaderName(), PATH_HELLO)).thenReturn(resolvedAction);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionTrueHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(400, res.getStatus(), "Expected 400 when action header is required but missing");
    }

    @Test
    void allowsRequestWhenEngineApproves() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        final MockHttpServletResponse res = new MockHttpServletResponse();

        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        mockHelloResolvedAction(req, ACTION_HEADER_NAME, false, false);
        when(droolsAuthEngine.evaluate(any(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(200, res.getStatus(), "Expected 200 when engine approves");
    }

    @Test
    void principalAttributeIsSetWhenEngineApproves() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        mockHelloResolvedAction(req, ACTION_HEADER_NAME, false, false);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        when(droolsAuthEngine.evaluate(any(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        final Object principalAttr = req.getAttribute(AuthzPrincipal.class.getName());
        assertNotNull(principalAttr, "Principal should be attached to the request");
    }


    @Test
    void deniesRequestWhenEngineRejects() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        mockHelloResolvedAction(req, ACTION_HEADER_NAME, false, false);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of("Guests"));
        when(droolsAuthEngine.evaluate(any(), any())).thenReturn(false);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(403, res.getStatus(), "Expected 403 when engine rejects");
    }

    @Test
    void usesHeaderActionName() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        req.addHeader(ACTION_HEADER_NAME, ACTION_GET_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        ResolvedAction resolvedAction = new ResolvedAction(ACTION_GET_HELLO, true, true);
        when(actionResolver.resolve(req, ACTION_HEADER_NAME, PATH_HELLO)).thenReturn(resolvedAction);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<AuthAction> captor = ArgumentCaptor.forClass(AuthAction.class);
        when(droolsAuthEngine.evaluate(captor.capture(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(ACTION_GET_HELLO, captor.getValue().getName(), "Action name should match header");
    }

    @Test
    void usesHeaderMethodAttribute() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        req.addHeader(ACTION_HEADER_NAME, ACTION_GET_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        mockHelloResolvedAction(req, ACTION_HEADER_NAME, false, false);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<AuthAction> captor = ArgumentCaptor.forClass(AuthAction.class);
        when(droolsAuthEngine.evaluate(captor.capture(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(METHOD_GET, captor.getValue().getAttributes().get("method"), "Method attribute should be GET");
    }

    @Test
    void usesHeaderPathAttribute() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        req.addHeader(ACTION_HEADER_NAME, ACTION_GET_HELLO);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        mockHelloResolvedAction(req, ACTION_HEADER_NAME, false, false);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<AuthAction> captor = ArgumentCaptor.forClass(AuthAction.class);
        when(droolsAuthEngine.evaluate(captor.capture(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(PATH_HELLO, captor.getValue().getAttributes().get(PATH_ATTRIBUTE), "Path attribute should be /api/hello");
    }

    @Test
    void computesMethodAttribute() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ECHO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        mockEchoResolvedAction(req, ACTION_HEADER_NAME, false, false);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<AuthAction> captor = ArgumentCaptor.forClass(AuthAction.class);
        when(droolsAuthEngine.evaluate(captor.capture(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(METHOD_POST, captor.getValue().getAttributes().get("method"), "Method attribute should be POST");
    }

    @Test
    void computesPathAttribute() throws IOException, ServletException {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_ECHO);
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        mockEchoResolvedAction(req, ACTION_HEADER_NAME, false, false);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));
        final ArgumentCaptor<AuthAction> captor = ArgumentCaptor.forClass(AuthAction.class);
        when(droolsAuthEngine.evaluate(captor.capture(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals(PATH_ECHO, captor.getValue().getAttributes().get(PATH_ATTRIBUTE), "Path attribute should be /api/echo");
    }

    @Test
    void honorsMultipleExcludePrefixes() throws Exception {
        HttpAuthPathProperties pathProperties = new HttpAuthPathProperties(
                false, "http://identity.com", PATH_ATTRIBUTE,
                List.of("/health/", "/metrics/", "/usersgroups-query-api/"));
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_EXCLUDED_METRICS);
        final MockHttpServletResponse res = new MockHttpServletResponse();
        HttpAuthFilter authzFilter = new HttpAuthFilter(pathProperties, actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
    }

    @Test
    void resolvesActionFromContentTypeVendorWinsOverHeader() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, "/sjp/anything");
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        req.addHeader("Content-Type", VENDOR_DELETE_MEDIA_TYPE);
        req.addHeader(ACTION_HEADER_NAME, "POST /sjp/anything"); // should be ignored in favor of vendor
        final MockHttpServletResponse res = new MockHttpServletResponse();
        ResolvedAction resolvedAction = new ResolvedAction("sjp.delete-financial-means", true, true);
        when(actionResolver.resolve(req, ACTION_HEADER_NAME, "/sjp/anything")).thenReturn(resolvedAction);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));

        final ArgumentCaptor<AuthAction> captor = ArgumentCaptor.forClass(AuthAction.class);
        when(droolsAuthEngine.evaluate(captor.capture(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals("sjp.delete-financial-means", captor.getValue().getName(),
                "Vendor token from Content-Type must take priority");
    }

    @Test
    void resolvesActionFromAcceptWhenNoContentType() throws Exception {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, "/hearing/draft-result");
        req.addHeader(USER_ID_HEADER_NAME, USER_ID);
        req.addHeader("Accept", "application/json, application/vnd.hearing.get-draft-result+json;q=0.9");
        final MockHttpServletResponse res = new MockHttpServletResponse();
        ResolvedAction resolvedAction = new ResolvedAction("hearing.get-draft-result", true, true);
        when(actionResolver.resolve(req, ACTION_HEADER_NAME, "/hearing/draft-result")).thenReturn(resolvedAction);
        final IdentityResponse identityResponse = mockIdentity(USER_ID);
        when(identityClient.fetchIdentity(USER_ID)).thenReturn(identityResponse);
        when(identityToGroupsMapper.toGroups(identityResponse)).thenReturn(Set.of(GROUP_LEGAL_ADVISERS));

        final ArgumentCaptor<AuthAction> captor = ArgumentCaptor.forClass(AuthAction.class);
        when(droolsAuthEngine.evaluate(captor.capture(), any())).thenReturn(true);
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);

        authzFilter.doFilter(req, res, filterChain);

        assertEquals("hearing.get-draft-result", captor.getValue().getName(),
                "Vendor token from Accept must be used when Content-Type is absent");
    }

    @Test
    void validate_userid_should_reject_none_guid() {
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);
        assertThat(authzFilter.validateUserId(null)).isEmpty();
        assertThat(authzFilter.validateUserId("")).isEmpty();
        assertThat(authzFilter.validateUserId("bad")).isEmpty();
        assertThat(authzFilter.validateUserId("a05078bd")).isEmpty();
        assertThat(authzFilter.validateUserId("a05078bd-b189-4fd9-8c6e")).isEmpty();
        assertThat(authzFilter.validateUserId("a05078bd-b189-4fd9-8c6e-181e9a1234567")).isEmpty();
        assertThat(authzFilter.validateUserId(USER_ID + "0")).isEmpty();
    }

    @Test
    void validate_userid_should_return_good_guid() {
        HttpAuthFilter authzFilter = new HttpAuthFilter(defaultPaths(), actionFalseHeader(), actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);
        assertThat(authzFilter.validateUserId("a05078bd-b189-4fd9-8c6e-181e9a123456").get()).isEqualTo(USER_ID);
        assertThat(authzFilter.validateUserId("E3F58BF7-FB59-4E5C-8ED9-E6A0F5966743").get()).isEqualTo(USER_ID_UC);
    }

    private IdentityResponse mockIdentity(final UUID userId) {
        final IdentityResponse identity = mock(IdentityResponse.class);
        when(identity.userId()).thenReturn(userId);
        return identity;
    }

    private void mockHelloResolvedAction(HttpServletRequest req, String actionHeader, boolean vendorSupplied, boolean headerSupplied) {
        ResolvedAction resolvedAction = new ResolvedAction("Name", vendorSupplied, headerSupplied);
        when(actionResolver.resolve(req, actionHeader, PATH_HELLO)).thenReturn(resolvedAction);
    }

    private void mockEchoResolvedAction(HttpServletRequest req, String actionHeader, boolean vendorSupplied, boolean headerSupplied) {
        ResolvedAction resolvedAction = new ResolvedAction("Name", vendorSupplied, headerSupplied);
        when(actionResolver.resolve(req, actionHeader, PATH_ECHO)).thenReturn(resolvedAction);
    }

    private HttpAuthPathProperties defaultPaths() {
        return new HttpAuthPathProperties(false, "http://identity.com", PATH_ATTRIBUTE, List.of("/actuator"));
    }

    private HttpAuthHeaderProperties actionFalseHeader() {
        return new HttpAuthHeaderProperties(false, USER_ID_HEADER_NAME, ACTION_HEADER_NAME,
                "application/vnd.usersgroups.get-logged-in-user-permissions+json");
    }

    private HttpAuthHeaderProperties actionTrueHeader() {
        return new HttpAuthHeaderProperties(true, USER_ID_HEADER_NAME, ACTION_HEADER_NAME,
                "application/vnd.usersgroups.get-logged-in-user-permissions+json");
    }
}
