package uk.gov.moj.cpp.authz.http.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.HttpAuthzFilter;
import uk.gov.moj.cpp.authz.http.IdentityClient;
import uk.gov.moj.cpp.authz.http.IdentityToGroupsMapper;
import uk.gov.moj.cpp.authz.http.RequestActionResolver;

@Configuration
@AllArgsConstructor
@Slf4j
public class AuthzAutoConfiguration {

    private static final int AUTH_FILTER_PRIORITY = Ordered.HIGHEST_PRECEDENCE + 30;

    private final HttpAuthzPathProperties pathProperties;
    private final HttpAuthzHeaderProperties headerProperties;
    private final DroolsProperties droolsProperties;

    @Bean
    public FilterRegistrationBean<HttpAuthzFilter> httpAuthzFilterRegistration(
            final HttpAuthzPathProperties pathProperties,
            final HttpAuthzHeaderProperties headerProperties,
            final RequestActionResolver actionResolver,
            final IdentityClient identityClient,
            final IdentityToGroupsMapper identityToGroupsMapper,
            final DroolsAuthzEngine droolsAuthzEngine) {
        logProperties();
        final HttpAuthzFilter filter =
                new HttpAuthzFilter(pathProperties, headerProperties, actionResolver, identityClient, identityToGroupsMapper, droolsAuthzEngine);
        final FilterRegistrationBean<HttpAuthzFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(AUTH_FILTER_PRIORITY);
        registration.addUrlPatterns("/*");
        registration.setName("cppHttpAuthzFilter");
        return registration;
    }

    @SneakyThrows
    private void logProperties() {
        ObjectMapper mapper = new ObjectMapper();
        log.info("AuthRulesFilter pathProperties -> {}", mapper.writeValueAsString(pathProperties));
        log.info("AuthRulesFilter headerProperties  -> {}", mapper.writeValueAsString(headerProperties));
        log.info("AuthRulesFilter droolsProperties -> {}", mapper.writeValueAsString(droolsProperties));
    }
}
