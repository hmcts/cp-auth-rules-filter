package uk.gov.moj.cpp.authz.http;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;
import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.RequestActionResolver.ResolvedAction;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.providers.RequestUserAndGroupProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
public final class HttpAuthzFilter implements Filter {
    private static final String GUID_REGEX = "^[0-9a-zA-Z\\-]*$";
    private static final int GUID_LENGTH = 36;

    private final HttpAuthzProperties properties;
    private final IdentityClient identityClient;
    private final IdentityToGroupsMapper identityToGroupsMapper;
    private final DroolsAuthzEngine droolsAuthzEngine;

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain filterChain) throws IOException, ServletException {

        boolean invokeChain = false;

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final String pathWithinApplication = new UrlPathHelper().getPathWithinApplication(httpRequest);
        final Optional<String> excluded = properties.getExcludePathPrefixes()
                .stream()
                .filter(excludedPrefix -> pathWithinApplication.startsWith(excludedPrefix))
                .findFirst();

        if (excluded.isPresent()) {
            invokeChain = true;
        } else {
            final Optional<String> userId = validateUserId(httpRequest.getHeader(properties.getUserIdHeader()));
            if (userId.isPresent()) {
                final ResolvedAction resolved =
                        RequestActionResolver.resolve(httpRequest, properties.getActionHeader(), pathWithinApplication);

                if (properties.isActionRequired() && !(resolved.vendorSupplied() || resolved.headerSupplied())) {
                    httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Missing header: " + properties.getActionHeader());
                } else {
                    final IdentityResponse identityResponse = identityClient.fetchIdentity(userId.get());
                    final Set<String> groups = identityToGroupsMapper.toGroups(identityResponse);
                    final AuthzPrincipal principal =
                            new AuthzPrincipal(identityResponse.userId(), null, null, null, groups);
                    httpRequest.setAttribute(AuthzPrincipal.class.getName(), principal);

                    final Map<String, Object> attributes = new HashMap<>();
                    attributes.put("method", httpRequest.getMethod());
                    attributes.put("path", pathWithinApplication);

                    final Action action = new Action(resolved.name(), attributes);
                    final RequestUserAndGroupProvider perRequestProvider =
                            new RequestUserAndGroupProvider(principal);

                    final boolean allowed = droolsAuthzEngine.evaluate(perRequestProvider, action);
                    if (allowed) {
                        invokeChain = true;
                    } else {
                        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    }
                }
            } else {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing header: " + properties.getUserIdHeader());
            }
        }

        if (invokeChain) {
            filterChain.doFilter(request, response);
        }
    }

    public Optional<String> validateUserId(String userId) {
        if (StringUtils.hasLength(userId) && userId.length() == GUID_LENGTH && userId.matches(GUID_REGEX)) {
            return Optional.of(userId);
        }
        return Optional.empty();
    }
}
