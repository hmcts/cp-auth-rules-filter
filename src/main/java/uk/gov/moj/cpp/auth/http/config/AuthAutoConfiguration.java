package uk.gov.moj.cpp.auth.http.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import uk.gov.moj.cpp.auth.drools.DroolsAuthEngine;
import uk.gov.moj.cpp.auth.http.HttpAuthFilter;
import uk.gov.moj.cpp.auth.http.IdentityClient;
import uk.gov.moj.cpp.auth.http.IdentityToGroupsMapper;
import uk.gov.moj.cpp.auth.http.RequestActionResolver;

@Configuration
@AllArgsConstructor
@Slf4j
public class AuthAutoConfiguration {

    private static final int AUTH_FILTER_PRIORITY = Ordered.HIGHEST_PRECEDENCE + 30;

    private final HttpAuthPathProperties pathProperties;
    private final HttpAuthHeaderProperties headerProperties;


    @Bean
    public FilterRegistrationBean<HttpAuthFilter> httpAuthFilterRegistration(
            final HttpAuthPathProperties pathProperties,
            final HttpAuthHeaderProperties headerProperties,
            final RequestActionResolver actionResolver,
            final IdentityClient identityClient,
            final IdentityToGroupsMapper identityToGroupsMapper,
            final DroolsAuthEngine droolsAuthEngine) {
        logProperties();
        final HttpAuthFilter filter =
                new HttpAuthFilter(pathProperties, headerProperties, actionResolver, identityClient, identityToGroupsMapper, droolsAuthEngine);
        final FilterRegistrationBean<HttpAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(AUTH_FILTER_PRIORITY);
        registration.addUrlPatterns("/*");
        registration.setName("cppHttpAuthFilter");
        return registration;
    }

    @SneakyThrows
    private void logProperties() {
        final ObjectMapper mapper = new ObjectMapper();
        log.info("AuthRulesFilter pathProperties -> {}", mapper.writeValueAsString(pathProperties));
        log.info("AuthRulesFilter headerProperties  -> {}", mapper.writeValueAsString(headerProperties));
    }
}
