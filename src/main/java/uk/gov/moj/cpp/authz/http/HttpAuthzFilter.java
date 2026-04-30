package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.RequestActionResolver.ResolvedAction;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.providers.RequestUserAndGroupProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

public final class HttpAuthzFilter implements Filter {
    public static final String OPTIONS = "OPTIONS";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAuthzFilter.class);

    private final HttpAuthzProperties properties;
    private final IdentityClient identityClient;
    private final IdentityToGroupsMapper identityToGroupsMapper;
    private final DroolsAuthzEngine droolsAuthzEngine;
    private final RequestMappingHandlerMapping handlerMapping;

    public HttpAuthzFilter(final HttpAuthzProperties properties,
                           final IdentityClient identityClient,
                           final IdentityToGroupsMapper identityToGroupsMapper,
                           final DroolsAuthzEngine droolsAuthzEngine,
                           final RequestMappingHandlerMapping handlerMapping) {
        this.properties = properties;
        this.identityClient = identityClient;
        this.identityToGroupsMapper = identityToGroupsMapper;
        this.droolsAuthzEngine = droolsAuthzEngine;
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain filterChain) throws IOException, ServletException {


        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final String pathWithinApplication = new UrlPathHelper().getPathWithinApplication(httpRequest);

        if (OPTIONS.equalsIgnoreCase(httpRequest.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean invokeChain = false;
        boolean isExcluded = false;
        for (final String prefix : properties.getExcludePathPrefixes()) {
            if (pathWithinApplication.startsWith(prefix)) {
                isExcluded = true;
                break;
            }
        }

        if (isExcluded) {
            invokeChain = true;
        } else {
            final String userId = httpRequest.getHeader(properties.getUserIdHeader());
            if (StringUtils.hasText(userId)) {
                final ResolvedAction resolved =
                        RequestActionResolver.resolve(httpRequest, properties.getActionHeader(), pathWithinApplication);

                if (properties.isActionRequired() && !(resolved.vendorSupplied() || resolved.headerSupplied())) {
                    httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Missing header: " + properties.getActionHeader());
                } else {
                    final ResolvedAction effectiveAction = springTemplatedUrlFallback(httpRequest, pathWithinApplication, resolved);

                    final IdentityResponse identityResponse = identityClient.fetchIdentity(userId);
                    final Set<String> groups = identityToGroupsMapper.toGroups(identityResponse);
                    final AuthzPrincipal principal =
                            new AuthzPrincipal(identityResponse.userId(), null, null, null, groups, identityResponse.permissions());
                    httpRequest.setAttribute(AuthzPrincipal.class.getName(), principal);

                    final Map<String, Object> attributes = new HashMap<>();
                    attributes.put("method", httpRequest.getMethod());
                    attributes.put("path", pathWithinApplication);

                    final Action action = new Action(effectiveAction.name(), attributes);
                    final RequestUserAndGroupProvider perRequestProvider = new RequestUserAndGroupProvider(principal, new ObjectMapper());

                    final boolean allowed = droolsAuthzEngine.evaluate(perRequestProvider, action);
                    if (allowed) {
                        invokeChain = true;
                    } else {
                        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    }
                }

            } else {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Missing header: " + properties.getUserIdHeader());
            }
        }

        if (invokeChain) {
            filterChain.doFilter(request, response);
        }
    }

    private ResolvedAction springTemplatedUrlFallback(final HttpServletRequest httpRequest,
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
