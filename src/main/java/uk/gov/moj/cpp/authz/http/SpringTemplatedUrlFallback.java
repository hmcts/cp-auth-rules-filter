package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.RequestActionResolver.ResolvedAction;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
public final class SpringTemplatedUrlFallback {

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
                log.warn("No Spring MVC handler matched {} {} - authz rules will see the raw path",
                        Encode.forJava(httpRequest.getMethod()),
                        Encode.forJava(httpRequest.getRequestURI()));
                return null;
            }
            final Object pattern = httpRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            return pattern instanceof String ? (String) pattern : pattern == null ? null : pattern.toString();
        } catch (final Exception ex) {
            log.warn("Failed to resolve matched route template for {} {} - falling back to raw path: {}",
                    Encode.forJava(httpRequest.getMethod()),
                    Encode.forJava(httpRequest.getRequestURI()),
                    ex.toString());
            return null;
        }
    }
}
