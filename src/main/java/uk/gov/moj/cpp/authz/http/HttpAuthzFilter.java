package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.AuthzDecider.Allow;
import uk.gov.moj.cpp.authz.http.AuthzDecider.Decision;
import uk.gov.moj.cpp.authz.http.AuthzDecider.Deny;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

public final class HttpAuthzFilter implements Filter {
    public static final String OPTIONS = "OPTIONS";

    private final HttpAuthzProperties properties;
    private final PathExclusionChecker exclusionChecker;
    private final AuthzDecider authzDecider;

    public HttpAuthzFilter(final HttpAuthzProperties properties,
                           final PathExclusionChecker exclusionChecker,
                           final AuthzDecider authzDecider) {
        this.properties = properties;
        this.exclusionChecker = exclusionChecker;
        this.authzDecider = authzDecider;
    }

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain filterChain) throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final String pathWithinApplication = new UrlPathHelper().getPathWithinApplication(httpRequest);

        if (OPTIONS.equalsIgnoreCase(httpRequest.getMethod())
                || exclusionChecker.isExcluded(pathWithinApplication)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String userId = httpRequest.getHeader(properties.getUserIdHeader());
        if (!StringUtils.hasText(userId)) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing header: " + properties.getUserIdHeader());
            return;
        }

        final Decision decision = authzDecider.decide(httpRequest, userId, pathWithinApplication);
        if (decision instanceof Allow) {
            filterChain.doFilter(request, response);
        } else if (decision instanceof Deny deny) {
            httpResponse.sendError(deny.status(), deny.reason());
        }
    }

}
