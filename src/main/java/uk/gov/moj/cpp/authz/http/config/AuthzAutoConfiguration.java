package uk.gov.moj.cpp.authz.http.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.HttpAuthzFilter;
import uk.gov.moj.cpp.authz.http.IdentityClient;
import uk.gov.moj.cpp.authz.http.IdentityToGroupsMapper;
import uk.gov.moj.cpp.authz.http.RequestActionResolver;

@AutoConfiguration
@EnableConfigurationProperties(HttpAuthzHeaderProperties.class)
@AllArgsConstructor
@Slf4j
public class AuthzAutoConfiguration {

    private static final int AUTH_FILTER_PRIORITY = Ordered.HIGHEST_PRECEDENCE + 30;
    private final HttpAuthzHeaderProperties properties;

    @PostConstruct
    private void onStart() throws JsonProcessingException {
        final String propertiesJson = new ObjectMapper().writeValueAsString(properties);
        log.info("CPP HTTP Authz starter ACTIVE -> {}", propertiesJson);
    }

    @Bean
    public FilterRegistrationBean<HttpAuthzFilter> httpAuthzFilterRegistration(
            final HttpAuthzPathProperties pathProperties,
            final HttpAuthzHeaderProperties headerProperties,
            final RequestActionResolver actionResolver,
            final IdentityClient identityClient,
            final IdentityToGroupsMapper identityToGroupsMapper,
            final DroolsAuthzEngine droolsAuthzEngine) {

        final HttpAuthzFilter filter =
                new HttpAuthzFilter(pathProperties, headerProperties, actionResolver, identityClient, identityToGroupsMapper, droolsAuthzEngine);
        final FilterRegistrationBean<HttpAuthzFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(AUTH_FILTER_PRIORITY);
        registration.addUrlPatterns("/*");
        registration.setName("cppHttpAuthzFilter");
        return registration;
    }
}
