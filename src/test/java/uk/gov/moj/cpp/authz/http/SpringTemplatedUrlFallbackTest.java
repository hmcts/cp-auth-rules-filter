package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.authz.http.RequestActionResolver.ResolvedAction;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class SpringTemplatedUrlFallbackTest {

    private static final String METHOD_POST = "POST";
    private static final String PATH_ORDERS_123 = "/api/orders/123";
    private static final String RAW_ACTION = METHOD_POST + " " + PATH_ORDERS_123;

    @Test
    void returnsResolvedWhenHandlerMappingIsNull() {
        final SpringTemplatedUrlFallback fallback = new SpringTemplatedUrlFallback(null);
        final ResolvedAction resolved = new ResolvedAction(RAW_ACTION, false, false);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123), PATH_ORDERS_123, resolved);

        assertSame(resolved, result, "Null handler mapping must short-circuit and return the resolved action unchanged");
    }

    @Test
    void returnsResolvedWhenVendorSupplied() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        final SpringTemplatedUrlFallback fallback = new SpringTemplatedUrlFallback(mapping);
        final ResolvedAction resolved = new ResolvedAction("sjp.delete-financial-means", true, false);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123), PATH_ORDERS_123, resolved);

        assertSame(resolved, result, "Vendor-supplied resolution must skip templating");
    }

    @Test
    void returnsResolvedWhenHeaderSupplied() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        final SpringTemplatedUrlFallback fallback = new SpringTemplatedUrlFallback(mapping);
        final ResolvedAction resolved = new ResolvedAction("orders.create", false, true);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123), PATH_ORDERS_123, resolved);

        assertSame(resolved, result, "Header-supplied resolution must skip templating");
    }

    @Test
    void templatesPathWithMultiplePathVariables() throws Exception {
        final String rawPath = "/api/users/u1/orders/o9";
        final SpringTemplatedUrlFallback fallback =
                new SpringTemplatedUrlFallback(mappingThatReturnsPattern("/api/users/{userId}/orders/{orderId}"));
        final ResolvedAction resolved = new ResolvedAction("PUT " + rawPath, false, false);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest("PUT", rawPath), rawPath, resolved);

        assertEquals("PUT /api/users/{userId}/orders/{orderId}", result.name(),
                "Action name should reflect every templated path variable");
    }

    @Test
    void returnsResolvedWhenPatternEqualsRawPath() throws Exception {
        final SpringTemplatedUrlFallback fallback =
                new SpringTemplatedUrlFallback(mappingThatReturnsPattern(PATH_ORDERS_123));
        final ResolvedAction resolved = new ResolvedAction(RAW_ACTION, false, false);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123), PATH_ORDERS_123, resolved);

        assertSame(resolved, result,
                "When matched pattern equals the raw path there is no templating to apply");
    }

    @Test
    void returnsResolvedWhenNoHandlerMatches() throws Exception {
        final SpringTemplatedUrlFallback fallback =
                new SpringTemplatedUrlFallback(mappingThatReturnsNoHandler());
        final ResolvedAction resolved = new ResolvedAction(RAW_ACTION, false, false);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123), PATH_ORDERS_123, resolved);

        assertSame(resolved, result,
                "No matching handler should fall back to the resolved action");
    }

    @Test
    void returnsResolvedWhenHandlerMappingThrows() throws Exception {
        final SpringTemplatedUrlFallback fallback =
                new SpringTemplatedUrlFallback(mappingThatThrows());
        final ResolvedAction resolved = new ResolvedAction(RAW_ACTION, false, false);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123), PATH_ORDERS_123, resolved);

        assertSame(resolved, result,
                "Handler-mapping failures must not surface to callers; resolved action is returned");
    }

    @Test
    void returnsResolvedWhenPatternAttributeMissing() throws Exception {
        final SpringTemplatedUrlFallback fallback =
                new SpringTemplatedUrlFallback(mappingThatMatchesButLeavesPatternUnset());
        final ResolvedAction resolved = new ResolvedAction(RAW_ACTION, false, false);

        final ResolvedAction result = fallback.apply(
                new MockHttpServletRequest(METHOD_POST, PATH_ORDERS_123), PATH_ORDERS_123, resolved);

        assertSame(resolved, result,
                "Missing best-matching-pattern attribute should fall back to the resolved action");
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

    private static RequestMappingHandlerMapping mappingThatThrows() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandler(any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("simulated handler-mapping failure"));
        return mapping;
    }

    private static RequestMappingHandlerMapping mappingThatMatchesButLeavesPatternUnset() throws Exception {
        final RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        final HandlerExecutionChain chain = mock(HandlerExecutionChain.class);
        when(mapping.getHandler(any(HttpServletRequest.class))).thenReturn(chain);
        return mapping;
    }
}
