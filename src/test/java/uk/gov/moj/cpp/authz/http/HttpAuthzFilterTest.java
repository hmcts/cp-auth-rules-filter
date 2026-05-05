package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.METHOD_GET;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.PATH_HELLO;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_123;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_ID_HEADER;

import uk.gov.moj.cpp.authz.http.AuthzDecider.Allow;
import uk.gov.moj.cpp.authz.http.AuthzDecider.Deny;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;

import java.util.List;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class HttpAuthzFilterTest {

    @Mock
    private AuthzDecider authzDecider;

    @Mock
    private FilterChain filterChain;

    private final HttpAuthzProperties properties = HttpAuthzProperties.builder()
            .userIdHeader(USER_ID_HEADER)
            .build();

    private HttpAuthzFilter filter;
    private MockHttpServletRequest req;
    private MockHttpServletResponse res;

    @BeforeEach
    void setUp() {
        filter = new HttpAuthzFilter(properties, new PathExclusionChecker(List.of("/usersgroups-query-api/")), authzDecider);
        req = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        res = new MockHttpServletResponse();
    }

    @Test
    void optionsRequestForwardsWithoutCallingDecider() throws Exception {
        final MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", PATH_HELLO);

        filter.doFilter(options, res, filterChain);

        verify(filterChain, times(1)).doFilter(options, res);
        verify(authzDecider, never()).decide(any(), any(), any());
    }

    @Test
    void excludedPathForwardsWithoutCallingDecider() throws Exception {
        final MockHttpServletRequest excluded =
                new MockHttpServletRequest(METHOD_GET, "/usersgroups-query-api/query/api/rest/ping");

        filter.doFilter(excluded, res, filterChain);

        verify(filterChain, times(1)).doFilter(excluded, res);
        verify(authzDecider, never()).decide(any(), any(), any());
    }

    @Test
    void missingUserIdHeaderReturns401() throws Exception {
        filter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus(), "Expected 401 when user id header is missing");
        verify(authzDecider, never()).decide(any(), any(), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void deciderAllowInvokesChain() throws Exception {
        req.addHeader(USER_ID_HEADER, USER_123);
        when(authzDecider.decide(req, USER_123, PATH_HELLO)).thenReturn(new Allow());

        filter.doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
    }

    @Test
    void deciderDenyWritesStatusAndDoesNotInvokeChain() throws Exception {
        req.addHeader(USER_ID_HEADER, USER_123);
        when(authzDecider.decide(req, USER_123, PATH_HELLO)).thenReturn(new Deny(403, "Access denied"));

        filter.doFilter(req, res, filterChain);

        assertEquals(403, res.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }
}
