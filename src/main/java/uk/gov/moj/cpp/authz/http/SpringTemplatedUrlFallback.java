package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.RequestActionResolver.ResolvedAction;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public final class SpringTemplatedUrlFallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringTemplatedUrlFallback.class);

    private final RequestMappingHandlerMapping handlerMapping;

    public SpringTemplatedUrlFallback(final RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public ResolvedAction apply(final HttpServletRequest httpRequest,
                                final String pathWithinApplication,
                                final ResolvedAction resolved) {
        if (resolved.vendorSupplied() || resolved.headerSupplied() || handlerMapping == null) {
            return resolved;
        }

        final String matchedPattern = resolveMatchedPattern(httpRequest);
        if (matchedPattern == null || matchedPattern.equals(pathWithinApplication)) {
            return resolved;
        }

        final String templatedName = httpRequest.getMethod() + " " + matchedPattern;
        return new ResolvedAction(templatedName, false, false);
    }

    private String resolveMatchedPattern(final HttpServletRequest httpRequest) {
        try {
            final HandlerExecutionChain chain = handlerMapping.getHandler(httpRequest);
            if (chain == null) {
                LOGGER.warn("No Spring MVC handler matched {} {} - authz rules will see the raw path",
                        httpRequest.getMethod(), httpRequest.getRequestURI());
                return null;
            }
            final Object pattern = httpRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            return pattern instanceof String ? (String) pattern : pattern == null ? null : pattern.toString();
        } catch (final Exception ex) {
            LOGGER.warn("Failed to resolve matched route template for {} {} - falling back to raw path: {}",
                    httpRequest.getMethod(), httpRequest.getRequestURI(), ex.toString());
            return null;
        }
    }
}
