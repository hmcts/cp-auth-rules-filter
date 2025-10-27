package uk.gov.moj.cpp.authz.http;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UrlPathHelper;
import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzHeaderProperties;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzPathProperties;
import uk.gov.moj.cpp.authz.http.providers.RequestUserAndGroupProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Slf4j
public final class HttpAuthzFilter implements Filter {

    private final HttpAuthzPathProperties pathProperties;
    private final HttpAuthzHeaderProperties headerProperties;
    private final RequestActionResolver actionResolver;
    private final IdentityClient identityClient;
    private final IdentityToGroupsMapper identityToGroupsMapper;
    private final DroolsAuthzEngine droolsAuthzEngine;

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain filterChain) throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        final String pathWithinApplication = new UrlPathHelper().getPathWithinApplication(httpRequest);
        if (pathProperties.getExcludePathPrefixes().stream().anyMatch(pathWithinApplication::startsWith)) {
            filterChain.doFilter(request, response);
        } else {
            final Optional<UUID> userId = validateUserId(httpRequest.getHeader(headerProperties.getUserIdHeaderName()));
            if (userId.isEmpty()) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing header: " + headerProperties.getUserIdHeaderName());
            } else {
                final ResolvedAction resolvedAction = actionResolver.resolve(httpRequest, headerProperties.getActionHeaderName(), pathWithinApplication);
                if (headerProperties.isActionRequired() && !resolvedAction.isVendorSupplied() && !resolvedAction.isHeaderSupplied()) {
                    httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing header: " + headerProperties.getActionHeaderName());
                } else {
                    if (validateIdentityRules(userId.get(), httpRequest, resolvedAction)) {
                        filterChain.doFilter(request, response);
                    } else {
                        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    }
                }
            }
        }
    }

    private boolean validateIdentityRules(final UUID userId, final HttpServletRequest request, final ResolvedAction resolvedAction) {
        final String pathWithinApplication = new UrlPathHelper().getPathWithinApplication(request);
        final IdentityResponse identityResponse = identityClient.fetchIdentity(userId);
        final Set<String> groups = identityToGroupsMapper.toGroups(identityResponse);
        final AuthzPrincipal principal =
                new AuthzPrincipal(identityResponse.userId(), null, null, null, groups);
        request.setAttribute(AuthzPrincipal.class.getName(), principal);

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("method", request.getMethod());
        attributes.put("path", pathWithinApplication);

        final Action action = new Action(resolvedAction.getName(), attributes);
        final RequestUserAndGroupProvider perRequestProvider = new RequestUserAndGroupProvider(principal);

        return droolsAuthzEngine.evaluate(perRequestProvider, action);
    }

    public Optional<UUID> validateUserId(final String userId) {
        try {
            return Optional.of(UUID.fromString(userId));
        } catch (Exception e) {
            log.error("Failed to convert userId to uuid");
            return Optional.empty();
        }
    }
}
