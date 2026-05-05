package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.RequestActionResolver.ResolvedAction;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.providers.RequestUserAndGroupProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AuthzDecider {

    private final HttpAuthzProperties properties;
    private final IdentityClient identityClient;
    private final IdentityToGroupsMapper identityToGroupsMapper;
    private final DroolsAuthzEngine droolsAuthzEngine;
    private final SpringTemplatedUrlFallback springTemplatedUrlFallback;

    public AuthzDecider(final HttpAuthzProperties properties,
                        final IdentityClient identityClient,
                        final IdentityToGroupsMapper identityToGroupsMapper,
                        final DroolsAuthzEngine droolsAuthzEngine,
                        final SpringTemplatedUrlFallback springTemplatedUrlFallback) {
        this.properties = properties;
        this.identityClient = identityClient;
        this.identityToGroupsMapper = identityToGroupsMapper;
        this.droolsAuthzEngine = droolsAuthzEngine;
        this.springTemplatedUrlFallback = springTemplatedUrlFallback;
    }

    public Decision decide(final HttpServletRequest request,
                           final String userId,
                           final String pathWithinApplication) {

        final ResolvedAction resolved =
                RequestActionResolver.resolve(request, properties.getActionHeader(), pathWithinApplication);

        if (properties.isActionRequired() && !(resolved.vendorSupplied() || resolved.headerSupplied())) {
            return new Deny(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing header: " + properties.getActionHeader());
        }

        final ResolvedAction effectiveAction =
                springTemplatedUrlFallback.apply(request, pathWithinApplication, resolved);

        final IdentityResponse identityResponse = identityClient.fetchIdentity(userId);
        final Set<String> groups = identityToGroupsMapper.toGroups(identityResponse);
        final AuthzPrincipal principal =
                new AuthzPrincipal(identityResponse.userId(), null, null, null, groups, identityResponse.permissions());
        request.setAttribute(AuthzPrincipal.class.getName(), principal);

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("method", request.getMethod());
        attributes.put("path", pathWithinApplication);

        final Action action = new Action(effectiveAction.name(), attributes);
        final RequestUserAndGroupProvider perRequestProvider =
                new RequestUserAndGroupProvider(principal, new ObjectMapper());

        final boolean allowed = droolsAuthzEngine.evaluate(perRequestProvider, action);
        if (allowed) {
            return new Allow();
        }
        return new Deny(HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }

    public sealed interface Decision permits Allow, Deny {
    }

    public record Allow() implements Decision {
    }

    public record Deny(int status, String reason) implements Decision {
    }
}
